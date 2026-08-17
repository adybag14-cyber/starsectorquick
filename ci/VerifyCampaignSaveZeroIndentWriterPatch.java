import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyCampaignSaveZeroIndentWriterPatch {
    private static final String TARGET = "com/fs/starfarer/campaign/save/CampaignGameManager$5.class";
    private static final String OLD = "com/sun/xml/txw2/output/IndentingXMLStreamWriter";
    private static final String REPLACEMENT = "com/fs/starfarer/BrowserZeroIndentXMLStreamWriter";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyCampaignSaveZeroIndentWriterPatch jar");
        final int[] methods = {0};
        final int[] oldNew = {0};
        final int[] newNew = {0};
        final int[] oldCtor = {0};
        final int[] newCtor = {0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            final JarEntry entry = jar.getJarEntry(TARGET);
            if (entry == null) throw new AssertionError("missing " + TARGET);
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        if (!"createWriter".equals(name)
                                || !"(Ljava/io/OutputStream;)Lcom/thoughtworks/xstream/io/HierarchicalStreamWriter;".equals(desc)) {
                            return null;
                        }
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitTypeInsn(int opcode, String type) {
                                if (opcode == Opcodes.NEW && OLD.equals(type)) oldNew[0]++;
                                if (opcode == Opcodes.NEW && REPLACEMENT.equals(type)) newNew[0]++;
                            }
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                                if (opcode == Opcodes.INVOKESPECIAL && "<init>".equals(n)
                                        && "(Ljavax/xml/stream/XMLStreamWriter;)V".equals(d)) {
                                    if (OLD.equals(owner)) oldCtor[0]++;
                                    if (REPLACEMENT.equals(owner)) newCtor[0]++;
                                }
                            }
                        };
                    }
                }, 0);
            }
        }
        if (methods[0] != 1 || oldNew[0] != 0 || oldCtor[0] != 0 || newNew[0] != 1 || newCtor[0] != 1) {
            throw new AssertionError("writer patch mismatch methods=" + methods[0]
                    + " oldNew=" + oldNew[0] + " newNew=" + newNew[0]
                    + " oldCtor=" + oldCtor[0] + " newCtor=" + newCtor[0]);
        }
        System.out.println("VerifyCampaignSaveZeroIndentWriterPatch: OK");
    }
}
