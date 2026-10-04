package com.zifang.z.asset.core.collector;

import com.zifang.z.asset.api.AssetProvider;
import com.zifang.z.asset.core.domain.entity.AssetEntity;
import com.zifang.z.asset.core.domain.mapper.AssetMapper;
import com.zifang.util.json.model.JsonObject;
import com.zifang.util.json.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AssetCollector} 落库契约测试。
 *
 * <p>本仓此前零测试。这一组盯的是 {@code z_asset.attributes} 的列语义——
 * {@link AssetEntity#getAttributes()} 的字段注释写死「属性信息（JSON格式）」，
 * 而实现写进去的是 {@code Map.toString()}。</p>
 */
class AssetCollectorTest {

    private AssetMapper assetMapper;
    private AssetCollector collector;

    @BeforeEach
    void setUp() {
        assetMapper = mock(AssetMapper.class);
        collector = new AssetCollector(mock(AssetProviderRegistry.class), assetMapper);
    }

    private static AssetProvider.AssetDTO dtoWith(Map<String, String> attributes) {
        AssetProvider.AssetDTO dto = new AssetProvider.AssetDTO("A-1", "资产一", "TABLE", "crm", "ONLINE");
        dto.setAttributes(attributes);
        return dto;
    }

    private AssetProvider healthyProviderReturning(AssetProvider.AssetDTO... dtos) {
        AssetProvider provider = mock(AssetProvider.class);
        when(provider.isHealthy()).thenReturn(true);
        when(provider.listAssets()).thenReturn(Arrays.asList(dtos));
        return provider;
    }

    private AssetEntity captureInserted() {
        ArgumentCaptor<AssetEntity> captor = ArgumentCaptor.forClass(AssetEntity.class);
        verify(assetMapper).insert(captor.capture());
        return captor.getValue();
    }

    // ==================================================================
    // attributes：必须是 JSON，不是 {k=v}
    // ==================================================================

    @Test
    @DisplayName("attributes 为 null 时落 null（不写 \"null\" 字面量）")
    void attributesNullStaysNull() {
        when(assetMapper.selectOne(any())).thenReturn(null);
        collector.collectFromProvider(healthyProviderReturning(dtoWith(null)));

        assertNull(captureInserted().getAttributes());
    }

    @Test
    @DisplayName("attributes 落的是 JSON：能原样解析回同一个 Map")
    void attributesRoundTripsAsJson() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("region", "cn");
        attrs.put("port", "8080");
        when(assetMapper.selectOne(any())).thenReturn(null);
        collector.collectFromProvider(healthyProviderReturning(dtoWith(attrs)));

        String stored = captureInserted().getAttributes();
        assertNotNull(stored);
        assertTrue(stored.startsWith("{") && stored.endsWith("}"), "应为 JSON 对象，实际: " + stored);

        JsonObject back = JsonUtil.parseObject(stored);
        assertEquals("cn", back.getString("region"), "JSON 往返后属性不得丢失或变形");
        assertEquals("8080", back.getString("port"));
    }

    @Test
    @DisplayName("属性值含 , 和 = 时仍是无歧义的 JSON（{k=v} 伪格式在这里不可解析）")
    void attributesValueContainingSeparatorsStaysParsable() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("conn", "host=10.0.0.1,port=8080");
        attrs.put("filter", "a=1,b=2");
        when(assetMapper.selectOne(any())).thenReturn(null);
        collector.collectFromProvider(healthyProviderReturning(dtoWith(attrs)));

        String stored = captureInserted().getAttributes();
        JsonObject back = JsonUtil.parseObject(stored);
        assertEquals("host=10.0.0.1,port=8080", back.getString("conn"), "含分隔符的值必须能无损往返");
        assertEquals("a=1,b=2", back.getString("filter"));
    }

    @Test
    @DisplayName("属性值含引号时按 JSON 转义，而不是裸拼进伪格式")
    void attributesValueWithQuoteIsEscaped() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("expr", "name=\"zifang\"");
        when(assetMapper.selectOne(any())).thenReturn(null);
        collector.collectFromProvider(healthyProviderReturning(dtoWith(attrs)));

        String stored = captureInserted().getAttributes();
        // 裸引号会让整串不再是合法 JSON —— JsonUtil 解析必须仍然成功且值原样
        JsonObject back = JsonUtil.parseObject(stored);
        assertEquals("name=\"zifang\"", back.getString("expr"));
    }

    // ==================================================================
    // 采集短路条件：不该写库时确实一��不写
    // ==================================================================

    @Test
    @DisplayName("provider 不健康时完全不落库")
    void unhealthyProviderWritesNothing() {
        AssetProvider provider = mock(AssetProvider.class);
        when(provider.isHealthy()).thenReturn(false);

        collector.collectFromProvider(provider);

        verify(assetMapper, never()).insert(any(AssetEntity.class));
    }

    @Test
    @DisplayName("listAssets 返回 null 时不落库（而不是抛 NPE）")
    void nullAssetListWritesNothing() {
        AssetProvider provider = mock(AssetProvider.class);
        when(provider.isHealthy()).thenReturn(true);
        when(provider.listAssets()).thenReturn(null);

        collector.collectFromProvider(provider);

        verify(assetMapper, never()).insert(any(AssetEntity.class));
    }

    @Test
    @DisplayName("已有记录走 update 分支时，attributes 保持不动（本测试只锁定「不误伤」）")
    void existingRecordTakesUpdateBranch() {
        AssetEntity existing = new AssetEntity();
        existing.setId(7L);
        existing.setAssetCode("A-1");
        existing.setAttributes("{\"region\":\"cn\"}");
        when(assetMapper.selectOne(any())).thenReturn(existing);

        collector.collectFromProvider(healthyProviderReturning(dtoWith(
                Collections.singletonMap("region", "us"))));

        ArgumentCaptor<AssetEntity> captor = ArgumentCaptor.forClass(AssetEntity.class);
        verify(assetMapper).updateById(captor.capture());
        verify(assetMapper, never()).insert(any(AssetEntity.class));
        assertEquals("{\"region\":\"cn\"}", captor.getValue().getAttributes(),
                "update 分支当前不刷新 attributes，此断言只用于记录现状，不构成对该行为的认可");
    }
}
