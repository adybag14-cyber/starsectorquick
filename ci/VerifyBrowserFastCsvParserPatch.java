import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Proves fast hook ordering and preservation of Starsector's complete fallback CSV parser. */
public final class VerifyBrowserFastCsvParserPatch {
    private static final String ENTRY="com/fs/starfarer/loading/oOoO.class";
    private static final String METHOD="o00000", DESC="(Ljava/lang/String;)Lorg/json/JSONArray;";
    private static final String HELPER="com/fs/starfarer/loading/BrowserFastCsvParser";
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifyBrowserFastCsvParserPatch jar"); C c=new C();
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile())){JarEntry e=jar.getJarEntry(ENTRY);if(e==null)throw new AssertionError("missing oOoO");try(InputStream in=jar.getInputStream(e)){new ClassReader(in).accept(visitor(c),0);}}
        if(c.methods!=1||c.helper!=1||c.helperAt<0||c.firstReplaceAllAt<0||c.helperAt>=c.firstReplaceAllAt)throw new AssertionError("fast hook order methods="+c.methods+" helper="+c.helper+" helperAt="+c.helperAt+" replaceAt="+c.firstReplaceAllAt);
        if(c.replaceAll<2||c.jsonObjectPut<1||c.jsonArrayPut<1||c.stringBufferNew<2||c.mismatchText!=1||c.returns<2)throw new AssertionError("stock fallback incomplete replaceAll="+c.replaceAll+" objectPut="+c.jsonObjectPut+" arrayPut="+c.jsonArrayPut+" stringBuffers="+c.stringBufferNew+" mismatch="+c.mismatchText+" returns="+c.returns);
        System.out.println("VerifyBrowserFastCsvParserPatch: OK fast hook before stock parser, malformed/error fallback retained");
    }
    private static ClassVisitor visitor(C c){return new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;c.methods++;return new MethodVisitor(Opcodes.ASM9){
        @Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){int at=++c.seq;if(op==Opcodes.INVOKESTATIC&&HELPER.equals(o)&&"parseIfEnabled".equals(n)&&DESC.equals(d)){c.helper++;if(c.helperAt<0)c.helperAt=at;}if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/String".equals(o)&&"replaceAll".equals(n)){c.replaceAll++;if(c.firstReplaceAllAt<0)c.firstReplaceAllAt=at;}if(op==Opcodes.INVOKEVIRTUAL&&"org/json/JSONObject".equals(o)&&"put".equals(n))c.jsonObjectPut++;if(op==Opcodes.INVOKEVIRTUAL&&"org/json/JSONArray".equals(o)&&"put".equals(n))c.jsonArrayPut++;}
        @Override public void visitTypeInsn(int op,String type){if(op==Opcodes.NEW&&"java/lang/StringBuffer".equals(type))c.stringBufferNew++;}
        @Override public void visitLdcInsn(Object v){if(v instanceof String&&((String)v).startsWith("Mismatched quotes in the string"))c.mismatchText++;}
        @Override public void visitInsn(int op){if(op==Opcodes.ARETURN)c.returns++;}
    };}};}
    static final class C{int methods,helper,helperAt=-1,firstReplaceAllAt=-1,replaceAll,jsonObjectPut,jsonArrayPut,stringBufferNew,mismatchText,returns,seq;}
}
