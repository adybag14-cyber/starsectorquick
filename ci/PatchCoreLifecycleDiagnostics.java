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

/** Adds low-volume checkpoints inside the two lifecycle callbacks used by create(). */
public final class PatchCoreLifecycleDiagnostics {
    private static final String TARGET_ENTRY =
            "com/fs/starfarer/api/impl/campaign/CoreLifecyclePluginImpl.class";
    private static final String TARGET_OWNER =
            "com/fs/starfarer/api/impl/campaign/CoreLifecyclePluginImpl";

    private static final Set<String> AFTER_ECONOMY_HELPERS = new HashSet<String>(
            Arrays.asList(
                    "addJunk",
                    "createInitialPeople",
                    "addScriptsIfNeeded",
                    "updateKnownPlanets",
                    "markStoryCriticalMarketsEtc",
                    "tagLuddicShrines"));
    private static final Set<String> GAME_LOAD_HELPERS = new HashSet<String>(
            Arrays.asList(
                    "econPostSaveRestore",
                    "addJunk",
                    "regenAsteroids",
                    "addScriptsIfNeeded",
                    "verifyFactionData",
                    "convertTo0951aSkillSystemIfNeeded",
                    "addMissingPeople"));

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCoreLifecycleDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classSeen = new int[] {0};
        int[] afterEconomyCalls = new int[] {0};
        int[] gameLoadCalls = new int[] {0};

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
                if (TARGET_ENTRY.equals(entry.getName())) {
                    classSeen[0]++;
                    bytes = patch(bytes, afterEconomyCalls, gameLoadCalls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classSeen[0] != 1 || afterEconomyCalls[0] != 6 || gameLoadCalls[0] != 7) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "CoreLifecycle diagnostics incomplete: classSeen="
                            + classSeen[0]
                            + " afterEconomyCalls="
                            + afterEconomyCalls[0]
                            + " gameLoadCalls="
                            + gameLoadCalls[0]);
        }
        System.out.println(
                "Patched CoreLifecycle diagnostics after-economy calls="
                        + afterEconomyCalls[0]
                        + " game-load calls="
                        + gameLoadCalls[0]);
    }

    private static byte[] patch(byte[] input, int[] afterEconomyCalls, int[] gameLoadCalls) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
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
                final String callback;
                final Set<String> helpers;
                final int[] counter;
                if ("onNewGameAfterEconomyLoad".equals(name) && "()V".equals(descriptor)) {
                    callback = "onNewGameAfterEconomyLoad";
                    helpers = AFTER_ECONOMY_HELPERS;
                    counter = afterEconomyCalls;
                } else if ("onGameLoad".equals(name) && "(Z)V".equals(descriptor)) {
                    callback = "onGameLoad";
                    helpers = GAME_LOAD_HELPERS;
                    counter = gameLoadCalls;
                } else {
                    return delegate;
                }

                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        boolean instrument = TARGET_OWNER.equals(owner) && helpers.contains(methodName);
                        if (instrument) {
                            emit("before " + callback + "." + methodName);
                            counter[0]++;
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                        if (instrument) {
                            emit("after " + callback + "." + methodName);
                        }
                    }

                    private void emit(String message) {
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn("CoreLifecycleDiag: " + message);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
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
