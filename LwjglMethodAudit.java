import java.io.*;
import java.util.*;
import java.util.jar.*;

public class LwjglMethodAudit {
    static class CpInfo {
        int tag;
        Object a;
        Object b;
    }

    static class RefUse {
        final String owner;
        final String name;
        final String desc;
        final String fromClass;
        RefUse(String owner, String name, String desc, String fromClass) {
            this.owner = owner; this.name = name; this.desc = desc; this.fromClass = fromClass;
        }
        String sig() { return owner + "." + name + ":" + desc; }
    }

    static class ParsedClass {
        String thisClass;
        List<RefUse> refs = new ArrayList<RefUse>();
        Set<String> methods = new HashSet<String>();
    }

    static int u1(DataInputStream in) throws IOException { return in.readUnsignedByte(); }
    static int u2(DataInputStream in) throws IOException { return in.readUnsignedShort(); }
    static long u4(DataInputStream in) throws IOException { return in.readInt() & 0xffffffffL; }

    static void skipN(DataInputStream in, long n) throws IOException {
        while (n > 0) {
            long s = in.skip(n);
            if (s <= 0) throw new EOFException("skip failed");
            n -= s;
        }
    }

    static String cpUtf8(CpInfo[] cp, int idx) {
        if (idx <= 0 || idx >= cp.length) return null;
        CpInfo c = cp[idx];
        if (c == null || c.tag != 1) return null;
        return (String) c.a;
    }

    static String cpClassName(CpInfo[] cp, int idx) {
        if (idx <= 0 || idx >= cp.length) return null;
        CpInfo c = cp[idx];
        if (c == null || c.tag != 7) return null;
        return cpUtf8(cp, (Integer) c.a);
    }

    static int[] cpNameAndType(CpInfo[] cp, int idx) {
        if (idx <= 0 || idx >= cp.length) return null;
        CpInfo c = cp[idx];
        if (c == null || c.tag != 12) return null;
        return new int[] { (Integer) c.a, (Integer) c.b };
    }

