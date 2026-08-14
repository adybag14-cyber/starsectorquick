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

/** Replaces TextureLoader's browser-hostile per-pixel Java upload preparation. */
public final class PatchTextureLoaderBulkUpload {
    private static final String TARGET_ENTRY = "com/fs/graphics/TextureLoader.class";
    private static final String TARGET = "com/fs/graphics/TextureLoader";
    private static final String METHOD = "super";
    private static final String DESC = "(Ljava/awt/image/BufferedImage;Lcom/fs/graphics/Object;)Ljava/nio/ByteBuffer;";
    private static final String COMPAT = "com/fs/starfarer/TextureUploadCompat";
    private static final String PREPARED = "com/fs/starfarer/TextureUploadCompat$PreparedTexture";
    private static final String PREPARE_DESC = "(Ljava/awt/image/BufferedImage;)Lcom/fs/starfarer/TextureUploadCompat$PreparedTexture;";
    private static final String COLOR_DESC = "Ljava/awt/Color;";
    private static final String BUFFER_DESC = "Ljava/nio/ByteBuffer;";

    // Exact stock 0.98a-RC8 obfuscated field names. Fail closed if they drift.
    private static final String AVERAGE_FIELD = "\u00f500000";
    private static final String MEDIAN_FIELD = "interface";
    private static final String ACCENT_FIELD = "\u00d300000";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchTextureLoaderBulkUpload input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        int[] expectedFields = {0};

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
                if (TARGET_ENTRY.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, expectedFields);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || methods[0] != 1 || expectedFields[0] != 3) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "TextureLoader bulk patch mismatch classes=" + classes[0]
                            + " methods=" + methods[0]
                            + " colorFields=" + expectedFields[0]);
        }
        System.out.println("Patched TextureLoader bulk upload methods=" + methods[0]
                + " colorFields=" + expectedFields[0]);
    }

    private static byte[] patch(byte[] input, int[] methods, int[] expectedFields) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                              String signature, Object value) {
                if (COLOR_DESC.equals(descriptor)
                        && (AVERAGE_FIELD.equals(name) || MEDIAN_FIELD.equals(name) || ACCENT_FIELD.equals(name))) {
                    expectedFields[0]++;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                methods[0]++;
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                emitReplacement(mv);
                // Returning null discards the stock ~1000-byte per-pixel loop while the
                // replacement method header/body already exists in the output class.
                return null;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitReplacement(MethodVisitor mv) {
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, COMPAT, "prepareTexture", PREPARE_DESC, false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        putColor(mv, AVERAGE_FIELD, "getAverageColor");
        putColor(mv, MEDIAN_FIELD, "getMedianColor");
        putColor(mv, ACCENT_FIELD, "getAccentColor");

        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PREPARED, "getBuffer", "()" + BUFFER_DESC, false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void putColor(MethodVisitor mv, String field, String getter) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PREPARED, getter, "()" + COLOR_DESC, false);
        mv.visitFieldInsn(Opcodes.PUTFIELD, TARGET, field, COLOR_DESC);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}

