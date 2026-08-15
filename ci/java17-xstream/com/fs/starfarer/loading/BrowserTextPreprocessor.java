package com.fs.starfarer.loading;

/** Browser-only exact linear equivalent of SpecStore's two smart-quote regex passes. */
public final class BrowserTextPreprocessor {
    private static final String PROPERTY = "starsector.browserFastTextPreprocess";
    private static boolean logged;

    private BrowserTextPreprocessor() {}

    /** Return normalized text when enabled; null asks the caller to run stock regex code. */
    public static String normalizeIfEnabled(String input) {
        if (input == null || !Boolean.getBoolean(PROPERTY)) return null;
        String result = normalizeExact(input);
        if (!logged) {
            logged = true;
            System.out.println("BrowserTextPreprocessor: enabled exact linear smart-quote normalization chars=" + input.length());
        }
        return result;
    }

    /** Exact equivalent of replaceAll("[\\u201c\\u201d]+", "\\\"") then replaceAll("[\\u2018\\u2019\\ufffd]+", "'"). */
    public static String normalizeExact(String input) {
        if (input == null || input.isEmpty()) return input;
        int first = firstSpecial(input);
        if (first < 0) return input;
        StringBuilder out = new StringBuilder(input.length());
        out.append(input, 0, first);
        int i = first;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (isDoubleSmart(c)) {
                out.append('"');
                i++;
                while (i < input.length() && isDoubleSmart(input.charAt(i))) i++;
                continue;
            }
            if (isSingleSmart(c)) {
                out.append('\'');
                i++;
                while (i < input.length() && isSingleSmart(input.charAt(i))) i++;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static int firstSpecial(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (isDoubleSmart(c) || isSingleSmart(c)) return i;
        }
        return -1;
    }

    private static boolean isDoubleSmart(char c) { return c == '\u201c' || c == '\u201d'; }
    private static boolean isSingleSmart(char c) { return c == '\u2018' || c == '\u2019' || c == '\ufffd'; }
}
