package io.quarkus.runtime.graal.service;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;

import com.oracle.svm.core.annotate.AlwaysInline;

/**
 *
 */
public abstract class ServiceLoaderInstance {
    final Class<?> serviceType;
    final int count;

    @AlwaysInline("ServiceLoader trivial method")
    public ServiceLoaderInstance(final Class<?> serviceType, final int count) {
        this.serviceType = serviceType;
        this.count = count;
    }

    @AlwaysInline("Fold service load class comparison")
    static ServiceLoaderInstance getInstance(@SuppressWarnings("unused") Class<?> clazz) {
        throw new UnsupportedOperationException("Substituted by generated code");
    }

    public abstract Class<?> getTypeOf(int index);

    public abstract Object getInstanceOf(int index);

    @AlwaysInline("Support for generated methods")
    public static boolean isEqual(int i1, int i2) {
        return i1 == i2;
    }

    public static final class Empty extends ServiceLoaderInstance {

        public Empty(final Class<?> serviceType) {
            super(serviceType, 0);
        }

        public Class<?> getTypeOf(final int index) {
            return null;
        }

        public Object getInstanceOf(final int index) {
            return null;
        }
    }

    static final class IteratorImpl<S> implements Iterator<S> {
        private final ServiceLoaderInstance sli;
        private int idx;

        IteratorImpl(final ServiceLoaderInstance sli) {
            this.sli = sli;
        }

        public boolean hasNext() {
            return idx < sli.count;
        }

        @SuppressWarnings("unchecked")
        public S next() {
            if (!hasNext())
                throw new NoSuchElementException();
            try {
                return (S) sli.getInstanceOf(idx++);
            } catch (Throwable t) {
                throw new ServiceConfigurationError("Failed to get service instance", t);
            }
        }
    }

    static final class ProviderIteratorImpl<S> implements Iterator<Target_java_util_ServiceLoader_Provider_JDK11<S>> {
        private final ServiceLoaderInstance sli;
        private int idx;

        ProviderIteratorImpl(final ServiceLoaderInstance sli) {
            this.sli = sli;
        }

        public boolean hasNext() {
            return idx < sli.count;
        }

        public Target_java_util_ServiceLoader_Provider_JDK11<S> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return new ServiceLoaderInstance.ProviderIteratorImpl.ProviderImpl<>(sli, idx++);
        }

        static final class ProviderImpl<S> implements Target_java_util_ServiceLoader_Provider_JDK11<S> {
            private final ServiceLoaderInstance sli;
            private final int idx;

            ProviderImpl(final ServiceLoaderInstance sli, final int idx) {
                this.sli = sli;
                this.idx = idx;
            }

            @SuppressWarnings("unchecked")
            public Class<? extends S> type() {
                return (Class<? extends S>) sli.getTypeOf(idx);
            }

            @SuppressWarnings("unchecked")
            public S get() {
                final Object value;
                try {
                    value = sli.getInstanceOf(idx);
                } catch (Throwable t) {
                    throw new ServiceConfigurationError("Failed to get service instance", t);
                }
                if (value == null) {
                    throw new ServiceConfigurationError("Null service instance returned");
                }
                return (S) value;
            }
        }
    }
}
