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

/** Redirects BaseGameState.traverse()'s stock sleep to the browser frame pacer. */
public final class PatchBaseGameStateFramePacing {
    private static final String TARGET = "com/fs/starfarer/BaseGameState.class";
    private static final int FPS_LOCAL = 17;
    private static final int REMAINING_LOCAL = 18;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchBaseGameStateFramePacing input.jar output.jar");
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] replacements = new int[] {0};
        try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (TARGET.equals(entry.getName())) bytes = patch(bytes, replacements);
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (replacements[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("expected exactly one BaseGameState.traverse Thread.sleep call, got " + replacements[0]);
        }
        System.out.println("Patched BaseGameState.traverse precise frame pacing calls=" + replacements[0]);
    }

    private static byte[] patch(byte[] input, int[] replacements) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"traverse".equals(name) || !"()Ljava/lang/String;".equals(descriptor)) return delegate;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private int lastOpcode = -1;
                    private int lastVar = -1;
                    private boolean frameSleepReady;

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        lastOpcode = opcode;
                        lastVar = var;
                        frameSleepReady = false;
                        super.visitVarInsn(opcode, var);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        frameSleepReady = opcode == Opcodes.I2L
                                && lastOpcode == Opcodes.ILOAD && lastVar == 19;
                        lastOpcode = opcode;
                        lastVar = -1;
                        super.visitInsn(opcode);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                        if (frameSleepReady && opcode == Opcodes.INVOKESTATIC
                                && "java/lang/Thread".equals(owner)
                                && "sleep".equals(methodName) && "(J)V".equals(methodDescriptor)) {
                            // Original integer delay is already on stack. Pass the exact
                            // fractional remainder plus configured fps from traverse locals.
                            super.visitVarInsn(Opcodes.FLOAD, REMAINING_LOCAL);
                            super.visitVarInsn(Opcodes.FLOAD, FPS_LOCAL);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "com/fs/starfarer/BrowserFramePacer", "sleep", "(JFF)V", false);
                            replacements[0]++;
                            frameSleepReady = false;
                            return;
                        }
                        frameSleepReady = false;
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
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
