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

/**
 * Adds one-shot diagnostics to CampaignFleet(Faction) so a stalled new-game
 * bootstrap can be localized to the exact constructor sub-call without skipping
 * any fleet initialization work.
 */
public final class PatchCampaignFleetConstructorDiagnostics {
    private static final String TARGET =
            "com/fs/starfarer/campaign/fleet/CampaignFleet.class";
    private static final String CTOR_DESC =
            "(Lcom/fs/starfarer/campaign/Faction;)V";
    private static final int EXPECTED_CALLS = 24;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchCampaignFleetConstructorDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classSeen = new int[] {0};
        int[] ctorSeen = new int[] {0};
        int[] calls = new int[] {0};

        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = readAll(in);
                }
                if (TARGET.equals(entry.getName())) {
                    classSeen[0]++;
                    bytes = patch(bytes, ctorSeen, calls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classSeen[0] != 1 || ctorSeen[0] != 1 || calls[0] != EXPECTED_CALLS) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "CampaignFleet constructor diagnostics incomplete: classSeen="
                            + classSeen[0]
                            + " ctorSeen="
                            + ctorSeen[0]
                            + " calls="
                            + calls[0]
                            + " expectedCalls="
                            + EXPECTED_CALLS);
        }
        System.out.println(
                "Patched CampaignFleet(Faction) constructor diagnostics calls=" + calls[0]);
    }

    private static byte[] patch(byte[] input, int[] ctorSeen, int[] calls) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"<init>".equals(name) || !CTOR_DESC.equals(descriptor)) {
                    return delegate;
                }
                ctorSeen[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private int ordinal;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emit("enter CampaignFleet(Faction)");
                    }

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        ordinal++;
                        calls[0]++;
                        String label =
                                "call#"
                                        + ordinal
                                        + " "
                                        + owner
                                        + "."
                                        + methodName
                                        + methodDescriptor;

                        // Do not insert an unrelated method call while uninitialized
                        // `this` is still on the operand stack before the superclass
                        // constructor. The constructor-entry marker above plus this
                        // after marker still distinguishes a superclass stall.
                        boolean superCtor =
                                ordinal == 1
                                        && opcode == Opcodes.INVOKESPECIAL
                                        && "com/fs/starfarer/campaign/BaseCampaignEntity".equals(owner)
                                        && "<init>".equals(methodName);
                        if (!superCtor) {
                            emit("before " + label);
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                        emit("after " + label);
                    }

                    private void emit(String message) {
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn("CampaignFleetCtorDiag: " + message);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
