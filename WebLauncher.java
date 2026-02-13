import java.lang.reflect.Method;
import java.io.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.combat.CombatMain;
import org.json.JSONObject;

public class WebLauncher {
    public static void main(String[] args) {
        try {
            String resourcePath = (args.length > 0) ? args[0] : ".";
            System.out.println("WebLauncher: Starting definitively with path: " + resourcePath);
            
            if (args.length >= 3) {
                System.out.println("WebLauncher: Injecting configuration files...");
                
                // Use Starfarer's own relaxed JSON parser
                Class<?> loadingUtils = Class.forName("com.fs.starfarer.loading.LoadingUtils");
                Method parseJson = loadingUtils.getDeclaredMethod("super", String.class, InputStream.class);
                parseJson.setAccessible(true);
                
                InputStream is = new ByteArrayInputStream(args[1].getBytes("UTF-8"));
                JSONObject settings = (JSONObject) parseJson.invoke(null, "internal", is);
                
                System.out.println("WebLauncher: Patching missing settings keys...");
                patchSettings(settings);
                
                writeFile(resourcePath + "/data/config/settings.json", settings.toString(4));
                writeFile(resourcePath + "/log4j.properties", args[2]);
                System.out.println("WebLauncher: Files injected and patched.");
            }

            // Standard Init
            Class<?> resClass = Class.forName("com.fs.util.ooOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
            Object resManager = resClass.getDeclaredMethod("\u00d300000").invoke(null);
            resClass.getDeclaredMethod("\u00d500000").invoke(resManager);
            resClass.getDeclaredMethod("\u00d500000", String.class).invoke(resManager, resourcePath);

            Class<?> settingsClass = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
            Method settingsInit = settingsClass.getDeclaredMethod("private.Object");
            settingsInit.setAccessible(true);
            settingsInit.invoke(null);

            Method getSettings = settingsClass.getDeclaredMethod("\u00f4O0000");
            getSettings.setAccessible(true);
            SettingsAPI settings = (SettingsAPI) getSettings.invoke(null);
            Global.setSettings(settings);

            System.out.println("WebLauncher: Starting CombatMain...");
            CombatMain.main(new String[]{"1024x768", "false", "false"});
            
        } catch (Throwable t) {
            System.err.println("WebLauncher: CRITICAL FAILURE");
            t.printStackTrace();
        }
    }

    private static void patchSettings(JSONObject json) throws Exception {
        // Missing keys identified from stack traces and decompilation
        String[][] boolKeys = {
            {"autosaveEnabledByDefault", "true"},
            {"autosaveFeatureEnabled", "true"},
            {"requireTypingDeleteToDeleteSave", "true"},
            {"renderDHullOverlay", "true"},
            {"allowForceQuitInIronMode", "true"}
        };
        for (String[] k : boolKeys) {
            if (!json.has(k[0])) {
                json.put(k[0], Boolean.parseBoolean(k[1]));
                System.out.println("WebLauncher: Injected missing boolean [" + k[0] + "]");
            }
        }

        String[][] floatKeys = {
            {"autosaveDefaultIntervalMinutes", "15.0"},
            {"suppliesPerMarinePerDay", "0.1"},
            {"minFighterReplacementRate", "0.1"},
            {"defaultBattleSize", "300.0"}
        };
        for (String[] k : floatKeys) {
            if (!json.has(k[0])) {
                json.put(k[0], Double.parseDouble(k[1]));
                System.out.println("WebLauncher: Injected missing float [" + k[0] + "]");
            }
        }

        if (!json.has("graphics")) json.put("graphics", new JSONObject());
        if (!json.has("bonusXP")) json.put("bonusXP", new JSONObject());
    }

    private static void writeFile(String path, String content) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }
    }
}