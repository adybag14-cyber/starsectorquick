import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Test-only telemetry around the seven real Campaign bottom-bar listeners. */
public final class PatchCampaignGameplayProbe {
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    private static final String ACTION = "actionPerformed";
    private static final String ACTION_DESC = "(Ljava/lang/Object;Ljava/lang/Object;)V";
    private static final Map<String, String> TARGETS = new LinkedHashMap<String, String>();
    static {
        TARGETS.put("com/fs/starfarer/ui/newui/L$2.class", "CHARACTER");
        TARGETS.put("com/fs/starfarer/ui/newui/L$3.class", "FLEET");
        TARGETS.put("com/fs/starfarer/ui/newui/L$4.class", "REFIT");
        TARGETS.put("com/fs/starfarer/ui/newui/L$5.class", "CARGO");
        TARGETS.put("com/fs/starfarer/ui/newui/L$6.class", "MAP");
        TARGETS.put("com/fs/starfarer/ui/newui/L$7.class", "INTEL");
        TARGETS.put("com/fs/starfarer/ui/newui/L$8.class", "OUTPOSTS");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchCampaignGameplayProbe input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, starts = {0}, ready = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(entry)) { bytes = in.readAllBytes(); }
                String tab = TARGETS.get(entry.getName());
                if (tab != null) { classes[0]++; bytes = patch(bytes, tab, methods, starts, ready); }
                out.write(bytes); out.closeEntry();
            }
        }
        if (classes[0] != 7 || methods[0] != 7 || starts[0] != 7 || ready[0] < 7) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("campaign gameplay probe mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " starts=" + starts[0] + " ready=" + ready[0]);
        }
        System.out.println("Patched Campaign gameplay listeners classes=7 methods=7 starts=7 ready=" + ready[0]);
    }

    private static byte[] patch(byte[] input, final String tab, int[] methods, int[] starts, int[] ready) {
        int existing = countProbeCalls(input);
        if (existing > 0) {
            if (existing < 2) throw new IllegalStateException("partial gameplay listener probe tab=" + tab + " calls=" + existing);
            methods[0]++; starts[0]++; ready[0] += existing - 1;
            return input;
        }
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                if (!ACTION.equals(name) || !ACTION_DESC.equals(desc)) return mv;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitLdcInsn(tab);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE, "coreTabStart", "(Ljava/lang/Object;)V", false);
                        starts[0]++;
                    }
                    @Override public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            super.visitLdcInsn(tab);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE, "coreTabReady", "(Ljava/lang/Object;)V", false);
                            ready[0]++;
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static int countProbeCalls(byte[] bytes) {
        final int[] count = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                if (!ACTION.equals(name) || !ACTION_DESC.equals(desc)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner)
                                && ("coreTabStart".equals(name) || "coreTabReady".equals(name))) count[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return count[0];
    }
}
