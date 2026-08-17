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

/** Adds diagnostic boundaries around NascentGravityWell readResolve restoration. */
public final class PatchNascentGravityWellLoadDiag {
    private static final String TARGET = "com/fs/starfarer/campaign/NascentGravityWell.class";
    private static final String BASE = "com/fs/starfarer/campaign/BaseCampaignEntity";
    private static final String SPRITE = "com/fs/graphics/Sprite";
    private static final String HELPER = "com/fs/starfarer/BrowserNascentGravityWellLoadDiag";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException(
                "usage: PatchNascentGravityWellLoadDiag input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, superCalls = {0}, spriteCalls = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> es = jar.entries();
            while (es.hasMoreElements()) {
                JarEntry e = es.nextElement();
                JarEntry copy = new JarEntry(e.getName());
                copy.setTime(e.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(e)) { bytes = readAll(in); }
                if (TARGET.equals(e.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, superCalls, spriteCalls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || superCalls[0] != 1 || spriteCalls[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("NascentGravityWell diag mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " superCalls=" + superCalls[0]
                    + " spriteCalls=" + spriteCalls[0]);
        }
        System.out.println("Patched NascentGravityWell load diagnostics super=1 sprite=1");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] superCalls, int[] spriteCalls) {
        ClassReader r = new ClassReader(input);
        ClassWriter w = new ClassWriter(r, ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9, w) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String sig, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
                if (!"readResolve".equals(name) || !"()Ljava/lang/Object;".equals(desc)) return mv;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private void mark(String phase) {
                        super.visitLdcInsn(phase);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "mark",
                                "(Ljava/lang/String;)V", false);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESPECIAL && BASE.equals(owner)
                                && "readResolve".equals(methodName)
                                && "()Ljava/lang/Object;".equals(descriptor)) {
                            mark("super-before");
                            super.visitMethodInsn(opcode, owner, methodName, descriptor, isInterface);
                            mark("super-after");
                            superCalls[0]++;
                            return;
                        }
                        if (opcode == Opcodes.INVOKESPECIAL && SPRITE.equals(owner)
                                && "<init>".equals(methodName)
                                && "(Ljava/lang/String;)V".equals(descriptor)) {
                            mark("sprite-before");
                            super.visitMethodInsn(opcode, owner, methodName, descriptor, isInterface);
                            mark("sprite-after");
                            spriteCalls[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, descriptor, isInterface);
                    }
                };
            }
        }, 0);
        return w.toByteArray();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[65536];
        int n;
        while ((n = in.read(b)) >= 0) out.write(b, 0, n);
        return out.toByteArray();
    }
}
