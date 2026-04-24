package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.WithContainer;
import io.github.stasbykov.spring.testcontainerwrapper.support.RecordingInfraContainerProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotationContainerDefinitionParserTest {

    private final AnnotationContainerDefinitionParser parser = new AnnotationContainerDefinitionParser();

    @Test
    void parsesRepeatedAnnotations() {
        assertEquals(2, this.parser.parse(ValidContainers.class).size());
    }

    @Test
    void failsOnInvalidProperties() {
        assertThrows(IllegalStateException.class, () -> this.parser.parse(InvalidPropertyContainer.class));
    }

    @Test
    void failsOnDuplicateIds() {
        assertThrows(IllegalStateException.class, () -> this.parser.parse(DuplicateIds.class));
    }

    @Test
    void failsOnUnknownDependencies() {
        assertThrows(IllegalStateException.class, () -> this.parser.parse(UnknownDependency.class));
    }

    @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class)
    @WithContainer(id = "kafka", provider = RecordingInfraContainerProvider.class)
    private static final class ValidContainers {
    }

    @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class, properties = {"broken"})
    private static final class InvalidPropertyContainer {
    }

    @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class)
    @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class)
    private static final class DuplicateIds {
    }

    @WithContainer(id = "app", provider = RecordingInfraContainerProvider.class, dependsOn = {"missing"})
    private static final class UnknownDependency {
    }
}
