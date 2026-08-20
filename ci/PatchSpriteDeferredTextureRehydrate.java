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

/** Preserve deferred Sprite texture identity and rehydrate exactly on first getTexture(). */
public final class PatchSpriteDeferredTextureRehydrate {
    private static final String ENTRY = "com/fs/graphics/Sprite.class";
    private static final String OWNER = "com/fs/graphics/Sprite";
    private static final String TEXTURE_OWNER = "com/fs/graphics/oOoO";
    private static final String TEXTURE_DESC = "Lcom/fs/graphics/Object;";
    private static final String CTOR_DESC = "(Ljava/lang/String;)V";
    private static final String GET_DESC = "()Lcom/fs/graphics/Object;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchSpriteDeferredTextureRehydrate input.jar output.jar");
        Path input=Path.of(args[0]), output=Path.of(args[1]);
        byte[] sprite;
        try(JarFile jar=new JarFile(input.toFile())){
            JarEntry e=jar.getJarEntry(ENTRY); if(e==null)throw new IllegalStateException("missing "+ENTRY);
            try(InputStream in=jar.getInputStream(e)){sprite=readAll(in);}
        }
        int[] existing={0};
        new ClassReader(sprite).accept(new ClassVisitor(Opcodes.ASM9){
            @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){
                if(!"getTexture".equals(n)||!GET_DESC.equals(d))return null;
                return new MethodVisitor(Opcodes.ASM9){
                    @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
                        if(op==Opcodes.INVOKESTATIC&&TEXTURE_OWNER.equals(owner)&&"new".equals(name)&&"(Ljava/lang/String;)Lcom/fs/graphics/Object;".equals(desc))existing[0]++;
                    }
                };
            }
        },0);
        if(existing[0]==1){Files.copy(input,output);System.out.println("Sprite deferred texture rehydrate already present");return;}
        if(existing[0]!=0)throw new IllegalStateException("unexpected existing lazy Sprite lookups="+existing[0]);

        int[] classes={0},ctors={0},gets={0},retains={0},rehydrates={0};
        try(JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();
            while(es.hasMoreElements()){
                JarEntry e=es.nextElement(); JarEntry c=new JarEntry(e.getName()); c.setTime(e.getTime()); out.putNextEntry(c);
                byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}
                if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,ctors,gets,retains,rehydrates);}
                out.write(bytes); out.closeEntry();
            }
        }
        if(classes[0]!=1||ctors[0]!=1||gets[0]!=1||retains[0]!=1||rehydrates[0]!=1){
            Files.deleteIfExists(output); throw new IllegalStateException("Sprite rehydrate mismatch classes="+classes[0]+" ctors="+ctors[0]+" gets="+gets[0]+" retains="+retains[0]+" rehydrates="+rehydrates[0]);
        }
        System.out.println("Patched Sprite deferred texture rehydrate retain=1 getTexture=1");
    }

    private static byte[] patch(byte[] input,int[] ctors,int[] gets,int[] retains,int[] rehydrates){
        ClassReader r=new ClassReader(input); ClassWriter w=new SafeClassWriter(r,ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
                MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex);
                if("<init>".equals(name)&&CTOR_DESC.equals(desc)){
                    ctors[0]++;
                    return new MethodVisitor(Opcodes.ASM9,mv){
                        @Override public void visitInsn(int opcode){
                            if(opcode==Opcodes.RETURN){
                                Label done=new Label();
                                super.visitVarInsn(Opcodes.ALOAD,0);
                                super.visitFieldInsn(Opcodes.GETFIELD,OWNER,"texture",TEXTURE_DESC);
                                super.visitJumpInsn(Opcodes.IFNONNULL,done);
                                super.visitVarInsn(Opcodes.ALOAD,0);
                                super.visitVarInsn(Opcodes.ALOAD,1);
                                super.visitFieldInsn(Opcodes.PUTFIELD,OWNER,"textureId","Ljava/lang/String;");
                                super.visitLabel(done); retains[0]++;
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                if("getTexture".equals(name)&&GET_DESC.equals(desc)){
                    gets[0]++;
                    return new MethodVisitor(Opcodes.ASM9,mv){
                        @Override public void visitCode(){
                            super.visitCode(); Label done=new Label();
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitFieldInsn(Opcodes.GETFIELD,OWNER,"texture",TEXTURE_DESC);
                            super.visitJumpInsn(Opcodes.IFNONNULL,done);
                            super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
                            super.visitLdcInsn("SpriteRehydrateDiag: entry key=");
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","print","(Ljava/lang/String;)V",false);
                            super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitFieldInsn(Opcodes.GETFIELD,OWNER,"textureId","Ljava/lang/String;");
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false);
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitFieldInsn(Opcodes.GETFIELD,OWNER,"textureId","Ljava/lang/String;");
                            super.visitJumpInsn(Opcodes.IFNULL,done);
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitFieldInsn(Opcodes.GETFIELD,OWNER,"textureId","Ljava/lang/String;");
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,TEXTURE_OWNER,"new","(Ljava/lang/String;)Lcom/fs/graphics/Object;",false);
                            super.visitVarInsn(Opcodes.ASTORE,1);
                            Label retryHit=new Label(), retryLogged=new Label();
                            super.visitVarInsn(Opcodes.ALOAD,1);
                            super.visitJumpInsn(Opcodes.IFNONNULL,retryHit);
                            super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
                            super.visitLdcInsn("SpriteRehydrateDiag: retry-null");
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false);
                            super.visitJumpInsn(Opcodes.GOTO,retryLogged);
                            super.visitLabel(retryHit);
                            super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
                            super.visitLdcInsn("SpriteRehydrateDiag: retry-hit");
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false);
                            super.visitLabel(retryLogged);
                            super.visitVarInsn(Opcodes.ALOAD,1);
                            super.visitJumpInsn(Opcodes.IFNULL,done);
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitVarInsn(Opcodes.ALOAD,1);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,OWNER,"setTexture","(Lcom/fs/graphics/Object;)V",false);
                            super.visitLabel(done); rehydrates[0]++;
                        }
                    };
                }
                return mv;
            }
        },ClassReader.EXPAND_FRAMES); return w.toByteArray();
    }
    private static final class SafeClassWriter extends ClassWriter {SafeClassWriter(ClassReader r,int flags){super(r,flags);}@Override protected String getCommonSuperClass(String a,String b){return "java/lang/Object";}}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=in.read(b))>=0)o.write(b,0,n);return o.toByteArray();}
}
