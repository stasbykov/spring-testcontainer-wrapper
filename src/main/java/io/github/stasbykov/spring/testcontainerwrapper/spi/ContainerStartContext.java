package io.github.stasbykov.spring.testcontainerwrapper.spi;

import io.github.stasbykov.spring.testcontainerwrapper.ContainerScope;
import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable input passed to an {@link InfraContainerProvider}.
 */
public final class ContainerStartContext {

    private final String id;
    private final ContainerScope scope;
    private final Class<?> testClass;
    private final Map<String, String> properties;
    private final Map<String, StartedInfraContainer> dependencies;

    public ContainerStartContext(
            String id,
            ContainerScope scope,
            Class<?> testClass,
            Map<String, String> properties,
            Map<String, StartedInfraContainer> dependencies
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.scope = Objects.requireNonNull(scope, "scope must not be null");
        this.testClass = Objects.requireNonNull(testClass, "testClass must not be null");
        this.properties = Map.copyOf(Objects.requireNonNull(properties, "properties must not be null"));
        this.dependencies = Map.copyOf(Objects.requireNonNull(dependencies, "dependencies must not be null"));
    }

    /**
     * Returns the logical container id.
     */
    public String id() {
        return this.id;
    }

    /**
     * Returns the lifecycle scope requested for the container.
     */
    public ContainerScope scope() {
        return this.scope;
    }

    /**
     * Returns the test class requesting the container.
     */
    public Class<?> testClass() {
        return this.testClass;
    }

    /**
     * Returns resolved container properties.
     */
    public Map<String, String> properties() {
        return this.properties;
    }

    /**
     * Returns a single resolved property or {@code null} when absent.
     */
    public String property(String name) {
        return this.properties.get(name);
    }

    /**
     * Returns started dependency containers indexed by id.
     */
    public Map<String, StartedInfraContainer> dependencies() {
        return this.dependencies;
    }

    /**
     * Returns a required started dependency.
     *
     * @param id dependency id
     * @return dependency container
     * @throws IllegalArgumentException when the dependency is unknown
     */
    public StartedInfraContainer dependency(String id) {
        StartedInfraContainer container = this.dependencies.get(id);
        if (container == null) {
            throw new IllegalArgumentException("Unknown dependency '" + id + "' for container '" + this.id + "'");
        }
        return container;
    }
}
