import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structurally verify the LoadingUtils bulk-cache fast path and stock fallback body. */
public final class VerifyLoadingUtilsBulkSpecPatch {
    private static final String TARGET = "com/fs/starfarer/loading/LoadingUtils.class";
    private static final String DESC = "(Ljava/lang/String;Ljava/util/Set;)Lorg/json/JSONObject;";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyLoadingUtilsBulkSpecPatch patched.jar");
        int[] targetMethods = {0};
        int[] cacheCalls = {0};
        int[] parserCalls = {0};
        int[] originalCalls = {0};
        boolean[] helperClass = {false};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            helperClass[0] = jar.getJarEntry("com/fs/starfarer/loading/BrowserSpecCache.class") != null;
            JarEntry entry = jar.getJarEntry(TARGET);
            if (entry == null) throw new AssertionError("missing " + TARGET);
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        if (!"super".equals(name) || !DESC.equals(descriptor)) return null;
                        targetMethods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String methodName,
                                                        String methodDescriptor, boolean isInterface) {
                                if ("com/fs/starfarer/loading/BrowserSpecCache".equals(owner)
                                        && "getRaw".equals(methodName)) cacheCalls[0]++;
                                if ("com/fs/starfarer/loading/LoadingUtils".equals(owner)
                                        && "\u00d600000".equals(methodName)
                                        && "(Ljava/lang/String;Ljava/lang/String;)Lorg/json/JSONObject;".equals(methodDescriptor)) {
                                    parserCalls[0]++;
                                }
                                if (!"com/fs/starfarer/loading/BrowserSpecCache".equals(owner)
                                        && !"com/fs/starfarer/loading/LoadingUtils".equals(owner)) {
                                    originalCalls[0]++;
                                }
                            }
                        };
                    }
                }, 0);
            }
        }
        if (!helperClass[0] || targetMethods[0] != 1 || cacheCalls[0] != 1 || parserCalls[0] < 1 || originalCalls[0] < 3) {
            throw new AssertionError(
                    "bulk patch mismatch helper=" + helperClass[0]
                            + " methods=" + targetMethods[0]
                            + " cacheCalls=" + cacheCalls[0]
                            + " parserCalls=" + parserCalls[0]
                            + " originalCalls=" + originalCalls[0]);
        }
        System.out.println(
                "VerifyLoadingUtilsBulkSpecPatch: OK cacheCalls=" + cacheCalls[0]
                        + " parserCalls=" + parserCalls[0]
                        + " originalCalls=" + originalCalls[0]);
    }
}