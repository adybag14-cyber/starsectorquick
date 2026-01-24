import os

path = r"C:\Users\adyba\ss2-wasm\starsector-gwt\src\main\java\com\fs\starfarer\api\util\Misc.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

if "public static String quote(" not in content:
    search_str = "public class Misc {"
    replacement = search_str + """
    public static String quote(String s) {
        if (s == null) return null;
        return s.replace("", "").replace(".", "\.").replace("[", "\[").replace("]", "\]");
    }
"""
    content = content.replace(search_str, replacement)
    
    # Also replace Pattern.quote
    content = content.replace("Pattern.quote(", "Misc.quote(")
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("Misc.java updated with quote method and Pattern.quote replaced.")
else:
    print("Misc.java already has quote method.")
