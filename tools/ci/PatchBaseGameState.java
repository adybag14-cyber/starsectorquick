import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

public final class PatchBaseGameState {
    private PatchBaseGameState() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchBaseGameState <input.class> <output.class>");
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        byte[] original = Files.readAllBytes(input);
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        final int[] sleepCalls = {0};
        final int[] traverseMarkers = {0};

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM8, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM8, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if ("traverse".equals(name)) {
                            super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "com/fs/starfarer/WebRuntimeCompat",
                                "onTraverse",
                                "()V",
                                false
                            );
                            traverseMarkers[0]++;
                        }
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "java/lang/Thread".equals(owner)
                                && "sleep".equals(methodName)
                                && "(J)V".equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "com/fs/starfarer/WebRuntimeCompat",
                                "safeSleep",
                                "(J)V",
                                false
                            );
                            sleepCalls[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };

        reader.accept(visitor, 0);
        if (sleepCalls[0] == 0) {
            throw new IllegalStateException("No Thread.sleep(J)V calls patched; refusing stale/incompatible patch");
        }
        if (traverseMarkers[0] == 0) {
            throw new IllegalStateException("No traverse method found; refusing stale/incompatible patch");
        }
        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
        System.out.println("PatchBaseGameState: patched Thread.sleep calls=" + sleepCalls[0]
                + " traverse markers=" + traverseMarkers[0]);
    }
}
