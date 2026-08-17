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

/** Replaces only Misc.fill()'s Math.pow call with an exact property-gated helper. */
public final class PatchMiscFractalNoisePow {
    private static final String TARGET = "com/fs/starfarer/api/util/Misc.class";
    private static final String METHOD = "fill";
    private static final String DESC = "(Ljava/util/Random;[[FIIIIIIIF)V";
    private static final String HELPER = "com/fs/starfarer/api/util/BrowserFractalNoiseCompat";
    private static final String POW_DESC = "(DD)D";

    private PatchMiscFractalNoisePow() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchMiscFractalNoisePow input.jar output.jar");
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
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
                    bytes = patch(bytes, methods);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("Misc fractal pow patch incomplete classes=" + classes[0] + " methods=" + methods[0]);
        }
        System.out.println("Patched Misc.fill fractal-noise Math.pow fast path");
    }

    private static byte[] patch(byte[] input, int[] methods) {
        Shape before = inspect(input);
        if (before.methods != 1) throw new IllegalStateException("unexpected Misc.fill method count=" + before.methods);
        if (before.helperCalls == 1 && before.mathPowCalls == 0) {
            methods[0]++;
            return input;
        }
        if (before.mathPowCalls != 1 || before.helperCalls != 0) {
            throw new IllegalStateException("unexpected Misc.fill pow shape " + before);
        }
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return mv;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && "java/lang/Math".equals(owner)
                                && "pow".equals(methodName) && POW_DESC.equals(methodDescriptor)) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "pow", POW_DESC, false);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        }, 0);
        byte[] output = writer.toByteArray();
        Shape after = inspect(output);
        if (after.methods != 1 || after.mathPowCalls != 0 || after.helperCalls != 1) {
            throw new IllegalStateException("invalid patched Misc.fill pow shape " + after);
        }
        return output;
    }

    private static Shape inspect(byte[] bytes) {
        Shape s = new Shape();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return null;
                s.methods++;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode != Opcodes.INVOKESTATIC || !POW_DESC.equals(methodDescriptor)) return;
                        if ("java/lang/Math".equals(owner) && "pow".equals(methodName)) s.mathPowCalls++;
                        if (HELPER.equals(owner) && "pow".equals(methodName)) s.helperCalls++;
                    }
                };
            }
        }, 0);
        return s;
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
        int mathPowCalls;
        int helperCalls;
        public String toString() {
            return "methods=" + methods + " mathPowCalls=" + mathPowCalls + " helperCalls=" + helperCalls;
        }
    }
}
