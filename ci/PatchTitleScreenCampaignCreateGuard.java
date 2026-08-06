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

/**
 * Stops the title-screen background combat simulation while a direct campaign
 * create is mutating Starsector's global campaign/combat state on a worker.
 *
 * Rendering and Display.update() continue, so CheerpJ/the browser remains alive;
 * only TitleScreenState.advance(float) is suppressed during the critical window.
 */
public final class PatchTitleScreenCampaignCreateGuard {
    private static final String TARGET = "com/fs/starfarer/TitleScreenState.class";
    private static final String PROPERTY = "starsector.campaignCreateInProgress";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchTitleScreenCampaignCreateGuard input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] patched = new int[] {0};
        int[] classSeen = new int[] {0};

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
                    classSeen[0]++;
                    bytes = patch(bytes, patched);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classSeen[0] != 1 || patched[0] < 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "TitleScreenState guard not applied: classSeen="
                            + classSeen[0]
                            + " advanceMethods="
                            + patched[0]);
        }
        System.out.println(
                "Patched TitleScreenState campaign-create advance guards=" + patched[0]);
    }

    private static byte[] patch(byte[] input, int[] patched) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new SafeClassWriter(
                reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"advance".equals(name) || !"(F)V".equals(descriptor)) {
                    return delegate;
                }
                patched[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label continueAdvance = new Label();
                        super.visitLdcInsn(PROPERTY);
                        super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "java/lang/Boolean",
                                "getBoolean",
                                "(Ljava/lang/String;)Z",
                                false);
                        super.visitJumpInsn(Opcodes.IFEQ, continueAdvance);
                        super.visitInsn(Opcodes.RETURN);
                        super.visitLabel(continueAdvance);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(ClassReader reader, int flags) {
            super(reader, flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
