import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structural verifier for the diagnostic TreeMarshaller conversion timer. */
public final class VerifyXStreamMarshalTimingPatch {
    private static final String TARGET = "com/thoughtworks/xstream/core/TreeMarshaller.class";
    private static final String HELPER = "com/fs/starfarer/BrowserXStreamMarshalDiag";
    private static final String CONVERT_DESC = "(Ljava/lang/Object;Lcom/thoughtworks/xstream/converters/Converter;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyXStreamMarshalTimingPatch candidate.jar");
        final int[] methods = {0};
        final int[] enters = {0};
        final int[] exits = {0};
        final int[] marshalCalls = {0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            final JarEntry entry = jar.getJarEntry(TARGET);
            if (entry == null) throw new IllegalStateException("missing " + TARGET);
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        if (!"convert".equals(name) || !CONVERT_DESC.equals(desc)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                                if (opcode == Opcodes.INVOKESTATIC && HELPER.equals(owner) && "enter".equals(n)) enters[0]++;
                                if (opcode == Opcodes.INVOKESTATIC && HELPER.equals(owner) && "exit".equals(n)) exits[0]++;
                                if (opcode == Opcodes.INVOKEINTERFACE
                                        && "com/thoughtworks/xstream/converters/Converter".equals(owner)
                                        && "marshal".equals(n)) marshalCalls[0]++;
                            }
                        };
                    }
                }, 0);
            }
        }
        if (methods[0] != 1 || enters[0] != 1 || exits[0] != 2 || marshalCalls[0] != 1) {
            throw new IllegalStateException("XStream marshal timing verification failed methods=" + methods[0]
                    + " enters=" + enters[0] + " exits=" + exits[0] + " marshalCalls=" + marshalCalls[0]);
        }
        System.out.println("VerifyXStreamMarshalTimingPatch: OK methods=1 enters=1 exits=2 marshalCalls=1");
    }
}
