package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.ContainerScope;
import io.github.stasbykov.spring.testcontainerwrapper.InfraContainerRegistry;
import io.github.stasbykov.spring.testcontainerwrapper.spi.DynamicPropertyRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.List;
import java.util.Objects;

public final class InfraContainersContextCustomizerFactory implements ContextCustomizerFactory {

    private final AnnotationContainerDefinitionParser parser = new AnnotationContainerDefinitionParser();

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes
    ) {
        List<ContainerDefinition> definitions = this.parser.parse(testClass);
        boolean isolateContextPerClass = definitions.stream().anyMatch(definition -> definition.scope() == ContainerScope.CLASS);
        return new InfraContainersContextCustomizer(testClass, definitions, isolateContextPerClass);
    }

    private static final class InfraContainersContextCustomizer implements ContextCustomizer {

        private final Class<?> testClass;
        private final List<ContainerDefinition> definitions;
        private final boolean isolateContextPerClass;
        private final ContainerBootstrapper bootstrapper =
                new ContainerBootstrapper(ContainerManager.INSTANCE, new ContainerPlaceholderResolver());

        private InfraContainersContextCustomizer(
                Class<?> testClass,
                List<ContainerDefinition> definitions,
                boolean isolateContextPerClass
        ) {
            this.testClass = testClass;
            this.definitions = List.copyOf(definitions);
            this.isolateContextPerClass = isolateContextPerClass;
        }

        @Override
        public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
            ContainerBootstrapResult result = this.bootstrapper.bootstrap(this.testClass, this.definitions);
            registerRegistryBean(context, result);
            registerDynamicProperties(context, result);
        }

        private void registerRegistryBean(ConfigurableApplicationContext context, ContainerBootstrapResult result) {
            context.getBeanFactory().registerSingleton(
                    InfraContainerRegistry.class.getName(),
                    new InfraContainerRegistry(result.containersById())
            );
        }

        private void registerDynamicProperties(ConfigurableApplicationContext context, ContainerBootstrapResult result) {
            CollectedDynamicPropertyRegistry registry = new CollectedDynamicPropertyRegistry();
            for (RegisteredContainer registration : result.registrations()) {
                DynamicPropertyRegistrar registrar = BeanUtils.instantiateClass(registration.definition().propertyRegistrarType());
                registrar.register(registry, registration.container());
            }
            if (registry.isEmpty()) {
                return;
            }
            MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
            propertySources.addFirst(new SupplierBackedPropertySource(
                    "infra-containers:" + this.testClass.getName(),
                    registry.asMap()
            ));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof InfraContainersContextCustomizer that)) {
                return false;
            }
            return Objects.equals(this.definitions, that.definitions)
                    && Objects.equals(this.isolationKey(), that.isolationKey());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.definitions, this.isolationKey());
        }

        private Object isolationKey() {
            return this.isolateContextPerClass ? this.testClass.getName() : null;
        }
    }
}
