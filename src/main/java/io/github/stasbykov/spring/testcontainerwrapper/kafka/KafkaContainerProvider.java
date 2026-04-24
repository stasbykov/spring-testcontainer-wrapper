package io.github.stasbykov.spring.testcontainerwrapper.kafka;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.ContainerStartContext;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KafkaContainerProvider implements InfraContainerProvider {

    private static final String DEFAULT_IMAGE = "apache/kafka-native:3.8.0";

    @Override
    public StartedInfraContainer start(ContainerStartContext context) {
        KafkaContainer container = new KafkaContainer(DockerImageName.parse(
                context.properties().getOrDefault("imageName", DEFAULT_IMAGE)
        ));
        if (context.properties().containsKey("reuse")) {
            container.withReuse(Boolean.parseBoolean(context.property("reuse")));
        }
        container.start();

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("host", container.getHost());
        attributes.put("port", String.valueOf(container.getFirstMappedPort()));
        attributes.put("bootstrapServers", container.getBootstrapServers());
        return StartedInfraContainer.of(context.id(), context.scope(), container, attributes);
    }
}
