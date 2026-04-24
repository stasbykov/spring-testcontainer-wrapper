package io.github.stasbykov.spring.testcontainerwrapper.kafka;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class KafkaPropertyRegistrar implements DynamicPropertyRegistrar {

    @Override
    public void register(DynamicPropertyRegistry registry, StartedInfraContainer container) {
        registry.add("spring.kafka.bootstrap-servers", () -> container.attribute("bootstrapServers"));
    }
}
