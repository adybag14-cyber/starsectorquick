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

/** Replaces Rules' quadratic per-trigger duplicate scan with a browser-only O(1) guard. */
public final class PatchRulesDuplicateIndex {
    private static final String ENTRY = "com/fs/starfarer/campaign/rules/Rules.class";
    private static final String RULES = "com/fs/starfarer/campaign/rules/Rules";
    private static final String RULE = "com/fs/starfarer/campaign/rules/ooOO";
    private static final String HELPER = "com/fs/starfarer/campaign/rules/BrowserRuleDuplicateIndex";
    private static final String METHOD = "super";
    private static final String DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    private static final String LIST_FOR_TRIGGER_DESC = "(Ljava/lang/String;)Ljava/util/List;";

    private PatchRulesDuplicateIndex() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchRulesDuplicateIndex input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
        int[] replaced = {0};
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
                    bytes = patch(bytes, methods, replaced);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1 || replaced[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "Rules duplicate-index patch incomplete classes=" + classes[0]
                            + " methods=" + methods[0] + " replaced=" + replaced[0]);
        }
        System.out.println("Patched Rules duplicate index methods=" + methods[0] + " replaced=" + replaced[0]);
    }

    private static byte[] patch(byte[] input, int[] methods, int[] replaced) {
        int existing = countHelperCalls(input);
        if (existing > 0) {
            if (existing != 4) throw new IllegalStateException("partial Rules duplicate-index patch helperCalls=" + existing);
            methods[0]++;
            replaced[0]++;
            System.out.println("Rules duplicate index already present; leaving bytecode unchanged.");
            return input;
        }
        int listCalls = countListForTriggerCalls(input);
        if (listCalls != 2) {
            throw new IllegalStateException("unexpected Rules trigger-list calls=" + listCalls);
        }

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!METHOD.equals(name) || !DESC.equals(descriptor)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    int triggerListCalls;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "begin", "()V", false);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && RULES.equals(owner)
                                && METHOD.equals(methodName)
                                && LIST_FOR_TRIGGER_DESC.equals(methodDescriptor)) {
                            triggerListCalls++;
                            if (triggerListCalls == 1) {
                                // The original ALOAD 8 (trigger) is already on the stack.
                                // Drop it, choose the fast duplicate guard or restore the exact
                                // stock Rules.super(trigger) list for the fallback path, then
                                // rejoin with a List on the stack for the untouched iterator code.
                                Label stock = new Label();
                                Label joined = new Label();
                                super.visitInsn(Opcodes.POP);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "enabled", "()Z", false);
                                super.visitJumpInsn(Opcodes.IFEQ, stock);
                                super.visitVarInsn(Opcodes.ALOAD, 8);
                                super.visitVarInsn(Opcodes.ALOAD, 10);
                                super.visitMethodInsn(
                                        Opcodes.INVOKEVIRTUAL,
                                        RULE,
                                        "getId",
                                        "()Ljava/lang/String;",
                                        false);
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        HELPER,
                                        "checkAndRecord",
                                        "(Ljava/lang/String;Ljava/lang/String;)V",
                                        false);
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "java/util/Collections",
                                        "emptyList",
                                        "()Ljava/util/List;",
                                        false);
                                super.visitJumpInsn(Opcodes.GOTO, joined);
                                super.visitLabel(stock);
                                super.visitVarInsn(Opcodes.ALOAD, 8);
                                super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                                super.visitLabel(joined);
                                replaced[0]++;
                                return;
                            }
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "finish", "()V", false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static int countHelperCalls(byte[] input) {
        int[] calls = {0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                if (!METHOD.equals(name) || !DESC.equals(desc)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && HELPER.equals(owner)
                                && ("begin".equals(method) || "enabled".equals(method)
                                    || "checkAndRecord".equals(method) || "finish".equals(method))) calls[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return calls[0];
    }

    private static int countListForTriggerCalls(byte[] input) {
        int[] calls = {0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                if (!METHOD.equals(name) || !DESC.equals(desc)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && RULES.equals(owner)
                                && METHOD.equals(method) && LIST_FOR_TRIGGER_DESC.equals(descriptor)) calls[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return calls[0];
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(ClassReader reader, int flags) { super(reader, flags); }
        @Override protected String getCommonSuperClass(String type1, String type2) { return "java/lang/Object"; }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
