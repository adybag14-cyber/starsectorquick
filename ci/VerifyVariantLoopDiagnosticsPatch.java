import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Verifies aggregate variant-loop timers while all original variant loader semantics remain. */
public final class VerifyVariantLoopDiagnosticsPatch {
    private static final String ENTRY="com/fs/starfarer/loading/SpecStore.class";
    private static final String METHOD="oO0000",DESC="()V";
    private static final String H="com/fs/starfarer/loading/BrowserVariantLoopDiag";
    public static void main(String[]args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: VerifyVariantLoopDiagnosticsPatch patched.jar helper.jar");
        Map<String,Integer> helper=new HashMap<>();int[] methods={0},json={0},ctor={0},reg={0},rel={0},source={0},path={0},variantId={0};
        try(JarFile helperJar=new JarFile(Path.of(args[1]).toFile())){if(helperJar.getJarEntry("com/fs/starfarer/loading/BrowserVariantLoopDiag.class")==null)throw new AssertionError("missing helper class in "+args[1]);}
        try(JarFile j=new JarFile(Path.of(args[0]).toFile())){JarEntry e=j.getJarEntry(ENTRY);if(e==null)throw new AssertionError("missing SpecStore");try(InputStream in=j.getInputStream(e)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;methods[0]++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitLdcInsn(Object v){if("variantId".equals(v))variantId[0]++;}@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&H.equals(o))helper.merge(n,1,Integer::sum);if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/LoadingUtils".equals(o)&&"(Ljava/lang/String;)Lorg/json/JSONObject;".equals(d))json[0]++;if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/loading/specs/HullVariantSpec".equals(o)&&"<init>".equals(n)&&"(Lorg/json/JSONObject;)V".equals(d))ctor[0]++;if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/new".equals(o)&&"(Lcom/fs/starfarer/loading/specs/HullVariantSpec;Z)V".equals(d))reg[0]++;if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/new".equals(o)&&"()Ljava/util/List;".equals(d))rel[0]++;if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/starfarer/loading/specs/HullVariantSpec".equals(o)&&"setSource".equals(n))source[0]++;if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/starfarer/loading/specs/HullVariantSpec".equals(o)&&"setSourcePath".equals(n))path[0]++;}};}},0);}}
        String[] names={"begin","parseStart","parseEnd","ctorStart","ctorEnd","registerStart","registerEnd","finishLoop"};for(String n:names)if(helper.getOrDefault(n,0)!=1)throw new AssertionError("helper calls="+helper);
        if(methods[0]!=1||json[0]!=1||ctor[0]!=1||reg[0]!=1||rel[0]!=1||source[0]!=1||path[0]!=1||variantId[0]<1)throw new AssertionError("variant semantics missing methods="+methods[0]+" json="+json[0]+" ctor="+ctor[0]+" reg="+reg[0]+" rel="+rel[0]+" source="+source[0]+" path="+path[0]+" variantId="+variantId[0]);
        System.out.println("VerifyVariantLoopDiagnosticsPatch: OK aggregate timers, JSON/constructor/registration semantics retained");
    }
}
