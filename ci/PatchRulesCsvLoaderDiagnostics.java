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

/** Diagnostic-only phase instrumentation for LoadingUtils merged rules.csv loader. */
public final class PatchRulesCsvLoaderDiagnostics {
    private static final String ENTRY="com/fs/starfarer/loading/LoadingUtils.class";
    private static final String TARGET="com/fs/starfarer/loading/LoadingUtils";
    private static final String SPECSTORE="com/fs/starfarer/loading/SpecStore";
    private static final String PARSER="com/fs/starfarer/loading/oOoO";
    private static final String HELPER="com/fs/starfarer/loading/BrowserRulesCsvLoadDiag";
    private static final String METHOD="super";
    private static final String DESC="(Ljava/util/List;Ljava/lang/String;ZZ)Lorg/json/JSONArray;";
    private static final String READ_DESC="(Ljava/io/InputStream;)Ljava/lang/String;";
    private static final String PREPROCESS_DESC="(Ljava/lang/String;)Ljava/lang/String;";
    private static final String PARSE_DESC="(Ljava/lang/String;)Lorg/json/JSONArray;";

    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchRulesCsvLoaderDiagnostics input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]);Counts c=new Counts();
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){Enumeration<JarEntry> es=jar.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry copy=new JarEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);byte[] bytes;try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}if(ENTRY.equals(e.getName())){c.classes++;bytes=patch(bytes,c);}out.write(bytes);out.closeEntry();}}
        require(c);System.out.println("Patched rules CSV loader diagnostics discovery=1 read=1 preprocess=1 parse=1 copy=1 finish=1");
    }
    private static byte[] patch(byte[] input,Counts c){Counts existing=scan(input);if(existing.begin==1){require(existing);copy(existing,c);System.out.println("Rules CSV loader diagnostics already present; leaving bytecode unchanged.");return input;}if(existing.helperCalls!=0)throw new IllegalStateException("partial rules CSV diagnostics helperCalls="+existing.helperCalls);
        ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){MethodVisitor mv=super.visitMethod(a,n,d,s,ex);if(!METHOD.equals(n)||!DESC.equals(d))return mv;c.methods++;return new MethodVisitor(Opcodes.ASM9,mv){
            void transition(int phase){super.visitIntInsn(Opcodes.BIPUSH,phase);super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"transition","(I)V",false);c.transitions++;c.helperCalls++;}
            @Override public void visitCode(){super.visitCode();super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"begin","(Ljava/lang/String;)V",false);c.begin++;c.helperCalls++;}
            @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
                boolean sourceList = op==Opcodes.INVOKEVIRTUAL && owner.startsWith("com/fs/util/oo") && "(Ljava/lang/String;)Ljava/util/List;".equals(desc);
                boolean read = op==Opcodes.INVOKESTATIC && TARGET.equals(owner) && METHOD.equals(name) && READ_DESC.equals(desc);
                boolean preprocess = op==Opcodes.INVOKESTATIC && SPECSTORE.equals(owner) && "Object".equals(name) && PREPROCESS_DESC.equals(desc);
                boolean parse = op==Opcodes.INVOKESTATIC && PARSER.equals(owner) && "o00000".equals(name) && PARSE_DESC.equals(desc);
                if(read)transition(2);
                super.visitMethodInsn(op,owner,name,desc,itf);
                if(sourceList){transition(1);c.discovery++;}
                if(read){transition(3);c.read++;}
                if(preprocess){transition(4);c.preprocess++;}
                if(parse){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"parsedSource","()V",false);c.parse++;c.helperCalls++;}
            }
            @Override public void visitTypeInsn(int op,String type){if(op==Opcodes.NEW&&"org/json/JSONArray".equals(type)){transition(6);c.copyPhase++;}super.visitTypeInsn(op,type);}
            @Override public void visitInsn(int op){if(op==Opcodes.ARETURN){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"finish","()V",false);c.finish++;c.helperCalls++;}super.visitInsn(op);}
        };}},0);return w.toByteArray();}
    private static Counts scan(byte[] input){Counts c=new Counts();c.classes=1;new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!METHOD.equals(n)||!DESC.equals(d))return null;c.methods++;return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){if(op==Opcodes.INVOKESTATIC&&HELPER.equals(o)){c.helperCalls++;if("begin".equals(n))c.begin++;else if("transition".equals(n))c.transitions++;else if("parsedSource".equals(n))c.parse++;else if("finish".equals(n))c.finish++;}}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);if(c.begin==1&&c.transitions==5&&c.parse==1&&c.finish==1){c.discovery=1;c.read=1;c.preprocess=1;c.copyPhase=1;}return c;}
    private static void require(Counts c){if(c.classes!=1||c.methods!=1||c.begin!=1||c.discovery!=1||c.read!=1||c.preprocess!=1||c.parse!=1||c.copyPhase!=1||c.finish!=1||c.transitions!=5||c.helperCalls!=8)throw new IllegalStateException("rules CSV diag mismatch classes="+c.classes+" methods="+c.methods+" begin="+c.begin+" discovery="+c.discovery+" read="+c.read+" preprocess="+c.preprocess+" parse="+c.parse+" copy="+c.copyPhase+" finish="+c.finish+" transitions="+c.transitions+" helpers="+c.helperCalls);}
    private static void copy(Counts a,Counts b){b.classes=a.classes;b.methods=a.methods;b.begin=a.begin;b.discovery=a.discovery;b.read=a.read;b.preprocess=a.preprocess;b.parse=a.parse;b.copyPhase=a.copyPhase;b.finish=a.finish;b.transitions=a.transitions;b.helperCalls=a.helperCalls;}
    static final class Counts{int classes,methods,begin,discovery,read,preprocess,parse,copyPhase,finish,transitions,helperCalls;}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
