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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Profile ResourceLoaderState's synchronous image/font loads without changing order. */
public final class PatchStaticResourceDiagnostics {
    private static final String TARGET = "com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String OWNER = "com/fs/starfarer/loading/ResourceLoaderState";
    private static final String IMAGE_OWNER = "com/fs/graphics/oOoO";
    private static final String IMAGE_METHOD = "o00000";
    private static final String FONT_OWNER = "com/fs/graphics/super/D";
    private static final String FONT_METHOD = "super";
    private static final String LOAD_DESC = "(Ljava/lang/String;Ljava/lang/String;)V";
    private static final String IMAGE_HELPER = "cheerpj$profileImageLoad";
    private static final String FONT_HELPER = "cheerpj$profileFontLoad";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchStaticResourceDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] imageCalls = {0};
        int[] fontCalls = {0};
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
                    bytes = patch(bytes, imageCalls, fontCalls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || imageCalls[0] < 2 || fontCalls[0] < 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "static-resource diagnostics incomplete classes=" + classes[0]
                            + " imageCalls=" + imageCalls[0]
                            + " fontCalls=" + fontCalls[0]);
        }
        System.out.println(
                "Patched static resource diagnostics imageCallSites=" + imageCalls[0]
                        + " fontCallSites=" + fontCalls[0]);
    }

    private static byte[] patch(byte[] input, int[] imageCalls, int[] fontCalls) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            private boolean imageHelperExists;
            private boolean fontHelperExists;

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (IMAGE_HELPER.equals(name) && LOAD_DESC.equals(descriptor)) imageHelperExists = true;
                if (FONT_HELPER.equals(name) && LOAD_DESC.equals(descriptor)) fontHelperExists = true;
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"init".equals(name) || !"(Ljava/util/Map;)V".equals(descriptor)) return delegate;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && LOAD_DESC.equals(methodDescriptor)
                                && IMAGE_OWNER.equals(owner) && IMAGE_METHOD.equals(methodName)) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, IMAGE_HELPER, LOAD_DESC, false);
                            imageCalls[0]++;
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC && LOAD_DESC.equals(methodDescriptor)
                                && FONT_OWNER.equals(owner) && FONT_METHOD.equals(methodName)) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, FONT_HELPER, LOAD_DESC, false);
                            fontCalls[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (!imageHelperExists) emitHelper(
                        super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                                IMAGE_HELPER, LOAD_DESC, null, new String[] {"java/io/IOException"}),
                        IMAGE_OWNER, IMAGE_METHOD, "image");
                if (!fontHelperExists) emitHelper(
                        super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                                FONT_HELPER, LOAD_DESC, null, null),
                        FONT_OWNER, FONT_METHOD, "font");
                super.visitEnd();
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitHelper(MethodVisitor mv, String targetOwner, String targetMethod, String kind) {
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        mv.visitVarInsn(Opcodes.LSTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner, targetMethod, LOAD_DESC, false);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("BrowserStaticResourceLoad: " + kind + ":");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        mv.visitVarInsn(Opcodes.LLOAD, 2);
        mv.visitInsn(Opcodes.LSUB);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn(":");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}