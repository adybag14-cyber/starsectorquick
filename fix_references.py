import os
import re

keywords = ["class", "int", "float", "new", "super", "return", "while", "for", "if", "void", "null", "public", "private", "protected", "static", "final", "native", "synchronized", "transient", "volatile", "strictfp", "byte", "short", "long", "char", "boolean", "do", "else", "switch", "case", "default", "break", "continue", "try", "catch", "finally", "throw", "throws", "import", "package", "instanceof", "extends", "implements", "this", "true", "false", "Object", "String"]

root_dir = "starsector-gwt/src/main/java"

# We want to replace things like:
# .class with ._class
# .int with ._int
# com.fs.util.String with com.fs.util._String
# import ...class; with import ..._class;

# Build patterns
# 1. Package components: .word -> ._word
# 2. Imports: .word; -> ._word;
# 3. Full class references in code: word var -> _word var (tricky)

for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            new_content = content
            for kw in keywords:
                # Replace .kw\b with ._kw
                # This covers com.fs.kw and also this.kw (if kw was a field, but here they are classes)
                pattern = r'\.' + re.escape(kw) + r'\b'
                new_content = re.sub(pattern, '._' + kw, new_content)
                
                # Replace "import ... kw;"
                pattern_import = r'\bimport\s+([\w\.]+)\.' + re.escape(kw) + r'\s*;'
                new_content = re.sub(pattern_import, r'import \1._' + kw + ';', new_content)

            if new_content != content:
                print(f"Updating references in {path}")
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(new_content)

print("Reference update complete.")
