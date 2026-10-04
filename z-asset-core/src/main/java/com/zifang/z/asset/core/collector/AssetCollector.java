package com.zifang.z.asset.core.collector;

import com.zifang.z.asset.api.AssetProvider;
import com.zifang.z.asset.core.domain.entity.AssetEntity;
import com.zifang.z.asset.core.domain.mapper.AssetMapper;
import com.zifang.util.json.JsonUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class AssetCollector {

    private final AssetProviderRegistry registry;
    private final AssetMapper assetMapper;

    public AssetCollector(AssetProviderRegistry registry, AssetMapper assetMapper) {
        this.registry = registry;
        this.assetMapper = assetMapper;
    }

    @Transactional
    public void collectAll() {
        for (AssetProvider provider : registry.getAllProviders()) {
            collectFromProvider(provider);
        }
    }

    @Transactional
    public void collectFromProvider(AssetProvider provider) {
        if (!provider.isHealthy()) {
            return;
        }

        List<AssetProvider.AssetDTO> assets = provider.listAssets();
        if (assets == null) {
            return;
        }

        for (AssetProvider.AssetDTO dto : assets) {
            AssetEntity entity = assetMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssetEntity>()
                            .eq(AssetEntity::getAssetCode, dto.getAssetCode())
            );
            if (entity == null) {
                entity = new AssetEntity();
                entity.setAssetCode(dto.getAssetCode());
                entity.setAssetName(dto.getAssetName());
                entity.setAssetType(dto.getAssetType());
                entity.setModule(dto.getModule());
                entity.setStatus(dto.getStatus());
                entity.setEndpoint(dto.getEndpoint());
                entity.setSpec(dto.getSpec());
                entity.setAttributes(toAttributesJson(dto.getAttributes()));
                entity.setTenantCode(dto.getTenantCode());
                entity.setDomainCode(dto.getDomainCode());
                entity.setCollectedAt(LocalDateTime.now());
                entity.setCreatedTime(LocalDateTime.now());
                entity.setUpdatedTime(LocalDateTime.now());
                assetMapper.insert(entity);
            } else {
                entity.setAssetName(dto.getAssetName());
                entity.setStatus(dto.getStatus());
                entity.setEndpoint(dto.getEndpoint());
                entity.setSpec(dto.getSpec());
                entity.setCollectedAt(LocalDateTime.now());
                entity.setUpdatedTime(LocalDateTime.now());
                assetMapper.updateById(entity);
            }
        }
    }

    /**
     * 把 provider 给的 attributes Map 序列化成 <b>JSON</b>。
     * <p>{@code z_asset.attributes} 的字段注释是"属性信息（JSON格式）"，而原实现写的是
     * {@code map.toString()}——Java 的 {@code {k=v, k2=v2}}，不是 JSON。</p>
     * <p>这不只是"格式不好看"：值里只要含 {@code ,} 或 {@code =}（连接串、query、表达式
     * 都是常见值），{@code {}}  伪格式就没有任何无歧义的解析方式，下游按 JSON 读必然失败。</p>
     */
    private static String toAttributesJson(Map<String, String> attributes) {
        return attributes == null ? null : JsonUtil.toJson(attributes);
    }
}
