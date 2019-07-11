package io.quarkus.nativeimage.deployment.items;

import java.nio.file.Path;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 *
 */
// TODO: make this extend an abstract base resources path build item
public final class NativeImageResourcePathBuildItem extends SimpleBuildItem {
    private final Path path;

    public NativeImageResourcePathBuildItem(final Path path) {
        Assert.checkNotNullParam("path", path);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
