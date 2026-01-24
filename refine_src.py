import os
import re

root_dir = "starsector-gwt/src/main/java"

# Only rename if it looks like a class reference, not a field/method access
# Bad: obj.RESERVED_class
# Good: RESERVED_class.method() or import com.fs.RESERVED_class;

def refine_references(path):
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    new_content = content
    
    # 1. Revert .RESERVED_class to .class (often used for literals or fields)
    # Actually, .class is a keyword in Java (Class literal). 
    # Decompiled code might have .class or .class()
    # Let's see common patterns in errors:
    # cannot find symbol variable RESERVED_class location class ...
    
    # Revert .RESERVED_class -> .class
    new_content = new_content.replace(".RESERVED_class", ".class")
    new_content = new_content.replace(".RESERVED_this", ".this")
    new_content = new_content.replace(".RESERVED_super", ".super")
    new_content = new_content.replace(".RESERVED_new", ".new")
    
    # 2. Fix specific sun.misc.Cleaner issue (not in GWT anyway, but for compilation)
    # We might need to stub this too if it's used in API impl
    
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)

for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            refine_references(os.path.join(root, file))

print("Refinement complete.")
