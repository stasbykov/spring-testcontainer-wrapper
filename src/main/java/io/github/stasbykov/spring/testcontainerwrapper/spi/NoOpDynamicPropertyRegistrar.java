package io.github.stasbykov.spring.testcontainerwrapper.spi;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Default no-op implementation used when a container does not need Spring property registration.
 */
public final class NoOpDynamicPropertyRegistrar implements DynamicPropertyRegistrar {

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
    }
}
