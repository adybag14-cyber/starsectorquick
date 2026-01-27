import zipfile
import os
import shutil

jars = ['starsectorquick/jars/fs.common_obf.jar', 'starsectorquick/jars/starfarer_obf.jar', 'starsectorquick/jars/resources.jar']

for jar_path in jars:
    if not os.path.exists(jar_path):
        print(f"Skipping {jar_path} (not found)")
        continue
        
    print(f"Processing {jar_path}")
    temp_path = jar_path + '.tmp'
    removed = False
    try:
        with zipfile.ZipFile(jar_path, 'r') as zin:
            with zipfile.ZipFile(temp_path, 'w') as zout:
                for item in zin.infolist():
                    if item.filename != 'log4j.properties':
                        zout.writestr(item, zin.read(item.filename))
                    else:
                        print(f"  Removed log4j.properties from {jar_path}")
                        removed = True
        
        if removed:
            shutil.move(temp_path, jar_path)
            print(f"Updated {jar_path}")
        else:
            os.remove(temp_path)
            print(f"No changes to {jar_path}")
            
    except Exception as e:
        print(f"Error processing {jar_path}: {e}")
        if os.path.exists(temp_path):
            os.remove(temp_path)
