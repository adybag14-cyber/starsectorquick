import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyGraphicsPredecodeWorkerGuardPatch {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifyGraphicsPredecodeWorkerGuardPatch patched.jar");
        int[] methods={0},property={0},bool={0},alive={0},threadNew={0},threadStart={0},threadPut={0},returns={0},seq={0},guardAt={-1},newAt={-1};
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile())){JarEntry e=jar.getJarEntry("com/fs/graphics/L.class");if(e==null)throw new AssertionError("missing L");try(InputStream in=jar.getInputStream(e)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){if(!"o00000".equals(n)||!"()V".equals(d))return null;methods[0]++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitLdcInsn(Object v){if("starsector.browserEarlyImagePredecode".equals(v))property[0]++;}@Override public void visitTypeInsn(int op,String type){int i=++seq[0];if(op==Opcodes.NEW&&"java/lang/Thread".equals(type)){threadNew[0]++;if(newAt[0]<0)newAt[0]=i;}}@Override public void visitFieldInsn(int op,String owner,String name,String desc){if(op==Opcodes.PUTSTATIC&&"com/fs/graphics/L".equals(owner)&&"o00000".equals(name)&&"Ljava/lang/Thread;".equals(desc))threadPut[0]++;}@Override public void visitInsn(int op){if(op==Opcodes.RETURN)returns[0]++;}@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){int i=++seq[0];if(op==Opcodes.INVOKESTATIC&&"java/lang/Boolean".equals(owner)&&"getBoolean".equals(name)){bool[0]++;}if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/Thread".equals(owner)&&"isAlive".equals(name)){alive[0]++;if(guardAt[0]<0)guardAt[0]=i;}if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/Thread".equals(owner)&&"start".equals(name)){threadStart[0]++;}}};}},0);}}
        if(methods[0]!=1||property[0]!=1||bool[0]!=1||alive[0]!=1||threadNew[0]!=1||threadStart[0]!=1||threadPut[0]!=1||returns[0]<2||guardAt[0]<0||newAt[0]<0||guardAt[0]>=newAt[0])throw new AssertionError("worker guard mismatch methods="+methods[0]+" property="+property[0]+" bool="+bool[0]+" alive="+alive[0]+" new="+threadNew[0]+" start="+threadStart[0]+" put="+threadPut[0]+" returns="+returns[0]+" order="+guardAt[0]+"/"+newAt[0]);
        System.out.println("VerifyGraphicsPredecodeWorkerGuardPatch: OK guarded start retains stock worker creation");
    }
}
