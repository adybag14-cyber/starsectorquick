import java.io.*;import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;
public final class VerifyAbilityGameplayProbePatch {
  static final String P="com/fs/starfarer/BrowserGameplayProbe";
  public static void main(String[]a)throws Exception{if(a.length!=1)throw new IllegalArgumentException("usage: VerifyAbilityGameplayProbePatch jar");Map<String,Integer> got=new TreeMap<>();try(JarFile j=new JarFile(Path.of(a[0]).toFile())){for(String e:Arrays.asList("com/fs/starfarer/api/impl/campaign/abilities/BaseAbilityPlugin.class","com/fs/starfarer/api/impl/campaign/abilities/BaseToggleAbility.class","com/fs/starfarer/api/impl/campaign/abilities/BaseDurationAbility.class","com/fs/starfarer/api/impl/campaign/abilities/TransponderAbility.class")){JarEntry x=j.getJarEntry(e);if(x==null)throw new AssertionError("missing "+e);try(InputStream in=j.getInputStream(x)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int ac,String n,String d,String s,String[]ex){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String m,String md,boolean itf){if(op==Opcodes.INVOKESTATIC&&P.equals(o)&&m.startsWith("ability"))got.merge(e+"#"+n+":"+m,1,Integer::sum);}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}}}
    if(got.size()!=5||got.values().stream().anyMatch(v->v!=1))throw new AssertionError("unexpected gameplay probe hooks "+got);
    System.out.println("VerifyAbilityGameplayProbePatch: OK hooks="+got.keySet());
  }
}
