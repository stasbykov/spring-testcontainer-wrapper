# spring-testcontainer-wrapper

`spring-testcontainer-wrapper` is a test library for Spring Boot + JUnit 5 that lets you declare Testcontainers infrastructure directly on a test class without using a shared base test class.

## Why

A common Testcontainers setup in Spring projects is a large base test class with all containers defined in one place. This approach causes several problems:

- tests start containers they do not need;
- the base class grows over time and becomes hard to maintain;
- test classes lose the ability to extend another class;
- container lifecycle is scattered across inheritance instead of being explicit on the test itself.

This library replaces that model with declarative container definitions using annotations and Spring TestContext integration.

## Current Version

The current version supports:

- Spring Boot `3.5.4`
- JUnit 5
- Java `21`
- `SHARED` and `CLASS` container scopes
- container dependencies via `dependsOn`
- placeholder resolution like `${postgres.jdbcUrl}`
- Spring dynamic property registration
- built-in providers for PostgreSQL and Kafka
- runtime access to started containers through `InfraContainerRegistry`

In the first version, the tested application itself is expected to run in-process through `@SpringBootTest`.

## Dependency

Add the library as a test dependency:

```xml
<dependency>
    <groupId>io.github.stasbykov</groupId>
    <artifactId>spring-testcontainer-wrapper</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Quick Start

```java
import io.github.stasbykov.spring.testcontainerwrapper.ContainerScope;
import io.github.stasbykov.spring.testcontainerwrapper.WithContainer;
import io.github.stasbykov.spring.testcontainerwrapper.kafka.KafkaContainerProvider;
import io.github.stasbykov.spring.testcontainerwrapper.kafka.KafkaPropertyRegistrar;
import io.github.stasbykov.spring.testcontainerwrapper.postgres.PostgreSqlContainerProvider;
import io.github.stasbykov.spring.testcontainerwrapper.postgres.PostgresPropertyRegistrar;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@WithContainer(
        id = "postgres",
        provider = PostgreSqlContainerProvider.class,
        scope = ContainerScope.SHARED,
        properties = {
                "databaseName=orders",
                "username=orders_user",
                "password=orders_pass"
        },
        propertyRegistrar = PostgresPropertyRegistrar.class
)
@WithContainer(
        id = "kafka",
        provider = KafkaContainerProvider.class,
        scope = ContainerScope.SHARED,
        propertyRegistrar = KafkaPropertyRegistrar.class
)
class OrderServiceTest {
}
```

This starts only the containers declared on the test class and registers:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.kafka.bootstrap-servers`

## Scopes

### `ContainerScope.SHARED`

The container is reused across test classes within the same JVM test run.

Use it for expensive infrastructure that should be shared:

- PostgreSQL
- Kafka
- Redis

### `ContainerScope.CLASS`

The container belongs to one test class and is stopped after that class finishes.

Use it for:

- isolated per-class environments;
- containers with test-specific state;
- cases where Spring context reuse must not leak infrastructure state.

## Dependencies Between Containers

You can declare startup dependencies and reference attributes from already started containers.

```java
@SpringBootTest
@WithContainer(
        id = "postgres",
        provider = PostgreSqlContainerProvider.class,
        scope = ContainerScope.SHARED,
        properties = {
                "databaseName=orders",
                "username=orders_user",
                "password=orders_pass"
        }
)
@WithContainer(
        id = "app-db-config",
        provider = CustomProvider.class,
        scope = ContainerScope.CLASS,
        dependsOn = {"postgres"},
        properties = {
                "attr.jdbcUrl=${postgres.jdbcUrl}",
                "attr.username=${postgres.username}",
                "attr.password=${postgres.password}"
        }
)
class OrderServiceTest {
}
```

Placeholders use the format:

```text
${containerId.attribute}
```

## Runtime Access to Started Containers

The library registers `InfraContainerRegistry` as a Spring bean for Spring test contexts.
If no `@WithContainer` annotations are declared, the registry is still available and will be empty.

```java
import io.github.stasbykov.spring.testcontainerwrapper.InfraContainerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

class OrderServiceTest {

    @Autowired
    private InfraContainerRegistry infraContainerRegistry;

    // example usage:
    // infraContainerRegistry.get("postgres").attribute("jdbcUrl");
}
```

## Built-in Providers

### PostgreSQL

Provider:

```java
io.github.stasbykov.spring.testcontainerwrapper.postgres.PostgreSqlContainerProvider
```

Supported properties:

- `imageName`
- `databaseName`
- `username`
- `password`
- `reuse`

Exported attributes:

- `host`
- `port`
- `jdbcUrl`
- `username`
- `password`
- `databaseName`

Property registrar:

```java
io.github.stasbykov.spring.testcontainerwrapper.postgres.PostgresPropertyRegistrar
```

### Kafka

Provider:

```java
io.github.stasbykov.spring.testcontainerwrapper.kafka.KafkaContainerProvider
```

Supported properties:

- `imageName`
- `reuse`

Exported attributes:

- `host`
- `port`
- `bootstrapServers`

Property registrar:

```java
io.github.stasbykov.spring.testcontainerwrapper.kafka.KafkaPropertyRegistrar
```

## Writing a Custom Provider

Implement `InfraContainerProvider` and return `StartedInfraContainer`.

```java
import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.ContainerStartContext;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;

import java.util.Map;

public final class FakeServiceProvider implements InfraContainerProvider {

    @Override
    public StartedInfraContainer start(ContainerStartContext context) {
        FakeService service = new FakeService(context.property("baseUrl"));
        service.start();

        return StartedInfraContainer.of(
                context.id(),
                context.scope(),
                service,
                Map.of(
                        "baseUrl", service.baseUrl(),
                        "healthUrl", service.baseUrl() + "/health"
                ),
                service::stop
        );
    }
}
```

If you want to register Spring properties, implement `DynamicPropertyRegistrar`:

```java
import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class FakeServicePropertyRegistrar implements DynamicPropertyRegistrar {

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
        registry.add("external.fake-service.base-url", () -> container.attribute("baseUrl"));
    }
}
```

## How It Works

- `@WithContainer` declarations are read from the test class.
- container definitions are validated before Spring context startup.
- dependencies are resolved and started in the correct order.
- placeholders are resolved from started dependency attributes.
- dynamic properties are added to the Spring `Environment`.
- `CLASS` containers are stopped after the test class.
- `SHARED` containers live for the current JVM test run.

## Current Limitations

- no built-in Dockerized tested-application provider yet;
- no meta-annotations yet;
- no built-in Redis/LocalStack/GenericContainer providers yet;
- the tested Spring Boot application is expected to run in the same JVM via `@SpringBootTest`.

## Tests

Run the test suite:

```bash
mvn test
```

## Russian Documentation

Russian documentation is available in [README.ru.md](README.ru.md).
