import com.fs.starfarer.loading.BrowserFastCsvParser;
import com.fs.starfarer.loading.oOoO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Exhaustive stock CSV equivalence plus property/fallback behavior for the browser parser. */
public final class VerifyBrowserFastCsvParser {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyBrowserFastCsvParser runtime-root");
        Path root = Paths.get(args[0]);

        System.setProperty("starsector.browserFastCsvParser", "false");
        System.setProperty("starsector.browserFastCsvParserSkipModCheck", "true");
        if (BrowserFastCsvParser.parseIfEnabled("a,b\n1,2\n") != null) {
            throw new AssertionError("disabled fast CSV parser did not fall through");
        }

        System.setProperty("starsector.browserFastCsvParser", "true");
        JSONArray smoke = BrowserFastCsvParser.parseIfEnabled("a,b\n1,2\n");
        if (smoke == null || smoke.length() != 1 || !"2".equals(smoke.getJSONObject(0).getString("b"))) {
            throw new AssertionError("enabled fast CSV parser smoke failed");
        }
        // Malformed input must fall through so the original parser can emit its exact detailed error.
        if (BrowserFastCsvParser.parseIfEnabled("a,b\n\"unterminated,2") != null) {
            throw new AssertionError("malformed CSV did not fall through to stock parser");
        }
        boolean stockRejected = false;
        try { oOoO.o00000("a,b\n\"unterminated,2"); } catch (JSONException expected) { stockRejected = true; }
        if (!stockRejected) throw new AssertionError("malformed test fixture was unexpectedly accepted by stock parser");

        List<Path> files = new ArrayList<Path>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> Files.isRegularFile(path)
                            && path.toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .forEach(files::add);
        }
        Collections.sort(files);
        int exact = 0;
        int matchedErrors = 0;
        long bytes = 0L;
        for (Path file : files) {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            bytes += text.getBytes(StandardCharsets.UTF_8).length;
            JSONArray stock;
            try {
                stock = oOoO.o00000(text);
            } catch (JSONException stockError) {
                matchedErrors++;
                try {
                    BrowserFastCsvParser.parseExact(text);
                    throw new AssertionError("fast parser accepted stock-rejected file " + root.relativize(file));
                } catch (JSONException expected) {
                    continue;
                }
            }
            JSONArray fast = BrowserFastCsvParser.parseExact(text);
            compare(stock, fast, root.relativize(file).toString());
            exact++;
        }
        if (files.size() < 30 || exact + matchedErrors != files.size()) {
            throw new AssertionError("unexpected CSV coverage files=" + files.size() + " exact=" + exact + " errors=" + matchedErrors);
        }
        System.out.println("VerifyBrowserFastCsvParser: OK files=" + files.size()
                + " exact=" + exact + " matchedErrors=" + matchedErrors + " bytes=" + bytes);
    }

    private static void compare(JSONArray stock, JSONArray fast, String path) throws Exception {
        if (stock.length() != fast.length()) {
            throw new AssertionError(path + " row-count stock=" + stock.length() + " fast=" + fast.length());
        }
        for (int i = 0; i < stock.length(); i++) {
            JSONObject a = stock.getJSONObject(i);
            JSONObject b = fast.getJSONObject(i);
            if (a.length() != b.length()) {
                throw new AssertionError(path + " row=" + i + " field-count stock=" + a.length() + " fast=" + b.length());
            }
            Iterator<?> keys = a.keys();
            while (keys.hasNext()) {
                String key = String.valueOf(keys.next());
                if (!b.has(key)) throw new AssertionError(path + " row=" + i + " missing key=" + key);
                String av = a.getString(key);
                String bv = b.getString(key);
                if (!av.equals(bv)) {
                    throw new AssertionError(path + " row=" + i + " key=" + key + " stock=" + av + " fast=" + bv);
                }
            }
        }
    }
}
