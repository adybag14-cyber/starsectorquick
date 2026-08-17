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

/** Replaces one short-lived Random allocation with an exact browser-gated factory. */
public final class PatchEconomyStockpileRandom {
    private static final String TARGET = "com/fs/starfarer/campaign/econ/reach/MainWorkTask2.class";
    private static final String METHOD = "updateStockpileAndPriceV2";
    private static final String DESC = "(Lcom/fs/starfarer/campaign/econ/Market;Lcom/fs/starfarer/loading/F;)V";
    private static final String RANDOM = "java/util/Random";
    private static final String HELPER = "com/fs/starfarer/campaign/econ/reach/BrowserEconomyRandomCompat";

    private PatchEconomyStockpileRandom() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchEconomyStockpileRandom input.jar output.jar");
        }
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
            throw new IllegalStateException("economy Random patch incomplete classes=" + classes[0]
                    + " methods=" + methods[0]);
        }
        System.out.println("Patched economy stockpile Random allocation");
    }

    private static byte[] patch(byte[] input, int[] methods) {
        Shape before = inspect(input);
        if (before.methods != 1 || before.nextFloatCalls != 2) {
            throw new IllegalStateException("unexpected stockpile Random method shape " + before);
        }
        if (before.acquireCalls == 1 && before.randomNews == 0 && before.randomConstructors == 0) {
            methods[0]++;
            return input;
        }
        if (before.acquireCalls != 0 || before.randomNews != 1 || before.randomConstructors != 1) {
            throw new IllegalStateException("unexpected stockpile Random allocation shape " + before);
        }

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
                    private boolean suppressRandomDup;
                    private boolean randomConstruction;

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW && RANDOM.equals(type)) {
                            if (randomConstruction) throw new IllegalStateException("nested Random construction");
                            randomConstruction = true;
                            suppressRandomDup = true;
                            return;
                        }
                        super.visitTypeInsn(opcode, type);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (suppressRandomDup) {
                            if (opcode != Opcodes.DUP) {
                                throw new IllegalStateException("expected DUP after Random NEW, got opcode=" + opcode);
                            }
                            suppressRandomDup = false;
                            return;
                        }
                        super.visitInsn(opcode);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (randomConstruction && opcode == Opcodes.INVOKESPECIAL
                                && RANDOM.equals(owner) && "<init>".equals(methodName)
                                && "(J)V".equals(methodDescriptor)) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "acquire",
                                    "(J)Ljava/util/Random;", false);
                            randomConstruction = false;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitEnd() {
                        if (suppressRandomDup || randomConstruction) {
                            throw new IllegalStateException("unterminated Random construction rewrite");
                        }
                        super.visitEnd();
                    }
                };
            }
        }, 0);
        byte[] output = writer.toByteArray();
        Shape after = inspect(output);
        if (after.methods != 1 || after.randomNews != 0 || after.randomConstructors != 0
                || after.acquireCalls != 1 || after.nextFloatCalls != 2) {
            throw new IllegalStateException("invalid patched stockpile Random shape " + after);
        }
        return output;
    }

    private static Shape inspect(byte[] bytes) {
        Shape shape = new Shape();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return null;
                shape.methods++;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW && RANDOM.equals(type)) shape.randomNews++;
                    }
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESPECIAL && RANDOM.equals(owner)
                                && "<init>".equals(methodName) && "(J)V".equals(methodDescriptor)) {
                            shape.randomConstructors++;
                        }
                        if (opcode == Opcodes.INVOKESTATIC && HELPER.equals(owner)
                                && "acquire".equals(methodName)
                                && "(J)Ljava/util/Random;".equals(methodDescriptor)) {
                            shape.acquireCalls++;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL && RANDOM.equals(owner)
                                && "nextFloat".equals(methodName) && "()F".equals(methodDescriptor)) {
                            shape.nextFloatCalls++;
                        }
                    }
                };
            }
        }, 0);
        return shape;
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
        int randomNews;
        int randomConstructors;
        int acquireCalls;
        int nextFloatCalls;
        public String toString() {
            return "methods=" + methods + " randomNews=" + randomNews
                    + " randomConstructors=" + randomConstructors
                    + " acquireCalls=" + acquireCalls + " nextFloatCalls=" + nextFloatCalls;
        }
    }
}
