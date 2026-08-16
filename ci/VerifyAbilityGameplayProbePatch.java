import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public final class VerifyAbilityGameplayProbePatch {
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyAbilityGameplayProbePatch jar");
        final Map<String, Integer> got = new TreeMap<String, Integer>();
        List<String> classes = Arrays.asList(
                "com/fs/starfarer/api/impl/campaign/abilities/BaseAbilityPlugin.class",
                "com/fs/starfarer/api/impl/campaign/abilities/BaseToggleAbility.class",
                "com/fs/starfarer/api/impl/campaign/abilities/BaseDurationAbility.class",
                "com/fs/starfarer/api/impl/campaign/abilities/TransponderAbility.class");
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            for (final String entryName : classes) {
                JarEntry entry = jar.getJarEntry(entryName);
                if (entry == null) throw new AssertionError("missing " + entryName);
                try (InputStream in = jar.getInputStream(entry)) {
                    new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                            final String method = name;
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String called, String calledDesc, boolean itf) {
                                    if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner) && called.startsWith("ability")) {
                                        got.merge(entryName + "#" + method + ":" + called, 1, Integer::sum);
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }

        Set<String> expected = new TreeSet<String>(Arrays.asList(
                "com/fs/starfarer/api/impl/campaign/abilities/BaseAbilityPlugin.class#activate:abilityActivate",
                "com/fs/starfarer/api/impl/campaign/abilities/BaseAbilityPlugin.class#deactivate:abilityDeactivate",
                "com/fs/starfarer/api/impl/campaign/abilities/BaseToggleAbility.class#pressButton:abilityPress",
                "com/fs/starfarer/api/impl/campaign/abilities/BaseToggleAbility.class#advance:abilityAdvance",
                "com/fs/starfarer/api/impl/campaign/abilities/BaseDurationAbility.class#pressButton:abilityPress",
                "com/fs/starfarer/api/impl/campaign/abilities/BaseDurationAbility.class#advance:abilityAdvance",
                "com/fs/starfarer/api/impl/campaign/abilities/TransponderAbility.class#pressButton:abilityPress"));
        if (!got.keySet().equals(expected) || got.values().stream().anyMatch(v -> v != 1)) {
            throw new AssertionError("unexpected gameplay probe hooks " + got);
        }
        System.out.println("VerifyAbilityGameplayProbePatch: OK hooks=" + got.keySet());
    }
}
