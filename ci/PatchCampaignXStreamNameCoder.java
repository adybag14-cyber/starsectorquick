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

/** Uses the exact memoizing NameCoder only for CampaignGameManager's StAX driver. */
public final class PatchCampaignXStreamNameCoder {
    private static final String TARGET = "com/fs/starfarer/campaign/save/CampaignGameManager$5.class";
    private static final String HELPER = "com/fs/starfarer/BrowserMemoizingNameCoder";
    private static final String DRIVER = "com/thoughtworks/xstream/io/xml/StaxDriver";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchCampaignXStreamNameCoder input.jar output.jar");
        final Path input = Path.of(args[0]), output = Path.of(args[1]);
        final int[] classes = {0}, ctors = {0}, replaced = {0};
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
                    bytes = patch(bytes, ctors, replaced);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || ctors[0] != 1 || replaced[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("NameCoder patch shape mismatch classes=" + classes[0]
                    + " ctors=" + ctors[0] + " replaced=" + replaced[0]);
        }
        System.out.println("Patched campaign XStream StAX NameCoder constructor=1");
    }

    private static byte[] patch(byte[] input, int[] ctors, int[] replaced) {
        final ClassReader reader = new ClassReader(input);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                final MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if (!"<init>".equals(name) || !"()V".equals(desc)) return delegate;
                ctors[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                        if (opcode == Opcodes.INVOKESPECIAL && DRIVER.equals(owner)
                                && "<init>".equals(n) && "()V".equals(d)) {
                            super.visitTypeInsn(Opcodes.NEW, HELPER);
                            super.visitInsn(Opcodes.DUP);
                            super.visitMethodInsn(Opcodes.INVOKESPECIAL, HELPER, "<init>", "()V", false);
                            super.visitMethodInsn(Opcodes.INVOKESPECIAL, DRIVER, "<init>",
                                    "(Lcom/thoughtworks/xstream/io/naming/NameCoder;)V", false);
                            replaced[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, n, d, itf);
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
