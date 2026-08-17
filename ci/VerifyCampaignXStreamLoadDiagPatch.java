import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyCampaignXStreamLoadDiagPatch {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyCampaignXStreamLoadDiagPatch jar");
        final int[] wraps = {0}, providers = {0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            verify(jar, "com/fs/starfarer/campaign/save/CampaignGameManager$5.class", wraps, providers);
            verify(jar, "com/fs/starfarer/campaign/save/CampaignGameManager$6.class", wraps, providers);
        }
        if (wraps[0] != 1 || providers[0] != 1) {
            throw new AssertionError("XStream load diag verification failed wraps=" + wraps[0] + " providers=" + providers[0]);
        }
        System.out.println("VerifyCampaignXStreamLoadDiagPatch: OK wraps=1 providers=1");
    }
    private static void verify(JarFile jar, String name, int[] wraps, int[] providers) throws Exception {
        final JarEntry entry = jar.getJarEntry(name);
        if (entry == null) throw new AssertionError("missing " + name);
        try (InputStream in = jar.getInputStream(entry)) {
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String n, String d, String s, String[] e) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String mn, String md, boolean itf) {
                            if (opcode == Opcodes.INVOKESTATIC && "com/fs/starfarer/BrowserXStreamLoadDiag".equals(owner)) {
                                if ("wrap".equals(mn)) wraps[0]++;
                                if ("logProvider".equals(mn)) providers[0]++;
                            }
                        }
                    };
                }
            }, 0);
        }
    }
}
