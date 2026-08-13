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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Adds manifest-first browser discovery to LoadingUtils with stock fallbacks intact. */
public final class PatchLoadingUtilsVariantIndex {
    private static final String TARGET = "com/fs/starfarer/loading/LoadingUtils.class";
    private static final String LOADING_UTILS = "com/fs/starfarer/loading/LoadingUtils";
    private static final String HELPER = "com/fs/starfarer/loading/BrowserVariantIndex";
    private static final String FILE_METHOD = "super";
    private static final String FILE_DESC =
            "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/util/List;";
    private static final String DIRECTORY_METHOD = "\u00D6" + "00000";
    private static final String DIRECTORY_DESC = "(Ljava/lang/String;)Ljava/util/List;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchLoadingUtilsVariantIndex input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] fileMethods = {0};
        int[] directoryMethods = {0};

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
                    bytes = patch(bytes, fileMethods, directoryMethods);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || fileMethods[0] != 1 || directoryMethods[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "LoadingUtils variant-index patch incomplete classes=" + classes[0]
                            + " fileMethods=" + fileMethods[0]
                            + " directoryMethods=" + directoryMethods[0]);
        }
        System.out.println("Patched LoadingUtils variant index fileMethods=" + fileMethods[0]
                + " directoryMethods=" + directoryMethods[0]);
    }

    private static byte[] patch(byte[] input, int[] fileMethods, int[] directoryMethods) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (FILE_METHOD.equals(name) && FILE_DESC.equals(descriptor)) {
                    fileMethods[0]++;
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            emitFileFastPath(this.mv);
                        }
                    };
                }
                if (DIRECTORY_METHOD.equals(name) && DIRECTORY_DESC.equals(descriptor)) {
                    directoryMethods[0]++;
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            emitDirectoryFastPath(this.mv);
                        }
                    };
                }
                return delegate;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitFileFastPath(MethodVisitor mv) {
        Label stock = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "tryListVariants",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/util/List;", false);
        mv.visitInsn(Opcodes.DUP);
        mv.visitJumpInsn(Opcodes.IFNULL, stock);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(stock);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/util/List"});
        mv.visitInsn(Opcodes.POP);
    }

    private static void emitDirectoryFastPath(MethodVisitor mv) {
        Label stock = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "tryListDirectories",
                "(Ljava/lang/String;)Ljava/util/List;", false);
        mv.visitInsn(Opcodes.DUP);
        mv.visitJumpInsn(Opcodes.IFNULL, stock);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(stock);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/util/List"});
        mv.visitInsn(Opcodes.POP);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
