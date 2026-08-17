import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Test-only telemetry for actual CampaignEngine pause-state transitions. */
public final class PatchCampaignPauseGameplayProbe {
    private static final String TARGET = "com/fs/starfarer/campaign/CampaignEngine.class";
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    private static final String METHOD = "setPaused";
    private static final String DESC = "(Z)V";
    private static final String PROBE_DESC = "(ZZ)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchCampaignPauseGameplayProbe input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, hooks = {0};
        try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(entry)) { bytes = in.readAllBytes(); }
                if (TARGET.equals(entry.getName())) { classes[0]++; bytes = patch(bytes, methods, hooks); }
                out.write(bytes); out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || hooks[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("campaign pause probe mismatch classes="+classes[0]+" methods="+methods[0]+" hooks="+hooks[0]);
        }
        System.out.println("Patched campaign pause gameplay probe classes=1 methods=1 hooks=1");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] hooks) {
        final int existing = countProbeCalls(input);
        if (existing != 0 && existing != 1) throw new IllegalStateException("partial campaign pause probe hooks="+existing);
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                MethodVisitor mv = super.visitMethod(access,name,desc,sig,ex);
                if (!METHOD.equals(name) || !DESC.equals(desc)) return mv;
                methods[0]++;
                if (existing == 1) { hooks[0]++; return mv; }
                return new MethodVisitor(Opcodes.ASM9,mv) {
                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, "com/fs/starfarer/campaign/CampaignEngine", "paused", "Z");
                        super.visitVarInsn(Opcodes.ILOAD, 1);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE, "pauseTransition", PROBE_DESC, false);
                        hooks[0]++;
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static int countProbeCalls(byte[] bytes) {
        final int[] count = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                if (!METHOD.equals(name) || !DESC.equals(desc)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner) && "pauseTransition".equals(name) && PROBE_DESC.equals(desc)) count[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return count[0];
    }
}
