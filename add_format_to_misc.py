import os

path = r"C:\Users\adyba\ss2-wasm\starsector-gwt\src\main\java\com\fs\starfarer\api\util\Misc.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

if "public static String format(" not in content:
    # Find the class declaration
    search_str = "public class Misc {"
    replacement = search_str + """
    public static String format(String format, Object... args) {
        if (args == null || args.length == 0) return format;
        String result = format;
        for (Object arg : args) {
            // Very simple replacement for stubs
            int pos = result.indexOf("%");
            if (pos >= 0 && pos < result.length() - 1) {
                result = result.substring(0, pos) + String.valueOf(arg) + result.substring(pos + 2);
            }
        }
        return result;
    }
"""
    content = content.replace(search_str, replacement)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("Misc.java updated with format method.")
else:
    print("Misc.java already has format method.")
