import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies fast normalizer precedes, but does not remove, the exact two stock regex passes. */
public final class VerifyBrowserTextPreprocessorPatch {
    private static final String ENTRY="com/fs/starfarer/loading/SpecStore.class",METHOD="Object",DESC="(Ljava/lang/String;)Ljava/lang/String;",H="com/fs/starfarer/loading/BrowserTextPreprocessor";
    public static void main(String[]args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("usage: VerifyBrowserTextPreprocessorPatch jar");C c=new C();try(JarFile j=new JarFile(Path.of(args[0]).toFile())){JarEntry e=j.getJarEntry(ENTRY);if(e==null)throw new AssertionError("missing SpecStore");try(InputStream in=j.getInputStream(e)){new ClassReader(in).accept(visitor(c),0);}}
        if(c.methods!=1||c.helper!=1||c.helperAt<0||c.firstRegexAt<0||c.helperAt>=c.firstRegexAt||c.regex!=2||c.doublePattern!=1||c.singlePattern!=1||c.returns<2)throw new AssertionError("preprocessor patch methods="+c.methods+" helper="+c.helper+" helperAt="+c.helperAt+" firstRegex="+c.firstRegexAt+" regex="+c.regex+" double="+c.doublePattern+" single="+c.singlePattern+" returns="+c.returns);
        System.out.println("VerifyBrowserTextPreprocessorPatch: OK helper before two stock regex fallbacks");}
    private static ClassVisitor visitor(C c){return new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;c.methods++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){int at=++c.seq;if(op==Opcodes.INVOKESTATIC&&H.equals(o)&&"normalizeIfEnabled".equals(n)){c.helper++;c.helperAt=at;}if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/String".equals(o)&&"replaceAll".equals(n)){c.regex++;if(c.firstRegexAt<0)c.firstRegexAt=at;}}@Override public void visitLdcInsn(Object v){if("[\\u201c\\u201d]+".equals(v))c.doublePattern++;if("[\\u2018\\u2019\\ufffd]+".equals(v))c.singlePattern++;}@Override public void visitInsn(int op){if(op==Opcodes.ARETURN)c.returns++;}};}};}
    static final class C{int methods,helper,helperAt=-1,firstRegexAt=-1,regex,doublePattern,singlePattern,returns,seq;}
}
