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

/**
 * Removes writes to Rules' variable-usage diagnostics after the warning-only tail
 * has already been removed. Token traversal/parsing stays untouched in this patch.
 */
public final class PatchRulesDeadVariableWrites {
    private static final String ENTRY="com/fs/starfarer/campaign/rules/Rules.class";
    private static final String METHOD="super";
    private static final String DESC="(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    private static final String COUNTING_MAP="com/fs/starfarer/api/util/CountingMap";
    private static final String MAP="java/util/Map";

    private PatchRulesDeadVariableWrites() {}

    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchRulesDeadVariableWrites input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]);int[] classes={0},methods={0},removedAdds={0},removedPuts={0};
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();while(es.hasMoreElements()){
                JarEntry e=es.nextElement();JarEntry c=new JarEntry(e.getName());c.setTime(e.getTime());out.putNextEntry(c);
                byte[] bytes;try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}
                if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,methods,removedAdds,removedPuts);}
                out.write(bytes);out.closeEntry();
            }
        }
        if(classes[0]!=1||methods[0]!=1||removedAdds[0]!=6||removedPuts[0]!=6){Files.deleteIfExists(output);throw new IllegalStateException("Rules dead-write patch incomplete classes="+classes[0]+" methods="+methods[0]+" adds="+removedAdds[0]+" puts="+removedPuts[0]);}
        System.out.println("Patched Rules dead variable writes adds="+removedAdds[0]+" puts="+removedPuts[0]);
    }

    private static byte[] patch(byte[] input,int[] methods,int[] removedAdds,int[] removedPuts){
        Counts before=count(input);
        if(before.warningReads!=0||before.systemOut!=0)throw new IllegalStateException("Rules warning diagnostics still present; refusing dead-write patch warningReads="+before.warningReads+" systemOut="+before.systemOut);
        if(before.adds==0&&before.puts==0){methods[0]=1;removedAdds[0]=6;removedPuts[0]=6;System.out.println("Rules dead variable writes already removed; leaving bytecode unchanged.");return input;}
        if(before.adds!=6||before.puts!=6)throw new IllegalStateException("unexpected Rules variable-write surface adds="+before.adds+" puts="+before.puts);
        ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,0);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                MethodVisitor mv=super.visitMethod(access,name,descriptor,signature,exceptions);
                if(!METHOD.equals(name)||!DESC.equals(descriptor))return mv;methods[0]++;
                return new MethodVisitor(Opcodes.ASM9,mv){
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean itf){
                        if(opcode==Opcodes.INVOKEVIRTUAL&&COUNTING_MAP.equals(owner)&&"add".equals(name)&&"(Ljava/lang/Object;)V".equals(descriptor)){
                            // Existing stack: [CountingMap, key]. Both are category-1 references.
                            super.visitInsn(Opcodes.POP2);removedAdds[0]++;return;
                        }
                        if(opcode==Opcodes.INVOKEINTERFACE&&MAP.equals(owner)&&"put".equals(name)&&"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(descriptor)){
                            // Existing stack: [map, key, value]. Consume all three but preserve
                            // the original Object return shape because stock bytecode POPs it next.
                            super.visitInsn(Opcodes.POP2); // value + key
                            super.visitInsn(Opcodes.POP);  // map
                            super.visitInsn(Opcodes.ACONST_NULL);
                            removedPuts[0]++;return;
                        }
                        super.visitMethodInsn(opcode,owner,name,descriptor,itf);
                    }
                };
            }
        },0);
        byte[] out=w.toByteArray();Counts after=count(out);
        if(after.adds!=0||after.puts!=0||after.warningReads!=0||after.systemOut!=0)throw new IllegalStateException("post-patch Rules variable writes remain adds="+after.adds+" puts="+after.puts+" warningReads="+after.warningReads+" systemOut="+after.systemOut);
        return out;
    }

    private static Counts count(byte[] input){Counts c=new Counts();new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){
        @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
            if(!METHOD.equals(name)||!DESC.equals(descriptor))return null;c.methods++;
            return new MethodVisitor(Opcodes.ASM9){
                @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean itf){
                    if(opcode==Opcodes.INVOKEVIRTUAL&&COUNTING_MAP.equals(owner)&&"add".equals(name)&&"(Ljava/lang/Object;)V".equals(descriptor))c.adds++;
                    if(opcode==Opcodes.INVOKEINTERFACE&&MAP.equals(owner)&&"put".equals(name)&&"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(descriptor))c.puts++;
                    if(COUNTING_MAP.equals(owner)&&("keySet".equals(name)||"getCount".equals(name)))c.warningReads++;
                }
                @Override public void visitFieldInsn(int opcode,String owner,String name,String descriptor){if(opcode==Opcodes.GETSTATIC&&"java/lang/System".equals(owner)&&"out".equals(name))c.systemOut++;}
            };
        }
    },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);if(c.methods!=1)throw new IllegalStateException("Rules method count="+c.methods);return c;}
    private static final class Counts{int methods,adds,puts,warningReads,systemOut;}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
