package io.github.stasbykov.spring.testcontainerwrapper.internal;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

final class CollectedDynamicPropertyRegistry implements DynamicPropertyRegistry {

    private final Map<String, Supplier<Object>> properties = new LinkedHashMap<>();

    @Override
    public void add(String name, Supplier<Object> valueSupplier) {
        this.properties.put(name, valueSupplier);
    }

    boolean isEmpty() {
        return this.properties.isEmpty();
    }

    Map<String, Supplier<Object>> asMap() {
        return Map.copyOf(this.properties);
    }
}
