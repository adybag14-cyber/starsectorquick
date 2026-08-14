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
 * Reuses BrowserSpecCache's generated stock variant manifest in SpecStore.oO0000().
 *
 * The original method first lists root-level .variant files, then lists every
 * data/variants child directory and synchronously lists .variant files in each
 * directory. In a browser each directory probe is expensive. This transform keeps
 * every original LoadingUtils call in bytecode for fallback/modded sessions, but:
 *
 *  1. expands the first root-file result to the complete cached stock manifest;
 *  2. filters the following child-directory result to empty while that cache is active.
 *
 * When BrowserSpecCache is disabled/unavailable both helpers return their input
 * lists unchanged, so the stock discovery path executes exactly as before.
 */
public final class PatchSpecStoreVariantDiscovery {
    private static final String TARGET = "com/fs/starfarer/loading/SpecStore.class";
    private static final String SPEC_STORE = "com/fs/starfarer/loading/SpecStore";
    private static final String LOADING_UTILS = "com/fs/starfarer/loading/LoadingUtils";
    private static final String CACHE = "com/fs/starfarer/loading/BrowserSpecCache";
    private static final String METHOD = "oO0000";
    private static final String DESC = "()V";
    private static final String FILE_LIST_METHOD = "super";
    private static final String FILE_LIST_DESC = "(Ljava/lang/String;Ljava/lang/String;)Ljava/util/List;";
    private static final String DIRECTORY_LIST_DESC = "(Ljava/lang/String;)Ljava/util/List;";
    private static final String LIST_HELPER_DESC = "(Ljava/util/List;)Ljava/util/List;";

    private PatchSpecStoreVariantDiscovery() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchSpecStoreVariantDiscovery input.jar output.jar");
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
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = readAll(in);
                }
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
            throw new IllegalStateException(
                    "SpecStore variant-discovery patch incomplete classes=" + classes[0]
                            + " methods=" + methods[0]);
        }
        System.out.println("Patched SpecStore variant discovery methods=" + methods[0]);
    }

    private static byte[] patch(byte[] input, int[] methods) {
        Shape before = inspect(input);
        if (before.targetMethods != 1 || before.fileListCalls != 2 || before.directoryListCalls != 1) {
            throw new IllegalStateException("unexpected SpecStore variant discovery shape: " + before);
        }
        if (before.expandCalls == 1 && before.filterCalls == 1) {
            methods[0]++;
            System.out.println("SpecStore variant discovery fast path already present; leaving bytecode unchanged.");
            return input;
        }
        if (before.expandCalls != 0 || before.filterCalls != 0) {
            throw new IllegalStateException("partial/duplicate SpecStore variant discovery fast path: " + before);
        }

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private int fileListCalls;
                    private int directoryListCalls;

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && LOADING_UTILS.equals(owner)
                                && FILE_LIST_METHOD.equals(methodName)
                                && FILE_LIST_DESC.equals(methodDescriptor)) {
                            fileListCalls++;
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            if (fileListCalls == 1) {
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        CACHE,
                                        "expandVariantPaths",
                                        LIST_HELPER_DESC,
                                        false);
                            }
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && LOADING_UTILS.equals(owner)
                                && DIRECTORY_LIST_DESC.equals(methodDescriptor)) {
                            directoryListCalls++;
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            if (directoryListCalls == 1) {
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        CACHE,
                                        "filterVariantDirectories",
                                        LIST_HELPER_DESC,
                                        false);
                            }
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, 0);

        byte[] output = writer.toByteArray();
        Shape after = inspect(output);
        if (after.targetMethods != 1
                || after.fileListCalls != 2
                || after.directoryListCalls != 1
                || after.expandCalls != 1
                || after.filterCalls != 1) {
            throw new IllegalStateException("invalid patched SpecStore variant discovery shape: " + after);
        }
        return output;
    }

    private static Shape inspect(byte[] input) {
        Shape shape = new Shape();
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return null;
                shape.targetMethods++;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && LOADING_UTILS.equals(owner)) {
                            if (FILE_LIST_METHOD.equals(methodName) && FILE_LIST_DESC.equals(methodDescriptor)) {
                                shape.fileListCalls++;
                            } else if (DIRECTORY_LIST_DESC.equals(methodDescriptor)) {
                                shape.directoryListCalls++;
                            }
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && CACHE.equals(owner)
                                && LIST_HELPER_DESC.equals(methodDescriptor)) {
                            if ("expandVariantPaths".equals(methodName)) shape.expandCalls++;
                            if ("filterVariantDirectories".equals(methodName)) shape.filterCalls++;
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
        int targetMethods;
        int fileListCalls;
        int directoryListCalls;
        int expandCalls;
        int filterCalls;

        @Override
        public String toString() {
            return "targetMethods=" + targetMethods
                    + " fileListCalls=" + fileListCalls
                    + " directoryListCalls=" + directoryListCalls
                    + " expandCalls=" + expandCalls
                    + " filterCalls=" + filterCalls;
        }
    }
}
