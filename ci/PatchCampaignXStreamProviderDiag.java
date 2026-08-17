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

/** Diagnostic-only: force CampaignGameManager's XStream subclass to use a logging SunUnsafe provider. */
public final class PatchCampaignXStreamProviderDiag {
    private static final String TARGET = "com/fs/starfarer/campaign/save/CampaignGameManager$6.class";
    private static final String XSTREAM = "com/thoughtworks/xstream/XStream";
    private static final String PROVIDER = "com/fs/starfarer/BrowserLoggingSunUnsafeReflectionProvider";
    private static final String DRIVER_DESC = "Lcom/thoughtworks/xstream/io/HierarchicalStreamDriver;";
    private static final String PROVIDER_DESC = "Lcom/thoughtworks/xstream/converters/reflection/ReflectionProvider;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchCampaignXStreamProviderDiag input.jar output.jar");
        final Path input = Path.of(args[0]), output = Path.of(args[1]);
        final int[] classes = {0}, replaced = {0};
        try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final JarEntry copy = new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (TARGET.equals(entry.getName())) { classes[0]++; bytes = patch(bytes, replaced); }
                out.write(bytes); out.closeEntry();
            }
        }
        if (classes[0] != 1 || replaced[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("provider diag shape mismatch classes=" + classes[0] + " replaced=" + replaced[0]);
        }
        System.out.println("Patched campaign XStream provider diagnostics constructors=1");
    }

    private static byte[] patch(byte[] input, int[] replaced) {
        final ClassReader reader = new ClassReader(input);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                final MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if (!"<init>".equals(name) || !"(".concat(DRIVER_DESC).concat(")V").equals(desc)) return delegate;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                        if (opcode == Opcodes.INVOKESPECIAL && XSTREAM.equals(owner) && "<init>".equals(n)
                                && ("(" + DRIVER_DESC + ")V").equals(d)) {
                            super.visitTypeInsn(Opcodes.NEW, PROVIDER);
                            super.visitInsn(Opcodes.DUP);
                            super.visitMethodInsn(Opcodes.INVOKESPECIAL, PROVIDER, "<init>", "()V", false);
                            super.visitInsn(Opcodes.SWAP);
                            super.visitMethodInsn(Opcodes.INVOKESPECIAL, XSTREAM, "<init>",
                                    "(" + PROVIDER_DESC + DRIVER_DESC + ")V", false);
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
        final ByteArrayOutputStream out = new ByteArrayOutputStream(); final byte[] buffer = new byte[65536]; int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read); return out.toByteArray();
    }
}
