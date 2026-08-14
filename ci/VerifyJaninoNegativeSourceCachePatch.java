import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Structural verifier for the browser-only Janino negative-source memo. */
public final class VerifyJaninoNegativeSourceCachePatch {
    private static final String ENTRY = "com/fs/starfarer/loading/ooOo.class";
    private static final String TARGET = "com/fs/starfarer/loading/ooOo";
    private static final String FIELD = "cheerpj$negativeSources";
    private static final String PROPERTY = "starsector.browserJaninoNegativeCache";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyJaninoNegativeSourceCachePatch jar");
        int[] fields={0}, ctorStores={0}, findMethods={0}, sourceReads={0}, contains={0}, adds={0}, props={0}, resources={0}, catches={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile()); InputStream in = jar.getInputStream(jar.getJarEntry(ENTRY))) {
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value) {
                    if (FIELD.equals(name) && "Ljava/util/Set;".equals(desc)) fields[0]++;
                    return null;
                }
                @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                    final boolean ctor="<init>".equals(name)&&"()V".equals(desc);
                    final boolean find="findResource".equals(name)&&"(Ljava/lang/String;)Lorg/codehaus/janino/util/resource/Resource;".equals(desc);
                    if (find) findMethods[0]++;
                    if (!ctor && !find) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitFieldInsn(int op,String owner,String field,String descriptor) {
                            if (ctor && op==Opcodes.PUTFIELD && TARGET.equals(owner) && FIELD.equals(field)) ctorStores[0]++;
                        }
                        @Override public void visitLdcInsn(Object value) {
                            if (find && PROPERTY.equals(value)) props[0]++;
                        }
                        @Override public void visitMethodInsn(int op,String owner,String method,String descriptor,boolean itf) {
                            if (!find) return;
                            if ("com/fs/starfarer/loading/LoadingUtils".equals(owner)
                                    && "\u00d500000".equals(method)
                                    && "(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) sourceReads[0]++;
                            if ("java/util/Set".equals(owner) && "contains".equals(method)) contains[0]++;
                            if ("java/util/Set".equals(owner) && "add".equals(method)) adds[0]++;
                            if ("com/fs/starfarer/loading/ooOo$1".equals(owner) && "<init>".equals(method)) resources[0]++;
                        }
                        @Override public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end,
                                                                  org.objectweb.asm.Label handler, String type) {
                            if (find && "java/lang/Exception".equals(type)) catches[0]++;
                        }
                    };
                }
            }, 0);
        }
        if(fields[0]!=1||ctorStores[0]!=1||findMethods[0]!=1||sourceReads[0]!=1||contains[0]!=1||adds[0]!=1||props[0]!=2||resources[0]!=1||catches[0]!=1) {
            throw new AssertionError("unexpected patch shape fields="+fields[0]+" ctorStores="+ctorStores[0]
                    +" find="+findMethods[0]+" sourceReads="+sourceReads[0]+" contains="+contains[0]
                    +" adds="+adds[0]+" props="+props[0]+" resources="+resources[0]+" catches="+catches[0]);
        }
        System.out.println("VerifyJaninoNegativeSourceCachePatch: OK");
    }
}
