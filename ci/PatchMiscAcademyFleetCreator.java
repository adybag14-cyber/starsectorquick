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
 * Small browser compatibility transforms for starfarer.api.jar.
 *
 * The Academy guard keeps an optional misc-fleet creator null-safe during campaign
 * bootstrap. The nebula transform replaces only the detailed stock
 * Misc.addNebulaFromPNG overload with BrowserNebulaCompat. The helper preserves the
 * stock terrain mask/placement semantics but bulk-reads Raster samples, removing the
 * measured per-pixel AWT virtual-call bottleneck that stalls Eos under CheerpJ.
 */
public final class PatchMiscAcademyFleetCreator {
    private static final String ACADEMY_TARGET =
            "com/fs/starfarer/api/impl/campaign/fleets/misc/MiscAcademyFleetCreator.class";
    private static final String ACADEMY_METHOD = "getAcademy";
    private static final String ACADEMY_DESC = "()Lcom/fs/starfarer/api/campaign/SectorEntityToken;";
    private static final String SECTOR = "com/fs/starfarer/api/campaign/SectorAPI";
    private static final String SYSTEM = "com/fs/starfarer/api/campaign/StarSystemAPI";

    private static final String MISC_TARGET = "com/fs/starfarer/api/util/Misc.class";
    private static final String MISC_METHOD = "addNebulaFromPNG";
    private static final String MISC_DESC =
            "(Ljava/lang/String;FFLcom/fs/starfarer/api/campaign/LocationAPI;"
                    + "Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;"
                    + "Lcom/fs/starfarer/api/impl/campaign/procgen/StarAge;)"
                    + "Lcom/fs/starfarer/api/campaign/SectorEntityToken;";
    private static final String NEBULA_COMPAT = "com/fs/starfarer/BrowserNebulaCompat";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchMiscAcademyFleetCreator input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] academyClasses = {0};
        int[] academyMethods = {0};
        int[] sectorGuards = {0};
        int[] systemGuards = {0};
        int[] miscClasses = {0};
        int[] miscMethods = {0};
        int[] bulkNebulaBodies = {0};

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
                if (ACADEMY_TARGET.equals(entry.getName())) {
                    academyClasses[0]++;
                    bytes = patchAcademy(bytes, academyMethods, sectorGuards, systemGuards);
                } else if (MISC_TARGET.equals(entry.getName())) {
                    miscClasses[0]++;
                    bytes = patchMiscNebulaBulk(bytes, miscMethods, bulkNebulaBodies);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (academyClasses[0] != 1
                || academyMethods[0] != 1
                || sectorGuards[0] != 1
                || systemGuards[0] != 1
                || miscClasses[0] != 1
                || miscMethods[0] != 1
                || bulkNebulaBodies[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "starfarer.api browser patch incomplete academyClasses=" + academyClasses[0]
                            + " academyMethods=" + academyMethods[0]
                            + " sectorGuards=" + sectorGuards[0]
                            + " systemGuards=" + systemGuards[0]
                            + " miscClasses=" + miscClasses[0]
                            + " miscMethods=" + miscMethods[0]
                            + " bulkNebulaBodies=" + bulkNebulaBodies[0]);
        }
        System.out.println(
                "Patched starfarer.api Academy null guards sector=" + sectorGuards[0]
                        + " system=" + systemGuards[0]
                        + " bulkNebulaBodies=" + bulkNebulaBodies[0]);
    }

    private static byte[] patchAcademy(
            byte[] input, int[] methods, int[] sectorGuards, int[] systemGuards) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!ACADEMY_METHOD.equals(name) || !ACADEMY_DESC.equals(descriptor)) {
                    return delegate;
                }
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
                                && "(Ljava/lang/String;)Lcom/fs/starfarer/api/campaign/StarSystemAPI;"
                                        .equals(methodDescriptor)) {
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

    private static byte[] patchMiscNebulaBulk(
            byte[] input, int[] methods, int[] bulkNebulaBodies) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate =
                        super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!MISC_METHOD.equals(name) || !MISC_DESC.equals(descriptor)) {
                    return delegate;
                }

                methods[0]++;
                delegate.visitCode();
                delegate.visitVarInsn(Opcodes.ALOAD, 0);
                delegate.visitVarInsn(Opcodes.FLOAD, 1);
                delegate.visitVarInsn(Opcodes.FLOAD, 2);
                delegate.visitVarInsn(Opcodes.ALOAD, 3);
                delegate.visitVarInsn(Opcodes.ALOAD, 4);
                delegate.visitVarInsn(Opcodes.ALOAD, 5);
                delegate.visitVarInsn(Opcodes.ILOAD, 6);
                delegate.visitVarInsn(Opcodes.ILOAD, 7);
                delegate.visitVarInsn(Opcodes.ALOAD, 8);
                delegate.visitVarInsn(Opcodes.ALOAD, 9);
                delegate.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        NEBULA_COMPAT,
                        "addNebulaFromPNG",
                        MISC_DESC,
                        false);
                delegate.visitInsn(Opcodes.ARETURN);
                delegate.visitMaxs(0, 0);
                delegate.visitEnd();
                bulkNebulaBodies[0]++;

                // Returning null tells ASM not to copy any instructions from the
                // original method after the replacement body written above.
                return null;
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
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
