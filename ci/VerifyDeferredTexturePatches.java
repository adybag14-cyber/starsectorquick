import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Proves deferred texture routing and lazy lookup ordering in final runtime JARs. */
public final class VerifyDeferredTexturePatches {
    private static final String HELPER="com/fs/starfarer/BrowserDeferredTextureQueue";
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: VerifyDeferredTexturePatches starfarer_obf.jar fs.common_obf.jar");
        verifyResourceLoader(Path.of(args[0])); verifyRegistry(Path.of(args[1]));
        System.out.println("VerifyDeferredTexturePatches: OK resource helpers=2 direct=5 lazy-before-map=true");
    }
    private static void verifyResourceLoader(Path jarPath)throws Exception{
        int[] methods={0},helper={0},direct={0};List<String> order=new ArrayList<String>();
        try(JarFile jar=new JarFile(jarPath.toFile())){JarEntry e=jar.getJarEntry("com/fs/starfarer/loading/ResourceLoaderState.class");if(e==null)throw new AssertionError("missing ResourceLoaderState");try(InputStream in=jar.getInputStream(e)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){if(!"init".equals(n)||!"(Ljava/util/Map;)V".equals(d))return null;methods[0]++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(op==Opcodes.INVOKESTATIC&&"(Ljava/lang/String;Ljava/lang/String;)V".equals(desc)){if("com/fs/graphics/oOoO".equals(owner)&&"o00000".equals(name)){direct[0]++;order.add("direct");}if(HELPER.equals(owner)&&"loadOrDefer".equals(name)){helper[0]++;order.add("helper");}}}};}},0);}}
        if(methods[0]!=1||helper[0]!=2||direct[0]!=5)throw new AssertionError("ResourceLoader deferred patch mismatch methods="+methods[0]+" helper="+helper[0]+" direct="+direct[0]+" order="+order);
        List<String> expected=java.util.Arrays.asList("direct","direct","direct","direct","helper","direct","helper");
        if(!order.equals(expected))throw new AssertionError("ResourceLoader image-call order mismatch actual="+order+" expected="+expected);
    }
    private static void verifyRegistry(Path jarPath)throws Exception{
        int[] methods={0},helper={0},contains={0},seq={0},helperAt={-1},containsAt={-1};
        try(JarFile jar=new JarFile(jarPath.toFile())){JarEntry e=jar.getJarEntry("com/fs/graphics/oOoO.class");if(e==null)throw new AssertionError("missing oOoO");try(InputStream in=jar.getInputStream(e)){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){if(!"new".equals(n)||!"(Ljava/lang/String;)Lcom/fs/graphics/Object;".equals(d))return null;methods[0]++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){int i=++seq[0];if(op==Opcodes.INVOKESTATIC&&HELPER.equals(owner)&&"ensureLoaded".equals(name)){helper[0]++;if(helperAt[0]<0)helperAt[0]=i;}if("java/util/Map".equals(owner)&&"containsKey".equals(name)){contains[0]++;if(containsAt[0]<0)containsAt[0]=i;}}};}},0);}}
        if(methods[0]!=1||helper[0]!=1||contains[0]<1||helperAt[0]<0||containsAt[0]<0||helperAt[0]>=containsAt[0])throw new AssertionError("registry lazy patch mismatch methods="+methods[0]+" helper="+helper[0]+" contains="+contains[0]+" helperAt="+helperAt[0]+" containsAt="+containsAt[0]);
    }
}
