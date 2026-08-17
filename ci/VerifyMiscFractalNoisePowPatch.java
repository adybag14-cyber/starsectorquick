import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyMiscFractalNoisePowPatch {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyMiscFractalNoisePowPatch patched.jar");
        int[] methods = {0};
        int[] helper = {0};
        int[] stockPow = {0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry("com/fs/starfarer/api/util/Misc.class");
            if (entry == null) throw new AssertionError("missing Misc.class");
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        if (!"fill".equals(name) || !"(Ljava/util/Random;[[FIIIIIIIF)V".equals(desc)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                if (opcode == Opcodes.INVOKESTATIC && "(DD)D".equals(desc)) {
                                    if ("com/fs/starfarer/api/util/BrowserFractalNoiseCompat".equals(owner) && "pow".equals(name)) helper[0]++;
                                    if ("java/lang/Math".equals(owner) && "pow".equals(name)) stockPow[0]++;
                                }
                            }
                        };
                    }
                }, 0);
            }
        }
        if (methods[0] != 1 || helper[0] != 1 || stockPow[0] != 0) {
            throw new AssertionError("Misc.fill pow patch mismatch methods=" + methods[0] + " helper=" + helper[0] + " stockPow=" + stockPow[0]);
        }
        System.out.println("VerifyMiscFractalNoisePowPatch: OK");
    }
}
