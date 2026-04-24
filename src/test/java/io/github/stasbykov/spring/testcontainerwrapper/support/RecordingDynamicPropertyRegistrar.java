package io.github.stasbykov.spring.testcontainerwrapper.support;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class RecordingDynamicPropertyRegistrar implements DynamicPropertyRegistrar {

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
        registry.add(container.attribute("propertyName"), () -> container.attribute("propertyValue"));
    }
}
