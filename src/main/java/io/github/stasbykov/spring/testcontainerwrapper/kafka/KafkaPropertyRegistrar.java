package io.github.stasbykov.spring.testcontainerwrapper.kafka;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class KafkaPropertyRegistrar implements DynamicPropertyRegistrar {

    private static final Logger log = LoggerFactory.getLogger(KafkaPropertyRegistrar.class);

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
        log.debug("Registering Kafka Spring properties from container '{}'", container.id());
        registry.add("spring.kafka.bootstrap-servers", () -> container.attribute("bootstrapServers"));
    }
}
