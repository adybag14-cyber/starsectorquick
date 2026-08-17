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

/** Adds property-gated timing probes to stock SectorProcGen without changing generation. */
public final class PatchSectorProcGenDiagnostics {
    private static final String TARGET = "com/fs/starfarer/api/impl/campaign/procgen/SectorProcGen.class";
    private static final String METHOD = "generate";
    private static final String DESC = "(Lcom/fs/starfarer/api/characters/CharacterCreationData;Lcom/fs/starfarer/api/campaign/SectorGenProgress;)V";
    private static final String DIAG = "com/fs/starfarer/api/impl/campaign/procgen/BrowserSectorProcGenDiag";
    private static final String STAR_GEN = "com/fs/starfarer/api/impl/campaign/procgen/StarSystemGenerator";
    private static final String NEBULA = "com/fs/starfarer/api/impl/campaign/procgen/NebulaEditor";
    private static final String THEME = "com/fs/starfarer/api/impl/campaign/procgen/themes/SectorThemeGenerator";

    private PatchSectorProcGenDiagnostics() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchSectorProcGenDiagnostics input.jar output.jar");
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        int[] constellationCalls = {0};
        int[] regenCalls = {0};
        int[] pruneCalls = {0};
        int[] spiralArcCalls = {0};
        int[] randomArcCalls = {0};
        int[] abyssCalls = {0};
        int[] themeCalls = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (TARGET.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, constellationCalls, regenCalls, pruneCalls,
                            spiralArcCalls, randomArcCalls, abyssCalls, themeCalls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || constellationCalls[0] != 1
                || regenCalls[0] != 2 || pruneCalls[0] != 1 || spiralArcCalls[0] != 1
                || randomArcCalls[0] != 1 || abyssCalls[0] != 1 || themeCalls[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "SectorProcGen diagnostic patch shape mismatch classes=" + classes[0]
                            + " methods=" + methods[0]
                            + " constellationCalls=" + constellationCalls[0]
                            + " regenCalls=" + regenCalls[0]
                            + " pruneCalls=" + pruneCalls[0]
                            + " spiralArcCalls=" + spiralArcCalls[0]
                            + " randomArcCalls=" + randomArcCalls[0]
                            + " abyssCalls=" + abyssCalls[0]
                            + " themeCalls=" + themeCalls[0]);
        }
        System.out.println("Patched SectorProcGen diagnostics constellation=1 regen=2 prune=1 spiralArc=1 randomArc=1 abyss=1 theme=1");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] constellationCalls,
                                int[] regenCalls, int[] pruneCalls, int[] spiralArcCalls,
                                int[] randomArcCalls, int[] abyssCalls, int[] themeCalls) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private int regenIndex;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        call("begin", "()V");
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && STAR_GEN.equals(owner)
                                && "generate".equals(methodName)
                                && "()Lcom/fs/starfarer/api/impl/campaign/procgen/Constellation;".equals(methodDescriptor)) {
                            constellationCalls[0]++;
                            call("beforeConstellation", "()V");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            call("afterConstellation", "()V");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL && NEBULA.equals(owner)
                                && "regenNoise".equals(methodName) && "()V".equals(methodDescriptor)) {
                            regenCalls[0]++;
                            regenIndex++;
                            mark("regenNoise" + regenIndex + "-before");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            mark("regenNoise" + regenIndex + "-after");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL && NEBULA.equals(owner)
                                && "noisePrune".equals(methodName) && "(F)V".equals(methodDescriptor)) {
                            pruneCalls[0]++;
                            mark("noisePrune-before");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            mark("noisePrune-after");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL && NEBULA.equals(owner)
                                && "clearArc".equals(methodName) && "(FFFFFFFF)V".equals(methodDescriptor)) {
                            spiralArcCalls[0]++;
                            mark("spiralArc-before");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            mark("spiralArc-after");
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL && NEBULA.equals(owner)
                                && "clearArc".equals(methodName) && "(FFFFFFF)V".equals(methodDescriptor)) {
                            randomArcCalls[0]++;
                            call("beforeRandomArc", "()V");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            call("afterRandomArc", "()V");
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/api/impl/campaign/procgen/SectorProcGen".equals(owner)
                                && "clearAbyssalHyperspaceAndSetSystemTags".equals(methodName)
                                && "()V".equals(methodDescriptor)) {
                            abyssCalls[0]++;
                            mark("abyss-before");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            mark("abyss-after");
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC && THEME.equals(owner)
                                && "generate".equals(methodName)
                                && "(Lcom/fs/starfarer/api/impl/campaign/procgen/themes/ThemeGenContext;)V".equals(methodDescriptor)) {
                            themeCalls[0]++;
                            mark("theme-before");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            mark("theme-after");
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) call("finish", "()V");
                        super.visitInsn(opcode);
                    }

                    private void mark(String label) {
                        super.visitLdcInsn(label);
                        call("mark", "(Ljava/lang/String;)V");
                    }

                    private void call(String name, String desc) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, DIAG, name, desc, false);
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
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
