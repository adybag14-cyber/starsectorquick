import subprocess
import os

mvn_path = r"C:\Users\adyba\ss2-wasm\build	ools\apache-maven-3.9.6\bin\mvn.cmd"
java_home = r"C:\Users\adyba\ss2-wasm\build	ools\jdk11_win\jdk-11.0.21+9"

env = os.environ.copy()
env["JAVA_HOME"] = java_home

jars = [
    ("libs/starfarer_obf.jar", "com.fs", "starfarer-obf", "0.97a"),
    ("libs/starfarer.api.jar", "com.fs", "starfarer-api", "0.97a"),
    ("libs/lwjgl.jar", "org.lwjgl", "lwjgl", "2.9.3"),
    ("libs/json.jar", "org.json", "json", "20090211"),
    ("libs/log4j-1.2.9.jar", "log4j", "log4j", "1.2.9"),
    ("libs/fs.sound_obf.jar", "com.fs", "fs-sound", "0.97a"),
    ("libs/xstream-1.4.10.jar", "com.thoughtworks.xstream", "xstream", "1.4.10"),
]

os.chdir("starsector-gwt")

for jar_path, gid, aid, ver in jars:
    # Build command string manually with proper quotes for Windows shell
    cmd = f'"{mvn_path}" install:install-file "-Dfile={jar_path}" "-DgroupId={gid}" "-DartifactId={aid}" "-Dversion={ver}" "-Dpackaging=jar"'
    print(f"Executing: {cmd}")
    subprocess.run(cmd, env=env, shell=True)

print("Installation complete.")
