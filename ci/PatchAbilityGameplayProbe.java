import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Adds disabled-by-default gameplay telemetry to campaign ability button/state paths. */
public final class PatchAbilityGameplayProbe {
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    private static final String ABILITY = "com/fs/starfarer/api/characters/AbilityPlugin";
    private static final Map<String, Map<String, String>> TARGETS = new LinkedHashMap<String, Map<String, String>>();

    static {
        add("com/fs/starfarer/api/impl/campaign/abilities/BaseAbilityPlugin.class", "activate()V", "abilityActivate");
        add("com/fs/starfarer/api/impl/campaign/abilities/BaseAbilityPlugin.class", "deactivate()V", "abilityDeactivate");
        add("com/fs/starfarer/api/impl/campaign/abilities/BaseToggleAbility.class", "pressButton()V", "abilityPress");
        add("com/fs/starfarer/api/impl/campaign/abilities/BaseToggleAbility.class", "advance(F)V", "abilityAdvance");
        add("com/fs/starfarer/api/impl/campaign/abilities/BaseDurationAbility.class", "pressButton()V", "abilityPress");
        add("com/fs/starfarer/api/impl/campaign/abilities/BaseDurationAbility.class", "advance(F)V", "abilityAdvance");
        add("com/fs/starfarer/api/impl/campaign/abilities/TransponderAbility.class", "pressButton()V", "abilityPress");
    }

    private static void add(String classEntry, String methodSig, String probeMethod) {
        Map<String, String> hooks = TARGETS.get(classEntry);
        if (hooks == null) {
            hooks = new LinkedHashMap<String, String>();
            TARGETS.put(classEntry, hooks);
        }
        hooks.put(methodSig, probeMethod);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchAbilityGameplayProbe input.jar output.jar");
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        int[] ready = {0};
        try (JarFile jar = new JarFile(in.toFile());
             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                jos.putNextEntry(copy);
                byte[] bytes;
                try (InputStream stream = jar.getInputStream(entry)) {
                    bytes = stream.readAllBytes();
                }
                Map<String, String> hooks = TARGETS.get(entry.getName());
                if (hooks != null) {
                    classes[0]++;
                    bytes = patch(bytes, hooks, methods, ready);
                }
                jos.write(bytes);
                jos.closeEntry();
            }
        }
        if (classes[0] != 4 || methods[0] != 7 || ready[0] != 7) {
            Files.deleteIfExists(out);
            throw new IllegalStateException("gameplay probe mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " ready=" + ready[0]);
        }
        System.out.println("Patched ability gameplay probe classes=4 methods=7 ready=7");
    }

    private static byte[] patch(byte[] input, Map<String, String> hooks, int[] methods, int[] ready) {
        final Set<String> existing = findExistingHooks(input);
        final Set<String> expected = new LinkedHashSet<String>();
        for (Map.Entry<String, String> hook : hooks.entrySet()) {
            expected.add(hook.getKey() + "#" + hook.getValue());
        }
        if (existing.containsAll(expected)) {
            methods[0] += hooks.size();
            ready[0] += hooks.size();
            return input;
        }

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                String methodSig = name + desc;
                String probeMethod = hooks.get(methodSig);
                if (probeMethod == null) return mv;
                methods[0]++;
                String hookKey = methodSig + "#" + probeMethod;
                if (existing.contains(hookKey)) {
                    ready[0]++;
                    return mv;
                }
                ready[0]++;
                if ("abilityAdvance".equals(probeMethod)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) {
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE, probeMethod,
                                        "(L" + ABILITY + ";)V", false);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE, probeMethod,
                                "(L" + ABILITY + ";)V", false);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static Set<String> findExistingHooks(byte[] bytes) {
        final Set<String> result = new LinkedHashSet<String>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                final String methodSig = name + desc;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String methodDesc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner) && method.startsWith("ability")) {
                            result.add(methodSig + "#" + method);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result;
    }
}
