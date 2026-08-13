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
 * Instruments SpecStore.public(ResourceLoaderState) at each top-level static
 * loader call after the initial plugin registration loop. The markers are
 * diagnostics only: they preserve the operand stack and original call order.
 */
public final class PatchSpecStoreDiagnostics {
    private static final String TARGET = "com/fs/starfarer/loading/SpecStore.class";
    private static final String SPECSTORE = "com/fs/starfarer/loading/SpecStore";
    private static final String METHOD = "public";
    private static final String DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    private static final String MARKER = "BrowserSpecStoreStage: ";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchSpecStoreDiagnostics input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        int[] calls = {0};
        int[] markers = {0};

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
                    bytes = patch(bytes, methods, calls, markers);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || methods[0] != 1 || calls[0] < 30 || markers[0] != calls[0] * 2) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "SpecStore diagnostics patch incomplete classes=" + classes[0]
                            + " methods=" + methods[0]
                            + " calls=" + calls[0]
                            + " markers=" + markers[0]);
        }
        System.out.println("Patched SpecStore diagnostics calls=" + calls[0] + " markers=" + markers[0]);
    }

    private static byte[] patch(byte[] input, int[] methods, int[] calls, int[] markers) {
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
                    private boolean profiling;
                    private int index;

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        // The first SpecStore(ResourceLoaderState) call occurs immediately
                        // after the plugin-registration loop. From this point on, static
                        // invocations are the serial top-level loader/finalizer chain.
                        if (!profiling
                                && opcode == Opcodes.INVOKESTATIC
                                && SPECSTORE.equals(owner)
                                && DESC.equals(methodDescriptor)) {
                            profiling = true;
                        }

                        if (profiling && opcode == Opcodes.INVOKESTATIC) {
                            index++;
                            calls[0]++;
                            String id = String.format("%03d", index);
                            String label = shortOwner(owner) + "." + printable(methodName) + methodDescriptor;
                            System.out.println("SpecStoreDiagMap: " + id + " " + label);
                            emitMarker(this.mv, id, "before", label);
                            markers[0]++;
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            emitMarker(this.mv, id, "after", label);
                            markers[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitMarker(MethodVisitor mv, String id, String direction, String label) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(MARKER + id + ":" + direction + ":" + label);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static String shortOwner(String owner) {
        int slash = owner.lastIndexOf('/');
        return slash >= 0 ? owner.substring(slash + 1) : owner;
    }

    private static String printable(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 32 && c <= 126 && c != ':') {
                out.append(c);
            } else {
                out.append(String.format("\\u%04x", (int)c));
            }
        }
        return out.toString();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
