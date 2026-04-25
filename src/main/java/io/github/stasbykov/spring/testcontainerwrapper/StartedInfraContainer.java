package io.github.stasbykov.spring.testcontainerwrapper;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a container instance started by the library.
 *
 * <p>The object exposes the underlying source object, resolved runtime attributes and the
 * lifecycle callback used to stop the resource.</p>
 */
public final class StartedInfraContainer implements AutoCloseable {

    /**
     * Callback invoked when the container must be stopped.
     */
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

    /**
     * Creates a descriptor backed by an {@link AutoCloseable} source object.
     */
    public static StartedInfraContainer of(String id, ContainerScope scope, AutoCloseable source, Map<String, String> attributes) {
        return new StartedInfraContainer(id, scope, source, attributes, source::close);
    }

    /**
     * Returns the logical id declared in {@link WithContainer}.
     */
    public String id() {
        return this.id;
    }

    /**
     * Returns the lifecycle scope of the container.
     */
    public ContainerScope scope() {
        return this.scope;
    }

    /**
     * Returns the raw source object created by the provider.
     */
    public Object source() {
        return this.source;
    }

    /**
     * Casts the raw source object to the requested type.
     *
     * @param sourceType target type expected by the caller
     * @param <T> source type
     * @return cast source object
     */
    public <T> T source(Class<T> sourceType) {
        return sourceType.cast(this.source);
    }

    /**
     * Returns immutable runtime attributes exposed by the provider.
     */
    public Map<String, String> attributes() {
        return this.attributes;
    }

    /**
     * Returns a required runtime attribute.
     *
     * @param name attribute name
     * @return attribute value
     * @throws IllegalArgumentException when the attribute does not exist
     */
    public String attribute(String name) {
        String value = this.attributes.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Container '" + this.id + "' does not expose attribute '" + name + "'");
        }
        return value;
    }

    /**
     * Stops the underlying resource via the configured close action.
     */
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
