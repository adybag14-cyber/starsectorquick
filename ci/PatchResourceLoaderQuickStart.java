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
 * the browser quick-start path, and emits coarse loader-stage timing markers.
 *
 * The stock loader still performs settings/spec/script/plugin initialization and
 * keeps startup-critical UI/font/portrait/cursor/ship/terrain/FX resources. In
 * quick-start mode it also defers a small set of combat/war-room-only static
 * categories that are not needed before the first campaign frame. Property-off
 * behavior remains byte-for-byte equivalent to the original loader body.
 */
public final class PatchResourceLoaderQuickStart {
    private static final String TARGET = "com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String QUICK_METHOD = "queueShipAndWeaponSprites";
    private static final String QUICK_DESC = "()V";
    private static final String QUEUE_METHOD = "queueResource";
    private static final String QUEUE_DESC =
            "(Lcom/fs/starfarer/loading/ResourceLoaderState$o;Ljava/lang/String;I)V";
    private static final String[] DEFERRED_STATIC_PREFIXES = {
            "graphics/damage/",
            "graphics/debris/",
            "graphics/asteroids/",
            "graphics/warroom/"
    };
    private static final String INIT_METHOD = "init";
    private static final String INIT_DESC = "(Ljava/util/Map;)V";
    private static final String PROPERTY = "starsector.browserQuickResourceLoad";
    private static final int EXPECTED_STAGE_MARKERS = 9;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchResourceLoaderQuickStart input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = new int[] {0};
        int[] quickMethods = new int[] {0};
        int[] queueMethods = new int[] {0};
        int[] initMethods = new int[] {0};
        int[] stageMarkers = new int[] {0};

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
                    bytes = patch(bytes, quickMethods, queueMethods, initMethods, stageMarkers);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }

        if (classes[0] != 1 || quickMethods[0] != 1 || queueMethods[0] != 1
                || initMethods[0] != 1 || stageMarkers[0] != EXPECTED_STAGE_MARKERS) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "ResourceLoaderState quick-start patch incomplete: classes=" + classes[0]
                            + " quickMethods=" + quickMethods[0]
                            + " queueMethods=" + queueMethods[0]
                            + " initMethods=" + initMethods[0]
                            + " stageMarkers=" + stageMarkers[0]);
        }
        System.out.println(
                "Patched ResourceLoaderState quick-start guards=" + quickMethods[0]
                        + " static-queue guards=" + queueMethods[0]
                        + " deferred-prefixes=" + DEFERRED_STATIC_PREFIXES.length
                        + " loader-stage markers=" + stageMarkers[0]);
    }

    private static byte[] patch(byte[] input, int[] quickMethods, int[] queueMethods,
                                int[] initMethods, int[] stageMarkers) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (QUICK_METHOD.equals(name) && QUICK_DESC.equals(descriptor)) {
                    quickMethods[0]++;
                    return quickGuard(delegate);
                }
                if (QUEUE_METHOD.equals(name) && QUEUE_DESC.equals(descriptor)) {
                    queueMethods[0]++;
                    return staticQueueGuard(delegate);
                }
                if (INIT_METHOD.equals(name) && INIT_DESC.equals(descriptor)) {
                    initMethods[0]++;
                    return initStages(delegate, stageMarkers);
                }
                return delegate;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static MethodVisitor quickGuard(MethodVisitor delegate) {
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
                emitPrint(super.mv,
                        "BrowserResourceLoader: deferred eager ship/weapon/projectile sprite preload.");
                super.visitInsn(Opcodes.RETURN);
                super.visitLabel(stockPath);
                super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
        };
    }

    private static MethodVisitor staticQueueGuard(MethodVisitor delegate) {
        return new MethodVisitor(Opcodes.ASM9, delegate) {
            @Override
            public void visitCode() {
                super.visitCode();
                Label stockPath = new Label();
                Label defer = new Label();

                super.visitLdcInsn(PROPERTY);
                super.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Boolean",
                        "getBoolean",
                        "(Ljava/lang/String;)Z",
                        false);
                super.visitJumpInsn(Opcodes.IFEQ, stockPath);

                // local 2 is the resource path in queueResource(type, path, weight).
                super.visitVarInsn(Opcodes.ALOAD, 2);
                super.visitJumpInsn(Opcodes.IFNULL, stockPath);
                for (String prefix : DEFERRED_STATIC_PREFIXES) {
                    super.visitVarInsn(Opcodes.ALOAD, 2);
                    super.visitLdcInsn(prefix);
                    super.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/String",
                            "startsWith",
                            "(Ljava/lang/String;)Z",
                            false);
                    super.visitJumpInsn(Opcodes.IFNE, defer);
                }
                super.visitJumpInsn(Opcodes.GOTO, stockPath);

                super.visitLabel(defer);
                super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                super.visitInsn(Opcodes.RETURN);

                super.visitLabel(stockPath);
                super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
        };
    }

    private static MethodVisitor initStages(MethodVisitor delegate, int[] stageMarkers) {
        return new MethodVisitor(Opcodes.ASM9, delegate) {
            @Override
            public void visitCode() {
                super.visitCode();
                emitStage(this.mv, "init-start", stageMarkers);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String methodName,
                                        String methodDescriptor, boolean isInterface) {
                if (opcode == Opcodes.INVOKESTATIC
                        && "java/util/concurrent/Executors".equals(owner)
                        && "newFixedThreadPool".equals(methodName)) {
                    emitStage(this.mv, "queued-resource-load-start", stageMarkers);
                }
                if (opcode == Opcodes.INVOKESTATIC
                        && "com/fs/graphics/L".equals(owner)
                        && "new".equals(methodName)
                        && "()V".equals(methodDescriptor)) {
                    emitStage(this.mv, "resource-queue-loop-complete", stageMarkers);
                }

                super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);

                if (opcode == Opcodes.INVOKESTATIC
                        && "com/fs/starfarer/settings/StarfarerSettings".equals(owner)
                        && "super".equals(methodName)
                        && "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V".equals(methodDescriptor)) {
                    emitStage(this.mv, "settings-resource-queue-ready", stageMarkers);
                } else if (opcode == Opcodes.INVOKESTATIC
                        && "com/fs/starfarer/loading/SpecStore".equals(owner)
                        && "public".equals(methodName)
                        && "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V".equals(methodDescriptor)) {
                    emitStage(this.mv, "spec-store-ready", stageMarkers);
                } else if (opcode == Opcodes.INVOKEVIRTUAL
                        && "com/fs/starfarer/loading/ResourceLoaderState".equals(owner)
                        && QUICK_METHOD.equals(methodName)
                        && QUICK_DESC.equals(methodDescriptor)) {
                    emitStage(this.mv, "dynamic-sprite-pass-ready", stageMarkers);
                } else if (opcode == Opcodes.INVOKESTATIC
                        && "com/fs/graphics/L".equals(owner)
                        && "o00000".equals(methodName)
                        && "()V".equals(methodDescriptor)) {
                    emitStage(this.mv, "resource-predecode-ready", stageMarkers);
                } else if (opcode == Opcodes.INVOKESTATIC
                        && "com/fs/graphics/L".equals(owner)
                        && "new".equals(methodName)
                        && "()V".equals(methodDescriptor)) {
                    emitStage(this.mv, "graphics-finalize-ready", stageMarkers);
                } else if (opcode == Opcodes.INVOKESTATIC
                        && "com/fs/starfarer/api/impl/campaign/velfield/SlipstreamManager".equals(owner)
                        && "validateConfigs".equals(methodName)
                        && "()V".equals(methodDescriptor)) {
                    emitStage(this.mv, "finalizers-ready", stageMarkers);
                }
            }
        };
    }

    private static void emitStage(MethodVisitor mv, String stage, int[] stageMarkers) {
        emitPrint(mv, "BrowserResourceLoaderStage: " + stage);
        stageMarkers[0]++;
    }

    private static void emitPrint(MethodVisitor mv, String text) {
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;");
        mv.visitLdcInsn(text);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
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
