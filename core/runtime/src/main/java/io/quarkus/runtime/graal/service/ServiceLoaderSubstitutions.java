package io.quarkus.runtime.graal.service;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.AlwaysInline;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.oracle.svm.core.jdk.JDK11OrLater;

@TargetClass(className = "java.lang.ModuleLayer", onlyWith = JDK11OrLater.class)
final class Target_java_lang_ModuleLayer {
}

@TargetClass(className = "java.util.ServiceLoader$Provider", onlyWith = JDK11OrLater.class)
interface Target_java_util_ServiceLoader_Provider_JDK11<S> {
    @Alias
    Class<? extends S> type();

    @Alias
    S get();
}

@TargetClass(ServiceLoader.class)
@Substitute
@SuppressWarnings("unused")
final class Target_java_util_ServiceLoader<S> implements Iterable<S> {

    final ServiceLoaderInstance instance;

    @AlwaysInline("Fast resolving of services")
    Target_java_util_ServiceLoader(ServiceLoaderInstance instance) {
        this.instance = instance;
    }

    @TargetElement(onlyWith = JDK11OrLater.class)
    @SuppressWarnings("unchecked")
    @Substitute
    public Optional<S> findFirst() {
        try {
            return Optional.ofNullable((S) instance.getInstanceOf(0));
        } catch (Throwable t) {
            throw new ServiceConfigurationError("Failed to get service instance", t);
        }
    }

    @Substitute
    @AlwaysInline("Fast resolving of services")
    public Iterator<S> iterator() {
        return new ServiceLoaderInstance.IteratorImpl<S>(instance);
    }

    @Substitute
    @TargetElement(onlyWith = JDK11OrLater.class)
    @AlwaysInline("Fast resolving of services")
    public Stream<Target_java_util_ServiceLoader_Provider_JDK11<S>> stream() {
        return StreamSupport.stream(Spliterators.spliterator(new ServiceLoaderInstance.ProviderIteratorImpl<>(instance),
                instance.count, Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.NONNULL), false);
    }

    @Substitute
    public void reload() {
        // no operation
    }

    @Substitute
    public String toString() {
        return "Service loader for " + instance.serviceType;
    }

    // --------------
    // static methods
    // --------------

    @SuppressWarnings("unchecked")
    @Substitute
    @AlwaysInline("Fast resolving of services")
    public static <S> ServiceLoader<S> load(Class<S> clazz) {
        Objects.requireNonNull(clazz);
        return SubstrateUtil.cast(new Target_java_util_ServiceLoader<>(ServiceLoaderInstance.getInstance(clazz)),
                ServiceLoader.class);
    }

    @Substitute
    @AlwaysInline("Fast resolving of services")
    public static <S> ServiceLoader<S> loadInstalled(Class<S> clazz) {
        return load(clazz);
    }

    @Substitute
    @AlwaysInline("Fast resolving of services")
    public static <S> ServiceLoader<S> load(Class<S> clazz, ClassLoader classLoader) {
        return load(clazz);
    }

    @TargetElement(onlyWith = JDK11OrLater.class)
    @AlwaysInline("Fast resolving of services")
    @Substitute
    public static <S> ServiceLoader<S> load(Target_java_lang_ModuleLayer layer, Class<S> clazz) {
        return load(clazz);
    }
}

@SuppressWarnings("unused")
final class ServiceLoaderSubstitutions {
}
