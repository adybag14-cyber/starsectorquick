import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyCampaignXStreamProviderDiagPatch {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyCampaignXStreamProviderDiagPatch jar");
        final int[] defaultCtor = {0}, providerCtor = {0}, helperCtor = {0}, providerLog = {0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            final JarEntry e = jar.getJarEntry("com/fs/starfarer/campaign/save/CampaignGameManager$6.class");
            if (e == null) throw new AssertionError("missing CampaignGameManager$6");
            try (InputStream in = jar.getInputStream(e)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] x) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int op, String owner, String mn, String md, boolean itf) {
                                if (op == Opcodes.INVOKESPECIAL && "com/thoughtworks/xstream/XStream".equals(owner) && "<init>".equals(mn)) {
                                    if ("(Lcom/thoughtworks/xstream/io/HierarchicalStreamDriver;)V".equals(md)) defaultCtor[0]++;
                                    if ("(Lcom/thoughtworks/xstream/converters/reflection/ReflectionProvider;Lcom/thoughtworks/xstream/io/HierarchicalStreamDriver;)V".equals(md)) providerCtor[0]++;
                                }
                                if (op == Opcodes.INVOKESPECIAL && "com/fs/starfarer/BrowserLoggingSunUnsafeReflectionProvider".equals(owner) && "<init>".equals(mn)) helperCtor[0]++;
                                if (op == Opcodes.INVOKESTATIC && "com/fs/starfarer/BrowserXStreamLoadDiag".equals(owner) && "logProvider".equals(mn)) providerLog[0]++;
                            }
                        };
                    }
                }, 0);
            }
        }
        if (defaultCtor[0] != 0 || providerCtor[0] != 1 || helperCtor[0] != 1 || providerLog[0] != 1) {
            throw new AssertionError("provider diag verification default=" + defaultCtor[0] + " provider=" + providerCtor[0]
                    + " helper=" + helperCtor[0] + " providerLog=" + providerLog[0]);
        }
        System.out.println("VerifyCampaignXStreamProviderDiagPatch: OK providerCtor=1 providerLog=1");
    }
}
