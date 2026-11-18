import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Executes the production constructor patch with live arguments and return values. */
public final class VerifyCampaignFleetDiagnostics {
    private static final String PROPERTY = "starsector.browserCampaignCreateDiagnostics";
    private static final String FLEET = "com/fs/starfarer/campaign/fleet/CampaignFleet";
    private static final String BASE = "com/fs/starfarer/campaign/BaseCampaignEntity";
    private static final String FACTION = "com/fs/starfarer/campaign/Faction";
    private static final String HELPER = "com/fs/starfarer/CampaignFleetDiagnostics";

    public static void main(String[] args) throws Exception {
        byte[] original = fleet();
        int[] seen = {0}, calls = {0};
        Method patch = PatchCampaignCreateDiagnostics.class.getDeclaredMethod(
                "patchFleetConstructor", byte[].class, int[].class, int[].class);
        patch.setAccessible(true);
        byte[] patched = (byte[]) patch.invoke(null, original, seen, calls);
        require(seen[0] == 1 && calls[0] == 24, "production patch did not see 24 original calls");
        List<String> messages = new ArrayList<String>();
        require(instructions(original, false, new ArrayList<String>()).equals(
                instructions(patched, true, messages)), "patch changed original instructions/order");
        List<String> expected = expectedMessages();
        require(messages.equals(expected), "injected messages/order changed: " + messages);

        Map<String, byte[]> classes = new HashMap<String, byte[]>();
        classes.put(BASE.replace('/', '.'), plain(BASE));
        classes.put(FACTION.replace('/', '.'), plain(FACTION));
        classes.put(HELPER.replace('/', '.'), resource(HELPER + ".class"));
        String previous = System.getProperty(PROPERTY);
        try {
            classes.put(FLEET.replace('/', '.'), original);
            require(run(classes, null).isEmpty(), "unpatched constructor produced output");
            classes.put(FLEET.replace('/', '.'), patched);
            require(run(classes, null).isEmpty(), "default-off constructor produced output");
            require(run(classes, "false").isEmpty(), "explicit-off constructor produced output");
            String expectedOutput = String.join(System.lineSeparator(), expected) + System.lineSeparator();
            require(run(classes, "true").equals(expectedOutput), "opt-in output differs from 48 original lines");
        } finally {
            if (previous == null) System.clearProperty(PROPERTY);
            else System.setProperty(PROPERTY, previous);
        }
        System.out.println("PASS fleet constructor diagnostics: original instructions preserved; "
                + "JVM-verified argument/result stack; default/explicit off silent; opt-in exact 48 lines");
    }

    private static String run(final Map<String, byte[]> classes, String enabled) throws Exception {
        if (enabled == null) System.clearProperty(PROPERTY);
        else System.setProperty(PROPERTY, enabled);
        ClassLoader loader = new ClassLoader(null) {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name);
                if (bytes == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        try (PrintStream capture = new PrintStream(bytes, true, "UTF-8")) {
            System.setOut(capture);
            Class<?> type = loader.loadClass(FLEET.replace('/', '.'));
            Class<?> faction = loader.loadClass(FACTION.replace('/', '.'));
            type.getConstructor(faction).newInstance(new Object[] {null});
            require(type.getField("checksum").getInt(null) == 276, "constructor state differs");
        } finally {
            System.setOut(previous);
        }
        return bytes.toString("UTF-8");
    }

    private static List<String> expectedMessages() {
        List<String> result = new ArrayList<String>();
        result.add("CampaignFleetCtorDiag: enter CampaignFleet(Faction)");
        result.add("CampaignFleetCtorDiag: after call#1 " + BASE + ".<init>()V");
        for (int i = 2; i <= 24; i++) {
            String call = "call#" + i + " " + FLEET + ".record(I)I";
            result.add("CampaignFleetCtorDiag: before " + call);
            result.add("CampaignFleetCtorDiag: after " + call);
        }
        return result;
    }

    private static List<String> instructions(byte[] bytes, final boolean strip,
            final List<String> messages) {
        final List<String> result = new ArrayList<String>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                result.add("method:" + access + ":" + name + ":" + desc);
                return new MethodVisitor(Opcodes.ASM9) {
                    private String pending;
                    @Override public void visitInsn(int opcode) { result.add("insn:" + opcode); }
                    @Override public void visitVarInsn(int opcode, int var) {
                        result.add("var:" + opcode + ":" + var);
                    }
                    @Override public void visitIntInsn(int opcode, int operand) {
                        result.add("int:" + opcode + ":" + operand);
                    }
                    @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        result.add("field:" + opcode + ":" + owner + ":" + name + ":" + desc);
                    }
                    @Override public void visitLdcInsn(Object value) {
                        if (strip && value instanceof String
                                && ((String)value).startsWith("CampaignFleetCtorDiag: ")) {
                            require(pending == null, "unconsumed checkpoint");
                            pending = (String)value;
                        } else result.add("ldc:" + value);
                    }
                    @Override public void visitMethodInsn(int opcode, String owner,
                            String name, String desc, boolean itf) {
                        if (strip && HELPER.equals(owner)) {
                            require(opcode == Opcodes.INVOKESTATIC && "log".equals(name)
                                    && "(Ljava/lang/String;)V".equals(desc) && !itf
                                    && pending != null, "invalid diagnostic call");
                            messages.add(pending);
                            pending = null;
                        } else {
                            require(pending == null, "checkpoint not followed by helper");
                            result.add("call:" + opcode + ":" + owner + ":" + name + ":" + desc + ":" + itf);
                        }
                    }
                    @Override public void visitEnd() { require(pending == null, "trailing checkpoint"); }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result;
    }

    private static byte[] plain(String name) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        MethodVisitor ctor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] fleet() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, FLEET, null, BASE, null);
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "checksum", "I", null, null).visitEnd();
        MethodVisitor ctor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "(L" + FACTION + ";)V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, BASE, "<init>", "()V", false);
        for (int i = 1; i <= 23; i++) {
            ctor.visitIntInsn(Opcodes.BIPUSH, i);
            // Before logging sees a live int argument; after logging sees a live return value.
            ctor.visitMethodInsn(Opcodes.INVOKESTATIC, FLEET, "record", "(I)I", false);
            ctor.visitInsn(Opcodes.POP);
        }
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        MethodVisitor record = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "record", "(I)I", null, null);
        record.visitCode();
        record.visitFieldInsn(Opcodes.GETSTATIC, FLEET, "checksum", "I");
        record.visitVarInsn(Opcodes.ILOAD, 0);
        record.visitInsn(Opcodes.IADD);
        record.visitFieldInsn(Opcodes.PUTSTATIC, FLEET, "checksum", "I");
        record.visitVarInsn(Opcodes.ILOAD, 0);
        record.visitInsn(Opcodes.ICONST_2);
        record.visitInsn(Opcodes.IMUL);
        record.visitInsn(Opcodes.IRETURN);
        record.visitMaxs(0, 0);
        record.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] resource(String path) throws Exception {
        try (InputStream in = VerifyCampaignFleetDiagnostics.class.getClassLoader().getResourceAsStream(path)) {
            require(in != null, "helper missing from compiled runtime classpath: " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
            return out.toByteArray();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
