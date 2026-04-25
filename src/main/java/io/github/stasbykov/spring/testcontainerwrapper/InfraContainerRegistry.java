package io.github.stasbykov.spring.testcontainerwrapper;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only registry of containers started for the current Spring test context.
 *
 * <p>The registry is exposed as a Spring bean and can be injected into tests to fetch container
 * details such as mapped ports or derived URLs.</p>
 */
public final class InfraContainerRegistry {

    private final Map<String, StartedInfraContainer> containers;

    public InfraContainerRegistry(Map<String, StartedInfraContainer> containers) {
        this.containers = Map.copyOf(Objects.requireNonNull(containers, "containers must not be null"));
    }

    /**
     * Returns a container by id.
     *
     * @param id container id declared in {@link WithContainer}
     * @return matching container
     * @throws IllegalArgumentException when no container with such id exists
     */
    public StartedInfraContainer get(String id) {
        StartedInfraContainer container = this.containers.get(id);
        if (container == null) {
            throw new IllegalArgumentException("No container registered with id '" + id + "'");
        }
        return container;
    }

    /**
     * Returns a container by id or {@code null} when absent.
     */
    public StartedInfraContainer getIfPresent(String id) {
        return this.containers.get(id);
    }

    /**
     * Returns registered container ids.
     */
    public Set<String> ids() {
        return this.containers.keySet();
    }
}
