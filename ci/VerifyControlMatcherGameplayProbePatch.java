import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public final class VerifyControlMatcherGameplayProbePatch {
    private static final String TARGET = "com/fs/starfarer/title/B/B.class";
    private static final String METHOD = "o00000";
    private static final String DESC = "(Lcom/fs/starfarer/util/super/Object;Lcom/fs/starfarer/title/B/B$o;Lcom/fs/starfarer/title/B/B$oo;)Z";
    private static final String PROBE = "com/fs/starfarer/BrowserGameplayProbe";
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyControlMatcherGameplayProbePatch jar");
        int[] returns={0}, hooks={0}, bindingCalls={0};
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile())){
            JarEntry e=jar.getJarEntry(TARGET); if(e==null)throw new AssertionError("missing "+TARGET);
            try(InputStream in=jar.getInputStream(e)){
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){
                    @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){
                        if(!METHOD.equals(n)||!DESC.equals(d))return null;
                        return new MethodVisitor(Opcodes.ASM9){
                            @Override public void visitInsn(int op){if(op==Opcodes.IRETURN)returns[0]++;}
                            @Override public void visitMethodInsn(int op,String owner,String name,String md,boolean itf){
                                if(op==Opcodes.INVOKESTATIC&&PROBE.equals(owner)&&"controlMatch".equals(name))hooks[0]++;
                                if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/starfarer/title/B/B$Oo".equals(owner)&&"o00000".equals(name)&&"(Lcom/fs/starfarer/util/super/Object;Lcom/fs/starfarer/title/B/B$o;)Z".equals(md))bindingCalls[0]++;
                            }
                        };}
                },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            }
        }
        if(returns[0]!=3||hooks[0]!=3||bindingCalls[0]!=2)throw new AssertionError("unexpected matcher shape returns="+returns[0]+" hooks="+hooks[0]+" bindingCalls="+bindingCalls[0]);
        System.out.println("VerifyControlMatcherGameplayProbePatch: OK returns=3 hooks=3 bindingCalls=2");
    }
}
