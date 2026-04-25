package io.github.stasbykov.spring.testcontainerwrapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation used by {@link WithContainer} to support repeatable declarations.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithContainers {

    /**
     * Wrapped repeatable {@link WithContainer} declarations.
     */
    WithContainer[] value();
}
