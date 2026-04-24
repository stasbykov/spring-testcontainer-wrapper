package io.github.stasbykov.spring.testcontainerwrapper;

import java.util.Map;
import java.util.Objects;

public final class StartedInfraContainer implements AutoCloseable {

    @FunctionalInterface
    public interface CloseAction {
        void close() throws Exception;
    }

    private final String id;
    private final ContainerScope scope;
    private final Object source;
    private final Map<String, String> attributes;
    private final CloseAction closeAction;

    private StartedInfraContainer(
            String id,
            ContainerScope scope,
            Object source,
            Map<String, String> attributes,
            CloseAction closeAction
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.scope = Objects.requireNonNull(scope, "scope must not be null");
        this.source = source;
        this.attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction must not be null");
    }

    public static StartedInfraContainer of(
            String id,
            ContainerScope scope,
            Object source,
            Map<String, String> attributes,
            CloseAction closeAction
    ) {
        return new StartedInfraContainer(id, scope, source, attributes, closeAction);
    }

    public static StartedInfraContainer of(String id, ContainerScope scope, AutoCloseable source, Map<String, String> attributes) {
        return new StartedInfraContainer(id, scope, source, attributes, source::close);
    }

    public String id() {
        return this.id;
    }

    public ContainerScope scope() {
        return this.scope;
    }

    public Object source() {
        return this.source;
    }

    public <T> T source(Class<T> sourceType) {
        return sourceType.cast(this.source);
    }

    public Map<String, String> attributes() {
        return this.attributes;
    }

    public String attribute(String name) {
        String value = this.attributes.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Container '" + this.id + "' does not expose attribute '" + name + "'");
        }
        return value;
    }

    @Override
    public void close() {
        try {
            this.closeAction.close();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to close container '" + this.id + "'", ex);
        }
    }
}
