package io.github.stasbykov.spring.testcontainerwrapper.spi;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * SPI for registering Spring properties derived from a started container.
 */
public interface DynamicPropertyRegistrar {

    /**
     * Registers properties in the provided Spring dynamic property registry.
     *
     * @param registry target Spring registry
     * @param container started container descriptor
     */
    void register(DynamicPropertyRegistry registry, StartedInfraContainer container);
}
