import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies only diagnostic writes were removed; rule/token parse structure remains. */
public final class VerifyRulesDeadVariableWritesPatch {
    private static final String DESC="(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifyRulesDeadVariableWritesPatch patched.jar");
        int[] methods={0},adds={0},puts={0},warningReads={0},systemOut={0},countingCtor={0},linkedCtor={0},first={0},second={0},params={0},isVar={0},csv={0},ruleCtor={0},scriptStore={0};
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile())){JarEntry e=jar.getJarEntry("com/fs/starfarer/campaign/rules/Rules.class");if(e==null)throw new AssertionError("missing Rules.class");try(InputStream in=jar.getInputStream(e)){
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){if(!"super".equals(name)||!DESC.equals(descriptor))return null;methods[0]++;return new MethodVisitor(Opcodes.ASM9){
                @Override public void visitMethodInsn(int op,String owner,String name,String d,boolean itf){
                    if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/starfarer/api/util/CountingMap".equals(owner)&&"add".equals(name))adds[0]++;
                    if(op==Opcodes.INVOKEINTERFACE&&"java/util/Map".equals(owner)&&"put".equals(name))puts[0]++;
                    if("com/fs/starfarer/api/util/CountingMap".equals(owner)&&("keySet".equals(name)||"getCount".equals(name)))warningReads[0]++;
                    if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/api/util/CountingMap".equals(owner)&&"<init>".equals(name))countingCtor[0]++;
                    if(op==Opcodes.INVOKESPECIAL&&"java/util/LinkedHashMap".equals(owner)&&"<init>".equals(name))linkedCtor[0]++;
                    if("com/fs/starfarer/campaign/rules/A".equals(owner)&&"getFirst".equals(name))first[0]++;
                    if("com/fs/starfarer/campaign/rules/A".equals(owner)&&"getSecond".equals(name))second[0]++;
                    if("com/fs/starfarer/campaign/rules/A".equals(owner)&&"getCommandParams".equals(name))params[0]++;
                    if("com/fs/starfarer/api/util/Misc$Token".equals(owner)&&"isVariable".equals(name))isVar[0]++;
                    if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/LoadingUtils".equals(owner)&&"(Ljava/util/List;Ljava/lang/String;ZZ)Lorg/json/JSONArray;".equals(d))csv[0]++;
                    if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/campaign/rules/ooOO".equals(owner)&&"<init>".equals(name))ruleCtor[0]++;
                    if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/scripts/ScriptStore".equals(owner)&&"(Ljava/lang/String;)V".equals(d))scriptStore[0]++;
                }
                @Override public void visitFieldInsn(int op,String owner,String name,String d){if(op==Opcodes.GETSTATIC&&"java/lang/System".equals(owner)&&"out".equals(name))systemOut[0]++;}
            };}},0);
        }}
        if(methods[0]!=1||adds[0]!=0||puts[0]!=0||warningReads[0]!=0||systemOut[0]!=0||countingCtor[0]!=1||linkedCtor[0]!=1||first[0]<6||second[0]<6||params[0]<4||isVar[0]<6||csv[0]!=1||ruleCtor[0]<1||scriptStore[0]<1)
            throw new AssertionError("Rules dead-write verifier mismatch methods="+methods[0]+" adds="+adds[0]+" puts="+puts[0]+" warningReads="+warningReads[0]+" systemOut="+systemOut[0]+" countingCtor="+countingCtor[0]+" linkedCtor="+linkedCtor[0]+" first="+first[0]+" second="+second[0]+" params="+params[0]+" isVar="+isVar[0]+" csv="+csv[0]+" ruleCtor="+ruleCtor[0]+" scriptStore="+scriptStore[0]);
        System.out.println("VerifyRulesDeadVariableWritesPatch: OK dead writes removed, token/rule parsing retained");
    }
}
