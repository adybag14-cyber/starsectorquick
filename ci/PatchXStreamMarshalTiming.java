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

/** Diagnostic-only timing hooks around AbstractReferenceMarshaller.convert(). */
public final class PatchXStreamMarshalTiming {
    private static final String TARGET = "com/thoughtworks/xstream/core/AbstractReferenceMarshaller.class";
    private static final String HELPER = "com/fs/starfarer/BrowserXStreamMarshalDiag";
    private static final String CONVERT_DESC = "(Ljava/lang/Object;Lcom/thoughtworks/xstream/converters/Converter;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchXStreamMarshalTiming input.jar output.jar");
        final Path input = Path.of(args[0]);
        final Path output = Path.of(args[1]);
        final int[] classes = {0};
        final int[] methods = {0};
        final int[] enters = {0};
        final int[] exits = {0};
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
                    bytes = patch(bytes, methods, enters, exits);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || enters[0] != 1 || exits[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("XStream marshal timing shape mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " enters=" + enters[0] + " exits=" + exits[0]);
        }
        System.out.println("Patched XStream marshal timing methods=1 enters=1 exits=1");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] enters, int[] exits) {
        final ClassReader reader = new ClassReader(input);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                final MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if (!"convert".equals(name) || !CONVERT_DESC.equals(desc)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitVarInsn(Opcodes.ALOAD, 2);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "enter",
                                "(Ljava/lang/Object;Lcom/thoughtworks/xstream/converters/Converter;)V", false);
                        enters[0]++;
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN || opcode == Opcodes.ATHROW) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "exit", "()V", false);
                            exits[0]++;
                        }
                        super.visitInsn(opcode);
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
