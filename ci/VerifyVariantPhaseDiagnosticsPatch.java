import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyVariantPhaseDiagnosticsPatch {
    private static final String ENTRY="com/fs/starfarer/loading/SpecStore.class", METHOD="oO0000", DESC="()V", STORE="com/fs/starfarer/loading/new";
    public static void main(String[] args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("usage: VerifyVariantPhaseDiagnosticsPatch jar");List<String> labels=new ArrayList<>();int[] relationship={0},finalize={0},ctor={0},json={0},register={0};
        try(JarFile j=new JarFile(Path.of(args[0]).toFile())){JarEntry e=j.getJarEntry(ENTRY);if(e==null)throw new AssertionError("missing SpecStore");try(InputStream in=j.getInputStream(e)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;return new MethodVisitor(Opcodes.ASM9){@Override public void visitLdcInsn(Object v){if(v instanceof String&&((String)v).startsWith("variant-phase:"))labels.add((String)v);}@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&STORE.equals(o)&&"o00000".equals(n)&&"()Ljava/util/List;".equals(d))relationship[0]++;if(op==Opcodes.INVOKESTATIC&&STORE.equals(o)&&"()V".equals(d))finalize[0]++;if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/loading/specs/HullVariantSpec".equals(o)&&"<init>".equals(n))ctor[0]++;if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/LoadingUtils".equals(o)&&d.endsWith("Lorg/json/JSONObject;"))json[0]++;if(op==Opcodes.INVOKESTATIC&&STORE.equals(o)&&"o00000".equals(n)&&d.startsWith("(Lcom/fs/starfarer/loading/specs/HullVariantSpec;"))register[0]++;}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}}
        List<String> expected=java.util.Arrays.asList("variant-phase:entry","variant-phase:relationships","variant-phase:finalize","variant-phase:exit");if(!labels.equals(expected)||relationship[0]!=1||finalize[0]!=1||ctor[0]<1||json[0]<1||register[0]<1)throw new AssertionError("variant phase verifier labels="+labels+" relationship="+relationship[0]+" finalize="+finalize[0]+" ctor="+ctor[0]+" json="+json[0]+" register="+register[0]);System.out.println("VerifyVariantPhaseDiagnosticsPatch: OK four markers, variant parse/register semantics retained");}
}
