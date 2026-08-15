import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Verifies cached browser guard + exact fallback around six dead diagnostic traversal blocks. */
public final class VerifyRulesDeadVariableTraversalPatch {
    private static final String ENTRY="com/fs/starfarer/campaign/rules/Rules.class", A="com/fs/starfarer/campaign/rules/A", H="com/fs/starfarer/campaign/rules/BrowserRuleDuplicateIndex", METHOD="super", DESC="(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("usage: VerifyRulesDeadVariableTraversalPatch jar"); C c=new C();
        try(JarFile j=new JarFile(Path.of(args[0]).toFile())){JarEntry e=j.getJarEntry(ENTRY);if(e==null)throw new AssertionError("missing Rules");try(InputStream in=j.getInputStream(e)){inspect(new ClassReader(in),c);}}
        if(c.methods!=1||c.cachedStores!=1||c.guarded!=6)throw new AssertionError("guard structure methods="+c.methods+" cached="+c.cachedStores+" guarded="+c.guarded);
        if(c.byGetter.getOrDefault("getFirst",0)!=2||c.byGetter.getOrDefault("getSecond",0)!=2||c.byGetter.getOrDefault("getCommandParams",0)!=2)throw new AssertionError("guard distribution="+c.byGetter);
        if(c.isVariable<6||c.entityReplace<6||c.ruleCtor<1||c.scriptStore<1||c.jsonGetString<3||c.ruleListAdd<1)throw new AssertionError("fallback/parse body missing isVar="+c.isVariable+" entityReplace="+c.entityReplace+" ctor="+c.ruleCtor+" script="+c.scriptStore+" json="+c.jsonGetString+" add="+c.ruleListAdd);
        System.out.println("VerifyRulesDeadVariableTraversalPatch: OK cached browser guard=1 guarded blocks=6 exact fallback bodies retained");
    }
    static void inspect(ClassReader r,C c){r.accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]ex){if(!METHOD.equals(name)||!DESC.equals(desc))return null;c.methods++;return new MethodVisitor(Opcodes.ASM9){
        String getter;int state;Label stock,target;boolean afterEnabled;
        boolean targetGetter(String o,String n){return A.equals(o)&&("getFirst".equals(n)||"getSecond".equals(n)||"getCommandParams".equals(n));}
        void reset(){getter=null;state=0;stock=null;target=null;afterEnabled=false;}
        @Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf){
            if(op==Opcodes.INVOKESTATIC&&H.equals(o)&&"deadVariableTrackingBypassEnabled".equals(n)&&"()Z".equals(d)){afterEnabled=true;return;}
            if(op==Opcodes.INVOKEVIRTUAL&&targetGetter(o,n)){getter=n;state=1;afterEnabled=false;return;}
            if(op==Opcodes.INVOKEVIRTUAL&&"com/fs/starfarer/api/util/Misc$Token".equals(o)&&"isVariable".equals(n))c.isVariable++;
            if(op==Opcodes.INVOKEVIRTUAL&&"java/lang/String".equals(o)&&"replace".equals(n))c.entityReplace++;
            if(op==Opcodes.INVOKESPECIAL&&"com/fs/starfarer/campaign/rules/ooOO".equals(o)&&"<init>".equals(n))c.ruleCtor++;
            if(op==Opcodes.INVOKESTATIC&&o.startsWith("com/fs/starfarer/loading/scripts/ScriptStore"))c.scriptStore++;
            if("org/json/JSONObject".equals(o)&&"getString".equals(n))c.jsonGetString++;
            if(op==Opcodes.INVOKEINTERFACE&&"java/util/List".equals(o)&&"add".equals(n))c.ruleListAdd++;
            reset();
        }
        @Override public void visitVarInsn(int op,int v){if(afterEnabled&&op==Opcodes.ISTORE){c.cachedStores++;c.guardLocal=v;afterEnabled=false;return;}if(state==1&&op==Opcodes.ILOAD&&v==c.guardLocal){state=2;return;}reset();}
        @Override public void visitJumpInsn(int op,Label l){if(state==2&&op==Opcodes.IFEQ){stock=l;state=3;return;}if(state==4&&op==Opcodes.GOTO){target=l;state=5;return;}if(state==6&&op==Opcodes.IFNULL&&l==target){c.guarded++;c.byGetter.merge(getter,1,Integer::sum);reset();return;}reset();}
        @Override public void visitInsn(int op){if(state==3&&op==Opcodes.POP){state=4;return;}reset();}
        @Override public void visitLabel(Label l){if(state==5&&l==stock){state=6;return;}if(state!=0)reset();}
        @Override public void visitFrame(int t,int nl,Object[]ls,int ns,Object[]st){}
        @Override public void visitFieldInsn(int op,String o,String n,String d){reset();}
        @Override public void visitLdcInsn(Object v){reset();}
        @Override public void visitTypeInsn(int op,String t){reset();}
        @Override public void visitIincInsn(int v,int i){reset();}
    };}},ClassReader.SKIP_DEBUG);}
    static final class C{int methods,cachedStores,guardLocal=-1,guarded,isVariable,entityReplace,ruleCtor,scriptStore,jsonGetString,ruleListAdd;final Map<String,Integer>byGetter=new HashMap<>();}
}
