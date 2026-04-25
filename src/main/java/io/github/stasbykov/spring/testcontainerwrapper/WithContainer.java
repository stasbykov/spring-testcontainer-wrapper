package io.github.stasbykov.spring.testcontainerwrapper;

import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;
import io.github.stasbykov.spring.testcontainerwrapper.spi.NoOpDynamicPropertyRegistrar;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an infrastructure container that must be started for a Spring test class.
 *
 * <p>The annotation is repeatable, so a single test class may declare any number of containers.
 * Each declaration references a provider responsible for starting the container and, optionally,
 * a registrar that exposes container-derived values as Spring dynamic properties.</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithContainers.class)
public @interface WithContainer {

    /**
     * Logical container identifier unique within the annotated test class.
     */
    String id();

    /**
     * Provider implementation that creates and starts the container.
     */
    Class<? extends InfraContainerProvider> provider();

    /**
     * Lifecycle scope for the declared container.
     */
    ContainerScope scope() default ContainerScope.SHARED;

    /**
     * Container configuration in {@code key=value} format.
     */
    String[] properties() default {};

    /**
     * Container ids that must be started before this container.
     */
    String[] dependsOn() default {};

    /**
     * Strategy that registers Spring properties from the started container.
     */
    Class<? extends DynamicPropertyRegistrar> propertyRegistrar() default NoOpDynamicPropertyRegistrar.class;
}
