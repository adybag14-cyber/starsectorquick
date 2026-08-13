import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structurally verify ResourceLoaderState quick-start and stage instrumentation. */
public final class VerifyResourceLoaderQuickStart {
    private static final String TARGET = "com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String QUICK_METHOD = "queueShipAndWeaponSprites";
    private static final String QUICK_DESC = "()V";
    private static final String QUEUE_METHOD = "queueResource";
    private static final String QUEUE_DESC =
            "(Lcom/fs/starfarer/loading/ResourceLoaderState$o;Ljava/lang/String;I)V";
    private static final Set<String> EXPECTED_DEFERRED_PREFIXES = Set.of(
            "graphics/damage/",
            "graphics/debris/",
            "graphics/asteroids/",
            "graphics/warroom/");
    private static final String INIT_METHOD = "init";
    private static final String INIT_DESC = "(Ljava/util/Map;)V";
    private static final String PROPERTY = "starsector.browserQuickResourceLoad";
    private static final String MARKER =
            "BrowserResourceLoader: deferred eager ship/weapon/projectile sprite preload.";
    private static final Set<String> EXPECTED_STAGES = Set.of(
            "init-start",
            "settings-resource-queue-ready",
            "spec-store-ready",
            "dynamic-sprite-pass-ready",
            "resource-predecode-ready",
            "queued-resource-load-start",
            "resource-queue-loop-complete",
            "graphics-finalize-ready",
            "finalizers-ready");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: VerifyResourceLoaderQuickStart patched.jar");
        }
        int[] quickMethods = {0};
        int[] queueMethods = {0};
        int[] initMethods = {0};
        int[] propertyLoads = {0};
        int[] queuePropertyLoads = {0};
        int[] booleanChecks = {0};
        int[] markerLoads = {0};
        int[] returns = {0};
        int[] stockSpecCalls = {0};
        int[] queueBooleanChecks = {0};
        int[] queueStartsWithCalls = {0};
        int[] queueReturns = {0};
        int[] stockQueueAdds = {0};
        Set<String> deferredPrefixes = new HashSet<>();
        Set<String> stages = new HashSet<>();

        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry(TARGET);
            if (entry == null) throw new AssertionError("missing " + TARGET);
            try (InputStream in = jar.getInputStream(entry)) {
                ClassReader reader = new ClassReader(in);
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        final boolean quick = QUICK_METHOD.equals(name) && QUICK_DESC.equals(descriptor);
                        final boolean queue = QUEUE_METHOD.equals(name) && QUEUE_DESC.equals(descriptor);
                        final boolean init = INIT_METHOD.equals(name) && INIT_DESC.equals(descriptor);
                        if (!quick && !queue && !init) return null;
                        if (quick) quickMethods[0]++;
                        if (queue) queueMethods[0]++;
                        if (init) initMethods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitLdcInsn(Object value) {
                                if (quick && PROPERTY.equals(value)) propertyLoads[0]++;
                                if (queue && PROPERTY.equals(value)) queuePropertyLoads[0]++;
                                if (queue && value instanceof String
                                        && EXPECTED_DEFERRED_PREFIXES.contains((String) value)) {
                                    deferredPrefixes.add((String) value);
                                }
                                if (quick && MARKER.equals(value)) markerLoads[0]++;
                                if (init && value instanceof String) {
                                    String text = (String) value;
                                    String prefix = "BrowserResourceLoaderStage: ";
                                    if (text.startsWith(prefix)) stages.add(text.substring(prefix.length()));
                                }
                            }

                            @Override
                            public void visitInsn(int opcode) {
                                if (quick && opcode == Opcodes.RETURN) returns[0]++;
                                if (queue && opcode == Opcodes.RETURN) queueReturns[0]++;
                            }

                            @Override
                            public void visitMethodInsn(int opcode, String owner, String methodName,
                                                        String methodDescriptor, boolean isInterface) {
                                if (quick && opcode == Opcodes.INVOKESTATIC
                                        && "java/lang/Boolean".equals(owner)
                                        && "getBoolean".equals(methodName)
                                        && "(Ljava/lang/String;)Z".equals(methodDescriptor)) {
                                    booleanChecks[0]++;
                                }
                                if (quick && "com/fs/starfarer/loading/o00O".equals(owner)) {
                                    stockSpecCalls[0]++;
                                }
                                if (queue && opcode == Opcodes.INVOKESTATIC
                                        && "java/lang/Boolean".equals(owner)
                                        && "getBoolean".equals(methodName)
                                        && "(Ljava/lang/String;)Z".equals(methodDescriptor)) {
                                    queueBooleanChecks[0]++;
                                }
                                if (queue && opcode == Opcodes.INVOKEVIRTUAL
                                        && "java/lang/String".equals(owner)
                                        && "startsWith".equals(methodName)
                                        && "(Ljava/lang/String;)Z".equals(methodDescriptor)) {
                                    queueStartsWithCalls[0]++;
                                }
                                if (queue && opcode == Opcodes.INVOKEINTERFACE
                                        && "java/util/List".equals(owner)
                                        && "add".equals(methodName)) {
                                    stockQueueAdds[0]++;
                                }
                            }
                        };
                    }
                }, 0);
            }
        }

        if (quickMethods[0] != 1 || queueMethods[0] != 1 || initMethods[0] != 1
                || propertyLoads[0] != 1 || queuePropertyLoads[0] != 1
                || booleanChecks[0] != 1 || queueBooleanChecks[0] != 1
                || queueStartsWithCalls[0] != EXPECTED_DEFERRED_PREFIXES.size()
                || markerLoads[0] != 1 || returns[0] < 2 || queueReturns[0] < 2
                || stockSpecCalls[0] < 2 || stockQueueAdds[0] < 1
                || !deferredPrefixes.equals(EXPECTED_DEFERRED_PREFIXES)
                || !stages.equals(EXPECTED_STAGES)) {
            throw new AssertionError(
                    "quick-loader structure mismatch quickMethods=" + quickMethods[0]
                            + " queueMethods=" + queueMethods[0]
                            + " initMethods=" + initMethods[0]
                            + " propertyLoads=" + propertyLoads[0]
                            + " queuePropertyLoads=" + queuePropertyLoads[0]
                            + " booleanChecks=" + booleanChecks[0]
                            + " queueBooleanChecks=" + queueBooleanChecks[0]
                            + " queueStartsWithCalls=" + queueStartsWithCalls[0]
                            + " markerLoads=" + markerLoads[0]
                            + " returns=" + returns[0]
                            + " queueReturns=" + queueReturns[0]
                            + " stockSpecCalls=" + stockSpecCalls[0]
                            + " stockQueueAdds=" + stockQueueAdds[0]
                            + " deferredPrefixes=" + deferredPrefixes
                            + " stages=" + stages);
        }
        System.out.println(
                "VerifyResourceLoaderQuickStart: OK quickMethods=" + quickMethods[0]
                        + " queueMethods=" + queueMethods[0]
                        + " deferredPrefixes=" + deferredPrefixes.size()
                        + " loaderStages=" + stages.size()
                        + " stockSpecCalls=" + stockSpecCalls[0]);
    }
}
