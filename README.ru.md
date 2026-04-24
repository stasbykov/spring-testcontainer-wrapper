# spring-testcontainer-wrapper

`spring-testcontainer-wrapper` — библиотека для тестов на Spring Boot + JUnit 5, которая позволяет декларативно подключать Testcontainers-инфраструктуру прямо на тестовом классе без общего базового класса.

## Зачем

Частый подход в Spring-проектах — сделать большой базовый тестовый класс, в котором объявлены все контейнеры. У этого подхода есть несколько проблем:

- тесты поднимают контейнеры, которые им не нужны;
- базовый класс со временем разрастается и становится неудобным;
- тестовые классы теряют возможность наследоваться от другого класса;
- lifecycle контейнеров скрыт в наследовании, а не описан явно на самом тесте.

Эта библиотека заменяет такой подход на декларативное описание контейнеров через аннотации и интеграцию со Spring TestContext.

## Что есть в MVP

Текущая версия поддерживает:

- Spring Boot `3.5.4`
- JUnit 5
- Java `21`
- scope контейнеров `SHARED` и `CLASS`
- зависимости между контейнерами через `dependsOn`
- placeholder-подстановки вида `${postgres.jdbcUrl}`
- регистрацию dynamic properties в Spring
- встроенные провайдеры для PostgreSQL и Kafka
- доступ к запущенным контейнерам через `InfraContainerRegistry`

В MVP тестируемое приложение предполагается запускать в том же JVM через `@SpringBootTest`.

## Подключение

Добавьте библиотеку как test dependency:

```xml
<dependency>
    <groupId>io.github.stasbykov</groupId>
    <artifactId>spring-testcontainer-wrapper</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Быстрый старт

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

Такой тест поднимет только те контейнеры, которые объявлены на самом классе, и зарегистрирует:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.kafka.bootstrap-servers`

## Scopes

### `ContainerScope.SHARED`

Контейнер переиспользуется между тестовыми классами в рамках одного JVM test run.

Подходит для дорогой общей инфраструктуры:

- PostgreSQL
- Kafka
- Redis

### `ContainerScope.CLASS`

Контейнер принадлежит одному тестовому классу и останавливается после завершения этого класса.

Подходит для:

- полностью изолированных окружений на класс;
- контейнеров с тест-специфичным состоянием;
- случаев, когда переиспользование Spring context не должно протаскивать состояние инфраструктуры.

## Зависимости между контейнерами

Можно задавать порядок старта и ссылаться на атрибуты уже поднятых контейнеров.

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

Формат placeholder:

```text
${containerId.attribute}
```

## Доступ к контейнерам во время теста

Библиотека регистрирует `InfraContainerRegistry` как Spring bean в тестовом Spring context.
Если на классе нет ни одного `@WithContainer`, реестр все равно будет доступен, но окажется пустым.

```java
import io.github.stasbykov.spring.testcontainerwrapper.InfraContainerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

class OrderServiceTest {

    @Autowired
    private InfraContainerRegistry infraContainerRegistry;

    // пример:
    // infraContainerRegistry.get("postgres").attribute("jdbcUrl");
}
```

## Встроенные провайдеры

### PostgreSQL

Provider:

```text
io.github.stasbykov.spring.testcontainerwrapper.postgres.PostgreSqlContainerProvider
```

Поддерживаемые свойства:

- `imageName`
- `databaseName`
- `username`
- `password`
- `reuse`

Экспортируемые атрибуты:

- `host`
- `port`
- `jdbcUrl`
- `username`
- `password`
- `databaseName`

Регистратор свойств:

```text
io.github.stasbykov.spring.testcontainerwrapper.postgres.PostgresPropertyRegistrar
```

### Kafka

Provider:

```text
io.github.stasbykov.spring.testcontainerwrapper.kafka.KafkaContainerProvider
```

Поддерживаемые свойства:

- `imageName`
- `reuse`

Экспортируемые атрибуты:

- `host`
- `port`
- `bootstrapServers`

Регистратор свойств:

```text
io.github.stasbykov.spring.testcontainerwrapper.kafka.KafkaPropertyRegistrar
```

## Как написать свой provider

Нужно реализовать `InfraContainerProvider` и вернуть `StartedInfraContainer`.

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

Если нужно зарегистрировать Spring properties, реализуйте `DynamicPropertyRegistrar`:

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

## Как это работает

- библиотека читает `@WithContainer` с тестового класса;
- валидирует конфигурацию до старта Spring context;
- поднимает контейнеры в правильном порядке с учетом зависимостей;
- резолвит placeholders из атрибутов уже запущенных контейнеров;
- добавляет dynamic properties в Spring `Environment`;
- останавливает `CLASS`-контейнеры после завершения тестового класса;
- держит `SHARED`-контейнеры до конца текущего JVM test run.

## Ограничения MVP

- пока нет встроенного провайдера для запуска тестируемого приложения в Docker;
- пока нет meta-annotations;
- пока нет встроенных провайдеров для Redis/LocalStack/GenericContainer;
- тестируемое Spring Boot приложение в MVP предполагается запускать в том же JVM через `@SpringBootTest`.

## Тесты

Запуск тестов:

```bash
mvn test
```

## English Documentation

English documentation is available in [README.md](README.md).
