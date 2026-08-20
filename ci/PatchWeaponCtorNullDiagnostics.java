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
import org.objectweb.asm.Type;

/** Diagnostic-only null provenance for the obfuscated ship weapon constructor; preserves the original NPE. */
public final class PatchWeaponCtorNullDiagnostics {
    private static final String ENTRY = "com/fs/starfarer/combat/entities/ship/A/J.class";
    private static final String METHOD = "<init>";
    private static final String DESC = "(Lcom/fs/starfarer/loading/specs/N;Lcom/fs/starfarer/loading/specs/Y;Lcom/fs/starfarer/combat/entities/Ship;)V";
    private static final String MARKER = "WeaponCtorNullDiag:";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchWeaponCtorNullDiagnostics input.jar output.jar");
        Path input=Path.of(args[0]), output=Path.of(args[1]); int[] classes={0},methods={0},probes={0},argProbes={0};
        try(JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();
            while(es.hasMoreElements()){
                JarEntry e=es.nextElement(); JarEntry c=new JarEntry(e.getName()); c.setTime(e.getTime()); out.putNextEntry(c);
                byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}
                if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,methods,probes,argProbes);} out.write(bytes); out.closeEntry();
            }
        }
        if(classes[0]!=1||methods[0]!=1||probes[0]<20||argProbes[0]!=3){Files.deleteIfExists(output);throw new IllegalStateException("weapon ctor diagnostic mismatch classes="+classes[0]+" methods="+methods[0]+" probes="+probes[0]+" args="+argProbes[0]);}
        System.out.println("Patched weapon ctor null diagnostics probes="+probes[0]+" args="+argProbes[0]);
    }

    private static byte[] patch(byte[] input,int[] methods,int[] probes,int[] argProbes){
        ClassReader r=new ClassReader(input); ClassWriter w=new SafeClassWriter(r,ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
                MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex); if(!METHOD.equals(name)||!DESC.equals(desc))return mv; methods[0]++;
                return new MethodVisitor(Opcodes.ASM9,mv){
                    private boolean argsProbed=false;
                    private void nullProbe(String what){Label ok=new Label();super.visitInsn(Opcodes.DUP);super.visitJumpInsn(Opcodes.IFNONNULL,ok);super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");super.visitLdcInsn(MARKER+what);super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false);super.visitLabel(ok);probes[0]++;}
                    private void localProbe(int index,String what){Label ok=new Label();super.visitVarInsn(Opcodes.ALOAD,index);super.visitInsn(Opcodes.DUP);super.visitJumpInsn(Opcodes.IFNONNULL,ok);super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");super.visitLdcInsn(MARKER+what);super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false);super.visitLabel(ok);super.visitInsn(Opcodes.POP);argProbes[0]++;}
                    private void probeArgs(){if(argsProbed)return;argsProbed=true;localProbe(1," arg weaponSpec");localProbe(2," arg slot");localProbe(3," arg ship");}
                    @Override public void visitFieldInsn(int opcode,String owner,String name,String descriptor){super.visitFieldInsn(opcode,owner,name,descriptor);Type t=Type.getType(descriptor);if(opcode==Opcodes.GETFIELD&&(t.getSort()==Type.OBJECT||t.getSort()==Type.ARRAY))nullProbe(" field "+owner+"."+name+" "+descriptor);}
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean itf){super.visitMethodInsn(opcode,owner,name,descriptor,itf);if(!argsProbed&&opcode==Opcodes.INVOKESPECIAL&&"java/lang/Object".equals(owner)&&"<init>".equals(name))probeArgs();Type t=Type.getReturnType(descriptor);if(t.getSort()==Type.OBJECT||t.getSort()==Type.ARRAY)nullProbe(" return "+owner+"."+name+descriptor);}
                };
            }
        },ClassReader.EXPAND_FRAMES); return w.toByteArray();
    }
    private static final class SafeClassWriter extends ClassWriter {SafeClassWriter(ClassReader r,int flags){super(r,flags);}@Override protected String getCommonSuperClass(String a,String b){return "java/lang/Object";}}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=in.read(b))>=0)o.write(b,0,n);return o.toByteArray();}
}
