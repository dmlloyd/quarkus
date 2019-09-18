package io.quarkus.deployment.steps;

import java.util.List;

import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.graal.service.ServiceLoaderInstance;

/**
 *
 */
public class NativeServiceLoaderSteps {

    static final String SLIR_NAME = "io.quarkus.runtime.generated.ServiceLoaderInstanceReplacement";
    static final String SLI_IMPL_BASE_NAME = "io.quarkus.runtime.generated.ServiceLoaderInstance_";

    static final MethodDescriptor SLI_IS_EQUAL = MethodDescriptor.ofMethod(ServiceLoaderInstance.class, "isEqual",
            boolean.class, int.class, int.class);
    static final MethodDescriptor OBJ_EQUALS = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class, Object.class);
    static final MethodDescriptor CTOR_EMPTY = MethodDescriptor.ofConstructor(ServiceLoaderInstance.Empty.class, Class.class);

    @BuildStep
    void generateMethod(
            BuildProducer<GeneratedNativeImageClassBuildItem> classProducer,
            List<ServiceProviderBuildItem> serviceProviders) {

        // todo: we should just scan for *every* service provider instead and let DCE clean house

        final io.quarkus.gizmo.ClassOutput classOutput = new ClassOutput() {
            public void write(final String name, final byte[] data) {
                classProducer.produce(new GeneratedNativeImageClassBuildItem(name, data));
            }
        };
        try (ClassCreator cc = ClassCreator.builder().setFinal(true).className(SLIR_NAME).classOutput(classOutput).build()) {
            cc.addAnnotation("com.oracle.svm.core.annotate.TargetClass").addValue("className",
                    ServiceLoaderInstance.class.getName());

            try (MethodCreator mc = cc.getMethodCreator(MethodDescriptor.ofMethod(
                    SLIR_NAME,
                    "getInstance",
                    ServiceLoaderInstance.class,
                    Class.class))) {
                mc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                mc.addAnnotation("com.oracle.svm.core.annotate.Substitute");
                mc.addAnnotation("org.graalvm.compiler.api.replacements.Fold");

                final ResultHandle clazz = mc.getMethodParam(0);

                for (ServiceProviderBuildItem serviceProvider : serviceProviders) {
                    if (!serviceProvider.providers().isEmpty()) {
                        final String serviceInterface = serviceProvider.getServiceInterface();
                        final ResultHandle eq = mc.invokeVirtualMethod(OBJ_EQUALS, clazz, mc.loadClass(serviceInterface));
                        try (BytecodeCreator matched = mc.ifNonZero(eq).trueBranch()) {
                            MethodDescriptor md = generateProviderMethod(serviceProvider, classOutput);
                            matched.returnValue(matched.newInstance(md));
                        }
                    }
                }
                mc.returnValue(mc.newInstance(CTOR_EMPTY, clazz));
            }
        }
    }

    private MethodDescriptor generateProviderMethod(final ServiceProviderBuildItem providerItem,
            final io.quarkus.gizmo.ClassOutput classOutput) {
        final String serviceInterface = providerItem.getServiceInterface();
        final List<String> providers = providerItem.providers();
        final String className = SLI_IMPL_BASE_NAME + serviceInterface.replace('.', '_');
        final MethodDescriptor ctorDesc = MethodDescriptor.ofConstructor(className);
        try (ClassCreator cc = ClassCreator.builder()
                .setFinal(true)
                .className(className)
                .classOutput(classOutput)
                .superClass(ServiceLoaderInstance.class)
                .build()) {

            // constructor
            try (MethodCreator mc = cc.getMethodCreator(ctorDesc)) {
                mc.invokeSpecialMethod(MethodDescriptor.ofConstructor(ServiceLoaderInstance.class, Class.class, int.class),
                        mc.getThis(), mc.loadClass(serviceInterface), mc.load(providers.size()));
                mc.returnValue(null);
            }

            // implementation methods
            try (MethodCreator mc = cc.getMethodCreator(
                    MethodDescriptor.ofMethod(className, "getTypeOf", Class.class, int.class))) {

                final ResultHandle index = mc.getMethodParam(0);
                for (int i = 0; i < providers.size(); i++) {
                    final BytecodeCreator match = mc.ifNonZero(mc.invokeStaticMethod(SLI_IS_EQUAL, index, mc.load(i)))
                            .trueBranch();
                    // just constructors for now
                    match.returnValue(match.loadClass(providers.get(i)));
                }
                mc.returnValue(mc.loadNull());
            }

            try (MethodCreator mc = cc.getMethodCreator(
                    MethodDescriptor.ofMethod(className, "getInstanceOf", Object.class, int.class))) {

                final ResultHandle index = mc.getMethodParam(0);
                for (int i = 0; i < providers.size(); i++) {
                    final BytecodeCreator match = mc.ifNonZero(mc.invokeStaticMethod(SLI_IS_EQUAL, index, mc.load(i)))
                            .trueBranch();
                    // just constructors for now
                    match.returnValue(match.newInstance(MethodDescriptor.ofConstructor(providers.get(i))));
                }
                mc.returnValue(mc.loadNull());
            }
        }
        return ctorDesc;
    }
}
