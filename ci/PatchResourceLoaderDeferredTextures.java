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

/** Defers normal/optional illustration and portrait loads during browser quick-start. */
public final class PatchResourceLoaderDeferredTextures {
    private static final String ENTRY = "com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String METHOD = "init";
    private static final String DESC = "(Ljava/util/Map;)V";
    private static final String TEXTURES = "com/fs/graphics/oOoO";
    private static final String LOAD = "o00000";
    private static final String LOAD_DESC = "(Ljava/lang/String;Ljava/lang/String;)V";
    private static final String HELPER = "com/fs/starfarer/BrowserDeferredTextureQueue";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchResourceLoaderDeferredTextures input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes={0}, methods={0}, directCalls={0}, helperCalls={0}, replaced={0};
        try (JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries=jar.entries();
            while(entries.hasMoreElements()) {
                JarEntry e=entries.nextElement(); JarEntry copy=new JarEntry(e.getName()); copy.setTime(e.getTime()); out.putNextEntry(copy);
                byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}
                if(ENTRY.equals(e.getName())) { classes[0]++; bytes=patch(bytes,methods,directCalls,helperCalls,replaced); }
                out.write(bytes); out.closeEntry();
            }
        }
        boolean already = helperCalls[0]==2 && directCalls[0]==5 && replaced[0]==0;
        boolean fresh = helperCalls[0]==2 && directCalls[0]==7 && replaced[0]==2;
        if(classes[0]!=1 || methods[0]!=1 || (!already && !fresh)) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("deferred ResourceLoader patch mismatch classes="+classes[0]+" methods="+methods[0]
                    +" direct="+directCalls[0]+" helper="+helperCalls[0]+" replaced="+replaced[0]);
        }
        System.out.println("Patched ResourceLoader deferred textures direct="+directCalls[0]+" helper="+helperCalls[0]+" replaced="+replaced[0]);
    }

    private static byte[] patch(byte[] input,int[] methods,int[] directCalls,int[] helperCalls,int[] replaced) {
        // First scan the target method so repeated preparation remains idempotent.
        int[] preDirect={0}, preHelper={0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                if(!METHOD.equals(name)||!DESC.equals(descriptor)) return null;
                return new MethodVisitor(Opcodes.ASM9){
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf){
                        if(opcode==Opcodes.INVOKESTATIC && LOAD_DESC.equals(desc)) {
                            if(TEXTURES.equals(owner)&&LOAD.equals(name)) preDirect[0]++;
                            if(HELPER.equals(owner)&&"loadOrDefer".equals(name)) preHelper[0]++;
                        }
                    }
                };}
        },0);
        final boolean already = preHelper[0]==2 && preDirect[0]==5;
        if(already) {
            methods[0]=1; directCalls[0]=preDirect[0]; helperCalls[0]=preHelper[0]; return input;
        }
        if(preHelper[0]!=0 || preDirect[0]!=7) throw new IllegalStateException("unexpected pre-patch image calls direct="+preDirect[0]+" helper="+preHelper[0]);

        ClassReader reader=new ClassReader(input); ClassWriter writer=new ClassWriter(reader,0);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                MethodVisitor mv=super.visitMethod(access,name,descriptor,signature,exceptions);
                if(!METHOD.equals(name)||!DESC.equals(descriptor)) return mv;
                methods[0]++; return new MethodVisitor(Opcodes.ASM9,mv){
                    int imageCall;
                    @Override public void visitMethodInsn(int opcode,String owner,String methodName,String methodDesc,boolean itf){
                        if(opcode==Opcodes.INVOKESTATIC && TEXTURES.equals(owner) && LOAD.equals(methodName) && LOAD_DESC.equals(methodDesc)) {
                            imageCall++; directCalls[0]++;
                            // Bytecode order: #1-4 loading/title bootstrap images; #5 TEXTURE,
                            // #6 TEXTURE_ALPHA_ADDER, #7 TEXTURE_OPTIONAL. Keep bootstrap and
                            // alpha-adder eager; defer only normal/optional queued textures.
                            if(imageCall==5 || imageCall==7) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"loadOrDefer",LOAD_DESC,false);
                                helperCalls[0]++; replaced[0]++; return;
                            }
                        }
                        super.visitMethodInsn(opcode,owner,methodName,methodDesc,itf);
                    }
                };}
        },0);
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
