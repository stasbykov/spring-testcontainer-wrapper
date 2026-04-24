package io.github.stasbykov.spring.testcontainerwrapper.internal;

import io.github.stasbykov.spring.testcontainerwrapper.StartedInfraContainer;

record ManagedContainer(StartedInfraContainer container) {
}
