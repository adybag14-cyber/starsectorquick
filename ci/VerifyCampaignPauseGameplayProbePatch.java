import java.io.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public final class VerifyCampaignPauseGameplayProbePatch {
    private static final String TARGET="com/fs/starfarer/campaign/CampaignEngine.class";
    private static final String PROBE="com/fs/starfarer/BrowserGameplayProbe";
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("usage: VerifyCampaignPauseGameplayProbePatch jar");
        int[] methods={0}, hooks={0}, pausedWrites={0};
        try(JarFile jar=new JarFile(args[0])) {
            JarEntry e=jar.getJarEntry(TARGET); if(e==null) throw new IllegalStateException("missing "+TARGET);
            byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=in.readAllBytes();}
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
                @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
                    if(!"setPaused".equals(name)||!"(Z)V".equals(desc)) return null;
                    methods[0]++;
                    return new MethodVisitor(Opcodes.ASM9){
                        @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
                            if(op==Opcodes.INVOKESTATIC&&PROBE.equals(owner)&&"pauseTransition".equals(name)&&"(ZZ)V".equals(desc)) hooks[0]++;
                        }
                        @Override public void visitFieldInsn(int op,String owner,String name,String desc){
                            if(op==Opcodes.PUTFIELD&&"com/fs/starfarer/campaign/CampaignEngine".equals(owner)&&"paused".equals(name)&&"Z".equals(desc)) pausedWrites[0]++;
                        }
                    };
                }
            },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
        if(methods[0]!=1||hooks[0]!=1||pausedWrites[0]!=1) throw new IllegalStateException("campaign pause verify methods="+methods[0]+" hooks="+hooks[0]+" pausedWrites="+pausedWrites[0]);
        System.out.println("VerifyCampaignPauseGameplayProbePatch: OK methods=1 hooks=1 pausedWrites=1");
    }
}
