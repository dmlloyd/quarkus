package io.quarkus.nativeimage.deployment.items;

import java.nio.file.Path;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The build item that contains the path of the final native image itself.
 */
public final class NativeImageBuildItem extends SimpleBuildItem {
    private final Path nativeImagePath;

    /**
     * Construct a new instance.
     *
     * @param nativeImagePath the native image path (must not be {@code null})
     */
    public NativeImageBuildItem(final Path nativeImagePath) {
        Assert.checkNotNullParam("nativeImagePath", nativeImagePath);
        this.nativeImagePath = nativeImagePath;
    }

    /**
     * Get the native image path.
     *
     * @return the native image path (not {@code null})
     */
    public Path getNativeImagePath() {
        return nativeImagePath;
    }
}
