package io.quarkus.test.junit.launcher;

import java.util.Collection;
import java.util.Optional;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.CompositeTestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import io.quarkus.test.junit.classloading.FacadeClassLoader;

public class CustomLauncherInterceptor implements LauncherInterceptor {

    private FacadeClassLoader facadeLoader = null;
    private ClassLoader origCl = null;

    public CustomLauncherInterceptor() {
    }

    @Override
    public <T> T intercept(Invocation<T> invocation) {
        // Do not do any classloading dance for prod mode tests;
        T result = System.getProperty("prod.mode.tests") != null ? invocation.proceed() : actuallyIntercept(invocation);
        System.out.printf("Intercepted %s and got %s%n", invocation, result);
        if (result instanceof TestPlan tp) {
            dumpTestPlan(tp, tp.getRoots(), 0);
        }
        return result;
    }

    private static void dumpTestPlan(TestPlan testPlan, Collection<TestIdentifier> ids, int depth) {
        for (TestIdentifier id : ids) {
            System.out.printf("%s - %s %s%n", "  ".repeat(depth), id,
                    id.getSource().map(Object::getClass).map(Class::getName).orElse(""));
            dumpTestPlan(testPlan, testPlan.getChildren(id), depth + 1);
        }
    }

    private static void eagerlyLoad(TestPlan testPlan, Collection<TestIdentifier> ids) {
        for (TestIdentifier id : ids) {
            Optional<TestSource> source = id.getSource();
            if (source.isPresent()) {
                TestSource testSource = source.get();
                eagerlyLoad(testSource);
            }
            eagerlyLoad(testPlan, testPlan.getChildren(id));
        }
    }

    private static void eagerlyLoad(final TestSource testSource) {
        if (testSource instanceof ClassSource cs) {
            cs.getJavaClass();
        } else if (testSource instanceof MethodSource ms) {
            ms.getJavaClass();
        } else if (testSource instanceof CompositeTestSource cts) {
            cts.getSources().forEach(CustomLauncherInterceptor::eagerlyLoad);
        }
    }

    private <T> T actuallyIntercept(Invocation<T> invocation) {
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        // Be aware, this method might be called more than once, for different kinds of invocations; especially for Gradle executions, the executions could happen before the TCCL gets constructed and set by JUnitTestRunner
        // We might not be in the same classloader as the Facade ClassLoader, so use a name comparison instead of an instanceof
        if (currentCl == null
                || (currentCl != facadeLoader && !currentCl.getClass().getName().equals(FacadeClassLoader.class.getName()))) {
            this.origCl = currentCl;

            // We don't ever want more than one FacadeClassLoader active, especially since config gets initialised on it.
            // The gradle test execution can make more than one, perhaps because of its threading model.
            if (facadeLoader == null) {
                // We want to tidy up classloaders we created, but not ones created upstream, so keep a record of what we created
                facadeLoader = new FacadeClassLoader(currentCl);
            }
            Thread.currentThread().setContextClassLoader(facadeLoader);
            T result = invocation.proceed();
            // eagerly load all the classes!
            if (result instanceof TestPlan tp) {
                eagerlyLoad(tp, tp.getRoots());
            }
            return result;

            // It's tempting to tidy up in a finally block by resetting the TCCL, but it looks like the gradle
            // devtools tests may be asynchronous, because if we reset the TCCL
            // at this point, the test loads with the wrong classloader.
            // Instead, reset the TCCL when close() is called

        } else {
            return invocation.proceed();
        }
    }

    @Override
    public void close() {

        try {
            // Tidy up classloaders we created, but not ones created upstream
            // Also make sure to reset the TCCL so we don't leave a closed classloader on the thread
            if (facadeLoader != null) {

                // Reset the TCCL if it's one we set, but not otherwise
                if (Thread.currentThread().getContextClassLoader() == facadeLoader) {
                    Thread.currentThread().setContextClassLoader(origCl);
                }

                facadeLoader.close();
                facadeLoader = null;

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to close custom classloader", e);
        }
    }
}
