import os
import re

root_dir = r"starsector-gwt/src/main/java"

def fix_botched_clones(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
    
    # Match the botched pattern I introduced
    pattern = r"((?:public|protected)\s+\w+\s+clone\(\)\s*\{)\s*return\s+null;\s*//\s*GWT\s*hack\s*\}\s*catch\s*\(CloneNotSupportedException\s+e\)\s*\{\s*return\s+null;\s*\}\s*\}"
    
    def replace_clone(match):
        header = match.group(1)
        return header + "
		return null; // GWT hack
	}"

    new_content = re.sub(pattern, replace_clone, content, flags=re.MULTILINE)
    
    if new_content != content:
        with open(path, "w", encoding="utf-8") as f:
            f.write(new_content)
        return True
    return False

count = 0
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            if fix_botched_clones(os.path.join(root, file)):
                count += 1

print(f"Fixed {count} botched files.")
