package io.github.stasbykov.spring.testcontainerwrapper.spi;

import io.github.stasbykov.spring.testcontainerwrapper.ContainerScope;
import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;

import java.util.Map;
import java.util.Objects;

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

    public String id() {
        return this.id;
    }

    public ContainerScope scope() {
        return this.scope;
    }

    public Class<?> testClass() {
        return this.testClass;
    }

    public Map<String, String> properties() {
        return this.properties;
    }

    public String property(String name) {
        return this.properties.get(name);
    }

    public Map<String, StartedInfraContainer> dependencies() {
        return this.dependencies;
    }

    public StartedInfraContainer dependency(String id) {
        StartedInfraContainer container = this.dependencies.get(id);
        if (container == null) {
            throw new IllegalArgumentException("Unknown dependency '" + id + "' for container '" + this.id + "'");
        }
        return container;
    }
}
