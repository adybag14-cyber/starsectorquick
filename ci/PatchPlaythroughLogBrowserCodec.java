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

/** Restores the chunked Base64 transport used by PlaythroughLog readResolve. */
public final class PatchPlaythroughLogBrowserCodec {
    private static final String TARGET =
            "com/fs/starfarer/api/impl/campaign/plog/PlaythroughLog.class";
    private static final String STOCK_TRANSPORT =
            "com/fs/starfarer/api/impl/campaign/terrain/BaseTiledTerrain";
    private static final String COMPAT =
            "com/fs/starfarer/api/impl/campaign/terrain/BrowserTiledTerrainCompat";
    private static final String TRANSPORT_DESC = "(Ljava/lang/String;)[B";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchPlaythroughLogBrowserCodec input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] readResolveMethods = {0};
        int[] transportCalls = {0};

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
                    bytes = patch(bytes, readResolveMethods, transportCalls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || readResolveMethods[0] != 1 || transportCalls[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "PlaythroughLog browser codec patch incomplete classes=" + classes[0]
                            + " readResolveMethods=" + readResolveMethods[0]
                            + " transportCalls=" + transportCalls[0]);
        }
        System.out.println(
                "Patched PlaythroughLog chunked Base64 transport calls=" + transportCalls[0]);
    }

    private static byte[] patch(byte[] input, int[] readResolveMethods, int[] transportCalls) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                final boolean readResolve = "readResolve".equals(name)
                        && "()Ljava/lang/Object;".equals(descriptor);
                if (!readResolve) return delegate;
                readResolveMethods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && STOCK_TRANSPORT.equals(owner)
                                && "toByteArray".equals(methodName)
                                && TRANSPORT_DESC.equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC, COMPAT,
                                    "decodeStockChunkedCompressedBytes",
                                    TRANSPORT_DESC, false);
                            transportCalls[0]++;
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
