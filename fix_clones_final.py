import os
import re

root_dir = r"starsector-gwt/src/main/java"

def fix_clones(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
    
    # Match public/protected Type clone() { ... }
    # We use a simple but effective regex for the common pattern
    pattern = r"((?:public|protected)\s+\w+\s+clone\(\)\s*\{)([\s\S]*?)(\n\s*\})"
    
    def replace_clone(match):
        header = match.group(1)
        footer = match.group(3)
        return header + "\n\t\treturn null; // GWT hack" + footer

    new_content = re.sub(pattern, replace_clone, content)
    
    if new_content != content:
        with open(path, "w", encoding="utf-8") as f:
            f.write(new_content)
        return True
    return False

count = 0
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            if fix_clones(os.path.join(root, file)):
                count += 1

print(f"Fixed {count} files.")