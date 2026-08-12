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

/** Routes ScriptStore's singleton plugin lookup through a browser compatibility resolver. */
public final class PatchScriptStorePluginFallback {
    private static final String TARGET = "com/fs/starfarer/loading/scripts/ScriptStore.class";
    private static final String TARGET_NAME = "com/fs/starfarer/loading/scripts/ScriptStore";
    private static final String REPOSITORY_DESC = "Lcom/fs/util/container/repo/ObjectRepository;";
    private static final String HELPER = "com/fs/starfarer/loading/scripts/BrowserScriptPluginResolver";
    private static final String HELPER_ENTRY = HELPER + ".class";

    private PatchScriptStorePluginFallback() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchScriptStorePluginFallback input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        byte[] helper = loadHelperClass();
        int[] replacements = {0};
        boolean[] helperSeen = {false};

        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = readAll(in);
                }
                if (TARGET.equals(entry.getName())) {
                    bytes = patch(bytes, replacements);
                } else if (HELPER_ENTRY.equals(entry.getName())) {
                    bytes = helper;
                    helperSeen[0] = true;
                }
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                out.write(bytes);
                out.closeEntry();
            }
            if (!helperSeen[0]) {
                JarEntry helperEntry = new JarEntry(HELPER_ENTRY);
                out.putNextEntry(helperEntry);
                out.write(helper);
                out.closeEntry();
            }
        }

        if (replacements[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly one ScriptStore.o00000(Class) replacement; found "
                            + replacements[0]);
        }
        System.out.println("Patched ScriptStore singleton plugin lookup with browser fallback.");
    }

    private static byte[] patch(byte[] input, int[] replacements) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if ("o00000".equals(name)
                        && "(Ljava/lang/Class;)Ljava/lang/Object;".equals(descriptor)) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    mv.visitFieldInsn(Opcodes.GETSTATIC, TARGET_NAME, "class", REPOSITORY_DESC);
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            HELPER,
                            "resolve",
                            "(Lcom/fs/util/container/repo/ObjectRepository;Ljava/lang/Class;)Ljava/lang/Object;",
                            false);
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(2, 1);
                    mv.visitEnd();
                    replacements[0]++;
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] loadHelperClass() throws IOException {
        try (InputStream in = PatchScriptStorePluginFallback.class.getResourceAsStream("/" + HELPER_ENTRY)) {
            if (in == null) {
                throw new IOException("compiled helper class not found: " + HELPER_ENTRY);
            }
            return readAll(in);
        }
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
