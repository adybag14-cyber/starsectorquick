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
 * Makes the optional Galatia Academy misc-fleet creator tolerate minimal worlds.
 *
 * Browser quick-start intentionally ships a reduced campaign economy. The stock
 * creator already treats a null Academy entity as "no route", but getAcademy()
 * dereferences a missing Galatia system before it can return null. Guard both
 * Global.getSector() and getStarSystem("Galatia") so the existing null path is
 * reached without synthesizing campaign content.
 */
public final class PatchMiscAcademyFleetCreator {
    private static final String TARGET =
            "com/fs/starfarer/api/impl/campaign/fleets/misc/MiscAcademyFleetCreator.class";
    private static final String METHOD = "getAcademy";
    private static final String DESC = "()Lcom/fs/starfarer/api/campaign/SectorEntityToken;";
    private static final String SECTOR = "com/fs/starfarer/api/campaign/SectorAPI";
    private static final String SYSTEM = "com/fs/starfarer/api/campaign/StarSystemAPI";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchMiscAcademyFleetCreator input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        int[] sectorGuards = {0};
        int[] systemGuards = {0};

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
                    bytes = patch(bytes, methods, sectorGuards, systemGuards);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || methods[0] != 1 || sectorGuards[0] != 1 || systemGuards[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "Academy null guard incomplete classes=" + classes[0]
                            + " methods=" + methods[0]
                            + " sectorGuards=" + sectorGuards[0]
                            + " systemGuards=" + systemGuards[0]);
        }
        System.out.println(
                "Patched MiscAcademyFleetCreator null guards sector=" + sectorGuards[0]
                        + " system=" + systemGuards[0]);
    }

    private static byte[] patch(byte[] input, int[] methods, int[] sectorGuards, int[] systemGuards) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/api/Global".equals(owner)
                                && "getSector".equals(methodName)
                                && "()Lcom/fs/starfarer/api/campaign/SectorAPI;".equals(methodDescriptor)) {
                            emitNullableReturnGuard(this.mv, SECTOR);
                            sectorGuards[0]++;
                        } else if (opcode == Opcodes.INVOKEINTERFACE
                                && SECTOR.equals(owner)
                                && "getStarSystem".equals(methodName)
                                && "(Ljava/lang/String;)Lcom/fs/starfarer/api/campaign/StarSystemAPI;".equals(methodDescriptor)) {
                            emitNullableReturnGuard(this.mv, SYSTEM);
                            systemGuards[0]++;
                        }
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitNullableReturnGuard(MethodVisitor mv, String stackType) {
        Label nonNull = new Label();
        mv.visitInsn(Opcodes.DUP);
        mv.visitJumpInsn(Opcodes.IFNONNULL, nonNull);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(nonNull);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {stackType});
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
