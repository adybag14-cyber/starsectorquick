import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structural verifier for CampaignGameManager's property-gated fast ID marshaller hook. */
public final class VerifyCampaignXStreamFastIdMarshallerPatch {
    private static final String TARGET = "com/fs/starfarer/campaign/save/CampaignGameManager.class";
    private static final String FACTORY_DESC = "(Ljava/lang/String;)Lcom/thoughtworks/xstream/XStream;";
    private static final String XSTREAM = "com/thoughtworks/xstream/XStream";
    private static final String HELPER = "com/fs/starfarer/BrowserXStreamMarshallingCompat";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyCampaignXStreamFastIdMarshallerPatch jar");
        int[] methods={0}, mode={0}, install={0}, callIndex={0}, modeIndex={0}, installIndex={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry(TARGET);
            if (entry == null) throw new AssertionError("missing " + TARGET);
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                        if ((access & Opcodes.ACC_STATIC)==0 || !FACTORY_DESC.equals(desc)) return null;
                        return new MethodVisitor(Opcodes.ASM9) {
                            boolean relevant;
                            @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf) {
                                int i=++callIndex[0];
                                if (op==Opcodes.INVOKEVIRTUAL && XSTREAM.equals(owner)
                                        && "setMode".equals(n) && "(I)V".equals(d)) {
                                    if (!relevant) { methods[0]++; relevant=true; }
                                    mode[0]++; modeIndex[0]=i;
                                }
                                if (op==Opcodes.INVOKESTATIC && HELPER.equals(owner)
                                        && "install".equals(n)
                                        && "(Lcom/thoughtworks/xstream/XStream;)V".equals(d)) {
                                    install[0]++; installIndex[0]=i;
                                }
                            }
                        };
                    }
                },0);
            }
        }
        if (methods[0]!=1 || mode[0]!=1 || install[0]!=1 || installIndex[0] != modeIndex[0]+1) {
            throw new AssertionError("fast-ID hook mismatch methods="+methods[0]+" mode="+mode[0]
                    +" install="+install[0]+" modeIndex="+modeIndex[0]+" installIndex="+installIndex[0]);
        }
        System.out.println("VerifyCampaignXStreamFastIdMarshallerPatch: OK install immediately follows ID setMode");
    }
}
