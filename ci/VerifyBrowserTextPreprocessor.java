import com.fs.starfarer.loading.BrowserTextPreprocessor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/** Exact-equivalence/property behavior gate for BrowserTextPreprocessor. */
public final class VerifyBrowserTextPreprocessor {
    private static String stock(String input) {
        return input.replaceAll("[\\u201c\\u201d]+", "\\\"")
                .replaceAll("[\\u2018\\u2019\\ufffd]+", "'");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyBrowserTextPreprocessor runtime-root");
        System.setProperty("starsector.browserFastTextPreprocess", "false");
        if (BrowserTextPreprocessor.normalizeIfEnabled("abc") != null) {
            throw new AssertionError("disabled normalizer did not fall through");
        }
        System.setProperty("starsector.browserFastTextPreprocess", "true");
        String edge = "a\u201c\u201d\u201cb\u2018\ufffd\u2019c\u201c\u2018\u201d";
        assertSame(edge, "edge");
        if (!stock(edge).equals(BrowserTextPreprocessor.normalizeIfEnabled(edge))) {
            throw new AssertionError("enabled property result mismatch");
        }

        Path root = Paths.get(args[0]);
        List<Path> files = new ArrayList<Path>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> Files.isRegularFile(path)
                            && path.toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .forEach(files::add);
        }
        Collections.sort(files);
        long bytes = 0L;
        int changed = 0;
        for (Path file : files) {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            bytes += text.getBytes(StandardCharsets.UTF_8).length;
            String expected = stock(text);
            String actual = BrowserTextPreprocessor.normalizeExact(text);
            if (!expected.equals(actual)) throw new AssertionError("stock CSV mismatch " + root.relativize(file));
            if (!text.equals(actual)) changed++;
        }
        if (files.size() < 30) throw new AssertionError("unexpected CSV coverage=" + files.size());

        Random random = new Random(0x5eed5eedL);
        char[] alphabet = new char[] {'a','Z','0',' ', ',', '\n', '\r', '"', '\'', '\u201c','\u201d','\u2018','\u2019','\ufffd'};
        for (int caseIndex = 0; caseIndex < 5000; caseIndex++) {
            int length = random.nextInt(512);
            StringBuilder value = new StringBuilder(length);
            for (int i = 0; i < length; i++) value.append(alphabet[random.nextInt(alphabet.length)]);
            assertSame(value.toString(), "fuzz-" + caseIndex);
        }
        System.out.println("VerifyBrowserTextPreprocessor: OK files=" + files.size()
                + " changedFiles=" + changed + " bytes=" + bytes + " fuzz=5000");
    }

    private static void assertSame(String input, String label) {
        String expected = stock(input);
        String actual = BrowserTextPreprocessor.normalizeExact(input);
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + printable(expected) + " actual=" + printable(actual));
        }
    }

    private static String printable(String value) {
        return value.replace("\n", "\\n").replace("\r", "\\r");
    }
}
