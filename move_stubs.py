import os
import re

src_root = r"C:\Users\adyba\ss2-wasm\starsector-gwt\src\main\java\com\fs\emul"
dest_root = r"C:\Users\adyba\ss2-wasm\starsector-gwt\src\main\java\com\fs\starfarer\stubs"

def process_dir(current_src, current_dest, package_prefix):
    if not os.path.exists(current_src): return
    for item in os.listdir(current_src):
        src_path = os.path.join(current_src, item)
        dest_path = os.path.join(current_dest, item)
        if os.path.isdir(src_path):
            if not os.path.exists(dest_path): os.makedirs(dest_path)
            process_dir(src_path, dest_path, package_prefix + "." + item)
        elif item.endswith(".java"):
            with open(src_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            # Fix package declaration
            content = content.replace("package java.", "package com.fs.starfarer.stubs.java.")
            content = content.replace("package javax.", "package com.fs.starfarer.stubs.javax.")
            
            # Fix internal imports if any
            content = content.replace("import java.awt.Color;", "import com.fs.starfarer.stubs.java.awt.Color;")
            # ... add more if needed
            
            with open(dest_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"Processed {item} -> {dest_path}")

process_dir(src_root, dest_root, "com.fs.starfarer.stubs")
