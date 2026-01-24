import subprocess
import os

mvn_path = r"C:\Users\adyba\ss2-wasm\build	ools\apache-maven-3.9.6\bin\mvn.cmd"
java_home = r"C:\Users\adyba\ss2-wasm\build	ools\jdk11_win\jdk-11.0.21+9"

env = os.environ.copy()
env["JAVA_HOME"] = java_home

base_jar_dir = r"C:\Users\adyba\ss2-wasm\sources\starsector\starsector"

jars = [
    (os.path.join(base_jar_dir, "fs.common_obf.jar"), "com.fs", "fs-common", "0.97a"),
    (os.path.join(base_jar_dir, "starfarer_obf.jar"), "com.fs", "starfarer-obf", "0.97a"),
    (os.path.join(base_jar_dir, "starfarer.api.jar"), "com.fs", "starfarer-api", "0.97a"),
    (os.path.join(base_jar_dir, "lwjgl.jar"), "org.lwjgl", "lwjgl", "2.9.3"),
    (os.path.join(base_jar_dir, "json.jar"), "org.json", "json", "20090211"),
    (os.path.join(base_jar_dir, "log4j-1.2.9.jar"), "log4j", "log4j", "1.2.9"),
]

for jar_path, gid, aid, ver in jars:
    cmd = f'"{mvn_path}" install:install-file -Dfile="{jar_path}" -DgroupId={gid} -DartifactId={aid} -Dversion={ver} -Dpackaging=jar'
    print(f"Installing {aid}...")
    subprocess.run(cmd, env=env, shell=True)

print("Installation complete.")
