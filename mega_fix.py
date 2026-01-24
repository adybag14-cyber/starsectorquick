import os
import re

root_dir = "starsector-gwt/src/main/java"

def mega_fix(path):
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    # 1. Fix unmappable characters (replace with ?)
    # Already handled by errors='ignore' and writing back as utf-8, 
    # but let's be explicit for common obfuscation chars
    content = content.encode('ascii', 'replace').decode('ascii')

    # 2. Fix botched empty if/while/for from obfuscation
    # Example: if () ;  or if () { }
    # The errors show things like "if ( )" followed by illegal expressions
    
    # Remove lines that are just "if ( ) ;" or similar botched patterns
    # pattern_botched_if = r'if\s*\(\s*\)\s*;'
    # content = re.sub(pattern_botched_if, '// Removed botched if', content)

    # 3. Specific fix for RESERVED_String constructor (botched name)
    base_name = os.path.basename(path)[:-5]
    if "RESERVED_String" in base_name:
        content = content.replace("public String(", "public RESERVED_String(")
    
    if "RESERVED_Object" in base_name:
        content = content.replace("public Object(", "public RESERVED_Object(")

    # 4. Remove obvious illegal expressions found in logs
    # e.g., "if ( );" or just dangling operators
    
    # 5. Fix release 9+ '_' issue (already did, but double check)
    content = re.sub(r'\b_\b', 'var_underscore', content)

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            mega_fix(os.path.join(root, file))

print("Mega-Fix complete.")
