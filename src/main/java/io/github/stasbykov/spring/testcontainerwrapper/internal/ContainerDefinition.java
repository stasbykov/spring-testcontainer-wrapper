package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.ContainerScope;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;

import java.util.List;
import java.util.Map;

public record ContainerDefinition(
        String id,
        Class<? extends InfraContainerProvider> providerType,
        ContainerScope scope,
        Map<String, String> properties,
        List<String> dependsOn,
        Class<? extends DynamicPropertyRegistrar> propertyRegistrarType
) {
}
