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

/** Diagnostic-only subphase markers inside SpecStore.oO0000() variant loading. */
public final class PatchVariantPhaseDiagnostics {
    private static final String ENTRY = "com/fs/starfarer/loading/SpecStore.class";
    private static final String SPECSTORE = "com/fs/starfarer/loading/SpecStore";
    private static final String VARIANT_STORE = "com/fs/starfarer/loading/new";
    private static final String METHOD = "oO0000";
    private static final String DESC = "()V";
    private static final String HELPER = "cheerpj$specStage";
    private static final String HELPER_DESC = "(Ljava/lang/String;)V";

    private PatchVariantPhaseDiagnostics() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchVariantPhaseDiagnostics input.jar output.jar");
        Path input=Path.of(args[0]), output=Path.of(args[1]);
        int[] classes={0}, methods={0}, markers={0}, relationship={0}, finalize={0}, returns={0};
        try(JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();
            while(es.hasMoreElements()){
                JarEntry e=es.nextElement(); JarEntry c=new JarEntry(e.getName()); c.setTime(e.getTime()); out.putNextEntry(c);
                byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}
                if(ENTRY.equals(e.getName())){classes[0]++; bytes=patch(bytes,methods,markers,relationship,finalize,returns);}
                out.write(bytes); out.closeEntry();
            }
        }
        if(classes[0]!=1||methods[0]!=1||markers[0]!=4||relationship[0]!=1||finalize[0]!=1||returns[0]!=1){
            Files.deleteIfExists(output);
            throw new IllegalStateException("variant phase patch mismatch classes="+classes[0]+" methods="+methods[0]+" markers="+markers[0]+" relationship="+relationship[0]+" finalize="+finalize[0]+" returns="+returns[0]);
        }
        System.out.println("Patched variant phase diagnostics markers=4 relationship=1 finalize=1");
    }

    private static byte[] patch(byte[] input,int[] methods,int[] markers,int[] relationship,int[] finalize,int[] returns){
        int existing=countMarkers(input);
        if(existing==4){methods[0]=1;markers[0]=4;relationship[0]=1;finalize[0]=1;returns[0]=1;System.out.println("Variant phase diagnostics already present; leaving bytecode unchanged.");return input;}
        if(existing!=0)throw new IllegalStateException("partial variant phase markers="+existing);
        if(!hasHelper(input))throw new IllegalStateException("SpecStore timestamp helper missing; apply PatchSpecStoreDiagnostics first");
        ClassReader r=new ClassReader(input); ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]ex){
            MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex); if(!METHOD.equals(name)||!DESC.equals(desc))return mv; methods[0]++;
            return new MethodVisitor(Opcodes.ASM9,mv){
                void mark(String label){super.visitLdcInsn(label);super.visitMethodInsn(Opcodes.INVOKESTATIC,SPECSTORE,HELPER,HELPER_DESC,false);markers[0]++;}
                @Override public void visitCode(){super.visitCode();mark("variant-phase:entry");}
                @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){
                    if(op==Opcodes.INVOKESTATIC&&VARIANT_STORE.equals(owner)&&"o00000".equals(n)&&"()Ljava/util/List;".equals(d)){mark("variant-phase:relationships");relationship[0]++;}
                    if(op==Opcodes.INVOKESTATIC&&VARIANT_STORE.equals(owner)&&"()V".equals(d)){mark("variant-phase:finalize");finalize[0]++;}
                    super.visitMethodInsn(op,owner,n,d,itf);
                }
                @Override public void visitInsn(int op){if(op==Opcodes.RETURN){mark("variant-phase:exit");returns[0]++;}super.visitInsn(op);}
            };}},0);
        return w.toByteArray();
    }

    private static boolean hasHelper(byte[] input){final boolean[] found={false};new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){if(HELPER.equals(n)&&HELPER_DESC.equals(d))found[0]=true;return null;}},ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return found[0];}
    private static int countMarkers(byte[] input){final int[] c={0};new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){if(!METHOD.equals(n)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitLdcInsn(Object v){if(v instanceof String&&((String)v).startsWith("variant-phase:"))c[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c[0];}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=in.read(b))>=0)o.write(b,0,n);return o.toByteArray();}
}
