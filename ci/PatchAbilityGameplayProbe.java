import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Adds disabled-by-default gameplay telemetry to campaign ability button/state paths. */
public final class PatchAbilityGameplayProbe {
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    private static final String ABILITY = "com/fs/starfarer/api/characters/AbilityPlugin";
    private static final String VOID = "()V";
    private static final Map<String, Set<String>> TARGETS = new HashMap<String, Set<String>>();
    static {
        TARGETS.put("com/fs/starfarer/api/impl/campaign/abilities/BaseAbilityPlugin.class", new HashSet<String>(Arrays.asList("activate", "deactivate")));
        TARGETS.put("com/fs/starfarer/api/impl/campaign/abilities/BaseToggleAbility.class", new HashSet<String>(Arrays.asList("pressButton")));
        TARGETS.put("com/fs/starfarer/api/impl/campaign/abilities/BaseDurationAbility.class", new HashSet<String>(Arrays.asList("pressButton")));
        TARGETS.put("com/fs/starfarer/api/impl/campaign/abilities/TransponderAbility.class", new HashSet<String>(Arrays.asList("pressButton")));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchAbilityGameplayProbe input.jar output.jar");
        Path in=Path.of(args[0]), out=Path.of(args[1]); int[] classes={0}, methods={0}, inserted={0};
        try(JarFile jar=new JarFile(in.toFile()); JarOutputStream jos=new JarOutputStream(Files.newOutputStream(out))){
            Enumeration<JarEntry> es=jar.entries();
            while(es.hasMoreElements()){
                JarEntry e=es.nextElement(); JarEntry c=new JarEntry(e.getName()); c.setTime(e.getTime()); jos.putNextEntry(c);
                byte[] bytes; try(InputStream is=jar.getInputStream(e)){bytes=is.readAllBytes();}
                Set<String> names=TARGETS.get(e.getName());
                if(names!=null){classes[0]++; bytes=patch(bytes,names,methods,inserted);}
                jos.write(bytes); jos.closeEntry();
            }
        }
        if(classes[0]!=4||methods[0]!=5||inserted[0]!=5){Files.deleteIfExists(out);throw new IllegalStateException("gameplay probe mismatch classes="+classes[0]+" methods="+methods[0]+" inserted="+inserted[0]);}
        System.out.println("Patched ability gameplay probe classes=4 methods=5 inserted=5");
    }
    private static byte[] patch(byte[] input, Set<String> names, int[] methods, int[] inserted){
        if(countProbeCalls(input)>0){
            int c=countProbeCalls(input); methods[0]+=c; inserted[0]+=c; return input;
        }
        ClassReader r=new ClassReader(input); ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){
            MethodVisitor mv=super.visitMethod(a,n,d,s,ex); if(!VOID.equals(d)||!names.contains(n))return mv; methods[0]++;
            final String probeMethod="pressButton".equals(n)?"abilityPress":("activate".equals(n)?"abilityActivate":"abilityDeactivate");
            return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,PROBE,probeMethod,"(L"+ABILITY+";)V",false);inserted[0]++;}};
        }},0); return w.toByteArray();
    }
    private static int countProbeCalls(byte[] bytes){final int[] c={0};new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean i){if(op==Opcodes.INVOKESTATIC&&PROBE.equals(o)&&n.startsWith("ability"))c[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c[0];}
}
