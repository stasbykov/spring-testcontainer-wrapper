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

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithContainers.class)
public @interface WithContainer {

    String id();

    Class<? extends InfraContainerProvider> provider();

    ContainerScope scope() default ContainerScope.SHARED;

    String[] properties() default {};

    String[] dependsOn() default {};

    Class<? extends DynamicPropertyRegistrar> propertyRegistrar() default NoOpDynamicPropertyRegistrar.class;
}
