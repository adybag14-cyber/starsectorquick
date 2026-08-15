import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies font lookup remains intact and the trace runs before the stock map lookup. */
public final class VerifyFontUsageTracePatch {
    private static final String HELPER="com/fs/starfarer/BrowserFontUsageTrace";
    private static final String DESC="(Ljava/lang/String;)Lcom/fs/graphics/super/return;";
    public static void main(String[] args)throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("usage: VerifyFontUsageTracePatch patched.jar");
        int[] methods={0},helper={0},mapGet={0},seq={0},helperAt={-1},getAt={-1};
        try(JarFile jar=new JarFile(Path.of(args[0]).toFile())) {
            JarEntry e=jar.getJarEntry("com/fs/graphics/super/D.class");
            if(e==null) throw new AssertionError("missing font registry D.class");
            try(InputStream in=jar.getInputStream(e)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){
                    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
                        if((access & Opcodes.ACC_STATIC)==0 || !DESC.equals(desc)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9){
                            @Override public void visitMethodInsn(int op,String owner,String method,String d,boolean itf){
                                int i=++seq[0];
                                if(op==Opcodes.INVOKESTATIC && HELPER.equals(owner) && "record".equals(method)){helper[0]++;if(helperAt[0]<0)helperAt[0]=i;}
                                if(op==Opcodes.INVOKEVIRTUAL && "java/util/HashMap".equals(owner) && "get".equals(method)){mapGet[0]++;if(getAt[0]<0)getAt[0]=i;}
                            }
                        };
                    }
                },0);
            }
        }
        if(methods[0]!=1 || helper[0]!=1 || mapGet[0]!=1 || helperAt[0]<0 || getAt[0]<0 || helperAt[0]>=getAt[0])
            throw new AssertionError("font trace structure mismatch methods="+methods[0]+" helper="+helper[0]+" mapGet="+mapGet[0]+" helperAt="+helperAt[0]+" getAt="+getAt[0]);
        System.out.println("VerifyFontUsageTracePatch: OK trace-before-map, stock lookup retained");
    }
}
