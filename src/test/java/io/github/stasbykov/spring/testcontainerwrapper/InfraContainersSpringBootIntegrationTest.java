package io.github.stasbykov.spring.testcontainerwrapper;

import io.github.stasbykov.spring.testcontainerwrapper.support.RecordingDynamicPropertyRegistrar;
import io.github.stasbykov.spring.testcontainerwrapper.support.RecordingInfraContainerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = InfraContainersSpringBootIntegrationTest.TestApplication.class)
@WithContainer(
        id = "postgres",
        provider = RecordingInfraContainerProvider.class,
        scope = ContainerScope.SHARED,
        properties = {
                "attr.jdbcUrl=jdbc:postgresql://localhost:5432/orders",
                "attr.username=orders_user"
        }
)
@WithContainer(
        id = "service",
        provider = RecordingInfraContainerProvider.class,
        scope = ContainerScope.CLASS,
        dependsOn = {"postgres"},
        properties = {
                "attr.propertyName=it.service.jdbc-url",
                "attr.propertyValue=${postgres.jdbcUrl}",
                "attr.sourceUser=${postgres.username}"
        },
        propertyRegistrar = RecordingDynamicPropertyRegistrar.class
)
class InfraContainersSpringBootIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private InfraContainerRegistry registry;

    @BeforeEach
    void setUp() {
        RecordingInfraContainerProvider.reset();
    }

    @Test
    void registersContainersAndDynamicPropertiesInSpringContext() {
        assertEquals("jdbc:postgresql://localhost:5432/orders", this.environment.getProperty("it.service.jdbc-url"));
        assertNotNull(this.registry.get("postgres"));
        assertNotNull(this.registry.get("service"));
        assertEquals("orders_user", this.registry.get("service").attribute("sourceUser"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
