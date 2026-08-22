#!/usr/bin/env python3
from pathlib import Path
p=Path("ci/campaign-render-test.js")
s=p.read_text(encoding="utf-8")
if "inputQueryTelemetryPresent:" not in s:
    anchor="    const perfAfter = await page.evaluate(() => ({ ...(window.__lwjglPresentationStats || {}) }));\n"
    assert anchor in s
    s=s.replace(anchor,anchor+"    const inputPerfAfter = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));\n",1)
    anchor="      immediateColorAttribDeferredObserved: Boolean(perfAfter.immediateColorAttribDeferredObserved),\n"
    assert anchor in s
    extra=(
      "      inputQueryTelemetryPresent: Object.prototype.hasOwnProperty.call(inputPerfAfter, 'inputQueryTelemetryActive'),\n"
      "      inputQueryTelemetryActive: Boolean(inputPerfAfter.inputQueryTelemetryActive),\n"
      "      inputKeyboardStateQueries: Number(inputPerfAfter.keyboardStateQueries || 0),\n"
      "      inputKeyboardPressedQueries: Number(inputPerfAfter.keyboardPressedQueries || 0),\n"
      "      inputMouseButtonQueries: Number(inputPerfAfter.mouseButtonQueries || 0),\n"
      "      inputMousePressedQueries: Number(inputPerfAfter.mousePressedQueries || 0),\n"
      "      inputMousePositionQueries: Number(inputPerfAfter.mousePositionQueries || 0),\n"
    )
    s=s.replace(anchor,anchor+extra,1)
p.write_text(s,encoding="utf-8",newline="\n")
print("Applied input query telemetry benchmark fields")
