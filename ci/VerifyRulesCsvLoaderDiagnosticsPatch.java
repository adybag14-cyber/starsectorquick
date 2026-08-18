import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies aggregate rules.csv loader instrumentation and original merge semantics. */
public final class VerifyRulesCsvLoaderDiagnosticsPatch {
    private static final String ENTRY = "com/fs/starfarer/loading/LoadingUtils.class";
    private static final String METHOD = "super";
    private static final String DESC = "(Ljava/util/List;Ljava/lang/String;ZZ)Lorg/json/JSONArray;";
    private static final String HELPER = "com/fs/starfarer/loading/BrowserRulesCsvLoadDiag";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyRulesCsvLoaderDiagnosticsPatch jar");
        Counts counts = new Counts();
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry(ENTRY);
            if (entry == null) throw new AssertionError("missing LoadingUtils");
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(visitor(counts), 0);
            }
        }
        if (counts.methods != 1
                || counts.helpers.getOrDefault("begin", 0) != 1
                || counts.helpers.getOrDefault("transition", 0) != 5
                || counts.helpers.getOrDefault("parsedSource", 0) != 1
                || counts.helpers.getOrDefault("finish", 0) != 1) {
            throw new AssertionError("helper calls=" + counts.helpers);
        }
        if (counts.parser != 1
                || counts.preprocess != 1
                || counts.inputRead != 1
                || counts.objectPut < 1
                || counts.listAdd < 2
                || counts.setContains < 2
                || counts.setAdd < 2
                || counts.arrayPut < 1) {
            throw new AssertionError(
                    "stock merge path missing parser=" + counts.parser
                            + " preprocess=" + counts.preprocess
                            + " read=" + counts.inputRead
                            + " objectPut=" + counts.objectPut
                            + " listAdd=" + counts.listAdd
                            + " setContains=" + counts.setContains
                            + " setAdd=" + counts.setAdd
                            + " arrayPut=" + counts.arrayPut);
        }
        System.out.println("VerifyRulesCsvLoaderDiagnosticsPatch: OK aggregate timings, generic merge/provenance path retained");
    }

    private static ClassVisitor visitor(Counts counts) {
        return new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return null;
                counts.methods++;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && HELPER.equals(owner)) {
                            counts.helpers.merge(methodName, 1, Integer::sum);
                        }
                        if ("com/fs/starfarer/loading/oOoO".equals(owner) && "o00000".equals(methodName)) counts.parser++;
                        if ("com/fs/starfarer/loading/SpecStore".equals(owner) && "Object".equals(methodName)) counts.preprocess++;
                        if ("com/fs/starfarer/loading/LoadingUtils".equals(owner)
                                && "super".equals(methodName)
                                && "(Ljava/io/InputStream;)Ljava/lang/String;".equals(methodDescriptor)) counts.inputRead++;
                        if ("org/json/JSONObject".equals(owner) && "put".equals(methodName)) counts.objectPut++;
                        if ("java/util/List".equals(owner) && "add".equals(methodName)) counts.listAdd++;
                        if ("java/util/Set".equals(owner) && "contains".equals(methodName)) counts.setContains++;
                        if ("java/util/Set".equals(owner) && "add".equals(methodName)) counts.setAdd++;
                        if ("org/json/JSONArray".equals(owner) && "put".equals(methodName)) counts.arrayPut++;
                    }
                };
            }
        };
    }

    private static final class Counts {
        int methods;
        int parser;
        int preprocess;
        int inputRead;
        int objectPut;
        int listAdd;
        int setContains;
        int setAdd;
        int arrayPut;
        final Map<String, Integer> helpers = new HashMap<String, Integer>();
    }
}
