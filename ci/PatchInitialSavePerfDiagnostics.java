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

/** Adds timing-only probes to CampaignGameManager's synchronous save implementation. */
public final class PatchInitialSavePerfDiagnostics {
    private static final String TARGET = "com/fs/starfarer/campaign/save/CampaignGameManager.class";
    private static final String METHOD = "o00000";
    private static final String DESC = "(Lcom/fs/starfarer/campaign/CampaignEngine$o;JZ)Ljava/lang/String;";
    private static final String DIAG = "com/fs/starfarer/BrowserInitialSavePerfDiag";

    private PatchInitialSavePerfDiagnostics() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchInitialSavePerfDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        Shape shape = new Shape();
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
                    bytes = patch(bytes, shape);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || !shape.valid()) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("initial-save diagnostic shape mismatch classes="
                    + classes[0] + " " + shape);
        }
        System.out.println("Patched initial-save diagnostics " + shape);
    }

    private static byte[] patch(byte[] input, Shape shape) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return mv;
                shape.methods++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private int serializerFactoryOrdinal;
                    private int toXmlOrdinal;
                    private int writerFinishOrdinal;
                    private int joinOrdinal;
                    private int fileSingleOrdinal;
                    private int fileMoveOrdinal;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        call("begin", "()V");
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "java/lang/System".equals(owner)
                                && "gc".equals(methodName)
                                && "()V".equals(methodDescriptor)) {
                            shape.gcCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface, "gc");
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/campaign/save/CampaignGameManager".equals(owner)
                                && "Ò00000".equals(methodName)
                                && "(Ljava/lang/String;)Lcom/thoughtworks/xstream/XStream;".equals(methodDescriptor)) {
                            serializerFactoryOrdinal++;
                            shape.serializerFactoryCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "serializerFactory" + serializerFactoryOrdinal);
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "com/thoughtworks/xstream/XStream".equals(owner)
                                && "toXML".equals(methodName)
                                && "(Ljava/lang/Object;Ljava/io/OutputStream;)V".equals(methodDescriptor)) {
                            toXmlOrdinal++;
                            shape.toXmlCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "toXML" + toXmlOrdinal);
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "com/fs/starfarer/util/do".equals(owner)
                                && "Ò00000".equals(methodName)
                                && "()V".equals(methodDescriptor)) {
                            writerFinishOrdinal++;
                            shape.writerFinishCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "writerFinish" + writerFinishOrdinal);
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "java/lang/Thread".equals(owner)
                                && "join".equals(methodName)
                                && "()V".equals(methodDescriptor)) {
                            joinOrdinal++;
                            shape.joinCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "threadJoin" + joinOrdinal);
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/campaign/save/CampaignGameManager".equals(owner)
                                && "Object".equals(methodName)
                                && "(Ljava/io/File;)V".equals(methodDescriptor)) {
                            fileSingleOrdinal++;
                            shape.fileCleanupCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "fileCleanup" + fileSingleOrdinal);
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/campaign/save/CampaignGameManager".equals(owner)
                                && "o00000".equals(methodName)
                                && "(Ljava/io/File;)V".equals(methodDescriptor)) {
                            fileSingleOrdinal++;
                            shape.fileCleanupCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "fileCleanup" + fileSingleOrdinal);
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/campaign/save/CampaignGameManager".equals(owner)
                                && "o00000".equals(methodName)
                                && "(Ljava/io/File;Ljava/io/File;)V".equals(methodDescriptor)) {
                            fileMoveOrdinal++;
                            shape.fileMoveCalls++;
                            around(opcode, owner, methodName, methodDescriptor, isInterface,
                                    "fileMove" + fileMoveOrdinal);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN || opcode == Opcodes.ATHROW) {
                            call("finish", "()V");
                        }
                        super.visitInsn(opcode);
                    }

                    private void around(int opcode, String owner, String methodName,
                                        String methodDescriptor, boolean isInterface, String phase) {
                        mark(phase + "-before");
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        mark(phase + "-after");
                    }

                    private void mark(String label) {
                        super.visitLdcInsn(label);
                        call("mark", "(Ljava/lang/String;)V");
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

    private static final class Shape {
        int methods;
        int gcCalls;
        int serializerFactoryCalls;
        int toXmlCalls;
        int writerFinishCalls;
        int joinCalls;
        int fileCleanupCalls;
        int fileMoveCalls;

        boolean valid() {
            return methods == 1 && gcCalls == 1 && serializerFactoryCalls == 2
                    && toXmlCalls == 3
                    && joinCalls >= 1 && fileCleanupCalls == 4 && fileMoveCalls == 4;
        }

        public String toString() {
            return "methods=" + methods + " gc=" + gcCalls
                    + " serializerFactories=" + serializerFactoryCalls
                    + " toXML=" + toXmlCalls + " writerFinish=" + writerFinishCalls
                    + " joins=" + joinCalls + " fileCleanup=" + fileCleanupCalls
                    + " fileMoves=" + fileMoveCalls;
        }
    }
}
