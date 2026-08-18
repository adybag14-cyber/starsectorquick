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

/** Diagnostic-only ResourceLoaderState queue/type/path/timing profiler. */
public final class PatchResourceQueueProfile {
    private static final String ENTRY="com/fs/starfarer/loading/ResourceLoaderState.class";
    private static final String RESOURCE="com/fs/starfarer/loading/ResourceLoaderState$Oo";
    private static final String TYPE="com/fs/starfarer/loading/ResourceLoaderState$o";
    private static final String HELPER="com/fs/starfarer/BrowserResourceQueueProfile";
    private static final String QUEUE_DESC="(Lcom/fs/starfarer/loading/ResourceLoaderState$o;Ljava/lang/String;I)V";
    private static final String INIT_DESC="(Ljava/util/Map;)V";

    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("usage: PatchResourceQueueProfile input.jar output.jar");
        Path input=Path.of(args[0]),output=Path.of(args[1]); int[] classes={0},queueMethods={0},initMethods={0},record={0},queueSummary={0},begin={0},end={0},loadSummary={0},phase={0};
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();JarEntry c=new JarEntry(e.getName());c.setTime(e.getTime());out.putNextEntry(c);byte[] bytes;try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,queueMethods,initMethods,record,queueSummary,begin,end,loadSummary,phase);}out.write(bytes);out.closeEntry();}
        }
        if(classes[0]!=1||queueMethods[0]!=1||initMethods[0]!=1||record[0]!=1||queueSummary[0]!=1||begin[0]!=1||end[0]!=2||loadSummary[0]!=1||phase[0]!=3){Files.deleteIfExists(output);throw new IllegalStateException("queue profile mismatch classes="+classes[0]+" queueMethods="+queueMethods[0]+" initMethods="+initMethods[0]+" record="+record[0]+" queueSummary="+queueSummary[0]+" begin="+begin[0]+" end="+end[0]+" loadSummary="+loadSummary[0]+" phase="+phase[0]);}
        System.out.println("Patched ResourceLoader queue profiler record=1 queueSummary=1 begin=1 end=2 loadSummary=1 phase=3");
    }

    private static byte[] patch(byte[] input,int[] queueMethods,int[] initMethods,int[] record,int[] queueSummary,int[] begin,int[] end,int[] loadSummary,int[] phase){
        int[] existing={0,0,0,0,0,0};
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(op==Opcodes.INVOKESTATIC&&HELPER.equals(owner)){if("record".equals(name))existing[0]++;else if("printQueueSummary".equals(name))existing[1]++;else if("beginLoad".equals(name))existing[2]++;else if("endLoad".equals(name))existing[3]++;else if("printLoadSummary".equals(name))existing[4]++;else if("printPhase".equals(name))existing[5]++;}}};}},0);
        if(existing[0]==1&&existing[1]==1&&existing[2]==1&&existing[3]==2&&existing[4]==1&&existing[5]==3){queueMethods[0]=1;initMethods[0]=1;record[0]=1;queueSummary[0]=1;begin[0]=1;end[0]=2;loadSummary[0]=1;phase[0]=3;return input;}
        for(int n:existing)if(n!=0)throw new IllegalStateException("partial queue profiler already present");
        ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex){MethodVisitor mv=super.visitMethod(a,n,d,s,ex);
            if("queueResource".equals(n)&&QUEUE_DESC.equals(d)){queueMethods[0]++;return new MethodVisitor(Opcodes.ASM9,mv){@Override public void visitCode(){super.visitCode();super.visitVarInsn(Opcodes.ALOAD,1);super.visitVarInsn(Opcodes.ALOAD,2);super.visitVarInsn(Opcodes.ILOAD,3);super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"record","(Ljava/lang/Object;Ljava/lang/String;I)V",false);record[0]++;}};}
            if("init".equals(n)&&INIT_DESC.equals(d)){initMethods[0]++;return new MethodVisitor(Opcodes.ASM9,mv){boolean afterExecutor;boolean expectLoadStore;boolean loadRegion;int endSite;
                @Override public void visitTypeInsn(int op,String type){super.visitTypeInsn(op,type);if(afterExecutor&&op==Opcodes.CHECKCAST&&RESOURCE.equals(type))expectLoadStore=true;}
                @Override public void visitVarInsn(int op,int var){
                    if(afterExecutor&&expectLoadStore&&op==Opcodes.ASTORE&&var==11){super.visitVarInsn(op,var);emitBegin(super.mv);begin[0]++;expectLoadStore=false;loadRegion=true;endSite=0;return;}
                    if(loadRegion&&op==Opcodes.ILOAD&&var==7&&endSite<2){emitEnd(super.mv);end[0]++;endSite++;if(endSite==2)loadRegion=false;}
                    super.visitVarInsn(op,var);
                }
                @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
                    if(op==Opcodes.INVOKESTATIC&&"com/fs/starfarer/loading/SpecStore".equals(owner)&&"public".equals(name)&&"(Lcom/fs/starfarer/loading/ResourceLoaderState;)V".equals(desc)){
                        emitPhase(super.mv,"before-specstore");phase[0]++;
                        super.visitMethodInsn(op,owner,name,desc,itf);
                        emitPhase(super.mv,"after-specstore");phase[0]++;
                        return;
                    }
                    if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/starfarer/loading/ResourceLoaderState".equals(owner)&&"queueShipAndWeaponSprites".equals(name)&&"()V".equals(desc)){
                        super.visitMethodInsn(op,owner,name,desc,itf);
                        emitPhase(super.mv,"after-dynamic-sprites");phase[0]++;
                        return;
                    }
                    if(op==Opcodes.INVOKESTATIC&&"java/util/concurrent/Executors".equals(owner)&&"newFixedThreadPool".equals(name)){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"printQueueSummary","()V",false);queueSummary[0]++;afterExecutor=true;}
                    if(op==Opcodes.INVOKESTATIC&&"com/fs/graphics/L".equals(owner)&&"new".equals(name)&&"()V".equals(desc)){super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"printLoadSummary","()V",false);loadSummary[0]++;}
                    super.visitMethodInsn(op,owner,name,desc,itf);
                }
                private void emitPhase(MethodVisitor x,String name){x.visitLdcInsn(name);x.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"printPhase","(Ljava/lang/String;)V",false);}
                private void emitBegin(MethodVisitor x){x.visitVarInsn(Opcodes.ALOAD,11);x.visitFieldInsn(Opcodes.GETFIELD,RESOURCE,"new","L"+TYPE+";");x.visitVarInsn(Opcodes.ALOAD,11);x.visitFieldInsn(Opcodes.GETFIELD,RESOURCE,"o00000","Ljava/lang/String;");x.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"beginLoad","(Ljava/lang/Object;Ljava/lang/String;)V",false);}
                private void emitEnd(MethodVisitor x){x.visitVarInsn(Opcodes.ALOAD,11);x.visitFieldInsn(Opcodes.GETFIELD,RESOURCE,"new","L"+TYPE+";");x.visitVarInsn(Opcodes.ALOAD,11);x.visitFieldInsn(Opcodes.GETFIELD,RESOURCE,"o00000","Ljava/lang/String;");x.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"endLoad","(Ljava/lang/Object;Ljava/lang/String;)V",false);}
            };}
            return mv;
        }},0);return w.toByteArray();
    }
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}
}
