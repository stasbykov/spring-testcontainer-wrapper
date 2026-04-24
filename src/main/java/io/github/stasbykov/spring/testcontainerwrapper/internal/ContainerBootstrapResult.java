package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;

import java.util.List;
import java.util.Map;

record ContainerBootstrapResult(
        Map<String, StartedInfraContainer> containersById,
        List<RegisteredContainer> registrations
) {
}
