package io.github.stasbykov.spring.testcontainerwrapper.postgres;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.ContainerStartContext;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built-in provider that starts a {@link PostgreSQLContainer}.
 *
 * <p>Supported properties: {@code imageName}, {@code databaseName}, {@code username},
 * {@code password}, {@code reuse}.</p>
 */
public final class PostgreSqlContainerProvider implements InfraContainerProvider {

    private static final String DEFAULT_IMAGE = "postgres:16-alpine";
    private static final Logger log = LoggerFactory.getLogger(PostgreSqlContainerProvider.class);

    @Override
    public StartedInfraContainer start(ContainerStartContext context) {
        String imageName = context.properties().getOrDefault("imageName", DEFAULT_IMAGE);
        log.debug("Starting PostgreSQL container '{}' with image {}", context.id(), imageName);
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse(
                imageName
        ));
        if (context.properties().containsKey("databaseName")) {
            container.withDatabaseName(context.property("databaseName"));
        }
        if (context.properties().containsKey("username")) {
            container.withUsername(context.property("username"));
        }
        if (context.properties().containsKey("password")) {
            container.withPassword(context.property("password"));
        }
        if (context.properties().containsKey("reuse")) {
            container.withReuse(Boolean.parseBoolean(context.property("reuse")));
        }
        container.start();
        log.debug("PostgreSQL container '{}' started on {} with database '{}'", context.id(), container.getJdbcUrl(), container.getDatabaseName());

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("host", container.getHost());
        attributes.put("port", String.valueOf(container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
        attributes.put("jdbcUrl", container.getJdbcUrl());
        attributes.put("username", container.getUsername());
        attributes.put("password", container.getPassword());
        attributes.put("databaseName", container.getDatabaseName());
        return StartedInfraContainer.of(context.id(), context.scope(), container, attributes);
    }
}
