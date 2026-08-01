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

/** Routes CampaignGameManager's heavy desktop procgen pass to the browser compat shim. */
public final class PatchCampaignProcGen {
    private static final String TARGET =
            "com/fs/starfarer/campaign/save/CampaignGameManager.class";
    private static final String OWNER =
            "com/fs/starfarer/api/campaign/SectorProcGenPlugin";
    private static final String DESC =
            "(Lcom/fs/starfarer/api/characters/CharacterCreationData;Lcom/fs/starfarer/api/campaign/SectorGenProgress;)V";
    private static final String SAFE_DESC =
            "(Lcom/fs/starfarer/api/campaign/SectorProcGenPlugin;Lcom/fs/starfarer/api/characters/CharacterCreationData;Lcom/fs/starfarer/api/campaign/SectorGenProgress;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchCampaignProcGen input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] replaced = new int[] {0};
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
                    bytes = patch(bytes, replaced);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (replaced[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "expected exactly one CampaignGameManager procgen generate call, found " + replaced[0]);
        }
        System.out.println("Patched CampaignGameManager desktop procgen calls=" + replaced[0]);
    }

    private static byte[] patch(byte[] input, int[] replaced) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEINTERFACE
                                && OWNER.equals(owner)
                                && "generate".equals(methodName)
                                && DESC.equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/fs/starfarer/CampaignInitCompat",
                                    "generateSectorProcGen",
                                    SAFE_DESC,
                                    false);
                            replaced[0]++;
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
