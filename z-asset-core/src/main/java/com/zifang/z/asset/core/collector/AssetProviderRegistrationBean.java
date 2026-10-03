package com.zifang.z.asset.core.collector;

import com.zifang.z.asset.api.AssetProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class AssetProviderRegistrationBean implements BeanPostProcessor {

    private final AssetProviderRegistry registry;

    public AssetProviderRegistrationBean(AssetProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AssetProvider) {
            registry.register((AssetProvider) bean);
        }
        return bean;
    }
}
