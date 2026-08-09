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
 * that require stock star systems to exist, and makes stock market-tagging
 * helpers tolerate markets intentionally absent from a partial browser world.
 *
 * The CheerpJ bootstrap deliberately creates a minimal sector so campaign state
 * can become interactive before desktop-only world generation is supported. The
 * lifecycle callbacks themselves are preserved; only calls that populate or
 * assume the deferred vanilla world are changed.
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
    private static final Set<String> ON_GAME_LOAD_WORLD_GENERATORS = new HashSet<String>(
            Arrays.asList(
                    "com/fs/starfarer/api/impl/campaign/world/Limbo",
                    "com/fs/starfarer/api/impl/campaign/world/GateHaulerLocation",
                    "com/fs/starfarer/api/impl/campaign/world/NamelessRock"));
    private static final String SECTOR_PROC_GEN =
            "com/fs/starfarer/api/impl/campaign/procgen/SectorProcGen";
    private static final String NAMELESS_ROCK =
            "com/fs/starfarer/api/impl/campaign/world/NamelessRock";
    private static final String CUSTOM_FLEETS =
            "com/fs/starfarer/api/impl/campaign/fleets/CustomFleets";
    private static final String MARKET_API =
            "com/fs/starfarer/api/campaign/econ/MarketAPI";
    private static final String MISC =
            "com/fs/starfarer/api/util/Misc";
    private static final String CORE_LIFECYCLE_COMPAT =
            "com/fs/starfarer/CoreLifecycleCompat";
    private static final String SAFE_MARKET_TAG_DESC =
            "(Lcom/fs/starfarer/api/campaign/econ/MarketAPI;Ljava/lang/String;)V";
    private static final String STORY_CRITICAL_DESC =
            "(Ljava/lang/String;Ljava/lang/String;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCoreLifecycleBrowserWorld input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] onNewGameSkipped = new int[] {0};
        int[] afterTimePassSkipped = new int[] {0};
        int[] onGameLoadSkipped = new int[] {0};
        int[] shrineMarketGuards = new int[] {0};
        int[] storyCriticalGuards = new int[] {0};

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
                    bytes = patch(
                            bytes,
                            onNewGameSkipped,
                            afterTimePassSkipped,
                            onGameLoadSkipped,
                            shrineMarketGuards,
                            storyCriticalGuards);
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
        if (onGameLoadSkipped[0] != 4) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly 4 optional onGameLoad world operations, found "
                            + onGameLoadSkipped[0]);
        }
        if (shrineMarketGuards[0] != 4) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly 4 tagLuddicShrines market-tag guards, found "
                            + shrineMarketGuards[0]);
        }
        if (storyCriticalGuards[0] != 36) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly 36 markStoryCriticalMarketsEtc guards, found "
                            + storyCriticalGuards[0]);
        }
        System.out.println(
                "Patched CoreLifecyclePluginImpl optional browser world generators="
                        + onNewGameSkipped[0]
                        + " post-new-game world population="
                        + afterTimePassSkipped[0]
                        + " game-load world operations="
                        + onGameLoadSkipped[0]
                        + " shrine-market guards="
                        + shrineMarketGuards[0]
                        + " story-critical guards="
                        + storyCriticalGuards[0]);
    }

    private static byte[] patch(
            byte[] input,
            int[] onNewGameSkipped,
            int[] afterTimePassSkipped,
            int[] onGameLoadSkipped,
            int[] shrineMarketGuards,
            int[] storyCriticalGuards) {
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
                if ("onGameLoad".equals(name) && "(Z)V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                                    String methodDescriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKEVIRTUAL
                                    && ON_GAME_LOAD_WORLD_GENERATORS.contains(owner)
                                    && "generate".equals(methodName)
                                    && GENERATE_DESC.equals(methodDescriptor)) {
                                // Stack contains [generator, sector]. These are the same
                                // deferred world constructors already removed from new-game
                                // lifecycle callbacks.
                                super.visitInsn(Opcodes.POP2);
                                onGameLoadSkipped[0]++;
                                return;
                            }
                            if (opcode == Opcodes.INVOKESTATIC
                                    && SECTOR_PROC_GEN.equals(owner)
                                    && "clearAbyssalHyperspaceAndSetSystemTags".equals(methodName)
                                    && "()V".equals(methodDescriptor)) {
                                // This cleanup belongs to the Limbo/Gate generation block.
                                // With those systems intentionally deferred there is nothing
                                // for it to normalize during browser bootstrap.
                                onGameLoadSkipped[0]++;
                                return;
                            }
                            super.visitMethodInsn(
                                    opcode, owner, methodName, methodDescriptor, isInterface);
                        }
                    };
                }
                if ("markStoryCriticalMarketsEtc".equals(name) && "()V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                                    String methodDescriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKESTATIC
                                    && MISC.equals(owner)
                                    && "makeStoryCritical".equals(methodName)
                                    && STORY_CRITICAL_DESC.equals(methodDescriptor)) {
                                // Stack is [marketId, reason]. The compatibility helper performs
                                // the same stock operation when that market exists and otherwise
                                // leaves the intentionally partial world unchanged.
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        CORE_LIFECYCLE_COMPAT,
                                        "makeStoryCriticalIfMarketPresent",
                                        STORY_CRITICAL_DESC,
                                        false);
                                storyCriticalGuards[0]++;
                                return;
                            }
                            super.visitMethodInsn(
                                    opcode, owner, methodName, methodDescriptor, isInterface);
                        }
                    };
                }
                if ("tagLuddicShrines".equals(name) && "()V".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                                    String methodDescriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKEINTERFACE
                                    && MARKET_API.equals(owner)
                                    && "addTag".equals(methodName)
                                    && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
                                // Stack is [market-or-null, tag]. The static helper consumes
                                // the exact same operands and only suppresses the call when the
                                // named stock market does not exist in the partial browser world.
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        CORE_LIFECYCLE_COMPAT,
                                        "addMarketTagIfPresent",
                                        SAFE_MARKET_TAG_DESC,
                                        false);
                                shrineMarketGuards[0]++;
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
