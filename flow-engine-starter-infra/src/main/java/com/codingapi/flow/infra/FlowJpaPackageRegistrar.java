package com.codingapi.flow.infra;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class FlowJpaPackageRegistrar
        implements ImportBeanDefinitionRegistrar  {

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata metadata,
            BeanDefinitionRegistry registry) {
        AutoConfigurationPackages.register(registry, "com.codingapi.flow.infra");
    }
}