import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies aggregate A(String)/Misc.tokenize instrumentation and retained expression semantics. */
public final class VerifyRuleExpressionDiagnosticsPatch {
    private static final String RULES="com/fs/starfarer/campaign/rules/Rules.class",EXPR="com/fs/starfarer/campaign/rules/A.class",H="com/fs/starfarer/campaign/rules/BrowserRuleExpressionDiag";
    public static void main(String[]args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("usage: VerifyRuleExpressionDiagnosticsPatch jar");C c=new C();try(JarFile j=new JarFile(Path.of(args[0]).toFile())){inspect(j,RULES,c);inspect(j,EXPR,c);}if(c.h.getOrDefault("beginRules",0)!=1||c.h.getOrDefault("finishRules",0)!=1||c.h.getOrDefault("expressionStart",0)!=1||c.h.getOrDefault("tokenizeStart",0)!=1||c.h.getOrDefault("tokenizeEnd",0)!=1||c.h.getOrDefault("expressionEnd",0)!=1)throw new AssertionError("helper calls="+c.h);if(c.miscTokenize!=1||c.trim<1||c.isOperator<1||c.ruleException<1||c.returnInsn<1)throw new AssertionError("expression semantics missing tokenize="+c.miscTokenize+" trim="+c.trim+" isOperator="+c.isOperator+" RuleException="+c.ruleException+" returns="+c.returnInsn);System.out.println("VerifyRuleExpressionDiagnosticsPatch: OK aggregate expression/tokenize timer, constructor semantics retained");}
    private static void inspect(JarFile j,String entry,C c)throws Exception{JarEntry e=j.getJarEntry(entry);if(e==null)throw new AssertionError("missing "+entry);try(InputStream in=j.getInputStream(e)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){boolean target=(RULES.equals(entry)&&"super".equals(n)&&"(Lcom/fs/starfarer/loading/ResourceLoaderState;)V".equals(d))||(EXPR.equals(entry)&&"<init>".equals(n)&&"(Ljava/lang/String;)V".equals(d));if(!target)return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&H.equals(o))c.h.merge(n,1,Integer::sum);if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/api/util/Misc".equals(o)&&"tokenize".equals(n))c.miscTokenize++;if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/String".equals(o)&&"trim".equals(n))c.trim++;if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/starfarer/api/util/Misc$Token".equals(o)&&"isOperator".equals(n))c.isOperator++;if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/api/util/RuleException".equals(o)&&"<init>".equals(n))c.ruleException++;}@Override public void visitInsn(int op){if(op==Opcodes.RETURN)c.returnInsn++;}};}},0);}}
    static final class C{int miscTokenize,trim,isOperator,ruleException,returnInsn;final Map<String,Integer>h=new HashMap<String,Integer>();}
}
