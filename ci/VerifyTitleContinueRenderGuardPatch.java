import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyTitleContinueRenderGuardPatch {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyTitleContinueRenderGuardPatch jar");
        final int[] methods = {0}, stockRenders = {0}, guardedRenders = {0}, loadCalls = {0}, stateCalls = {0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            final JarEntry entry = jar.getJarEntry("com/fs/starfarer/title/TitleScreenState.class");
            if (entry == null) throw new AssertionError("missing TitleScreenState");
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        if (!"menuItemSelected".equals(name)
                                || !"(Lcom/fs/starfarer/title/ooOO$o$o;)V".equals(desc)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                                if (opcode == Opcodes.INVOKEVIRTUAL
                                        && "com/fs/starfarer/title/TitleScreenState".equals(owner)
                                        && "render".equals(n) && "(F)V".equals(d)) stockRenders[0]++;
                                if (opcode == Opcodes.INVOKESTATIC
                                        && "com/fs/starfarer/BrowserTitleContinueCompat".equals(owner)
                                        && "renderBeforeContinue".equals(n)) guardedRenders[0]++;
                                if (opcode == Opcodes.INVOKESTATIC
                                        && "com/fs/starfarer/campaign/save/CampaignGameManager".equals(owner)
                                        && d.endsWith(";Z)Ljava/lang/String;")) loadCalls[0]++;
                                if (opcode == Opcodes.INVOKEVIRTUAL
                                        && "com/fs/starfarer/title/TitleScreenState".equals(owner)
                                        && "goToState".equals(n)) stateCalls[0]++;
                            }
                        };
                    }
                }, 0);
            }
        }
        if (methods[0] != 1 || stockRenders[0] != 0 || guardedRenders[0] != 2
                || loadCalls[0] != 1 || stateCalls[0] < 1) {
            throw new AssertionError("Continue guard verification failed methods=" + methods[0]
                    + " stockRender=" + stockRenders[0] + " guarded=" + guardedRenders[0]
                    + " loadCalls=" + loadCalls[0] + " goToState=" + stateCalls[0]);
        }
        System.out.println("VerifyTitleContinueRenderGuardPatch: OK guarded=2 stockLoad=1");
    }
}
