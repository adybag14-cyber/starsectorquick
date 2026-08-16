import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public final class VerifyAbilityUiGameplayProbePatch {
    private static final String ENTRY = "com/fs/starfarer/ui/newui/H.class";
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyAbilityUiGameplayProbePatch jar");
        final Map<String,Integer> got = new TreeMap<String,Integer>();
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry(ENTRY);
            if (entry == null) throw new AssertionError("missing " + ENTRY);
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                        final String key = name + desc;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int opcode,String owner,String called,String calledDesc,boolean itf) {
                                if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner)
                                        && ("abilityUiAdvance".equals(called) || "abilityUiAction".equals(called))) {
                                    got.merge(key + "#" + called, 1, Integer::sum);
                                }
                            }
                        };
                    }
                },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            }
        }
        Set<String> expected = new TreeSet<String>(Arrays.asList(
                "advanceImpl(F)V#abilityUiAdvance",
                "actionPerformed(Ljava/lang/Object;Ljava/lang/Object;)V#abilityUiAction"));
        if (!got.keySet().equals(expected) || got.values().stream().anyMatch(v -> v != 1)) {
            throw new AssertionError("unexpected ability UI gameplay hooks " + got);
        }
        System.out.println("VerifyAbilityUiGameplayProbePatch: OK hooks=" + got.keySet());
    }
}
