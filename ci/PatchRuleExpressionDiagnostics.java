import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Diagnostic-only aggregate timing around Rules A(String) and Misc.tokenize(String). */
public final class PatchRuleExpressionDiagnostics {
    private static final String RULES_ENTRY="com/fs/starfarer/campaign/rules/Rules.class";
    private static final String EXPR_ENTRY="com/fs/starfarer/campaign/rules/A.class";
    private static final String RULES_METHOD="super";
    private static final String RULES_DESC="(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    private static final String CTOR="<init>";
    private static final String CTOR_DESC="(Ljava/lang/String;)V";
    private static final String MISC="com/fs/starfarer/api/util/Misc";
    private static final String TOKENIZE_DESC="(Ljava/lang/String;)Ljava/util/List;";
    private static final String HELPER="com/fs/starfarer/campaign/rules/BrowserRuleExpressionDiag";

    public static void main(String[]args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchRuleExpressionDiagnostics input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]);Counts c=new Counts();
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){Enumeration<JarEntry>es=jar.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry copy=new JarEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);byte[]bytes;try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}if(RULES_ENTRY.equals(e.getName())){c.rulesClasses++;bytes=patchRules(bytes,c);}else if(EXPR_ENTRY.equals(e.getName())){c.exprClasses++;bytes=patchExpr(bytes,c);}out.write(bytes);out.closeEntry();}}
        require(c);System.out.println("Patched Rules expression diagnostics Rules=2 expression=4 tokenize=1");
    }
    private static byte[] patchRules(byte[]input,Counts c){int existing=countHelper(input,"beginRules")+countHelper(input,"finishRules");if(existing==2){c.rulesMethods=1;c.rulesBegin=1;c.rulesFinish=1;return input;}if(existing!=0)throw new IllegalStateException("partial Rules expression diag calls="+existing);ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){MethodVisitor mv=super.visitMethod(a,n,d,s,ex);if(!RULES_METHOD.equals(n)||!RULES_DESC.equals(d))return mv;c.rulesMethods++;return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"beginRules","()V",false);c.rulesBegin++;}@Override public void visitInsn(int op){if(op==Opcodes.RETURN){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"finishRules","()V",false);c.rulesFinish++;}super.visitInsn(op);}};}},0);return w.toByteArray();}
    private static byte[] patchExpr(byte[]input,Counts c){int existing=countHelper(input,"expressionStart")+countHelper(input,"tokenizeStart")+countHelper(input,"tokenizeEnd")+countHelper(input,"expressionEnd");if(existing==4){c.exprMethods=1;c.exprStart=1;c.tokenStart=1;c.tokenEnd=1;c.exprEnd=1;c.tokenizeCalls=1;return input;}if(existing!=0)throw new IllegalStateException("partial expression diag calls="+existing);ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){MethodVisitor mv=super.visitMethod(a,n,d,s,ex);if(!CTOR.equals(n)||!CTOR_DESC.equals(d))return mv;c.exprMethods++;return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"expressionStart","()V",false);c.exprStart++;}@Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&MISC.equals(owner)&&"tokenize".equals(n)&&TOKENIZE_DESC.equals(d)){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"tokenizeStart","()V",false);c.tokenStart++;super.visitMethodInsn(op,owner,n,d,itf);super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"tokenizeEnd","()V",false);c.tokenEnd++;c.tokenizeCalls++;return;}super.visitMethodInsn(op,owner,n,d,itf);}@Override public void visitInsn(int op){if(op==Opcodes.RETURN){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"expressionEnd","()V",false);c.exprEnd++;}super.visitInsn(op);}};}},0);return w.toByteArray();}
    private static int countHelper(byte[]input,String name){int[]c={0};new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&HELPER.equals(o)&&name.equals(n))c[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c[0];}
    private static void require(Counts c){if(c.rulesClasses!=1||c.exprClasses!=1||c.rulesMethods!=1||c.rulesBegin!=1||c.rulesFinish!=1||c.exprMethods!=1||c.exprStart!=1||c.tokenStart!=1||c.tokenEnd!=1||c.exprEnd!=1||c.tokenizeCalls!=1)throw new IllegalStateException("expression diag mismatch rulesClasses="+c.rulesClasses+" exprClasses="+c.exprClasses+" rulesMethods="+c.rulesMethods+" begin="+c.rulesBegin+" finish="+c.rulesFinish+" exprMethods="+c.exprMethods+" exprStart="+c.exprStart+" tokenStart="+c.tokenStart+" tokenEnd="+c.tokenEnd+" exprEnd="+c.exprEnd+" tokenize="+c.tokenizeCalls);}
    static final class Counts{int rulesClasses,exprClasses,rulesMethods,rulesBegin,rulesFinish,exprMethods,exprStart,tokenStart,tokenEnd,exprEnd,tokenizeCalls;}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
