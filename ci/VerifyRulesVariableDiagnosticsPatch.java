import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies Rules keeps its parse/registration body but no longer runs warning-only tail. */
public final class VerifyRulesVariableDiagnosticsPatch {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifyRulesVariableDiagnosticsPatch patched.jar");
        int[] methods={0},arrayLists={0},systemOut={0},ruleCtors={0},csvLoads={0},returns={0};
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile())){JarEntry e=jar.getJarEntry("com/fs/starfarer/campaign/rules/Rules.class");if(e==null)throw new AssertionError("missing Rules.class");try(InputStream in=jar.getInputStream(e)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){if(!"super".equals(n)||!"(Lcom/fs/starfarer/loading/ResourceLoaderState;)V".equals(d))return null;methods[0]++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitTypeInsn(int op,String type){if(op==Opcodes.NEW&&"java/util/ArrayList".equals(type))arrayLists[0]++;}@Override public void visitFieldInsn(int op,String owner,String name,String desc){if(op==Opcodes.GETSTATIC&&"java/lang/System".equals(owner)&&"out".equals(name))systemOut[0]++;}@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/campaign/rules/ooOO".equals(owner)&&"<init>".equals(name))ruleCtors[0]++;if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/LoadingUtils".equals(owner)&&"(Ljava/util/List;Ljava/lang/String;ZZ)Lorg/json/JSONArray;".equals(desc))csvLoads[0]++;}@Override public void visitInsn(int op){if(op==Opcodes.RETURN)returns[0]++;}};}},0);}}
        if(methods[0]!=1||arrayLists[0]!=1||systemOut[0]!=0||ruleCtors[0]<1||csvLoads[0]!=1||returns[0]<1)throw new AssertionError("Rules patch mismatch methods="+methods[0]+" arrayLists="+arrayLists[0]+" systemOut="+systemOut[0]+" ruleCtors="+ruleCtors[0]+" csvLoads="+csvLoads[0]+" returns="+returns[0]);
        System.out.println("VerifyRulesVariableDiagnosticsPatch: OK parse retained, warning tail removed");
    }
}
