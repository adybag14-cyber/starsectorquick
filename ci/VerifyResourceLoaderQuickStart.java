import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structurally verify the ResourceLoaderState quick-start guard using ASM. */
public final class VerifyResourceLoaderQuickStart {
    private static final String TARGET = "com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String METHOD = "queueShipAndWeaponSprites";
    private static final String DESC = "()V";
    private static final String PROPERTY = "starsector.browserQuickResourceLoad";
    private static final String MARKER =
            "BrowserResourceLoader: deferred eager ship/weapon/projectile sprite preload.";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: VerifyResourceLoaderQuickStart patched.jar");
        }
        int[] methods = {0};
        int[] propertyLoads = {0};
        int[] booleanChecks = {0};
        int[] markerLoads = {0};
        int[] returns = {0};
        int[] stockSpecCalls = {0};

        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry(TARGET);
            if (entry == null) throw new AssertionError("missing " + TARGET);
            try (InputStream in = jar.getInputStream(entry)) {
                ClassReader reader = new ClassReader(in);
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        if (!METHOD.equals(name) || !DESC.equals(descriptor)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitLdcInsn(Object value) {
                                if (PROPERTY.equals(value)) propertyLoads[0]++;
                                if (MARKER.equals(value)) markerLoads[0]++;
                            }

                            @Override
                            public void visitInsn(int opcode) {
                                if (opcode == Opcodes.RETURN) returns[0]++;
                            }

                            @Override
                            public void visitMethodInsn(int opcode, String owner, String methodName,
                                                        String methodDescriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKESTATIC
                                        && "java/lang/Boolean".equals(owner)
                                        && "getBoolean".equals(methodName)
                                        && "(Ljava/lang/String;)Z".equals(methodDescriptor)) {
                                    booleanChecks[0]++;
                                }
                                // Preserve proof that the original eager body still exists behind
                                // the property-off branch; these calls enumerate stock weapon specs.
                                if ("com/fs/starfarer/loading/o00O".equals(owner)) {
                                    stockSpecCalls[0]++;
                                }
                            }
                        };
                    }
                }, 0);
            }
        }

        if (methods[0] != 1 || propertyLoads[0] != 1 || booleanChecks[0] != 1
                || markerLoads[0] != 1 || returns[0] < 2 || stockSpecCalls[0] < 2) {
            throw new AssertionError(
                    "quick-loader structure mismatch methods=" + methods[0]
                            + " propertyLoads=" + propertyLoads[0]
                            + " booleanChecks=" + booleanChecks[0]
                            + " markerLoads=" + markerLoads[0]
                            + " returns=" + returns[0]
                            + " stockSpecCalls=" + stockSpecCalls[0]);
        }
        System.out.println(
                "VerifyResourceLoaderQuickStart: OK methods=" + methods[0]
                        + " returns=" + returns[0]
                        + " stockSpecCalls=" + stockSpecCalls[0]);
    }
}
