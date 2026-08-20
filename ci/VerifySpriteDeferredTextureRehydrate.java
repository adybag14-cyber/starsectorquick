import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structural proof for deferred Sprite key retention and lazy getTexture rehydration. */
public final class VerifySpriteDeferredTextureRehydrate {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifySpriteDeferredTextureRehydrate fs.common_obf.jar");
        int[] ctorKeyWrites={0}, getLazyCalls={0}, getSetCalls={0};
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile()); InputStream in=jar.getInputStream(jar.getJarEntry("com/fs/graphics/Sprite.class"))){
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){
                @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){
                    if("<init>".equals(n)&&"(Ljava/lang/String;)V".equals(d))return new MethodVisitor(Opcodes.ASM9){
                        @Override public void visitFieldInsn(int op,String owner,String name,String desc){if(op==Opcodes.PUTFIELD&&"com/fs/graphics/Sprite".equals(owner)&&"textureId".equals(name))ctorKeyWrites[0]++;}
                    };
                    if("getTexture".equals(n)&&"()Lcom/fs/graphics/Object;".equals(d))return new MethodVisitor(Opcodes.ASM9){
                        @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
                            if(op==Opcodes.INVOKESTATIC&&"com/fs/graphics/oOoO".equals(owner)&&"new".equals(name)&&"(Ljava/lang/String;)Lcom/fs/graphics/Object;".equals(desc))getLazyCalls[0]++;
                            if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/graphics/Sprite".equals(owner)&&"setTexture".equals(name))getSetCalls[0]++;
                        }
                    };
                    return null;
                }
            },0);
        }
        if(ctorKeyWrites[0]!=1||getLazyCalls[0]!=1||getSetCalls[0]!=1)throw new AssertionError("Sprite rehydrate verifier failed ctorKeyWrites="+ctorKeyWrites[0]+" getLazyCalls="+getLazyCalls[0]+" getSetCalls="+getSetCalls[0]);
        System.out.println("VerifySpriteDeferredTextureRehydrate: OK ctorKeyWrites=1 lazyCalls=1 setCalls=1");
    }
}
