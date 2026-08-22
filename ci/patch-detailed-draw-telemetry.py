#!/usr/bin/env python3
from pathlib import Path
p=Path("ci/campaign-render-test.js")
s=p.read_text(encoding="utf-8")
marker="detailedDrawTelemetryPresent: Object.prototype.hasOwnProperty.call(perfAfter, 'detailedDrawTelemetryActive'),\n      detailedDrawTelemetryActive: Boolean(perfAfter.detailedDrawTelemetryActive),"
if marker not in s:
    anchor="      immediateColorAttribDeferredObserved: Boolean(perfAfter.immediateColorAttribDeferredObserved),\n"
    assert anchor in s
    s=s.replace(anchor,anchor+"      "+marker+"\n",1)
p.write_text(s,encoding="utf-8",newline="\n")
print("Applied detailed draw telemetry opt-out result telemetry")
