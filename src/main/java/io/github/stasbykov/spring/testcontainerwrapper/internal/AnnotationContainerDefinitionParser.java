package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.WithContainer;
import org.springframework.core.annotation.MergedAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AnnotationContainerDefinitionParser {

    private static final Logger log = LoggerFactory.getLogger(AnnotationContainerDefinitionParser.class);

    List<ContainerDefinition> parse(Class<?> testClass) {
        List<Class<?>> declarationClasses = resolveDeclarationClasses(testClass);
        Map<String, ContainerDefinition> definitionsById = new LinkedHashMap<>();
        for (Class<?> declarationClass : declarationClasses) {
            Map<String, ContainerDefinition> classDefinitions = parseClassDefinitions(testClass, declarationClass);
            for (ContainerDefinition definition : classDefinitions.values()) {
                if (definitionsById.containsKey(definition.id())) {
                    definitionsById.remove(definition.id());
                }
                definitionsById.put(definition.id(), definition);
            }
        }
        if (definitionsById.isEmpty()) {
            log.debug("No @WithContainer annotations found on test class {}", testClass.getName());
            return List.of();
        }
        validateDependencies(testClass, definitionsById);
        log.debug("Parsed container definitions for test class {} from declaration classes {}: {}",
                testClass.getName(), declarationClasses.stream().map(Class::getName).toList(), definitionsById.keySet());
        return List.copyOf(definitionsById.values());
    }

    private List<Class<?>> resolveDeclarationClasses(Class<?> testClass) {
        List<Class<?>> declarationClasses = new ArrayList<>();
        Class<?> current = testClass;
        declarationClasses.add(current);
        while (TestContextAnnotationUtils.searchEnclosingClass(current)) {
            current = current.getEnclosingClass();
            declarationClasses.add(current);
        }
        Collections.reverse(declarationClasses);
        return List.copyOf(declarationClasses);
    }

    private Map<String, ContainerDefinition> parseClassDefinitions(Class<?> testClass, Class<?> declarationClass) {
        List<WithContainer> annotations = MergedAnnotations.from(declarationClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .stream()
                .filter(annotation -> annotation.getType().equals(WithContainer.class))
                .map(annotation -> (WithContainer) annotation.synthesize())
                .toList();
        if (annotations.isEmpty()) {
            return Map.of();
        }
        log.debug("Found {} @WithContainer annotations on declaration class {} for test class {}",
                annotations.size(), declarationClass.getName(), testClass.getName());

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
                throw new IllegalStateException("Duplicate container id '" + id + "' on declaration class "
                        + declarationClass.getName() + " for test class " + testClass.getName());
            }
        }
        return definitionsById;
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
