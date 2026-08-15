import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public final class VerifyCampaignGameplayProbePatch {
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
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
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyCampaignGameplayProbePatch jar");
        Map<String,Integer> starts = new LinkedHashMap<String,Integer>();
        Map<String,Integer> ready = new LinkedHashMap<String,Integer>();
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            for (Map.Entry<String,String> target : TARGETS.entrySet()) {
                JarEntry entry = jar.getJarEntry(target.getKey());
                if (entry == null) throw new AssertionError("missing " + target.getKey());
                final String expected = target.getValue();
                try (InputStream in = jar.getInputStream(entry)) {
                    new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                            if (!"actionPerformed".equals(name) || !"(Ljava/lang/Object;Ljava/lang/Object;)V".equals(desc)) return null;
                            return new MethodVisitor(Opcodes.ASM9) {
                                String lastString;
                                @Override public void visitLdcInsn(Object value) { if (value instanceof String) lastString = (String)value; }
                                @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf) {
                                    if (opcode == Opcodes.INVOKESTATIC && PROBE.equals(owner)) {
                                        if (!expected.equals(lastString)) throw new AssertionError("wrong tab marker entry="+target.getKey()+" expected="+expected+" actual="+lastString+" method="+name);
                                        if ("coreTabStart".equals(name)) starts.merge(expected,1,Integer::sum);
                                        if ("coreTabReady".equals(name)) ready.merge(expected,1,Integer::sum);
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
        for (String tab : TARGETS.values()) {
            if (starts.getOrDefault(tab,0) != 1) throw new AssertionError("start count tab="+tab+" count="+starts.getOrDefault(tab,0));
            if (ready.getOrDefault(tab,0) < 1) throw new AssertionError("ready missing tab="+tab);
        }
        System.out.println("VerifyCampaignGameplayProbePatch: OK starts="+starts+" ready="+ready);
    }
}
