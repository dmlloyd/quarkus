package io.quarkus.deployment.builditem;

import java.util.concurrent.ScheduledExecutorService;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The main executor for scheduled tasks.
 */
public final class ScheduledExecutorBuildItem extends SimpleBuildItem {
    private final ScheduledExecutorService executor;

    public ScheduledExecutorBuildItem(final ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public ScheduledExecutorService getExecutorProxy() {
        return executor;
    }
}
