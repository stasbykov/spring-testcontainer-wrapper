package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.WithContainer;
import io.github.stasbykov.spring.testcontainerwrapper.support.RecordingInfraContainerProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.NestedTestConfiguration;

import java.util.List;

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

    @Test
    void inheritsContainersFromEnclosingClassForNestedTests() {
        List<ContainerDefinition> definitions = this.parser.parse(OuterWithNested.InnerInherit.class);

        assertEquals(List.of("db", "kafka"), definitions.stream().map(ContainerDefinition::id).toList());
    }

    @Test
    void nestedClassOverridesContainerWithSameId() {
        List<ContainerDefinition> definitions = this.parser.parse(OuterWithOverride.OverrideNested.class);

        assertEquals(List.of("kafka", "db"), definitions.stream().map(ContainerDefinition::id).toList());
        assertEquals("inner", definitions.stream()
                .filter(definition -> definition.id().equals("db"))
                .findFirst()
                .orElseThrow()
                .properties()
                .get("value"));
    }

    @Test
    void nestedOverrideModeDoesNotInheritContainersFromEnclosingClass() {
        List<ContainerDefinition> definitions = this.parser.parse(OuterWithOverrideMode.OverrideNested.class);

        assertEquals(List.of("kafka"), definitions.stream().map(ContainerDefinition::id).toList());
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

    @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class)
    private static final class OuterWithNested {

        @WithContainer(id = "kafka", provider = RecordingInfraContainerProvider.class)
        private final class InnerInherit {
        }
    }

    @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class, properties = {"value=outer"})
    private static final class OuterWithOverride {

        @WithContainer(id = "kafka", provider = RecordingInfraContainerProvider.class)
        @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class, properties = {"value=inner"})
        private final class OverrideNested {
        }
    }

    @WithContainer(id = "db", provider = RecordingInfraContainerProvider.class)
    private static final class OuterWithOverrideMode {

        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
        @WithContainer(id = "kafka", provider = RecordingInfraContainerProvider.class)
        private final class OverrideNested {
        }
    }
}
