package io.github.stasbykov.spring.testcontainerwrapper.spi;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;

/**
 * SPI for starting a concrete infrastructure container.
 */
public interface InfraContainerProvider {

    /**
     * Starts the container described by the provided context.
     *
     * @param context resolved container start context
     * @return started container descriptor
     */
    StartedInfraContainer start(ContainerStartContext context);
}
