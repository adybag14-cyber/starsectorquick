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

/** Uses Sprite's stock null-safe average-color path inside Ship.renderSlotCovers. */
public final class PatchShipSlotCoverSafeAverageColor {
    private static final String ENTRY = "com/fs/starfarer/combat/entities/Ship.class";
    private static final String METHOD = "renderSlotCovers";
    private static final String DESC = "(Ljava/awt/Color;ZF)V";
    private static final String SPRITE = "com/fs/graphics/Sprite";
    private static final String TEXTURE = "com/fs/graphics/Object";
    private static final String TEXTURE_DESC = "()Lcom/fs/graphics/Object;";
    private static final String COLOR_DESC = "()Ljava/awt/Color;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchShipSlotCoverSafeAverageColor input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, replacements = {0};
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
                if (ENTRY.equals(entry.getName())) {
                    classes[0]++;
                    bytes = patch(bytes, methods, replacements);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || replacements[0] != 3) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("slot-cover safe-color shape mismatch classes=" + classes[0]
                    + " methods=" + methods[0] + " replacements=" + replacements[0]);
        }
        System.out.println("Patched Ship.renderSlotCovers null-safe average-color chains=" + replacements[0]);
    }

    private static byte[] patch(byte[] input, int[] methods, int[] replacements) {
        int existing = countSafeCalls(input);
        if (existing == 3) {
            methods[0] = 1;
            replacements[0] = 3;
            return input;
        }
        if (existing != 0) throw new IllegalStateException("unexpected existing Sprite.getAverageColor calls=" + existing);
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor delegate = super.visitMethod(access, name, desc, sig, ex);
                if (!METHOD.equals(name) || !DESC.equals(desc)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    boolean pendingTexture = false;
                    private void rejectPending(String kind) {
                        if (pendingTexture) throw new IllegalStateException("Sprite.getTexture chain changed before average-color: " + kind);
                    }
                    @Override public void visitMethodInsn(int opcode, String owner, String methodName, String methodDesc, boolean itf) {
                        if (pendingTexture) {
                            if (opcode == Opcodes.INVOKEVIRTUAL && TEXTURE.equals(owner) && COLOR_DESC.equals(methodDesc)) {
                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, SPRITE, "getAverageColor", COLOR_DESC, false);
                                replacements[0]++;
                                pendingTexture = false;
                                return;
                            }
                            throw new IllegalStateException("unexpected call after Sprite.getTexture: " + owner + "." + methodName + methodDesc);
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL && SPRITE.equals(owner)
                                && "getTexture".equals(methodName) && TEXTURE_DESC.equals(methodDesc)) {
                            pendingTexture = true;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDesc, itf);
                    }
                    @Override public void visitInsn(int opcode) { rejectPending("insn"); super.visitInsn(opcode); }
                    @Override public void visitIntInsn(int opcode, int operand) { rejectPending("int-insn"); super.visitIntInsn(opcode, operand); }
                    @Override public void visitVarInsn(int opcode, int var) { rejectPending("var-insn"); super.visitVarInsn(opcode, var); }
                    @Override public void visitTypeInsn(int opcode, String type) { rejectPending("type-insn"); super.visitTypeInsn(opcode, type); }
                    @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) { rejectPending("field-insn"); super.visitFieldInsn(opcode, owner, name, desc); }
                    @Override public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) { rejectPending("jump-insn"); super.visitJumpInsn(opcode, label); }
                    @Override public void visitLdcInsn(Object value) { rejectPending("ldc"); super.visitLdcInsn(value); }
                    @Override public void visitIincInsn(int var, int increment) { rejectPending("iinc"); super.visitIincInsn(var, increment); }
                    @Override public void visitEnd() { rejectPending("method-end"); super.visitEnd(); }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static int countSafeCalls(byte[] input) {
        int[] count = {0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                if (!METHOD.equals(name) || !DESC.equals(desc)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String methodName, String methodDesc, boolean itf) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && SPRITE.equals(owner)
                                && "getAverageColor".equals(methodName) && COLOR_DESC.equals(methodDesc)) count[0]++;
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
