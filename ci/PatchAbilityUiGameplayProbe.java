import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Adds disabled-by-default telemetry to the actual campaign ability button UI. */
public final class PatchAbilityUiGameplayProbe {
    private static final String ENTRY = "com/fs/starfarer/ui/newui/H.class";
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    private static final Map<String,String> HOOKS = new LinkedHashMap<String,String>();
    static {
        HOOKS.put("advanceImpl(F)V", "abilityUiAdvance");
        HOOKS.put("actionPerformed(Ljava/lang/Object;Ljava/lang/Object;)V", "abilityUiAction");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchAbilityUiGameplayProbe input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, ready = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(entry)) { bytes = in.readAllBytes(); }
                if (ENTRY.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, ready);
                }
                out.write(bytes); out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 2 || ready[0] != 2) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("ability UI probe mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " ready=" + ready[0]);
        }
        System.out.println("Patched ability UI gameplay probe classes=1 methods=2 ready=2");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] ready) {
        final Set<String> existing = findExisting(input);
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                MethodVisitor mv = super.visitMethod(access,name,desc,sig,ex);
                final String key = name + desc;
                final String probe = HOOKS.get(key);
                if (probe == null) return mv;
                methods[0]++;
                if (existing.contains(key + "#" + probe)) {
                    ready[0]++;
                    return mv;
                }
                ready[0]++;
                if ("abilityUiAction".equals(probe)) {
                    return new MethodVisitor(Opcodes.ASM9,mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,PROBE,probe,"(Ljava/lang/Object;)V",false);
                        }
                    };
                }
                return new MethodVisitor(Opcodes.ASM9,mv) {
                    @Override public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,PROBE,probe,"(Ljava/lang/Object;)V",false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        },0);
        return writer.toByteArray();
    }

    private static Set<String> findExisting(byte[] bytes) {
        final Set<String> result = new LinkedHashSet<String>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                final String key = name + desc;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode,String owner,String called,String calledDesc,boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner)
                                && ("abilityUiAdvance".equals(called) || "abilityUiAction".equals(called))) {
                            result.add(key + "#" + called);
                        }
                    }
                };
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        return result;
    }
}
