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

/** Diagnostic-only instrumentation for XStream provider and reader progress. */
public final class PatchCampaignXStreamLoadDiag {
    private static final String DRIVER = "com/fs/starfarer/campaign/save/CampaignGameManager$5.class";
    private static final String XSTREAM = "com/fs/starfarer/campaign/save/CampaignGameManager$6.class";
    private static final String HELPER = "com/fs/starfarer/BrowserXStreamLoadDiag";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchCampaignXStreamLoadDiag input.jar output.jar");
        final Path input = Path.of(args[0]), output = Path.of(args[1]);
        final int[] driverClasses = {0}, xstreamClasses = {0}, wraps = {0}, providers = {0};
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
                if (DRIVER.equals(entry.getName())) {
                    driverClasses[0]++;
                    bytes = patchDriver(bytes, wraps);
                } else if (XSTREAM.equals(entry.getName())) {
                    xstreamClasses[0]++;
                    bytes = patchXStream(bytes, providers);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (driverClasses[0] != 1 || xstreamClasses[0] != 1 || wraps[0] != 1 || providers[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("XStream load diag shape mismatch driver=" + driverClasses[0]
                    + " xstream=" + xstreamClasses[0] + " wraps=" + wraps[0] + " providers=" + providers[0]);
        }
        System.out.println("Patched campaign XStream load diagnostics readerWraps=1 providerLogs=1");
    }

    private static byte[] patchDriver(byte[] input, int[] wraps) {
        final ClassReader reader = new ClassReader(input);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                final MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if (!"createReader".equals(name)
                        || !"(Ljava/io/InputStream;)Lcom/thoughtworks/xstream/io/HierarchicalStreamReader;".equals(desc)) return delegate;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "wrap",
                                    "(Lcom/thoughtworks/xstream/io/HierarchicalStreamReader;)Lcom/thoughtworks/xstream/io/HierarchicalStreamReader;", false);
                            wraps[0]++;
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] patchXStream(byte[] input, int[] providers) {
        final ClassReader reader = new ClassReader(input);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                final MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if (!"<init>".equals(name)
                        || !"(Lcom/thoughtworks/xstream/io/HierarchicalStreamDriver;)V".equals(desc)) return delegate;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                        super.visitMethodInsn(opcode, owner, n, d, itf);
                        if (opcode == Opcodes.INVOKESPECIAL
                                && "com/thoughtworks/xstream/XStream".equals(owner)
                                && "<init>".equals(n)
                                && "(Lcom/thoughtworks/xstream/io/HierarchicalStreamDriver;)V".equals(d)) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "logProvider",
                                    "(Lcom/thoughtworks/xstream/XStream;)V", false);
                            providers[0]++;
                        }
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
