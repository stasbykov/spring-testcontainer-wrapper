package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.ContainerScope;
import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.ContainerStartContext;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class ContainerManager {

    static final ContainerManager INSTANCE = new ContainerManager();
    private static final Logger log = LoggerFactory.getLogger(ContainerManager.class);

    private final ConcurrentMap<SharedContainerKey, CompletableFuture<ManagedContainer>> sharedContainers = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassContainerKey, CompletableFuture<ManagedContainer>> classContainers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Set<ClassContainerKey>> classOwnership = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

    StartedInfraContainer getOrStart(
            Class<?> testClass,
            ContainerDefinition definition,
            Map<String, String> resolvedProperties,
            Map<String, StartedInfraContainer> dependencies
    ) {
        return switch (definition.scope()) {
            case SHARED -> getOrStartShared(testClass, definition, resolvedProperties, dependencies);
            case CLASS -> getOrStartClass(testClass, definition, resolvedProperties, dependencies);
        };
    }

    void stopClassContainers(Class<?> testClass) {
        Set<ClassContainerKey> keys = this.classOwnership.remove(testClass);
        if (keys == null || keys.isEmpty()) {
            log.debug("No CLASS-scoped containers registered for test class {}", testClass.getName());
            return;
        }
        log.debug("Stopping {} CLASS-scoped containers for test class {}", keys.size(), testClass.getName());
        for (ClassContainerKey key : keys) {
            CompletableFuture<ManagedContainer> future = this.classContainers.remove(key);
            if (future == null) {
                continue;
            }
            ManagedContainer managedContainer = future.getNow(null);
            if (managedContainer != null) {
                log.debug("Stopping CLASS-scoped container '{}'", managedContainer.container().id());
                managedContainer.container().close();
            }
        }
    }

    void reset() {
        for (CompletableFuture<ManagedContainer> future : this.classContainers.values()) {
            ManagedContainer container = future.getNow(null);
            if (container != null) {
                container.container().close();
            }
        }
        this.classContainers.clear();
        this.classOwnership.clear();
        shutdownSharedContainers();
    }

    private StartedInfraContainer getOrStartShared(
            Class<?> testClass,
            ContainerDefinition definition,
            Map<String, String> resolvedProperties,
            Map<String, StartedInfraContainer> dependencies
    ) {
        registerShutdownHookOnce();
        SharedContainerKey key = new SharedContainerKey(definition.id(), definition.providerType(), resolvedProperties);
        CompletableFuture<ManagedContainer> future = new CompletableFuture<>();
        CompletableFuture<ManagedContainer> existing = this.sharedContainers.putIfAbsent(key, future);
        if (existing == null) {
            try {
                log.debug("Creating SHARED container '{}' for test class {}", definition.id(), testClass.getName());
                ManagedContainer container = createManagedContainer(testClass, definition, resolvedProperties, dependencies);
                future.complete(container);
                return container.container();
            } catch (Throwable ex) {
                this.sharedContainers.remove(key, future);
                future.completeExceptionally(ex);
                log.error("Failed to create SHARED container '{}' for test class {}", definition.id(), testClass.getName(), ex);
                throw ex;
            }
        }
        log.debug("Reusing SHARED container '{}' for test class {}", definition.id(), testClass.getName());
        return existing.join().container();
    }

    private StartedInfraContainer getOrStartClass(
            Class<?> testClass,
            ContainerDefinition definition,
            Map<String, String> resolvedProperties,
            Map<String, StartedInfraContainer> dependencies
    ) {
        ClassContainerKey key = new ClassContainerKey(testClass, definition.id(), definition.providerType(), resolvedProperties);
        CompletableFuture<ManagedContainer> future = new CompletableFuture<>();
        CompletableFuture<ManagedContainer> existing = this.classContainers.putIfAbsent(key, future);
        if (existing == null) {
            try {
                log.debug("Creating CLASS container '{}' for test class {}", definition.id(), testClass.getName());
                ManagedContainer container = createManagedContainer(testClass, definition, resolvedProperties, dependencies);
                this.classOwnership.computeIfAbsent(testClass, ignored -> ConcurrentHashMap.newKeySet()).add(key);
                future.complete(container);
                return container.container();
            } catch (Throwable ex) {
                this.classContainers.remove(key, future);
                future.completeExceptionally(ex);
                log.error("Failed to create CLASS container '{}' for test class {}", definition.id(), testClass.getName(), ex);
                throw ex;
            }
        }
        this.classOwnership.computeIfAbsent(testClass, ignored -> ConcurrentHashMap.newKeySet()).add(key);
        log.debug("Reusing CLASS container '{}' within test class {}", definition.id(), testClass.getName());
        return existing.join().container();
    }

    private ManagedContainer createManagedContainer(
            Class<?> testClass,
            ContainerDefinition definition,
            Map<String, String> resolvedProperties,
            Map<String, StartedInfraContainer> dependencies
    ) {
        InfraContainerProvider provider = BeanUtils.instantiateClass(definition.providerType());
        log.debug("Starting provider {} for container '{}' in scope {}", definition.providerType().getName(), definition.id(), definition.scope());
        StartedInfraContainer started = provider.start(new ContainerStartContext(
                definition.id(),
                definition.scope(),
                testClass,
                resolvedProperties,
                dependencies
        ));
        if (!Objects.equals(started.id(), definition.id())) {
            throw new IllegalStateException("Provider '" + definition.providerType().getName()
                    + "' returned container with id '" + started.id()
                    + "' but '" + definition.id() + "' was expected");
        }
        if (started.scope() != definition.scope()) {
            throw new IllegalStateException("Provider '" + definition.providerType().getName()
                    + "' returned scope '" + started.scope()
                    + "' but '" + definition.scope() + "' was expected");
        }
        return new ManagedContainer(started);
    }

    private void registerShutdownHookOnce() {
        if (!this.shutdownHookRegistered.compareAndSet(false, true)) {
            return;
        }
        log.debug("Registering JVM shutdown hook for SHARED containers");
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownSharedContainers, "infra-containers-shutdown"));
    }

    private void shutdownSharedContainers() {
        log.debug("Shutting down {} SHARED containers", this.sharedContainers.size());
        for (CompletableFuture<ManagedContainer> future : this.sharedContainers.values()) {
            ManagedContainer container = future.getNow(null);
            if (container != null) {
                log.debug("Stopping SHARED container '{}'", container.container().id());
                container.container().close();
            }
        }
        this.sharedContainers.clear();
    }

    record SharedContainerKey(
            String id,
            Class<? extends InfraContainerProvider> providerType,
            Map<String, String> resolvedProperties
    ) {
        SharedContainerKey {
            resolvedProperties = Map.copyOf(resolvedProperties);
        }
    }

    record ClassContainerKey(
            Class<?> testClass,
            String id,
            Class<? extends InfraContainerProvider> providerType,
            Map<String, String> resolvedProperties
    ) {
        ClassContainerKey {
            resolvedProperties = Map.copyOf(resolvedProperties);
        }
    }
}
