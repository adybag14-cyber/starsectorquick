public final class VerifyPatchedRuntimeClasses {
    private static final String CAMPAIGN_GAME_MANAGER =
            "com.fs.starfarer.campaign.save.CampaignGameManager";

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
                // The stock 0.98a-RC8 CampaignGameManager contains the obfuscated
                // method name `do.new`. HotSpot rejects that identifier before it
                // can verify the transformed bytecode, while javap/ASM/CheerpJ can
                // parse the original class. Do not turn this pre-existing stock
                // incompatibility into a false CI failure. Keep every other class
                // and every other ClassFormatError strict.
                String message = formatError.getMessage();
                if (CAMPAIGN_GAME_MANAGER.equals(name)
                        && message != null
                        && message.contains("Illegal method name \"do.new\"")) {
                    System.out.println(
                            "Skipped HotSpot load verification for stock-obfuscated CampaignGameManager: "
                                    + message);
                    continue;
                }
                throw formatError;
            }
        }
    }
}
