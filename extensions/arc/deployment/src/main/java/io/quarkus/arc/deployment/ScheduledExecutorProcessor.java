package io.quarkus.arc.deployment;

import java.util.concurrent.ScheduledExecutorService;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ScheduledExecutorBuildItem;

/**
 * This processor allows scheduled executors to be injected.
 */
public class ScheduledExecutorProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    RuntimeBeanBuildItem install(ScheduledExecutorBuildItem item) {
        return RuntimeBeanBuildItem.builder(ScheduledExecutorService.class).setSupplier(
                () -> item.getExecutorProxy()).build();
    }
}
