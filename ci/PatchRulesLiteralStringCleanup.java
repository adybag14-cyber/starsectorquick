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

/**
 * Replaces Rules' regex engine use for fixed CR/LF cleanup with literal String.replace().
 * The real trailing-whitespace regex replaceAll("\\s+$", "") is intentionally untouched.
 */
public final class PatchRulesLiteralStringCleanup {
    private static final String ENTRY = "com/fs/starfarer/campaign/rules/Rules.class";
    private static final String METHOD = "super";
    private static final String DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    private static final String STRING = "java/lang/String";
    private static final String REPLACE_ALL_DESC = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
    private static final String REPLACE_DESC = "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;";

    private PatchRulesLiteralStringCleanup() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchRulesLiteralStringCleanup input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes={0}, methods={0}, replacements={0};
        try (JarFile jar=new JarFile(input.toFile()); JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> es=jar.entries();
            while(es.hasMoreElements()) {
                JarEntry e=es.nextElement(); JarEntry c=new JarEntry(e.getName()); c.setTime(e.getTime()); out.putNextEntry(c);
                byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}
                if(ENTRY.equals(e.getName())) { classes[0]++; bytes=patch(bytes,methods,replacements); }
                out.write(bytes); out.closeEntry();
            }
        }
        if(classes[0]!=1 || methods[0]!=1 || replacements[0]!=4) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("Rules literal cleanup incomplete classes="+classes[0]+" methods="+methods[0]+" replacements="+replacements[0]);
        }
        System.out.println("Patched Rules literal CR/LF cleanup replacements="+replacements[0]);
    }

    private static byte[] patch(byte[] input,int[] methods,int[] replacements) {
        Counts before=count(input);
        if(before.replaceAll==1 && before.regexCleanup==0 && before.literalCleanup==4) {
            methods[0]=1; replacements[0]=4;
            System.out.println("Rules literal cleanup already present; leaving bytecode unchanged.");
            return input;
        }
        if(before.replaceAll!=5 || before.regexCleanup!=4 || before.literalCleanup!=0) {
            throw new IllegalStateException("unexpected Rules cleanup surface replaceAll="+before.replaceAll+" regexCleanup="+before.regexCleanup+" literalCleanup="+before.literalCleanup);
        }
        ClassReader r=new ClassReader(input); ClassWriter w=new ClassWriter(r,0);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                MethodVisitor mv=super.visitMethod(access,name,descriptor,signature,exceptions);
                if(!METHOD.equals(name)||!DESC.equals(descriptor)) return mv;
                methods[0]++;
                return new CleanupVisitor(mv,replacements);
            }
        },0);
        byte[] output=w.toByteArray();
        Counts after=count(output);
        if(after.replaceAll!=1 || after.regexCleanup!=0 || after.literalCleanup!=4) {
            throw new IllegalStateException("post-patch Rules cleanup mismatch replaceAll="+after.replaceAll+" regexCleanup="+after.regexCleanup+" literalCleanup="+after.literalCleanup);
        }
        return output;
    }

    private static final class CleanupVisitor extends MethodVisitor {
        private final int[] replacements;
        private String pendingPattern;
        private String pendingReplacement;
        private boolean hasReplacement;
        CleanupVisitor(MethodVisitor mv,int[] replacements){super(Opcodes.ASM9,mv);this.replacements=replacements;}

        @Override public void visitLdcInsn(Object value) {
            if(pendingPattern!=null) {
                if(!hasReplacement && value instanceof String) {
                    pendingReplacement=(String)value; hasReplacement=true; return;
                }
                flushPending();
            }
            if(value instanceof String && isRegexCleanupPattern((String)value)) {
                pendingPattern=(String)value; pendingReplacement=null; hasReplacement=false; return;
            }
            super.visitLdcInsn(value);
        }

        @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean isInterface) {
            if(pendingPattern!=null && hasReplacement && opcode==Opcodes.INVOKEVIRTUAL
                    && STRING.equals(owner) && "replaceAll".equals(name) && REPLACE_ALL_DESC.equals(descriptor)) {
                super.visitLdcInsn("\\r".equals(pendingPattern) ? "\r" : "\n");
                super.visitLdcInsn(pendingReplacement);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,STRING,"replace",REPLACE_DESC,false);
                replacements[0]++;
                clearPending();
                return;
            }
            flushPending();
            super.visitMethodInsn(opcode,owner,name,descriptor,isInterface);
        }

        private void clearPending(){pendingPattern=null;pendingReplacement=null;hasReplacement=false;}
        private void flushPending(){
            if(pendingPattern==null)return;
            super.visitLdcInsn(pendingPattern);
            if(hasReplacement)super.visitLdcInsn(pendingReplacement);
            clearPending();
        }

        @Override public void visitInsn(int opcode){flushPending();super.visitInsn(opcode);}
        @Override public void visitIntInsn(int opcode,int operand){flushPending();super.visitIntInsn(opcode,operand);}
        @Override public void visitVarInsn(int opcode,int var){flushPending();super.visitVarInsn(opcode,var);}
        @Override public void visitTypeInsn(int opcode,String type){flushPending();super.visitTypeInsn(opcode,type);}
        @Override public void visitFieldInsn(int opcode,String owner,String name,String descriptor){flushPending();super.visitFieldInsn(opcode,owner,name,descriptor);}
        @Override public void visitInvokeDynamicInsn(String name,String descriptor,Handle handle,Object... args){flushPending();super.visitInvokeDynamicInsn(name,descriptor,handle,args);}
        @Override public void visitJumpInsn(int opcode,Label label){flushPending();super.visitJumpInsn(opcode,label);}
        @Override public void visitIincInsn(int var,int increment){flushPending();super.visitIincInsn(var,increment);}
        @Override public void visitTableSwitchInsn(int min,int max,Label dflt,Label... labels){flushPending();super.visitTableSwitchInsn(min,max,dflt,labels);}
        @Override public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels){flushPending();super.visitLookupSwitchInsn(dflt,keys,labels);}
        @Override public void visitMultiANewArrayInsn(String descriptor,int dims){flushPending();super.visitMultiANewArrayInsn(descriptor,dims);}
        @Override public void visitLabel(Label label){flushPending();super.visitLabel(label);}
        @Override public void visitFrame(int type,int nLocal,Object[] local,int nStack,Object[] stack){flushPending();super.visitFrame(type,nLocal,local,nStack,stack);}
        @Override public void visitLineNumber(int line,Label start){flushPending();super.visitLineNumber(line,start);}
        @Override public void visitEnd(){flushPending();super.visitEnd();}
    }

    private static Counts count(byte[] bytes) {
        Counts c=new Counts();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                if(!METHOD.equals(name)||!DESC.equals(descriptor)) return null;
                c.methods++;
                return new AdjacentCallScanner(c);
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        if(c.methods!=1) throw new IllegalStateException("Rules cleanup method count="+c.methods);
        return c;
    }

    static final class AdjacentCallScanner extends MethodVisitor {
        final Counts c; String prev; String pattern; String replacement; boolean prevString;
        AdjacentCallScanner(Counts c){super(Opcodes.ASM9);this.c=c;}
        void reset(){prev=null;pattern=null;replacement=null;prevString=false;}
        @Override public void visitLdcInsn(Object value){
            if(value instanceof String){String s=(String)value;if(prevString){pattern=prev;replacement=s;}else{pattern=null;replacement=null;}prev=s;prevString=true;}else reset();
        }
        @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean itf){
            if(opcode==Opcodes.INVOKEVIRTUAL && STRING.equals(owner)) {
                if("replaceAll".equals(name)&&REPLACE_ALL_DESC.equals(descriptor)) {
                    c.replaceAll++;
                    if(isRegexCleanupPattern(pattern) && isCleanupReplacement(pattern,replacement)) c.regexCleanup++;
                } else if("replace".equals(name)&&REPLACE_DESC.equals(descriptor)) {
                    if(isLiteralCleanupPattern(pattern) && isLiteralCleanupReplacement(pattern,replacement)) c.literalCleanup++;
                }
            }
            reset();
        }
        @Override public void visitInsn(int o){reset();}@Override public void visitIntInsn(int o,int v){reset();}
        @Override public void visitVarInsn(int o,int v){reset();}@Override public void visitTypeInsn(int o,String t){reset();}
        @Override public void visitFieldInsn(int o,String ow,String n,String d){reset();}@Override public void visitJumpInsn(int o,Label l){reset();}
        @Override public void visitIincInsn(int v,int i){reset();}@Override public void visitInvokeDynamicInsn(String n,String d,Handle h,Object...a){reset();}
    }

    private static boolean isRegexCleanupPattern(String s){return "\\r".equals(s)||"\\n".equals(s);}
    private static boolean isCleanupReplacement(String pattern,String replacement){return "\\r".equals(pattern)?"".equals(replacement):" ".equals(replacement);}
    private static boolean isLiteralCleanupPattern(String s){return "\r".equals(s)||"\n".equals(s);}
    private static boolean isLiteralCleanupReplacement(String pattern,String replacement){return "\r".equals(pattern)?"".equals(replacement):" ".equals(replacement);}
    private static final class Counts {int methods,replaceAll,regexCleanup,literalCleanup;}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
