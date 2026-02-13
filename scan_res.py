import subprocess
import os

jar_path = 'jars/fs.common_obf.jar'
# Find the long class name again
res = subprocess.check_output(['jar', 'tf', jar_path], text=True)
long_name = None
for line in res.splitlines():
    if 'com/fs/util/oo' in line and line.endswith('.class') and '$' not in line:
        long_name = line.replace('.class', '').replace('/', '.')
        break

if long_name:
    print(f"Scanning {long_name[:50]}...")
    res = subprocess.check_output(['javap', '-p', '-cp', jar_path, long_name], text=True)
    for line in res.splitlines():
        if '(' in line:
            encoded = line.encode('unicode_escape').decode('ascii')
            print(f"Method: {encoded}")
else:
    print("Could not find long name")
