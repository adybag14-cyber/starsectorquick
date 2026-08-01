import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 * Browser-runtime bytecode fixes that must live in Starsector's own jars.
 *
 * 1. Decorative orbital junk is skipped because its readResolve path is not
 *    compatible with CheerpJ during sector construction.
 * 2. Campaign state requests are drained by BaseGameState.traverse immediately
 *    before Display.update(), which guarantees goToState() runs on the render /
 *    AppDriver thread rather than on Fixer's watcher thread.
 * 3. CampaignGameManager's launcher mod-list lookups are made null-safe. The
 *    direct browser launch does not construct the desktop ModManager singleton;
 *    for a vanilla game that correctly means an empty mod/plugin list.
 */
public final class PatchCampaignOrbitalJunk {
    private static final String JUNK_TARGET =
            "com/fs/starfarer/api/impl/campaign/CoreLifecyclePluginImpl.class";
    private static final String JUNK_METHOD = "addJunk";
    private static final String JUNK_DESCRIPTOR =
            "(Lcom/fs/starfarer/api/campaign/econ/MarketAPI;)V";
    private static final String BASE_GAME_STATE_TARGET =
            "com/fs/starfarer/BaseGameState.class";
    private static final String CAMPAIGN_GAME_MANAGER_TARGET =
            "com/fs/starfarer/campaign/save/CampaignGameManager.class";
    private static final String MOD_MANAGER_OWNER =
            "com/fs/starfarer/launcher/ModManager";
    private static final String MOD_MANAGER_COMPAT_OWNER =
            "com/fs/starfarer/ModManagerCompat";
    private static final String LIST_DESCRIPTOR = "()Ljava/util/List;";
    private static final String SAFE_LIST_DESCRIPTOR =
            "(Lcom/fs/starfarer/launcher/ModManager;)Ljava/util/List;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCampaignOrbitalJunk input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] replacements = new int[] {0};

        rewriteJar(input, output, replacements, null, null);
        if (replacements[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected one CoreLifecyclePluginImpl.addJunk(MarketAPI) replacement, found "
                            + replacements[0]);
        }

        Path obf = Path.of("jars", "starfarer_obf.jar");
        if (!Files.isRegularFile(obf)) {
            throw new IllegalStateException("missing " + obf);
        }
        Path obfPatched = obf.resolveSibling("starfarer_obf.jar.main-thread.tmp");
        int[] transitionInjections = new int[] {0};
        int[] modManagerGuards = new int[] {0};
        rewriteJar(obf, obfPatched, null, transitionInjections, modManagerGuards);
        if (transitionInjections[0] < 1) {
            Files.deleteIfExists(obfPatched);
            throw new IllegalStateException(
                    "no Display.update call found in BaseGameState.traverse");
        }
        if (modManagerGuards[0] < 1) {
            Files.deleteIfExists(obfPatched);
            throw new IllegalStateException(
                    "no CampaignGameManager ModManager list lookups were patched");
        }
        Files.move(obfPatched, obf, StandardCopyOption.REPLACE_EXISTING);

        System.out.println(
                "Patched cosmetic campaign orbital-junk generation methods="
                        + replacements[0]);
        System.out.println(
                "Patched BaseGameState render-thread transition drains="
                        + transitionInjections[0]);
        System.out.println(
                "Patched CampaignGameManager null-safe ModManager list lookups="
                        + modManagerGuards[0]);
    }

    private static void rewriteJar(
            Path input,
            Path output,
            int[] junkReplacements,
            int[] transitionInjections,
            int[] modManagerGuards) throws Exception {
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
                if (junkReplacements != null && JUNK_TARGET.equals(entry.getName())) {
                    bytes = patchJunk(bytes, junkReplacements);
                }
                if (transitionInjections != null
                        && BASE_GAME_STATE_TARGET.equals(entry.getName())) {
                    bytes = patchBaseGameState(bytes, transitionInjections);
                }
                if (modManagerGuards != null
                        && CAMPAIGN_GAME_MANAGER_TARGET.equals(entry.getName())) {
                    bytes = patchCampaignGameManager(bytes, modManagerGuards);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
    }

    private static byte[] patchJunk(byte[] input, int[] replacements) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor output = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                if (!JUNK_METHOD.equals(name) || !JUNK_DESCRIPTOR.equals(descriptor)) {
                    return output;
                }
                replacements[0]++;
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

    private static byte[] patchBaseGameState(byte[] input, int[] injections) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                if (!"traverse".equals(name) || !"()Ljava/lang/String;".equals(descriptor)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "org/lwjgl/opengl/Display".equals(owner)
                                && "update".equals(methodName)) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/fs/starfarer/MainThreadTransitionBridge",
                                    "drain",
                                    "(Ljava/lang/Object;)V",
                                    false);
                            injections[0]++;
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

    private static byte[] patchCampaignGameManager(byte[] input, int[] guards) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && MOD_MANAGER_OWNER.equals(owner)
                                && LIST_DESCRIPTOR.equals(methodDescriptor)
                                && ("getEnabledModPlugins".equals(methodName)
                                        || "getEnabledMods".equals(methodName))) {
                            // The ModManager reference is already on the operand stack. A static
                            // compatibility method consumes the same reference and can therefore
                            // handle null without changing the surrounding iterator bytecode.
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    MOD_MANAGER_COMPAT_OWNER,
                                    methodName,
                                    SAFE_LIST_DESCRIPTOR,
                                    false);
                            guards[0]++;
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
