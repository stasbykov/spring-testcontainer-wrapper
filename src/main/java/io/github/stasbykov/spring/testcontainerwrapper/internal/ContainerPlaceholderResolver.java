package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ContainerPlaceholderResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Logger log = LoggerFactory.getLogger(ContainerPlaceholderResolver.class);

    Map<String, String> resolve(ContainerDefinition definition, Map<String, StartedInfraContainer> dependencies) {
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : definition.properties().entrySet()) {
            resolved.put(entry.getKey(), resolveValue(definition, entry.getValue(), dependencies));
        }
        log.debug("Resolved properties for container '{}': {}", definition.id(), resolved.keySet());
        return Map.copyOf(resolved);
    }

    private String resolveValue(
            ContainerDefinition definition,
            String value,
            Map<String, StartedInfraContainer> dependencies
    ) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            int separatorIndex = placeholder.indexOf('.');
            if (separatorIndex <= 0 || separatorIndex == placeholder.length() - 1) {
                throw new IllegalStateException("Invalid placeholder '${" + placeholder + "}' for container '"
                        + definition.id() + "'. Expected ${containerId.attribute}");
            }
            String dependencyId = placeholder.substring(0, separatorIndex);
            String attributeName = placeholder.substring(separatorIndex + 1);
            StartedInfraContainer dependency = dependencies.get(dependencyId);
            if (dependency == null) {
                throw new IllegalStateException("Container '" + definition.id() + "' references unknown dependency '"
                        + dependencyId + "' in placeholder '${" + placeholder + "}'");
            }
            String replacement = dependency.attribute(attributeName);
            log.debug("Resolved placeholder '${{{}}}' for container '{}' from dependency '{}'", placeholder, definition.id(), dependencyId);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
