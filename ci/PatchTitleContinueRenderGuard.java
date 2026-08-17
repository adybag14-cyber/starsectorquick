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

/** Routes only Continue's two pre-load title renders through a browser NPE guard. */
public final class PatchTitleContinueRenderGuard {
    private static final String TARGET = "com/fs/starfarer/title/TitleScreenState.class";
    private static final String TARGET_OWNER = "com/fs/starfarer/title/TitleScreenState";
    private static final String MENU_DESC = "(Lcom/fs/starfarer/title/ooOO$o$o;)V";
    private static final String HELPER = "com/fs/starfarer/BrowserTitleContinueCompat";
    private static final String HELPER_DESC = "(Lcom/fs/starfarer/title/TitleScreenState;F)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchTitleContinueRenderGuard input.jar output.jar");
        final Path input = Path.of(args[0]);
        final Path output = Path.of(args[1]);
        final int[] classes = {0};
        final int[] methods = {0};
        final int[] replaced = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (TARGET.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, replaced);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || replaced[0] != 2) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("Continue render guard shape mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " renders=" + replaced[0]);
        }
        System.out.println("Patched TitleScreenState Continue pre-load render calls=" + replaced[0]);
    }

    private static byte[] patch(byte[] input, int[] methods, int[] replaced) {
        final ClassReader reader = new ClassReader(input);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                final MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if (!"menuItemSelected".equals(name) || !MENU_DESC.equals(desc)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDesc, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && TARGET_OWNER.equals(owner)
                                && "render".equals(methodName)
                                && "(F)V".equals(methodDesc)
                                && replaced[0] < 2) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER,
                                    "renderBeforeContinue", HELPER_DESC, false);
                            replaced[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
