import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structural verifier for the exact BrowserFastPathTracker allocation swap. */
public final class VerifyXStreamFastPathTrackerPatch {
    private static final String TARGET = "com/thoughtworks/xstream/core/AbstractReferenceMarshaller.class";
    private static final String CTOR_DESC = "(Lcom/thoughtworks/xstream/io/HierarchicalStreamWriter;Lcom/thoughtworks/xstream/converters/ConverterLookup;Lcom/thoughtworks/xstream/mapper/Mapper;)V";
    private static final String STOCK = "com/thoughtworks/xstream/io/path/PathTracker";
    private static final String FAST = "com/thoughtworks/xstream/io/path/BrowserFastPathTracker";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyXStreamFastPathTrackerPatch jar");
        int[] methods={0}, fastNew={0}, fastInit={0}, stockNew={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry(TARGET);
            if (entry == null) throw new AssertionError("missing " + TARGET);
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                        if (!"<init>".equals(name) || !CTOR_DESC.equals(desc)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitTypeInsn(int opcode,String type) {
                                if (opcode==Opcodes.NEW && FAST.equals(type)) fastNew[0]++;
                                if (opcode==Opcodes.NEW && STOCK.equals(type)) stockNew[0]++;
                            }
                            @Override public void visitMethodInsn(int opcode,String owner,String n,String d,boolean itf) {
                                if (opcode==Opcodes.INVOKESPECIAL && FAST.equals(owner)
                                        && "<init>".equals(n) && "()V".equals(d)) fastInit[0]++;
                            }
                        };
                    }
                },0);
            }
        }
        if (methods[0]!=1 || fastNew[0]!=1 || fastInit[0]!=1 || stockNew[0]!=0) {
            throw new AssertionError("fast PathTracker structure mismatch methods="+methods[0]
                    +" fastNew="+fastNew[0]+" fastInit="+fastInit[0]+" stockNew="+stockNew[0]);
        }
        System.out.println("VerifyXStreamFastPathTrackerPatch: OK exact tracker allocation swap");
    }
}
