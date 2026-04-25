package io.github.stasbykov.spring.testcontainerwrapper.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public final class InfraContainersTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(InfraContainersTestExecutionListener.class);

    @Override
    public void afterTestClass(TestContext testContext) {
        log.debug("afterTestClass invoked for {}", testContext.getTestClass().getName());
        ContainerManager.INSTANCE.stopClassContainers(testContext.getTestClass());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
