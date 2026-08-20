import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class VerifyShipSlotCoverSafeAverageColorPatch {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyShipSlotCoverSafeAverageColorPatch jar");
        int[] methods={0}, safe={0}, rawTexture={0};
        try (JarFile jar = new JarFile(Path.of(args[0]).toFile())) {
            JarEntry entry = jar.getJarEntry("com/fs/starfarer/combat/entities/Ship.class");
            if (entry == null) throw new AssertionError("missing Ship.class");
            try (InputStream in = jar.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex) {
                        if (!"renderSlotCovers".equals(n) || !"(Ljava/awt/Color;ZF)V".equals(d)) return null;
                        methods[0]++;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf) {
                                if (op==Opcodes.INVOKEVIRTUAL && "com/fs/graphics/Sprite".equals(owner)
                                        && "getAverageColor".equals(name) && "()Ljava/awt/Color;".equals(desc)) safe[0]++;
                                if (op==Opcodes.INVOKEVIRTUAL && "com/fs/graphics/Sprite".equals(owner)
                                        && "getTexture".equals(name) && "()Lcom/fs/graphics/Object;".equals(desc)) rawTexture[0]++;
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        if (methods[0]!=1 || safe[0]!=3 || rawTexture[0]!=0)
            throw new AssertionError("slot-cover safe-color mismatch methods="+methods[0]+" safe="+safe[0]+" rawTexture="+rawTexture[0]);
        System.out.println("VerifyShipSlotCoverSafeAverageColorPatch: OK safeCalls=3 rawTextureCalls=0");
    }
}
