#!/usr/bin/env python3
"""Keep Fixer's mature ability repair out of stock browser tutorial mode."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
path = root / "jars" / "Fixer.java"
text = path.read_text(encoding="utf-8")

marker = "BROWSER_TUTORIAL_ABILITY_GUARD_V1"
if marker in text:
    print("Browser tutorial ability guard already present.")
    raise SystemExit(0)

old = '''    private static boolean ensureAutoCampaignStarterAbilities(Object sector, Object fleet) {
        if (sector == null || fleet == null) return false;
        final String[] abilityIds = new String[] {
'''
new = '''    private static boolean ensureAutoCampaignStarterAbilities(Object sector, Object fleet) {
        if (sector == null || fleet == null) return false;
        // BROWSER_TUTORIAL_ABILITY_GUARD_V1
        // NGCAddStandardStartingScript clears the mature ability bar before
        // CampaignTutorialScript begins and lets the tutorial grant/progress
        // abilities normally. The direct browser path installs that tutorial via
        // BrowserTutorialCompat, so do not immediately overwrite its state here.
        if (Boolean.parseBoolean(System.getProperty("starsector.browserTutorial", "false"))) {
            System.out.println(
                    "Fixer: browser tutorial active; preserving tutorial-managed ability slots.");
            return true;
        }
        final String[] abilityIds = new String[] {
'''

count = text.count(old)
if count != 1:
    raise RuntimeError(f"ensureAutoCampaignStarterAbilities anchor: expected 1 match, found {count}")
text = text.replace(old, new, 1)
path.write_text(text, encoding="utf-8", newline="\n")
print("Enabled browser tutorial ability guard in Fixer.java")
