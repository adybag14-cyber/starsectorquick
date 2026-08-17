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
 * Replaces only BaseTiledTerrain's transient init encode/decode round-trip with
 * BrowserTiledTerrainCompat. Save serialization remains on the stock codec.
 */
public final class PatchBaseTiledTerrainBrowserCodec {
    private static final String TARGET =
            "com/fs/starfarer/api/impl/campaign/terrain/BaseTiledTerrain.class";
    private static final String OWNER =
            "com/fs/starfarer/api/impl/campaign/terrain/BaseTiledTerrain";
    private static final String COMPAT =
            "com/fs/starfarer/api/impl/campaign/terrain/BrowserTiledTerrainCompat";
    private static final String INIT_DESC =
            "(Ljava/lang/String;Lcom/fs/starfarer/api/campaign/SectorEntityToken;Ljava/lang/Object;)V";
    private static final String ENCODE_DESC = "([[I)Ljava/lang/String;";
    private static final String DECODE_DESC = "(Ljava/lang/String;II)[[I";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: PatchBaseTiledTerrainBrowserCodec input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] initMethods = {0};
        int[] readResolveMethods = {0};
        int[] encodeCalls = {0};
        int[] decodeCalls = {0};

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
                    bytes = patch(bytes, initMethods, readResolveMethods, encodeCalls, decodeCalls);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || initMethods[0] != 1 || readResolveMethods[0] != 1
                || encodeCalls[0] != 1 || decodeCalls[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "BaseTiledTerrain browser codec patch incomplete classes=" + classes[0]
                            + " initMethods=" + initMethods[0]
                            + " readResolveMethods=" + readResolveMethods[0]
                            + " encodeCalls=" + encodeCalls[0]
                            + " decodeCalls=" + decodeCalls[0]);
        }
        System.out.println(
                "Patched BaseTiledTerrain transient codec encodeCalls=" + encodeCalls[0]
                        + " decodeCalls=" + decodeCalls[0]);
    }

    private static byte[] patch(
            byte[] input, int[] initMethods, int[] readResolveMethods,
            int[] encodeCalls, int[] decodeCalls) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                final boolean init = "init".equals(name) && INIT_DESC.equals(descriptor);
                final boolean readResolve = "readResolve".equals(name)
                        && "()Ljava/lang/Object;".equals(descriptor);
                if (!init && !readResolve) return delegate;
                if (init) initMethods[0]++;
                if (readResolve) readResolveMethods[0]++;

                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (init && opcode == Opcodes.INVOKESTATIC
                                && OWNER.equals(owner)
                                && "encodeTiles".equals(methodName)
                                && ENCODE_DESC.equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC, COMPAT, "encodeTilesFast",
                                    ENCODE_DESC, false);
                            encodeCalls[0]++;
                            return;
                        }
                        if (readResolve && opcode == Opcodes.INVOKESTATIC
                                && OWNER.equals(owner)
                                && "decodeTiles".equals(methodName)
                                && DECODE_DESC.equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC, COMPAT, "decodeTilesFast",
                                    DECODE_DESC, false);
                            decodeCalls[0]++;
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
