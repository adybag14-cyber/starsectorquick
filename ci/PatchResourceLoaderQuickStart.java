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

/**
 * Makes ResourceLoaderState's all-ships/all-weapons sprite preload optional in
 * the browser quick-start path.
 *
 * The stock loader still performs settings/spec/script/plugin initialization and
 * its normal static UI/font/resource setup. Only queueShipAndWeaponSprites() is
 * guarded. Runtime sprite lookup remains available for the starter ship and for
 * assets encountered later in play.
 */
public final class PatchResourceLoaderQuickStart {
    private static final String TARGET = "com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String METHOD = "queueShipAndWeaponSprites";
    private static final String DESC = "()V";
    private static final String PROPERTY = "starsector.browserQuickResourceLoad";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchResourceLoaderQuickStart input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = new int[] {0};
        int[] methods = new int[] {0};

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
                    "ResourceLoaderState quick-start patch incomplete: classes="
                            + classes[0] + " methods=" + methods[0]);
        }
        System.out.println(
                "Patched ResourceLoaderState queueShipAndWeaponSprites quick-start guards=" + methods[0]);
    }

    private static byte[] patch(byte[] input, int[] methods) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) {
                    return delegate;
                }
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label stockPath = new Label();
                        super.visitLdcInsn(PROPERTY);
                        super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "java/lang/Boolean",
                                "getBoolean",
                                "(Ljava/lang/String;)Z",
                                false);
                        super.visitJumpInsn(Opcodes.IFEQ, stockPath);
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn(
                                "BrowserResourceLoader: deferred eager ship/weapon/projectile sprite preload.");
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                        super.visitInsn(Opcodes.RETURN);
                        super.visitLabel(stockPath);
                        super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
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
