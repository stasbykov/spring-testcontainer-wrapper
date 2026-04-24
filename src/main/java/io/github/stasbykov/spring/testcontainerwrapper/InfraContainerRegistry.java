package io.github.stasbykov.spring.testcontainerwrapper;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class InfraContainerRegistry {

    private final Map<String, StartedInfraContainer> containers;

    public InfraContainerRegistry(Map<String, StartedInfraContainer> containers) {
        this.containers = Map.copyOf(Objects.requireNonNull(containers, "containers must not be null"));
    }

    public StartedInfraContainer get(String id) {
        StartedInfraContainer container = this.containers.get(id);
        if (container == null) {
            throw new IllegalArgumentException("No container registered with id '" + id + "'");
        }
        return container;
    }

    public StartedInfraContainer getIfPresent(String id) {
        return this.containers.get(id);
    }

    public Set<String> ids() {
        return this.containers.keySet();
    }
}
