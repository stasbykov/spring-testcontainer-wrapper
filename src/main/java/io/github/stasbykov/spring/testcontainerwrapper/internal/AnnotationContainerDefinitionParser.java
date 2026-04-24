package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.WithContainer;
import org.springframework.core.annotation.MergedAnnotations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AnnotationContainerDefinitionParser {

    List<ContainerDefinition> parse(Class<?> testClass) {
        List<WithContainer> annotations = MergedAnnotations.from(testClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .stream()
                .filter(annotation -> annotation.getType().equals(WithContainer.class))
                .map(annotation -> (WithContainer) annotation.synthesize())
                .toList();
        if (annotations.isEmpty()) {
            return List.of();
        }

        Map<String, ContainerDefinition> definitionsById = new LinkedHashMap<>();
        for (WithContainer annotation : annotations) {
            String id = annotation.id().trim();
            if (id.isEmpty()) {
                throw new IllegalStateException("Container id must not be blank on test class " + testClass.getName());
            }
            ContainerDefinition definition = new ContainerDefinition(
                    id,
                    annotation.provider(),
                    annotation.scope(),
                    parseProperties(testClass, id, annotation.properties()),
                    parseDependencies(annotation.dependsOn()),
                    annotation.propertyRegistrar()
            );
            ContainerDefinition previous = definitionsById.putIfAbsent(id, definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate container id '" + id + "' on test class " + testClass.getName());
            }
        }
        validateDependencies(testClass, definitionsById);
        return List.copyOf(definitionsById.values());
    }

    private Map<String, String> parseProperties(Class<?> testClass, String id, String[] rawProperties) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (String property : rawProperties) {
            int separatorIndex = property.indexOf('=');
            if (separatorIndex <= 0) {
                throw new IllegalStateException("Invalid property '" + property + "' for container '" + id
                        + "' on test class " + testClass.getName() + ". Expected key=value");
            }
            String key = property.substring(0, separatorIndex).trim();
            String value = property.substring(separatorIndex + 1).trim();
            if (key.isEmpty()) {
                throw new IllegalStateException("Invalid property '" + property + "' for container '" + id
                        + "' on test class " + testClass.getName() + ". Key must not be blank");
            }
            properties.put(key, value);
        }
        return Map.copyOf(properties);
    }

    private List<String> parseDependencies(String[] rawDependencies) {
        List<String> dependencies = new ArrayList<>(rawDependencies.length);
        for (String dependency : rawDependencies) {
            String id = dependency.trim();
            if (!id.isEmpty()) {
                dependencies.add(id);
            }
        }
        return List.copyOf(dependencies);
    }

    private void validateDependencies(Class<?> testClass, Map<String, ContainerDefinition> definitionsById) {
        for (ContainerDefinition definition : definitionsById.values()) {
            for (String dependencyId : definition.dependsOn()) {
                if (!definitionsById.containsKey(dependencyId)) {
                    throw new IllegalStateException("Container '" + definition.id() + "' on test class "
                            + testClass.getName() + " depends on unknown container '" + dependencyId + "'");
                }
            }
        }
    }
}
