
public class Fixer {
    public static void main(String[] args) throws Exception {
        System.out.println("Fixer: Loading NPE fix library...");
        try { 
            System.loadLibrary("npefix"); 
            System.out.println("Fixer: Library loaded.");
        } catch (Throwable t) { 
            System.out.println("Fixer: Failed to load library: " + t);
            t.printStackTrace(); 
        }
        System.out.println("Fixer: Launching StarfarerLauncher...");
        com.fs.starfarer.StarfarerLauncher.main(args);
    }
}
