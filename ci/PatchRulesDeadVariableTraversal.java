import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Browser-only bypass around six dead Rules variable-usage traversal blocks. */
public final class PatchRulesDeadVariableTraversal {
    private static final String ENTRY = "com/fs/starfarer/campaign/rules/Rules.class";
    private static final String RULE_EXPR = "com/fs/starfarer/campaign/rules/A";
    private static final String HELPER = "com/fs/starfarer/campaign/rules/BrowserRuleDuplicateIndex";
    private static final String METHOD = "super";
    private static final String DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    private static final String TOKEN_DESC = "()Lcom/fs/starfarer/api/util/Misc$Token;";
    private static final String PARAM_DESC = "()Ljava/util/List;";

    private PatchRulesDeadVariableTraversal() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchRulesDeadVariableTraversal input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        int[] classes={0}, methods={0}, guarded={0};
        try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){
            Enumeration<JarEntry> es=jar.entries();
            while(es.hasMoreElements()){
                JarEntry e=es.nextElement(); JarEntry c=new JarEntry(e.getName()); c.setTime(e.getTime()); out.putNextEntry(c);
                byte[] bytes; try(InputStream in=jar.getInputStream(e)){bytes=readAll(in);}
                if(ENTRY.equals(e.getName())){classes[0]++;bytes=patch(bytes,methods,guarded);}
                out.write(bytes);out.closeEntry();
            }
        }
        if(classes[0]!=1||methods[0]!=1||guarded[0]!=6){Files.deleteIfExists(output);throw new IllegalStateException("Rules traversal patch mismatch classes="+classes[0]+" methods="+methods[0]+" guarded="+guarded[0]);}
        System.out.println("Patched Rules browser dead-variable traversal guards="+guarded[0]);
    }

    private static byte[] patch(byte[] input,int[] methods,int[] guarded){
        Scan s=scan(input);
        if(s.cachedEnabledStores==1 && s.guardedTotal()==6){
            requireDistribution(s.guarded,"already-guarded"); methods[0]++;guarded[0]=6;
            System.out.println("Rules dead-variable traversal guards already present; leaving bytecode unchanged.");
            return input;
        }
        if(s.cachedEnabledStores!=0||s.guardedTotal()!=0||s.stockTotal()!=6) throw new IllegalStateException("unexpected Rules traversal state stock="+s.stock+" guarded="+s.guarded+" cached="+s.cachedEnabledStores);
        requireDistribution(s.stock,"stock");
        int guardLocal=s.maxLocals;

        ClassReader r=new ClassReader(input);
        ClassWriter w=new SafeClassWriter(r,ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
                MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex); if(!METHOD.equals(name)||!DESC.equals(desc))return mv; methods[0]++;
                return new MethodVisitor(Opcodes.ASM9,mv){
                    String pendingGetter;
                    boolean justBegin;
                    boolean target(String owner,String n,String d){
                        if(!RULE_EXPR.equals(owner))return false;
                        if(("getFirst".equals(n)||"getSecond".equals(n))&&TOKEN_DESC.equals(d))return true;
                        return "getCommandParams".equals(n)&&PARAM_DESC.equals(d);
                    }
                    void clearGetter(){pendingGetter=null;}
                    @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){
                        super.visitMethodInsn(op,owner,n,d,itf);
                        if(op==Opcodes.INVOKESTATIC&&HELPER.equals(owner)&&"begin".equals(n)&&"()V".equals(d)){
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HELPER,"deadVariableTrackingBypassEnabled","()Z",false);
                            super.visitVarInsn(Opcodes.ISTORE,guardLocal);
                            justBegin=true; clearGetter(); return;
                        }
                        justBegin=false;
                        pendingGetter=(op==Opcodes.INVOKEVIRTUAL&&target(owner,n,d))?n:null;
                    }
                    @Override public void visitJumpInsn(int op,Label target){
                        justBegin=false;
                        if(pendingGetter!=null&&op==Opcodes.IFNULL){
                            Label stock=new Label();
                            super.visitVarInsn(Opcodes.ILOAD,guardLocal);
                            super.visitJumpInsn(Opcodes.IFEQ,stock); // disabled/fallback: original analysis
                            super.visitInsn(Opcodes.POP);           // enabled/browser: discard getter result
                            super.visitJumpInsn(Opcodes.GOTO,target);
                            super.visitLabel(stock);
                            super.visitJumpInsn(Opcodes.IFNULL,target); // exact stock branch retained
                            guarded[0]++; clearGetter(); return;
                        }
                        clearGetter(); super.visitJumpInsn(op,target);
                    }
                    @Override public void visitInsn(int op){justBegin=false;clearGetter();super.visitInsn(op);}
                    @Override public void visitIntInsn(int op,int v){justBegin=false;clearGetter();super.visitIntInsn(op,v);}
                    @Override public void visitVarInsn(int op,int v){
                        // The ISTORE we inject is emitted through super, so original bytecode only reaches here.
                        justBegin=false;clearGetter();super.visitVarInsn(op,v);
                    }
                    @Override public void visitTypeInsn(int op,String t){justBegin=false;clearGetter();super.visitTypeInsn(op,t);}
                    @Override public void visitFieldInsn(int op,String o,String n,String d){justBegin=false;clearGetter();super.visitFieldInsn(op,o,n,d);}
                    @Override public void visitLdcInsn(Object v){justBegin=false;clearGetter();super.visitLdcInsn(v);}
                    @Override public void visitIincInsn(int v,int i){justBegin=false;clearGetter();super.visitIincInsn(v,i);}
                    @Override public void visitLabel(Label l){justBegin=false;clearGetter();super.visitLabel(l);}
                    @Override public void visitMaxs(int ms,int ml){super.visitMaxs(ms,Math.max(ml,guardLocal+1));}
                };
            }
        },0);
        return w.toByteArray();
    }

    private static Scan scan(byte[] input){
        Scan out=new Scan();
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
                if(!METHOD.equals(name)||!DESC.equals(desc))return null;
                return new MethodVisitor(Opcodes.ASM9){
                    String getter; int state; Label stockLabel,targetLabel; boolean afterEnabled;
                    boolean target(String owner,String n,String d){return RULE_EXPR.equals(owner)&&(("getFirst".equals(n)||"getSecond".equals(n))||"getCommandParams".equals(n));}
                    void reset(){getter=null;state=0;stockLabel=null;targetLabel=null;afterEnabled=false;}
                    @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){
                        if(op==Opcodes.INVOKESTATIC&&HELPER.equals(owner)&&"deadVariableTrackingBypassEnabled".equals(n)&&"()Z".equals(d)){afterEnabled=true;return;}
                        if(op==Opcodes.INVOKEVIRTUAL&&target(owner,n,d)){getter=n;state=1;afterEnabled=false;return;}
                        reset();
                    }
                    @Override public void visitVarInsn(int op,int v){
                        if(afterEnabled&&op==Opcodes.ISTORE){out.cachedEnabledStores++;out.guardLocal=v;afterEnabled=false;return;}
                        if(state==1&&op==Opcodes.ILOAD){out.guardLocal=v;state=2;return;} reset();
                    }
                    @Override public void visitJumpInsn(int op,Label l){
                        if(state==1&&op==Opcodes.IFNULL){out.stock.merge(getter,1,Integer::sum);reset();return;}
                        if(state==2&&op==Opcodes.IFEQ){stockLabel=l;state=3;return;}
                        if(state==4&&op==Opcodes.GOTO){targetLabel=l;state=5;return;}
                        if(state==6&&op==Opcodes.IFNULL&&l==targetLabel){out.guarded.merge(getter,1,Integer::sum);reset();return;}
                        reset();
                    }
                    @Override public void visitInsn(int op){if(state==3&&op==Opcodes.POP){state=4;return;}reset();}
                    @Override public void visitLabel(Label l){if(state==5&&l==stockLabel){state=6;return;} if(state!=0)reset();}
                    @Override public void visitFrame(int type,int nl,Object[] l,int ns,Object[] st){}
                    @Override public void visitMaxs(int ms,int ml){out.maxLocals=ml;}
                    @Override public void visitFieldInsn(int op,String o,String n,String d){reset();}
                    @Override public void visitLdcInsn(Object v){reset();}
                    @Override public void visitTypeInsn(int op,String t){reset();}
                    @Override public void visitIincInsn(int v,int i){reset();}
                };
            }
        },ClassReader.SKIP_DEBUG);
        return out;
    }
    private static void requireDistribution(Map<String,Integer> m,String label){
        if(m.getOrDefault("getFirst",0)!=2||m.getOrDefault("getSecond",0)!=2||m.getOrDefault("getCommandParams",0)!=2) throw new IllegalStateException(label+" distribution="+m);
    }
    static final class Scan{final Map<String,Integer>stock=new HashMap<>(),guarded=new HashMap<>();int cachedEnabledStores,guardLocal=-1,maxLocals;int stockTotal(){return stock.values().stream().mapToInt(Integer::intValue).sum();}int guardedTotal(){return guarded.values().stream().mapToInt(Integer::intValue).sum();}}
    static final class SafeClassWriter extends ClassWriter{SafeClassWriter(ClassReader r,int f){super(r,f);}@Override protected String getCommonSuperClass(String a,String b){return "java/lang/Object";}}
    static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[65536];int n;while((n=in.read(b))>=0)o.write(b,0,n);return o.toByteArray();}
}
