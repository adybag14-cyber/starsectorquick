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

/** Replaces AbstractReferenceMarshaller's PathTracker allocation with exact browser subclass. */
public final class PatchXStreamFastPathTracker {
    private static final String TARGET = "com/thoughtworks/xstream/core/AbstractReferenceMarshaller.class";
    private static final String CTOR_DESC = "(Lcom/thoughtworks/xstream/io/HierarchicalStreamWriter;Lcom/thoughtworks/xstream/converters/ConverterLookup;Lcom/thoughtworks/xstream/mapper/Mapper;)V";
    private static final String STOCK = "com/thoughtworks/xstream/io/path/PathTracker";
    private static final String FAST = "com/thoughtworks/xstream/io/path/BrowserFastPathTracker";

    private PatchXStreamFastPathTracker() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchXStreamFastPathTracker input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes={0}, methods={0}, news={0}, inits={0};
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
                    bytes = patch(bytes, methods, news, inits);
                }
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (classes[0]!=1 || methods[0]!=1 || news[0]!=1 || inits[0]!=1) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("fast PathTracker patch mismatch classes="+classes[0]
                    +" methods="+methods[0]+" news="+news[0]+" inits="+inits[0]);
        }
        System.out.println("Patched XStream PathTracker allocation methods=1 new=1 init=1");
    }

    private static byte[] patch(byte[] input, int[] methods, int[] news, int[] inits) {
        int existing = countNew(input, FAST);
        if (existing == 1) {
            methods[0]=news[0]=inits[0]=1;
            System.out.println("Fast PathTracker allocation already present; leaving bytecode unchanged.");
            return input;
        }
        if (existing != 0) throw new IllegalStateException("duplicate fast PathTracker allocations="+existing);
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                MethodVisitor delegate = super.visitMethod(access,name,desc,sig,ex);
                if (!"<init>".equals(name) || !CTOR_DESC.equals(desc)) return delegate;
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override public void visitTypeInsn(int opcode,String type) {
                        if (opcode==Opcodes.NEW && STOCK.equals(type)) {
                            super.visitTypeInsn(opcode,FAST); news[0]++; return;
                        }
                        super.visitTypeInsn(opcode,type);
                    }
                    @Override public void visitMethodInsn(int opcode,String owner,String n,String d,boolean itf) {
                        if (opcode==Opcodes.INVOKESPECIAL && STOCK.equals(owner)
                                && "<init>".equals(n) && "()V".equals(d)) {
                            super.visitMethodInsn(opcode,FAST,n,d,itf); inits[0]++; return;
                        }
                        super.visitMethodInsn(opcode,owner,n,d,itf);
                    }
                };
            }
        },0);
        return writer.toByteArray();
    }

    private static int countNew(byte[] bytes, String type) {
        int[] count={0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitTypeInsn(int opcode,String t) {
                        if (opcode==Opcodes.NEW && type.equals(t)) count[0]++;
                    }
                };
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        return count[0];
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int read;
        while ((read=in.read(buffer))>=0) out.write(buffer,0,read);
        return out.toByteArray();
    }
}
