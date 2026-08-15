import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Test-only telemetry around CampaignState core-tab open/dismiss paths. */
public final class PatchCampaignGameplayProbe {
    private static final String ENTRY="com/fs/starfarer/campaign/CampaignState.class";
    private static final String CLASS="com/fs/starfarer/campaign/CampaignState";
    private static final String PROBE="com/fs/starfarer/BrowserGameplayProbe";
    private static final String TAB="Lcom/fs/starfarer/api/campaign/CoreUITabId;";
    private static final String ONE="("+TAB+")V";
    private static final String TWO="("+TAB+"Ljava/lang/Object;)V";
    private static final String DISMISS="()V";
    public static void main(String[] a)throws Exception{
        if(a.length!=2)throw new IllegalArgumentException("usage: PatchCampaignGameplayProbe input.jar output.jar");
        Path in=Path.of(a[0]),out=Path.of(a[1]);int[] classes={0},methods={0},starts={0},ready={0},dismiss={0};
        try(JarFile j=new JarFile(in.toFile());JarOutputStream o=new JarOutputStream(Files.newOutputStream(out))){Enumeration<JarEntry> es=j.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry c=new JarEntry(e.getName());c.setTime(e.getTime());o.putNextEntry(c);byte[] b;try(InputStream x=j.getInputStream(e)){b=x.readAllBytes();}if(ENTRY.equals(e.getName())){classes[0]++;b=patch(b,methods,starts,ready,dismiss);}o.write(b);o.closeEntry();}}
        if(classes[0]!=1||methods[0]!=3||starts[0]!=2||ready[0]<2||dismiss[0]!=1){Files.deleteIfExists(out);throw new IllegalStateException("campaign gameplay probe mismatch classes="+classes[0]+" methods="+methods[0]+" starts="+starts[0]+" ready="+ready[0]+" dismiss="+dismiss[0]);}
        System.out.println("Patched Campaign gameplay probe methods=3 starts=2 ready="+ready[0]+" dismiss=1");
    }
    private static byte[] patch(byte[] in,int[] methods,int[] starts,int[] ready,int[] dismiss){
        int existing=count(in);if(existing>0){if(existing<5)throw new IllegalStateException("partial campaign gameplay probe calls="+existing);methods[0]=3;starts[0]=2;dismiss[0]=1;ready[0]=existing-3;return in;}
        ClassReader r=new ClassReader(in);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int ac,String n,String d,String s,String[]ex){MethodVisitor mv=super.visitMethod(ac,n,d,s,ex);boolean one="showCoreUITab".equals(n)&&ONE.equals(d),two="showCoreUITab".equals(n)&&TWO.equals(d),dis="notifyCoreUIDismissed".equals(n)&&DISMISS.equals(d);if(!one&&!two&&!dis)return mv;methods[0]++;return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();if(one||two){super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,PROBE,"coreTabStart","(Ljava/lang/Object;)V",false);starts[0]++;}else{super.visitMethodInsn(Opcodes.INVOKESTATIC,PROBE,"coreUiDismissed","()V",false);dismiss[0]++;}}@Override public void visitInsn(int op){if(op==Opcodes.RETURN&&(one||two)){super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,PROBE,"coreTabReady","(Ljava/lang/Object;)V",false);ready[0]++;}super.visitInsn(op);}};}},0);return w.toByteArray();
    }
    private static int count(byte[] b){final int[] c={0};new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean i){if(op==Opcodes.INVOKESTATIC&&PROBE.equals(o)&&(n.startsWith("coreTab")||"coreUiDismissed".equals(n)))c[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c[0];}
}
