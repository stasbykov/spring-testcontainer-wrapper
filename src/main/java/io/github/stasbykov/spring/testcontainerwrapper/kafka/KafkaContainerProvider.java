package io.github.stasbykov.spring.testcontainerwrapper.kafka;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.ContainerStartContext;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built-in provider that starts a {@link KafkaContainer}.
 *
 * <p>Supported properties: {@code imageName}, {@code reuse}.</p>
 */
public final class KafkaContainerProvider implements InfraContainerProvider {

    private static final String DEFAULT_IMAGE = "apache/kafka-native:3.8.0";
    private static final Logger log = LoggerFactory.getLogger(KafkaContainerProvider.class);

    @Override
    public StartedInfraContainer start(ContainerStartContext context) {
        String imageName = context.properties().getOrDefault("imageName", DEFAULT_IMAGE);
        log.debug("Starting Kafka container '{}' with image {}", context.id(), imageName);
        KafkaContainer container = new KafkaContainer(DockerImageName.parse(
                imageName
        ));
        if (context.properties().containsKey("reuse")) {
            container.withReuse(Boolean.parseBoolean(context.property("reuse")));
        }
        container.start();
        log.debug("Kafka container '{}' started with bootstrap servers {}", context.id(), container.getBootstrapServers());

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("host", container.getHost());
        attributes.put("port", String.valueOf(container.getFirstMappedPort()));
        attributes.put("bootstrapServers", container.getBootstrapServers());
        return StartedInfraContainer.of(context.id(), context.scope(), container, attributes);
    }
}
