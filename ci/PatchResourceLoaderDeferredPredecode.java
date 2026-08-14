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

/** Skips background image predecode for textures intentionally deferred to first use. */
public final class PatchResourceLoaderDeferredPredecode {
    private static final String ENTRY="com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String METHOD="init";
    private static final String DESC="(Ljava/util/Map;)V";
    private static final String IMAGE_CACHE="com/fs/graphics/L";
    private static final String QUEUE_IMAGE="\u00d600000";
    private static final String HELPER="com/fs/starfarer/BrowserDeferredTextureQueue";
    private static final String RESOURCE="com/fs/starfarer/loading/ResourceLoaderState$Oo";
    private static final String TYPE="com/fs/starfarer/loading/ResourceLoaderState$o";

    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchResourceLoaderDeferredPredecode input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]);int[] classes={0},methods={0},stock={0},helper={0};
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry c=new JarEntry(e.getName());c.setTime(e.getTime());out.putNextEntry(c);byte[] bytes;try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,methods,stock,helper);}out.write(bytes);out.closeEntry();}
        }
        boolean fresh=classes[0]==1&&methods[0]==1&&stock[0]==1&&helper[0]==1;
        boolean already=classes[0]==1&&methods[0]==1&&stock[0]==0&&helper[0]==1;
        if(!fresh&&!already){Files.deleteIfExists(output);throw new IllegalStateException("deferred predecode patch mismatch classes="+classes[0]+" methods="+methods[0]+" stock="+stock[0]+" helper="+helper[0]);}
        System.out.println("Patched deferred texture predecode stock="+stock[0]+" helper="+helper[0]);
    }

    private static byte[] patch(byte[] input,int[] methods,int[] stock,int[] helper){
        int[] preStock={0},preHelper={0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(op==Opcodes.INVOKESTATIC&&"(Ljava/lang/String;)V".equals(desc)&&IMAGE_CACHE.equals(owner)&&QUEUE_IMAGE.equals(name))preStock[0]++;if(op==Opcodes.INVOKESTATIC&&"(Ljava/lang/String;I)V".equals(desc)&&HELPER.equals(owner)&&"queueImagePredecode".equals(name))preHelper[0]++;}};}},0);
        if(preHelper[0]==1&&preStock[0]==0){methods[0]=1;helper[0]=1;return input;}
        if(preHelper[0]!=0||preStock[0]!=1)throw new IllegalStateException("unexpected predecode call surface stock="+preStock[0]+" helper="+preHelper[0]);

        ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){MethodVisitor mv=super.visitMethod(a,n,d,s,ex);if(!METHOD.equals(n)||!DESC.equals(d))return mv;methods[0]++;return new MethodVisitor(Opcodes.ASM9,mv){boolean pathFieldReady;
            @Override public void visitVarInsn(int op,int var){super.visitVarInsn(op,var); if(!(op==Opcodes.ALOAD&&var==4)) pathFieldReady=false;}
            @Override public void visitFieldInsn(int op,String owner,String name,String desc){super.visitFieldInsn(op,owner,name,desc);pathFieldReady=op==Opcodes.GETFIELD&&RESOURCE.equals(owner)&&"o00000".equals(name)&&"Ljava/lang/String;".equals(desc);}
            @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
                if(op==Opcodes.INVOKESTATIC&&IMAGE_CACHE.equals(owner)&&QUEUE_IMAGE.equals(name)&&"(Ljava/lang/String;)V".equals(desc)){
                    if(!pathFieldReady)throw new IllegalStateException("image predecode call no longer follows ResourceLoaderState$Oo.path field");
                    stock[0]++;
                    // Existing stack: [path]. Append resource.type.ordinal() and call helper(path, ordinal).
                    super.visitVarInsn(Opcodes.ALOAD,4);
                    super.visitFieldInsn(Opcodes.GETFIELD,RESOURCE,"new","L"+TYPE+";");
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TYPE,"ordinal","()I",false);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"queueImagePredecode","(Ljava/lang/String;I)V",false);
                    helper[0]++; pathFieldReady=false; return;
                }
                pathFieldReady=false; super.visitMethodInsn(op,owner,name,desc,itf);
            }
        };}},0);return w.toByteArray();
    }
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
