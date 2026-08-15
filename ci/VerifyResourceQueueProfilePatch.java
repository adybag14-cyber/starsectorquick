import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies diagnostic hooks while proving the stock queue/load calls remain. */
public final class VerifyResourceQueueProfilePatch {
    private VerifyResourceQueueProfilePatch() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: VerifyResourceQueueProfilePatch patched.jar");
        }
        final int[] queue = {0};
        final int[] init = {0};
        final int[] record = {0};
        final int[] summary = {0};
        final int[] begin = {0};
        final int[] end = {0};
        final int[] loadSummary = {0};
        final int[] executor = {0};
        final int[] textureLoads = {0};

        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry("com/fs/starfarer/loading/ResourceLoaderState.class");
            if (entry == null) throw new AssertionError("missing ResourceLoaderState");
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        if ("queueResource".equals(name)
                                && "(Lcom/fs/starfarer/loading/ResourceLoaderState$o;Ljava/lang/String;I)V".equals(descriptor)) {
                            queue[0]++;
                        }
                        if ("init".equals(name) && "(Ljava/util/Map;)V".equals(descriptor)) {
                            init[0]++;
                        }
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String methodName,
                                                        String methodDescriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKESTATIC
                                        && "com/fs/starfarer/BrowserResourceQueueProfile".equals(owner)) {
                                    if ("record".equals(methodName)) record[0]++;
                                    else if ("printQueueSummary".equals(methodName)) summary[0]++;
                                    else if ("beginLoad".equals(methodName)) begin[0]++;
                                    else if ("endLoad".equals(methodName)) end[0]++;
                                    else if ("printLoadSummary".equals(methodName)) loadSummary[0]++;
                                }
                                if (opcode == Opcodes.INVOKESTATIC
                                        && "java/util/concurrent/Executors".equals(owner)
                                        && "newFixedThreadPool".equals(methodName)) {
                                    executor[0]++;
                                }
                                if (opcode == Opcodes.INVOKESTATIC
                                        && "com/fs/graphics/oOoO".equals(owner)
                                        && "o00000".equals(methodName)
                                        && "(Ljava/lang/String;Ljava/lang/String;)V".equals(methodDescriptor)) {
                                    textureLoads[0]++;
                                }
                            }
                        };
                    }
                }, 0);
            }
        }

        if (queue[0] != 1 || init[0] != 1 || record[0] != 1 || summary[0] != 1
                || begin[0] != 1 || end[0] != 2 || loadSummary[0] != 1
                || executor[0] != 1 || textureLoads[0] < 5) {
            throw new AssertionError(
                    "queue profile structure mismatch queue=" + queue[0]
                            + " init=" + init[0]
                            + " record=" + record[0]
                            + " summary=" + summary[0]
                            + " begin=" + begin[0]
                            + " end=" + end[0]
                            + " loadSummary=" + loadSummary[0]
                            + " executor=" + executor[0]
                            + " textureLoads=" + textureLoads[0]);
        }
        System.out.println("VerifyResourceQueueProfilePatch: OK stock queue/load calls retained with diagnostic hooks");
    }
}
