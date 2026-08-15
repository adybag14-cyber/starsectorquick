import com.fs.starfarer.campaign.rules.BrowserRuleDuplicateIndex;

/** Direct semantic checks for the optimized Rules duplicate-ID guard. */
public final class TestBrowserRuleDuplicateIndex {
    public static void main(String[] args) {
        System.setProperty("starsector.browserRuleDuplicateIndex", "false");
        BrowserRuleDuplicateIndex.begin();
        if (BrowserRuleDuplicateIndex.enabled()) throw new AssertionError("disabled property unexpectedly enabled index");
        if (BrowserRuleDuplicateIndex.deadVariableTrackingBypassEnabled()) throw new AssertionError("disabled property unexpectedly enabled traversal bypass");
        BrowserRuleDuplicateIndex.checkAndRecord("T", "id");
        BrowserRuleDuplicateIndex.checkAndRecord("T", "id");
        BrowserRuleDuplicateIndex.finish();

        System.setProperty("starsector.browserRuleDuplicateIndex", "true");
        BrowserRuleDuplicateIndex.begin();
        if (!BrowserRuleDuplicateIndex.enabled()) throw new AssertionError("enabled property did not create index");
        if (!BrowserRuleDuplicateIndex.deadVariableTrackingBypassEnabled()) throw new AssertionError("enabled property did not enable traversal bypass");
        BrowserRuleDuplicateIndex.checkAndRecord("T", "id");
        BrowserRuleDuplicateIndex.checkAndRecord("U", "id");
        BrowserRuleDuplicateIndex.checkAndRecord("T", "other");
        boolean duplicate = false;
        try {
            BrowserRuleDuplicateIndex.checkAndRecord("T", "id");
        } catch (RuntimeException expected) {
            duplicate = "Duplicate rule id:id".equals(expected.getMessage());
        }
        if (!duplicate) throw new AssertionError("duplicate trigger/id pair did not preserve stock exception");
        BrowserRuleDuplicateIndex.finish();
        if (BrowserRuleDuplicateIndex.enabled()) throw new AssertionError("finish did not clear per-load index");
        if (BrowserRuleDuplicateIndex.deadVariableTrackingBypassEnabled()) throw new AssertionError("finish did not clear traversal bypass state");
        System.out.println("TestBrowserRuleDuplicateIndex: OK disabled fallback, trigger isolation, duplicate exception, cleanup");
    }
}
