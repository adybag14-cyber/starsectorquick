import os
import re

root_dir = "starsector-gwt/src/main/java"

def fix_file(path):
    # Try reading with Cp1252 first as it's common for decompiled code
    try:
        with open(path, 'r', encoding='cp1252') as f:
            content = f.read()
    except:
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()

    # 1. Fix single underscore identifier (illegal in Java 9+)
    # Match _ but not inside words
    content = re.sub(r'\b_\b', 'var_underscore', content)

    # 2. Fix botched constructor renames
    # If file is RESERVED_Name.java, ensures class name and constructor are RESERVED_Name
    base_name = os.path.basename(path)[:-5]
    if base_name.startswith("RESERVED_"):
        actual_name = base_name[9:] # strip RESERVED_
        # Look for the old class name or botched constructor
        # This is tricky. Let's look for "public/protected/private Name(" and replace with "public RESERVED_Name("
        pattern = r'(\b(public|protected|private)\s+)' + re.escape(actual_name) + r'(\s*\()'
        content = re.sub(pattern, r'\1' + base_name + r'\3', content)
        
        # Also ensure "class Name" is "class RESERVED_Name"
        pattern_class = r'(\b(class|interface|enum)\s+)' + re.escape(actual_name) + r'(\b)'
        content = re.sub(pattern_class, r'\1' + base_name + r'\3', content)

    # Write back as UTF-8
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            fix_file(os.path.join(root, file))

print("Cleanup complete.")
