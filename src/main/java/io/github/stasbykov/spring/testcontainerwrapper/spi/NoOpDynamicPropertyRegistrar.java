package io.github.stasbykov.spring.testcontainerwrapper.spi;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class NoOpDynamicPropertyRegistrar implements DynamicPropertyRegistrar {

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
    }
}
