public final class VerifyPatchedRuntimeClasses {
    private static final String CAMPAIGN_GAME_MANAGER =
            "com.fs.starfarer.campaign.save.CampaignGameManager";
    private static final String BASE_GAME_STATE =
            "com.fs.starfarer.BaseGameState";

    private VerifyPatchedRuntimeClasses() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("at least one class name is required");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String name : args) {
            try {
                Class<?> cls = Class.forName(name, false, loader);
                // Force method/constructor signature resolution as an additional check
                // without running static initializers or native-dependent game code.
                cls.getDeclaredConstructors();
                cls.getDeclaredMethods();
                System.out.println("Verified patched runtime class: " + name);
            } catch (ClassFormatError formatError) {
                // Two stock 0.98a-RC8 obfuscated classes contain identifiers that
                // HotSpot rejects before it can verify the transformed bytecode.
                // javap/ASM/CheerpJ can parse the original classes, and the bytecode
                // transforms themselves have exact-count structural assertions.
                // Keep this exception list exact; every other class/format error is
                // still a hard CI failure.
                String message = formatError.getMessage();
                if (CAMPAIGN_GAME_MANAGER.equals(name)
                        && message != null
                        && message.contains("Illegal method name \"do.new\"")) {
                    System.out.println(
                            "Skipped HotSpot load verification for stock-obfuscated CampaignGameManager: "
                                    + message);
                    continue;
                }
                if (BASE_GAME_STATE.equals(name)
                        && message != null
                        && message.contains("Illegal field name \"while.return\"")) {
                    System.out.println(
                            "Skipped HotSpot load verification for stock-obfuscated BaseGameState: "
                                    + message);
                    continue;
                }
                throw formatError;
            }
        }
    }
}
