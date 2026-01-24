import subprocess
import os

mvn_path = r"C:\Users\adyba\ss2-wasm\build	ools\apache-maven-3.9.6\bin\mvn.cmd"
java_home = r"C:\Users\adyba\ss2-wasm\build	ools\jdk11_win\jdk-11.0.21+9"

env = os.environ.copy()
env["JAVA_HOME"] = java_home

jars = [
    ("../sources/starsector/starsector/fs.common_obf.jar", "com.fs", "fs-common", "0.97a"),
    ("../sources/starsector/starsector/starfarer_obf.jar", "com.fs", "starfarer-obf", "0.97a"),
    ("../sources/starsector/starsector/starfarer.api.jar", "com.fs", "starfarer-api", "0.97a"),
    ("../sources/starsector/starsector/lwjgl.jar", "org.lwjgl", "lwjgl", "2.9.3"),
    ("../sources/starsector/starsector/json.jar", "org.json", "json", "20090211"),
    ("../sources/starsector/starsector/log4j-1.2.9.jar", "log4j", "log4j", "1.2.9"),
]

os.chdir("starsector-gwt")

for jar_path, gid, aid, ver in jars:
    cmd = [
        mvn_path, "install:install-file",
        f"-Dfile={jar_path}",
        f"-DgroupId={gid}",
        f"-DartifactId={aid}",
        f"-Dversion={ver}",
        "-Dpackaging=jar"
    ]
    print(f"Installing {aid}...")
    subprocess.run(cmd, env=env, shell=True)

print("Installation complete.")
