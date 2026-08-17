import com.fs.starfarer.BrowserMemoizingNameCoder;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import java.util.Random;

public final class VerifyBrowserMemoizingNameCoder {
    public static void main(String[] args) {
        final XmlFriendlyNameCoder stock = new XmlFriendlyNameCoder();
        final BrowserMemoizingNameCoder fast = new BrowserMemoizingNameCoder();
        final String[] fixed = {
            "ref", "z", "cl", "CampaignEngine", "some_field", "with$dollar",
            "double__under", "_.0041", "a-b.c", "é", "属性", ""
        };
        for (int pass = 0; pass < 20; pass++) {
            for (String s : fixed) compare(stock, fast, s);
        }
        final Random r = new Random(0x51A7C0DEL);
        final String alphabet = "abcXYZ019_$.-éΩ_";
        for (int i = 0; i < 20000; i++) {
            final int n = r.nextInt(24);
            final StringBuilder b = new StringBuilder(n);
            for (int j = 0; j < n; j++) b.append(alphabet.charAt(r.nextInt(alphabet.length())));
            compare(stock, fast, b.toString());
        }
        final BrowserMemoizingNameCoder cloned = (BrowserMemoizingNameCoder) fast.clone();
        for (String s : fixed) compare(stock, cloned, s);
        verifyXStreamGraph();
        System.out.println("VerifyBrowserMemoizingNameCoder: OK");
    }

    private static void verifyXStreamGraph() {
        final XStream stock = new XStream(new StaxDriver());
        final XStream fast = new XStream(new StaxDriver(new BrowserMemoizingNameCoder()));
        stock.setMode(XStream.ID_REFERENCES);
        fast.setMode(XStream.ID_REFERENCES);
        final GraphChild shared = new GraphChild("shared_name", 42);
        final GraphRoot root = new GraphRoot();
        root.first_child = shared;
        root.second_child = shared;
        root.dollar$value = "v_$__";
        root.count = 7;
        final String stockXml = stock.toXML(root);
        final String fastXml = fast.toXML(root);
        if (!stockXml.equals(fastXml)) {
            throw new AssertionError("XStream XML mismatch\nstock=" + stockXml + "\nfast=" + fastXml);
        }
        final GraphRoot loaded = (GraphRoot) fast.fromXML(fastXml);
        if (loaded == null || loaded.first_child == null
                || loaded.first_child != loaded.second_child
                || loaded.first_child.value != 42
                || !"shared_name".equals(loaded.first_child.name)
                || !"v_$__".equals(loaded.dollar$value)
                || loaded.count != 7) {
            throw new AssertionError("XStream round-trip mismatch");
        }
    }

    public static final class GraphRoot {
        GraphChild first_child;
        GraphChild second_child;
        String dollar$value;
        int count;
    }

    public static final class GraphChild {
        String name;
        int value;
        GraphChild() {}
        GraphChild(final String name, final int value) {
            this.name = name;
            this.value = value;
        }
    }

    private static void compare(final XmlFriendlyNameCoder stock,
                                final BrowserMemoizingNameCoder fast,
                                final String s) {
        compareCall(stock, fast, s, 0, "encodeNode");
        compareCall(stock, fast, s, 1, "decodeNode");
        compareCall(stock, fast, s, 2, "encodeAttribute");
        compareCall(stock, fast, s, 3, "decodeAttribute");
    }

    private static void compareCall(final XmlFriendlyNameCoder stock,
                                    final BrowserMemoizingNameCoder fast,
                                    final String input,
                                    final int op,
                                    final String label) {
        String a = null, b = null;
        Throwable ae = null, be = null;
        try { a = call(stock, input, op); } catch (Throwable t) { ae = t; }
        try { b = call(fast, input, op); } catch (Throwable t) { be = t; }
        if (ae != null || be != null) {
            if (ae == null || be == null
                    || !ae.getClass().equals(be.getClass())
                    || !String.valueOf(ae.getMessage()).equals(String.valueOf(be.getMessage()))) {
                throw new AssertionError(label + " exception mismatch input=" + input
                        + " stock=" + ae + " fast=" + be);
            }
            return;
        }
        if (a == null ? b != null : !a.equals(b)) {
            throw new AssertionError(label + " mismatch input=" + input + " stock=" + a + " fast=" + b);
        }
    }

    private static String call(final XmlFriendlyNameCoder coder, final String input, final int op) {
        switch (op) {
            case 0: return coder.encodeNode(input);
            case 1: return coder.decodeNode(input);
            case 2: return coder.encodeAttribute(input);
            case 3: return coder.decodeAttribute(input);
            default: throw new AssertionError("bad op " + op);
        }
    }
}
