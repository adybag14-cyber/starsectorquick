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

/** Queue stock ImageIO predecode as resources are registered and start it before SpecStore. */
public final class PatchResourceLoaderEarlyPredecode {
    private static final String ENTRY = "com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String HELPER = "com/fs/starfarer/BrowserDeferredTextureQueue";
    private static final String TYPE = "com/fs/starfarer/loading/ResourceLoaderState$o";
    private static final String QUEUE_METHOD = "queueResource";
    private static final String QUEUE_DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState$o;Ljava/lang/String;I)V";
    private static final String INIT_METHOD = "init";
    private static final String INIT_DESC = "(Ljava/util/Map;)V";
    private static final String SPECSTORE = "com/fs/starfarer/loading/SpecStore";
    private static final String SPEC_METHOD = "public";
    private static final String SPEC_DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchResourceLoaderEarlyPredecode input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, queueMethods = {0}, initMethods = {0}, earlyCalls = {0}, startCalls = {0};
        try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (ENTRY.equals(entry.getName())) { classes[0]++; bytes = patch(bytes, queueMethods, initMethods, earlyCalls, startCalls); }
                out.write(bytes); out.closeEntry();
            }
        }
        if (classes[0] != 1 || queueMethods[0] != 1 || initMethods[0] != 1 || earlyCalls[0] != 1 || startCalls[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("early predecode patch mismatch classes=" + classes[0] + " queueMethods=" + queueMethods[0]
                    + " initMethods=" + initMethods[0] + " earlyCalls=" + earlyCalls[0] + " startCalls=" + startCalls[0]);
        }
        System.out.println("Patched ResourceLoader early image predecode queue=1 start=1");
    }

    private static byte[] patch(byte[] input, int[] queueMethods, int[] initMethods, int[] earlyCalls, int[] startCalls) {
        int[] existing = {0, 0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                        if (op == Opcodes.INVOKESTATIC && HELPER.equals(owner)) {
                            if ("queueEarlyImagePredecode".equals(name) && "(Ljava/lang/String;I)V".equals(desc)) existing[0]++;
                            if ("startEarlyImagePredecode".equals(name) && "()V".equals(desc)) existing[1]++;
                        }
                    }
                };
            }
        }, 0);
        if (existing[0] == 1 && existing[1] == 1) {
            queueMethods[0] = 1; initMethods[0] = 1; earlyCalls[0] = 1; startCalls[0] = 1; return input;
        }
        if (existing[0] != 0 || existing[1] != 0) throw new IllegalStateException("partial early predecode patch existing=" + existing[0] + "/" + existing[1]);

        ClassReader reader = new ClassReader(input); ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                if (QUEUE_METHOD.equals(name) && QUEUE_DESC.equals(desc)) {
                    queueMethods[0]++;
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            super.visitVarInsn(Opcodes.ALOAD, 2);
                            super.visitVarInsn(Opcodes.ALOAD, 1);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TYPE, "ordinal", "()I", false);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "queueEarlyImagePredecode", "(Ljava/lang/String;I)V", false);
                            earlyCalls[0]++;
                        }
                    };
                }
                if (INIT_METHOD.equals(name) && INIT_DESC.equals(desc)) {
                    initMethods[0]++;
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitMethodInsn(int op, String owner, String method, String d, boolean itf) {
                            if (op == Opcodes.INVOKESTATIC && SPECSTORE.equals(owner) && SPEC_METHOD.equals(method) && SPEC_DESC.equals(d)) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "startEarlyImagePredecode", "()V", false);
                                startCalls[0]++;
                            }
                            super.visitMethodInsn(op, owner, method, d, itf);
                        }
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[65536]; int n;
        while ((n = in.read(b)) >= 0) out.write(b, 0, n); return out.toByteArray();
    }
}
