import os
import re

keywords = ["class", "int", "float", "new", "super", "return", "while", "for", "if", "void", "null", "public", "private", "protected", "static", "final", "native", "synchronized", "transient", "volatile", "strictfp", "byte", "short", "long", "char", "boolean", "do", "else", "switch", "case", "default", "break", "continue", "try", "catch", "finally", "throw", "throws", "import", "package", "instanceof", "extends", "implements", "this", "true", "false", "Object", "String"]

root_dir = "starsector-gwt/src/main/java"

for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            base_name = file[:-5]
            if base_name in keywords:
                old_path = os.path.join(root, file)
                new_base_name = "_" + base_name
                new_file = new_base_name + ".java"
                new_path = os.path.join(root, new_file)
                
                print(f"Renaming {old_path} to {new_path}")
                
                # Read content
                with open(old_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                
                # Replace class declaration
                # Look for "public class class" or "class class" etc.
                # Use regex to be safe
                pattern = r'(\bclass\s+)' + re.escape(base_name) + r'(\b)'
                new_content = re.sub(pattern, r'\1' + new_base_name + r'\2', content)
                
                # Also replace interface/enum if any
                pattern_int = r'(\binterface\s+)' + re.escape(base_name) + r'(\b)'
                new_content = re.sub(pattern_int, r'\1' + new_base_name + r'\2', new_content)
                
                pattern_enum = r'(\benum\s+)' + re.escape(base_name) + r'(\b)'
                new_content = re.sub(pattern_enum, r'\1' + new_base_name + r'\2', new_content)

                # Write new content
                with open(new_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                
                # Remove old file
                os.remove(old_path)

print("Renaming complete.")
