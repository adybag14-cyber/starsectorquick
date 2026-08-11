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
 * Fixes Starsector's ImageIO texture upload path for browser/WebGL.
 *
 * The stock loader copies DataBufferByte storage verbatim while declaring the
 * upload as RGBA. ImageIO decodes the shipped assets as TYPE_3BYTE_BGR or
 * TYPE_4BYTE_ABGR, so the stock path both swaps channels and, for 24-bit
 * images, allocates three bytes per pixel even though GL_RGBA consumes four.
 */
public final class PatchTextureUploadRaster {
    private static final String TARGET = "com/fs/starfarer/util/O.class";
    private static final String METHOD = "o00000";
    private static final String DESC = "(Ljava/awt/image/BufferedImage;)I";
    private static final String BYTE_BUFFER = "java/nio/ByteBuffer";
    private static final String DATA_BUFFER_BYTE = "java/awt/image/DataBufferByte";
    private static final String COMPAT = "com/fs/starfarer/TextureUploadCompat";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchTextureUploadRaster input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] allocations = {0};
        int[] canonicalizers = {0};
        int[] classes = {0};

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
                    bytes = patch(bytes, allocations, canonicalizers);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || allocations[0] != 2 || canonicalizers[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "unexpected texture loader patch counts classes=" + classes[0]
                            + " allocations=" + allocations[0]
                            + " canonicalizers=" + canonicalizers[0]);
        }
        System.out.println(
                "Patched texture raster uploads RGBA allocations=" + allocations[0]
                        + " byte-raster canonicalizers=" + canonicalizers[0]);
    }

    private static byte[] patch(byte[] input, int[] allocations, int[] canonicalizers) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && BYTE_BUFFER.equals(owner)
                                && "allocateDirect".equals(methodName)
                                && "(I)Ljava/nio/ByteBuffer;".equals(methodDescriptor)) {
                            // Replace stock pixelSize/8 * width * height with RGBA8 size.
                            super.visitInsn(Opcodes.POP);
                            super.visitInsn(Opcodes.ICONST_4);
                            super.visitVarInsn(Opcodes.ILOAD, 1);
                            super.visitInsn(Opcodes.IMUL);
                            super.visitVarInsn(Opcodes.ILOAD, 2);
                            super.visitInsn(Opcodes.IMUL);
                            allocations[0]++;
                            super.visitMethodInsn(
                                    opcode, owner, methodName, methodDescriptor, isInterface);
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && DATA_BUFFER_BYTE.equals(owner)
                                && "getData".equals(methodName)
                                && "()[B".equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                    opcode, owner, methodName, methodDescriptor, isInterface);
                            // Stack is [ByteBuffer, rawBytes]. Add the BufferedImage local so
                            // the helper can canonicalize BGR/ABGR into RGBA without changing
                            // the surrounding ByteBuffer.put(byte[]) bytecode.
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    COMPAT,
                                    "canonicalizeImageBytes",
                                    "([BLjava/awt/image/BufferedImage;)[B",
                                    false);
                            canonicalizers[0]++;
                            return;
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
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
