import com.thoughtworks.xstream.io.path.BrowserFastPathTracker;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import java.util.Random;

/** Exact differential and native microbenchmark for BrowserFastPathTracker. */
public final class VerifyBrowserFastPathTracker {
    public static void main(String[] args) {
        deterministic();
        randomized();
        benchmark();
        System.out.println("VerifyBrowserFastPathTracker: OK enabled=" + Boolean.getBoolean("starsector.browserXstreamFastPathTracker"));
    }

    private static void deterministic() {
        PathTracker stock = new PathTracker();
        BrowserFastPathTracker fast = new BrowserFastPathTracker();
        push(stock, fast, "root");
        push(stock, fast, "items");
        for (int i = 1; i <= 12; i++) {
            push(stock, fast, "item");
            compare(stock, fast, "item-" + i);
            pop(stock, fast);
        }
        push(stock, fast, "other");
        push(stock, fast, "item");
        compare(stock, fast, "nested");
        pop(stock, fast);
        pop(stock, fast);
        compare(stock, fast, "after-nested");
        pop(stock, fast);
        pop(stock, fast);
        compare(stock, fast, "empty");
    }

    private static void randomized() {
        Random random = new Random(0x51A7C0DEL);
        String[] names = {"a", "b", "item", "entry", "same", "x_y", "with$dollar", "ÃŽÂ©"};
        for (int trial = 0; trial < 200; trial++) {
            PathTracker stock = new PathTracker();
            BrowserFastPathTracker fast = new BrowserFastPathTracker();
            int depth = 0;
            for (int step = 0; step < 4000; step++) {
                boolean doPush = depth == 0 || (depth < 48 && random.nextInt(100) < 62);
                if (doPush) {
                    String name = names[random.nextInt(names.length)];
                    push(stock, fast, name);
                    depth++;
                } else {
                    pop(stock, fast);
                    depth--;
                }
                compare(stock, fast, "trial=" + trial + " step=" + step);
                if (depth > 0) {
                    int back = random.nextInt(depth) - (depth - 1);
                    comparePeek(stock, fast, back, "peek trial=" + trial + " step=" + step);
                }
            }
            while (depth-- > 0) {
                pop(stock, fast);
                compare(stock, fast, "drain trial=" + trial);
            }
        }
    }

    private static void benchmark() {
        final int count = 100000;
        long stockNs = timeSiblings(new PathTracker(), count);
        long fastNs = timeSiblings(new BrowserFastPathTracker(), count);
        System.out.println("FastPathTrackerBenchmark: siblings=" + count
                + " stockMs=" + stockNs / 1000000L
                + " fastMs=" + fastNs / 1000000L);
    }

    private static long timeSiblings(PathTracker tracker, int count) {
        tracker.pushElement("root");
        tracker.pushElement("items");
        long started = System.nanoTime();
        for (int i = 0; i < count; i++) {
            tracker.pushElement("item");
            Path p = tracker.getPath();
            if (p == null) throw new AssertionError();
            tracker.popElement();
        }
        long elapsed = System.nanoTime() - started;
        tracker.popElement();
        tracker.popElement();
        return elapsed;
    }

    private static void push(PathTracker stock, BrowserFastPathTracker fast, String name) {
        stock.pushElement(name);
        fast.pushElement(name);
        compare(stock, fast, "push " + name);
    }

    private static void pop(PathTracker stock, BrowserFastPathTracker fast) {
        stock.popElement();
        fast.popElement();
    }

    private static void compare(PathTracker stock, BrowserFastPathTracker fast, String where) {
        if (stock.depth() != fast.depth()) {
            throw new AssertionError(where + " depth stock=" + stock.depth() + " fast=" + fast.depth());
        }
        Path a = stock.getPath();
        Path b = fast.getPath();
        if (!a.equals(b) || !a.toString().equals(b.toString())
                || !a.explicit().equals(b.explicit()) || a.hashCode() != b.hashCode()) {
            throw new AssertionError(where + " path mismatch stock=" + a + " fast=" + b
                    + " stockExplicit=" + a.explicit() + " fastExplicit=" + b.explicit());
        }
        if (stock.depth() > 0) comparePeek(stock, fast, 0, where + " peek0");
    }

    private static void comparePeek(PathTracker stock, BrowserFastPathTracker fast, int index, String where) {
        String a = stock.peekElement(index);
        String b = fast.peekElement(index);
        if (!a.equals(b)) throw new AssertionError(where + " stock=" + a + " fast=" + b);
    }
}
