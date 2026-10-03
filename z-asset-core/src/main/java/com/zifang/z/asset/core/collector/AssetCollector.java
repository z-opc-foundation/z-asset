package com.zifang.z.asset.core.collector;

import com.zifang.z.asset.api.AssetProvider;
import com.zifang.z.asset.core.domain.entity.AssetEntity;
import com.zifang.z.asset.core.domain.mapper.AssetMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
                entity.setAttributes(dto.getAttributes() != null ? dto.getAttributes().toString() : null);
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
}
