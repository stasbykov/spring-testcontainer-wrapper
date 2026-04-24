package io.github.stasbykov.spring.testcontainerwrapper.internal;

import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.function.Supplier;

final class SupplierBackedPropertySource extends PropertySource<Map<String, Supplier<Object>>> {

    SupplierBackedPropertySource(String name, Map<String, Supplier<Object>> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        Supplier<Object> supplier = this.source.get(name);
        return supplier != null ? supplier.get() : null;
    }
}