    static ParsedClass parseClass(byte[] bytes, Set<String> ownerFilter) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) throw new IOException("bad class magic");
        u2(in); // minor
        u2(in); // major
        int cpCount = u2(in);
        CpInfo[] cp = new CpInfo[cpCount];
        for (int i = 1; i < cpCount; i++) {
            int tag = u1(in);
            CpInfo c = new CpInfo();
            c.tag = tag;
            switch (tag) {
                case 1:
                    c.a = in.readUTF();
                    break;
                case 3:
                case 4:
                    in.readInt();
                    break;
                case 5:
                case 6:
                    in.readLong();
                    cp[i] = c;
                    i++; // takes two entries
                    continue;
                case 7:
                case 8:
                case 16:
                case 19:
                case 20:
                    c.a = u2(in);
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 18:
                    c.a = u2(in);
                    c.b = u2(in);
                    break;
                case 15:
                    c.a = u1(in);
                    c.b = u2(in);
                    break;
                default:
                    throw new IOException("unknown cp tag " + tag);
            }
            cp[i] = c;
        }

        u2(in); // access
        int thisClassIdx = u2(in);
        u2(in); // super
        String thisClass = cpClassName(cp, thisClassIdx);

        int ifaceCount = u2(in);
        for (int i = 0; i < ifaceCount; i++) u2(in);

        int fields = u2(in);
        for (int i = 0; i < fields; i++) {
            u2(in); u2(in); u2(in);
            int ac = u2(in);
            for (int j = 0; j < ac; j++) {
                u2(in);
                long len = u4(in);
                skipN(in, len);
            }
        }

        ParsedClass out = new ParsedClass();
        out.thisClass = thisClass;

        int methods = u2(in);
        for (int i = 0; i < methods; i++) {
            u2(in); // access
            int nameIdx = u2(in);
            int descIdx = u2(in);
            String n = cpUtf8(cp, nameIdx);
            String d = cpUtf8(cp, descIdx);
            if (n != null && d != null) out.methods.add(n + ":" + d);
            int ac = u2(in);
            for (int j = 0; j < ac; j++) {
                u2(in);
                long len = u4(in);
                skipN(in, len);
            }
        }

        int classAttrs = u2(in);
        for (int i = 0; i < classAttrs; i++) {
            u2(in);
            long len = u4(in);
            skipN(in, len);
        }

        for (int i = 1; i < cp.length; i++) {
            CpInfo c = cp[i];
            if (c == null) continue;
            if (c.tag == 10 || c.tag == 11) {
                int classIdx = (Integer) c.a;
                int ntIdx = (Integer) c.b;
                String owner = cpClassName(cp, classIdx);
                int[] nt = cpNameAndType(cp, ntIdx);
                if (owner == null || nt == null) continue;
                if (!ownerFilter.contains(owner)) continue;
                String n = cpUtf8(cp, nt[0]);
                String d = cpUtf8(cp, nt[1]);
                if (n == null || d == null) continue;
                out.refs.add(new RefUse(owner, n, d, thisClass));
            }
        }

        return out;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: java LwjglMethodAudit <bridgeJar> <jar1> [jar2...]");
            return;
        }

        Set<String> owners = new LinkedHashSet<String>();
        owners.add("org/lwjgl/opengl/Display");
        owners.add("org/lwjgl/opengl/Displa2");
        owners.add("org/lwjgl/opengl/GL11");
        owners.add("org/lwjgl/opengl/GL1A");
        owners.add("org/lwjgl/opengl/GL1D");
        owners.add("org/lwjgl/opengl/LinuxContextImplementation");
        owners.add("org/lwjgl/Sys");
        owners.add("org/lwjgl/Zys");
        owners.add("org/lwjgl/MemoryUtil");

        Map<String, Set<String>> bridgeMethods = new HashMap<String, Set<String>>();
        for (String o : owners) bridgeMethods.put(o, new HashSet<String>());

        JarFile bridge = new JarFile(args[0]);
        Enumeration<JarEntry> ben = bridge.entries();
        while (ben.hasMoreElements()) {
            JarEntry je = ben.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) continue;
            InputStream is = bridge.getInputStream(je);
            byte[] bytes = readAll(is);
            ParsedClass pc = parseClass(bytes, owners);
            if (pc.thisClass != null && bridgeMethods.containsKey(pc.thisClass)) {
                bridgeMethods.get(pc.thisClass).addAll(pc.methods);
            }
        }
        bridge.close();

        Map<String, Integer> refCount = new HashMap<String, Integer>();
        Map<String, String> sampleFrom = new HashMap<String, String>();

        for (int ai = 1; ai < args.length; ai++) {
            JarFile jf = new JarFile(args[ai]);
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                if (je.isDirectory() || !je.getName().endsWith(".class")) continue;
                InputStream is = jf.getInputStream(je);
                byte[] bytes = readAll(is);
                ParsedClass pc;
                try {
                    pc = parseClass(bytes, owners);
                } catch (Throwable t) {
                    continue;
                }
                for (RefUse r : pc.refs) {
                    String key = r.owner + "." + r.name + ":" + r.desc;
                    refCount.put(key, refCount.getOrDefault(key, 0) + 1);
                    if (!sampleFrom.containsKey(key)) sampleFrom.put(key, r.fromClass + " @ " + args[ai]);
                }
            }
            jf.close();
        }

        List<String> missing = new ArrayList<String>();
        for (String key : refCount.keySet()) {
            int dot = key.lastIndexOf('.');
            int colon = key.indexOf(':', dot + 1);
            String owner = key.substring(0, dot).replace('.', '/');
            String sig = key.substring(dot + 1, colon) + key.substring(colon);
            Set<String> mset = bridgeMethods.get(owner);
            boolean present = mset != null && mset.contains(sig);
            if (!present) missing.add(key);
        }
        Collections.sort(missing, new Comparator<String>() {
            public int compare(String a, String b) {
                return Integer.compare(refCount.getOrDefault(b,0), refCount.getOrDefault(a,0));
            }
        });

        System.out.println("=== Missing Method Refs In Bridge ===");
        for (String m : missing) {
            System.out.println(refCount.get(m) + " | " + m + " | sample=" + sampleFrom.get(m));
        }
        System.out.println("missing_count=" + missing.size());
    }

    static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
        is.close();
        return bos.toByteArray();
    }
}