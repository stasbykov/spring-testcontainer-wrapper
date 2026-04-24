package io.github.stasbykov.spring.testcontainerwrapper.support;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;
import io.github.stasbykov.spring.testcontainerwrapper.spi.ContainerStartContext;
import io.github.stasbykov.spring.testcontainerwrapper.spi.InfraContainerProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RecordingInfraContainerProvider implements InfraContainerProvider {

    private static final AtomicInteger START_COUNT = new AtomicInteger();
    private static final AtomicInteger CLOSE_COUNT = new AtomicInteger();
    private static final AtomicInteger INSTANCE_SEQUENCE = new AtomicInteger();
    private static final ConcurrentMap<String, AtomicInteger> STARTS_BY_ID = new ConcurrentHashMap<>();

    public static void reset() {
        START_COUNT.set(0);
        CLOSE_COUNT.set(0);
        INSTANCE_SEQUENCE.set(0);
        STARTS_BY_ID.clear();
    }

    public static int startCount() {
        return START_COUNT.get();
    }

    public static int closeCount() {
        return CLOSE_COUNT.get();
    }

    public static int startsFor(String id) {
        AtomicInteger counter = STARTS_BY_ID.get(id);
        return counter != null ? counter.get() : 0;
    }

    @Override
    public StartedInfraContainer start(ContainerStartContext context) {
        START_COUNT.incrementAndGet();
        STARTS_BY_ID.computeIfAbsent(context.id(), ignored -> new AtomicInteger()).incrementAndGet();

        String startupDelayMs = context.property("startupDelayMs");
        if (startupDelayMs != null && !startupDelayMs.isBlank()) {
            try {
                Thread.sleep(Long.parseLong(startupDelayMs));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted during fake container startup", ex);
            }
        }

        Map<String, String> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : context.properties().entrySet()) {
            if (entry.getKey().startsWith("attr.")) {
                attributes.put(entry.getKey().substring("attr.".length()), entry.getValue());
            }
        }
        if (attributes.isEmpty() && context.property("value") != null) {
            attributes.put("value", context.property("value"));
        }
        attributes.put("instanceId", String.valueOf(INSTANCE_SEQUENCE.incrementAndGet()));

        return StartedInfraContainer.of(
                context.id(),
                context.scope(),
                context,
                attributes,
                CLOSE_COUNT::incrementAndGet
        );
    }
}
