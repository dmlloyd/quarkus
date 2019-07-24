package io.quarkus.scheduler.runtime;

import java.util.ArrayDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduler;

@Typed(Scheduler.class)
@ApplicationScoped
public class SchedulerImpl implements Scheduler {
    @Inject
    ScheduledExecutorService executor;

    private volatile boolean paused;
    private final ArrayDeque<Runnable> pausedTasks = new ArrayDeque<>();

    public void pause() {
        paused = true;
    }

    public void resume() {
        if (paused) {
            synchronized (pausedTasks) {
                if (paused) {
                    Runnable task;
                    while ((task = pausedTasks.pollFirst()) != null) {
                        executor.execute(task);
                    }
                    paused = false;
                }
            }
        }
    }

    public void startTimer(final long delay, final Runnable action) {
        executor.schedule(new Runnable() {
            public void run() {
                if (paused) {
                    synchronized (pausedTasks) {
                        if (paused) {
                            pausedTasks.addLast(action);
                            return;
                        }
                    }
                }
                action.run();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
