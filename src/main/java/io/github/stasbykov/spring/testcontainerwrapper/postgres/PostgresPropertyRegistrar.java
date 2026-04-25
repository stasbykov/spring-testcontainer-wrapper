package io.github.stasbykov.spring.testcontainerwrapper.postgres;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Registers standard Spring datasource properties from a PostgreSQL container.
 */
public final class PostgresPropertyRegistrar implements DynamicPropertyRegistrar {

    private static final Logger log = LoggerFactory.getLogger(PostgresPropertyRegistrar.class);

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
        log.debug("Registering PostgreSQL Spring properties from container '{}'", container.id());
        registry.add("spring.datasource.url", () -> container.attribute("jdbcUrl"));
        registry.add("spring.datasource.username", () -> container.attribute("username"));
        registry.add("spring.datasource.password", () -> container.attribute("password"));
    }
}
