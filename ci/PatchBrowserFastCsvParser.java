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

/** Adds a browser/no-mod fast CSV parser before Starsector's original oOoO parser. */
public final class PatchBrowserFastCsvParser {
    private static final String ENTRY = "com/fs/starfarer/loading/oOoO.class";
    private static final String METHOD = "o00000";
    private static final String DESC = "(Ljava/lang/String;)Lorg/json/JSONArray;";
    private static final String HELPER = "com/fs/starfarer/loading/BrowserFastCsvParser";
    private static final String HELPER_METHOD = "parseIfEnabled";

    private PatchBrowserFastCsvParser() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchBrowserFastCsvParser input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, hooks = {0};
        try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (ENTRY.equals(entry.getName())) { classes[0]++; bytes = patch(bytes, methods, hooks); }
                out.write(bytes); out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || hooks[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("fast CSV patch mismatch classes=" + classes[0] + " methods=" + methods[0] + " hooks=" + hooks[0]);
        }
        System.out.println("Patched browser fast CSV parser hooks=1 stock fallback retained");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] hooks) {
        int existing = countHooks(input);
        if (existing == 1) {
            methods[0] = 1; hooks[0] = 1;
            System.out.println("Browser fast CSV hook already present; leaving bytecode unchanged.");
            return input;
        }
        if (existing != 0) throw new IllegalStateException("unexpected existing fast CSV hooks=" + existing);

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override public void visitCode() {
                        super.visitCode();
                        Label stock = new Label();
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, HELPER_METHOD, DESC, false);
                        hooks[0]++;
                        super.visitInsn(Opcodes.DUP);
                        super.visitJumpInsn(Opcodes.IFNULL, stock);
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitLabel(stock);
                        super.visitInsn(Opcodes.POP);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static int countHooks(byte[] input) {
        int[] count = {0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                if (!METHOD.equals(name) || !DESC.equals(desc)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf) {
                        if (op == Opcodes.INVOKESTATIC && HELPER.equals(owner) && HELPER_METHOD.equals(name) && DESC.equals(desc)) count[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return count[0];
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(ClassReader reader, int flags) { super(reader, flags); }
        @Override protected String getCommonSuperClass(String a, String b) { return "java/lang/Object"; }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[65536]; int read;
        while((read=in.read(buffer))>=0) out.write(buffer,0,read); return out.toByteArray();
    }
}
