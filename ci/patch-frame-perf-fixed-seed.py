#!/usr/bin/env python3
from pathlib import Path

SEED = "SEK968276040"

test = Path("ci/campaign-render-test.js")
s = test.read_text(encoding="utf-8")
if "__STARSECTOR_AUTO_CAMPAIGN_SEED_STRING__" not in s:
    anchor = "    __STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__: deepGameplay ? 'Corvus' : 'Galatia',\n"
    if s.count(anchor) != 1:
        raise SystemExit("campaign seed insertion anchor mismatch")
    s = s.replace(anchor, anchor + f"    __STARSECTOR_AUTO_CAMPAIGN_SEED_STRING__: '{SEED}',\n", 1)
    test.write_text(s, encoding="utf-8", newline="\n")

launch = Path("launch.html")
s = launch.read_text(encoding="utf-8")
if "const autoCampaignSeedString" not in s:
    anchor = """                const autoCampaignVariant = (\n                    window.__STARSECTOR_AUTO_CAMPAIGN_START_VARIANT__ || 'lasher_Standard'\n                ).toString().trim();\n"""
    insert = anchor + """                const autoCampaignSeedString = (\n                    window.__STARSECTOR_AUTO_CAMPAIGN_SEED_STRING__ || ''\n                ).toString().trim();\n"""
    if s.count(anchor) != 1:
        raise SystemExit("launcher seed variable anchor mismatch")
    s = s.replace(anchor, insert, 1)
    prop_anchor = """                    javaProperties.push(`-Dstarsector.autoCampaignStartVariant=${autoCampaignVariant}`);\n                    javaProperties.push(`starsector.autoCampaignStartVariant=${autoCampaignVariant}`);\n"""
    prop_insert = prop_anchor + """                    if (autoCampaignSeedString) {\n                        javaProperties.push(`-Dstarsector.autoCampaignSeedString=${autoCampaignSeedString}`);\n                        javaProperties.push(`starsector.autoCampaignSeedString=${autoCampaignSeedString}`);\n                    }\n"""
    if s.count(prop_anchor) != 1:
        raise SystemExit("launcher seed property anchor mismatch")
    s = s.replace(prop_anchor, prop_insert, 1)
    launch.write_text(s, encoding="utf-8", newline="\n")

print(f"Pinned browser campaign benchmark seed to {SEED}")
