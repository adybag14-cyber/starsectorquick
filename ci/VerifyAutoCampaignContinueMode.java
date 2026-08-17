public final class VerifyAutoCampaignContinueMode {
    public static void main(String[] args) {
        require(false, Fixer.shouldPreferDirectNewGameBeforeContinue(true, "continue_direct"),
                "pure direct Continue must not be suppressed");
        require(true, Fixer.shouldPreferDirectNewGameBeforeContinue(true, "continue_direct_then_new"),
                "Continue+New direct fallback must preserve New-first preference");
        require(false, Fixer.shouldPreferDirectNewGameBeforeContinue(false, "continue_direct_then_new"),
                "non-direct launch must not force direct New");
        require(false, Fixer.shouldPreferDirectNewGameBeforeContinue(true, "new_direct"),
                "mode without Continue is not a Continue preference");
        require(false, Fixer.shouldPreferDirectNewGameBeforeContinue(true, null),
                "null mode must be safe");
        System.out.println("VerifyAutoCampaignContinueMode: OK");
    }

    private static void require(boolean expected, boolean actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
