#!/usr/bin/env python3
"""Verify real GL11 Java control flow with an independent native recording backend.

Only native declarations are replaced. Real direct ByteBuffers and MemoryUtil
addresses are used; the recorder reads native addresses through Unsafe. Compile
the unmodified production class separately as a native signature/build gate.
"""
from pathlib import Path
import argparse
import re
import subprocess
import os

parser=argparse.ArgumentParser()
parser.add_argument("--output",default="test_output/vertex-batch-java")
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
out=(root/args.output).resolve()
source=(root/"bridge_src/org/lwjgl/opengl/GL11.java").read_text(encoding="utf-8")
testsource=out/"src/org/lwjgl/opengl/GL11.java"
testsource.parent.mkdir(parents=True,exist_ok=True)
testclasses=out/"test-classes";testclasses.mkdir(exist_ok=True)
production=out/"production-classes";production.mkdir(exist_ok=True)
native_pattern=re.compile(r"static native (void|boolean|int|String) (\w+)\(([^)]*)\);")
seen=[]
def replace_native(match):
    returns,name,params=match.groups()
    names=[part.strip().split()[-1] for part in params.split(",") if part.strip()]
    seen.append(name)
    if name=="nglBegin": body="return VertexBatchJavaProbe.begin(p0);"
    elif name=="nglEnd": body="VertexBatchJavaProbe.end();"
    elif name=="nglAppendVertexBatch": body="VertexBatchJavaProbe.batch(address, count);"
    elif name=="nglVertex3fTexCoord": body="VertexBatchJavaProbe.vertex(p0,p1,p2,p3,p4);"
    elif name=="nglColor3f": body="VertexBatchJavaProbe.color(p0,p1,p2,1.0f);"
    elif name=="nglColor4f": body="VertexBatchJavaProbe.color(p0,p1,p2,p3);"
    else:
        body='VertexBatchJavaProbe.nativeEvent("'+name+'"'+(","+",".join(names) if names else "")+");"
        body+={"void":"","boolean":" return false;","int":" return 1;","String":' return "fixture";'}[returns]
    return "static "+returns+" "+name+"("+params+") { "+body+" }"
fixture=native_pattern.sub(replace_native,source)
assert "nglAppendVertexBatch" in seen and "nglBegin" in seen
assert "static native" not in fixture
testsource.write_text(fixture,encoding="utf-8",newline="\n")
cp=os.pathsep.join(str(root/"jars"/name) for name in ("bridge.jar","lwjgl.jar"))
javac=os.environ.get("JAVAC","javac")
java=os.environ.get("JAVA","java")
common=[javac,"-source","8","-target","8","-encoding","UTF-8","-classpath",cp]
subprocess.run(common+["-d",str(production),str(root/"bridge_src/org/lwjgl/opengl/GL11.java")],check=True)
subprocess.run(common+["-d",str(testclasses),str(testsource),str(root/"ci/VertexBatchJavaProbe.java")],check=True)
subprocess.run([java,"-classpath",str(testclasses)+os.pathsep+cp,"org.lwjgl.opengl.VertexBatchJavaProbe"],check=True)
print("Production native GL11 compiled:",production/"org/lwjgl/opengl/GL11.class")
