#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "jars" / "Fixer.java"
text = path.read_text(encoding="utf-8")

anchor = '''        Object resolvedTitleState = resolveTitleStateForTransition(ctx, titleState);\n        maybePrepareUiForCampaignTransition();\n        Object beforeState = readCurrentStateFromDriver(ctx);'''
replacement = '''        Object resolvedTitleState = resolveTitleStateForTransition(ctx, titleState);\n        maybePrepareUiForCampaignTransition();\n        if (resolvedTitleState != null) {\n            final String transitionKey = "starsector.pendingStateTransition";\n            String pendingTransition = System.getProperty(transitionKey);\n            if (pendingTransition == null || pendingTransition.length() == 0) {\n                System.setProperty(transitionKey, CAMPAIGN_STATE_ID);\n                System.out.println(\n                        "Fixer: queued Campaign State transition for the AppDriver render thread.");\n            }\n            return true;\n        }\n        Object beforeState = readCurrentStateFromDriver(ctx);'''

count = text.count(anchor)
if count != 1:
    raise RuntimeError(f"main-thread transition queue anchor: expected 1 match, found {count}")
text = text.replace(anchor, replacement, 1)
path.write_text(text, encoding="utf-8", newline="\n")
print("Queued campaign transitions through the AppDriver render thread bridge.")
