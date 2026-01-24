import os
import re

# Keywords and common class names that might cause issues
keywords = ["class", "int", "float", "new", "super", "return", "while", "for", "if", "void", "null", "public", "private", "protected", "static", "final", "native", "synchronized", "transient", "volatile", "strictfp", "byte", "short", "long", "char", "boolean", "do", "else", "switch", "case", "default", "break", "continue", "try", "catch", "finally", "throw", "throws", "import", "package", "instanceof", "extends", "implements", "this", "true", "false", "Object", "String"]

root_dir = "starsector-gwt/src/main/java"

prefix = "RESERVED_"

# First, rename files and fix class declarations inside them
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            base_name = file[:-5]
            # Handle both already prefixed with _ and raw keywords
            actual_kw = base_name[1:] if base_name.startswith("_") and base_name[1:] in keywords else base_name
            
            if actual_kw in keywords:
                old_path = os.path.join(root, file)
                new_base_name = prefix + actual_kw
                new_file = new_base_name + ".java"
                new_path = os.path.join(root, new_file)
                
                print(f"Renaming {old_path} to {new_path}")
                
                with open(old_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                
                # Replace class/interface/enum declaration
                # Also replace constructors!
                new_content = content
                
                # Pattern for class/interface/enum
                for kind in ["class", "interface", "enum"]:
                    pattern = r'(\b' + kind + r'\s+)' + re.escape(base_name) + r'(\b)'
                    new_content = re.sub(pattern, r'\1' + new_base_name + r'\2', new_content)
                
                # Pattern for constructors (public _Object(...) { )
                # base_name might be "_Object" or "Object"
                pattern_cons = r'(\b)' + re.escape(base_name) + r'(\s*\()'
                new_content = re.sub(pattern_cons, r'\1' + new_base_name + r'\2', new_content)

                with open(new_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                
                if old_path != new_path:
                    os.remove(old_path)

# Second, update all references in all files
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            new_content = content
            for kw in keywords:
                # Replace ._kw or .kw with .RESERVED_kw
                pattern_dot = r'\.(\b_?' + re.escape(kw) + r'\b)'
                new_content = re.sub(pattern_dot, '.' + prefix + kw, new_content)
                
                # Replace import ..._kw or import ...kw
                pattern_import = r'\bimport\s+([\w\.]+)\.(_?' + re.escape(kw) + r')\s*;'
                new_content = re.sub(pattern_import, r'import \1.' + prefix + kw + ';', new_content)
                
                # Handle cases where the class is used without package prefix but was imported
                # This is dangerous as it might hit local variables. 
                # But in this codebase, these are mostly class names.
                # We'll stick to safer replacements for now.

            if new_content != content:
                # print(f"Updating references in {path}")
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(new_content)

print("Renaming and reference update complete.")
