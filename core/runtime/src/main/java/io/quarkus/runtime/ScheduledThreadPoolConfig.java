package io.quarkus.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The scheduled thread pool config. This thread pool is responsible for running
 * all scheduled tasks.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ScheduledThreadPoolConfig {

    /**
     * The scheduled thread pool size.
     */
    @ConfigItem(defaultValue = "1")
    public int size;

    /**
     * The shutdown timeout. If all pending work has not been completed by this time
     * then additional threads will be spawned to attempt to finish any pending tasks, and the shutdown process will
     * continue
     */
    @ConfigItem(defaultValue = "1M")
    public Duration shutdownTimeout;

    /**
     * The frequency at which the status of the thread pool should be checked during shutdown. Information about
     * waiting tasks and threads will be checked and possibly logged at this interval. Setting this key to an empty
     * value disables the shutdown check interval.
     */
    @ConfigItem(defaultValue = "5")
    public Optional<Duration> shutdownCheckInterval;
}
