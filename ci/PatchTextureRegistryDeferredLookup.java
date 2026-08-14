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

/** Makes oOoO.new(key) materialize a deferred browser texture before registry lookup. */
public final class PatchTextureRegistryDeferredLookup {
    private static final String ENTRY="com/fs/graphics/oOoO.class";
    private static final String METHOD="new";
    private static final String DESC="(Ljava/lang/String;)Lcom/fs/graphics/Object;";
    private static final String HELPER="com/fs/starfarer/BrowserDeferredTextureQueue";

    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchTextureRegistryDeferredLookup input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]);int[] classes={0},methods={0},calls={0};
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry c=new JarEntry(e.getName());c.setTime(e.getTime());out.putNextEntry(c);byte[] bytes;try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,methods,calls);}out.write(bytes);out.closeEntry();}
        }
        if(classes[0]!=1||methods[0]!=1||calls[0]!=1){Files.deleteIfExists(output);throw new IllegalStateException("deferred registry patch mismatch classes="+classes[0]+" methods="+methods[0]+" calls="+calls[0]);}
        System.out.println("Patched texture registry deferred lookup calls="+calls[0]);
    }
    private static byte[] patch(byte[] input,int[] methods,int[] calls){
        int[] existing={0};new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){if(!METHOD.equals(n)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(op==Opcodes.INVOKESTATIC&&HELPER.equals(owner)&&"ensureLoaded".equals(name)&&"(Ljava/lang/String;)V".equals(desc))existing[0]++;}};}},0);
        if(existing[0]==1){methods[0]=1;calls[0]=1;return input;}if(existing[0]!=0)throw new IllegalStateException("unexpected existing ensureLoaded calls="+existing[0]);
        ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){MethodVisitor mv=super.visitMethod(a,n,d,s,e);if(!METHOD.equals(n)||!DESC.equals(d))return mv;methods[0]++;return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"ensureLoaded","(Ljava/lang/String;)V",false);calls[0]++;}};}},0);return w.toByteArray();
    }
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
