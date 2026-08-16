import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Diagnostic-only timers around each precompiled SectorGen.runSectorStep invocation body. */
public final class PatchSectorGenTiming {
    private static final String TARGET = "data/scripts/world/SectorGen.class";
    private static final String METHOD = "runSectorStep";
    private static final String DESC = "(Ljava/lang/String;ZLdata/scripts/world/SectorGen$SectorStep;)V";
    private static final String STEP_OWNER = "data/scripts/world/SectorGen$SectorStep";
    private static final String HELPER = "com/fs/starfarer/BrowserProcgenTiming";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchSectorGenTiming input.jar output.jar");
        Path input=Path.of(args[0]), output=Path.of(args[1]);
        int[] classes={0}, methods={0}, begins={0}, ends={0};
        try(JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> es=jar.entries();
            while(es.hasMoreElements()) {
                JarEntry e=es.nextElement(); JarEntry c=new JarEntry(e.getName()); c.setTime(e.getTime()); out.putNextEntry(c);
                byte[] b; try(InputStream in=jar.getInputStream(e)){ b=in.readAllBytes(); }
                if(TARGET.equals(e.getName())) { classes[0]++; b=patch(b,methods,begins,ends); }
                out.write(b); out.closeEntry();
            }
        }
        if(classes[0]!=1 || methods[0]!=1 || begins[0]!=1 || ends[0]!=1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("SectorGen timing mismatch classes="+classes[0]+" methods="+methods[0]+" begins="+begins[0]+" ends="+ends[0]);
        }
        System.out.println("PatchSectorGenTiming: OK classes=1 methods=1 begins=1 ends=1");
    }

    private static byte[] patch(byte[] input,int[] methods,int[] begins,int[] ends) {
        int existing=countHelperCalls(input);
        if(existing!=0 && existing!=2) throw new IllegalStateException("partial SectorGen timing hooks="+existing);
        ClassReader r=new ClassReader(input); ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex);
                if(!METHOD.equals(name)||!DESC.equals(desc)) return mv;
                methods[0]++;
                if(existing==2){ begins[0]++; ends[0]++; return mv; }
                return new MethodVisitor(Opcodes.ASM9,mv) {
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf) {
                        if(opcode==Opcodes.INVOKEINTERFACE && STEP_OWNER.equals(owner) && "run".equals(name) && "()V".equals(desc)) {
                            // Stack currently contains the SectorStep receiver. Loading label does not disturb it.
                            super.visitVarInsn(Opcodes.ALOAD,1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"stepBegin","(Ljava/lang/String;)V",false);
                            begins[0]++;
                            super.visitMethodInsn(opcode,owner,name,desc,itf);
                            super.visitVarInsn(Opcodes.ALOAD,1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"stepEnd","(Ljava/lang/String;)V",false);
                            ends[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode,owner,name,desc,itf);
                    }
                };
            }
        },0);
        return w.toByteArray();
    }

    private static int countHelperCalls(byte[] b) {
        int[] c={0};
        new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e) {
                if(!METHOD.equals(n)||!DESC.equals(d)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf) {
                        if(op==Opcodes.INVOKESTATIC && HELPER.equals(owner)
                                && ("stepBegin".equals(name)||"stepEnd".equals(name))) c[0]++;
                    }
                };
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        return c[0];
    }
}
