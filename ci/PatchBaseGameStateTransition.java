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
 * Keeps AppDriver-owned work on BaseGameState.traverse().
 *
 * In addition to draining queued state transitions immediately before
 * Display.update(), this patch replaces the one virtual advance(float,input)
 * call in traverse() with a small static method injected into BaseGameState.
 * While Fixer is creating a campaign on its worker, that method suppresses only
 * the Title Screen State's background-combat advance. render(), fader.advance(),
 * and Display.update() continue, so CheerpJ/browser event processing stays live
 * without allowing TitleScreenState's CombatEngine to race CampaignGameManager.
 */
public final class PatchBaseGameStateTransition {
    private static final String TARGET = "com/fs/starfarer/BaseGameState.class";
    private static final String BASE = "com/fs/starfarer/BaseGameState";
    private static final String INPUT = "com/fs/starfarer/util/super/B";
    private static final String ADVANCE_DESC = "(FLcom/fs/starfarer/util/super/B;)V";
    private static final String GUARDED_ADVANCE = "cheerpj$guardedAdvance";
    private static final String GUARDED_ADVANCE_DESC =
            "(Lcom/fs/starfarer/BaseGameState;FLcom/fs/starfarer/util/super/B;)V";
    private static final String CREATE_PROPERTY = "starsector.campaignCreateInProgress";
    private static final String TITLE_ID = "Title Screen State";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchBaseGameStateTransition input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] drains = new int[] {0};
        int[] advanceGuards = new int[] {0};
        int[] classes = new int[] {0};
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
                    bytes = patch(bytes, drains, advanceGuards);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || drains[0] < 1 || advanceGuards[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "BaseGameState patch incomplete: classes="
                            + classes[0]
                            + " drains="
                            + drains[0]
                            + " advanceGuards="
                            + advanceGuards[0]);
        }
        System.out.println(
                "Patched BaseGameState render-thread drains="
                        + drains[0]
                        + " title-advance guards="
                        + advanceGuards[0]);
    }

    private static byte[] patch(byte[] input, int[] drains, int[] advanceGuards) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new SafeClassWriter(
                reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            private boolean helperExists;

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (GUARDED_ADVANCE.equals(name) && GUARDED_ADVANCE_DESC.equals(descriptor)) {
                    helperExists = true;
                }
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"traverse".equals(name) || !"()Ljava/lang/String;".equals(descriptor)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && BASE.equals(owner)
                                && "advance".equals(methodName)
                                && ADVANCE_DESC.equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    BASE,
                                    GUARDED_ADVANCE,
                                    GUARDED_ADVANCE_DESC,
                                    false);
                            advanceGuards[0]++;
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC
                                && "org/lwjgl/opengl/Display".equals(owner)
                                && "update".equals(methodName)) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/fs/starfarer/MainThreadTransitionBridge",
                                    "drain",
                                    "(Ljava/lang/Object;)V",
                                    false);
                            drains[0]++;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (!helperExists) {
                    emitGuardedAdvance(super.visitMethod(
                            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                            GUARDED_ADVANCE,
                            GUARDED_ADVANCE_DESC,
                            null,
                            null));
                }
                super.visitEnd();
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static void emitGuardedAdvance(MethodVisitor mv) {
        mv.visitCode();
        Label invokeAdvance = new Label();

        // Fast path: when no campaign create is in progress, preserve the exact
        // original virtual advance() call.
        mv.visitLdcInsn(CREATE_PROPERTY);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Boolean",
                "getBoolean",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, invokeAdvance);

        // During campaign creation, suppress only the active title state. This
        // uses the stable state ID instead of the title implementation's
        // physical/obfuscated class name.
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                BASE,
                "getID",
                "()Ljava/lang/String;",
                false);
        mv.visitLdcInsn(TITLE_ID);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "equals",
                "(Ljava/lang/Object;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, invokeAdvance);
        mv.visitInsn(Opcodes.RETURN);

        mv.visitLabel(invokeAdvance);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.FLOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                BASE,
                "advance",
                ADVANCE_DESC,
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(ClassReader reader, int flags) {
            super(reader, flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
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
