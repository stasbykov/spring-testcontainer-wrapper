package io.github.stasbykov.spring.testcontainerwrapper.spi;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;

public interface InfraContainerProvider {

    StartedInfraContainer start(ContainerStartContext context);
}
