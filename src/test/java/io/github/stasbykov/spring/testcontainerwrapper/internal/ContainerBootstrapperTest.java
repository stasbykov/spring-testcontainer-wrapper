package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.ContainerScope;
import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.NoOpDynamicPropertyRegistrar;
import io.github.stasbykov.spring.testcontainerwrapper.support.RecordingInfraContainerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContainerBootstrapperTest {

    private final ContainerBootstrapper bootstrapper =
            new ContainerBootstrapper(ContainerManager.INSTANCE, new ContainerPlaceholderResolver());

    @BeforeEach
    void setUp() {
        ContainerManager.INSTANCE.reset();
        RecordingInfraContainerProvider.reset();
    }

    @AfterEach
    void tearDown() {
        ContainerManager.INSTANCE.reset();
        RecordingInfraContainerProvider.reset();
    }

    @Test
    void resolvesDependencyPlaceholdersBeforeContainerStart() {
        List<ContainerDefinition> definitions = List.of(
                new ContainerDefinition(
                        "postgres",
                        RecordingInfraContainerProvider.class,
                        ContainerScope.SHARED,
                        Map.of(
                                "attr.jdbcUrl", "jdbc:postgresql://localhost:5432/orders",
                                "attr.username", "orders_user"
                        ),
                        List.of(),
                        NoOpDynamicPropertyRegistrar.class
                ),
                new ContainerDefinition(
                        "service",
                        RecordingInfraContainerProvider.class,
                        ContainerScope.CLASS,
                        Map.of(
                                "attr.resolvedJdbcUrl", "${postgres.jdbcUrl}",
                                "attr.resolvedUsername", "${postgres.username}"
                        ),
                        List.of("postgres"),
                        NoOpDynamicPropertyRegistrar.class
                )
        );

        ContainerBootstrapResult result = this.bootstrapper.bootstrap(TestClassA.class, definitions);

        StartedInfraContainer service = result.containersById().get("service");
        assertEquals("jdbc:postgresql://localhost:5432/orders", service.attribute("resolvedJdbcUrl"));
        assertEquals("orders_user", service.attribute("resolvedUsername"));
        assertEquals(2, RecordingInfraContainerProvider.startCount());
    }

    @Test
    void reusesSharedContainersAcrossParallelBootstraps() throws ExecutionException, InterruptedException {
        List<ContainerDefinition> definitions = List.of(
                new ContainerDefinition(
                        "kafka",
                        RecordingInfraContainerProvider.class,
                        ContainerScope.SHARED,
                        Map.of(
                                "startupDelayMs", "100",
                                "attr.bootstrapServers", "localhost:9092"
                        ),
                        List.of(),
                        NoOpDynamicPropertyRegistrar.class
                )
        );

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<StartedInfraContainer> taskA = () -> this.bootstrapper.bootstrap(TestClassA.class, definitions)
                    .containersById()
                    .get("kafka");
            Callable<StartedInfraContainer> taskB = () -> this.bootstrapper.bootstrap(TestClassB.class, definitions)
                    .containersById()
                    .get("kafka");

            StartedInfraContainer containerA = executor.submit(taskA).get();
            StartedInfraContainer containerB = executor.submit(taskB).get();

            assertSame(containerA, containerB);
            assertEquals(1, RecordingInfraContainerProvider.startCount());
        }
    }

    @Test
    void stopsClassScopedContainersOnManagerRequest() {
        List<ContainerDefinition> definitions = List.of(
                new ContainerDefinition(
                        "postgres",
                        RecordingInfraContainerProvider.class,
                        ContainerScope.CLASS,
                        Map.of("attr.jdbcUrl", "jdbc:test"),
                        List.of(),
                        NoOpDynamicPropertyRegistrar.class
                )
        );

        this.bootstrapper.bootstrap(TestClassA.class, definitions);
        assertEquals(1, RecordingInfraContainerProvider.startCount());
        assertEquals(0, RecordingInfraContainerProvider.closeCount());

        ContainerManager.INSTANCE.stopClassContainers(TestClassA.class);

        assertEquals(1, RecordingInfraContainerProvider.closeCount());
    }

    @Test
    void failsOnCircularDependencies() {
        List<ContainerDefinition> definitions = List.of(
                new ContainerDefinition(
                        "a",
                        RecordingInfraContainerProvider.class,
                        ContainerScope.CLASS,
                        Map.of(),
                        List.of("b"),
                        NoOpDynamicPropertyRegistrar.class
                ),
                new ContainerDefinition(
                        "b",
                        RecordingInfraContainerProvider.class,
                        ContainerScope.CLASS,
                        Map.of(),
                        List.of("a"),
                        NoOpDynamicPropertyRegistrar.class
                )
        );

        assertThrows(IllegalStateException.class, () -> this.bootstrapper.bootstrap(TestClassA.class, definitions));
    }

    private static final class TestClassA {
    }

    private static final class TestClassB {
    }
}
