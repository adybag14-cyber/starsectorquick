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

/**
 * Disables purely cosmetic orbital-junk generation around campaign markets.
 *
 * The browser runtime otherwise creates hundreds of CustomCampaignEntity
 * instances during sector generation. In CheerpJ this path reaches an
 * incompatible readResolve call and aborts Galatia generation. Market data,
 * economy state, fleets, and campaign gameplay do not depend on these decorative
 * debris entities, so the browser build safely omits them.
 */
public final class PatchCampaignOrbitalJunk {
    private static final String TARGET =
            "com/fs/starfarer/api/impl/campaign/CoreLifecyclePluginImpl.class";
    private static final String METHOD = "addJunk";
    private static final String DESCRIPTOR =
            "(Lcom/fs/starfarer/api/campaign/econ/MarketAPI;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCampaignOrbitalJunk input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] replacements = new int[] {0};

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
                    bytes = patch(bytes, replacements);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (replacements[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected one CoreLifecyclePluginImpl.addJunk(MarketAPI) replacement, found "
                            + replacements[0]);
        }
        System.out.println(
                "Patched cosmetic campaign orbital-junk generation methods="
                        + replacements[0]);
    }

    private static byte[] patch(byte[] input, int[] replacements) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor output = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESCRIPTOR.equals(descriptor)) {
                    return output;
                }
                replacements[0]++;
                // Consume the original method events without forwarding them,
                // then emit a valid no-op static method at visitEnd.
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitEnd() {
                        output.visitCode();
                        output.visitInsn(Opcodes.RETURN);
                        output.visitMaxs(0, 1);
                        output.visitEnd();
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
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
