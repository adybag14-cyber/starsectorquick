import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Skips the three optional special-location generators run by
 * CoreLifecyclePluginImpl.onNewGame(). The browser compatibility SectorGen can
 * intentionally finish with no vanilla star systems when Planet construction is
 * unavailable; these add-on generators assume those systems exist and otherwise
 * abort campaign creation (first observed in TTBlackSite.generate()).
 *
 * The lifecycle callback itself is preserved; only the three generate(SectorAPI)
 * calls are removed. This keeps AppDriver/game state ownership untouched.
 */
public final class PatchCoreLifecycleBrowserWorld {
    private static final String TARGET =
            "com/fs/starfarer/api/impl/campaign/CoreLifecyclePluginImpl.class";
    private static final String GENERATE_DESC =
            "(Lcom/fs/starfarer/api/campaign/SectorAPI;)V";
    private static final Set<String> OPTIONAL_WORLD_GENERATORS = new HashSet<String>(
            Arrays.asList(
                    "com/fs/starfarer/api/impl/campaign/world/TTBlackSite",
                    "com/fs/starfarer/api/impl/campaign/world/Limbo",
                    "com/fs/starfarer/api/impl/campaign/world/GateHaulerLocation"));

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCoreLifecycleBrowserWorld input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] skipped = new int[] {0};

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
                    bytes = patch(bytes, skipped);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (skipped[0] != 3) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly 3 optional onNewGame world-generator calls, found "
                            + skipped[0]);
        }
        System.out.println(
                "Patched CoreLifecyclePluginImpl optional browser world generators=" + skipped[0]);
    }

    private static byte[] patch(byte[] input, int[] skipped) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                if (!"onNewGame".equals(name) || !"()V".equals(descriptor)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && OPTIONAL_WORLD_GENERATORS.contains(owner)
                                && "generate".equals(methodName)
                                && GENERATE_DESC.equals(methodDescriptor)) {
                            // Stack contains [generator, sector]. Both are category-1 refs.
                            super.visitInsn(Opcodes.POP2);
                            skipped[0]++;
                            return;
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
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
