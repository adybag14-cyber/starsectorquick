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

/** Routes Starsector's empty-indent save XML wrapper through a browser specialization. */
public final class PatchCampaignSaveZeroIndentWriter {
    private static final String TARGET =
            "com/fs/starfarer/campaign/save/CampaignGameManager$5.class";
    private static final String OLD =
            "com/sun/xml/txw2/output/IndentingXMLStreamWriter";
    private static final String REPLACEMENT =
            "com/fs/starfarer/BrowserZeroIndentXMLStreamWriter";
    private static final String CTOR_DESC = "(Ljavax/xml/stream/XMLStreamWriter;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchCampaignSaveZeroIndentWriter input.jar output.jar");
        }
        final Path input = Path.of(args[0]);
        final Path output = Path.of(args[1]);
        final int[] classes = {0};
        final int[] news = {0};
        final int[] ctors = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = readAll(in);
                }
                if (TARGET.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, news, ctors);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || news[0] != 1 || ctors[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "zero-indent writer patch shape mismatch classes=" + classes[0]
                            + " new=" + news[0] + " ctor=" + ctors[0]);
        }
        System.out.println("Patched campaign save zero-indent writer new=" + news[0] + " ctor=" + ctors[0]);
    }

    private static byte[] patch(byte[] input, int[] news, int[] ctors) {
        final ClassReader reader = new ClassReader(input);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                final MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"createWriter".equals(name)
                        || !"(Ljava/io/OutputStream;)Lcom/thoughtworks/xstream/io/HierarchicalStreamWriter;".equals(descriptor)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW && OLD.equals(type)) {
                            news[0]++;
                            super.visitTypeInsn(opcode, REPLACEMENT);
                            return;
                        }
                        super.visitTypeInsn(opcode, type);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESPECIAL
                                && OLD.equals(owner)
                                && "<init>".equals(methodName)
                                && CTOR_DESC.equals(methodDescriptor)) {
                            ctors[0]++;
                            super.visitMethodInsn(opcode, REPLACEMENT, methodName, methodDescriptor, false);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
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
