import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies Rules retains stock parse/registration and gains only the indexed duplicate branch. */
public final class VerifyRulesDuplicateIndexPatch {
    private static final String ENTRY = "com/fs/starfarer/campaign/rules/Rules.class";
    private static final String RULES = "com/fs/starfarer/campaign/rules/Rules";
    private static final String HELPER = "com/fs/starfarer/campaign/rules/BrowserRuleDuplicateIndex";
    private static final String DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyRulesDuplicateIndexPatch jar");
        int[] methods={0},begin={0},enabled={0},check={0},finish={0},empty={0},lists={0},csv={0},ctors={0},adds={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile()); InputStream in = jar.getInputStream(jar.getJarEntry(ENTRY))) {
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                    if (!"super".equals(name) || !DESC.equals(desc)) return null;
                    methods[0]++;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitTypeInsn(int op,String type) {
                            if (op==Opcodes.NEW && "com/fs/starfarer/campaign/rules/ooOO".equals(type)) ctors[0]++;
                        }
                        @Override public void visitMethodInsn(int op,String owner,String method,String descriptor,boolean itf) {
                            if (op==Opcodes.INVOKESTATIC && HELPER.equals(owner)) {
                                if ("begin".equals(method)) begin[0]++;
                                else if ("enabled".equals(method)) enabled[0]++;
                                else if ("checkAndRecord".equals(method)) check[0]++;
                                else if ("finish".equals(method)) finish[0]++;
                            }
                            if (op==Opcodes.INVOKESTATIC && "java/util/Collections".equals(owner) && "emptyList".equals(method)) empty[0]++;
                            if (op==Opcodes.INVOKESTATIC && RULES.equals(owner) && "super".equals(method)
                                    && "(Ljava/lang/String;)Ljava/util/List;".equals(descriptor)) lists[0]++;
                            if (op==Opcodes.INVOKESTATIC && "com/fs/starfarer/loading/LoadingUtils".equals(owner)
                                    && "(Ljava/util/List;Ljava/lang/String;ZZ)Lorg/json/JSONArray;".equals(descriptor)) csv[0]++;
                            if (op==Opcodes.INVOKEINTERFACE && "java/util/List".equals(owner) && "add".equals(method)) adds[0]++;
                        }
                    };
                }
            }, 0);
        }
        if(methods[0]!=1||begin[0]!=1||enabled[0]!=1||check[0]!=1||finish[0]!=1||empty[0]!=1
                ||lists[0]!=2||csv[0]!=1||ctors[0]<1||adds[0]<3) {
            throw new AssertionError("Rules duplicate-index shape mismatch methods="+methods[0]+" begin="+begin[0]
                    +" enabled="+enabled[0]+" check="+check[0]+" finish="+finish[0]+" empty="+empty[0]
                    +" lists="+lists[0]+" csv="+csv[0]+" ctors="+ctors[0]+" adds="+adds[0]);
        }
        System.out.println("VerifyRulesDuplicateIndexPatch: OK stock scan retained behind indexed browser guard");
    }
}
