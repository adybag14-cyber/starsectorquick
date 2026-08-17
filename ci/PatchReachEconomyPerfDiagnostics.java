import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Adds timing-only probes around ReachEconomy.nextStep()'s four task loops. */
public final class PatchReachEconomyPerfDiagnostics {
    private static final String TARGET = "com/fs/starfarer/campaign/econ/reach/ReachEconomy.class";
    private static final String METHOD = "nextStep";
    private static final String DESC = "(Lcom/fs/starfarer/campaign/econ/reach/MainWorkTask$EconWorkParams;)V";
    private static final String TASK = "com/fs/starfarer/campaign/econ/contract/iter/MultiFrameTask";
    private static final String DIAG = "com/fs/starfarer/campaign/econ/reach/BrowserReachEconomyPerfDiag";

    private PatchReachEconomyPerfDiagnostics() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchReachEconomyPerfDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        int[] batchCallsites = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (TARGET.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, batchCallsites);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || batchCallsites[0] != 4) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "ReachEconomy perf diagnostic shape mismatch classes=" + classes[0]
                            + " methods=" + methods[0]
                            + " batchCallsites=" + batchCallsites[0]);
        }
        System.out.println("Patched ReachEconomy.nextStep diagnostics taskLoops=4");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] batchCallsites) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return mv;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private int ordinal;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        call("begin", "()V");
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && TASK.equals(owner)
                                && "doNextBatch".equals(methodName)
                                && "()V".equals(methodDescriptor)) {
                            ordinal++;
                            batchCallsites[0]++;
                            pushInt(ordinal);
                            call("beforeBatch", "(I)V");
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            pushInt(ordinal);
                            call("afterBatch", "(I)V");
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) call("finish", "()V");
                        super.visitInsn(opcode);
                    }

                    private void pushInt(int value) {
                        if (value >= 0 && value <= 5) super.visitInsn(Opcodes.ICONST_0 + value);
                        else super.visitLdcInsn(value);
                    }

                    private void call(String name, String desc) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, DIAG, name, desc, false);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
