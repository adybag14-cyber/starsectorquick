import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verify early predecode hooks while retaining the complete stock queue/spec/predecode path. */
public final class VerifyResourceLoaderEarlyPredecodePatch {
    private static final String HELPER = "com/fs/starfarer/BrowserDeferredTextureQueue";
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyResourceLoaderEarlyPredecodePatch patched.jar");
        int[] queueMethods={0}, initMethods={0}, early={0}, start={0}, ordinal={0}, stockAdds={0}, spec={0}, laterPredecode={0};
        int[] seq={0}, startAt={-1}, specAt={-1}, laterAt={-1};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry e = jar.getJarEntry("com/fs/starfarer/loading/ResourceLoaderState.class"); if (e == null) throw new AssertionError("missing ResourceLoaderState");
            try (InputStream in = jar.getInputStream(e)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex) {
                        final boolean queue = "queueResource".equals(n) && "(Lcom/fs/starfarer/loading/ResourceLoaderState$o;Ljava/lang/String;I)V".equals(d);
                        final boolean init = "init".equals(n) && "(Ljava/util/Map;)V".equals(d);
                        if (queue) queueMethods[0]++; if (init) initMethods[0]++;
                        if (!queue && !init) return null;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf) {
                                int i=++seq[0];
                                if (queue && op==Opcodes.INVOKEVIRTUAL && "com/fs/starfarer/loading/ResourceLoaderState$o".equals(owner) && "ordinal".equals(name)) ordinal[0]++;
                                if (queue && op==Opcodes.INVOKEINTERFACE && "java/util/List".equals(owner) && "add".equals(name)) stockAdds[0]++;
                                if (op==Opcodes.INVOKESTATIC && HELPER.equals(owner) && "queueEarlyImagePredecode".equals(name)) early[0]++;
                                if (init && op==Opcodes.INVOKESTATIC && HELPER.equals(owner) && "startEarlyImagePredecode".equals(name)) { start[0]++; if(startAt[0]<0) startAt[0]=i; }
                                if (init && op==Opcodes.INVOKESTATIC && "com/fs/starfarer/loading/SpecStore".equals(owner) && "public".equals(name)) { spec[0]++; if(specAt[0]<0) specAt[0]=i; }
                                if (init && op==Opcodes.INVOKESTATIC && HELPER.equals(owner) && "queueImagePredecode".equals(name)) { laterPredecode[0]++; if(laterAt[0]<0) laterAt[0]=i; }
                            }
                        };
                    }
                }, 0);
            }
        }
        if(queueMethods[0]!=1||initMethods[0]!=1||early[0]!=1||start[0]!=1||ordinal[0]<1||stockAdds[0]<1||spec[0]!=1||laterPredecode[0]!=1
                || startAt[0]<0||specAt[0]<0||laterAt[0]<0||startAt[0]>=specAt[0]||specAt[0]>=laterAt[0]) {
            throw new AssertionError("early predecode structure queueMethods="+queueMethods[0]+" initMethods="+initMethods[0]+" early="+early[0]+" start="+start[0]
                    +" ordinal="+ordinal[0]+" stockAdds="+stockAdds[0]+" spec="+spec[0]+" later="+laterPredecode[0]+" order="+startAt[0]+"/"+specAt[0]+"/"+laterAt[0]);
        }
        System.out.println("VerifyResourceLoaderEarlyPredecodePatch: OK early queue/start before stock spec and predecode path");
    }
}
