import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Diagnostic-only aggregate timing for JSON parse, HullVariantSpec construction, and registration. */
public final class PatchVariantLoopDiagnostics {
    private static final String ENTRY="com/fs/starfarer/loading/SpecStore.class";
    private static final String METHOD="oO0000", DESC="()V";
    private static final String LOADING="com/fs/starfarer/loading/LoadingUtils";
    private static final String JSON_DESC="(Ljava/lang/String;)Lorg/json/JSONObject;";
    private static final String VARIANT="com/fs/starfarer/loading/specs/HullVariantSpec";
    private static final String CTOR_DESC="(Lorg/json/JSONObject;)V";
    private static final String STORE="com/fs/starfarer/loading/new";
    private static final String REGISTER_DESC="(Lcom/fs/starfarer/loading/specs/HullVariantSpec;Z)V";
    private static final String REL_DESC="()Ljava/util/List;";
    private static final String HELPER="com/fs/starfarer/loading/BrowserVariantLoopDiag";

    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchVariantLoopDiagnostics input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]); Counts c=new Counts();
        try(JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries(); while(es.hasMoreElements()){
                JarEntry e=es.nextElement(); JarEntry copy=new JarEntry(e.getName()); copy.setTime(e.getTime()); out.putNextEntry(copy);
                byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);} if(ENTRY.equals(e.getName())){c.classes++;bytes=patch(bytes,c);} out.write(bytes);out.closeEntry();
            }
        }
        require(c); System.out.println("Patched variant loop diagnostics parse=1 ctor=1 register=1 relationship=1");
    }

    private static byte[] patch(byte[] input,Counts c){
        int existing=countHelper(input);
        if(existing==8){c.methods=1;c.begin=1;c.parse=1;c.ctorNew=1;c.ctor=1;c.register=1;c.relationship=1;c.helperCalls=8;return input;}
        if(existing!=0)throw new IllegalStateException("partial variant loop diagnostics helperCalls="+existing);
        ClassReader r=new ClassReader(input); ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
                MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex); if(!METHOD.equals(name)||!DESC.equals(desc))return mv; c.methods++;
                return new MethodVisitor(Opcodes.ASM9,mv){
                    void helper(String name){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,name,"()V",false);c.helperCalls++;}
                    @Override public void visitCode(){super.visitCode();helper("begin");c.begin++;}
                    @Override public void visitTypeInsn(int op,String type){
                        if(op==Opcodes.NEW&&VARIANT.equals(type)){helper("ctorStart");c.ctorNew++;}
                        super.visitTypeInsn(op,type);
                    }
                    @Override public void visitMethodInsn(int op,String owner,String name,String d,boolean itf){
                        if(op==Opcodes.INVOKESTATIC&&LOADING.equals(owner)&&JSON_DESC.equals(d)){
                            helper("parseStart");super.visitMethodInsn(op,owner,name,d,itf);helper("parseEnd");c.parse++;return;
                        }
                        if(op==Opcodes.INVOKESPECIAL&&VARIANT.equals(owner)&&"<init>".equals(name)&&CTOR_DESC.equals(d)){
                            super.visitMethodInsn(op,owner,name,d,itf);helper("ctorEnd");c.ctor++;return;
                        }
                        if(op==Opcodes.INVOKESTATIC&&STORE.equals(owner)&&REGISTER_DESC.equals(d)){
                            helper("registerStart");super.visitMethodInsn(op,owner,name,d,itf);helper("registerEnd");c.register++;return;
                        }
                        if(op==Opcodes.INVOKESTATIC&&STORE.equals(owner)&&REL_DESC.equals(d)){
                            helper("finishLoop");c.relationship++;
                        }
                        super.visitMethodInsn(op,owner,name,d,itf);
                    }
                };
            }
        },0);
        return w.toByteArray();
    }
    private static int countHelper(byte[] input){int[] n={0};new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String name,String d,String s,String[]e){if(!METHOD.equals(name)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&HELPER.equals(owner))n[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return n[0];}
    private static void require(Counts c){if(c.classes!=1||c.methods!=1||c.begin!=1||c.parse!=1||c.ctorNew!=1||c.ctor!=1||c.register!=1||c.relationship!=1||c.helperCalls!=8)throw new IllegalStateException("variant loop diag mismatch classes="+c.classes+" methods="+c.methods+" begin="+c.begin+" parse="+c.parse+" ctorNew="+c.ctorNew+" ctor="+c.ctor+" register="+c.register+" relationship="+c.relationship+" helperCalls="+c.helperCalls);}
    static final class Counts{int classes,methods,begin,parse,ctorNew,ctor,register,relationship,helperCalls;}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=in.read(b))>=0)o.write(b,0,n);return o.toByteArray();}
}
