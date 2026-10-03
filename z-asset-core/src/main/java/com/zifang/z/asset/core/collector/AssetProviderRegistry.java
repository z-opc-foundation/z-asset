package com.zifang.z.asset.core.collector;

import com.zifang.z.asset.api.AssetProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AssetProviderRegistry {

    private final List<AssetProvider> providers = new CopyOnWriteArrayList<>();

    public void register(AssetProvider provider) {
        providers.add(provider);
    }

    public List<AssetProvider> getAllProviders() {
        return providers;
    }

    public List<AssetProvider> getProvidersByModule(String module) {
        List<AssetProvider> result = new java.util.ArrayList<>();
        for (AssetProvider p : providers) {
            if (p.getModule().equals(module)) {
                result.add(p);
            }
        }
        return result;
    }

    public AssetProvider getProvider(String module, String assetType) {
        for (AssetProvider p : providers) {
            if (p.getModule().equals(module) && p.getAssetType().equals(assetType)) {
                return p;
            }
        }
        return null;
    }
}
