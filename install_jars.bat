@echo off
set JAVA_HOME=C:\Users\adyba\ss2-wasm\build	ools\jdk11_win\jdk-11.0.21+9
set MVN=C:\Users\adyba\ss2-wasm\build	ools\apache-maven-3.9.6\bin\mvn.cmd
set JAR_DIR=C:\Users\adyba\ss2-wasm\sources\starsector\starsector

call "%MVN%" install:install-file -Dfile="%JAR_DIR%\fs.common_obf.jar" -DgroupId=com.fs -DartifactId=fs-common -Dversion=0.97a -Dpackaging=jar
call "%MVN%" install:install-file -Dfile="%JAR_DIR%\starfarer_obf.jar" -DgroupId=com.fs -DartifactId=starfarer-obf -Dversion=0.97a -Dpackaging=jar
call "%MVN%" install:install-file -Dfile="%JAR_DIR%\starfarer.api.jar" -DgroupId=com.fs -DartifactId=starfarer-api -Dversion=0.97a -Dpackaging=jar
call "%MVN%" install:install-file -Dfile="%JAR_DIR%\lwjgl.jar" -DgroupId=org.lwjgl -DartifactId=lwjgl -Dversion=2.9.3 -Dpackaging=jar
call "%MVN%" install:install-file -Dfile="%JAR_DIR%\json.jar" -DgroupId=org.json -DartifactId=json -Dversion=20090211 -Dpackaging=jar
call "%MVN%" install:install-file -Dfile="%JAR_DIR%\log4j-1.2.9.jar" -DgroupId=log4j -DartifactId=log4j -Dversion=1.2.9 -Dpackaging=jar
