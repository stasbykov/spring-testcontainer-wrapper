package io.github.stasbykov.spring.testcontainerwrapper.postgres;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class PostgresPropertyRegistrar implements DynamicPropertyRegistrar {

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
        registry.add("spring.datasource.url", () -> container.attribute("jdbcUrl"));
        registry.add("spring.datasource.username", () -> container.attribute("username"));
        registry.add("spring.datasource.password", () -> container.attribute("password"));
    }
}
