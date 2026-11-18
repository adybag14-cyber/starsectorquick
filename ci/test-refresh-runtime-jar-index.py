#!/usr/bin/env python3
"""Check post-build JAR index metadata using tiny, isolated fixture files."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
TRACKED = (
    "bridge.jar", "fixer_patch.jar", "fs.common_obf.jar",
    "starfarer.api.jar", "starfarer_obf.jar", "scripts-precompiled.jar",
)


class RuntimeJarIndexTest(unittest.TestCase):
    def setUp(self):
        output = ROOT / "test_output"
        output.mkdir(exist_ok=True)
        self.temporary = tempfile.TemporaryDirectory(prefix="jar-index-contract-", dir=output)
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / "ci").mkdir()
        (self.root / "jars").mkdir()
        self.script = self.root / "ci" / "refresh-runtime-jar-index.py"
        shutil.copyfile(ROOT / "ci" / self.script.name, self.script)
        self.index = self.root / "jars" / "index.list"
        self.sizes = {}
        for i, name in enumerate(TRACKED):
            body = bytes([65 + i]) * (17 + i * 3)
            (self.root / "jars" / name).write_bytes(body)
            self.sizes[name] = len(body)
        self.original = "\n".join(name + "\t1" for name in TRACKED) + "\nuntouched.jar\t123\n"
        self.index.write_text(self.original, encoding="utf-8", newline="\n")

    def run_refresh(self):
        return subprocess.run(
            [sys.executable, str(self.script)],
            cwd=self.root, text=True, capture_output=True,
        )

    def test_rebuilt_bridge_and_other_sizes_are_refreshed(self):
        result = self.run_refresh()
        self.assertEqual(result.returncode, 0, result.stderr)
        expected = "".join(name + "\t" + str(self.sizes[name]) + "\n" for name in TRACKED)
        expected += "untouched.jar\t123\n"
        self.assertEqual(self.index.read_bytes(), expected.encode("utf-8"))
        self.assertIn("bridge.jar", result.stdout)
        # Repeating the preparation must not duplicate or reorder metadata.
        self.assertEqual(self.run_refresh().returncode, 0)
        self.assertEqual(self.index.read_bytes(), expected.encode("utf-8"))

    def test_missing_bridge_index_entry_is_rejected_without_partial_write(self):
        original = self.original.replace("bridge.jar\t1\n", "")
        self.index.write_text(original, encoding="utf-8", newline="\n")
        result = self.run_refresh()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("runtime JARs missing from jars/index.list: bridge.jar", result.stderr)
        self.assertEqual(self.index.read_bytes(), original.encode("utf-8"))

    def test_missing_rebuilt_bridge_is_rejected_without_partial_write(self):
        (self.root / "jars" / "bridge.jar").unlink()
        result = self.run_refresh()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("missing transformed runtime JAR: bridge.jar", result.stderr)
        self.assertEqual(self.index.read_bytes(), self.original.encode("utf-8"))


if __name__ == "__main__":
    unittest.main()
