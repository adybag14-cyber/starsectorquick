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
 * Bypasses hull-skin root/child directory discovery when BrowserSpecCache has the
 * complete stock skin manifest. Null preserves the untouched stock path.
 */
public final class PatchShipHullSkinDirectManifest {
    private static final String ENTRY = "com/fs/starfarer/loading/ShipHullSpecLoader.class";
    private static final String CACHE = "com/fs/starfarer/loading/BrowserSpecCache";
    private static final String ROOT = "data/hulls/skins";
    private static final String LOGGER_DESC = "Lorg/apache/log4j/Logger;";
    private static final String DIRECT_DESC = "()Ljava/util/List;";

    private PatchShipHullSkinDirectManifest() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchShipHullSkinDirectManifest input.jar output.jar");
        }
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes = {0}, methods = {0}, directCalls = {0}, targets = {0};
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
                    bytes = patch(bytes, methods, directCalls, targets);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || directCalls[0] != 1 || targets[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "skin direct manifest patch mismatch classes=" + classes[0]
                            + " methods=" + methods[0]
                            + " directCalls=" + directCalls[0]
                            + " targets=" + targets[0]);
        }
        System.out.println("Patched hull skin direct manifest fast path calls=1 target=1");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] directCalls, int[] targets) {
        int existing = countDirect(input);
        if (existing == 1) {
            methods[0] = directCalls[0] = targets[0] = 1;
            System.out.println("Hull skin direct manifest fast path already present; leaving bytecode unchanged.");
            return input;
        }
        if (existing != 0) throw new IllegalStateException("duplicate hull skin direct calls=" + existing);

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ((access & Opcodes.ACC_STATIC) == 0 || !"()V".equals(desc)) return mv;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    boolean targetMethod;
                    boolean injected;
                    boolean placedTarget;
                    int skinFileListCalls;
                    boolean childFileListSeen;
                    final Label stockPath = new Label();
                    final Label directTarget = new Label();

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (!injected && ROOT.equals(value)) {
                            targetMethod = true;
                            injected = true;
                            methods[0]++;
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    CACHE,
                                    "directSkinPathsOrNull",
                                    DIRECT_DESC,
                                    false);
                            directCalls[0]++;
                            super.visitInsn(Opcodes.DUP);
                            super.visitJumpInsn(Opcodes.IFNULL, stockPath);
                            super.visitVarInsn(Opcodes.ASTORE, 0);
                            super.visitJumpInsn(Opcodes.GOTO, directTarget);
                            super.visitLabel(stockPath);
                            super.visitInsn(Opcodes.POP);
                        }
                        super.visitLdcInsn(value);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDesc, boolean isInterface) {
                        if (targetMethod
                                && opcode == Opcodes.INVOKESTATIC
                                && "com/fs/starfarer/loading/LoadingUtils".equals(owner)
                                && "(Ljava/lang/String;Ljava/lang/String;)Ljava/util/List;".equals(methodDesc)) {
                            skinFileListCalls++;
                            if (skinFileListCalls >= 2) childFileListSeen = true;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface);
                    }

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        // After the child-directory file-list call, the next ALOAD 0 is
                        // the start of the actual skin-list iterator setup. Jump there so
                        // the direct manifest still executes iterator()/next()/ASTORE path.
                        if (targetMethod && !placedTarget && childFileListSeen
                                && opcode == Opcodes.ALOAD && var == 0) {
                            super.visitLabel(directTarget);
                            placedTarget = true;
                            targets[0]++;
                        }
                        super.visitVarInsn(opcode, var);
                    }

                    @Override
                    public void visitEnd() {
                        if (targetMethod && !placedTarget) {
                            throw new IllegalStateException("hull skin load-loop logger target not found");
                        }
                        super.visitEnd();
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
        byte[] output = writer.toByteArray();
        if (countDirect(output) != 1) {
            throw new IllegalStateException("post-patch hull skin direct call count=" + countDirect(output));
        }
        return output;
    }

    private static int countDirect(byte[] bytes) {
        int[] count = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDesc, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && CACHE.equals(owner)
                                && "directSkinPathsOrNull".equals(methodName)
                                && DIRECT_DESC.equals(methodDesc)) {
                            count[0]++;
                        }
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
