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
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Removes only Rules' post-parse single-use-variable warning scan in browser builds. */
public final class PatchRulesVariableDiagnostics {
    private static final String ENTRY = "com/fs/starfarer/campaign/rules/Rules.class";
    private static final String METHOD = "super";
    private static final String DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchRulesVariableDiagnostics input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes={0}, methods={0}, originalArrayLists={0}, originalSystemOut={0}, removed={0};
        try (JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries=jar.entries();
            while(entries.hasMoreElements()) {
                JarEntry entry=entries.nextElement(); JarEntry copy=new JarEntry(entry.getName()); copy.setTime(entry.getTime()); out.putNextEntry(copy);
                byte[] bytes; try(InputStream in=jar.getInputStream(entry)){bytes=readAll(in);}
                if(ENTRY.equals(entry.getName())) { classes[0]++; bytes=patch(bytes,methods,originalArrayLists,originalSystemOut,removed); }
                out.write(bytes); out.closeEntry();
            }
        }
        boolean fresh=classes[0]==1&&methods[0]==1&&originalArrayLists[0]==2&&originalSystemOut[0]==1&&removed[0]==1;
        boolean already=classes[0]==1&&methods[0]==1&&originalArrayLists[0]==1&&originalSystemOut[0]==0&&removed[0]==0;
        if(!fresh&&!already){Files.deleteIfExists(output);throw new IllegalStateException("Rules diagnostics patch mismatch classes="+classes[0]+" methods="+methods[0]+" arrayLists="+originalArrayLists[0]+" systemOut="+originalSystemOut[0]+" removed="+removed[0]);}
        System.out.println("Patched Rules variable diagnostics arrayLists="+originalArrayLists[0]+" systemOut="+originalSystemOut[0]+" removed="+removed[0]);
    }

    private static byte[] patch(byte[] input,int[] methods,int[] arrayLists,int[] systemOut,int[] removed) {
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;methods[0]++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitTypeInsn(int op,String type){if(op==Opcodes.NEW&&"java/util/ArrayList".equals(type))arrayLists[0]++;}@Override public void visitFieldInsn(int op,String owner,String name,String desc){if(op==Opcodes.GETSTATIC&&"java/lang/System".equals(owner)&&"out".equals(name)&&"Ljava/io/PrintStream;".equals(desc))systemOut[0]++;}};}},0);
        if(arrayLists[0]==1&&systemOut[0]==0) return input;
        if(arrayLists[0]!=2||systemOut[0]!=1) throw new IllegalStateException("unexpected pre-patch Rules tail arrayLists="+arrayLists[0]+" systemOut="+systemOut[0]);

        ClassReader reader=new ClassReader(input); ClassWriter writer=new ClassWriter(reader,0);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                MethodVisitor mv=super.visitMethod(access,name,descriptor,signature,exceptions);
                if(!METHOD.equals(name)||!DESC.equals(descriptor)) return mv;
                return new MethodVisitor(Opcodes.ASM9,mv){
                    int arrayListNew; boolean skipping;
                    @Override public void visitTypeInsn(int op,String type){
                        if(skipping)return;
                        if(op==Opcodes.NEW&&"java/util/ArrayList".equals(type)&&++arrayListNew==2){super.visitInsn(Opcodes.RETURN);removed[0]++;skipping=true;return;}
                        super.visitTypeInsn(op,type);
                    }
                    @Override public void visitInsn(int op){if(!skipping)super.visitInsn(op);}
                    @Override public void visitIntInsn(int op,int operand){if(!skipping)super.visitIntInsn(op,operand);}
                    @Override public void visitVarInsn(int op,int var){if(!skipping)super.visitVarInsn(op,var);}
                    @Override public void visitFieldInsn(int op,String owner,String n,String d){if(!skipping)super.visitFieldInsn(op,owner,n,d);}
                    @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){if(!skipping)super.visitMethodInsn(op,owner,n,d,itf);}
                    @Override public void visitInvokeDynamicInsn(String n,String d,Handle b,Object... a){if(!skipping)super.visitInvokeDynamicInsn(n,d,b,a);}
                    @Override public void visitJumpInsn(int op,Label label){if(!skipping)super.visitJumpInsn(op,label);}
                    @Override public void visitLabel(Label label){if(!skipping)super.visitLabel(label);}
                    @Override public void visitLdcInsn(Object value){if(!skipping)super.visitLdcInsn(value);}
                    @Override public void visitIincInsn(int var,int inc){if(!skipping)super.visitIincInsn(var,inc);}
                    @Override public void visitTableSwitchInsn(int min,int max,Label dflt,Label... labels){if(!skipping)super.visitTableSwitchInsn(min,max,dflt,labels);}
                    @Override public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels){if(!skipping)super.visitLookupSwitchInsn(dflt,keys,labels);}
                    @Override public void visitMultiANewArrayInsn(String d,int dims){if(!skipping)super.visitMultiANewArrayInsn(d,dims);}
                    @Override public void visitFrame(int type,int nLocal,Object[] local,int nStack,Object[] stack){if(!skipping)super.visitFrame(type,nLocal,local,nStack,stack);}
                    @Override public void visitLineNumber(int line,Label start){if(!skipping)super.visitLineNumber(line,start);}
                };
            }
        },0);
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
