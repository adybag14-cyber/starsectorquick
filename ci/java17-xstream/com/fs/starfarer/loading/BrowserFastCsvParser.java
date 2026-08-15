package com.fs.starfarer.loading;

import com.fs.starfarer.launcher.ModManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Browser-only fast path for Starsector's CSV-to-JSONArray parser.
 *
 * The parser preserves oOoO.o00000(String) output exactly for valid input while
 * avoiding diagnostic-only per-character/per-row work. Any uncertainty returns
 * null so the patched caller runs Starsector's original parser unchanged.
 */
public final class BrowserFastCsvParser {
    private static final String ENABLE_PROPERTY = "starsector.browserFastCsvParser";
    private static final String SKIP_MOD_CHECK_PROPERTY = "starsector.browserFastCsvParserSkipModCheck";
    private static final AtomicBoolean FIRST_HIT_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean FALLBACK_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean MOD_FALLBACK_LOGGED = new AtomicBoolean();
    // 0 unknown, 1 stock/no external mods, -1 external mods or failed mod check.
    private static volatile int stockSession;

    private BrowserFastCsvParser() {}

    /** Return a parsed array only when the browser fast path is safe; null means use stock parser. */
    public static JSONArray parseIfEnabled(String input) {
        if (input == null || !Boolean.getBoolean(ENABLE_PROPERTY) || !isStockSession()) {
            return null;
        }
        try {
            JSONArray parsed = parseExact(input);
            if (FIRST_HIT_LOGGED.compareAndSet(false, true)) {
                System.out.println("BrowserFastCsvParser: enabled stock fast path rows=" + parsed.length());
            }
            return parsed;
        } catch (Throwable error) {
            // Preserve Starsector's detailed malformed-CSV diagnostics by falling back
            // to the original parser, which remains immediately behind this hook.
            if (FALLBACK_LOGGED.compareAndSet(false, true)) {
                System.out.println("BrowserFastCsvParser: fallback after fast-parse failure: " + describe(error));
            }
            return null;
        }
    }

    /**
     * Exact valid-input equivalent of com.fs.starfarer.loading.oOoO.o00000(String).
     * Public for the exhaustive stock-data equivalence gate; production calls use
     * parseIfEnabled() so malformed input falls through to the stock parser.
     */
    public static JSONArray parseExact(String input) throws JSONException {
        JSONArray out = new JSONArray();
        String text = input.replace("\r\n", "\n");
        int newline = text.indexOf('\n');
        if (newline < 0 || text.length() <= newline + 1) return out;

        String[] rawHeaders = text.substring(0, newline).split(",");
        List<String> headers = new ArrayList<String>(rawHeaders.length);
        for (String header : rawHeaders) {
            String value = header.trim();
            if (value.startsWith("\"")) value = value.substring(1);
            if (value.endsWith("\"")) value = value.substring(0, value.length() - 1);
            value = value.replace("\"\"", "\"");
            headers.add(value);
        }

        JSONObject row = null;
        StringBuilder field = null;
        boolean inQuotes = false;
        boolean comment = false;
        int column = 0;
        int state = 0; // 0=new row, 1=new field, 2=reading field
        int length = text.length();

        // Starsector copies the body and appends one newline before scanning it.
        // Iterating the original text from header+1 and synthesizing that final newline
        // is equivalent without the multi-megabyte substring/StringBuilder copy.
        for (int i = newline + 1; i <= length; i++) {
            char c = i == length ? '\n' : text.charAt(i);
            char next = i + 1 < length ? text.charAt(i + 1) : ' ';
            boolean rowStartedThisCharacter = false;

            if (state == 0) {
                comment = c == '#';
                row = new JSONObject();
                column = 0;
                rowStartedThisCharacter = true;
                state = 1;
            }
            if (state == 1) {
                field = new StringBuilder();
                state = 2;
            }

            if (c == '"') {
                if (next == '"') {
                    field.append(c);
                    i++;
                    continue;
                }
                inQuotes = !inQuotes;
                continue;
            }

            if ((c == ',' || c == '\n') && !inQuotes) {
                if (column < headers.size()) {
                    row.put(headers.get(column), field.toString());
                }
                if (c == ',') {
                    column++;
                    state = 1;
                } else {
                    if (!rowStartedThisCharacter && !comment) out.put(row);
                    state = 0;
                }
                continue;
            }
            field.append(c);
        }

        if (inQuotes) throw new JSONException("Mismatched quotes in the string");
        return out;
    }

    private static boolean isStockSession() {
        if (Boolean.getBoolean(SKIP_MOD_CHECK_PROPERTY)) return true;
        int current = stockSession;
        if (current != 0) return current > 0;
        synchronized (BrowserFastCsvParser.class) {
            current = stockSession;
            if (current != 0) return current > 0;
            try {
                List<ModManager.ModSpec> enabled = ModManager.getInstance().getEnabledMods();
                current = enabled == null || enabled.isEmpty() ? 1 : -1;
                stockSession = current;
                if (current < 0 && MOD_FALLBACK_LOGGED.compareAndSet(false, true)) {
                    System.out.println("BrowserFastCsvParser: stock fast path disabled because external mods are enabled count=" + enabled.size());
                }
                return current > 0;
            } catch (Throwable error) {
                stockSession = -1;
                if (MOD_FALLBACK_LOGGED.compareAndSet(false, true)) {
                    System.out.println("BrowserFastCsvParser: stock fast path disabled after mod check failure: " + describe(error));
                }
                return false;
            }
        }
    }

    private static String describe(Throwable error) {
        if (error == null) return "unknown";
        String message = error.getMessage();
        return error.getClass().getName() + (message == null || message.isEmpty() ? "" : ": " + message);
    }
}
