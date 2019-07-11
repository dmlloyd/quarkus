package io.quarkus.nativeimage.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.AugmentPhaseBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.StringUtil;
import io.quarkus.nativeimage.deployment.items.NativeImageBuildItem;
import io.quarkus.nativeimage.deployment.items.NativeImageResourceBuildItem;
import io.quarkus.nativeimage.deployment.items.NativeImageResourcePathBuildItem;
import io.quarkus.runtime.Quarkus;
import io.quarkus.utilities.JavaBinFinder;

/**
 * The native image extension processor.
 */
public class NativeImageProcessor {
    private static final Logger niLog = Logger.getLogger("io.quarkus.native-image");

    @BuildStep
    NativeImageResourcePathBuildItem processNativeResources(
            NativeImageConfig config,
            List<NativeImageResourceBuildItem> resources) throws IOException {

        Path basePath = config.resourcePath;

        try {
            for (NativeImageResourceBuildItem resource : resources) {
                try (OutputStream os = Files.newOutputStream(basePath.resolve(resource.getName()), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    resource.writeTo(os);
                }
            }
        } catch (IOException e) {
            try {
                FileUtil.deleteDirectory(basePath);
            } catch (IOException e2) {
                e2.addSuppressed(e);
                throw e2;
            }
            throw e;
        }
        return new NativeImageResourcePathBuildItem(basePath);
    }

    @BuildStep
    @Produce(AugmentPhaseBuildItem.class)
    NativeImageBuildItem buildNativeImage(
            NativeImageConfig config,
            NativeImageResourcePathBuildItem nativeResourcePathItem) throws IOException {

        ArrayList<String> cmd = new ArrayList<>();

        // todo: once the artifacts move to Maven, only use this when not driving thru host JDK
        String graalHome = System.getenv("GRAALVM_HOME");
        if (graalHome == null) {
            throw new IOException("No GRAALVM_HOME set");
        }
        while (graalHome.endsWith(File.separator)) {
            graalHome = graalHome.substring(0, graalHome.length() - File.separator.length());
        }
        if (graalHome.isEmpty()) {
            throw new IOException("GRAALVM_HOME is empty");
        }

        if (config.useHostJdkForDriver) {
            cmd.add(JavaBinFinder.findBin());
        } else {
            cmd.add(StringUtil.join(File.separator, graalHome, "bin", "java"));
        }

        ArrayList<String> niClassPath = new ArrayList<>();

        // todo: these should come from Maven
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "graalvm", "svm-driver.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "graalvm", "svm-configure.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "graalvm", "svm-agent.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "graalvm", "launcher-common.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "svm", "builder", "javacpp.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "svm", "builder", "pointsto.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "svm", "builder", "objectfile.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "svm", "library-support.jar"));

        // todo: these should come from our class path
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "svm", "builder", "svm.jar"));
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "boot", "graal-sdk.jar"));

        if (config.useHostJdkForDriver) {
            if (JDK_VERSION >= 9) {
                // todo...?
                //                cmd.add("-XX:+UnlockExperimentalVMOptions");
                //                cmd.add("-XX:+EnableJVMCI");

                openModule(cmd, "jdk.internal.vm.ci",
                        "jdk.vm.ci.services");
                openModule(cmd, "jdk.internal.vm.compiler",
                        "jdk.internal.vm.compiler.collections",
                        "jdk.internal.vm.compiler.word",
                        "jdk.internal.vm.compiler.word.impl",
                        "org.graalvm.compiler.api.directives",
                        "org.graalvm.compiler.api.replacements",
                        "org.graalvm.compiler.api.runtime",
                        "org.graalvm.compiler.asm",
                        "org.graalvm.compiler.asm.aarch64",
                        "org.graalvm.compiler.asm.amd64",
                        "org.graalvm.compiler.asm.sparc",
                        "org.graalvm.compiler.bytecode",
                        "org.graalvm.compiler.code",
                        "org.graalvm.compiler.core",
                        "org.graalvm.compiler.core.aarch64",
                        "org.graalvm.compiler.core.amd64",
                        "org.graalvm.compiler.core.common",
                        "org.graalvm.compiler.core.common.alloc",
                        "org.graalvm.compiler.core.common.calc",
                        "org.graalvm.compiler.core.common.cfg",
                        "org.graalvm.compiler.core.common.spi",
                        "org.graalvm.compiler.core.common.type",
                        "org.graalvm.compiler.core.common.util",
                        "org.graalvm.compiler.core.gen",
                        "org.graalvm.compiler.core.match",
                        "org.graalvm.compiler.core.phases",
                        "org.graalvm.compiler.core.sparc",
                        "org.graalvm.compiler.core.target",
                        "org.graalvm.compiler.debug",
                        "org.graalvm.compiler.graph",
                        "org.graalvm.compiler.graph.iterators",
                        "org.graalvm.compiler.graph.spi",
                        "org.graalvm.compiler.hotspot",
                        "org.graalvm.compiler.hotspot.aarch64",
                        "org.graalvm.compiler.hotspot.amd64",
                        "org.graalvm.compiler.hotspot.debug",
                        "org.graalvm.compiler.hotspot.lir",
                        "org.graalvm.compiler.hotspot.meta",
                        "org.graalvm.compiler.hotspot.nodes",
                        "org.graalvm.compiler.hotspot.nodes.aot",
                        "org.graalvm.compiler.hotspot.nodes.profiling",
                        "org.graalvm.compiler.hotspot.nodes.type",
                        "org.graalvm.compiler.hotspot.phases",
                        "org.graalvm.compiler.hotspot.phases.aot",
                        "org.graalvm.compiler.hotspot.phases.profiling",
                        "org.graalvm.compiler.hotspot.replacements",
                        "org.graalvm.compiler.hotspot.replacements.aot",
                        "org.graalvm.compiler.hotspot.replacements.arraycopy",
                        "org.graalvm.compiler.hotspot.replacements.profiling",
                        "org.graalvm.compiler.hotspot.sparc",
                        "org.graalvm.compiler.hotspot.stubs",
                        "org.graalvm.compiler.hotspot.word",
                        "org.graalvm.compiler.java",
                        "org.graalvm.compiler.lir",
                        "org.graalvm.compiler.lir.aarch64",
                        "org.graalvm.compiler.lir.alloc",
                        "org.graalvm.compiler.lir.alloc.lsra",
                        "org.graalvm.compiler.lir.alloc.lsra.ssa",
                        "org.graalvm.compiler.lir.alloc.trace",
                        "org.graalvm.compiler.lir.alloc.trace.bu",
                        "org.graalvm.compiler.lir.alloc.trace.lsra",
                        "org.graalvm.compiler.lir.amd64",
                        "org.graalvm.compiler.lir.amd64.phases",
                        "org.graalvm.compiler.lir.amd64.vector",
                        "org.graalvm.compiler.lir.asm",
                        "org.graalvm.compiler.lir.constopt",
                        "org.graalvm.compiler.lir.debug",
                        "org.graalvm.compiler.lir.dfa",
                        "org.graalvm.compiler.lir.framemap",
                        "org.graalvm.compiler.lir.gen",
                        "org.graalvm.compiler.lir.phases",
                        "org.graalvm.compiler.lir.profiling",
                        "org.graalvm.compiler.lir.sparc",
                        "org.graalvm.compiler.lir.ssa",
                        "org.graalvm.compiler.lir.stackslotalloc",
                        "org.graalvm.compiler.lir.util",
                        "org.graalvm.compiler.loop",
                        "org.graalvm.compiler.loop.phases",
                        "org.graalvm.compiler.nodeinfo",
                        "org.graalvm.compiler.nodes",
                        "org.graalvm.compiler.nodes.calc",
                        "org.graalvm.compiler.nodes.cfg",
                        "org.graalvm.compiler.nodes.debug",
                        "org.graalvm.compiler.nodes.extended",
                        "org.graalvm.compiler.nodes.graphbuilderconf",
                        "org.graalvm.compiler.nodes.java",
                        "org.graalvm.compiler.nodes.memory",
                        "org.graalvm.compiler.nodes.memory.address",
                        "org.graalvm.compiler.nodes.spi",
                        "org.graalvm.compiler.nodes.type",
                        "org.graalvm.compiler.nodes.util",
                        "org.graalvm.compiler.nodes.virtual",
                        "org.graalvm.compiler.options",
                        "org.graalvm.compiler.phases",
                        "org.graalvm.compiler.phases.common",
                        "org.graalvm.compiler.phases.common.inlining",
                        "org.graalvm.compiler.phases.common.inlining.info",
                        "org.graalvm.compiler.phases.common.inlining.info.elem",
                        "org.graalvm.compiler.phases.common.inlining.policy",
                        "org.graalvm.compiler.phases.common.inlining.walker",
                        "org.graalvm.compiler.phases.common.util",
                        "org.graalvm.compiler.phases.contract",
                        "org.graalvm.compiler.phases.graph",
                        "org.graalvm.compiler.phases.schedule",
                        "org.graalvm.compiler.phases.tiers",
                        "org.graalvm.compiler.phases.util",
                        "org.graalvm.compiler.phases.verify",
                        "org.graalvm.compiler.printer",
                        "org.graalvm.compiler.replacements",
                        "org.graalvm.compiler.replacements.aarch64",
                        "org.graalvm.compiler.replacements.amd64",
                        "org.graalvm.compiler.replacements.classfile",
                        "org.graalvm.compiler.replacements.nodes",
                        "org.graalvm.compiler.replacements.nodes.arithmetic",
                        "org.graalvm.compiler.replacements.sparc",
                        "org.graalvm.compiler.runtime",
                        "org.graalvm.compiler.serviceprovider",
                        "org.graalvm.compiler.virtual.nodes",
                        "org.graalvm.compiler.virtual.phases.ea",
                        "org.graalvm.compiler.word",
                        "org.graalvm.graphio",
                        "org.graalvm.util");
            }
            // else JVMCI should already be on the class path
        } else {
            // not a JVMCI-enabled JDK
            niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "jvmci-services.jar"));
        }
        niClassPath.add(StringUtil.join(File.separator, graalHome, "jre", "lib", "jvmci", "graal.jar"));

        cmd.add("-classpath");
        cmd.add(StringUtil.join(File.pathSeparator, niClassPath.iterator()));
        cmd.add("com.oracle.svm.driver.NativeImage");

        // next come the arguments to native image itself

        cmd.add("--initialize-at-build-time=");
        cmd.add("-H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy$BySpaceAndTime"); //the default collection policy results in full GC's 50% of the time
        //https://github.com/oracle/graal/issues/660
        cmd.add("-J-Djava.util.concurrent.ForkJoinPool.common.parallelism=1");
        cmd.add("--no-fallback");

        if (config.enableJni) {
            cmd.add("-H:+JNI");
        } else {
            cmd.add("-H:-JNI");
        }

        if (config.allowIncompleteClassPath) {
            cmd.add("--allow-incomplete-classpath");
        }

        if (config.debugSymbols) {
            cmd.add("-g");
        }

        if (config.enableFullStackTraces) {
            cmd.add("-H:+StackTrace");
        } else {
            cmd.add("-H:-StackTrace");
        }

        if (!config.enableIsolates) {
            cmd.add("-H:-SpawnIsolates");
        }

        if (config.enableReports) {
            cmd.add("-H:+PrintAnalysisCallTree");
        }

        if (config.enableAssertions) {
            cmd.add("-ea");
        } else {
            cmd.add("-da");
        }

        cmd.add("-classpath");

        ArrayList<String> appClassPath = new ArrayList<>();

        appClassPath.add(nativeResourcePathItem.getPath().toString());
        // TODO: add each generated-class output path
        // TODO: add each local module classes path
        // TODO: add each dependency classes path

        cmd.add(StringUtil.join(File.pathSeparator, appClassPath.iterator()));

        // Main class
        cmd.add(Quarkus.class.getName());

        // Image name
        final Path outputPath = config.outputPath.resolve(config.imageName);
        cmd.add(outputPath.toString());

        // now build the process

        niLog.infof("Running: '%s'", StringUtil.join(" ", cmd.iterator()));
        ProcessBuilder pb = new ProcessBuilder(cmd);

        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        // todo: pb.redirectInput(ProcessBuilder.Redirect.DISCARD); // Java 9+
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);

        final Process process = pb.start();
        final int res;
        try {
            process.getOutputStream().close(); // todo: remove (Java 9+)
            final Thread outThread = startLogThread(Logger.Level.INFO, process.getInputStream());
            try {
                final Thread errThread = startLogThread(Logger.Level.WARN, process.getErrorStream());
                try {
                    try {
                        res = awaitProcess(process);
                    } catch (Throwable t) {
                        process.destroy();
                        throw t;
                    }
                    awaitThread(errThread);
                } catch (Throwable t) {
                    awaitThread(errThread);
                    throw t;
                }
                awaitThread(outThread);
            } catch (Throwable t) {
                awaitThread(outThread);
                throw t;
            }
        } catch (Throwable t) {
            process.destroy();
            awaitProcess(process);
            throw t;
        }
        if (res != 0) {
            throw new IOException("Failed to build native image (non-zero exit code)");
        }
        return new NativeImageBuildItem(outputPath);
    }

    private void openModule(final ArrayList<String> cmd, final String moduleName, final String... packages) {
        cmd.add("--add-modules");
        cmd.add(moduleName);
        for (String pkg : packages) {
            final String str = moduleName + "/" + pkg + "=ALL-UNNAMED";
            cmd.add("--add-exports");
            cmd.add(str);
        }
    }

    private void awaitThread(final Thread thread) {
        boolean intr = false;
        try {
            for (;;) {
                try {
                    thread.join();
                    return;
                } catch (InterruptedException e) {
                    intr = true;
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Thread startLogThread(final Logger.Level level, final InputStream is) {
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                final StringBuilder sb = new StringBuilder();
                int b;
                final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                try {
                    while ((b = isr.read()) != -1) {
                        if (b == '\n' || b == '\r') {
                            if (sb.length() > 0) {
                                if (isError(sb)) {
                                    niLog.log(Logger.Level.ERROR, sb);
                                } else {
                                    niLog.log(level, sb);
                                }
                                sb.setLength(0);
                            }
                        } else {
                            sb.append((char) b);
                        }
                    }
                    if (sb.length() > 0) {
                        if (isError(sb)) {
                            niLog.log(Logger.Level.ERROR, sb);
                        } else {
                            niLog.log(level, sb);
                        }
                    }
                    safeClose(is);
                } catch (Throwable t) {
                    safeClose(is);
                }
            }

            boolean isError(StringBuilder sb) {
                return prefixIs(sb, "Error:");
            }

            boolean prefixIs(StringBuilder sb, String compare) {
                if (compare.length() > sb.length()) {
                    return false;
                }
                for (int i = 0; i < compare.length(); i++) {
                    if (Character.toLowerCase(sb.charAt(i)) != Character.toLowerCase(compare.charAt(i))) {
                        return false;
                    }
                }
                return true;
            }

            void safeClose(AutoCloseable c) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable t) {
                        niLog.warnf(t, "Failed to close %s", c);
                    }
                }
            }
        }, "native-image output");
        thread.start();
        return thread;
    }

    private static final int JDK_VERSION;

    static {
        String version = System.getProperty("java.specification.version", "unknown");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        JDK_VERSION = Integer.parseInt(version);
    }

    private int awaitProcess(final Process process) {
        boolean intr = false;
        try {
            for (;;) {
                try {
                    return process.waitFor();
                } catch (InterruptedException e) {
                    intr = true;
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
