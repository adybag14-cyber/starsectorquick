import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Test-only telemetry around Starsector's named-control shortcut matcher. */
public final class PatchControlMatcherGameplayProbe {
    private static final String TARGET = "com/fs/starfarer/title/B/B.class";
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    private static final String METHOD = "o00000";
    private static final String DESC = "(Lcom/fs/starfarer/util/super/Object;Lcom/fs/starfarer/title/B/B$o;Lcom/fs/starfarer/title/B/B$oo;)Z";
    private static final String PROBE_DESC = "(Ljava/lang/Object;IZZZ)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchControlMatcherGameplayProbe input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, hooks = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(entry)) { bytes = in.readAllBytes(); }
                if (TARGET.equals(entry.getName())) { classes[0]++; bytes = patch(bytes, methods, hooks); }
                out.write(bytes); out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || hooks[0] != 3) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("control matcher probe mismatch classes="+classes[0]+" methods="+methods[0]+" hooks="+hooks[0]);
        }
        System.out.println("Patched control matcher gameplay probe classes=1 methods=1 hooks=3");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] hooks) {
        final int existing = countProbeCalls(input);
        if (existing != 0 && existing != 3) throw new IllegalStateException("partial control matcher probe hooks="+existing);
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                MethodVisitor mv = super.visitMethod(access,name,desc,sig,ex);
                if (!METHOD.equals(name) || !DESC.equals(desc)) return mv;
                methods[0]++;
                if (existing == 3) { hooks[0] += 3; return mv; }
                return new MethodVisitor(Opcodes.ASM9,mv) {
                    @Override public void visitInsn(int opcode) {
                        if (opcode == Opcodes.IRETURN) {
                            super.visitVarInsn(Opcodes.ISTORE, 4);
                            super.visitVarInsn(Opcodes.ALOAD, 2);
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/fs/starfarer/util/super/Object", "getEventValue", "()I", false);
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/fs/starfarer/util/super/Object", "isKeyDownEvent", "()Z", false);
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/fs/starfarer/util/super/Object", "isConsumed", "()Z", false);
                            super.visitVarInsn(Opcodes.ILOAD, 4);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE, "controlMatch", PROBE_DESC, false);
                            super.visitVarInsn(Opcodes.ILOAD, 4);
                            hooks[0]++;
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
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                if (!METHOD.equals(name) || !DESC.equals(desc)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner) && "controlMatch".equals(name) && PROBE_DESC.equals(desc)) count[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return count[0];
    }
}
