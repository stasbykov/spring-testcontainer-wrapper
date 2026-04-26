package io.github.stasbykov.spring.testcontainerwrapper;

import io.github.stasbykov.spring.testcontainerwrapper.support.RecordingDynamicPropertyRegistrar;
import io.github.stasbykov.spring.testcontainerwrapper.support.RecordingInfraContainerProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = NestedInfraContainersSpringBootIntegrationTest.TestApplication.class)
@WithContainer(
        id = "postgres",
        provider = RecordingInfraContainerProvider.class,
        scope = ContainerScope.SHARED,
        properties = {
                "attr.propertyName=it.jdbc-url",
                "attr.propertyValue=jdbc:postgresql://localhost:5432/nested"
        },
        propertyRegistrar = RecordingDynamicPropertyRegistrar.class
)
class NestedInfraContainersSpringBootIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private InfraContainerRegistry registry;

    @Nested
    class InheritedContainers {

        @Test
        void inheritsContainersFromEnclosingTestClass() {
            assertEquals("jdbc:postgresql://localhost:5432/nested", environment.getProperty("it.jdbc-url"));
            assertNotNull(registry.get("postgres"));
        }
    }

    @Nested
    @TestPropertySource(properties = "nested.flag=enabled")
    class NestedWithTestPropertySource {

        @Test
        void keepsInheritedContainersWhenNestedClassAddsTestProperties() {
            assertEquals("enabled", environment.getProperty("nested.flag"));
            assertEquals("jdbc:postgresql://localhost:5432/nested", environment.getProperty("it.jdbc-url"));
            assertNotNull(registry.get("postgres"));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
