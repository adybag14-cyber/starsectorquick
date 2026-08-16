import java.io.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public final class VerifySectorGenTimingPatch {
    private static final String TARGET="data/scripts/world/SectorGen.class";
    private static final String METHOD="runSectorStep";
    private static final String DESC="(Ljava/lang/String;ZLdata/scripts/world/SectorGen$SectorStep;)V";
    private static final String HELPER="com/fs/starfarer/BrowserProcgenTiming";
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("usage: VerifySectorGenTimingPatch jar");
        int[] methods={0}, begin={0}, end={0}, run={0};
        try(JarFile jar=new JarFile(args[0])) {
            JarEntry e=jar.getJarEntry(TARGET); if(e==null) throw new AssertionError("SectorGen missing");
            byte[] b; try(InputStream in=jar.getInputStream(e)){b=in.readAllBytes();}
            new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex) {
                    if(!METHOD.equals(n)||!DESC.equals(d)) return null; methods[0]++;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf) {
                            if(op==Opcodes.INVOKEINTERFACE && owner.endsWith("SectorGen$SectorStep") && "run".equals(name)) run[0]++;
                            if(op==Opcodes.INVOKESTATIC && HELPER.equals(owner) && "stepBegin".equals(name)) begin[0]++;
                            if(op==Opcodes.INVOKESTATIC && HELPER.equals(owner) && "stepEnd".equals(name)) end[0]++;
                        }
                    };
                }
            },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
        if(methods[0]!=1||run[0]!=1||begin[0]!=1||end[0]!=1)
            throw new AssertionError("timing patch structure methods="+methods[0]+" run="+run[0]+" begin="+begin[0]+" end="+end[0]);
        System.out.println("VerifySectorGenTimingPatch: OK");
    }
}
