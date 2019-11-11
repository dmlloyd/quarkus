package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.IntFunction;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 *
 */
public final class ConfigUtils {
    private ConfigUtils() {
    }

    public static <T> IntFunction<List<T>> listFactory() {
        return ArrayList::new;
    }

    public static <T> IntFunction<Set<T>> setFactory() {
        return LinkedHashSet::new;
    }

    public static <T> IntFunction<SortedSet<T>> sortedSetFactory() {
        return size -> new TreeSet<>();
    }

    /**
     * Get the basic configuration builder.
     *
     * @return the configuration builder
     */
    public static SmallRyeConfigBuilder configBuilder() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        final ApplicationPropertiesConfigSource.InFileSystem inFileSystem = new ApplicationPropertiesConfigSource.InFileSystem();
        final ApplicationPropertiesConfigSource.InJar inJar = new ApplicationPropertiesConfigSource.InJar();
        builder.withSources(inFileSystem, inJar);
        final ExpandingConfigSource.Cache cache = new ExpandingConfigSource.Cache();
        builder.withWrapper(ExpandingConfigSource.wrapper(cache));
        builder.withWrapper(DeploymentProfileConfigSource.wrapper());
        builder.addDefaultSources();
        builder.addDiscoveredSources();
        builder.addDiscoveredConverters();
        return builder;
    }
}
