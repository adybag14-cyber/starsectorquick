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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Browser fast-path hook for SpecStore.Object(String); full regex implementation remains fallback. */
public final class PatchBrowserTextPreprocessor {
    private static final String ENTRY="com/fs/starfarer/loading/SpecStore.class";
    private static final String METHOD="Object";
    private static final String DESC="(Ljava/lang/String;)Ljava/lang/String;";
    private static final String HELPER="com/fs/starfarer/loading/BrowserTextPreprocessor";
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchBrowserTextPreprocessor input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]);int[] classes={0},methods={0},hooks={0};
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){Enumeration<JarEntry> es=jar.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry copy=new JarEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);byte[] bytes;try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,methods,hooks);}out.write(bytes);out.closeEntry();}}
        if(classes[0]!=1||methods[0]!=1||hooks[0]!=1){Files.deleteIfExists(output);throw new IllegalStateException("text preprocessor patch mismatch classes="+classes[0]+" methods="+methods[0]+" hooks="+hooks[0]);}
        System.out.println("Patched browser text preprocessor hooks=1 stock regex fallback retained");
    }
    private static byte[] patch(byte[] input,int[]methods,int[]hooks){int existing=count(input);if(existing==1){methods[0]=1;hooks[0]=1;System.out.println("Browser text preprocessor hook already present; leaving bytecode unchanged.");return input;}if(existing!=0)throw new IllegalStateException("unexpected existing hooks="+existing);
        ClassReader r=new ClassReader(input);ClassWriter w=new SafeClassWriter(r,ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){MethodVisitor mv=super.visitMethod(a,n,d,s,ex);if(!METHOD.equals(n)||!DESC.equals(d))return mv;methods[0]++;return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();Label stock=new Label();super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"normalizeIfEnabled",DESC,false);hooks[0]++;super.visitInsn(Opcodes.DUP);super.visitJumpInsn(Opcodes.IFNULL,stock);super.visitInsn(Opcodes.ARETURN);super.visitLabel(stock);super.visitInsn(Opcodes.POP);}};}},0);return w.toByteArray();}
    private static int count(byte[] input){int[] c={0};new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&HELPER.equals(o)&&"normalizeIfEnabled".equals(n)&&DESC.equals(d))c[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c[0];}
    static final class SafeClassWriter extends ClassWriter{SafeClassWriter(ClassReader r,int f){super(r,f);}@Override protected String getCommonSuperClass(String a,String b){return "java/lang/Object";}}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
