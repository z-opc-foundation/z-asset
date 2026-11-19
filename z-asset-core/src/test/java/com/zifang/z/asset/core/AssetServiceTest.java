package com.zifang.z.asset.core;

import com.zifang.z.asset.api.enums.AssetLifecycle;
import com.zifang.z.asset.core.domain.entity.ZAssetDO;
import com.zifang.z.asset.core.domain.mapper.ZAssetMapper;
import com.zifang.z.asset.core.service.AssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资产流转校验的自守卫：不合法的流转一律拒掉，且一库不写。
 */
class AssetServiceTest {

    private ZAssetMapper mapper;
    private AssetService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ZAssetMapper.class);
        service = new AssetService(mapper);
    }

    private static ZAssetDO asset(String code, String status) {
        ZAssetDO asset = new ZAssetDO();
        asset.setId(1L);
        asset.setAssetCode(code);
        asset.setAssetName("测试资产");
        asset.setAssetCategory("PHYSICAL");
        asset.setStatus(status);
        return asset;
    }

    @Test
    @DisplayName("合法流转：更新状态与责任人并落库")
    void legalTransitionUpdatesAsset() {
        when(mapper.selectOne(any())).thenReturn(asset("A-1", AssetLifecycle.IN_STOCK.name()));

        service.transition("A-1", AssetLifecycle.ALLOCATED, "张三", "领用");

        ArgumentCaptor<ZAssetDO> captor = ArgumentCaptor.forClass(ZAssetDO.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(AssetLifecycle.ALLOCATED.name(), captor.getValue().getStatus());
        assertEquals("张三", captor.getValue().getOwner());
    }

    @Test
    @DisplayName("非法流转：拒绝且一库不写")
    void illegalTransitionIsRejected() {
        when(mapper.selectOne(any())).thenReturn(asset("A-2", AssetLifecycle.IN_STOCK.name()));

        assertThrows(IllegalStateException.class,
                () -> service.transition("A-2", AssetLifecycle.IN_USE, "张三", null));
        verify(mapper, never()).updateById(any(ZAssetDO.class));
    }

    @Test
    @DisplayName("领用与启用必须指定责任人")
    void allocationRequiresOwner() {
        when(mapper.selectOne(any())).thenReturn(asset("A-3", AssetLifecycle.IN_STOCK.name()));

        assertThrows(IllegalStateException.class,
                () -> service.transition("A-3", AssetLifecycle.ALLOCATED, null, null));
        verify(mapper, never()).updateById(any(ZAssetDO.class));
    }

    @Test
    @DisplayName("资产不存在：报错而不是静默跳过")
    void missingAssetIsRejected() {
        when(mapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.transition("NOPE", AssetLifecycle.IN_STOCK, null, null));
        verify(mapper, never()).updateById(any(ZAssetDO.class));
    }
}
