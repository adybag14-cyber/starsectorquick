#!/usr/bin/env python3
"""
Script to replace String.format() calls with StringFormat.format() calls
for GWT compatibility
"""

import os
import re
from pathlib import Path

def fix_string_format_in_file(filepath):
    """Replace String.format() with StringFormat.format() in a file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Pattern to match String.format( with various arguments
        # This handles cases like:
        # String.format("%s", arg)
        # String.format("%.2f", value)
        # String.format("%d %s", 1, "test")
        pattern = r'\bString\.format\s*\('
        
        # Replace with StringFormat.format(
        content = re.sub(pattern, 'StringFormat.format(', content)
        
        # Only write if changes were made
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        
        return False
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def process_directory(directory):
    """Process all Java files in a directory"""
    java_files = list(Path(directory).rglob('*.java'))
    
    modified_count = 0
    total_count = len(java_files)
    
    print(f"Found {total_count} Java files to process...")
    
    for java_file in java_files:
        if fix_string_format_in_file(java_file):
            modified_count += 1
            print(f"  Modified: {java_file}")
    
    print(f"\nSummary:")
    print(f"  Total files processed: {total_count}")
    print(f"  Files modified: {modified_count}")
    print(f"  Files unchanged: {total_count - modified_count}")

if __name__ == '__main__':
    # Process the starsector-gwt source directory
    source_dir = 'starsector-gwt/src/main/java'
    
    if not os.path.exists(source_dir):
        print(f"Error: Directory '{source_dir}' not found")
        exit(1)
    
    print("Fixing String.format() calls for GWT compatibility...")
    print(f"Processing directory: {source_dir}\n")
    
    process_directory(source_dir)
    
    print("\nDone! String.format() calls have been replaced with StringFormat.format()")