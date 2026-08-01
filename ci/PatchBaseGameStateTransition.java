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

/** Injects a render-thread transition drain immediately before Display.update(). */
public final class PatchBaseGameStateTransition {
    private static final String TARGET = "com/fs/starfarer/BaseGameState.class";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchBaseGameStateTransition input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] injections = new int[] {0};
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
                    bytes = patch(bytes, injections);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (injections[0] < 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("no Display.update call found in BaseGameState.traverse");
        }
        System.out.println("Patched BaseGameState render-thread transition drains=" + injections[0]);
    }

    private static byte[] patch(byte[] input, int[] injections) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"traverse".equals(name) || !"()Ljava/lang/String;".equals(descriptor)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "org/lwjgl/opengl/Display".equals(owner)
                                && "update".equals(methodName)) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/fs/starfarer/MainThreadTransitionBridge",
                                    "drain",
                                    "(Ljava/lang/Object;)V",
                                    false);
                            injections[0]++;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
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
