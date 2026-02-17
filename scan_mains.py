import subprocess
import os

jar_path = 'jars/starfarer_obf.jar'
class_name = 'com.fs.starfarer.settings.StarfarerSettings'

res = subprocess.check_output(['javap', '-p', '-cp', jar_path, class_name], text=True)
for line in res.splitlines():
    if 'static void' in line:
        encoded = line.encode('unicode_escape').decode('ascii')
        print(f"Method: {encoded}")