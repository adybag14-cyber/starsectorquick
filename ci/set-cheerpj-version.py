#!/usr/bin/env python3
import os
from pathlib import Path

version = os.environ.get("CHEERPJ_JAVA_VERSION", "8").strip()
if version not in {"8", "11", "17"}:
    raise RuntimeError(f"Unsupported CheerpJ Java version: {version}")

path = Path(__file__).resolve().parents[1] / "launch.html"
text = path.read_text(encoding="utf-8")
old = "                    version: 17,"
new = f"                    version: {version},"
count = text.count(old)
if count != 1:
    raise RuntimeError(f"CheerpJ version field: expected one Java 17 match, found {count}")
text = text.replace(old, new, 1)
text = text.replace(
    "Initializing CheerpJ 4.3 Runtime (Java 17 compatibility mode)...",
    f"Initializing CheerpJ 4.3 Runtime (Java {version} compatibility mode)...",
    1,
)
path.write_text(text, encoding="utf-8", newline="\n")
print(f"Configured CheerpJ Java runtime version={version}")
