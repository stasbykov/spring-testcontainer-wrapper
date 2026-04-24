package io.github.stasbykov.spring.testcontainerwrapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EmptyInfraContainerRegistryIntegrationTest.TestApplication.class)
class EmptyInfraContainerRegistryIntegrationTest {

    @Autowired
    private InfraContainerRegistry infraContainerRegistry;

    @Test
    void registersEmptyRegistryEvenWithoutContainerAnnotations() {
        assertNotNull(this.infraContainerRegistry);
        assertTrue(this.infraContainerRegistry.ids().isEmpty());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
