package io.quarkus.nativeimage.deployment;

import java.nio.file.Path;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Native image extension configuration.
 */
@ConfigRoot
public class NativeImageConfig {

    /**
     * Specify the path for native image specific resource generation. This should be a distinct
     * path from the augmentation resource path.
     */
    // TODO: this path should be based on a global, build-system-agnostic base build path
    // for now it's just a sloppy hack to get something working
    @ConfigItem(defaultValue = "${user.dir}/target/native-image-classes")
    Path resourcePath;

    /**
     * The output path in which the generated native image should be placed.
     */
    // TODO: this path should be based on a global, build-system-agnostic base build path
    @ConfigItem(defaultValue = "${user.dir}/target")
    Path outputPath;

    /**
     * The name of the native image executable file.
     */
    @ConfigItem(defaultValue = "${quarkus.application.name}")
    String imageName;

    /**
     * Enable generation of debug symbols in the native image.
     */
    @ConfigItem
    boolean debugSymbols;

    /**
     * Enable isolates in the native image.
     */
    @ConfigItem
    boolean enableIsolates;

    /**
     * Enable full stack traces in the native image.
     */
    @ConfigItem(defaultValue = "true")
    boolean enableFullStackTraces;

    /**
     * Add code to allow JNI libraries to be linked in to the image. If an extension is included
     * which requires JNI, this option will be ignored and assumed to be {@code true}.
     */
    @ConfigItem
    boolean enableJni;

    /**
     * Allow the build classpath to be incomplete.
     */
    @ConfigItem(defaultValue = "true")
    boolean allowIncompleteClassPath;

    /**
     * Enable generation of build reports.
     */
    @ConfigItem(defaultValue = "true")
    boolean enableReports;

    /**
     * Use the host JDK for running the {@code native-image} driver (experimental).
     */
    @ConfigItem
    boolean useHostJdkForDriver;

    /**
     * Enable assertions in the generated native image.
     */
    @ConfigItem
    boolean enableAssertions;
}
