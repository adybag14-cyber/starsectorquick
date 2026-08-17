import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyCampaignXStreamNameCoderPatch {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyCampaignXStreamNameCoderPatch jar");
        final int[] stockCtor={0}, fastCtor={0};
        try (JarFile jar=new JarFile(Path.of(args[0]).toFile())) {
            JarEntry e=jar.getJarEntry("com/fs/starfarer/campaign/save/CampaignGameManager$5.class");
            if(e==null) throw new AssertionError("missing driver");
            try(InputStream in=jar.getInputStream(e)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] x) {
                        if(!"<init>".equals(n)||!"()V".equals(d)) return null;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int op,String owner,String mn,String md,boolean itf) {
                                if(op==Opcodes.INVOKESPECIAL && "com/thoughtworks/xstream/io/xml/StaxDriver".equals(owner) && "<init>".equals(mn)) {
                                    if("()V".equals(md)) stockCtor[0]++;
                                    if("(Lcom/thoughtworks/xstream/io/naming/NameCoder;)V".equals(md)) fastCtor[0]++;
                                }
                            }
                        };
                    }
                },0);
            }
        }
        if(stockCtor[0]!=0||fastCtor[0]!=1) throw new AssertionError("driver ctor shape stock="+stockCtor[0]+" fast="+fastCtor[0]);
        System.out.println("VerifyCampaignXStreamNameCoderPatch: OK fastCtor=1");
    }
}
