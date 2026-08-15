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

/** Diagnostic-only aggregate phase profiler for Rules.super(ResourceLoaderState). */
public final class PatchRulesPhaseDiagnostics {
    private static final String ENTRY = "com/fs/starfarer/campaign/rules/Rules.class";
    private static final String RULES = "com/fs/starfarer/campaign/rules/Rules";
    private static final String METHOD = "super";
    private static final String DESC = "(Lcom/fs/starfarer/loading/ResourceLoaderState;)V";
    private static final String HELPER = "com/fs/starfarer/campaign/rules/BrowserRulesPhaseDiag";
    private static final String DUP = "com/fs/starfarer/campaign/rules/BrowserRuleDuplicateIndex";
    private static final String JSON_ARRAY = "org/json/JSONArray";

    private PatchRulesPhaseDiagnostics() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: PatchRulesPhaseDiagnostics input.jar output.jar");
        Path input = Path.of(args[0]), output = Path.of(args[1]);
        Counts c = new Counts();
        try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> es = jar.entries();
            while (es.hasMoreElements()) {
                JarEntry e = es.nextElement(); JarEntry copy = new JarEntry(e.getName()); copy.setTime(e.getTime()); out.putNextEntry(copy);
                byte[] bytes; try (InputStream in = jar.getInputStream(e)) { bytes = readAll(in); }
                if (ENTRY.equals(e.getName())) { c.classes++; bytes = patch(bytes, c); }
                out.write(bytes); out.closeEntry();
            }
        }
        require(c);
        System.out.println("Patched Rules phase diagnostics rowStart=1 conditions=1 options=1 script=1 register=1 post=1");
    }

    private static byte[] patch(byte[] input, Counts c) {
        Counts existing = scan(input);
        if (existing.begin == 1) {
            require(existing); copy(existing, c); System.out.println("Rules phase diagnostics already present; leaving bytecode unchanged."); return input;
        }
        if (existing.begin != 0 || existing.helperCalls != 0) throw new IllegalStateException("partial Rules phase diagnostics helperCalls=" + existing.helperCalls);
        ClassReader r = new ClassReader(input); ClassWriter w = new ClassWriter(r, ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9, w) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                if (!METHOD.equals(name) || !DESC.equals(desc)) return mv;
                c.methods++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    void helper(String name, String desc) { super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, name, desc, false); c.helperCalls++; }
                    void transition(int phase) { super.visitIntInsn(Opcodes.BIPUSH, phase); helper("transition", "(I)V"); }
                    @Override public void visitCode() { super.visitCode(); helper("begin", "()V"); c.begin++; }
                    @Override public void visitLdcInsn(Object value) {
                        if ("conditions".equals(value)) { transition(2); c.conditions++; }
                        else if ("options".equals(value)) { transition(3); c.options++; }
                        else if ("script".equals(value)) { transition(4); c.script++; }
                        super.visitLdcInsn(value);
                    }
                    @Override public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                        if (op == Opcodes.INVOKEVIRTUAL && JSON_ARRAY.equals(owner) && "getJSONObject".equals(n) && "(I)Lorg/json/JSONObject;".equals(d)) {
                            helper("rowStart", "()V"); c.rowStart++;
                        }
                        if (op == Opcodes.INVOKESTATIC && DUP.equals(owner) && "enabled".equals(n) && "()Z".equals(d)) { transition(5); c.register++; }
                        if (op == Opcodes.INVOKESTATIC && DUP.equals(owner) && "finish".equals(n) && "()V".equals(d)) { transition(6); c.post++; }
                        super.visitMethodInsn(op, owner, n, d, itf);
                    }
                    @Override public void visitInsn(int op) {
                        if (op == Opcodes.RETURN) { helper("finish", "()V"); c.finish++; }
                        super.visitInsn(op);
                    }
                };
            }
        }, 0);
        return w.toByteArray();
    }

    private static Counts scan(byte[] input) {
        Counts c = new Counts(); c.classes = 1;
        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] ex) {
                if (!METHOD.equals(n) || !DESC.equals(d)) return null; c.methods++;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf) {
                        if (op != Opcodes.INVOKESTATIC || !HELPER.equals(o)) return;
                        c.helperCalls++;
                        if ("begin".equals(n)) c.begin++;
                        else if ("rowStart".equals(n)) c.rowStart++;
                        else if ("transition".equals(n)) c.transitions++;
                        else if ("finish".equals(n)) c.finish++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (c.begin == 1 && c.transitions == 5) { c.conditions=1; c.options=1; c.script=1; c.register=1; c.post=1; }
        return c;
    }

    private static void require(Counts c) {
        if (c.classes != 1 || c.methods != 1 || c.begin != 1 || c.rowStart != 1 || c.conditions != 1 || c.options != 1 || c.script != 1 || c.register != 1 || c.post != 1 || c.finish != 1 || c.helperCalls != 8)
            throw new IllegalStateException("Rules phase diagnostic mismatch classes="+c.classes+" methods="+c.methods+" begin="+c.begin+" rowStart="+c.rowStart+" conditions="+c.conditions+" options="+c.options+" script="+c.script+" register="+c.register+" post="+c.post+" finish="+c.finish+" helperCalls="+c.helperCalls);
    }
    private static void copy(Counts a, Counts b) { b.classes=a.classes;b.methods=a.methods;b.begin=a.begin;b.rowStart=a.rowStart;b.conditions=a.conditions;b.options=a.options;b.script=a.script;b.register=a.register;b.post=a.post;b.finish=a.finish;b.helperCalls=a.helperCalls; }
    static final class Counts { int classes, methods, begin, rowStart, conditions, options, script, register, post, finish, transitions, helperCalls; }
    private static byte[] readAll(InputStream in) throws IOException { ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[65536]; int n; while((n=in.read(b))>=0) out.write(b,0,n); return out.toByteArray(); }
}
