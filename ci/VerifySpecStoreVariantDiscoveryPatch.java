import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structural guard for the browser variant-discovery transform. */
public final class VerifySpecStoreVariantDiscoveryPatch {
    private static final String TARGET = "com/fs/starfarer/loading/SpecStore.class";
    private static final String LOADING_UTILS = "com/fs/starfarer/loading/LoadingUtils";
    private static final String CACHE = "com/fs/starfarer/loading/BrowserSpecCache";
    private static final String METHOD = "oO0000";
    private static final String DESC = "()V";
    private static final String FILE_LIST_DESC = "(Ljava/lang/String;Ljava/lang/String;)Ljava/util/List;";
    private static final String DIRECTORY_LIST_DESC = "(Ljava/lang/String;)Ljava/util/List;";
    private static final String HELPER_DESC = "(Ljava/util/List;)Ljava/util/List;";

    private VerifySpecStoreVariantDiscoveryPatch() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: VerifySpecStoreVariantDiscoveryPatch starfarer.jar");
        }

        int targetClasses = 0;
        boolean helperClass = false;
        Shape shape = new Shape();
        try (JarFile jar = new JarFile(args[0])) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if ("com/fs/starfarer/loading/BrowserSpecCache.class".equals(entry.getName())) {
                    helperClass = true;
                }
                if (!TARGET.equals(entry.getName())) continue;
                targetClasses++;
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = readAll(in);
                }
                inspect(bytes, shape);
            }
        }

        List<String> expectedSequence = Arrays.asList("file", "expand", "directory", "filter", "file");
        if (targetClasses != 1
                || !helperClass
                || shape.targetMethods != 1
                || shape.fileCalls != 2
                || shape.directoryCalls != 1
                || shape.expandCalls != 1
                || shape.filterCalls != 1
                || !shape.sequence.equals(expectedSequence)
                || !shape.sawVariantRoot
                || !shape.sawVariantExtension
                || !shape.sawVariantLoadLog
                || !shape.sawVariantId) {
            throw new AssertionError(
                    "invalid SpecStore variant discovery patch classes=" + targetClasses
                            + " helperClass=" + helperClass
                            + " shape=" + shape
                            + " sequence=" + shape.sequence);
        }
        System.out.println("VerifySpecStoreVariantDiscoveryPatch: OK sequence=" + shape.sequence);
    }

    private static void inspect(byte[] bytes, Shape shape) {
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return null;
                shape.targetMethods++;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if ("data/variants".equals(value)) shape.sawVariantRoot = true;
                        if ("variant".equals(value)) shape.sawVariantExtension = true;
                        if ("Loading ship & fighter variants".equals(value)) shape.sawVariantLoadLog = true;
                        if ("variantId".equals(value)) shape.sawVariantId = true;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && LOADING_UTILS.equals(owner)) {
                            if ("super".equals(methodName) && FILE_LIST_DESC.equals(methodDescriptor)) {
                                shape.fileCalls++;
                                shape.sequence.add("file");
                            } else if (DIRECTORY_LIST_DESC.equals(methodDescriptor)) {
                                shape.directoryCalls++;
                                shape.sequence.add("directory");
                            }
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && CACHE.equals(owner)
                                && HELPER_DESC.equals(methodDescriptor)) {
                            if ("expandVariantPaths".equals(methodName)) {
                                shape.expandCalls++;
                                shape.sequence.add("expand");
                            } else if ("filterVariantDirectories".equals(methodName)) {
                                shape.filterCalls++;
                                shape.sequence.add("filter");
                            }
                        }
                    }
                };
            }
        }, 0);
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private static final class Shape {
        int targetMethods;
        int fileCalls;
        int directoryCalls;
        int expandCalls;
        int filterCalls;
        boolean sawVariantRoot;
        boolean sawVariantExtension;
        boolean sawVariantLoadLog;
        boolean sawVariantId;
        final List<String> sequence = new ArrayList<String>();

        @Override
        public String toString() {
            return "targetMethods=" + targetMethods
                    + " fileCalls=" + fileCalls
                    + " directoryCalls=" + directoryCalls
                    + " expandCalls=" + expandCalls
                    + " filterCalls=" + filterCalls
                    + " root=" + sawVariantRoot
                    + " extension=" + sawVariantExtension
                    + " log=" + sawVariantLoadLog
                    + " variantId=" + sawVariantId;
        }
    }
}
