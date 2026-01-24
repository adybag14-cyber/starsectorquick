import os
import re

path = "starsector-gwt/src/main/java/com/fs/starfarer/api/util/Misc.java"
if os.path.exists(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove imports
    content = content.replace("import sun.misc.Cleaner;", "")
    content = content.replace("import sun.nio.ch.DirectBuffer;", "")
    
    # Replace cleanBuffer method with regex
    pattern = r'public static void cleanBuffer\(Buffer toBeDestroyed\) \{.*?\}'
    content = re.sub(pattern, 'public static void cleanBuffer(Buffer toBeDestroyed) {}', content, flags=re.DOTALL)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Misc.java patched.")
else:
    print("Misc.java not found.")
