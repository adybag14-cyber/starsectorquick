import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 * Redirects only BaseGameState.traverse()'s dynamic frame-limiter sleep through
 * BrowserFramePacer. Positive delays keep the original Thread.sleep(delay) behavior
 * so fast title/bootstrap frames yield real scheduler time; a computed zero delay
 * uses Thread.yield() instead of Thread.sleep(0) when rendering is already behind.
 */
public final class PatchBaseGameState {
    private static final String TARGET = "com/fs/starfarer/BaseGameState.class";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchBaseGameState input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] replacements = new int[] {0};

        byte[] pacerClass = loadPacerClass();
        boolean[] pacerEntrySeen = new boolean[] {false};

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
                    bytes = patch(bytes, replacements);
                }
                if ("com/fs/starfarer/BrowserFramePacer.class".equals(entry.getName())) {
                    pacerEntrySeen[0] = true;
                    bytes = pacerClass;
                }
                out.write(bytes);
                out.closeEntry();
            }
            if (!pacerEntrySeen[0]) {
                JarEntry pacer = new JarEntry("com/fs/starfarer/BrowserFramePacer.class");
                out.putNextEntry(pacer);
                out.write(pacerClass);
                out.closeEntry();
            }
        }

        if (replacements[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly one dynamic frame-limiter Thread.sleep replacement; found "
                            + replacements[0]);
        }
        System.out.println("Patched BaseGameState.traverse dynamic frame-limiter sleep=" + replacements[0]
                + " with conditional BrowserFramePacer");
    }

    private static byte[] patch(byte[] input, int[] replacements) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"traverse".equals(name) || !"()Ljava/lang/String;".equals(descriptor)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private boolean previousWasI2L = false;

                    @Override
                    public void visitInsn(int opcode) {
                        super.visitInsn(opcode);
                        previousWasI2L = opcode == Opcodes.I2L;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        boolean dynamicFrameSleep = previousWasI2L
                                && opcode == Opcodes.INVOKESTATIC
                                && "java/lang/Thread".equals(owner)
                                && "sleep".equals(methodName)
                                && "(J)V".equals(methodDescriptor);
                        previousWasI2L = false;
                        if (dynamicFrameSleep) {
                            // Preserve the long delay on the stack and route it through a
                            // helper: positive delays still sleep, zero delay cooperatively
                            // yields without entering CheerpJ's timer path.
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/fs/starfarer/BrowserFramePacer",
                                    "sleepOrYield",
                                    "(J)V",
                                    false);
                            replacements[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitVarInsn(int opcode, int varIndex) {
                        previousWasI2L = false;
                        super.visitVarInsn(opcode, varIndex);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        previousWasI2L = false;
                        super.visitLdcInsn(value);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] loadPacerClass() throws IOException {
        try (InputStream in = PatchBaseGameState.class.getResourceAsStream(
                "/com/fs/starfarer/BrowserFramePacer.class")) {
            if (in == null) {
                throw new IOException("compiled BrowserFramePacer.class not found on transformer classpath");
            }
            return readAll(in);
        }
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
