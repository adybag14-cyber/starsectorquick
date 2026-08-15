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

/** Verifies Rules phase profiler and that parsing/registration calls remain intact. */
public final class VerifyRulesPhaseDiagnosticsPatch {
    private static final String ENTRY="com/fs/starfarer/campaign/rules/Rules.class", METHOD="super", DESC="(Lcom/fs/starfarer/loading/ResourceLoaderState;)V", H="com/fs/starfarer/campaign/rules/BrowserRulesPhaseDiag", DUP="com/fs/starfarer/campaign/rules/BrowserRuleDuplicateIndex";
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifyRulesPhaseDiagnosticsPatch jar"); C c=new C();
        try(JarFile j=new JarFile(Path.of(args[0]).toFile())){JarEntry e=j.getJarEntry(ENTRY);if(e==null)throw new AssertionError("missing Rules");try(InputStream in=j.getInputStream(e)){new ClassReader(in).accept(visitor(c),0);}}
        if(c.methods!=1||c.helper.getOrDefault("begin",0)!=1||c.helper.getOrDefault("rowStart",0)!=1||c.helper.getOrDefault("transition",0)!=5||c.helper.getOrDefault("finish",0)!=1)throw new AssertionError("profiler calls="+c.helper);
        if(c.rowJson<1||c.jsonGetString<4||c.ruleCtor<1||c.conditionCtor<2||c.scriptStore<1||c.dupEnabled!=1||c.dupFinish!=1||c.ruleListAdd<1)throw new AssertionError("Rules semantics missing rowJson="+c.rowJson+" getString="+c.jsonGetString+" ruleCtor="+c.ruleCtor+" conditionCtor="+c.conditionCtor+" scriptStore="+c.scriptStore+" dupEnabled="+c.dupEnabled+" dupFinish="+c.dupFinish+" listAdd="+c.ruleListAdd);
        System.out.println("VerifyRulesPhaseDiagnosticsPatch: OK aggregate profiler calls=8 Rules parsing/registration retained");
    }
    private static ClassVisitor visitor(C c){return new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;c.methods++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){
        if(op==Opcodes.INVOKESTATIC&&H.equals(o))c.helper.merge(n,1,Integer::sum);
        if("org/json/JSONArray".equals(o)&&"getJSONObject".equals(n))c.rowJson++;
        if("org/json/JSONObject".equals(o)&&"getString".equals(n))c.jsonGetString++;
        if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/campaign/rules/ooOO".equals(o)&&"<init>".equals(n))c.ruleCtor++;
        if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/campaign/rules/A".equals(o)&&"<init>".equals(n))c.conditionCtor++;
        if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/scripts/ScriptStore".equals(o)&&"(Ljava/lang/String;)V".equals(d))c.scriptStore++;
        if(op==Opcodes.INVOKESTATIC&&DUP.equals(o)&&"enabled".equals(n))c.dupEnabled++;
        if(op==Opcodes.INVOKESTATIC&&DUP.equals(o)&&"finish".equals(n))c.dupFinish++;
        if(op==Opcodes.INVOKEINTERFACE&&"java/util/List".equals(o)&&"add".equals(n))c.ruleListAdd++;
    }};}};}
    static final class C{int methods,rowJson,jsonGetString,ruleCtor,conditionCtor,scriptStore,dupEnabled,dupFinish,ruleListAdd;final Map<String,Integer>helper=new HashMap<String,Integer>();}
}
