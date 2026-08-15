import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies fixed CR/LF cleanup is literal while the real whitespace regex and Rules body remain. */
public final class VerifyRulesLiteralStringCleanupPatch {
    private static final String TARGET="com/fs/starfarer/campaign/rules/Rules.class";
    private static final String DESC="(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifyRulesLiteralStringCleanupPatch patched.jar");
        int[] methods={0},replaceAll={0},literalCr={0},literalLf={0},whitespacePattern={0},csvLoads={0},ruleCtors={0},scriptStore={0},splitCalls={0};
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile())){
            JarEntry e=jar.getJarEntry(TARGET);if(e==null)throw new AssertionError("missing Rules.class");
            try(InputStream in=jar.getInputStream(e)){
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        if(!"super".equals(name)||!DESC.equals(descriptor))return null;methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9){String prev,pattern,replacement;boolean prevString;
                            void reset(){prev=null;pattern=null;replacement=null;prevString=false;}
                            @Override public void visitLdcInsn(Object value){if(value instanceof String){String s=(String)value;if("\\s+$".equals(s))whitespacePattern[0]++;if(prevString){pattern=prev;replacement=s;}else{pattern=null;replacement=null;}prev=s;prevString=true;}else reset();}
                            @Override public void visitMethodInsn(int op,String owner,String method,String d,boolean itf){
                                if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/String".equals(owner)){
                                    if("replaceAll".equals(method)&&"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(d))replaceAll[0]++;
                                    if("replace".equals(method)&&"(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;".equals(d)){
                                        if("\r".equals(pattern)&&"".equals(replacement))literalCr[0]++;
                                        if("\n".equals(pattern)&&" ".equals(replacement))literalLf[0]++;
                                    }
                                    if("split".equals(method))splitCalls[0]++;
                                }
                                if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/LoadingUtils".equals(owner)&&"(Ljava/util/List;Ljava/lang/String;ZZ)Lorg/json/JSONArray;".equals(d))csvLoads[0]++;
                                if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/campaign/rules/ooOO".equals(owner)&&"<init>".equals(method))ruleCtors[0]++;
                                if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/scripts/ScriptStore".equals(owner)&&"(Ljava/lang/String;)V".equals(d))scriptStore[0]++;
                                reset();
                            }
                            @Override public void visitInsn(int o){reset();}@Override public void visitIntInsn(int o,int v){reset();}@Override public void visitVarInsn(int o,int v){reset();}
                            @Override public void visitTypeInsn(int o,String t){reset();}@Override public void visitFieldInsn(int o,String ow,String n,String d){reset();}
                            @Override public void visitJumpInsn(int o,Label l){reset();}@Override public void visitIincInsn(int v,int i){reset();}
                            @Override public void visitInvokeDynamicInsn(String n,String d,Handle h,Object...a){reset();}
                        };
                    }
                },0);
            }
        }
        if(methods[0]!=1||replaceAll[0]!=1||literalCr[0]!=3||literalLf[0]!=1||whitespacePattern[0]!=1||csvLoads[0]!=1||ruleCtors[0]<1||scriptStore[0]<1||splitCalls[0]<3)
            throw new AssertionError("Rules literal cleanup verifier mismatch methods="+methods[0]+" replaceAll="+replaceAll[0]+" literalCr="+literalCr[0]+" literalLf="+literalLf[0]+" whitespacePattern="+whitespacePattern[0]+" csvLoads="+csvLoads[0]+" ruleCtors="+ruleCtors[0]+" scriptStore="+scriptStore[0]+" splitCalls="+splitCalls[0]);
        System.out.println("VerifyRulesLiteralStringCleanupPatch: OK literalCr=3 literalLf=1 realRegex=1 Rules body retained");
    }
}
