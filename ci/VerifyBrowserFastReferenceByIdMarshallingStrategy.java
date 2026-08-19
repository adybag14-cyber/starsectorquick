import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.BrowserFastReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.core.ReferencingMarshallingContext;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Byte-for-byte semantic differential for the browser ID-reference marshaller prototype. */
public final class VerifyBrowserFastReferenceByIdMarshallingStrategy {
    private static final XStream STOCK = configured(false);
    private static final XStream FAST = configured(true);

    public static void main(String[] args) {
        compare("shared-cycle", buildSharedCycle());
        compare("implicit-list", buildImplicitList());
        compare("write-replace", buildWriteReplace());
        compare("collections", buildCollections());
        compare("path-probe", buildPathProbe());
        Random random = new Random(0x5A17E55L);
        for (int i = 0; i < 1000; i++) compare("fuzz-" + i, buildFuzz(random, i));
        benchmark();
        System.out.println("VerifyBrowserFastReferenceByIdMarshallingStrategy: OK");
    }

    private static void compare(String label, Object value) {
        String a = STOCK.toXML(value);
        String b = FAST.toXML(value);
        if (!a.equals(b)) {
            int p = firstDiff(a, b);
            throw new AssertionError(label + " XML mismatch at " + p
                    + "\nstock=" + around(a, p) + "\nfast =" + around(b, p));
        }
        Object loaded = FAST.fromXML(b);
        if (loaded == null || !loaded.getClass().equals(value.getClass())) {
            throw new AssertionError(label + " roundtrip class mismatch");
        }
    }

    private static XStream configured(boolean fast) {
        XStream x = new XStream(new StaxDriver());
        x.setMode(XStream.ID_REFERENCES);
        if (fast) x.setMarshallingStrategy(new BrowserFastReferenceByIdMarshallingStrategy());
        x.alias("Node", Node.class);
        x.alias("ImplicitHolder", ImplicitHolder.class);
        x.alias("ReplaceHolder", ReplaceHolder.class);
        x.alias("Replaceable", Replaceable.class);
        x.alias("Replacement", Replacement.class);
        x.alias("CollectionsHolder", CollectionsHolder.class);
        x.alias("PathProbeHolder", PathProbeHolder.class);
        x.alias("PathProbe", PathProbe.class);
        x.registerConverter(new PathProbeConverter());
        x.addImplicitCollection(ImplicitHolder.class, "children");
        return x;
    }

    private static Node buildSharedCycle() {
        Node root = new Node("root");
        Node a = new Node("a");
        Node b = new Node("b");
        root.left = a;
        root.right = a;
        a.left = b;
        b.right = root;
        root.items.add(a);
        root.items.add(b);
        root.items.add(a);
        root.named.put("a", a);
        root.named.put("b", b);
        root.named.put("root", root);
        return root;
    }

    private static ImplicitHolder buildImplicitList() {
        ImplicitHolder holder = new ImplicitHolder();
        Node shared = new Node("shared");
        holder.children.add(new Node("one"));
        holder.children.add(shared);
        holder.children.add(shared);
        holder.direct = shared;
        return holder;
    }

    private static ReplaceHolder buildWriteReplace() {
        ReplaceHolder holder = new ReplaceHolder();
        Replaceable shared = new Replaceable("swap-me", 73);
        holder.first = shared;
        holder.second = shared;
        return holder;
    }

    private static CollectionsHolder buildCollections() {
        CollectionsHolder holder = new CollectionsHolder();
        Node shared = new Node("shared-map-value");
        holder.list.add("alpha");
        holder.list.add(Integer.valueOf(4));
        holder.list.add(shared);
        holder.list.add(shared);
        holder.map.put("one", shared);
        holder.map.put("self", holder.map);
        holder.array = new Object[] { shared, "x", shared };
        return holder;
    }


