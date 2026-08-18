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

/** Prevent a second stock image-predecode worker while the early worker is still alive. */
public final class PatchGraphicsPredecodeWorkerGuard {
    private static final String ENTRY="com/fs/graphics/L.class";
    private static final String OWNER="com/fs/graphics/L";
    private static final String METHOD="o00000";
    private static final String DESC="()V";
    private static final String THREAD_FIELD="o00000";
    private static final String PROPERTY="starsector.browserEarlyImagePredecode";
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchGraphicsPredecodeWorkerGuard input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]); int[] classes={0},methods={0},guards={0};
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries(); while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry c=new JarEntry(e.getName());c.setTime(e.getTime());out.putNextEntry(c);byte[] b;try(InputStream in=jar.getInputStream(e)){b=readAll(in);}if(ENTRY.equals(e.getName())){classes[0]++;b=patch(b,methods,guards);}out.write(b);out.closeEntry();}
        }
        if(classes[0]!=1||methods[0]!=1||guards[0]!=1){Files.deleteIfExists(output);throw new IllegalStateException("predecode worker guard mismatch classes="+classes[0]+" methods="+methods[0]+" guards="+guards[0]);}
        System.out.println("Patched graphics predecode worker idempotent start guard=1");
    }
    private static byte[] patch(byte[] input,int[] methods,int[] guards){
        int[] property={0},alive={0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitLdcInsn(Object v){if(PROPERTY.equals(v))property[0]++;}@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/Thread".equals(owner)&&"isAlive".equals(name)&&"()Z".equals(desc))alive[0]++;}};}},0);
        if(property[0]==1&&alive[0]==1){methods[0]=1;guards[0]=1;return input;}
        if(property[0]!=0||alive[0]!=0)throw new IllegalStateException("partial predecode worker guard property="+property[0]+" alive="+alive[0]);
        ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){MethodVisitor mv=super.visitMethod(a,n,d,s,ex);if(!METHOD.equals(n)||!DESC.equals(d))return mv;methods[0]++;return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();Label stock=new Label();super.visitLdcInsn(PROPERTY);super.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/Boolean","getBoolean","(Ljava/lang/String;)Z",false);super.visitJumpInsn(Opcodes.IFEQ,stock);super.visitFieldInsn(Opcodes.GETSTATIC,OWNER,THREAD_FIELD,"Ljava/lang/Thread;");super.visitJumpInsn(Opcodes.IFNULL,stock);super.visitFieldInsn(Opcodes.GETSTATIC,OWNER,THREAD_FIELD,"Ljava/lang/Thread;");super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/Thread","isAlive","()Z",false);super.visitJumpInsn(Opcodes.IFEQ,stock);super.visitInsn(Opcodes.RETURN);super.visitLabel(stock);super.visitFrame(Opcodes.F_SAME,0,null,0,null);guards[0]++;}};}},0);return w.toByteArray();
    }
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
