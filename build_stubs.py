import subprocess
import os

code = '''package org.apache.log4j;
public class FileAppender extends org.apache.log4j.WriterAppender {
    public FileAppender() {}
    public FileAppender(org.apache.log4j.Layout layout, String filename, boolean append) {}
    public FileAppender(org.apache.log4j.Layout layout, String filename) {}
    public void append(org.apache.log4j.spi.LoggingEvent event) {}
    public void setFile(String file) {}
}'''

os.makedirs('org/apache/log4j', exist_ok=True)
with open('org/apache/log4j/FileAppender.java', 'w') as f:
    f.write(code)

jar_path = os.path.abspath('jars/log4j-1.2.9.jar')
subprocess.run(['javac', '-cp', jar_path, 'org/apache/log4j/FileAppender.java'], check=True)
subprocess.run(['jar', 'cvf', 'jars/stubs.jar', 'org/apache/log4j/FileAppender.class'], check=True)
print("stubs.jar created successfully")
