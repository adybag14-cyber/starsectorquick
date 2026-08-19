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

/** Installs the browser fast ID marshaller immediately after CampaignGameManager selects ID mode. */
public final class PatchCampaignXStreamFastIdMarshaller {
    private static final String TARGET = "com/fs/starfarer/campaign/save/CampaignGameManager.class";
    private static final String FACTORY_DESC = "(Ljava/lang/String;)Lcom/thoughtworks/xstream/XStream;";
    private static final String XSTREAM = "com/thoughtworks/xstream/XStream";
    private static final String HELPER = "com/fs/starfarer/BrowserXStreamMarshallingCompat";
    private static final String INSTALL_DESC = "(Lcom/thoughtworks/xstream/XStream;)V";

    private PatchCampaignXStreamFastIdMarshaller() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchCampaignXStreamFastIdMarshaller input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, modeCalls = {0}, installs = {0};
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                if (TARGET.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, modeCalls, installs);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || modeCalls[0] != 1 || installs[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("campaign XStream fast-ID patch mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " modeCalls=" + modeCalls[0] + " installs=" + installs[0]);
        }
        System.out.println("Patched campaign XStream fast ID marshaller factory=1 setMode=1 install=1");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] modeCalls, int[] installs) {
        int existing = countInstalls(input);
        if (existing == 1) {
            methods[0] = modeCalls[0] = installs[0] = 1;
            System.out.println("Campaign XStream fast ID marshaller already installed; leaving bytecode unchanged.");
            return input;
        }
        if (existing != 0) throw new IllegalStateException("duplicate fast-ID installs=" + existing);

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if ((access & Opcodes.ACC_STATIC) == 0 || !FACTORY_DESC.equals(desc)) return delegate;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    boolean counted;
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDesc, boolean isInterface) {
                        super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface);
                        if (opcode == Opcodes.INVOKEVIRTUAL && XSTREAM.equals(owner)
                                && "setMode".equals(methodName) && "(I)V".equals(methodDesc)) {
                            if (!counted) { methods[0]++; counted = true; }
                            modeCalls[0]++;
                            super.visitVarInsn(Opcodes.ALOAD, 1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "install", INSTALL_DESC, false);
                            installs[0]++;
                        }
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static int countInstalls(byte[] input) {
        int[] count = {0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && HELPER.equals(owner)
                                && "install".equals(n) && INSTALL_DESC.equals(d)) count[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return count[0];
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
