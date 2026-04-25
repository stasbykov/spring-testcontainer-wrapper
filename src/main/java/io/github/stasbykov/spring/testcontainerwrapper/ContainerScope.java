package io.github.stasbykov.spring.testcontainerwrapper;

/**
 * Defines how long a started container should live.
 */
public enum ContainerScope {
    /**
     * Creates a dedicated container per test class and stops it after the class finishes.
     */
    CLASS,

    /**
     * Reuses a container between test classes within the same JVM test run.
     */
    SHARED
}
