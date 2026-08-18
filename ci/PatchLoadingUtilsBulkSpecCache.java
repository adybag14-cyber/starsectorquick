import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Adds a stock-browser bulk-cache fast path to LoadingUtils' merged JSON reader. */
public final class PatchLoadingUtilsBulkSpecCache {
    private static final String TARGET = "com/fs/starfarer/loading/LoadingUtils.class";
    private static final String LOADING_UTILS = "com/fs/starfarer/loading/LoadingUtils";
    private static final String CACHE = "com/fs/starfarer/loading/BrowserSpecCache";
    private static final String METHOD = "super";
    private static final String DESC = "(Ljava/lang/String;Ljava/util/Set;)Lorg/json/JSONObject;";
    private static final String PARSER_DESC = "(Ljava/lang/String;Ljava/lang/String;)Lorg/json/JSONObject;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchLoadingUtilsBulkSpecCache input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = readAll(in);
                }
                if (TARGET.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "LoadingUtils bulk-cache patch incomplete classes=" + classes[0] + " methods=" + methods[0]);
        }
        System.out.println("Patched LoadingUtils bulk spec cache methods=" + methods[0]);
    }

    private static byte[] patch(byte[] input, int[] methods) {
        int existingFastPaths = countExistingFastPaths(input);
        if (existingFastPaths > 1) {
            throw new IllegalStateException("LoadingUtils already contains duplicate bulk-cache fast paths=" + existingFastPaths);
        }
        if (existingFastPaths == 1) {
            methods[0]++;
            System.out.println("LoadingUtils bulk spec cache fast path already present; leaving bytecode unchanged.");
            return input;
        }

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label stockPath = new Label();
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                CACHE,
                                "getRaw",
                                "(Ljava/lang/String;)Ljava/lang/String;",
                                false);
                        super.visitVarInsn(Opcodes.ASTORE, 2);
                        super.visitVarInsn(Opcodes.ALOAD, 2);
                        super.visitJumpInsn(Opcodes.IFNULL, stockPath);
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitVarInsn(Opcodes.ALOAD, 2);
                        super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                CACHE,
                                "parseRaw",
                                PARSER_DESC,
                                false);
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitLabel(stockPath);
                        super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }


    private static int countExistingFastPaths(byte[] input) {
        int[] matches = {0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && CACHE.equals(owner)
                                && "getRaw".equals(methodName)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(methodDescriptor)) {
                            matches[0]++;
                        }
                    }
                };
            }
        }, 0);
        return matches[0];
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}