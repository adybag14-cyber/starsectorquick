import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyEconomyStockpileRandomPatch {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyEconomyStockpileRandomPatch patched.jar");
        int[] methods={0}, news={0}, ctors={0}, acquire={0}, nextFloat={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry("com/fs/starfarer/campaign/econ/reach/MainWorkTask2.class");
            if (entry == null) throw new AssertionError("missing MainWorkTask2.class");
            try (InputStream in=jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                        if (!"updateStockpileAndPriceV2".equals(name)
                                || !"(Lcom/fs/starfarer/campaign/econ/Market;Lcom/fs/starfarer/loading/F;)V".equals(desc)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitTypeInsn(int opcode,String type) {
                                if(opcode==Opcodes.NEW && "java/util/Random".equals(type)) news[0]++;
                            }
                            @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf) {
                                if(opcode==Opcodes.INVOKESPECIAL && "java/util/Random".equals(owner) && "<init>".equals(name)) ctors[0]++;
                                if(opcode==Opcodes.INVOKESTATIC && "com/fs/starfarer/campaign/econ/reach/BrowserEconomyRandomCompat".equals(owner) && "acquire".equals(name)) acquire[0]++;
                                if(opcode==Opcodes.INVOKEVIRTUAL && "java/util/Random".equals(owner) && "nextFloat".equals(name)) nextFloat[0]++;
                            }
                        };
                    }
                },0);
            }
        }
        if(methods[0]!=1||news[0]!=0||ctors[0]!=0||acquire[0]!=1||nextFloat[0]!=2) {
            throw new AssertionError("stockpile Random patch mismatch methods="+methods[0]+" news="+news[0]+" ctors="+ctors[0]+" acquire="+acquire[0]+" nextFloat="+nextFloat[0]);
        }
        System.out.println("VerifyEconomyStockpileRandomPatch: OK");
    }
}
