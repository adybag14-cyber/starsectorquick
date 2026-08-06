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
 * Patches the SectorGen class that is actually loaded from scripts-precompiled.jar.
 *
 * The repository also contains loose copies of data/scripts/world/SectorGen.java,
 * but the CheerpJ launcher puts scripts-precompiled.jar on the JVM classpath. The
 * precompiled generate() therefore wins before Janino can make the loose source
 * rewrite authoritative. Keep the stock generate() bootstrap, relationships and
 * core plugin/script setup, but remove its calls to runSectorStep(), which are the
 * entry points for the 24 heavyweight vanilla star-system generators.
 */
public final class PatchPrecompiledSectorGen {
    private static final String TARGET_ENTRY = "data/scripts/world/SectorGen.class";
    private static final String TARGET_OWNER = "data/scripts/world/SectorGen";
    private static final String GENERATE_DESC =
            "(Lcom/fs/starfarer/api/campaign/SectorAPI;)V";
    private static final String RUN_STEP_DESC =
            "(Ljava/lang/String;ZLdata/scripts/world/SectorGen$SectorStep;)V";
    private static final int EXPECTED_STEPS = 24;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchPrecompiledSectorGen input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classSeen = new int[] {0};
        int[] generateSeen = new int[] {0};
        int[] skippedSteps = new int[] {0};

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
                    bytes = patch(bytes, generateSeen, skippedSteps);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classSeen[0] != 1 || generateSeen[0] != 1 || skippedSteps[0] != EXPECTED_STEPS) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "precompiled SectorGen patch incomplete: classSeen="
                            + classSeen[0]
                            + " generateSeen="
                            + generateSeen[0]
                            + " skippedSteps="
                            + skippedSteps[0]
                            + " expectedSteps="
                            + EXPECTED_STEPS);
        }
        System.out.println(
                "Patched scripts-precompiled SectorGen world steps=" + skippedSteps[0]);
    }

    private static byte[] patch(byte[] input, int[] generateSeen, int[] skippedSteps) {
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
                if (!"generate".equals(name) || !GENERATE_DESC.equals(descriptor)) {
                    return delegate;
                }
                generateSeen[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn(
                                "BrowserSectorGenDiag: executing patched scripts-precompiled SectorGen.generate");
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                    }

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && TARGET_OWNER.equals(owner)
                                && "runSectorStep".equals(methodName)
                                && RUN_STEP_DESC.equals(methodDescriptor)) {
                            // Operand stack is [this, label, compatibilityFastPath, step].
                            // All four operands are category-1 values; two POP2s consume
                            // exactly the arguments and receiver that the removed call used.
                            super.visitInsn(Opcodes.POP2);
                            super.visitInsn(Opcodes.POP2);
                            skippedSteps[0]++;
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
