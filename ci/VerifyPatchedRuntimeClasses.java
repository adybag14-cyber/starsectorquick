public final class VerifyPatchedRuntimeClasses {
    private VerifyPatchedRuntimeClasses() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("at least one class name is required");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String name : args) {
            Class<?> cls = Class.forName(name, false, loader);
            // Force method/constructor signature resolution as an additional check
            // without running static initializers or native-dependent game code.
            cls.getDeclaredConstructors();
            cls.getDeclaredMethods();
            System.out.println("Verified patched runtime class: " + name);
        }
    }
}