    private static PathProbeHolder buildPathProbe() {
        PathProbeHolder holder = new PathProbeHolder();
        holder.probes.add(new PathProbe());
        holder.probes.add(new PathProbe());
        holder.probes.add(new PathProbe());
        holder.nested.add(holder.probes.get(0));
        holder.nested.add(holder.probes.get(1));
        return holder;
    }

    private static Node buildFuzz(Random random, int index) {
        int count = 4 + random.nextInt(18);
        ArrayList<Node> nodes = new ArrayList<Node>();
        for (int i = 0; i < count; i++) nodes.add(new Node("n" + index + "_" + i));
        Node root = nodes.get(0);
        for (int i = 0; i < count; i++) {
            Node n = nodes.get(i);
            if (random.nextBoolean()) n.left = nodes.get(random.nextInt(count));
            if (random.nextBoolean()) n.right = nodes.get(random.nextInt(count));
            int listCount = random.nextInt(5);
            for (int j = 0; j < listCount; j++) n.items.add(nodes.get(random.nextInt(count)));
            int mapCount = random.nextInt(4);
            for (int j = 0; j < mapCount; j++) n.named.put("k" + j, nodes.get(random.nextInt(count)));
        }
        return root;
    }

    private static void benchmark() {
        Node root = buildFuzz(new Random(0xC0FFEE), 999);
        XStream stock = STOCK;
        XStream fast = FAST;
        String expected = stock.toXML(root);
        String actual = fast.toXML(root);
        if (!expected.equals(actual)) throw new AssertionError("benchmark graph mismatch");
        for (int i = 0; i < 15; i++) { stock.toXML(root); fast.toXML(root); }
        long s0 = System.nanoTime();
        for (int i = 0; i < 120; i++) stock.toXML(root);
        long s1 = System.nanoTime();
        for (int i = 0; i < 120; i++) fast.toXML(root);
        long s2 = System.nanoTime();
        long stockMs = (s1 - s0) / 1_000_000L;
        long fastMs = (s2 - s1) / 1_000_000L;
        System.out.println("FastIdMarshallerBenchmark: stockMs=" + stockMs + " fastMs=" + fastMs);
    }

    private static int firstDiff(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) if (a.charAt(i) != b.charAt(i)) return i;
        return n;
    }

    private static String around(String s, int p) {
        int a = Math.max(0, p - 220), b = Math.min(s.length(), p + 420);
        return s.substring(a, b);
    }

    public static final class Node {
        String name;
        Node left;
        Node right;
        List<Node> items = new ArrayList<Node>();
        Map<String, Node> named = new LinkedHashMap<String, Node>();
        Node() {}
        Node(String name) { this.name = name; }
    }

    public static final class ImplicitHolder {
        List<Node> children = new ArrayList<Node>();
        Node direct;
    }

    public static final class ReplaceHolder {
        Replaceable first;
        Replaceable second;
    }

    public static class Replaceable {
        String name;
        int value;
        Replaceable() {}
        Replaceable(String name, int value) { this.name = name; this.value = value; }
        private Object writeReplace() { return new Replacement(name, value); }
    }

    public static final class Replacement extends Replaceable {
        Replacement() {}
        Replacement(String name, int value) { super(name, value); }
    }

    public static final class CollectionsHolder {
        List<Object> list = new ArrayList<Object>();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        Object[] array;
    }

    public static final class PathProbeHolder {
        List<PathProbe> probes = new ArrayList<PathProbe>();
        List<PathProbe> nested = new ArrayList<PathProbe>();
    }

    public static final class PathProbe {}

    public static final class PathProbeConverter implements Converter {
        @Override public boolean canConvert(Class type) { return type == PathProbe.class; }
        @Override public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            ReferencingMarshallingContext referencing = (ReferencingMarshallingContext) context;
            writer.addAttribute("currentPath", referencing.currentPath().toString());
        }
        @Override public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            return new PathProbe();
        }
    }
}
