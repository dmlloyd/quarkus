package io.quarkus.runtime;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

final class TrackingThreadFactory implements ThreadFactory {
    private static final Thread[] NO_THREADS = new Thread[0];

    private final ThreadFactory delegate;
    private final Set<Thread> threads = Collections.newSetFromMap(new ConcurrentHashMap<>());

    TrackingThreadFactory(final ThreadFactory delegate) {
        this.delegate = delegate;
    }

    public Thread newThread(final Runnable r) {
        return delegate.newThread(new Runnable() {
            public void run() {
                threads.add(Thread.currentThread());
                try {
                    r.run();
                } finally {
                    threads.remove(Thread.currentThread());
                }
            }
        });
    }

    Thread[] getThreads() {
        return threads.toArray(NO_THREADS);
    }
}
