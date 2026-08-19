import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Proves direct skin manifest precedes and retains the complete stock fallback. */
public final class VerifyShipHullSkinDirectManifestPatch {
    private static final String ENTRY = "com/fs/starfarer/loading/ShipHullSpecLoader.class";
    private static final String CACHE = "com/fs/starfarer/loading/BrowserSpecCache";
    private static final String LOADING = "com/fs/starfarer/loading/LoadingUtils";
    private static final String ROOT = "data/hulls/skins";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyShipHullSkinDirectManifestPatch jar");
        int[] targetMethods={0}, direct={0}, fileLists={0}, dirLists={0}, loaders={0}, info={0};
        int[] callIndex={0}, firstDirect={0}, firstLoading={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            if (jar.getJarEntry("com/fs/starfarer/loading/BrowserSpecCache.class") == null) {
                throw new AssertionError("missing BrowserSpecCache helper");
            }
            JarEntry entry = jar.getJarEntry(ENTRY);
            if (entry == null) throw new AssertionError("missing ShipHullSpecLoader");
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]ex) {
                        if ((access & Opcodes.ACC_STATIC)==0 || !"()V".equals(desc)) return null;
                        return new MethodVisitor(Opcodes.ASM9) {
                            boolean skin;
                            @Override public void visitLdcInsn(Object value) {
                                if (ROOT.equals(value) && !skin) { skin=true; targetMethods[0]++; }
                            }
                            @Override public void visitMethodInsn(int op,String owner,String method,String d,boolean itf) {
                                int index=++callIndex[0];
                                if (op==Opcodes.INVOKESTATIC && CACHE.equals(owner)
                                        && "directSkinPathsOrNull".equals(method)) {
                                    direct[0]++; if(firstDirect[0]==0)firstDirect[0]=index;
                                }
                                if (!skin) return;
                                if (op==Opcodes.INVOKESTATIC && LOADING.equals(owner)) {
                                    if(firstLoading[0]==0)firstLoading[0]=index;
                                    if ("(Ljava/lang/String;Ljava/lang/String;)Ljava/util/List;".equals(d)) fileLists[0]++;
                                    if ("(Ljava/lang/String;)Ljava/util/List;".equals(d)) dirLists[0]++;
                                }
                                if (op==Opcodes.INVOKESTATIC
                                        && "com/fs/starfarer/loading/ShipHullSpecLoader".equals(owner)
                                        && "(Ljava/lang/String;)V".equals(d)) loaders[0]++;
                                if (op==Opcodes.INVOKEVIRTUAL && "org/apache/log4j/Logger".equals(owner)
                                        && "info".equals(method)) info[0]++;
                            }
                        };
                    }
                },0);
            }
        }
        boolean ordered = firstDirect[0] > 0 && firstLoading[0] > firstDirect[0];
        if (targetMethods[0] != 1 || direct[0] != 1 || fileLists[0] != 2 || dirLists[0] != 1
                || loaders[0] != 1 || info[0] != 1 || !ordered) {
            throw new AssertionError("skin direct structure methods="+targetMethods[0]+" direct="+direct[0]
                    +" fileLists="+fileLists[0]+" dirLists="+dirLists[0]+" loaders="+loaders[0]
                    +" info="+info[0]+" firstDirect="+firstDirect[0]+" firstLoading="+firstLoading[0]);
        }
        System.out.println("VerifyShipHullSkinDirectManifestPatch: OK direct branch precedes complete skin discovery fallback");
    }
}
