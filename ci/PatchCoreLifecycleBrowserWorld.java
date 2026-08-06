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
 * Skips optional vanilla world-population calls from CoreLifecyclePluginImpl
 * that require stock star systems to exist.
 *
 * The CheerpJ bootstrap deliberately creates a minimal sector so campaign state
 * can become interactive before desktop-only world generation is supported. The
 * lifecycle callbacks themselves are preserved; only calls that populate or
 * assume the deferred vanilla world are removed.
 */
public final class PatchCoreLifecycleBrowserWorld {
    private static final String TARGET =
            "com/fs/starfarer/api/impl/campaign/CoreLifecyclePluginImpl.class";
    private static final String GENERATE_DESC =
            "(Lcom/fs/starfarer/api/campaign/SectorAPI;)V";
    private static final Set<String> ON_NEW_GAME_WORLD_GENERATORS = new HashSet<String>(
            Arrays.asList(
                    "com/fs/starfarer/api/impl/campaign/world/TTBlackSite",
                    "com/fs/starfarer/api/impl/campaign/world/Limbo",
                    "com/fs/starfarer/api/impl/campaign/world/GateHaulerLocation"));
    private static final String NAMELESS_ROCK =
            "com/fs/starfarer/api/impl/campaign/world/NamelessRock";
    private static final String CUSTOM_FLEETS =
            "com/fs/starfarer/api/impl/campaign/fleets/CustomFleets";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCoreLifecycleBrowserWorld input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] onNewGameSkipped = new int[] {0};
        int[] afterTimePassSkipped = new int[] {0};

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
                    bytes = patch(bytes, onNewGameSkipped, afterTimePassSkipped);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (onNewGameSkipped[0] != 3) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly 3 optional onNewGame world-generator calls, found "
                            + onNewGameSkipped[0]);
        }
        if (afterTimePassSkipped[0] != 2) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly 2 optional onNewGameAfterTimePass world-population calls, found "
                            + afterTimePassSkipped[0]);
        }
        System.out.println(
                "Patched CoreLifecyclePluginImpl optional browser world generators="
                        + onNewGameSkipped[0]
                        + " post-new-game world population="
                        + afterTimePassSkipped[0]);
    }

    private static byte[] patch(
            byte[] input, int[] onNewGameSkipped, int[] afterTimePassSkipped) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                if ("onNewGame".equals(name) && "()V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                                    String methodDescriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKEVIRTUAL
                                    && ON_NEW_GAME_WORLD_GENERATORS.contains(owner)
                                    && "generate".equals(methodName)
                                    && GENERATE_DESC.equals(methodDescriptor)) {
                                // Stack contains [generator, sector]. Both are category-1 refs.
                                super.visitInsn(Opcodes.POP2);
                                onNewGameSkipped[0]++;
                                return;
                            }
                            super.visitMethodInsn(
                                    opcode, owner, methodName, methodDescriptor, isInterface);
                        }
                    };
                }
                if ("onNewGameAfterTimePass".equals(name) && "()V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                                    String methodDescriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKEVIRTUAL
                                    && NAMELESS_ROCK.equals(owner)
                                    && "generate".equals(methodName)
                                    && GENERATE_DESC.equals(methodDescriptor)) {
                                // Stack contains [NamelessRock, sector].
                                super.visitInsn(Opcodes.POP2);
                                afterTimePassSkipped[0]++;
                                return;
                            }
                            if (opcode == Opcodes.INVOKEVIRTUAL
                                    && CUSTOM_FLEETS.equals(owner)
                                    && "spawn".equals(methodName)
                                    && "()V".equals(methodDescriptor)) {
                                // Stack contains the CustomFleets receiver only.
                                super.visitInsn(Opcodes.POP);
                                afterTimePassSkipped[0]++;
                                return;
                            }
                            super.visitMethodInsn(
                                    opcode, owner, methodName, methodDescriptor, isInterface);
                        }
                    };
                }
                return delegate;
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
