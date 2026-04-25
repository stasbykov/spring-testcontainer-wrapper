package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ContainerBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(ContainerBootstrapper.class);

    private final ContainerManager containerManager;
    private final ContainerPlaceholderResolver placeholderResolver;

    ContainerBootstrapper(ContainerManager containerManager, ContainerPlaceholderResolver placeholderResolver) {
        this.containerManager = containerManager;
        this.placeholderResolver = placeholderResolver;
    }

    ContainerBootstrapResult bootstrap(Class<?> testClass, List<ContainerDefinition> definitions) {
        log.debug("Starting container bootstrap for test class {} with {} definitions", testClass.getName(), definitions.size());
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
        log.debug("Finished container bootstrap for test class {}. Started containers: {}", testClass.getName(), startedById.keySet());
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
            log.debug("Container '{}' already started during current bootstrap for test class {}", definition.id(), testClass.getName());
            return existing;
        }
        if (!visiting.add(definition.id())) {
            throw new IllegalStateException("Circular container dependency detected for '" + definition.id() + "'");
        }
        log.debug("Bootstrapping container '{}' for test class {} with dependencies {}", definition.id(), testClass.getName(), definition.dependsOn());

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
        log.debug("Container '{}' is available for test class {}", definition.id(), testClass.getName());
        return started;
    }
}
