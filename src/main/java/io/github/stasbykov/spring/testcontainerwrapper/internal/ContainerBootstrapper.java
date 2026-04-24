package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ContainerBootstrapper {

    private final ContainerManager containerManager;
    private final ContainerPlaceholderResolver placeholderResolver;

    ContainerBootstrapper(ContainerManager containerManager, ContainerPlaceholderResolver placeholderResolver) {
        this.containerManager = containerManager;
        this.placeholderResolver = placeholderResolver;
    }

    ContainerBootstrapResult bootstrap(Class<?> testClass, List<ContainerDefinition> definitions) {
        Map<String, ContainerDefinition> definitionsById = new LinkedHashMap<>();
        for (ContainerDefinition definition : definitions) {
            definitionsById.put(definition.id(), definition);
        }

        Map<String, StartedInfraContainer> startedById = new LinkedHashMap<>();
        List<RegisteredContainer> registrations = new ArrayList<>();
        Set<String> visiting = new java.util.HashSet<>();

        for (ContainerDefinition definition : definitions) {
            start(testClass, definition, definitionsById, startedById, registrations, visiting);
        }
        return new ContainerBootstrapResult(Map.copyOf(startedById), List.copyOf(registrations));
    }

    private StartedInfraContainer start(
            Class<?> testClass,
            ContainerDefinition definition,
            Map<String, ContainerDefinition> definitionsById,
            Map<String, StartedInfraContainer> startedById,
            List<RegisteredContainer> registrations,
            Set<String> visiting
    ) {
        StartedInfraContainer existing = startedById.get(definition.id());
        if (existing != null) {
            return existing;
        }
        if (!visiting.add(definition.id())) {
            throw new IllegalStateException("Circular container dependency detected for '" + definition.id() + "'");
        }

        Map<String, StartedInfraContainer> dependencies = new LinkedHashMap<>();
        for (String dependencyId : definition.dependsOn()) {
            ContainerDefinition dependency = definitionsById.get(dependencyId);
            if (dependency == null) {
                throw new IllegalStateException("Unknown dependency '" + dependencyId + "' for container '" + definition.id() + "'");
            }
            dependencies.put(dependencyId, start(testClass, dependency, definitionsById, startedById, registrations, visiting));
        }

        Map<String, String> resolvedProperties = this.placeholderResolver.resolve(definition, dependencies);
        StartedInfraContainer started = this.containerManager.getOrStart(testClass, definition, resolvedProperties, dependencies);
        startedById.put(definition.id(), started);
        registrations.add(new RegisteredContainer(definition, started));
        visiting.remove(definition.id());
        return started;
    }
}
