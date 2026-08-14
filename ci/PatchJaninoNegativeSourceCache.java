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

/** Memoizes failed Janino source lookups per browser ResourceFinder instance. */
public final class PatchJaninoNegativeSourceCache {
    private static final String TARGET = "com/fs/starfarer/loading/ooOo.class";
    private static final String TARGET_NAME = "com/fs/starfarer/loading/ooOo";
    private static final String INNER_RESOURCE = "com/fs/starfarer/loading/ooOo$1";
    private static final String LOADING_UTILS = "com/fs/starfarer/loading/LoadingUtils";
    private static final String SOURCE_READER = "\u00d500000";
    private static final String FIELD = "cheerpj$negativeSources";
    private static final String FIELD_DESC = "Ljava/util/Set;";
    private static final String PROPERTY = "starsector.browserJaninoNegativeCache";
    private static final String FIND = "findResource";
    private static final String FIND_DESC = "(Ljava/lang/String;)Lorg/codehaus/janino/util/resource/Resource;";

    private PatchJaninoNegativeSourceCache() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchJaninoNegativeSourceCache input.jar output.jar");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int[] classes = {0};
        int[] methods = {0};
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
                    bytes = patch(bytes, methods);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0] != 1 || methods[0] != 1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                    "Janino negative-source patch incomplete classes=" + classes[0] + " methods=" + methods[0]);
        }
        System.out.println("Patched Janino per-finder negative source cache methods=" + methods[0]);
    }

    private static byte[] patch(byte[] input, int[] methods) {
        int existing = countField(input);
        if (existing > 1) {
            throw new IllegalStateException("duplicate Janino negative-source fields=" + existing);
        }
        if (existing == 1) {
            methods[0]++;
            System.out.println("Janino negative source cache already present; leaving bytecode unchanged.");
            return input;
        }

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visitEnd() {
                super.visitField(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                        FIELD,
                        FIELD_DESC,
                        "Ljava/util/Set<Ljava/lang/String;>;",
                        null).visitEnd();
                super.visitEnd();
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if ("<init>".equals(name) && "()V".equals(descriptor)) {
                    MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, delegate) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) {
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitTypeInsn(Opcodes.NEW, "java/util/HashSet");
                                super.visitInsn(Opcodes.DUP);
                                super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false);
                                super.visitFieldInsn(Opcodes.PUTFIELD, TARGET_NAME, FIELD, FIELD_DESC);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                if (!FIND.equals(name) || !FIND_DESC.equals(descriptor)) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                methods[0]++;
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                emitFindResource(mv);
                return null;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitFindResource(MethodVisitor mv) {
        Label doLookup = new Label();
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label handler = new Label();
        Label noRemember = new Label();
        Label doneRemember = new Label();
        mv.visitTryCatchBlock(tryStart, tryEnd, handler, "java/lang/Exception");
        mv.visitCode();

        mv.visitLdcInsn(PROPERTY);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "getBoolean", "(Ljava/lang/String;)Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, doLookup);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, TARGET_NAME, FIELD, FIELD_DESC);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Set", "contains", "(Ljava/lang/Object;)Z", true);
        mv.visitJumpInsn(Opcodes.IFEQ, doLookup);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitLabel(doLookup);
        mv.visitLabel(tryStart);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                LOADING_UTILS,
                SOURCE_READER,
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitTypeInsn(Opcodes.NEW, INNER_RESOURCE);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                INNER_RESOURCE,
                "<init>",
                "(Lcom/fs/starfarer/loading/ooOo;Ljava/lang/String;Ljava/lang/String;)V",
                false);
        mv.visitLabel(tryEnd);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitLabel(handler);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitLdcInsn(PROPERTY);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "getBoolean", "(Ljava/lang/String;)Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, doneRemember);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, TARGET_NAME, FIELD, FIELD_DESC);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);
        mv.visitJumpInsn(Opcodes.IFEQ, noRemember);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("BrowserJaninoNegativeCache: remember path=");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        mv.visitLabel(noRemember);
        mv.visitLabel(doneRemember);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static int countField(byte[] input) {
        int[] matches = {0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                               String signature, Object value) {
                if (FIELD.equals(name) && FIELD_DESC.equals(descriptor)) matches[0]++;
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return matches[0];
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
