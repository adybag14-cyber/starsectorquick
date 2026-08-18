import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/**
 * Bypasses SpecStore's stock variant directory discovery when BrowserSpecCache
 * already has the complete stock manifest. The untouched stock discovery body
 * remains in bytecode and is used whenever the helper returns null.
 */
public final class PatchSpecStoreDirectVariantManifest {
    private static final String ENTRY="com/fs/starfarer/loading/SpecStore.class";
    private static final String SPEC="com/fs/starfarer/loading/SpecStore";
    private static final String METHOD="oO0000", DESC="()V";
    private static final String CACHE="com/fs/starfarer/loading/BrowserSpecCache";
    private static final String DIRECT_DESC="()Ljava/util/List;";
    private static final String LOGGER_DESC="Lorg/apache/log4j/Logger;";
    private PatchSpecStoreDirectVariantManifest() {}

    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchSpecStoreDirectVariantManifest input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]); C c=new C();
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();while(es.hasMoreElements()){
                JarEntry e=es.nextElement();JarEntry copy=new JarEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);
                byte[]b;try(InputStream in=jar.getInputStream(e)){b=read(in);}if(ENTRY.equals(e.getName())){c.classes++;b=patch(b,c);}out.write(b);out.closeEntry();
            }
        }
        if(c.classes!=1||c.methods!=1||c.directCalls!=1||c.targetLabels!=1)throw new IllegalStateException("direct manifest patch mismatch classes="+c.classes+" methods="+c.methods+" directCalls="+c.directCalls+" targetLabels="+c.targetLabels);
        System.out.println("Patched SpecStore direct variant manifest fast path calls=1 target=1");
    }

    private static byte[] patch(byte[] input,C c){
        int existing=countDirect(input);
        if(existing==1){c.methods=1;c.directCalls=1;c.targetLabels=1;System.out.println("SpecStore direct variant manifest fast path already present; leaving bytecode unchanged.");return input;}
        if(existing!=0)throw new IllegalStateException("duplicate direct variant manifest calls="+existing);
        ClassReader r=new ClassReader(input);
        ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]ex){
                MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex);if(!METHOD.equals(name)||!DESC.equals(desc))return mv;c.methods++;
                return new MethodVisitor(Opcodes.ASM9,mv){
                    final Label stock=new Label();final Label directTarget=new Label();boolean placed;
                    @Override public void visitCode(){
                        super.visitCode();
                        super.visitMethodInsn(Opcodes.INVOKESTATIC,CACHE,"directVariantPathsOrNull",DIRECT_DESC,false);c.directCalls++;
                        super.visitInsn(Opcodes.DUP);
                        super.visitJumpInsn(Opcodes.IFNULL,stock);
                        super.visitVarInsn(Opcodes.ASTORE,0);
                        super.visitJumpInsn(Opcodes.GOTO,directTarget);
                        super.visitLabel(stock);
                        super.visitInsn(Opcodes.POP);
                    }
                    @Override public void visitFieldInsn(int op,String owner,String name,String d){
                        if(!placed&&op==Opcodes.GETSTATIC&&SPEC.equals(owner)&&LOGGER_DESC.equals(d)){
                            super.visitLabel(directTarget);placed=true;c.targetLabels++;
                        }
                        super.visitFieldInsn(op,owner,name,d);
                    }
                    @Override public void visitEnd(){if(!placed)throw new IllegalStateException("variant discovery target logger field not found");super.visitEnd();}
                };
            }
        },ClassReader.SKIP_FRAMES);
        byte[] out=w.toByteArray();if(countDirect(out)!=1)throw new IllegalStateException("post-patch direct call count="+countDirect(out));return out;
    }
    private static int countDirect(byte[]b){int[]n={0};new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String m,String d,String s,String[]e){if(!METHOD.equals(m)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String m,String d,boolean i){if(op==Opcodes.INVOKESTATIC&&CACHE.equals(o)&&"directVariantPathsOrNull".equals(m)&&DIRECT_DESC.equals(d))n[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return n[0];}
    private static byte[]read(InputStream i)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=i.read(b))>=0)o.write(b,0,n);return o.toByteArray();}
    static final class C{int classes,methods,directCalls,targetLabels;}
}
