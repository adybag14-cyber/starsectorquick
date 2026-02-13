import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;

public class Fixer {
    private static final String LOGS_PROPERTY = "com.fs.starfarer.settings.paths.logs";
    private static final String SAVES_PROPERTY = "com.fs.starfarer.settings.paths.saves";
    private static final String SCREENSHOTS_PROPERTY = "com.fs.starfarer.settings.paths.screenshots";
    private static final String MODS_PROPERTY = "com.fs.starfarer.settings.paths.mods";
    private static final String LOGS_PATH = "/files/logs";
    private static final String SAVES_PATH = "/files/saves";
    private static final String SCREENSHOTS_PATH = "/files/screenshots";
    private static final String MODS_PATH = "/files/mods";
    private static final String DIRECT_LAUNCH_PROPERTY = "starsector.directLaunch";
    private static final String DISABLE_SHIP_HULL_POST_PASS_PROPERTY =
            "starsector.disableShipHullPostPass";
    private static final String PRELOAD_RULE_COMMANDS_PROPERTY = "starsector.preloadRuleCommands";
    private static final String SERIAL_PROPERTY = "starsector.serial";
    private static final String SERIAL_PREF_NODE = "/com/fs/starfarer";
    private static final String SERIAL_PREF_KEY = "serial";
    private static final String SERIAL_FILE_PATH = "/app/.starsector_serial";
    private static final String JAVA_VM_VENDOR_PROPERTY = "java.vm.vendor";
    private static final String JAVA_VENDOR_PROPERTY = "java.vendor";
    private static final String JAVA_VM_NAME_PROPERTY = "java.vm.name";
    private static final String JAVA_SPEC_VERSION_PROPERTY = "java.specification.version";
    private static final String JAVA_SPEC_VENDOR_PROPERTY = "java.specification.vendor";
    private static final String JAVA_SPEC_NAME_PROPERTY = "java.specification.name";
    private static final String DEFAULT_JAVA_VM_VENDOR = "CheerpJ";
    private static final String DEFAULT_JAVA_VENDOR = "CheerpJ";
    private static final String DEFAULT_JAVA_VM_NAME = "CheerpJ Runtime";
    private static final String DEFAULT_JAVA_SPEC_VERSION = "1.8";
    private static final String DEFAULT_JAVA_SPEC_VENDOR = "Oracle Corporation";
    private static final String DEFAULT_JAVA_SPEC_NAME = "Java Platform API Specification";
    private static final String BOOT_MODE_PROPERTY = "starsector.boot";
    private static final String SUPPRESS_STARTUP_PROMPTS_PROPERTY = "starsector.suppressStartupPrompts";
    private static final String FORCE_CONTROLS_VERSION_PROPERTY = "starsector.forceControlsVersion";
    private static final String DISABLE_LAUNCHER_WARNINGS_PROPERTY = "starsector.disableLauncherWarnings";
    private static final String AUTO_CAMPAIGN_PROPERTY = "starsector.autoCampaign";
    private static final String AUTO_CAMPAIGN_MODE_PROPERTY = "starsector.autoCampaignMode";
    private static final String AUTO_CAMPAIGN_START_VARIANT_PROPERTY =
            "starsector.autoCampaignStartVariant";
    private static final String AUTO_CAMPAIGN_TIMEOUT_MS_PROPERTY =
            "starsector.autoCampaignTimeoutMs";
    private static final String AUTO_CAMPAIGN_POLL_MS_PROPERTY = "starsector.autoCampaignPollMs";
    private static final String AUTO_CAMPAIGN_FALLBACK_MS_PROPERTY =
            "starsector.autoCampaignFallbackMs";
    private static final String AUTO_CAMPAIGN_CAPTAIN_NAME_PROPERTY =
            "starsector.autoCampaignCaptainName";
    private static final String AUTO_VISIT_COLONY_PROPERTY = "starsector.autoVisitColony";
    private static final String AUTO_VISIT_COLONY_NAME_PROPERTY = "starsector.autoVisitColonyName";
    private static final String AUTO_CAMPAIGN_SEED_STRING_PROPERTY =
            "starsector.autoCampaignSeedString";
    private static final String AUTO_CAMPAIGN_SECTOR_AGE_PROPERTY =
            "starsector.autoCampaignSectorAge";
    private static final String AUTO_CAMPAIGN_SECTOR_SIZE_PROPERTY =
            "starsector.autoCampaignSectorSize";
    private static final String AUTO_CAMPAIGN_STARTING_LOCATION_PROPERTY =
            "starsector.autoCampaignStartingLocation";
    private static final String AUTO_CAMPAIGN_START_X_PROPERTY =
            "starsector.autoCampaignStartX";
    private static final String AUTO_CAMPAIGN_START_Y_PROPERTY =
            "starsector.autoCampaignStartY";
    private static final String AUTO_CAMPAIGN_WITH_TIME_PASS_PROPERTY =
            "starsector.autoCampaignWithTimePass";
    private static final String AUTO_CAMPAIGN_MENU_FAILURE_LIMIT_PROPERTY =
            "starsector.autoCampaignMenuFailureLimit";
    private static final String AUTO_CAMPAIGN_DIRECT_FAILURE_LIMIT_PROPERTY =
            "starsector.autoCampaignDirectFailureLimit";
    private static final String AUTO_CAMPAIGN_FATAL_FAILURE_LIMIT_PROPERTY =
            "starsector.autoCampaignFatalFailureLimit";
    private static final String AUTO_CAMPAIGN_FATAL_SIGNATURE_LIMIT_PROPERTY =
            "starsector.autoCampaignFatalSignatureLimit";
    private static final String AUTO_CAMPAIGN_TITLE_SETTLE_MS_PROPERTY =
            "starsector.autoCampaignTitleSettleMs";
    private static final String AUTO_CAMPAIGN_ALLOW_DIRECT_ESCAPE_PROPERTY =
            "starsector.autoCampaignAllowDirectEscape";
    private static final String AUTO_CAMPAIGN_TITLE_RENDER_WARMUP_PROPERTY =
            "starsector.autoCampaignTitleRenderWarmup";
    private static final String AUTO_CAMPAIGN_DIRECT_NEW_GAME_COMMITTED_AT_PROPERTY =
            "starsector.autoCampaignDirectNewGameCommittedAt";
    private static final String AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_OWNER_PROPERTY =
            "starsector.autoCampaignDirectNewGameLeaseOwner";
    private static final String AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_AT_PROPERTY =
            "starsector.autoCampaignDirectNewGameLeaseAt";
    private static final String AUTO_CAMPAIGN_MUTATING_SPEC_PREFLIGHT_PROPERTY =
            "starsector.autoCampaignMutatingSpecPreflight";
    private static final String AUTO_CAMPAIGN_NON_MUTATING_ORBITAL_JUNK_FALLBACK_PROPERTY =
            "starsector.autoCampaignAllowOrbitalJunkFallbackWhenNonMutating";
    private static final String AUTO_CAMPAIGN_ORBITAL_SPEC_WATCHDOG_PROPERTY =
            "starsector.autoCampaignOrbitalSpecWatchdog";
    private static final String USER_DIR_OVERRIDE_PROPERTY = "starsector.userDir";
    private static final String SPEC_DIAGNOSTICS_PROPERTY = "starsector.specDiagnostics";
    private static final String CAMPAIGN_SESSION_KEY = "campaign state in session";
    private static final String TITLE_STATE_ID = "Title Screen State";
    private static final String CAMPAIGN_STATE_ID = "Campaign State";
    private static final String CONTROLS_VERSION_FALLBACK = "6.3";
    private static final String LOG4J_CONFIG_PROPERTY = "starsector.log4jConfig";
    private static final String LOG4J_FORCE_BASIC_PROPERTY = "starsector.forceBasicLog4j";
    private static final String DEFAULT_LOG4J_CONFIG = "file:/app/starsector/starsector/log4j.properties";
    private static final String APP_ROOT = "/app/starsector/starsector";
    private static final String FILES_ROOT = "/files";
    private static final int AUTO_CAMPAIGN_MENU_TRACE_LIMIT = 3;
    private static int autoCampaignMenuTraceCount = 0;
    private static boolean uiBorderSpriteSanityLogged = false;
    private static int uiBorderSpriteRepairAttempts = 0;
    private static boolean uiMenuNewGamePreflightReady = false;
    private static boolean campaignTransitionUiPreflightReady = false;
    private static long campaignTransitionUiPreflightLastAttemptAt = 0L;
    private static long campaignTransitionUiPreflightLastLogAt = 0L;
    private static String campaignTransitionUiPreflightLastIssue = null;
    private static long autoCampaignNewGamePreflightLogAt = 0L;
    private static String autoCampaignLastNewGamePreflightIssue = null;
    private static long autoCampaignCoreSpecBaselinePendingSince = 0L;
    private static int autoCampaignCoreSpecBaselinePendingCount = 0;
    private static final long AUTO_CAMPAIGN_FACTION_WARMUP_COOLDOWN_MS = 750L;
    private static final int AUTO_CAMPAIGN_FACTION_WARMUP_ATTEMPT_LIMIT = 240;
    private static long autoCampaignFactionWarmupAttemptAt = 0L;
    private static int autoCampaignFactionWarmupAttempts = 0;
    private static final Object AUTO_CAMPAIGN_WATCHER_LOCK = new Object();
    private static Thread autoCampaignWatcherThread;
    private static long autoCampaignWatcherSerial = 0L;
    private static volatile long autoCampaignDirectNewGameCommittedAt = -1L;
    private static final Object DIRECT_NEW_GAME_WORKER_LOCK = new Object();
    private static Thread directNewGameWorker;
    private static long directNewGameWorkerStartedAt = 0L;
    private static boolean directNewGameWorkerCompleted = false;
    private static String directNewGameWorkerResult;
    private static Throwable directNewGameWorkerError;
    private static int directNewGameWorkerId = 0;
    private static Object resourceManagerInstance;
    private static Method resourceManagerOpenResource;
    private static final Set<String> mirroredSpecPaths = new LinkedHashSet<String>();
    private static final class DriverContext {
        final Object driver;
        final Class<?> driverClass;
        final Object currentState;
        final ClassLoader loader;
        final Map states;
        final Object startStateId;
        final Map session;

        DriverContext(
                Object driver,
                Class<?> driverClass,
                Object currentState,
                ClassLoader loader,
                Map states,
                Object startStateId,
                Map session) {
            this.driver = driver;
            this.driverClass = driverClass;
            this.currentState = currentState;
            this.loader = loader;
            this.states = states;
            this.startStateId = startStateId;
            this.session = session;
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.includeInExceptions", "false");
        String resolvedUserDir = System.getProperty(USER_DIR_OVERRIDE_PROPERTY, APP_ROOT);
        if (resolvedUserDir == null || resolvedUserDir.trim().isEmpty()) {
            resolvedUserDir = APP_ROOT;
        } else {
            resolvedUserDir = resolvedUserDir.trim();
        }
        System.setProperty("user.dir", resolvedUserDir);
        configureJvmIdentityProperties();
        maybeOpenJavaBasePackagesForReflection();
        // Prefer the game's bundled log4j settings to avoid excessive startup logging.
        System.setProperty("log4j.defaultInitOverride", "false");
        System.setProperty(
                "log4j.configuration",
                System.getProperty(LOG4J_CONFIG_PROPERTY, DEFAULT_LOG4J_CONFIG));
        configureLog4jFallback();
        applySerialFromProperty();
        applyStartupPreferenceOverrides();
        maybeDisableLauncherWarnings();
        initializeInputBridges();

        configureFilesystemPaths();

        loadNpeFixNativeLibrary();

        initializeResourceManager();
        mirrorCoreSpecDirectoriesToFiles();
        installUncaughtExceptionLogging();
        maybePreloadRuleCommandClasses();
        if (Boolean.parseBoolean(System.getProperty("starsector.primeProjectiles", "false"))) {
            primeProjectileSpecs();
        }
        if (Boolean.parseBoolean(System.getProperty(SPEC_DIAGNOSTICS_PROPERTY, "false"))) {
            debugSpecDirectoryListing();
        }

        String startRes = System.getProperty("startRes", "1280x768");
        String[] resParts = startRes.split("x");
        String resX = (resParts.length > 0 && !resParts[0].isEmpty()) ? resParts[0] : "1280";
        String resY = (resParts.length > 1 && !resParts[1].isEmpty()) ? resParts[1] : "768";
        boolean startFS = Boolean.parseBoolean(System.getProperty("startFS", "false"));
        boolean startSound = !"false".equalsIgnoreCase(System.getProperty("startSound", "true"));
        boolean directLaunch = !"false".equalsIgnoreCase(System.getProperty(DIRECT_LAUNCH_PROPERTY, "true"));
        String bootMode = System.getProperty(BOOT_MODE_PROPERTY, "launcher");
        maybeDisableShipHullSpreadsheetPostPass();
        maybeStartAutoCampaignWatcher();

        System.out.println(
                "Fixer: Launch config direct="
                        + directLaunch
                        + " fs="
                        + startFS
                        + " sound="
                        + startSound
                        + " res="
                        + resX
                        + "x"
                        + resY
                        + " boot="
                        + bootMode
                        + " "
                        + MODS_PROPERTY
                        + "="
                        + System.getProperty(MODS_PROPERTY)
                        + ", user.dir="
                        + System.getProperty("user.dir"));

        if ("combat".equalsIgnoreCase(bootMode)) {
            try {
                System.out.println("Fixer: boot mode combat -> calling CombatMain.main directly");
                com.fs.starfarer.combat.CombatMain.main(new String[0]);
                return;
            } catch (Throwable t) {
                System.out.println("Fixer: direct CombatMain boot failed, falling back: " + t);
            }
        }

        if (directLaunch) {
            try {
                com.fs.starfarer.StarfarerLauncher.o00000(startFS, startSound, resX, resY);
                return;
            } catch (Throwable t) {
                System.out.println("Fixer: direct launch failed, falling back to launcher UI: " + t);
            }
        }

        System.out.println("Fixer: Launching StarfarerLauncher UI path.");
        com.fs.starfarer.StarfarerLauncher.main(args == null ? new String[0] : args);
    }

    private static void loadNpeFixNativeLibrary() {
        List<String> failures = new ArrayList<String>();
        try {
            System.loadLibrary("npefix");
            System.out.println("Fixer: npefix loaded via System.loadLibrary.");
            return;
        } catch (Throwable t) {
            failures.add("loadLibrary(npefix): " + t);
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<String>();
        candidates.add("/app/build/final/wasm-modules/libnpefix.so");
        candidates.add("/app/starsectorquick/build/final/wasm-modules/libnpefix.so");
        candidates.add("/app/starsector/starsector/build/final/wasm-modules/libnpefix.so");
        candidates.add("/files/libnpefix.so");

        String javaLibraryPath = System.getProperty("java.library.path", "");
        if (javaLibraryPath != null && javaLibraryPath.length() > 0) {
            String[] entries = javaLibraryPath.split(File.pathSeparator);
            for (String entry : entries) {
                if (entry == null) {
                    continue;
                }
                String trimmed = entry.trim();
                if (trimmed.length() == 0) {
                    continue;
                }
                if (trimmed.endsWith("/") || trimmed.endsWith("\\")) {
                    candidates.add(trimmed + "libnpefix.so");
                } else {
                    candidates.add(trimmed + "/libnpefix.so");
                }
            }
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.length() == 0) {
                continue;
            }
            try {
                File f = new File(candidate);
                if (!f.exists()) {
                    failures.add(candidate + ": missing");
                    continue;
                }
                System.load(f.getAbsolutePath());
                System.out.println(
                        "Fixer: npefix loaded via System.load path " + f.getAbsolutePath() + ".");
                return;
            } catch (Throwable t) {
                failures.add(candidate + ": " + t);
            }
        }

        System.out.println("Fixer: npefix unavailable after all load attempts.");
        int max = Math.min(12, failures.size());
        for (int i = 0; i < max; i++) {
            System.out.println("Fixer: npefix attempt " + (i + 1) + "/" + failures.size() + " -> " + failures.get(i));
        }
        if (failures.size() > max) {
            System.out.println("Fixer: npefix additional failures suppressed: " + (failures.size() - max));
        }
    }

    private static void maybeOpenJavaBasePackagesForReflection() {
        final String[] packages =
                new String[] {
                    "java.lang",
                    "java.lang.reflect",
                    "java.nio",
                    "java.util",
                    "java.text",
                    "java.time",
                    "java.time.chrono"
                };
        try {
            Method getModule = Class.class.getMethod("getModule");
            Object javaBaseModule = getModule.invoke(Object.class);
            Object fixerModule = getModule.invoke(Fixer.class);
            if (javaBaseModule == null || fixerModule == null) {
                return;
            }

            Class<?> moduleClass = Class.forName("java.lang.Module");
            if (tryOpenToAllUnnamed(moduleClass, javaBaseModule, packages)) {
                return;
            }
            if (tryOpenToModule(moduleClass, javaBaseModule, fixerModule, packages)) {
                return;
            }
            if (tryOpenViaInternalModules(moduleClass, javaBaseModule, fixerModule, packages)) {
                return;
            }
        } catch (Throwable t) {
            System.out.println("Fixer: module-open bootstrap skipped: " + t);
        }
    }

    private static boolean tryOpenToAllUnnamed(
            Class<?> moduleClass, Object sourceModule, String[] packages) {
        try {
            Method m = moduleClass.getDeclaredMethod("implAddOpensToAllUnnamed", String.class);
            m.setAccessible(true);
            int opened = 0;
            for (String pkg : packages) {
                try {
                    m.invoke(sourceModule, pkg);
                    opened++;
                } catch (Throwable ignored) {
                }
            }
            if (opened > 0) {
                System.out.println(
                        "Fixer: opened java.base packages to all unnamed modules (count=" + opened + ").");
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean tryOpenToModule(
            Class<?> moduleClass, Object sourceModule, Object targetModule, String[] packages) {
        try {
            Method m = moduleClass.getDeclaredMethod("implAddOpens", String.class, moduleClass);
            m.setAccessible(true);
            int opened = 0;
            for (String pkg : packages) {
                try {
                    m.invoke(sourceModule, pkg, targetModule);
                    opened++;
                } catch (Throwable ignored) {
                }
            }
            if (opened > 0) {
                System.out.println(
                        "Fixer: opened java.base packages to Fixer module (count=" + opened + ").");
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean tryOpenViaInternalModules(
            Class<?> moduleClass, Object sourceModule, Object targetModule, String[] packages) {
        try {
            Class<?> modulesClass = Class.forName("jdk.internal.module.Modules");
            Method addOpens =
                    modulesClass.getDeclaredMethod("addOpens", moduleClass, String.class, moduleClass);
            addOpens.setAccessible(true);
            int opened = 0;
            for (String pkg : packages) {
                try {
                    addOpens.invoke(null, sourceModule, pkg, targetModule);
                    opened++;
                } catch (Throwable ignored) {
                }
            }
            if (opened > 0) {
                System.out.println(
                        "Fixer: opened java.base packages via jdk.internal.module.Modules (count="
                                + opened
                                + ").");
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void initializeInputBridges() {
        try {
            org.lwjgl.input.Mouse.create();
            System.out.println("Fixer: Mouse input bridge initialized.");
        } catch (Throwable t) {
            System.out.println("Fixer: Mouse input bridge init failed: " + t);
        }

        try {
            org.lwjgl.input.Keyboard.create();
            org.lwjgl.input.Keyboard.enableRepeatEvents(true);
            System.out.println("Fixer: Keyboard input bridge initialized.");
        } catch (Throwable t) {
            System.out.println("Fixer: Keyboard input bridge init failed: " + t);
        }
    }

    private static void maybeStartAutoCampaignWatcher() {
        if (!Boolean.parseBoolean(System.getProperty(AUTO_CAMPAIGN_PROPERTY, "false"))) {
            return;
        }
        final String mode =
                System.getProperty(AUTO_CAMPAIGN_MODE_PROPERTY, "continue_then_new")
                        .trim()
                        .toLowerCase();
        final long timeoutMs = parseLongProperty(AUTO_CAMPAIGN_TIMEOUT_MS_PROPERTY, 900000L);
        final long pollMs = Math.max(100L, parseLongProperty(AUTO_CAMPAIGN_POLL_MS_PROPERTY, 500L));
        final long fallbackMs =
                Math.max(1500L, parseLongProperty(AUTO_CAMPAIGN_FALLBACK_MS_PROPERTY, 7000L));
        synchronized (AUTO_CAMPAIGN_WATCHER_LOCK) {
            if (autoCampaignWatcherThread != null && autoCampaignWatcherThread.isAlive()) {
                System.out.println(
                        "Fixer: auto campaign watcher already running (thread="
                                + autoCampaignWatcherThread.getName()
                                + "); skipping duplicate start.");
                return;
            }
            final long watcherSerial = ++autoCampaignWatcherSerial;
            autoCampaignWatcherThread =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        runAutoCampaignWatcher(mode, timeoutMs, pollMs, fallbackMs);
                                    } finally {
                                        synchronized (AUTO_CAMPAIGN_WATCHER_LOCK) {
                                            if (autoCampaignWatcherThread == Thread.currentThread()) {
                                                autoCampaignWatcherThread = null;
                                            }
                                        }
                                    }
                                }
                            },
                            "fixer-auto-campaign-" + watcherSerial);
            autoCampaignWatcherThread.setDaemon(true);
            autoCampaignWatcherThread.start();
            System.out.println(
                    "Fixer: auto campaign watcher started mode="
                            + mode
                            + " timeoutMs="
                            + timeoutMs
                            + " pollMs="
                            + pollMs
                            + " fallbackMs="
                            + fallbackMs
                            + " thread="
                            + autoCampaignWatcherThread.getName());
        }
    }

    private static void runAutoCampaignWatcher(
            String mode, long timeoutMs, long pollMs, long fallbackMs) {
        long startedAt = System.currentTimeMillis();
        final long watcherStartedAt = startedAt;
        final String watcherLeaseOwner =
                Thread.currentThread().getName() + "@" + String.valueOf(watcherStartedAt);
        String normalizedMode = mode == null ? "" : mode.toLowerCase();
        final int fatalMenuFailureLimit =
                Math.min(6, Math.max(1, parseIntProperty(AUTO_CAMPAIGN_MENU_FAILURE_LIMIT_PROPERTY, 2)));
        final int fatalDirectFailureLimit =
                Math.min(6, Math.max(1, parseIntProperty(AUTO_CAMPAIGN_DIRECT_FAILURE_LIMIT_PROPERTY, 2)));
        final int fatalFailureLimit =
                Math.min(24, Math.max(2, parseIntProperty(AUTO_CAMPAIGN_FATAL_FAILURE_LIMIT_PROPERTY, 10)));
        final int fatalSignatureLimit =
                Math.min(12, Math.max(1, parseIntProperty(AUTO_CAMPAIGN_FATAL_SIGNATURE_LIMIT_PROPERTY, 3)));
        final long titleSettleMs =
                Math.max(0L, parseLongProperty(AUTO_CAMPAIGN_TITLE_SETTLE_MS_PROPERTY, 25000L));
        final long directAttemptTimeoutMs =
                Math.max(
                        2000L,
                        parseLongProperty("starsector.autoCampaignDirectAttemptTimeoutMs", 12000L));
        final long continueStallMs =
                Math.max(
                        4000L,
                        parseLongProperty(
                                "starsector.autoCampaignContinueStallMs",
                                Math.max(12000L, fallbackMs * 2L)));
        final int retryableMenuSignatureLimit =
                Math.min(
                        24,
                        Math.max(
                                1,
                                parseIntProperty(
                                        "starsector.autoCampaignRetryableMenuSignatureLimit", 8)));
        final int retryableDirectSignatureLimit =
                Math.min(
                        24,
                        Math.max(
                                1,
                                parseIntProperty(
                                        "starsector.autoCampaignRetryableDirectSignatureLimit", 4)));
        final int directTimeoutFailureLimit =
                Math.min(
                        12,
                        Math.max(
                                1,
                                parseIntProperty(
                                        "starsector.autoCampaignDirectTimeoutFailureLimit", 3)));
        final int continueDisableFailureLimit =
                Math.min(
                        64,
                        Math.max(
                                3,
                                parseIntProperty(
                                        "starsector.autoCampaignContinueDisableFailureLimit",
                                        normalizedMode.indexOf("visit_colony") >= 0 ? 24 : 3)));
        final long directTransitionMinSettleMs =
                Math.max(
                        0L,
                        parseLongProperty(
                                "starsector.autoCampaignDirectTransitionMinSettleMs", 5000L));
        final long directTransitionForceAfterMs =
                Math.max(
                        directTransitionMinSettleMs + 5000L,
                        parseLongProperty(
                                "starsector.autoCampaignDirectTransitionForceAfterMs", 30000L));
        final long campaignTransitionAttemptCooldownMs =
                Math.max(
                        1000L,
                        parseLongProperty(
                                "starsector.autoCampaignTransitionAttemptCooldownMs", 5000L));
        final long passiveDirectRearmAfterMs =
                Math.max(
                        15000L,
                        parseLongProperty(
                                "starsector.autoCampaignPassiveDirectRearmAfterMs", 120000L));
        final int passiveDirectRearmLimit =
                Math.max(
                        0,
                        parseIntProperty("starsector.autoCampaignPassiveDirectRearmLimit", 2));
        final long directSectorNullRearmAfterMs =
                Math.max(
                        30000L,
                        parseLongProperty(
                                "starsector.autoCampaignDirectSectorNullRearmAfterMs", 60000L));
        final int directSectorNullRearmLimit =
                Math.max(
                        0,
                        parseIntProperty("starsector.autoCampaignDirectSectorNullRearmLimit", 3));
        final long directNoAdvanceRearmAfterMs =
                Math.max(
                        30000L,
                        parseLongProperty(
                                "starsector.autoCampaignDirectNoAdvanceRearmAfterMs", 45000L));
        final int directNoAdvanceRearmAttemptLimit =
                Math.max(
                        0,
                        parseIntProperty(
                                "starsector.autoCampaignDirectNoAdvanceRearmAttempts", 3));
        final boolean allowModeDirectEscape =
                Boolean.parseBoolean(
                        System.getProperty(AUTO_CAMPAIGN_ALLOW_DIRECT_ESCAPE_PROPERTY, "false"));
        final boolean passiveCampaignStateTransitions =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignPassiveTransitions",
                                "false"));
        final boolean allowUnsafeDirectContinueState =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignUnsafeDirectContinueState",
                                normalizedMode.indexOf("visit_colony") >= 0 ? "true" : "false"));
        boolean disableDirectNewGame =
                normalizedMode.indexOf("no_direct") >= 0
                        || normalizedMode.indexOf("nodirect") >= 0
                        || normalizedMode.indexOf("menu_only") >= 0
                        || normalizedMode.indexOf("menuonly") >= 0;
        final boolean modeDisablesDirectNewGame = disableDirectNewGame;
        boolean enableColonyVisit =
                normalizedMode.indexOf("visit_colony") >= 0
                        || Boolean.parseBoolean(System.getProperty(AUTO_VISIT_COLONY_PROPERTY, "false"));
        String lastStateId = null;
        String lastLoaderName = null;
        long lastNoStateLogAt = 0L;
        long continueAt = -1L;
        boolean continueTriggered = false;
        boolean continueDisabled = false;
        int continueFailures = 0;
        boolean unsafeContinueTransitionNoticeLogged = false;
        boolean newGameStarted = false;
        boolean newGameMenuTriggered = false;
        long newGameMenuAt = -1L;
        int newGameMenuAttempts = 0;
        long lastNewGameAttemptAt = 0L;
        int newGameAttempts = 0;
        int fatalNewGameMenuFailures = 0;
        int fatalDirectNewGameFailures = 0;
        int fatalFailureCount = 0;
        Map<String, Integer> fatalFailureSignatures = new LinkedHashMap<String, Integer>();
        Map<String, Integer> retryableMenuFailureSignatures = new LinkedHashMap<String, Integer>();
        Map<String, Integer> retryableDirectFailureSignatures = new LinkedHashMap<String, Integer>();
        boolean newGameMenuDisabled = false;
        boolean directNewGameDisabled = false;
        boolean directNewGameSuppressedLogged = false;
        boolean colonyVisitDone = false;
        int colonyVisitAttempts = 0;
        long lastColonyVisitAttemptAt = 0L;
        long titleStateSince = -1L;
        long titleSettleLogAt = 0L;
        int continueNoTransitionFailures = 0;
        int directTimeoutFailures = 0;
        int passiveDirectRearmCount = 0;
        int directSectorNullRearmCount = 0;
        int directNoAdvanceTransitionAttempts = 0;
        long lastCampaignTransitionAttemptAt = 0L;
        long campaignTransitionPendingLogAt = 0L;
        long directNewGameSuccessAt = -1L;
        while (System.currentTimeMillis() - startedAt < timeoutMs) {
            try {
                DriverContext ctx = resolveDriverContext();
                Object currentState = ctx == null ? null : ctx.currentState;
                if (currentState == null) {
                    currentState = resolveStateFromCombatUI();
                }
                Object titleStateFromMap = null;
                if (currentState == null && ctx != null && ctx.states != null) {
                    try {
                        titleStateFromMap = ctx.states.get(TITLE_STATE_ID);
                    } catch (Throwable ignored) {
                    }
                }
                String stateId = null;
                if (currentState != null) {
                    try {
                        Method getId = currentState.getClass().getMethod("getID");
                        Object idObj = getId.invoke(currentState);
                        if (idObj != null) {
                            stateId = String.valueOf(idObj);
                        }
                    } catch (Throwable ignored) {
                    }
                    if (stateId == null || stateId.isEmpty()) {
                        stateId = currentState.getClass().getName();
                    }
                }
                if (stateId == null && titleStateFromMap != null) {
                    stateId = TITLE_STATE_ID;
                }

                if (stateId != null && !stateId.equals(lastStateId)) {
                    System.out.println("Fixer: auto campaign watcher state=" + stateId);
                    lastStateId = stateId;
                }
                if (stateId == null) {
                    long now = System.currentTimeMillis();
                    if (now - lastNoStateLogAt >= 15000L) {
                        System.out.println("Fixer: auto campaign watcher state=<null>");
                        if (ctx != null) {
                            int statesSize = ctx.states == null ? -1 : ctx.states.size();
                            int sessionSize = ctx.session == null ? -1 : ctx.session.size();
                            System.out.println(
                                    "Fixer: auto campaign context states="
                                            + statesSize
                                            + " startState="
                                            + String.valueOf(ctx.startStateId)
                                            + " sessionSize="
                                            + sessionSize
                                            + " stateKeys="
                                            + summarizeStateKeys(ctx.states));
                        }
                        System.out.println("Fixer: auto campaign driver-probe " + probeDriverContextsSummary());
                        lastNoStateLogAt = now;
                    }
                }
                if (ctx != null) {
                    String loaderName = String.valueOf(ctx.loader);
                    if (lastLoaderName == null || !lastLoaderName.equals(loaderName)) {
                        System.out.println("Fixer: auto campaign watcher driverLoader=" + loaderName);
                        lastLoaderName = loaderName;
                    }
                }
                if (isCampaignState(stateId, currentState)) {
                    if (!enableColonyVisit) {
                        System.out.println("Fixer: auto campaign watcher reached Campaign State.");
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if (!colonyVisitDone
                            && (colonyVisitAttempts == 0 || (now - lastColonyVisitAttemptAt) >= 3000L)) {
                        lastColonyVisitAttemptAt = now;
                        String result = tryPrimeColonyInteractionTarget();
                        if (result == null) {
                            colonyVisitAttempts++;
                            colonyVisitDone = true;
                            System.out.println(
                                    "Fixer: auto campaign watcher primed colony visit target (attempt "
                                            + colonyVisitAttempts
                                            + ").");
                            return;
                        } else if (isPendingColonyTargetResult(result)) {
                            System.out.println(
                                    "Fixer: auto campaign colony-target pending: " + result);
                        } else {
                            colonyVisitAttempts++;
                            System.out.println(
                                    "Fixer: auto campaign colony-target attempt "
                                            + colonyVisitAttempts
                                            + " failed: "
                                            + result);
                        }
                    }
                    if (colonyVisitAttempts >= 20) {
                        System.out.println(
                                "Fixer: auto campaign watcher reached Campaign State but colony priming was not completed.");
                        return;
                    }
                }
                Object titleState = currentState != null ? currentState : titleStateFromMap;
                if (!isTitleState(stateId, titleState) || titleState == null) {
                    titleStateSince = -1L;
                    directNoAdvanceTransitionAttempts = 0;
                    try {
                        Thread.sleep(pollMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }
                long now = System.currentTimeMillis();
                if (titleStateSince <= 0L) {
                    titleStateSince = now;
                }
                long settledFor = now - titleStateSince;
                if (titleSettleMs > 0L && settledFor < titleSettleMs) {
                    if (now - titleSettleLogAt >= 5000L) {
                        System.out.println(
                                "Fixer: auto campaign title settle wait remaining="
                                        + (titleSettleMs - settledFor)
                                        + "ms before menu/new-game actions.");
                        titleSettleLogAt = now;
                    }
                    try {
                        Thread.sleep(pollMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }

                if (!passiveCampaignStateTransitions && newGameStarted && isTitleState(stateId, titleState)) {
                    long sinceDirectNewGameSuccess =
                            directNewGameSuccessAt > 0L ? (now - directNewGameSuccessAt) : 0L;
                    if (directNewGameSuccessAt > 0L
                            && sinceDirectNewGameSuccess < directTransitionMinSettleMs) {
                        if (now - campaignTransitionPendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign waiting for direct new-game settle before Campaign State transition: remaining="
                                            + (directTransitionMinSettleMs - sinceDirectNewGameSuccess)
                                            + "ms");
                            campaignTransitionPendingLogAt = now;
                        }
                        try {
                            Thread.sleep(pollMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        continue;
                    }
                    String transitionReadinessIssue = checkCampaignStateTransitionReadiness(ctx);
                    boolean allowTransitionForPlayerFleetNull =
                            transitionReadinessIssue != null
                                    && transitionReadinessIssue
                                            .toLowerCase()
                                            .indexOf("player-fleet-null")
                                            >= 0;
                    boolean sectorNullTransitionIssue =
                            transitionReadinessIssue != null
                                    && transitionReadinessIssue.toLowerCase().indexOf("sector-null")
                                            >= 0;
                    if (!passiveCampaignStateTransitions
                            && directSectorNullRearmLimit > 0
                            && sectorNullTransitionIssue
                            && directNewGameSuccessAt > 0L
                            && sinceDirectNewGameSuccess >= directSectorNullRearmAfterMs
                            && directSectorNullRearmCount < directSectorNullRearmLimit) {
                        directSectorNullRearmCount++;
                        directNoAdvanceTransitionAttempts = 0;
                        newGameStarted = false;
                        directNewGameSuccessAt = -1L;
                        continueTriggered = false;
                        continueAt = -1L;
                        continueNoTransitionFailures = 0;
                        releaseDirectNewGameLease(watcherLeaseOwner);
                        resetAutoCampaignDirectNewGameCommittedAt();
                        directNewGameSuppressedLogged = false;
                        lastCampaignTransitionAttemptAt = 0L;
                        campaignTransitionPendingLogAt = now;
                        System.out.println(
                                "Fixer: auto campaign re-arming direct new-game after sector-null transition stall (elapsed="
                                        + sinceDirectNewGameSuccess
                                        + "ms, rearm="
                                        + directSectorNullRearmCount
                                        + "/"
                                        + directSectorNullRearmLimit
                                        + ").");
                        continue;
                    }
                    boolean forceTransitionForStalledReadiness =
                            transitionReadinessIssue != null
                                    && shouldForceCampaignTransitionForReadinessIssue(
                                            transitionReadinessIssue)
                                    && directNewGameSuccessAt > 0L
                                    && sinceDirectNewGameSuccess >= directTransitionForceAfterMs;
                    if (transitionReadinessIssue == null
                            || forceTransitionForStalledReadiness
                            || allowTransitionForPlayerFleetNull) {
                        if (forceTransitionForStalledReadiness
                                && now - campaignTransitionPendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign forcing Campaign State transition after prolonged startup-readiness wait issue="
                                            + transitionReadinessIssue
                                            + " ("
                                            + sinceDirectNewGameSuccess
                                            + "ms).");
                            campaignTransitionPendingLogAt = now;
                        }
                        if (allowTransitionForPlayerFleetNull
                                && now - campaignTransitionPendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign attempting Campaign State transition despite player-fleet-null readiness gate.");
                            campaignTransitionPendingLogAt = now;
                        }
                        if (now - lastCampaignTransitionAttemptAt
                                >= campaignTransitionAttemptCooldownMs) {
                            lastCampaignTransitionAttemptAt = now;
                            if (setCampaignStateIfPossible(ctx, titleState)) {
                                directNoAdvanceTransitionAttempts = 0;
                                continueTriggered = true;
                                continueAt = now;
                                continueNoTransitionFailures = 0;
                            } else if (now - campaignTransitionPendingLogAt >= 5000L) {
                                directNoAdvanceTransitionAttempts++;
                                if (!passiveCampaignStateTransitions
                                        && directSectorNullRearmLimit > 0
                                        && directNoAdvanceRearmAttemptLimit > 0
                                        && directNewGameSuccessAt > 0L
                                        && sinceDirectNewGameSuccess >= directNoAdvanceRearmAfterMs
                                        && directNoAdvanceTransitionAttempts
                                                >= directNoAdvanceRearmAttemptLimit
                                        && directSectorNullRearmCount < directSectorNullRearmLimit) {
                                    directSectorNullRearmCount++;
                                    directNoAdvanceTransitionAttempts = 0;
                                    newGameStarted = false;
                                    directNewGameSuccessAt = -1L;
                                    continueTriggered = false;
                                    continueAt = -1L;
                                    continueNoTransitionFailures = 0;
                                    releaseDirectNewGameLease(watcherLeaseOwner);
                                    resetAutoCampaignDirectNewGameCommittedAt();
                                    directNewGameSuppressedLogged = false;
                                    lastCampaignTransitionAttemptAt = 0L;
                                    campaignTransitionPendingLogAt = now;
                                    System.out.println(
                                            "Fixer: auto campaign re-arming direct new-game after repeated no-advance Campaign State transition attempts (elapsed="
                                                    + sinceDirectNewGameSuccess
                                                    + "ms, attempts="
                                                    + directNoAdvanceRearmAttemptLimit
                                                    + ", rearm="
                                                    + directSectorNullRearmCount
                                                    + "/"
                                                    + directSectorNullRearmLimit
                                                    + ").");
                                    continue;
                                }
                                System.out.println(
                                        "Fixer: auto campaign direct new-game transition retry pending: goToState unavailable.");
                                campaignTransitionPendingLogAt = now;
                            }
                        }
                    } else if (now - campaignTransitionPendingLogAt >= 5000L) {
                        System.out.println(
                                "Fixer: auto campaign waiting for campaign transition readiness: "
                                        + transitionReadinessIssue);
                        campaignTransitionPendingLogAt = now;
                    }
                } else if (passiveCampaignStateTransitions
                        && newGameStarted
                        && isTitleState(stateId, titleState)) {
                    long sinceDirectSuccess =
                            directNewGameSuccessAt > 0L ? (now - directNewGameSuccessAt) : 0L;
                    if (passiveDirectRearmLimit > 0
                            && directNewGameSuccessAt > 0L
                            && sinceDirectSuccess >= passiveDirectRearmAfterMs
                            && passiveDirectRearmCount < passiveDirectRearmLimit) {
                        passiveDirectRearmCount++;
                        newGameStarted = false;
                        directNewGameSuccessAt = -1L;
                        continueTriggered = false;
                        continueAt = -1L;
                        resetAutoCampaignDirectNewGameCommittedAt();
                        directNewGameSuppressedLogged = false;
                        System.out.println(
                                "Fixer: auto campaign re-arming direct new-game after passive transition stall (elapsed="
                                        + sinceDirectSuccess
                                        + "ms, rearm="
                                        + passiveDirectRearmCount
                                        + "/"
                                        + passiveDirectRearmLimit
                                        + ").");
                    } else if (now - campaignTransitionPendingLogAt >= 10000L) {
                        System.out.println(
                                "Fixer: auto campaign passive-transition mode active; waiting for title/menu flow to enter Campaign State.");
                        campaignTransitionPendingLogAt = now;
                    }
                }

                if (continueTriggered
                        && continueAt > 0L
                        && (now - continueAt) >= continueStallMs
                        && isTitleState(stateId, titleState)) {
                    continueTriggered = false;
                    continueAt = -1L;
                    continueNoTransitionFailures++;
                    continueFailures++;
                    System.out.println(
                            "Fixer: auto campaign Continue fallback did not leave title state after "
                                    + continueStallMs
                                    + "ms (no-transition failures="
                                    + continueNoTransitionFailures
                                    + ", total continue failures="
                                    + continueFailures
                                    + ").");
                    if (continueFailures >= continueDisableFailureLimit) {
                        continueDisabled = true;
                        System.out.println(
                                "Fixer: auto campaign disabling Continue menu automation after "
                                        + continueFailures
                                        + " failures (including no-transition stalls, limit="
                                        + continueDisableFailureLimit
                                        + ").");
                    }
                    if (modeDisablesDirectNewGame
                            && disableDirectNewGame
                            && allowModeDirectEscape
                            && (newGameMenuDisabled || fatalNewGameMenuFailures > 0)) {
                        disableDirectNewGame = false;
                        directNewGameSuppressedLogged = false;
                        System.out.println(
                                "Fixer: auto campaign enabling direct new-game escape fallback after Continue no-transition stall.");
                    }
                }

                boolean observeOnly =
                        normalizedMode.indexOf("observe") >= 0
                                || normalizedMode.indexOf("noop") >= 0
                                || normalizedMode.indexOf("none") >= 0;
                boolean allowContinue =
                        !observeOnly
                                && !passiveCampaignStateTransitions
                                && (normalizedMode.indexOf("continue") >= 0
                                        || normalizedMode.indexOf("new") < 0);
                boolean allowNewGame = !observeOnly && normalizedMode.indexOf("new") >= 0;

                long globalCommittedAt =
                        parseLongProperty(
                                AUTO_CAMPAIGN_DIRECT_NEW_GAME_COMMITTED_AT_PROPERTY, -1L);
                if (globalCommittedAt > autoCampaignDirectNewGameCommittedAt) {
                    autoCampaignDirectNewGameCommittedAt = globalCommittedAt;
                }

                if (!newGameStarted && autoCampaignDirectNewGameCommittedAt >= watcherStartedAt) {
                    newGameStarted = true;
                    if (directNewGameSuccessAt <= 0L) {
                        directNewGameSuccessAt = autoCampaignDirectNewGameCommittedAt;
                    }
                    if (now - campaignTransitionPendingLogAt >= 10000L) {
                        System.out.println(
                                "Fixer: auto campaign detected prior direct new-game commit from peer watcher; suppressing duplicate new-game init attempts.");
                        campaignTransitionPendingLogAt = now;
                    }
                }

                if (allowContinue && !continueTriggered && !continueDisabled) {
                    boolean transitioned = false;
                    boolean attemptedDirectContinueTransition = false;
                    if (allowUnsafeDirectContinueState) {
                        try {
                            Object campaignState = resolveCampaignStateFromDriver(ctx);
                            if (campaignState == null) {
                                Object titleSessionObj = readFieldRecursive(titleState, "session");
                                if (titleSessionObj instanceof Map) {
                                    campaignState = ((Map) titleSessionObj).get(CAMPAIGN_SESSION_KEY);
                                }
                            }
                            if (campaignState != null) {
                                attemptedDirectContinueTransition = true;
                                if (now - lastCampaignTransitionAttemptAt
                                        >= campaignTransitionAttemptCooldownMs) {
                                    lastCampaignTransitionAttemptAt = now;
                                    if (setCampaignStateIfPossible(ctx, titleState)) {
                                        transitioned = true;
                                        continueTriggered = true;
                                        continueAt = System.currentTimeMillis();
                                        continueNoTransitionFailures = 0;
                                        System.out.println(
                                                "Fixer: auto campaign requested Campaign State transition via Continue fallback.");
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    } else if (!unsafeContinueTransitionNoticeLogged) {
                        unsafeContinueTransitionNoticeLogged = true;
                        System.out.println(
                                "Fixer: auto campaign direct Continue->Campaign state fallback is disabled (menu selection only).");
                    }

                    if (!transitioned && !attemptedDirectContinueTransition) {
                        String continueFailure =
                                trySelectTitleMenu(titleState, "OO0000", "Continue");
                        if (continueFailure == null) {
                            continueTriggered = true;
                            continueAt = System.currentTimeMillis();
                            continueNoTransitionFailures = 0;
                        } else {
                            System.out.println(
                                    "Fixer: auto campaign Continue menu attempt failed: "
                                            + continueFailure);
                            String continueSignature = classifyAutoCampaignFailure(continueFailure);
                            if ("title-newgame-menu-npe".equals(continueSignature)) {
                                continueDisabled = true;
                                System.out.println(
                                        "Fixer: auto campaign disabling Continue menu automation immediately for signature "
                                                + continueSignature
                                                + ".");
                                continue;
                            }
                            continueFailures++;
                            if (continueFailures >= continueDisableFailureLimit) {
                                continueDisabled = true;
                                System.out.println(
                                        "Fixer: auto campaign disabling Continue menu automation after "
                                                + continueFailures
                                                + " failures (limit="
                                                + continueDisableFailureLimit
                                                + ").");
                            }
                        }
                    }
                }

                if (allowNewGame && !newGameStarted) {
                    boolean fallbackDue =
                            !continueTriggered
                                    || (continueAt > 0L && (now - continueAt) >= fallbackMs);
                    boolean canAttemptNow = (now - lastNewGameAttemptAt) >= 5000L;
                    if (fallbackDue && canAttemptNow) {
                        if (newGameStarted && directNewGameSuccessAt > 0L) {
                            if (now - campaignTransitionPendingLogAt >= 10000L) {
                                System.out.println(
                                        "Fixer: auto campaign skipping repeated direct new-game attempts after successful init; waiting for sector generation to finish.");
                                campaignTransitionPendingLogAt = now;
                            }
                            continue;
                        }
                        boolean forceMenuPath =
                                normalizedMode.indexOf("menu_only") >= 0
                                        || normalizedMode.indexOf("menuonly") >= 0;
                        boolean preferMenuNewGame =
                                forceMenuPath
                                        || disableDirectNewGame
                                        || (normalizedMode.indexOf("direct") < 0
                                                && normalizedMode.indexOf("visit_colony") < 0);
                        boolean shouldTryMenuSelection =
                                preferMenuNewGame
                                        && (!newGameMenuTriggered
                                                || (newGameMenuAt > 0L
                                                        && (now - newGameMenuAt)
                                                                >= Math.max(3000L, fallbackMs)));
                        if (shouldTryMenuSelection && !newGameMenuDisabled) {
                            lastNewGameAttemptAt = now;
                            newGameMenuAttempts++;
                            String newGameFailure =
                                    trySelectTitleMenu(titleState, "NEW_GAME", "New Game");
                            if (newGameFailure == null) {
                                newGameMenuTriggered = true;
                                newGameMenuAt = now;
                                System.out.println(
                                        "Fixer: auto campaign triggered title New Game menu (attempt "
                                                + newGameMenuAttempts
                                                + ").");
                                continue;
                            } else if (isPendingNewGamePreflightFailure(newGameFailure)) {
                                continue;
                            } else if (isFatalAutoCampaignFailure(newGameFailure)) {
                                String signature = classifyAutoCampaignFailure(newGameFailure);
                                if (isRetryableMenuAutoCampaignSignature(signature)) {
                                    int retryableCount =
                                            incrementNamedCounter(
                                                    retryableMenuFailureSignatures, signature);
                                    System.out.println(
                                            "Fixer: auto campaign New Game menu failure treated as retryable signature "
                                                    + signature
                                                    + " ("
                                                    + retryableCount
                                                    + "/"
                                                    + retryableMenuSignatureLimit
                                                    + "): "
                                                    + newGameFailure);
                                    if ("title-newgame-menu-npe".equals(signature)) {
                                        warmupTitleRenderTicks(
                                                titleState, Math.min(16, 6 + retryableCount));
                                    }
                                    if (retryableCount < retryableMenuSignatureLimit) {
                                        continue;
                                    }
                                    System.out.println(
                                            "Fixer: auto campaign retryable signature "
                                                    + signature
                                                    + " reached limit "
                                                    + retryableMenuSignatureLimit
                                                    + "; escalating as fatal.");
                                }
                                fatalNewGameMenuFailures++;
                                fatalFailureCount++;
                                int signatureCount =
                                        incrementNamedCounter(fatalFailureSignatures, signature);
                                System.out.println(
                                        "Fixer: auto campaign New Game menu fatal failure ("
                                                + fatalNewGameMenuFailures
                                                + "/"
                                                + fatalMenuFailureLimit
                                                + ", total="
                                                + fatalFailureCount
                                                + "/"
                                                + fatalFailureLimit
                                                + ", signature="
                                                + signature
                                                + ":"
                                                + signatureCount
                                                + "/"
                                                + fatalSignatureLimit
                                                + "): "
                                                + newGameFailure);
                                if (fatalNewGameMenuFailures >= fatalMenuFailureLimit) {
                                    newGameMenuDisabled = true;
                                    System.out.println(
                                            "Fixer: auto campaign disabling New Game menu automation after repeated fatal failures.");
                                    if (disableDirectNewGame
                                            && (!modeDisablesDirectNewGame || allowModeDirectEscape)) {
                                        disableDirectNewGame = false;
                                        directNewGameSuppressedLogged = false;
                                        System.out.println(
                                                "Fixer: auto campaign enabling direct new-game fallback after menu fatal failures.");
                                    }
                                }
                                boolean fatalThresholdReached =
                                        fatalFailureCount >= fatalFailureLimit
                                                || signatureCount >= fatalSignatureLimit;
                                boolean hasDirectRecoveryPath =
                                        !disableDirectNewGame && !directNewGameDisabled;
                                if (fatalThresholdReached && !hasDirectRecoveryPath) {
                                    System.out.println(
                                            "Fixer: auto campaign aborting after repeated fatal failures (menu path).");
                                    return;
                                } else if (fatalThresholdReached) {
                                    System.out.println(
                                            "Fixer: auto campaign fatal threshold reached on menu path, but direct new-game fallback is still available; continuing.");
                                }
                            }
                        }

                        boolean directDue =
                                !preferMenuNewGame
                                        || !newGameMenuTriggered
                                        || (newGameMenuAt > 0L
                                                && (now - newGameMenuAt) >= Math.max(15000L, fallbackMs * 3L));
                        if (directDue && !disableDirectNewGame && !directNewGameDisabled) {
                            long directLeaseMs = Math.max(60000L, directAttemptTimeoutMs * 3L);
                            if (!claimDirectNewGameLease(watcherLeaseOwner, now, directLeaseMs)) {
                                if (now - campaignTransitionPendingLogAt >= 5000L) {
                                    System.out.println(
                                            "Fixer: auto campaign direct new-game attempt skipped; another watcher lease is active.");
                                    campaignTransitionPendingLogAt = now;
                                }
                                continue;
                            }
                            lastNewGameAttemptAt = now;
                            newGameAttempts++;
                            String result =
                                    tryStartDirectNewGameWithTimeout(
                                            ctx, titleState, directAttemptTimeoutMs);
                            boolean keepDirectLease =
                                    result != null
                                            && result.toLowerCase()
                                                            .indexOf("active attempt still running")
                                                    >= 0;
                            if (result == null) {
                                if (!keepDirectLease) {
                                    releaseDirectNewGameLease(watcherLeaseOwner);
                                }
                                directTimeoutFailures = 0;
                                String transitionReadinessIssue =
                                        checkCampaignStateTransitionReadiness(ctx);
                                if (transitionReadinessIssue != null) {
                                    String transitionReadinessIssueLower =
                                            transitionReadinessIssue.toLowerCase();
                                    boolean playerFleetNullReadiness =
                                            transitionReadinessIssueLower.indexOf("player-fleet-null")
                                                    >= 0;
                                    boolean sectorNullReadiness =
                                            transitionReadinessIssueLower.indexOf("sector-null")
                                                    >= 0;
                                    if (playerFleetNullReadiness || sectorNullReadiness) {
                                        newGameStarted = true;
                                        if (directNewGameSuccessAt <= 0L) {
                                            directNewGameSuccessAt = now;
                                        }
                                        publishAutoCampaignDirectNewGameCommittedAt(now);
                                        if (sectorNullReadiness
                                                && now - campaignTransitionPendingLogAt >= 5000L) {
                                            System.out.println(
                                                    "Fixer: direct new-game runtime readiness still sector-null; treating as transitional startup state.");
                                        }
                                    } else if (sectorNullReadiness
                                            && now - campaignTransitionPendingLogAt >= 5000L) {
                                        System.out.println(
                                                "Fixer: direct new-game runtime readiness still sector-null; keeping direct init retry path active.");
                                    }
                                    if (now - campaignTransitionPendingLogAt >= 5000L) {
                                        System.out.println(
                                                "Fixer: direct new-game returned without campaign readiness ("
                                                        + transitionReadinessIssue
                                                        + "); keeping fallback paths active.");
                                        campaignTransitionPendingLogAt = now;
                                    }
                                    if (!passiveCampaignStateTransitions
                                            && now - lastCampaignTransitionAttemptAt
                                                    >= campaignTransitionAttemptCooldownMs) {
                                        lastCampaignTransitionAttemptAt = now;
                                        if (setCampaignStateIfPossible(ctx, titleState)) {
                                            continueTriggered = true;
                                            continueAt = now;
                                            continueNoTransitionFailures = 0;
                                        }
                                    }
                                    continue;
                                }

                                newGameStarted = true;
                                directNewGameSuccessAt = now;
                                publishAutoCampaignDirectNewGameCommittedAt(now);
                                System.out.println("Fixer: direct new-game init succeeded.");
                                if (!passiveCampaignStateTransitions
                                        && directTransitionMinSettleMs <= 0L) {
                                    if (!setCampaignStateIfPossible(ctx, titleState)) {
                                        System.out.println(
                                                "Fixer: direct new-game init succeeded but goToState could not be invoked.");
                                    } else {
                                        continueTriggered = true;
                                        continueAt = now;
                                        continueNoTransitionFailures = 0;
                                    }
                                } else {
                                    System.out.println(
                                            "Fixer: direct new-game init succeeded; waiting "
                                                    + directTransitionMinSettleMs
                                                    + "ms before forced transition checks.");
                                }
                            } else {
                                if (!keepDirectLease) {
                                    releaseDirectNewGameLease(watcherLeaseOwner);
                                }
                                if (isPendingNewGamePreflightFailure(result)) {
                                    maybeLogNewGamePreflightIssue(result);
                                    continue;
                                }
                                System.out.println(
                                        "Fixer: direct new-game init failed (attempt "
                                                + newGameAttempts
                                                + "): "
                                                + result);
                                String resultLower = result == null ? "" : result.toLowerCase();
                                if (resultLower.indexOf("timeout") >= 0) {
                                    directTimeoutFailures++;
                                    if (directTimeoutFailures >= directTimeoutFailureLimit) {
                                        directNewGameDisabled = true;
                                        System.out.println(
                                                "Fixer: auto campaign disabling direct new-game fallback after "
                                                        + directTimeoutFailures
                                                        + " timeout failures.");
                                        continue;
                                    }
                                } else {
                                    directTimeoutFailures = 0;
                                }
                                if (isFatalAutoCampaignFailure(result)) {
                                    String signature = classifyAutoCampaignFailure(result);
                                    if (isRetryableAutoCampaignSignature(signature)) {
                                        int retryableCount =
                                                incrementNamedCounter(
                                                        retryableDirectFailureSignatures, signature);
                                        System.out.println(
                                                "Fixer: auto campaign direct new-game failure treated as retryable signature "
                                                        + signature
                                                        + " ("
                                                        + retryableCount
                                                        + "/"
                                                        + retryableDirectSignatureLimit
                                                        + ")"
                                                        + ": "
                                                        + result);
                                        if (retryableCount >= retryableDirectSignatureLimit) {
                                            directNewGameDisabled = true;
                                            System.out.println(
                                                    "Fixer: auto campaign disabling direct new-game fallback after retryable signature "
                                                            + signature
                                                            + " reached limit "
                                                            + retryableDirectSignatureLimit
                                                            + ".");
                                        }
                                        continue;
                                    }
                                    fatalDirectNewGameFailures++;
                                    fatalFailureCount++;
                                    int signatureCount =
                                            incrementNamedCounter(fatalFailureSignatures, signature);
                                    System.out.println(
                                            "Fixer: auto campaign direct new-game fatal failure ("
                                                    + fatalDirectNewGameFailures
                                                    + "/"
                                                    + fatalDirectFailureLimit
                                                    + ", total="
                                                    + fatalFailureCount
                                                    + "/"
                                                    + fatalFailureLimit
                                                    + ", signature="
                                                    + signature
                                                    + ":"
                                                    + signatureCount
                                                    + "/"
                                                    + fatalSignatureLimit
                                                    + ").");
                                    boolean signatureIsNpe =
                                            signature != null && signature.indexOf("npe") >= 0;
                                    if (signatureIsNpe && !directNewGameDisabled) {
                                        directNewGameDisabled = true;
                                        System.out.println(
                                                "Fixer: auto campaign disabling direct new-game fallback immediately for NPE signature "
                                                        + signature
                                                        + " to avoid repeated fatal loops.");
                                    }
                                    if (fatalDirectNewGameFailures >= fatalDirectFailureLimit) {
                                        directNewGameDisabled = true;
                                        System.out.println(
                                                "Fixer: auto campaign disabling direct new-game fallback after repeated fatal failures.");
                                    }
                                    if (fatalFailureCount >= fatalFailureLimit
                                            || signatureCount >= fatalSignatureLimit) {
                                        System.out.println(
                                                "Fixer: auto campaign aborting after repeated fatal failures (direct path).");
                                        return;
                                    }
                                }
                            }
                        } else if (directDue && !directNewGameSuppressedLogged) {
                            directNewGameSuppressedLogged = true;
                            System.out.println(
                                    "Fixer: direct new-game fallback suppressed by mode "
                                            + normalizedMode
                                            + ".");
                        }

                        boolean continueUnavailable = !allowContinue || continueDisabled;
                        if (continueUnavailable
                                && newGameMenuDisabled
                                && (disableDirectNewGame || directNewGameDisabled)) {
                            System.out.println(
                                    "Fixer: auto campaign exhausted all new-game recovery paths; stopping watcher to avoid repeated fatal loops.");
                            return;
                        }
                    }
                }
            } catch (Throwable t) {
                System.out.println(
                        "Fixer: auto campaign watcher error: "
                                + describeThrowableChain(t)
                                + " @"
                                + firstStackFrame(t));
            }
            try {
                Thread.sleep(pollMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println("Fixer: auto campaign watcher timed out after " + timeoutMs + "ms.");
    }

    private static DriverContext resolveDriverContext() {
        DriverContext direct = resolveDriverContextDirect();
        if (direct != null) {
            boolean hasContext =
                    direct.currentState != null
                            || (direct.states != null && !direct.states.isEmpty())
                            || (direct.session != null && !direct.session.isEmpty())
                            || direct.startStateId != null;
            if (hasContext) {
                return direct;
            }
        }

        LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        addLoaderForClass(loaders, "com.fs.starfarer.combat.CombatMain");
        addLoaderForClass(loaders, "com.fs.starfarer.StarfarerLauncher");
        addLoaderForClass(loaders, "com.fs.starfarer.title.TitleScreenState");
        addLoaderForClass(loaders, "com.fs.state.AppDriver");
        loaders.add(Fixer.class.getClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                loaders.add(t.getContextClassLoader());
            }
        } catch (Throwable ignored) {
        }

        DriverContext best = null;
        int bestScore = Integer.MIN_VALUE;
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                Class<?> appDriverClass = Class.forName("com.fs.state.AppDriver", false, loader);
                Object driver = readStaticFieldRecursive(appDriverClass, "instance");
                if (driver == null) {
                    // Avoid getInstance() here: calling it on unrelated classloaders creates
                    // empty AppDriver singletons and pollutes watcher selection.
                    continue;
                }
                Method getCurrentState = appDriverClass.getMethod("getCurrentState");
                Object currentState = getCurrentState.invoke(driver);
                Object statesObj = readFieldRecursive(driver, "states");
                Object startState = readFieldRecursive(driver, "startStateID");
                Object sessionObj = readFieldRecursive(driver, "session");
                Map states = statesObj instanceof Map ? (Map) statesObj : null;
                Map session = sessionObj instanceof Map ? (Map) sessionObj : null;
                if (currentState == null && states != null && startState instanceof String) {
                    try {
                        Object titleState = states.get(TITLE_STATE_ID);
                        if (titleState != null) {
                            currentState = titleState;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                DriverContext ctx =
                        new DriverContext(
                                driver,
                                appDriverClass,
                                currentState,
                                loader,
                                states,
                                startState,
                                session);

                int score = 0;
                if (currentState != null) {
                    score += 1000;
                }
                if (states != null) {
                    score += Math.min(states.size(), 500);
                }
                if (startState != null) {
                    score += 50;
                }
                if (session != null) {
                    score += Math.min(session.size(), 100);
                }

                if (currentState != null) {
                    return ctx;
                }
                if (best == null || score > bestScore) {
                    best = ctx;
                    bestScore = score;
                }
            } catch (Throwable ignored) {
            }
        }
        if (best != null) {
            return best;
        }
        return direct;
    }

    private static DriverContext resolveDriverContextDirect() {
        try {
            com.fs.state.AppDriver driver = com.fs.state.AppDriver.getInstance();
            if (driver == null) {
                return null;
            }
            Object currentState = null;
            try {
                currentState = driver.getCurrentState();
            } catch (Throwable ignored) {
            }

            Object statesObj = readFieldRecursive(driver, "states");
            Object startState = readFieldRecursive(driver, "startStateID");
            Object sessionObj = readFieldRecursive(driver, "session");
            Map states = statesObj instanceof Map ? (Map) statesObj : null;
            Map session = sessionObj instanceof Map ? (Map) sessionObj : null;

            if (currentState == null && states != null && startState instanceof String) {
                try {
                    Object titleState = states.get(TITLE_STATE_ID);
                    if (titleState != null) {
                        currentState = titleState;
                    }
                } catch (Throwable ignored) {
                }
            }

            return new DriverContext(
                    driver,
                    com.fs.state.AppDriver.class,
                    currentState,
                    com.fs.state.AppDriver.class.getClassLoader(),
                    states,
                    startState,
                    session);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String probeDriverContextsSummary() {
        try {
            LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
            addLoaderForClass(loaders, "com.fs.starfarer.combat.CombatMain");
            addLoaderForClass(loaders, "com.fs.starfarer.StarfarerLauncher");
            addLoaderForClass(loaders, "com.fs.starfarer.title.TitleScreenState");
            addLoaderForClass(loaders, "com.fs.state.AppDriver");
            loaders.add(Fixer.class.getClassLoader());
            loaders.add(Thread.currentThread().getContextClassLoader());
            loaders.add(ClassLoader.getSystemClassLoader());
            try {
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    loaders.add(t.getContextClassLoader());
                }
            } catch (Throwable ignored) {
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (ClassLoader loader : loaders) {
                if (loader == null) {
                    continue;
                }
                if (count >= 10) {
                    sb.append(" ...");
                    break;
                }
                count++;
                try {
                    Class<?> appDriverClass = Class.forName("com.fs.state.AppDriver", false, loader);
                    Object driver = readStaticFieldRecursive(appDriverClass, "instance");
                    if (driver == null) {
                        sb.append(" {")
                                .append(shortLoader(loader))
                                .append(" inst=0}");
                        continue;
                    }
                    Object statesObj = readFieldRecursive(driver, "states");
                    Object startState = readFieldRecursive(driver, "startStateID");
                    int statesSize = statesObj instanceof Map ? ((Map) statesObj).size() : -1;
                    Object current = null;
                    Method getCurrentState = findMethodRecursive(appDriverClass, "getCurrentState");
                    if (getCurrentState != null) {
                        try {
                            getCurrentState.setAccessible(true);
                            current = getCurrentState.invoke(driver);
                        } catch (Throwable ignored) {
                        }
                    }
                    String currentDesc = current == null ? "null" : current.getClass().getName();
                    sb.append(" {")
                            .append(shortLoader(loader))
                            .append(" inst=1 states=")
                            .append(statesSize)
                            .append(" start=")
                            .append(String.valueOf(startState))
                            .append(" current=")
                            .append(currentDesc)
                            .append("}");
                } catch (Throwable t) {
                    sb.append(" {")
                            .append(shortLoader(loader))
                            .append(" err=")
                            .append(t.getClass().getSimpleName())
                            .append("@")
                            .append(firstStackFrame(t))
                            .append("}");
                }
            }
            if (sb.length() == 0) {
                return "<none>";
            }
            return sb.toString();
        } catch (Throwable t) {
            return "<error " + describeThrowableChain(t) + " @" + firstStackFrame(t) + ">";
        }
    }

    private static String firstStackFrame(Throwable t) {
        if (t == null) {
            return "none";
        }
        try {
            StackTraceElement[] trace = t.getStackTrace();
            if (trace != null && trace.length > 0 && trace[0] != null) {
                return trace[0].toString();
            }
        } catch (Throwable ignored) {
        }
        return "no-stack";
    }

    private static String shortLoader(ClassLoader loader) {
        if (loader == null) {
            return "null";
        }
        String cls = loader.getClass().getName();
        String id = Integer.toHexString(System.identityHashCode(loader));
        return cls + "@" + id;
    }

    private static void addLoaderForClass(LinkedHashSet<ClassLoader> loaders, String className) {
        if (loaders == null || className == null || className.isEmpty()) {
            return;
        }
        try {
            Class<?> cls = Class.forName(className, false, Fixer.class.getClassLoader());
            loaders.add(cls == null ? null : cls.getClassLoader());
        } catch (Throwable ignored) {
        }
        try {
            Class<?> cls = Class.forName(className);
            loaders.add(cls == null ? null : cls.getClassLoader());
        } catch (Throwable ignored) {
        }
    }

    private static Object resolveStateFromCombatUI() {
        LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        addLoaderForClass(loaders, "com.fs.starfarer.combat.CombatEngine");
        addLoaderForClass(loaders, "com.fs.starfarer.combat.CombatMain");
        addLoaderForClass(loaders, "com.fs.starfarer.title.TitleScreenState");
        addLoaderForClass(loaders, "com.fs.starfarer.campaign.CampaignState");
        addLoaderForClass(loaders, "com.fs.starfarer.StarfarerLauncher");
        loaders.add(Fixer.class.getClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                loaders.add(t.getContextClassLoader());
            }
        } catch (Throwable ignored) {
        }

        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                Class<?> combatEngineClass = Class.forName("com.fs.starfarer.combat.CombatEngine", false, loader);
                Method getInstance = findMethodRecursive(combatEngineClass, "getInstance");
                if (getInstance == null || !Modifier.isStatic(getInstance.getModifiers())) {
                    continue;
                }
                getInstance.setAccessible(true);
                Object combatEngine = getInstance.invoke(null);
                if (combatEngine == null) {
                    continue;
                }
                Method getCombatUI = findMethodRecursive(combatEngine.getClass(), "getCombatUI");
                if (getCombatUI == null) {
                    continue;
                }
                getCombatUI.setAccessible(true);
                Object state = getCombatUI.invoke(combatEngine);
                if (state != null) {
                    return state;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean isTitleState(String stateId, Object state) {
        return stateMatches(stateId, state, TITLE_STATE_ID, "TitleScreenState");
    }

    private static boolean isCampaignState(String stateId, Object state) {
        return stateMatches(stateId, state, CAMPAIGN_STATE_ID, "CampaignState");
    }

    private static boolean stateMatches(
            String stateId, Object state, String canonicalId, String classNameSuffix) {
        if (stateId != null) {
            if (canonicalId.equals(stateId)) {
                return true;
            }
            if (classNameSuffix != null && !classNameSuffix.isEmpty()) {
                if (stateId.endsWith(classNameSuffix)) {
                    return true;
                }
                if (stateId.indexOf(classNameSuffix) >= 0) {
                    return true;
                }
            }
        }
        if (state != null && classNameSuffix != null && !classNameSuffix.isEmpty()) {
            try {
                String className = state.getClass().getName();
                if (className != null) {
                    if (className.endsWith(classNameSuffix) || className.indexOf(classNameSuffix) >= 0) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static String summarizeStateKeys(Map states) {
        if (states == null || states.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        for (Object key : states.keySet()) {
            if (i >= 8) {
                sb.append("...");
                break;
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.valueOf(key));
            i++;
        }
        sb.append(']');
        return sb.toString();
    }

    private static String trySelectTitleMenu(Object titleState, String enumName, String label) {
        try {
            Method menuMethod = null;
            for (Method m : titleState.getClass().getMethods()) {
                if ("menuItemSelected".equals(m.getName()) && m.getParameterTypes().length == 1) {
                    menuMethod = m;
                    break;
                }
            }
            if (menuMethod == null) {
                for (Method m : titleState.getClass().getDeclaredMethods()) {
                    if ("menuItemSelected".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        menuMethod = m;
                        break;
                    }
                }
            }
            if (menuMethod == null) {
                return "menuItemSelected method not found";
            }
            Class<?> enumType = menuMethod.getParameterTypes()[0];
            if (!enumType.isEnum()) {
                return "menu enum type unavailable";
            }
            Object enumValue = null;
            try {
                @SuppressWarnings("rawtypes")
                Object byName = Enum.valueOf((Class) enumType, enumName);
                enumValue = byName;
            } catch (Throwable ignored) {
            }
            if (enumValue == null) {
                enumValue = pickMenuEnumFallback(enumType, label);
            }
            if (enumValue == null) {
                System.out.println(
                        "Fixer: auto campaign menu select unresolved for "
                                + label
                                + " available="
                                + summarizeEnumConstants(enumType));
                return "menu enum value unresolved";
            }
            if (wouldSelectLoadMenuEntry(enumValue, label)) {
                System.out.println(
                        "Fixer: auto campaign refusing menu candidate for "
                                + label
                                + " because it appears to be Load Game: "
                                + String.valueOf(enumValue)
                                + " available="
                                + summarizeEnumConstants(enumType));
                return "menu fallback resolved to load-game candidate";
            }
            if (isNewGameMenuSelection(label, enumValue)) {
                if (!shouldSkipFactionPreflight()) {
                    String factionIssue =
                            ensureFactionSpecsReadyForDirectNewGame(
                                    allowMutatingSpecPreflight());
                    if (factionIssue != null) {
                        String warmupResult = maybeWarmupFactionSpecsForNewGame(factionIssue);
                        if (warmupResult != null) {
                            maybeLogNewGamePreflightIssue(
                                    factionIssue + " [warmup:" + warmupResult + "]");
                            warmupTitleRenderTicks(titleState, 6);
                            return "new-game preflight pending";
                        }
                    }
                } else {
                    System.out.println(
                            "Fixer: auto campaign skipping New Game faction warmup; running repair-only faction preflight.");
                    String factionIssue = ensureFactionSpecsReadyForDirectNewGame(true);
                    if (factionIssue != null) {
                        maybeLogNewGamePreflightIssue(factionIssue);
                        warmupTitleRenderTicks(titleState, 4);
                        return "new-game preflight pending";
                    }
                }
                if (!uiMenuNewGamePreflightReady) {
                    String uiFontIssue = ensureUiFontStateReadyForDirectNewGame();
                    if (uiFontIssue != null) {
                        System.out.println(
                                "Fixer: auto campaign ui-font preflight issue before menu New Game: "
                                        + uiFontIssue);
                    }
                    String uiTextureIssue = ensureUiPanelTextureObjectsReadyForDirectNewGame();
                    if (uiTextureIssue != null) {
                        System.out.println(
                                "Fixer: auto campaign ui-texture preflight issue before menu New Game: "
                                        + uiTextureIssue);
                    }
                    String uiSpriteIssue = ensureUiBorderSpriteMappingsReady();
                    if (uiSpriteIssue != null) {
                        System.out.println(
                                "Fixer: auto campaign ui-border sprite sanity issue before menu New Game: "
                                        + uiSpriteIssue);
                    }
                    if (uiFontIssue == null && uiTextureIssue == null && uiSpriteIssue == null) {
                        uiMenuNewGamePreflightReady = true;
                    }
                }
                warmupTitleRenderTicks(titleState, 8);
            }
            menuMethod.setAccessible(true);
            menuMethod.invoke(titleState, enumValue);
            System.out.println("Fixer: auto campaign selected title menu " + label + ".");
            return null;
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign menu select failed for "
                            + label
                            + ": "
                            + describeThrowableChain(t));
            if (autoCampaignMenuTraceCount < AUTO_CAMPAIGN_MENU_TRACE_LIMIT) {
                autoCampaignMenuTraceCount++;
                System.out.println(
                        "Fixer: auto campaign menu select stack (" + autoCampaignMenuTraceCount + "): "
                                + stackTraceToString(t));
            }
            String chain = describeThrowableChain(t);
            return annotateTitleMenuFailure(t, chain);
        }
    }

    private static String annotateTitleMenuFailure(Throwable t, String baseMessage) {
        String message = baseMessage == null ? "" : baseMessage;
        if (isTitleNewGameMenuNpe(t) && message.indexOf("[title-newgame-menu-npe]") < 0) {
            if (message.length() == 0) {
                return "[title-newgame-menu-npe]";
            }
            return message + " [title-newgame-menu-npe]";
        }
        return message;
    }

    private static boolean isTitleNewGameMenuNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && (lower.indexOf("titlescreenstate.shownewgamedialog") >= 0
                        || lower.indexOf("menuitemselected") >= 0);
    }

    private static boolean isNewGameMenuSelection(String label, Object enumValue) {
        String labelText = label == null ? "" : label.toLowerCase();
        if (labelText.indexOf("new game") >= 0) {
            return true;
        }
        if (enumValue != null) {
            String enumText = String.valueOf(enumValue).toLowerCase();
            if (enumText.indexOf("new") >= 0 && enumText.indexOf("game") >= 0) {
                return true;
            }
            if ("new_game".equals(enumText) || "newgame".equals(enumText)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPendingNewGamePreflightFailure(String message) {
        if (message == null || message.length() == 0) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.indexOf("new-game preflight pending") >= 0
                || lower.indexOf("direct new-game preflight pending") >= 0;
    }

    private static void maybeLogNewGamePreflightIssue(String issue) {
        String safeIssue = issue == null ? "unknown" : issue;
        long now = System.currentTimeMillis();
        boolean changed =
                autoCampaignLastNewGamePreflightIssue == null
                        || !autoCampaignLastNewGamePreflightIssue.equals(safeIssue);
        if (changed || now - autoCampaignNewGamePreflightLogAt >= 15000L) {
            System.out.println("Fixer: auto campaign new-game preflight pending: " + safeIssue);
            autoCampaignLastNewGamePreflightIssue = safeIssue;
            autoCampaignNewGamePreflightLogAt = now;
        }
    }

    private static boolean allowMutatingSpecPreflight() {
        return Boolean.parseBoolean(
                System.getProperty(AUTO_CAMPAIGN_MUTATING_SPEC_PREFLIGHT_PROPERTY, "false"));
    }

    private static boolean allowOrbitalJunkFallbackWhenNonMutating() {
        return Boolean.parseBoolean(
                System.getProperty(
                        AUTO_CAMPAIGN_NON_MUTATING_ORBITAL_JUNK_FALLBACK_PROPERTY, "true"));
    }

    private static boolean allowOrbitalJunkSpecWatchdog() {
        return Boolean.parseBoolean(
                System.getProperty(AUTO_CAMPAIGN_ORBITAL_SPEC_WATCHDOG_PROPERTY, "true"));
    }

    private static Thread startOrbitalJunkSpecWatchdog(
            final boolean[] stopFlag, final boolean mutatingSpecPreflight) {
        if (stopFlag == null || stopFlag.length == 0) {
            return null;
        }
        if (!allowOrbitalJunkSpecWatchdog()) {
            return null;
        }
        final long pollMs =
                Math.max(
                        20L,
                        Math.min(
                                250L,
                                parseLongProperty(
                                        "starsector.autoCampaignOrbitalSpecWatchdogPollMs", 40L)));
        final long logIntervalMs =
                Math.max(
                        3000L,
                        parseLongProperty("starsector.autoCampaignOrbitalSpecWatchdogLogMs", 12000L));
        final long maxDurationMs =
                Math.max(
                        2000L,
                        parseLongProperty(
                                "starsector.autoCampaignOrbitalSpecWatchdogMaxMs", 120000L));

        Thread watchdog =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                long startedAt = System.currentTimeMillis();
                                long nextLogAt = startedAt + logIntervalMs;
                                while (!stopFlag[0]) {
                                    long now = System.currentTimeMillis();
                                    if (now - startedAt >= maxDurationMs) {
                                        System.out.println(
                                                "Fixer: orbital_junk spec watchdog timed out after "
                                                        + maxDurationMs
                                                        + "ms.");
                                        return;
                                    }
                                    String issue = ensureOrbitalJunkCustomEntitySpecFallback(false);
                                    if (issue != null && now >= nextLogAt) {
                                        System.out.println(
                                                "Fixer: orbital_junk spec watchdog warning"
                                                        + (mutatingSpecPreflight ? "" : " (non-mutating)")
                                                        + ": "
                                                        + issue);
                                        nextLogAt = now + logIntervalMs;
                                    }
                                    try {
                                        Thread.sleep(pollMs);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                }
                            }
                        },
                        "Fixer-OrbitalSpecWatchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        return watchdog;
    }

    private static String maybeWaitForCoreSpecBaselineBeforeDirectNewGame() {
        String baselineIssue = checkCoreSpecStoreBaselineForDirectNewGame();
        if (baselineIssue == null) {
            autoCampaignCoreSpecBaselinePendingSince = 0L;
            autoCampaignCoreSpecBaselinePendingCount = 0;
            return null;
        }

        String repairedBaselineIssue = maybeRepairCoreSpecBaselineForDirectNewGame(baselineIssue);
        if (repairedBaselineIssue == null) {
            autoCampaignCoreSpecBaselinePendingSince = 0L;
            autoCampaignCoreSpecBaselinePendingCount = 0;
            return null;
        }
        baselineIssue = repairedBaselineIssue;

        long now = System.currentTimeMillis();
        if (autoCampaignCoreSpecBaselinePendingSince <= 0L) {
            autoCampaignCoreSpecBaselinePendingSince = now;
        }
        autoCampaignCoreSpecBaselinePendingCount++;

        long pendingMs = Math.max(0L, now - autoCampaignCoreSpecBaselinePendingSince);
        long maxWaitMs =
                Math.max(
                        5000L,
                        parseLongProperty(
                                "starsector.autoCampaignCoreSpecBaselineWaitMs", 45000L));
        int maxPendingAttempts =
                Math.max(
                        1,
                        parseIntProperty(
                                "starsector.autoCampaignCoreSpecBaselinePendingLimit", 16));
        if (pendingMs < maxWaitMs && autoCampaignCoreSpecBaselinePendingCount <= maxPendingAttempts) {
            return "core spec baseline not ready ("
                    + pendingMs
                    + "ms/"
                    + maxWaitMs
                    + "ms, attempt "
                    + autoCampaignCoreSpecBaselinePendingCount
                    + "/"
                    + maxPendingAttempts
                    + "): "
                    + baselineIssue;
        }

        if (allowMutatingSpecPreflight()) {
            System.out.println(
                    "Fixer: direct-new-game core spec baseline unresolved after wait window; proceeding with repair path: "
                            + baselineIssue);
        } else {
            System.out.println(
                    "Fixer: direct-new-game core spec baseline unresolved after wait window; proceeding without mutating spec preflight: "
                            + baselineIssue);
        }
        autoCampaignCoreSpecBaselinePendingSince = 0L;
        autoCampaignCoreSpecBaselinePendingCount = 0;
        return null;
    }

    private static String maybeRepairCoreSpecBaselineForDirectNewGame(String baselineIssue) {
        if (baselineIssue == null || baselineIssue.trim().isEmpty()) {
            return baselineIssue;
        }
        if (!allowMutatingSpecPreflight()) {
            String issueLower = baselineIssue.toLowerCase();
            if (issueLower.indexOf("faction:") >= 0) {
                if (shouldSkipFactionPreflight()) {
                    String factionRepairIssue = ensureFactionSpecsReadyForDirectNewGame(true);
                    if (factionRepairIssue == null) {
                        String rechecked = checkCoreSpecStoreBaselineForDirectNewGame();
                        if (rechecked == null) {
                            System.out.println(
                                    "Fixer: core spec baseline repaired via non-mutating faction spec repair.");
                            return null;
                        }
                        baselineIssue = rechecked;
                        issueLower = baselineIssue.toLowerCase();
                    }
                } else {
                    String warmupResult = maybeWarmupFactionSpecsForNewGame(baselineIssue);
                    if (warmupResult == null) {
                        String rechecked = checkCoreSpecStoreBaselineForDirectNewGame();
                        if (rechecked == null) {
                            System.out.println(
                                    "Fixer: core spec baseline repaired via non-mutating faction warmup.");
                            return null;
                        }
                        baselineIssue = rechecked;
                        issueLower = baselineIssue.toLowerCase();
                    }
                }
            }

            if (issueLower.indexOf("commodity:supplies") >= 0
                    || issueLower.indexOf("commodity:drugs") >= 0) {
                String commodityRepairIssue = ensureCommoditySpecsReadyForDirectNewGame();
                if (commodityRepairIssue != null) {
                    System.out.println(
                            "Fixer: non-mutating commodity preflight issue: " + commodityRepairIssue);
                }
                String rechecked = checkCoreSpecStoreBaselineForDirectNewGame();
                if (rechecked == null) {
                    System.out.println(
                            "Fixer: core spec baseline repaired via non-mutating commodity preload.");
                    return null;
                }
                if (!rechecked.equals(baselineIssue)) {
                    System.out.println(
                            "Fixer: core spec baseline changed after non-mutating commodity preload: before="
                                    + baselineIssue
                                    + " after="
                                    + rechecked);
                }
                baselineIssue = rechecked;
            }
            return baselineIssue;
        }
        String issueLower = baselineIssue.toLowerCase();
        boolean attempted = false;
        ArrayList<String> notes = new ArrayList<String>();

        if (issueLower.indexOf("custom_entity:orbital_junk") >= 0) {
            attempted = true;
            String customEntityRepairIssue = ensureCustomEntitySpecsReadyForDirectNewGame();
            if (customEntityRepairIssue != null) {
                notes.add("custom-entity=" + customEntityRepairIssue);
            }
            String orbitalJunkFallbackIssue = ensureOrbitalJunkCustomEntitySpecFallback();
            if (orbitalJunkFallbackIssue != null) {
                notes.add("orbital-junk-fallback=" + orbitalJunkFallbackIssue);
            }
        }
        if (issueLower.indexOf("commodity:supplies") >= 0
                || issueLower.indexOf("commodity:drugs") >= 0) {
            attempted = true;
            String commodityRepairIssue = ensureCommoditySpecsReadyForDirectNewGame();
            if (commodityRepairIssue != null) {
                notes.add("commodity=" + commodityRepairIssue);
            }
        }
        if (issueLower.indexOf("submarket:open_market") >= 0) {
            attempted = true;
            String submarketRepairIssue = ensureSubmarketSpecsReadyForDirectNewGame();
            if (submarketRepairIssue != null) {
                notes.add("submarket=" + submarketRepairIssue);
            }
        }
        if (issueLower.indexOf("terrain:asteroid_field") >= 0) {
            attempted = true;
            String terrainRepairIssue = ensureTerrainSpecsReadyForDirectNewGame();
            if (terrainRepairIssue != null) {
                notes.add("terrain=" + terrainRepairIssue);
            }
        }
        if (issueLower.indexOf("faction:neutral") >= 0) {
            attempted = true;
            String factionRepairIssue = ensureFactionSpecsReadyForDirectNewGame(true);
            if (factionRepairIssue != null) {
                notes.add("faction=" + factionRepairIssue);
            }
        }

        if (!attempted) {
            return baselineIssue;
        }

        String rechecked = checkCoreSpecStoreBaselineForDirectNewGame();
        if (rechecked == null) {
            if (!notes.isEmpty()) {
                System.out.println(
                        "Fixer: core spec baseline repaired via targeted preload: "
                                + String.join("; ", notes));
            } else {
                System.out.println("Fixer: core spec baseline repaired via targeted preload.");
            }
            return null;
        }

        if (!rechecked.equals(baselineIssue)) {
            System.out.println(
                    "Fixer: core spec baseline changed after targeted preload: before="
                            + baselineIssue
                            + " after="
                            + rechecked
                            + (notes.isEmpty() ? "" : " notes=" + String.join("; ", notes)));
        }
        return rechecked;
    }

    private static String checkCoreSpecStoreBaselineForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            List<String> missing = new ArrayList<String>();
            boolean mutatingSpecPreflight = allowMutatingSpecPreflight();

            boolean hasNeutralFaction = false;
            String[] factionCandidates =
                    new String[] {
                        "com.fs.starfarer.loading.if",
                        "com.fs.starfarer.loading.specs.FactionSpec"
                    };
            for (String className : factionCandidates) {
                try {
                    Class<?> candidate = Class.forName(className);
                    if (lookupSpecById(specStoreClass, candidate, "neutral") != null) {
                        hasNeutralFaction = true;
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (!hasNeutralFaction) {
                missing.add("faction:neutral");
            }

            boolean hasSuppliesCommodity = hasCommoditySpecForId(specStoreClass, "supplies");
            if (!hasSuppliesCommodity) {
                missing.add("commodity:supplies");
            }
            boolean hasDrugsCommodity = hasCommoditySpecForId(specStoreClass, "drugs");
            if (!hasDrugsCommodity) {
                missing.add("commodity:drugs");
            }

            if (mutatingSpecPreflight) {
                if (!hasSpecForClassName(
                        specStoreClass,
                        "com.fs.starfarer.loading.specs.Stringsuper",
                        "asteroid_field")) {
                    missing.add("terrain:asteroid_field");
                }
                Class<?> customEntitySpecClass = resolveCustomEntitySpecClass();
                if (customEntitySpecClass == null
                        || lookupSpecById(specStoreClass, customEntitySpecClass, "orbital_junk")
                                == null) {
                    missing.add("custom_entity:orbital_junk");
                }
                if (!hasSpecForClassName(
                        specStoreClass, "com.fs.starfarer.loading.for", "open_market")) {
                    missing.add("submarket:open_market");
                }
            }

            if (missing.isEmpty()) {
                return null;
            }
            return String.join(", ", missing);
        } catch (Throwable t) {
            return "core spec baseline reflection issue: " + describeThrowableChain(t);
        }
    }

    private static boolean hasSpecForClassName(
            Class<?> specStoreClass, String className, String specId) {
        if (specStoreClass == null || className == null || className.length() == 0) {
            return false;
        }
        if (specId == null || specId.length() == 0) {
            return false;
        }
        try {
            Class<?> specClass = Class.forName(className);
            return lookupSpecById(specStoreClass, specClass, specId) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Class<?> resolveCustomEntitySpecClass() {
        Class<?> settingsClass = resolveCustomEntitySpecClassFromSettings();
        if (settingsClass != null) {
            return settingsClass;
        }
        try {
            Class<?> entityClass = Class.forName("com.fs.starfarer.campaign.CustomCampaignEntity");
            Field specField = findFieldRecursive(entityClass, "spec");
            if (specField != null) {
                specField.setAccessible(true);
                Class<?> type = specField.getType();
                if (type != null) {
                    return type;
                }
            }
            Class<?> cursor = entityClass;
            while (cursor != null && cursor != Object.class) {
                for (Field field : cursor.getDeclaredFields()) {
                    Class<?> type = field.getType();
                    if (type == null || type.isPrimitive()) {
                        continue;
                    }
                    String typeName = type.getName();
                    if (typeName.startsWith("com.fs.starfarer.loading.specs.")) {
                        return type;
                    }
                }
                cursor = cursor.getSuperclass();
            }
        } catch (Throwable ignored) {
        }

        String[] candidates =
                new String[] {
                    "com.fs.starfarer.loading.specs.oooo_3",
                    "com.fs.starfarer.loading.specs.oooO",
                    "com.fs.starfarer.loading.specs.CustomEntitySpec"
                };
        for (String className : candidates) {
            try {
                return Class.forName(className);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Class<?> resolveCustomEntitySpecClassFromSettings() {
        try {
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSettings = findMethodRecursive(globalClass, "getSettings");
            if (getSettings == null) {
                return null;
            }
            getSettings.setAccessible(true);
            Object settings = getSettings.invoke(null);
            if (settings == null) {
                return null;
            }
            Method getCustomEntitySpec =
                    findMethodRecursive(settings.getClass(), "getCustomEntitySpec", String.class);
            if (getCustomEntitySpec == null) {
                return null;
            }
            getCustomEntitySpec.setAccessible(true);
            String[] probeIds =
                    new String[] {"orbital_junk", "comm_relay", "base_campaign_objective"};
            for (String id : probeIds) {
                try {
                    Object spec = getCustomEntitySpec.invoke(settings, id);
                    if (spec != null) {
                        return spec.getClass();
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static List<Class<?>> resolveCustomEntitySpecClassCandidates() {
        LinkedHashSet<Class<?>> out = new LinkedHashSet<Class<?>>();
        Class<?> primary = resolveCustomEntitySpecClass();
        if (primary != null) {
            out.add(primary);
        }

        String[] classNames =
                new String[] {
                    "com.fs.starfarer.loading.specs.oooo_3",
                    "com.fs.starfarer.loading.specs.oooO",
                    "com.fs.starfarer.loading.specs.CustomEntitySpec"
                };
        LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        loaders.add(Fixer.class.getClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        addLoaderForClass(loaders, "com.fs.starfarer.campaign.CustomCampaignEntity");
        addLoaderForClass(loaders, "com.fs.starfarer.loading.SpecStore");
        addLoaderForClass(loaders, "com.fs.starfarer.api.Global");
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                loaders.add(t.getContextClassLoader());
            }
        } catch (Throwable ignored) {
        }
        for (String className : classNames) {
            if (className == null || className.length() == 0) {
                continue;
            }
            for (ClassLoader loader : loaders) {
                if (loader == null) {
                    continue;
                }
                try {
                    Class<?> cls = Class.forName(className, false, loader);
                    if (cls != null) {
                        out.add(cls);
                    }
                } catch (Throwable ignored) {
                }
            }
            try {
                Class<?> cls = Class.forName(className);
                if (cls != null) {
                    out.add(cls);
                }
            } catch (Throwable ignored) {
            }
        }
        Class<?> fromSettings = resolveCustomEntitySpecClassFromSettings();
        if (fromSettings != null) {
            out.add(fromSettings);
        }
        return new ArrayList<Class<?>>(out);
    }

    private static String summarizeClassCandidates(List<Class<?>> classes) {
        if (classes == null || classes.isEmpty()) {
            return "<none>";
        }
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (Class<?> cls : classes) {
            if (cls == null) {
                continue;
            }
            if (idx > 0) {
                sb.append(", ");
            }
            if (idx >= 5) {
                sb.append("... +").append(classes.size() - idx);
                break;
            }
            sb.append(cls.getName()).append("@").append(shortLoader(cls.getClassLoader()));
            idx++;
        }
        if (sb.length() == 0) {
            return "<none>";
        }
        return sb.toString();
    }

    private static Class<?> resolveSpecStoreClassForCustomEntityClass(Class<?> customEntitySpecClass) {
        LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        if (customEntitySpecClass != null) {
            loaders.add(customEntitySpecClass.getClassLoader());
        }
        loaders.add(Fixer.class.getClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        addLoaderForClass(loaders, "com.fs.starfarer.loading.SpecStore");
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                Class<?> cls = Class.forName("com.fs.starfarer.loading.SpecStore", false, loader);
                if (cls != null) {
                    return cls;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            return Class.forName("com.fs.starfarer.loading.SpecStore");
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean hasCommoditySpecForId(Class<?> specStoreClass, String commodityId) {
        if (specStoreClass == null || commodityId == null || commodityId.length() == 0) {
            return false;
        }
        String[] commodityCandidates =
                new String[] {
                    "com.fs.starfarer.loading.specs.CommoditySpec",
                    "com.fs.starfarer.loading.CommoditySpec",
                    "com.fs.starfarer.loading.F"
                };
        for (String className : commodityCandidates) {
            try {
                Class<?> candidate = Class.forName(className);
                if (lookupSpecById(specStoreClass, candidate, commodityId) != null) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static String maybeWarmupFactionSpecsForNewGame(String currentIssue) {
        long now = System.currentTimeMillis();
        if (autoCampaignFactionWarmupAttempts >= AUTO_CAMPAIGN_FACTION_WARMUP_ATTEMPT_LIMIT) {
            return "attempt-limit";
        }
        if (now - autoCampaignFactionWarmupAttemptAt < AUTO_CAMPAIGN_FACTION_WARMUP_COOLDOWN_MS) {
            return "cooldown";
        }
        autoCampaignFactionWarmupAttemptAt = now;
        autoCampaignFactionWarmupAttempts++;

        String warmupIssue = warmupFactionSpecsViaSpecStore();
        String afterWarmupIssue = ensureFactionSpecsReadyForDirectNewGame(false);
        if (afterWarmupIssue == null) {
            System.out.println(
                    "Fixer: auto campaign faction warmup attempt "
                            + autoCampaignFactionWarmupAttempts
                            + " completed"
                            + (warmupIssue == null ? "." : " (with tolerated loader issue: " + warmupIssue + ")."));
            return null;
        }

        if (warmupIssue != null) {
            System.out.println(
                    "Fixer: auto campaign faction warmup attempt "
                            + autoCampaignFactionWarmupAttempts
                            + " failed: "
                            + warmupIssue
                            + " (preflight="
                            + currentIssue
                            + ", afterWarmup="
                            + afterWarmupIssue
                            + ")");
        }
        System.out.println(
                "Fixer: auto campaign faction warmup attempt "
                        + autoCampaignFactionWarmupAttempts
                        + " incomplete: "
                        + afterWarmupIssue);
        return "still-missing";
    }

    private static boolean shouldSkipFactionPreflight() {
        return Boolean.parseBoolean(
                System.getProperty("starsector.autoCampaignSkipFactionPreflight", "true"));
    }

    private static String warmupFactionSpecsViaSpecStore() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            String firstIssue = null;
            String hullWarmupIssue = warmupShipHullSpecsForFactionPreflight();
            if (hullWarmupIssue != null) {
                System.out.println("Fixer: warmup ship-hull issue: " + hullWarmupIssue);
                firstIssue = "ship-hulls:" + hullWarmupIssue;
            }
            String criticalHullIssue = ensureCriticalHullSpecsForFactionWarmup();
            if (criticalHullIssue != null) {
                System.out.println("Fixer: warmup critical-hull issue: " + criticalHullIssue);
                if (firstIssue == null) {
                    firstIssue = "critical-hulls:" + criticalHullIssue;
                }
            }
            String criticalVariantIssue = ensureCriticalVariantsForFactionWarmup();
            if (criticalVariantIssue != null) {
                System.out.println("Fixer: warmup critical-variant pre issue: " + criticalVariantIssue);
                if (firstIssue == null) {
                    firstIssue = "critical-variants-pre:" + criticalVariantIssue;
                }
            }
            String variantsIssue = invokeNoArgMethodWithTolerance(specStoreClass, "oO0000", 12000L);
            if (variantsIssue != null) {
                System.out.println("Fixer: warmup SpecStore.oO0000 issue: " + variantsIssue);
                if (firstIssue == null) {
                    firstIssue = "oO0000:" + variantsIssue;
                }
            }
            String criticalVariantPostIssue = ensureCriticalVariantsForFactionWarmup();
            if (criticalVariantPostIssue != null) {
                System.out.println("Fixer: warmup critical-variant post issue: " + criticalVariantPostIssue);
                if (firstIssue == null) {
                    firstIssue = "critical-variants-post:" + criticalVariantPostIssue;
                }
            }
            Class<?> resourceLoaderStateClass =
                    Class.forName("com.fs.starfarer.loading.ResourceLoaderState");
            Method warmup = findMethodRecursive(specStoreClass, "oo0000", resourceLoaderStateClass);
            if (warmup == null) {
                return "no-faction-loader-method";
            }
            try {
                warmup.setAccessible(true);
                warmup.invoke(null, new Object[] {null});
                return firstIssue;
            } catch (Throwable t) {
                String warmupIssue = describeThrowableChain(t);
                List<String> repairedVariants = new ArrayList<String>();
                int repairAttempts = 0;
                while (repairAttempts < 192) {
                    String missingVariantId = extractMissingHullVariantIdFromChain(warmupIssue);
                    if (missingVariantId == null) {
                        break;
                    }
                    repairAttempts++;
                    String injectIssue = ensureSingleVariantPresentForFactionWarmup(missingVariantId);
                    if (injectIssue != null) {
                        if (shouldAttemptWarmupVariantAlias(injectIssue)) {
                            String aliasIssue =
                                    ensureVariantAliasForWarmup(missingVariantId, "lasher_Standard");
                            if (aliasIssue == null) {
                                repairedVariants.add(missingVariantId + "(alias)");
                                try {
                                    warmup.invoke(null, new Object[] {null});
                                    System.out.println(
                                            "Fixer: warmup SpecStore.oo0000 recovered after aliasing missing variants "
                                                    + String.join(", ", repairedVariants)
                                                    + ".");
                                    return firstIssue;
                                } catch (Throwable aliasRetryThrowable) {
                                    warmupIssue = describeThrowableChain(aliasRetryThrowable);
                                    continue;
                                }
                            }
                            injectIssue = injectIssue + ";variant-alias=" + aliasIssue;
                        }
                        return "oo0000:"
                                + warmupIssue
                                + ";variant-repair="
                                + missingVariantId
                                + ":"
                                + injectIssue
                                + ";repaired="
                                + String.join(",", repairedVariants);
                    }
                    repairedVariants.add(missingVariantId);
                    try {
                        warmup.invoke(null, new Object[] {null});
                        System.out.println(
                                "Fixer: warmup SpecStore.oo0000 recovered after injecting missing variants "
                                        + String.join(", ", repairedVariants)
                                        + ".");
                        return firstIssue;
                    } catch (Throwable retryThrowable) {
                        warmupIssue = describeThrowableChain(retryThrowable);
                    }
                }
                if (!repairedVariants.isEmpty()) {
                    return "oo0000:"
                            + warmupIssue
                            + ";variant-repair-retry="
                            + repairAttempts
                            + ":"
                            + String.join(",", repairedVariants);
                }
                return "oo0000:" + warmupIssue;
            }
        } catch (Throwable t) {
            return "specstore-warmup-exception:" + describeThrowableChain(t);
        }
    }

    private static String extractMissingHullVariantIdFromChain(String chain) {
        if (chain == null || chain.length() == 0) {
            return null;
        }
        String marker = "Ship hull variant [";
        int start = chain.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int idStart = start + marker.length();
        int idEnd = chain.indexOf(']', idStart);
        if (idEnd <= idStart) {
            return null;
        }
        String variantId = chain.substring(idStart, idEnd).trim();
        if (variantId.length() == 0) {
            return null;
        }
        return variantId;
    }

    private static String extractInvalidShipVariantIdFromChain(String chain) {
        if (chain == null || chain.length() == 0) {
            return null;
        }
        String marker = "] is not a valid ship variant id";
        int markerIndex = chain.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        int start = chain.lastIndexOf('[', markerIndex);
        if (start < 0 || start + 1 >= markerIndex) {
            return null;
        }
        String variantId = chain.substring(start + 1, markerIndex).trim();
        if (variantId.length() == 0) {
            return null;
        }
        return variantId;
    }

    private static String extractMissingHullSpecIdFromChain(String chain) {
        if (chain == null || chain.length() == 0) {
            return null;
        }
        String marker = "Ship hull spec [";
        int start = chain.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int idStart = start + marker.length();
        int idEnd = chain.indexOf(']', idStart);
        if (idEnd <= idStart) {
            return null;
        }
        String hullId = chain.substring(idStart, idEnd).trim();
        if (hullId.length() == 0) {
            return null;
        }
        return hullId;
    }

    private static String extractMissingWeaponSpecIdFromChain(String chain) {
        if (chain == null || chain.length() == 0) {
            return null;
        }
        String marker = "Weapon spec [";
        int start = chain.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int idStart = start + marker.length();
        int idEnd = chain.indexOf(']', idStart);
        if (idEnd <= idStart) {
            return null;
        }
        String weaponId = chain.substring(idStart, idEnd).trim();
        if (weaponId.length() == 0) {
            return null;
        }
        return weaponId;
    }

    private static String invokeNoArgLoaderWithDuplicateTolerance(
            String className, String methodName) {
        try {
            Class<?> targetClass = Class.forName(className);
            return invokeNoArgMethodWithTolerance(targetClass, methodName, 12000L);
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static String warmupShipHullSpecsForFactionPreflight() {
        try {
            Class<?> hullLoaderClass = Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader");
            Method[] methods = hullLoaderClass.getDeclaredMethods();
            int invoked = 0;
            for (Method method : methods) {
                if (method == null) {
                    continue;
                }
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterTypes().length != 0) {
                    continue;
                }
                if (method.getReturnType() != Void.TYPE) {
                    continue;
                }
                invoked++;
                String issue = invokeNoArgMethodWithTolerance(hullLoaderClass, method.getName(), 12000L);
                if (issue != null) {
                    return method.getName() + ":" + issue;
                }
            }
            if (invoked == 0) {
                return "no-noarg-static-hull-loaders";
            }
            return null;
        } catch (Throwable t) {
            return "hull-loader-reflection:" + describeThrowableChain(t);
        }
    }

    private static String ensureCriticalHullSpecsForFactionWarmup() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> hullLoaderClass = Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader");
            List<Class<?>> hullSpecClasses = new ArrayList<Class<?>>();
            try {
                hullSpecClasses.add(Class.forName("com.fs.starfarer.loading.specs.g"));
            } catch (Throwable ignored) {
            }
            try {
                hullSpecClasses.add(Class.forName("com.fs.starfarer.loading.specs.ShipHullSpec"));
            } catch (Throwable ignored) {
            }
            LinkedHashMap<String, String> required = new LinkedHashMap<String, String>();
            required.put("ziggurat", "data/hulls/ziggurat.ship");
            required.put("afflictor_d_pirates", "data/hulls/skins/afflictor_d_pirates.skin");

            if (hullSpecClasses.isEmpty()) {
                // Some builds expose hull specs through obfuscated classes we can't resolve here.
                // Fall back to best-effort loader priming instead of failing preflight immediately.
                for (Map.Entry<String, String> entry : required.entrySet()) {
                    String sourcePath = entry.getValue();
                    if (sourcePath == null || sourcePath.trim().isEmpty()) {
                        continue;
                    }
                    String loadIssue =
                            invokeStringArgMethodWithTolerance(hullLoaderClass, sourcePath, 12000L);
                    if (loadIssue != null) {
                        return "hull-loader-issue:" + loadIssue;
                    }
                }
                return null;
            }

            List<String> unresolved = new ArrayList<String>();
            for (Map.Entry<String, String> entry : required.entrySet()) {
                String hullId = entry.getKey();
                String sourcePath = entry.getValue();
                if (hullId == null || hullId.trim().isEmpty()) {
                    continue;
                }
                if (lookupSpecByIdAcrossClasses(specStoreClass, hullSpecClasses, hullId) != null) {
                    continue;
                }
                String issue = invokeStringArgMethodWithTolerance(hullLoaderClass, sourcePath, 12000L);
                Object loaded = lookupSpecByIdAcrossClasses(specStoreClass, hullSpecClasses, hullId);
                if (loaded == null) {
                    unresolved.add(
                            hullId + "(" + (issue == null ? "still-missing-after-loader" : issue) + ")");
                }
            }
            if (!unresolved.isEmpty()) {
                return String.join(", ", unresolved);
            }
            return null;
        } catch (Throwable t) {
            return "critical-hull-check:" + describeThrowableChain(t);
        }
    }

    private static String ensureCriticalVariantsForFactionWarmup() {
        try {
            List<String> required = new ArrayList<String>();
            required.add("ziggurat_Strike");
            required.add("afflictor_d_pirates_Strike");

            List<String> unresolved = new ArrayList<String>();
            for (String variantId : required) {
                String issue = ensureSingleVariantPresentForFactionWarmup(variantId);
                if (issue != null && "ziggurat_Strike".equals(variantId)) {
                    String aliasIssue =
                            ensureVariantAliasForWarmup(variantId, "lasher_Standard");
                    if (aliasIssue == null) {
                        issue = null;
                        System.out.println(
                                "Fixer: warmup aliased missing variant "
                                        + variantId
                                        + " to fallback lasher_Standard.");
                    } else {
                        issue = issue + ";alias:" + aliasIssue;
                    }
                }
                if (issue != null) {
                    System.out.println(
                            "Fixer: warmup critical variant unresolved "
                                    + variantId
                                    + " -> "
                                    + issue);
                    unresolved.add(variantId + "(" + issue + ")");
                }
            }
            if (!unresolved.isEmpty()) {
                return String.join(", ", unresolved);
            }
            return null;
        } catch (Throwable t) {
            return "critical-variant-check:" + describeThrowableChain(t);
        }
    }

    private static String ensureGalatiaDerelictVariantsForWarmup() {
        try {
            LinkedHashSet<String> required = new LinkedHashSet<String>();
            required.add("wolf_Assault");
            required.add("lasher_CS");
            required.add("kite_Standard");
            required.add("tarsus_d_Standard");
            required.add("buffalo2_FS");
            required.add("hammerhead_Balanced");
            required.add("condor_Support");
            required.add("dram_Light");

            List<String> unresolved = new ArrayList<String>();
            for (String variantId : required) {
                if (variantId == null || variantId.trim().isEmpty()) {
                    continue;
                }
                String issue = ensureSingleVariantPresentForFactionWarmup(variantId);
                if (issue != null && shouldAttemptWarmupVariantAlias(issue)) {
                    String aliasIssue = ensureVariantAliasForWarmup(variantId, "lasher_Standard");
                    if (aliasIssue == null) {
                        issue = null;
                        System.out.println(
                                "Fixer: warmup aliased Galatia derelict variant "
                                        + variantId
                                        + " to lasher_Standard.");
                    } else {
                        issue = issue + ";alias:" + aliasIssue;
                    }
                }
                if (issue != null) {
                    unresolved.add(variantId + "(" + issue + ")");
                }
            }
            if (!unresolved.isEmpty()) {
                return String.join(", ", unresolved);
            }
            return null;
        } catch (Throwable t) {
            return "galatia-derelict-variant-check:" + describeThrowableChain(t);
        }
    }

    private static String ensureVariantAliasForWarmup(String targetVariantId, String fallbackVariantId) {
        if (targetVariantId == null || targetVariantId.trim().isEmpty()) {
            return "target-id-missing";
        }
        if (fallbackVariantId == null || fallbackVariantId.trim().isEmpty()) {
            return "fallback-id-missing";
        }
        String targetId = targetVariantId.trim();
        String fallbackId = fallbackVariantId.trim();
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> hullVariantSpecClass =
                    Class.forName("com.fs.starfarer.loading.specs.HullVariantSpec");
            Object existing = lookupSpecById(specStoreClass, hullVariantSpecClass, targetId);
            if (existing != null) {
                return null;
            }
            String fallbackEnsureIssue = ensureSingleVariantPresentForFactionWarmup(fallbackId);
            if (fallbackEnsureIssue != null) {
                System.out.println(
                        "Fixer: warmup fallback variant ensure issue for "
                                + fallbackId
                                + ": "
                                + fallbackEnsureIssue);
            }

            Class<?> variantStoreClass = Class.forName("com.fs.starfarer.loading.new");
            Method getVariantWithOptional =
                    findMethodRecursive(variantStoreClass, "o00000", String.class, Boolean.TYPE);
            Method getVariantRequired =
                    findMethodRecursive(variantStoreClass, "o00000", String.class);

            Object fallbackSpec = null;
            if (getVariantWithOptional != null) {
                getVariantWithOptional.setAccessible(true);
                fallbackSpec = getVariantWithOptional.invoke(null, fallbackId, Boolean.TRUE);
            }
            if (fallbackSpec == null && getVariantRequired != null) {
                getVariantRequired.setAccessible(true);
                try {
                    fallbackSpec = getVariantRequired.invoke(null, fallbackId);
                } catch (Throwable ignored) {
                }
            }
            if (fallbackSpec == null) {
                return "fallback-not-found:" + fallbackId;
            }
            if (!hullVariantSpecClass.isInstance(fallbackSpec)) {
                return "fallback-type-mismatch:" + fallbackSpec.getClass().getName();
            }

            Method cloneMethod = findMethodRecursive(hullVariantSpecClass, "clone");
            if (cloneMethod == null) {
                return "clone-method-missing";
            }
            cloneMethod.setAccessible(true);
            Object aliasSpec = cloneMethod.invoke(fallbackSpec);
            if (aliasSpec == null) {
                return "clone-returned-null";
            }

            Method setVariantId = findMethodRecursive(hullVariantSpecClass, "setHullVariantId", String.class);
            if (setVariantId == null) {
                return "setHullVariantId-missing";
            }
            setVariantId.setAccessible(true);
            setVariantId.invoke(aliasSpec, targetId);

            try {
                Class<?> variantSourceClass = Class.forName("com.fs.starfarer.api.loading.VariantSource");
                Method setSource = findMethodRecursive(hullVariantSpecClass, "setSource", variantSourceClass);
                if (setSource != null && variantSourceClass.isEnum()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum> enumClass =
                            (Class<? extends Enum>) variantSourceClass.asSubclass(Enum.class);
                    Object stock = Enum.valueOf(enumClass, "STOCK");
                    setSource.setAccessible(true);
                    setSource.invoke(aliasSpec, stock);
                }
            } catch (Throwable ignored) {
            }

            Method registerWithFlag =
                    findMethodRecursive(variantStoreClass, "o00000", hullVariantSpecClass, Boolean.TYPE);
            Method register =
                    findMethodRecursive(variantStoreClass, "o00000", hullVariantSpecClass);
            if (registerWithFlag != null) {
                registerWithFlag.setAccessible(true);
                registerWithFlag.invoke(null, aliasSpec, Boolean.FALSE);
            } else if (register != null) {
                register.setAccessible(true);
                register.invoke(null, aliasSpec);
            } else {
                return "variant-register-method-missing";
            }

            try {
                Method registerSpec =
                        findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
                if (registerSpec != null) {
                    registerSpec.setAccessible(true);
                    registerSpec.invoke(null, hullVariantSpecClass, targetId, aliasSpec);
                }
            } catch (Throwable ignored) {
            }

            Object after = lookupSpecById(specStoreClass, hullVariantSpecClass, targetId);
            if (after == null) {
                return "alias-register-verify-failed";
            }
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static String ensureSingleVariantPresentForFactionWarmup(String variantId) {
        return ensureSingleVariantPresentForFactionWarmup(variantId, true);
    }

    private static String ensureSingleVariantPresentForFactionWarmup(
            String variantId, boolean allowHullRepair) {
        if (variantId == null || variantId.trim().isEmpty()) {
            return "invalid-variant-id";
        }
        String trimmedId = variantId.trim();
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> hullVariantSpecClass = Class.forName("com.fs.starfarer.loading.specs.HullVariantSpec");
            Object existing = lookupSpecById(specStoreClass, hullVariantSpecClass, trimmedId);
            if (existing != null) {
                return null;
            }

            String sourcePath = resolveVariantPathForWarmup(trimmedId);
            if (sourcePath == null) {
                return "source-path-missing";
            }
            Object json = loadConfigJsonViaLoadingUtils(sourcePath);
            if (json == null) {
                return "json-load-failed:" + sourcePath;
            }

            java.lang.reflect.Constructor<?> ctor = null;
            try {
                ctor = hullVariantSpecClass.getDeclaredConstructor(json.getClass());
            } catch (Throwable ignored) {
            }
            if (ctor == null) {
                try {
                    Class<?> jsonObjectClass = Class.forName("org.json.JSONObject");
                    ctor = hullVariantSpecClass.getDeclaredConstructor(jsonObjectClass);
                } catch (Throwable ignored) {
                }
            }
            if (ctor == null) {
                return "variant-ctor-missing";
            }
            ctor.setAccessible(true);
            Object spec = ctor.newInstance(json);

            try {
                Class<?> variantSourceClass = Class.forName("com.fs.starfarer.api.loading.VariantSource");
                Method setSource =
                        findMethodRecursive(hullVariantSpecClass, "setSource", variantSourceClass);
                if (setSource != null && variantSourceClass.isEnum()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum> enumClass =
                            (Class<? extends Enum>) variantSourceClass.asSubclass(Enum.class);
                    Object stock = Enum.valueOf(enumClass, "STOCK");
                    setSource.setAccessible(true);
                    setSource.invoke(spec, stock);
                }
            } catch (Throwable ignored) {
            }
            try {
                Method setSourcePath =
                        findMethodRecursive(hullVariantSpecClass, "setSourcePath", String.class);
                if (setSourcePath != null) {
                    setSourcePath.setAccessible(true);
                    setSourcePath.invoke(spec, sourcePath);
                }
            } catch (Throwable ignored) {
            }

            boolean registered = false;
            try {
                Class<?> variantStoreClass = Class.forName("com.fs.starfarer.loading.new");
                Method registerWithFlag =
                        findMethodRecursive(variantStoreClass, "o00000", hullVariantSpecClass, Boolean.TYPE);
                if (registerWithFlag != null) {
                    registerWithFlag.setAccessible(true);
                    registerWithFlag.invoke(null, spec, Boolean.FALSE);
                    registered = true;
                } else {
                    Method register =
                            findMethodRecursive(variantStoreClass, "o00000", hullVariantSpecClass);
                    if (register != null) {
                        register.setAccessible(true);
                        register.invoke(null, spec);
                        registered = true;
                    }
                }
            } catch (Throwable ignored) {
            }

            try {
                Method registerSpec =
                        findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
                if (registerSpec != null) {
                    registerSpec.setAccessible(true);
                    registerSpec.invoke(null, hullVariantSpecClass, trimmedId, spec);
                    registered = true;
                }
            } catch (Throwable ignored) {
            }

            Object after = lookupSpecById(specStoreClass, hullVariantSpecClass, trimmedId);
            if (after == null) {
                String registerIssue = registered ? "register-verify-failed" : "register-methods-unavailable";
                if (shouldAttemptWarmupVariantAlias(registerIssue)) {
                    String aliasIssue = ensureVariantAliasForWarmup(trimmedId, "lasher_Standard");
                    if (aliasIssue == null) {
                        System.out.println(
                                "Fixer: warmup aliased missing variant "
                                        + trimmedId
                                        + " after register issue "
                                        + registerIssue
                                        + ".");
                        return null;
                    }
                    return registerIssue + ";variant-alias=" + aliasIssue;
                }
                return registerIssue;
            }
            System.out.println(
                    "Fixer: warmup injected critical variant " + trimmedId + " from " + sourcePath + ".");
            return null;
        } catch (Throwable t) {
            String issue = describeThrowableChain(t);
            if (allowHullRepair) {
                String missingHullId = extractMissingHullSpecIdFromChain(issue);
                if (missingHullId != null) {
                    String hullRepairIssue = ensureSingleHullSpecPresentForWarmup(missingHullId);
                    if (hullRepairIssue == null) {
                        String retryIssue = ensureSingleVariantPresentForFactionWarmup(trimmedId, false);
                        if (retryIssue == null) {
                            return null;
                        }
                        if (shouldAttemptWarmupVariantAlias(retryIssue)) {
                            String aliasIssue = ensureVariantAliasForWarmup(trimmedId, "lasher_Standard");
                            if (aliasIssue == null) {
                                System.out.println(
                                        "Fixer: warmup aliased missing variant "
                                                + trimmedId
                                                + " after unresolved hull-spec retry for "
                                                + missingHullId
                                                + ".");
                                return null;
                            }
                            return retryIssue + ";variant-alias=" + aliasIssue;
                        }
                        return retryIssue;
                    }
                    if (shouldAttemptWarmupVariantAlias(hullRepairIssue)) {
                        String aliasIssue = ensureVariantAliasForWarmup(trimmedId, "lasher_Standard");
                        if (aliasIssue == null) {
                            System.out.println(
                                    "Fixer: warmup aliased missing variant "
                                            + trimmedId
                                            + " after hull repair issue "
                                            + missingHullId
                                            + ": "
                                            + hullRepairIssue);
                            return null;
                        }
                        return issue
                                + ";hull-repair="
                                + missingHullId
                                + ":"
                                + hullRepairIssue
                                + ";variant-alias="
                                + aliasIssue;
                    }
                    return issue
                            + ";hull-repair="
                            + missingHullId
                            + ":"
                            + hullRepairIssue;
                }
                String missingWeaponId = extractMissingWeaponSpecIdFromChain(issue);
                if (missingWeaponId != null) {
                    String weaponRepairIssue = ensureSingleWeaponSpecPresentForWarmup(missingWeaponId);
                    if (weaponRepairIssue == null) {
                        String retryIssue = ensureSingleVariantPresentForFactionWarmup(trimmedId, false);
                        if (retryIssue == null) {
                            return null;
                        }
                        if (shouldAttemptWarmupVariantAlias(retryIssue)) {
                            String aliasIssue = ensureVariantAliasForWarmup(trimmedId, "lasher_Standard");
                            if (aliasIssue == null) {
                                System.out.println(
                                        "Fixer: warmup aliased missing variant "
                                                + trimmedId
                                                + " after unresolved weapon-spec retry for "
                                                + missingWeaponId
                                                + ".");
                                return null;
                            }
                            return retryIssue + ";variant-alias=" + aliasIssue;
                        }
                        return retryIssue;
                    }
                    if (shouldAttemptWarmupVariantAlias(weaponRepairIssue)) {
                        String aliasIssue = ensureVariantAliasForWarmup(trimmedId, "lasher_Standard");
                        if (aliasIssue == null) {
                            System.out.println(
                                    "Fixer: warmup aliased missing variant "
                                            + trimmedId
                                            + " after weapon repair issue "
                                            + missingWeaponId
                                            + ": "
                                            + weaponRepairIssue);
                            return null;
                        }
                        return issue
                                + ";weapon-repair="
                                + missingWeaponId
                                + ":"
                                + weaponRepairIssue
                                + ";variant-alias="
                                + aliasIssue;
                    }
                    return issue
                            + ";weapon-repair="
                            + missingWeaponId
                            + ":"
                            + weaponRepairIssue;
                }
            }
            return issue;
        }
    }

    private static String resolveVariantPathForWarmup(String variantId) {
        String fileName = variantId + ".variant";
        try {
            String direct = "data/variants/" + fileName;
            InputStream in = openResourceStream(direct);
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
                return direct;
            }
        } catch (Throwable ignored) {
        }
        try {
            LinkedHashSet<String> files = new LinkedHashSet<String>();
            collectSpecFilesFromIndex("data/variants", new LinkedHashSet<String>(), files);
            for (String rel : files) {
                if (rel == null) {
                    continue;
                }
                String clean = rel.replace('\\', '/');
                if (clean.endsWith("/" + fileName) || clean.endsWith(fileName)) {
                    return clean;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String ensureSingleHullSpecPresentForWarmup(String hullId) {
        if (hullId == null || hullId.trim().isEmpty()) {
            return "invalid-hull-id";
        }
        String trimmedId = hullId.trim();
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> hullLoaderClass = Class.forName("com.fs.starfarer.loading.ShipHullSpecLoader");
            List<Class<?>> hullSpecClasses = new ArrayList<Class<?>>();
            try {
                hullSpecClasses.add(Class.forName("com.fs.starfarer.loading.Class"));
            } catch (Throwable ignored) {
            }
            try {
                Class<?> fallback = Class.forName("com.fs.starfarer.loading.specs.ShipHullSpec");
                if (!hullSpecClasses.contains(fallback)) {
                    hullSpecClasses.add(fallback);
                }
            } catch (Throwable ignored) {
            }
            if (lookupSpecByIdAcrossClasses(specStoreClass, hullSpecClasses, trimmedId) != null) {
                return null;
            }
            String sourcePath = resolveHullPathForWarmup(trimmedId);
            if (sourcePath == null) {
                return "hull-source-path-missing";
            }
            String issue = invokeStringArgMethodWithTolerance(hullLoaderClass, sourcePath, 12000L);
            if (hullSpecClasses.isEmpty()) {
                return issue == null ? null : ("hull-loader-issue:" + issue);
            }
            Object after = lookupSpecByIdAcrossClasses(specStoreClass, hullSpecClasses, trimmedId);
            if (after == null) {
                return issue == null
                        ? "hull-still-missing-after-loader"
                        : ("hull-loader-issue:" + issue);
            }
            System.out.println(
                    "Fixer: warmup injected missing hull spec "
                            + trimmedId
                            + " from "
                            + sourcePath
                            + ".");
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static String ensureSingleWeaponSpecPresentForWarmup(String weaponId) {
        if (weaponId == null || weaponId.trim().isEmpty()) {
            return "invalid-weapon-id";
        }
        String trimmedId = weaponId.trim();
        try {
            Class<?> weaponLoaderClass = Class.forName("com.fs.starfarer.loading.WeaponSpecLoader");
            String sourcePath = resolveWeaponPathForWarmup(trimmedId);
            if (sourcePath == null) {
                return "weapon-source-path-missing";
            }
            String issue = invokeStringArgMethodWithTolerance(weaponLoaderClass, sourcePath, 12000L);
            if (issue != null) {
                return "weapon-loader-issue:" + issue;
            }
            System.out.println(
                    "Fixer: warmup injected missing weapon spec "
                            + trimmedId
                            + " from "
                            + sourcePath
                            + ".");
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static boolean shouldAttemptWarmupVariantAlias(String hullRepairIssue) {
        if (hullRepairIssue == null || hullRepairIssue.length() == 0) {
            return false;
        }
        String lowered = hullRepairIssue.toLowerCase();
        return lowered.indexOf("hull-spec-class-missing") >= 0
                || lowered.indexOf("hull-source-path-missing") >= 0
                || lowered.indexOf("hull-loader-issue") >= 0
                || lowered.indexOf("hull-still-missing-after-loader") >= 0
                || lowered.indexOf("register-verify-failed") >= 0
                || lowered.indexOf("register-methods-unavailable") >= 0
                || lowered.indexOf("register-method-unavailable") >= 0
                || lowered.indexOf("weapon-source-path-missing") >= 0
                || lowered.indexOf("weapon-loader-issue") >= 0
                || lowered.indexOf("weapon spec [") >= 0
                || lowered.indexOf("ship hull spec [") >= 0
                || lowered.indexOf("hull spec [") >= 0;
    }

    private static String resolveWeaponPathForWarmup(String weaponId) {
        String weaponFileName = weaponId + ".wpn";
        try {
            String direct = "data/weapons/" + weaponFileName;
            InputStream in = openResourceStream(direct);
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
                return direct;
            }
        } catch (Throwable ignored) {
        }
        try {
            LinkedHashSet<String> files = new LinkedHashSet<String>();
            collectSpecFilesFromIndex("data/weapons", new LinkedHashSet<String>(), files);
            for (String rel : files) {
                if (rel == null) {
                    continue;
                }
                String clean = rel.replace('\\', '/');
                if (clean.endsWith("/" + weaponFileName) || clean.endsWith(weaponFileName)) {
                    return clean;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String resolveHullPathForWarmup(String hullId) {
        String shipFileName = hullId + ".ship";
        try {
            String direct = "data/hulls/" + shipFileName;
            InputStream in = openResourceStream(direct);
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
                return direct;
            }
        } catch (Throwable ignored) {
        }
        try {
            LinkedHashSet<String> files = new LinkedHashSet<String>();
            collectSpecFilesFromIndex("data/hulls", new LinkedHashSet<String>(), files);
            for (String rel : files) {
                if (rel == null) {
                    continue;
                }
                String clean = rel.replace('\\', '/');
                if (clean.endsWith("/" + shipFileName) || clean.endsWith(shipFileName)) {
                    return clean;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String invokeStringArgMethodWithTolerance(
            Class<?> targetClass, String arg, long timeoutMs) {
        if (targetClass == null) {
            return "target-class-null";
        }
        Method method = null;
        for (Method m : targetClass.getDeclaredMethods()) {
            if (m == null || !Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getReturnType() != Void.TYPE) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1 && params[0] == String.class) {
                method = m;
                break;
            }
        }
        if (method == null) {
            return "string-loader-method-not-found";
        }

        final Method invokeMethod = method;
        final Throwable[] errorHolder = new Throwable[1];
        Thread worker =
                new Thread(
                        () -> {
                            try {
                                invokeMethod.setAccessible(true);
                                invokeMethod.invoke(null, arg);
                            } catch (Throwable t) {
                                errorHolder[0] = t;
                            }
                        },
                        "FixerStringWarmup-"
                                + targetClass.getSimpleName()
                                + "-"
                                + invokeMethod.getName());
        worker.setDaemon(true);
        worker.start();
        long waitMs = timeoutMs <= 0L ? 12000L : timeoutMs;
        try {
            worker.join(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
        if (worker.isAlive()) {
            try {
                worker.interrupt();
            } catch (Throwable ignored) {
            }
            return "timeout";
        }
        if (errorHolder[0] == null) {
            return null;
        }
        String chain = describeThrowableChain(errorHolder[0]);
        if (isToleratedWarmupIssue(chain)) {
            return null;
        }
        return chain;
    }

    private static String invokeNoArgMethodWithTolerance(
            Class<?> targetClass, String methodName, long timeoutMs) {
        Method method = findMethodRecursive(targetClass, methodName);
        if (method == null) {
            return "method-not-found";
        }
        return invokeNoArgMethodWithTolerance(targetClass, method, timeoutMs);
    }

    private static String invokeNoArgMethodWithTolerance(
            Class<?> targetClass, Method method, long timeoutMs) {
        if (method == null) {
            return "method-not-found";
        }
        final Throwable[] errorHolder = new Throwable[1];
        final Class<?> cls = targetClass == null ? method.getDeclaringClass() : targetClass;
        Thread worker =
                new Thread(
                        () -> {
                            try {
                                method.setAccessible(true);
                                method.invoke(null);
                            } catch (Throwable t) {
                                errorHolder[0] = t;
                            }
                        },
                        "FixerNoArgWarmup-"
                                + (cls == null ? "unknown" : cls.getSimpleName())
                                + "-"
                                + method.getName());
        worker.setDaemon(true);
        worker.start();
        long waitMs = timeoutMs <= 0L ? 12000L : timeoutMs;
        try {
            worker.join(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
        if (worker.isAlive()) {
            try {
                worker.interrupt();
            } catch (Throwable ignored) {
            }
            return "timeout";
        }
        if (errorHolder[0] == null) {
            return null;
        }
        String chain = describeThrowableChain(errorHolder[0]);
        if (isToleratedWarmupIssue(chain)) {
            return null;
        }
        return chain;
    }

    private static boolean isToleratedWarmupIssue(String chain) {
        if (chain == null || chain.length() == 0) {
            return false;
        }
        String lower = chain.toLowerCase();
        if (lower.indexOf("already exists") >= 0) {
            return true;
        }
        if ((lower.indexOf("ship hull variant [") >= 0 || lower.indexOf("ship hull spec [") >= 0)
                && lower.indexOf("not found") >= 0) {
            return true;
        }
        return false;
    }

    private static String tryStartDirectNewGame(DriverContext ctx, Object titleState) {
        return tryStartDirectNewGame(ctx, titleState, true);
    }

    private static String tryStartDirectNewGame(
            DriverContext ctx, Object titleState, boolean allowOrbitalJunkImmediateRetry) {
        try {
            System.out.println("Fixer: direct-new-game stage=begin");
            if (ctx == null || ctx.driver == null || ctx.driverClass == null) {
                return "driver context unavailable";
            }
            Map session = null;
            Method getSession = findMethodRecursive(ctx.driverClass, "getSession");
            if (getSession != null) {
                getSession.setAccessible(true);
                Object sessionObj = getSession.invoke(ctx.driver);
                if (sessionObj instanceof Map) {
                    session = (Map) sessionObj;
                }
            }

            Map titleSession = null;
            Object titleSessionObj = readFieldRecursive(titleState, "session");
            if (titleSessionObj instanceof Map) {
                titleSession = (Map) titleSessionObj;
            }
            if ((session == null || session.isEmpty())
                    && titleSession != null
                    && !titleSession.isEmpty()) {
                session = titleSession;
            }

            Object campaignState = session == null ? null : session.get(CAMPAIGN_SESSION_KEY);
            if (campaignState == null) {
                if (titleSession != null && titleSession != session) {
                    campaignState = titleSession.get(CAMPAIGN_SESSION_KEY);
                }
            }
            if (campaignState == null) {
                campaignState = resolveCampaignStateFromDriver(ctx);
            }
            if (campaignState == null) {
                int sessionSize = session == null ? -1 : session.size();
                int titleSessionSize = titleSession == null ? -1 : titleSession.size();
                return "campaign session state missing (sessionSize="
                        + sessionSize
                        + ", titleSessionSize="
                        + titleSessionSize
                        + ")";
            }
            if (session != null) {
                try {
                    session.put(CAMPAIGN_SESSION_KEY, campaignState);
                } catch (Throwable ignored) {
                }
            }
            if (titleSession != null && titleSession != session) {
                try {
                    titleSession.put(CAMPAIGN_SESSION_KEY, campaignState);
                } catch (Throwable ignored) {
                }
            }

            try {
                invokeStaticNoArg(Class.forName("com.fs.starfarer.campaign.CampaignEngine"), "resetInstance");
            } catch (Throwable ignored) {
            }

            Class<?> managerClass = Class.forName("com.fs.starfarer.campaign.save.CampaignGameManager");
            Method createMethod = findDirectNewGameMethod(managerClass);
            if (createMethod == null) {
                return "CampaignGameManager new-game method not found";
            }

            String baselineIssue = maybeWaitForCoreSpecBaselineBeforeDirectNewGame();
            if (baselineIssue != null) {
                maybeLogNewGamePreflightIssue(baselineIssue);
                return "direct new-game preflight pending";
            }

            boolean mutatingSpecPreflight = allowMutatingSpecPreflight();
            System.out.println("Fixer: direct-new-game stage=faction-preflight");
            if (!shouldSkipFactionPreflight()) {
                String factionPreflight =
                        ensureFactionSpecsReadyForDirectNewGame(mutatingSpecPreflight);
                if (factionPreflight != null) {
                    String warmupResult = maybeWarmupFactionSpecsForNewGame(factionPreflight);
                    if (warmupResult != null) {
                        maybeLogNewGamePreflightIssue(
                                factionPreflight + " [warmup:" + warmupResult + "]");
                        return "direct new-game preflight pending";
                    }
                }
            } else {
                System.out.println("Fixer: direct-new-game skipping faction preflight.");
                String factionRepairIssue = ensureFactionSpecsReadyForDirectNewGame(true);
                if (factionRepairIssue != null) {
                    System.out.println(
                            "Fixer: direct-new-game faction repair warning (skip mode): "
                                    + factionRepairIssue);
                    if (factionRepairIssue.indexOf("faction:") >= 0) {
                        maybeLogNewGamePreflightIssue(factionRepairIssue);
                        return "direct new-game preflight pending";
                    }
                } else {
                    System.out.println(
                            "Fixer: direct-new-game faction repair completed (skip mode).");
                }
            }
            System.out.println("Fixer: direct-new-game stage=galatia-derelict-variant-preflight");
            String galatiaDerelictVariantPreflight = ensureGalatiaDerelictVariantsForWarmup();
            if (galatiaDerelictVariantPreflight != null) {
                maybeLogNewGamePreflightIssue(
                        "galatia derelict variant preflight issue: "
                                + galatiaDerelictVariantPreflight);
                return "direct new-game preflight pending";
            }
            boolean runBroadSpecPreflight = mutatingSpecPreflight;
            boolean allowTemporarySpecInjection =
                    mutatingSpecPreflight || allowOrbitalJunkImmediateRetry;
            boolean clearTemporaryPlanetPreload = false;
            boolean clearTemporaryCustomEntityPreload = false;
            boolean clearTemporaryTerrainPreload = false;
            boolean clearTemporaryMarketConditionPreload = false;
            boolean clearTemporaryIndustryPreload = false;
            boolean clearTemporaryCommodityPreload = false;
            boolean clearTemporarySubmarketPreload = false;
            Class<?> planetSpecClassForCleanup = null;
            Class<?> terrainSpecClassForCleanup = null;
            Class<?> customEntitySpecClassForCleanup = null;
            Class<?> marketConditionSpecClassForCleanup = null;
            Class<?> industrySpecClassForCleanup = null;
            Class<?> commoditySpecClassForCleanup = null;
            Class<?> submarketSpecClassForCleanup = null;
            Class<?> specStoreClassForCleanup = null;
            if (allowTemporarySpecInjection) {
                System.out.println("Fixer: direct-new-game stage=planet-preflight");
                int planetCountBefore = -1;
                if (!mutatingSpecPreflight) {
                    try {
                        specStoreClassForCleanup = Class.forName("com.fs.starfarer.loading.SpecStore");
                        planetSpecClassForCleanup =
                                Class.forName("com.fs.starfarer.loading.specs.PlanetSpec");
                        planetCountBefore =
                                countSpecsForClass(specStoreClassForCleanup, planetSpecClassForCleanup);
                    } catch (Throwable ignored) {
                        planetSpecClassForCleanup = null;
                    }
                }
                String planetPreflight = ensurePlanetSpecsReadyForDirectNewGame();
                if (planetPreflight != null) {
                    boolean strictPlanetPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictPlanetSpecPreflight",
                                            "false"));
                    if (strictPlanetPreflight) {
                        return "planet spec preflight failed: " + planetPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game planet-spec preflight warning (continuing): "
                                    + planetPreflight);
                }
                if (!mutatingSpecPreflight
                        && specStoreClassForCleanup != null
                        && planetSpecClassForCleanup != null) {
                    int planetCountAfter =
                            countSpecsForClass(specStoreClassForCleanup, planetSpecClassForCleanup);
                    if (planetCountBefore == 0 && planetCountAfter > 0) {
                        clearTemporaryPlanetPreload = true;
                        System.out.println(
                                "Fixer: direct-new-game temporary planet preload armed for post-create cleanup (before="
                                        + planetCountBefore
                                        + ", after="
                                        + planetCountAfter
                                        + ").");
                    }
                }
            } else {
                System.out.println(
                        "Fixer: direct-new-game non-mutating retry mode; skipping temporary planet preflight injection.");
            }

            if (runBroadSpecPreflight) {
                System.out.println("Fixer: direct-new-game stage=custom-entity-preflight");
                String customEntityPreflight = ensureCustomEntitySpecsReadyForDirectNewGame();
                if (customEntityPreflight != null) {
                    boolean strictCustomEntityPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictCustomEntitySpecPreflight",
                                            "false"));
                    if (strictCustomEntityPreflight) {
                        return "custom entity spec preflight failed: " + customEntityPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game custom-entity preflight warning (continuing): "
                                    + customEntityPreflight);
                }
                System.out.println("Fixer: direct-new-game stage=terrain-preflight");
                String terrainPreflight = ensureTerrainSpecsReadyForDirectNewGame();
                if (terrainPreflight != null) {
                    boolean strictTerrainPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictTerrainSpecPreflight",
                                            "false"));
                    if (strictTerrainPreflight) {
                        return "terrain spec preflight failed: " + terrainPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game terrain-spec preflight warning (continuing): "
                                    + terrainPreflight);
                }

                System.out.println("Fixer: direct-new-game stage=market-condition-preflight");
                String marketConditionPreflight = ensureMarketConditionSpecsReadyForDirectNewGame();
                if (marketConditionPreflight != null) {
                    boolean strictMarketConditionPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictMarketConditionPreflight",
                                            "false"));
                    if (strictMarketConditionPreflight) {
                        return "market condition preflight failed: " + marketConditionPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game market-condition preflight warning (continuing): "
                                    + marketConditionPreflight);
                }
                System.out.println("Fixer: direct-new-game stage=industry-preflight");
                String industryPreflight = ensureIndustrySpecsReadyForDirectNewGame();
                if (industryPreflight != null) {
                    boolean strictIndustryPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictIndustrySpecPreflight",
                                            "false"));
                    if (strictIndustryPreflight) {
                        return "industry preflight failed: " + industryPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game industry preflight warning (continuing): "
                                    + industryPreflight);
                }
                System.out.println("Fixer: direct-new-game stage=commodity-preflight");
                String commodityPreflight = ensureCommoditySpecsReadyForDirectNewGame();
                if (commodityPreflight != null) {
                    boolean strictCommodityPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictCommoditySpecPreflight",
                                            "false"));
                    if (strictCommodityPreflight) {
                        return "commodity preflight failed: " + commodityPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game commodity preflight warning (continuing): "
                                    + commodityPreflight);
                }
                System.out.println("Fixer: direct-new-game stage=submarket-preflight");
                String submarketPreflight = ensureSubmarketSpecsReadyForDirectNewGame();
                if (submarketPreflight != null) {
                    boolean strictSubmarketPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictSubmarketSpecPreflight",
                                            "false"));
                    if (strictSubmarketPreflight) {
                        return "submarket preflight failed: " + submarketPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game submarket preflight warning (continuing): "
                                    + submarketPreflight);
                }
            } else if (allowTemporarySpecInjection) {
                System.out.println(
                        "Fixer: direct-new-game non-mutating mode; skipping non-planet broad spec registrations.");
                System.out.println("Fixer: direct-new-game stage=custom-entity-preflight");
                int customEntityCountBefore = -1;
                try {
                    specStoreClassForCleanup = Class.forName("com.fs.starfarer.loading.SpecStore");
                    customEntitySpecClassForCleanup = resolveCustomEntitySpecClass();
                    customEntityCountBefore =
                            countSpecsForClass(
                                    specStoreClassForCleanup, customEntitySpecClassForCleanup);
                } catch (Throwable ignored) {
                    if (specStoreClassForCleanup == null) {
                        specStoreClassForCleanup = null;
                    }
                    customEntitySpecClassForCleanup = null;
                }
                String customEntityPreflight = ensureCustomEntitySpecsReadyForDirectNewGame();
                if (customEntityPreflight != null) {
                    boolean strictCustomEntityPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictCustomEntitySpecPreflight",
                                            "false"));
                    if (strictCustomEntityPreflight) {
                        return "custom entity spec preflight failed: " + customEntityPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game custom-entity preflight warning (continuing): "
                                    + customEntityPreflight);
                }
                if (specStoreClassForCleanup != null && customEntitySpecClassForCleanup != null) {
                    int customEntityCountAfter =
                            countSpecsForClass(
                                    specStoreClassForCleanup, customEntitySpecClassForCleanup);
                    if (customEntityCountBefore == 0 && customEntityCountAfter > 0) {
                        clearTemporaryCustomEntityPreload = true;
                        System.out.println(
                                "Fixer: direct-new-game temporary custom-entity preload armed for post-create cleanup (before="
                                        + customEntityCountBefore
                                        + ", after="
                                        + customEntityCountAfter
                                        + ").");
                    }
                }
                System.out.println("Fixer: direct-new-game stage=terrain-preflight");
                int terrainCountBefore = -1;
                try {
                    if (specStoreClassForCleanup == null) {
                        specStoreClassForCleanup = Class.forName("com.fs.starfarer.loading.SpecStore");
                    }
                    terrainSpecClassForCleanup =
                            Class.forName("com.fs.starfarer.loading.specs.Stringsuper");
                    terrainCountBefore =
                            countSpecsForClass(specStoreClassForCleanup, terrainSpecClassForCleanup);
                } catch (Throwable ignored) {
                    terrainSpecClassForCleanup = null;
                }
                String terrainPreflight = ensureTerrainSpecsReadyForDirectNewGame();
                if (terrainPreflight != null) {
                    boolean strictTerrainPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictTerrainSpecPreflight",
                                            "false"));
                    if (strictTerrainPreflight) {
                        return "terrain spec preflight failed: " + terrainPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game terrain-spec preflight warning (continuing): "
                                    + terrainPreflight);
                }
                if (specStoreClassForCleanup != null && terrainSpecClassForCleanup != null) {
                    int terrainCountAfter =
                            countSpecsForClass(specStoreClassForCleanup, terrainSpecClassForCleanup);
                    if (terrainCountBefore == 0 && terrainCountAfter > 0) {
                        clearTemporaryTerrainPreload = true;
                        System.out.println(
                                "Fixer: direct-new-game temporary terrain preload armed for post-create cleanup (before="
                                        + terrainCountBefore
                                        + ", after="
                                        + terrainCountAfter
                                        + ").");
                    }
                }

                System.out.println("Fixer: direct-new-game stage=market-condition-preflight");
                int marketConditionCountBefore = -1;
                try {
                    if (specStoreClassForCleanup == null) {
                        specStoreClassForCleanup = Class.forName("com.fs.starfarer.loading.SpecStore");
                    }
                    marketConditionSpecClassForCleanup = Class.forName("com.fs.starfarer.loading.T");
                    marketConditionCountBefore =
                            countSpecsForClass(
                                    specStoreClassForCleanup, marketConditionSpecClassForCleanup);
                } catch (Throwable ignored) {
                    marketConditionSpecClassForCleanup = null;
                }
                String marketConditionPreflight = ensureMarketConditionSpecsReadyForDirectNewGame();
                if (marketConditionPreflight != null) {
                    boolean strictMarketConditionPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictMarketConditionPreflight",
                                            "false"));
                    if (strictMarketConditionPreflight) {
                        return "market condition preflight failed: " + marketConditionPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game market-condition preflight warning (continuing): "
                                    + marketConditionPreflight);
                }
                if (specStoreClassForCleanup != null && marketConditionSpecClassForCleanup != null) {
                    int marketConditionCountAfter =
                            countSpecsForClass(
                                    specStoreClassForCleanup, marketConditionSpecClassForCleanup);
                    if (marketConditionCountBefore == 0 && marketConditionCountAfter > 0) {
                        clearTemporaryMarketConditionPreload = true;
                        System.out.println(
                                "Fixer: direct-new-game temporary market-condition preload armed for post-create cleanup (before="
                                        + marketConditionCountBefore
                                        + ", after="
                                        + marketConditionCountAfter
                                        + ").");
                    }
                }

                System.out.println("Fixer: direct-new-game stage=industry-preflight");
                int industryCountBefore = -1;
                try {
                    if (specStoreClassForCleanup == null) {
                        specStoreClassForCleanup = Class.forName("com.fs.starfarer.loading.SpecStore");
                    }
                    industrySpecClassForCleanup = Class.forName("com.fs.starfarer.loading.specs.H");
                    industryCountBefore =
                            countSpecsForClass(specStoreClassForCleanup, industrySpecClassForCleanup);
                } catch (Throwable ignored) {
                    industrySpecClassForCleanup = null;
                }
                String industryPreflight = ensureIndustrySpecsReadyForDirectNewGame();
                if (industryPreflight != null) {
                    boolean strictIndustryPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictIndustrySpecPreflight",
                                            "false"));
                    if (strictIndustryPreflight) {
                        return "industry preflight failed: " + industryPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game industry preflight warning (continuing): "
                                    + industryPreflight);
                }
                if (specStoreClassForCleanup != null && industrySpecClassForCleanup != null) {
                    int industryCountAfter =
                            countSpecsForClass(specStoreClassForCleanup, industrySpecClassForCleanup);
                    if (industryCountBefore == 0 && industryCountAfter > 0) {
                        clearTemporaryIndustryPreload = true;
                        System.out.println(
                                "Fixer: direct-new-game temporary industry preload armed for post-create cleanup (before="
                                        + industryCountBefore
                                        + ", after="
                                        + industryCountAfter
                                        + ").");
                    }
                }

                System.out.println("Fixer: direct-new-game stage=commodity-preflight");
                int commodityCountBefore = -1;
                try {
                    if (specStoreClassForCleanup == null) {
                        specStoreClassForCleanup = Class.forName("com.fs.starfarer.loading.SpecStore");
                    }
                    commoditySpecClassForCleanup = Class.forName("com.fs.starfarer.loading.F");
                    commodityCountBefore =
                            countSpecsForClass(
                                    specStoreClassForCleanup, commoditySpecClassForCleanup);
                } catch (Throwable ignored) {
                    commoditySpecClassForCleanup = null;
                }
                String commodityPreflight = ensureCommoditySpecsReadyForDirectNewGame();
                if (commodityPreflight != null) {
                    boolean strictCommodityPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictCommoditySpecPreflight",
                                            "false"));
                    if (strictCommodityPreflight) {
                        return "commodity preflight failed: " + commodityPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game commodity preflight warning (continuing): "
                                    + commodityPreflight);
                }
                if (specStoreClassForCleanup != null && commoditySpecClassForCleanup != null) {
                    int commodityCountAfter =
                            countSpecsForClass(
                                    specStoreClassForCleanup, commoditySpecClassForCleanup);
                    if (commodityCountBefore == 0 && commodityCountAfter > 0) {
                        clearTemporaryCommodityPreload = true;
                        System.out.println(
                                "Fixer: direct-new-game temporary commodity preload armed for post-create cleanup (before="
                                        + commodityCountBefore
                                        + ", after="
                                        + commodityCountAfter
                                        + ").");
                    }
                }

                System.out.println("Fixer: direct-new-game stage=submarket-preflight");
                int submarketCountBefore = -1;
                try {
                    if (specStoreClassForCleanup == null) {
                        specStoreClassForCleanup = Class.forName("com.fs.starfarer.loading.SpecStore");
                    }
                    submarketSpecClassForCleanup = Class.forName("com.fs.starfarer.loading.for");
                    submarketCountBefore =
                            countSpecsForClass(
                                    specStoreClassForCleanup, submarketSpecClassForCleanup);
                } catch (Throwable ignored) {
                    submarketSpecClassForCleanup = null;
                }
                String submarketPreflight = ensureSubmarketSpecsReadyForDirectNewGame();
                if (submarketPreflight != null) {
                    boolean strictSubmarketPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictSubmarketSpecPreflight",
                                            "false"));
                    if (strictSubmarketPreflight) {
                        return "submarket preflight failed: " + submarketPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game submarket preflight warning (continuing): "
                                    + submarketPreflight);
                }
                if (specStoreClassForCleanup != null && submarketSpecClassForCleanup != null) {
                    int submarketCountAfter =
                            countSpecsForClass(
                                    specStoreClassForCleanup, submarketSpecClassForCleanup);
                    if (submarketCountBefore == 0 && submarketCountAfter > 0) {
                        clearTemporarySubmarketPreload = true;
                        System.out.println(
                                "Fixer: direct-new-game temporary submarket preload armed for post-create cleanup (before="
                                        + submarketCountBefore
                                        + ", after="
                                        + submarketCountAfter
                                        + ").");
                    }
                }
            } else {
                System.out.println(
                        "Fixer: direct-new-game non-mutating retry mode; skipping temporary broad spec injections.");
            }
            if (!mutatingSpecPreflight) {
                System.out.println(
                        "Fixer: direct-new-game mutating spec preflight disabled; running expanded non-mutating preflight set.");
            }
            System.out.println("Fixer: direct-new-game stage=starmap-preflight");
            String starMapPreflight = ensureStarSystemLocationMapReadyForDirectNewGame();
            if (starMapPreflight != null) {
                boolean strictStarMapPreflight =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignStrictStarMapPreflight", "false"));
                if (strictStarMapPreflight) {
                    return "starmap preflight failed: " + starMapPreflight;
                }
                System.out.println(
                        "Fixer: direct new-game starmap preflight warning (continuing): "
                                + starMapPreflight);
            }
            System.out.println("Fixer: direct-new-game stage=salvage-preflight");
            String salvagePreflight = ensureSalvageEntityGenSpecsReadyForDirectNewGame();
            if (salvagePreflight != null) {
                boolean strictSalvagePreflight =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignStrictSalvageSpecPreflight",
                                        "false"));
                if (strictSalvagePreflight) {
                    return "salvage spec preflight failed: " + salvagePreflight;
                }
                System.out.println(
                        "Fixer: direct new-game salvage-spec preflight warning (continuing): "
                                + salvagePreflight);
            }
            System.out.println("Fixer: direct-new-game stage=ui-preflight");
            String uiFontPreflight = ensureUiFontStateReadyForDirectNewGame();
            if (uiFontPreflight != null) {
                boolean strictUiFontPreflight =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignStrictUiFontPreflight", "false"));
                if (strictUiFontPreflight) {
                    return "ui font preflight failed: " + uiFontPreflight;
                }
                System.out.println(
                        "Fixer: direct new-game ui-font preflight warning (continuing): "
                                + uiFontPreflight);
            }
            String uiTexturePreflight = ensureUiPanelTextureObjectsReadyForDirectNewGame();
            if (uiTexturePreflight != null) {
                boolean strictUiTexturePreflight =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignStrictUiTexturePreflight",
                                        "false"));
                if (strictUiTexturePreflight) {
                    return "ui texture preflight failed: " + uiTexturePreflight;
                }
                System.out.println(
                        "Fixer: direct new-game ui-texture preflight warning (continuing): "
                                + uiTexturePreflight);
            }
            String uiSpriteIssue = ensureUiBorderSpriteMappingsReady();
            if (uiSpriteIssue != null) {
                System.out.println(
                        "Fixer: direct new-game ui-border sprite sanity issue: " + uiSpriteIssue);
            }
            boolean allowOrbitalJunkFallback =
                    allowTemporarySpecInjection
                            && (mutatingSpecPreflight
                                    || allowOrbitalJunkFallbackWhenNonMutating());
            if (allowOrbitalJunkFallback) {
                String orbitalJunkFallbackIssue = ensureOrbitalJunkCustomEntitySpecFallback();
                if (orbitalJunkFallbackIssue != null) {
                    System.out.println(
                            "Fixer: direct new-game orbital_junk fallback warning: "
                                    + orbitalJunkFallbackIssue);
                } else if (!mutatingSpecPreflight) {
                    System.out.println(
                            "Fixer: direct new-game orbital_junk fallback preloaded in non-mutating mode.");
                }
            } else if (!mutatingSpecPreflight) {
                System.out.println(
                        "Fixer: direct new-game non-mutating retry mode; skipping orbital_junk fallback injection.");
            }
            String orbitalJunkSettingsProbe = probeOrbitalJunkSettingsAvailability();
            if (orbitalJunkSettingsProbe == null) {
                System.out.println(
                        "Fixer: direct new-game orbital_junk settings probe: resolved via Global.getSettings.");
            } else {
                System.out.println(
                        "Fixer: direct new-game orbital_junk settings probe: "
                                + orbitalJunkSettingsProbe);
            }
            String orbitalJunkSpecStoreProbe = probeOrbitalJunkSpecStoreAvailability();
            if (orbitalJunkSpecStoreProbe != null
                    && orbitalJunkSpecStoreProbe.startsWith("resolved:")) {
                System.out.println(
                        "Fixer: direct new-game orbital_junk SpecStore probe: "
                                + orbitalJunkSpecStoreProbe);
            } else if (orbitalJunkSpecStoreProbe == null) {
                System.out.println(
                        "Fixer: direct new-game orbital_junk SpecStore probe: resolved.");
            } else {
                System.out.println(
                        "Fixer: direct new-game orbital_junk SpecStore probe: "
                                + orbitalJunkSpecStoreProbe);
            }
            String orbitalJunkEntityProbe = probeOrbitalJunkEntityInstantiation();
            if (orbitalJunkEntityProbe == null) {
                System.out.println(
                        "Fixer: direct new-game orbital_junk entity probe: resolved.");
            } else {
                System.out.println(
                        "Fixer: direct new-game orbital_junk entity probe: "
                                + orbitalJunkEntityProbe);
                String runtimeFactionIssue =
                        ensureRuntimeFactionsPresentForDirectNewGame(
                                new String[] {"neutral", "player"}, true);
                if (runtimeFactionIssue != null) {
                    System.out.println(
                            "Fixer: direct new-game orbital_junk runtime-faction repair warning: "
                                    + runtimeFactionIssue);
                }
                String fallbackIssue = ensureOrbitalJunkCustomEntitySpecFallback();
                if (fallbackIssue != null) {
                    System.out.println(
                            "Fixer: direct new-game orbital_junk fallback retry warning: "
                                    + fallbackIssue);
                }
                String retriedOrbitalJunkEntityProbe = probeOrbitalJunkEntityInstantiation();
                if (retriedOrbitalJunkEntityProbe == null) {
                    System.out.println(
                            "Fixer: direct new-game orbital_junk entity probe: resolved after targeted repair.");
                } else {
                    maybeLogNewGamePreflightIssue(
                            "orbital_junk entity probe unresolved: "
                                    + retriedOrbitalJunkEntityProbe);
                    return "direct new-game preflight pending";
                }
            }

            Object data = buildDefaultCharacterCreationData(createMethod.getParameterTypes()[0]);
            if (data == null) {
                return "character creation data build failed";
            }
            int additionalShips = getAdditionalShipsCount(data);
            if (additionalShips <= 0) {
                return "character creation data has no starting fleet members";
            }

            warmupTitleRenderTicks(titleState, 12);
            createMethod.setAccessible(true);
            System.out.println("Fixer: direct-new-game stage=invoke-create");
            final boolean[] orbitalSpecWatchdogStop = new boolean[] {false};
            Thread orbitalSpecWatchdogThread = null;
            if (allowOrbitalJunkFallback && allowOrbitalJunkSpecWatchdog()) {
                orbitalSpecWatchdogThread =
                        startOrbitalJunkSpecWatchdog(
                                orbitalSpecWatchdogStop, mutatingSpecPreflight);
            }
            try {
                Object result = createMethod.invoke(null, data, campaignState);
                System.out.println("Fixer: direct-new-game stage=invoke-create-return");
                if (result == null) {
                    String runtimeReadyIssue = waitForDirectNewGameRuntimeReadiness();
                    if (runtimeReadyIssue != null) {
                        if ("player-fleet-null".equals(runtimeReadyIssue)) {
                            System.out.println(
                                    "Fixer: direct new-game runtime readiness returned player-fleet-null; proceeding to transition flow.");
                            return null;
                        }
                        maybeLogNewGamePreflightIssue(
                                "post-init campaign runtime not ready: " + runtimeReadyIssue);
                        return "direct new-game preflight pending";
                    }
                    return null;
                }
                return String.valueOf(result);
            } finally {
                if (orbitalSpecWatchdogThread != null) {
                    orbitalSpecWatchdogStop[0] = true;
                    orbitalSpecWatchdogThread.interrupt();
                    try {
                        orbitalSpecWatchdogThread.join(600L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (clearTemporarySubmarketPreload
                        && specStoreClassForCleanup != null
                        && submarketSpecClassForCleanup != null) {
                    clearSpecsForClass(specStoreClassForCleanup, submarketSpecClassForCleanup);
                    int remainingSubmarket =
                            countSpecsForClass(specStoreClassForCleanup, submarketSpecClassForCleanup);
                    System.out.println(
                            "Fixer: direct-new-game temporary submarket preload cleanup complete (remaining="
                                    + remainingSubmarket
                                    + ").");
                }
                if (clearTemporaryCommodityPreload
                        && specStoreClassForCleanup != null
                        && commoditySpecClassForCleanup != null) {
                    clearSpecsForClass(specStoreClassForCleanup, commoditySpecClassForCleanup);
                    int remainingCommodity =
                            countSpecsForClass(specStoreClassForCleanup, commoditySpecClassForCleanup);
                    System.out.println(
                            "Fixer: direct-new-game temporary commodity preload cleanup complete (remaining="
                                    + remainingCommodity
                                    + ").");
                }
                if (clearTemporaryIndustryPreload
                        && specStoreClassForCleanup != null
                        && industrySpecClassForCleanup != null) {
                    clearSpecsForClass(specStoreClassForCleanup, industrySpecClassForCleanup);
                    int remainingIndustry =
                            countSpecsForClass(specStoreClassForCleanup, industrySpecClassForCleanup);
                    System.out.println(
                            "Fixer: direct-new-game temporary industry preload cleanup complete (remaining="
                                    + remainingIndustry
                                    + ").");
                }
                if (clearTemporaryMarketConditionPreload
                        && specStoreClassForCleanup != null
                        && marketConditionSpecClassForCleanup != null) {
                    clearSpecsForClass(specStoreClassForCleanup, marketConditionSpecClassForCleanup);
                    int remainingMarketCondition =
                            countSpecsForClass(
                                    specStoreClassForCleanup, marketConditionSpecClassForCleanup);
                    System.out.println(
                            "Fixer: direct-new-game temporary market-condition preload cleanup complete (remaining="
                                    + remainingMarketCondition
                                    + ").");
                }
                if (clearTemporaryCustomEntityPreload
                        && specStoreClassForCleanup != null
                        && customEntitySpecClassForCleanup != null) {
                    clearSpecsForClass(specStoreClassForCleanup, customEntitySpecClassForCleanup);
                    int remainingCustom =
                            countSpecsForClass(
                                    specStoreClassForCleanup, customEntitySpecClassForCleanup);
                    System.out.println(
                            "Fixer: direct-new-game temporary custom-entity preload cleanup complete (remaining="
                                    + remainingCustom
                                    + ").");
                }
                if (clearTemporaryTerrainPreload
                        && specStoreClassForCleanup != null
                        && terrainSpecClassForCleanup != null) {
                    clearSpecsForClass(specStoreClassForCleanup, terrainSpecClassForCleanup);
                    int remaining =
                            countSpecsForClass(specStoreClassForCleanup, terrainSpecClassForCleanup);
                    System.out.println(
                            "Fixer: direct-new-game temporary terrain preload cleanup complete (remaining="
                                    + remaining
                                    + ").");
                }
                if (clearTemporaryPlanetPreload
                        && specStoreClassForCleanup != null
                        && planetSpecClassForCleanup != null) {
                    clearSpecsForClass(specStoreClassForCleanup, planetSpecClassForCleanup);
                    int remainingPlanet =
                            countSpecsForClass(specStoreClassForCleanup, planetSpecClassForCleanup);
                    System.out.println(
                            "Fixer: direct-new-game temporary planet preload cleanup complete (remaining="
                                    + remainingPlanet
                                    + ").");
                }
            }
        } catch (Throwable t) {
            if (isOrbitalJunkSpecResolutionNpe(t)) {
                System.out.println(
                        "Fixer: orbital_junk resolution exception detail: " + stackTraceToString(t));
                boolean allowOrbitalJunkRepair = allowMutatingSpecPreflight();
                if (allowOrbitalJunkRepair) {
                    String repairIssue = ensureOrbitalJunkCustomEntitySpecFallback();
                    if (repairIssue == null && allowOrbitalJunkImmediateRetry) {
                        System.out.println(
                                "Fixer: orbital_junk custom-entity spec repaired; retrying direct new-game immediately.");
                        return tryStartDirectNewGame(ctx, titleState, false);
                    }
                    maybeLogNewGamePreflightIssue(
                            repairIssue == null
                                    ? "orbital_junk custom-entity spec repaired; retrying direct new-game."
                                    : "orbital_junk custom-entity spec repair issue: " + repairIssue);
                    return "direct new-game preflight pending";
                }
                if (allowOrbitalJunkImmediateRetry) {
                    System.out.println(
                            "Fixer: orbital_junk resolution NPE observed in non-mutating mode; retrying direct new-game without spec injection.");
                    return tryStartDirectNewGame(ctx, titleState, false);
                }
                maybeLogNewGamePreflightIssue(
                        "orbital_junk resolution NPE observed in non-mutating mode; waiting for loader readiness.");
                return "direct new-game preflight pending";
            }
            if (isGalatiaDerelictVariantNpe(t)) {
                String galatiaIssue = ensureGalatiaDerelictVariantsForWarmup();
                if (galatiaIssue == null && allowOrbitalJunkImmediateRetry) {
                    System.out.println(
                            "Fixer: direct new-game Galatia derelict variant repair succeeded; retrying immediately.");
                    return tryStartDirectNewGame(ctx, titleState, false);
                }
                maybeLogNewGamePreflightIssue(
                        galatiaIssue == null
                                ? "galatia derelict variant NPE observed; retry pending."
                                : "galatia derelict variant preflight issue: " + galatiaIssue);
                return "direct new-game preflight pending";
            }
            String chain = describeThrowableChain(t);
            String invalidVariantId = extractInvalidShipVariantIdFromChain(chain);
            String missingVariantId = extractMissingHullVariantIdFromChain(chain);
            String variantToRepair = null;
            if (invalidVariantId != null) {
                variantToRepair = invalidVariantId;
            } else if (missingVariantId != null) {
                variantToRepair = missingVariantId;
            }
            if (variantToRepair != null) {
                String repairIssue = ensureSingleVariantPresentForFactionWarmup(variantToRepair);
                if (repairIssue != null) {
                    String aliasIssue = ensureVariantAliasForWarmup(variantToRepair, "lasher_Standard");
                    if (aliasIssue == null) {
                        repairIssue = null;
                        System.out.println(
                                "Fixer: direct new-game variant repair aliased "
                                        + variantToRepair
                                        + " to lasher_Standard.");
                    } else {
                        repairIssue = repairIssue + ";alias:" + aliasIssue;
                    }
                }
                if (repairIssue == null && allowOrbitalJunkImmediateRetry) {
                    System.out.println(
                            "Fixer: direct new-game repaired missing/invalid variant "
                                    + variantToRepair
                                    + "; retrying immediately.");
                    return tryStartDirectNewGame(ctx, titleState, false);
                }
                if (repairIssue != null) {
                    maybeLogNewGamePreflightIssue(
                            "direct new-game variant repair issue for "
                                    + variantToRepair
                                    + ": "
                                    + repairIssue);
                }
                return "direct new-game preflight pending";
            }
            System.out.println(
                    "Fixer: direct new-game exception detail: " + stackTraceToString(t));
            return annotateDirectNewGameFailure(t, chain);
        }
    }

    private static String ensureUiBorderSpriteMappingsReady() {
        uiBorderSpriteRepairAttempts++;
        try {
            Class<?> settingsClass = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
            Object rootObj = resolveSettingsSpriteRootJson(settingsClass);
            if (rootObj == null) {
                return "settings sprite root json unavailable";
            }
            Class<?> jsonClass = rootObj.getClass();
            Method jsonHas = findMethodRecursive(jsonClass, "has", String.class);
            Method jsonGetObject = findMethodRecursive(jsonClass, "getJSONObject", String.class);
            Method jsonGetString = findMethodRecursive(jsonClass, "getString", String.class);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonHas == null || jsonGetObject == null || jsonPut == null) {
                return "json reflection methods unavailable";
            }
            jsonHas.setAccessible(true);
            jsonGetObject.setAccessible(true);
            if (jsonGetString != null) {
                jsonGetString.setAccessible(true);
            }
            jsonPut.setAccessible(true);

            Object uiObj;
            boolean hasUi =
                    ((Boolean) jsonHas.invoke(rootObj, "ui")).booleanValue();
            if (!hasUi) {
                Object newUi = jsonClass.getDeclaredConstructor().newInstance();
                jsonPut.invoke(rootObj, "ui", newUi);
                uiObj = newUi;
            } else {
                uiObj = jsonGetObject.invoke(rootObj, "ui");
            }
            if (uiObj == null) {
                return "ui sprite category missing";
            }

            final String[][] required =
                    new String[][] {
                        {"ui_border1_bot_left", "graphics/ui/bgs/ui_border1_sw.png"},
                        {"ui_border1_bot_right", "graphics/ui/bgs/ui_border1_se.png"},
                        {"ui_border1_bot", "graphics/ui/bgs/ui_border1_s.png"},
                        {"ui_border1_left", "graphics/ui/bgs/ui_border1_w.png"},
                        {"ui_border1_right", "graphics/ui/bgs/ui_border1_e.png"},
                        {"ui_border1_top_left", "graphics/ui/bgs/ui_border1_nw.png"},
                        {"ui_border1_top_right", "graphics/ui/bgs/ui_border1_ne.png"},
                        {"ui_border1_top", "graphics/ui/bgs/ui_border1_n.png"}
                    };
            final String[][] fallback =
                    new String[][] {
                        {"ui_border1_bot_left", "graphics/ui/bgs/panel01_bot_left.png"},
                        {"ui_border1_bot_right", "graphics/ui/bgs/panel01_bot_right.png"},
                        {"ui_border1_bot", "graphics/ui/bgs/panel01_bot.png"},
                        {"ui_border1_left", "graphics/ui/bgs/panel01_left.png"},
                        {"ui_border1_right", "graphics/ui/bgs/panel01_right.png"},
                        {"ui_border1_top_left", "graphics/ui/bgs/panel01_top_left.png"},
                        {"ui_border1_top_right", "graphics/ui/bgs/panel01_top_right.png"},
                        {"ui_border1_top", "graphics/ui/bgs/panel01_top.png"}
                    };
            int injected = 0;
            for (int i = 0; i < required.length; i++) {
                String key = required[i][0];
                String value = required[i][1];
                boolean hasKey =
                        ((Boolean) jsonHas.invoke(uiObj, key)).booleanValue();
                if (!hasKey) {
                    jsonPut.invoke(uiObj, key, value);
                    injected++;
                }
            }

            Method spriteLoadMethod = findSpriteLoadMethod(settingsClass);
            if (spriteLoadMethod == null) {
                return "sprite load method not found";
            }
            spriteLoadMethod.setAccessible(true);
            Class<?> textureRegistryClass = findTextureRegistryClass();
            Method textureLoadMethod = findTextureCacheLoadMethod(textureRegistryClass);
            Method textureLookupMethod = findTextureCacheLookupMethod(textureRegistryClass);
            if (textureLoadMethod != null) {
                textureLoadMethod.setAccessible(true);
            }
            if (textureLookupMethod != null) {
                textureLookupMethod.setAccessible(true);
            }
            List<String> unresolved = new ArrayList<String>();
            for (int i = 0; i < required.length; i++) {
                String key = required[i][0];
                String spritePath = resolveUiSpritePath(uiObj, jsonGetString, key, required[i][1]);
                String reason =
                        validateUiBorderSprite(
                                spriteLoadMethod,
                                key,
                                spritePath,
                                textureLoadMethod,
                                textureLookupMethod);
                if (reason != null) {
                    unresolved.add(key + "(" + reason + ")");
                }
            }
            int remapped = 0;
            if (!unresolved.isEmpty()) {
                for (int i = 0; i < fallback.length; i++) {
                    String key = fallback[i][0];
                    String value = fallback[i][1];
                    jsonPut.invoke(uiObj, key, value);
                    remapped++;
                }
                unresolved.clear();
                for (int i = 0; i < fallback.length; i++) {
                    String key = fallback[i][0];
                    String spritePath = resolveUiSpritePath(uiObj, jsonGetString, key, fallback[i][1]);
                    String reason =
                            validateUiBorderSprite(
                                    spriteLoadMethod,
                                    key,
                                    spritePath,
                                    textureLoadMethod,
                                    textureLookupMethod);
                    if (reason != null) {
                        unresolved.add(key + "(fallback-" + reason + ")");
                    }
                }
            }

            if (!uiBorderSpriteSanityLogged || injected > 0 || !unresolved.isEmpty()) {
                System.out.println(
                        "Fixer: ui-border sprite sanity attempt="
                                + uiBorderSpriteRepairAttempts
                                + " injected="
                                + injected
                                + " remapped="
                                + remapped
                                + " unresolved="
                                + unresolved);
                uiBorderSpriteSanityLogged = true;
            }
            if (!unresolved.isEmpty()) {
                return "unresolved ui sprites: " + String.join(", ", unresolved);
            }
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static String ensureUiPanelTextureObjectsReadyForDirectNewGame() {
        try {
            Class<?> settingsClass = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
            Object rootObj = resolveSettingsSpriteRootJson(settingsClass);
            if (rootObj == null) {
                return "settings sprite root json unavailable";
            }
            Class<?> jsonClass = rootObj.getClass();
            Method jsonHas = findMethodRecursive(jsonClass, "has", String.class);
            Method jsonGetObject = findMethodRecursive(jsonClass, "getJSONObject", String.class);
            Method jsonGetString = findMethodRecursive(jsonClass, "getString", String.class);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonHas == null || jsonGetObject == null || jsonPut == null) {
                return "json reflection methods unavailable";
            }
            jsonHas.setAccessible(true);
            jsonGetObject.setAccessible(true);
            if (jsonGetString != null) {
                jsonGetString.setAccessible(true);
            }
            jsonPut.setAccessible(true);

            Object uiObj;
            boolean hasUi = ((Boolean) jsonHas.invoke(rootObj, "ui")).booleanValue();
            if (!hasUi) {
                Object newUi = jsonClass.getDeclaredConstructor().newInstance();
                jsonPut.invoke(rootObj, "ui", newUi);
                uiObj = newUi;
            } else {
                uiObj = jsonGetObject.invoke(rootObj, "ui");
            }
            if (uiObj == null) {
                return "ui sprite category missing";
            }

            final String[][] required =
                    new String[][] {
                        {"scanline11", "graphics/fx/scanline11.png"},
                        {"noise", "graphics/fx/noise.png"}
                    };

            for (int i = 0; i < required.length; i++) {
                String key = required[i][0];
                String value = required[i][1];
                boolean hasKey = ((Boolean) jsonHas.invoke(uiObj, key)).booleanValue();
                if (!hasKey) {
                    jsonPut.invoke(uiObj, key, value);
                }
            }

            Class<?> textureRegistryClass = findTextureRegistryClass();
            Method textureLoadMethod = findTextureCacheLoadMethod(textureRegistryClass);
            Method textureLookupMethod = findTextureCacheLookupMethod(textureRegistryClass);
            if (textureLoadMethod != null) {
                textureLoadMethod.setAccessible(true);
            }
            if (textureLookupMethod != null) {
                textureLookupMethod.setAccessible(true);
            }

            Method textureObjectMethod = findSettingsTextureObjectMethod(settingsClass);
            if (textureObjectMethod == null) {
                return "settings texture lookup method not found";
            }
            textureObjectMethod.setAccessible(true);

            List<String> unresolved = new ArrayList<String>();
            for (int i = 0; i < required.length; i++) {
                String key = required[i][0];
                String spritePath = resolveUiSpritePath(uiObj, jsonGetString, key, required[i][1]);
                primeTextureCacheForSpritePath(textureLoadMethod, textureLookupMethod, spritePath);
                Object texObj = textureObjectMethod.invoke(null, "ui", key);
                if (texObj == null) {
                    primeTextureCacheForSpritePath(textureLoadMethod, textureLookupMethod, spritePath);
                    texObj = textureObjectMethod.invoke(null, "ui", key);
                }
                if (texObj == null) {
                    unresolved.add(key + "(null)");
                }
            }
            if (!unresolved.isEmpty()) {
                return "ui texture objects unresolved: " + String.join(", ", unresolved);
            }
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static Method findSpriteLoadMethod(Class<?> settingsClass) {
        if (settingsClass == null) {
            return null;
        }
        Method[] methods = settingsClass.getDeclaredMethods();
        for (Method m : methods) {
            if (m == null) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 3) {
                continue;
            }
            if (params[0] != String.class || params[1] != String.class || params[2] != Boolean.TYPE) {
                continue;
            }
            Class<?> ret = m.getReturnType();
            if (ret != null && "com.fs.graphics.Sprite".equals(ret.getName())) {
                return m;
            }
        }
        return null;
    }

    private static Method findSettingsTextureObjectMethod(Class<?> settingsClass) {
        if (settingsClass == null) {
            return null;
        }
        Method[] methods = settingsClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m == null) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 2 || params[0] != String.class || params[1] != String.class) {
                continue;
            }
            Class<?> ret = m.getReturnType();
            if (ret != null && "com.fs.graphics.Object".equals(ret.getName())) {
                return m;
            }
        }
        return null;
    }

    private static Class<?> findTextureRegistryClass() {
        String[] candidates = new String[] {"com.fs.graphics.oOoO", "com.fs.graphics.oooo_1"};
        for (int i = 0; i < candidates.length; i++) {
            try {
                return Class.forName(candidates[i]);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Method findTextureCacheLoadMethod(Class<?> textureRegistryClass) {
        if (textureRegistryClass == null) {
            return null;
        }
        Method[] methods = textureRegistryClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m == null || !Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 2
                    && params[0] == String.class
                    && params[1] == String.class
                    && m.getReturnType() == Void.TYPE) {
                return m;
            }
        }
        return null;
    }

    private static Method findTextureCacheLookupMethod(Class<?> textureRegistryClass) {
        if (textureRegistryClass == null) {
            return null;
        }
        Method[] methods = textureRegistryClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m == null || !Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1 || params[0] != String.class) {
                continue;
            }
            Class<?> ret = m.getReturnType();
            if (ret != null && "com.fs.graphics.Object".equals(ret.getName())) {
                return m;
            }
        }
        return null;
    }

    private static String resolveUiSpritePath(
            Object uiObj, Method jsonGetString, String key, String fallbackPath) {
        if (fallbackPath != null && fallbackPath.trim().length() > 0) {
            fallbackPath = fallbackPath.trim();
        }
        if (uiObj == null || jsonGetString == null || key == null || key.length() == 0) {
            return fallbackPath;
        }
        try {
            Object value = jsonGetString.invoke(uiObj, key);
            if (value instanceof String) {
                String path = ((String) value).trim();
                if (path.length() > 0) {
                    return path;
                }
            }
        } catch (Throwable ignored) {
        }
        return fallbackPath;
    }

    private static String validateUiBorderSprite(
            Method spriteLoadMethod,
            String key,
            String spritePath,
            Method textureLoadMethod,
            Method textureLookupMethod) {
        if (spriteLoadMethod == null || key == null || key.length() == 0) {
            return "load-method-missing";
        }
        try {
            primeTextureCacheForSpritePath(textureLoadMethod, textureLookupMethod, spritePath);
            Object sprite = spriteLoadMethod.invoke(null, "ui", key, Boolean.TRUE);
            if (sprite == null) {
                return "null";
            }
            if (isSpriteTextureMissing(sprite)) {
                primeTextureCacheForSpritePath(textureLoadMethod, textureLookupMethod, spritePath);
                sprite = spriteLoadMethod.invoke(null, "ui", key, Boolean.TRUE);
                if (sprite == null) {
                    return "null";
                }
                if (isSpriteTextureMissing(sprite)) {
                    return "texture-null";
                }
            }
            Method spriteWidthMethod = findMethodRecursive(sprite.getClass(), "getImageWidth");
            if (spriteWidthMethod == null) {
                return null;
            }
            spriteWidthMethod.setAccessible(true);
            Object widthObj = spriteWidthMethod.invoke(sprite);
            if (widthObj instanceof Number) {
                return ((Number) widthObj).intValue() > 0 ? null : "zero-width";
            }
            return null;
        } catch (Throwable firstFailure) {
            try {
                primeTextureCacheForSpritePath(textureLoadMethod, textureLookupMethod, spritePath);
                Object retrySprite = spriteLoadMethod.invoke(null, "ui", key, Boolean.TRUE);
                if (retrySprite == null) {
                    return "image-width-npe";
                }
                if (isSpriteTextureMissing(retrySprite)) {
                    return "texture-null";
                }
                Method retryWidthMethod = findMethodRecursive(retrySprite.getClass(), "getImageWidth");
                if (retryWidthMethod == null) {
                    return null;
                }
                retryWidthMethod.setAccessible(true);
                Object retryWidthObj = retryWidthMethod.invoke(retrySprite);
                if (retryWidthObj instanceof Number && ((Number) retryWidthObj).intValue() <= 0) {
                    return "zero-width";
                }
                return null;
            } catch (Throwable ignored) {
                return "image-width-npe";
            }
        }
    }

    private static boolean primeTextureCacheForSpritePath(
            Method textureLoadMethod, Method textureLookupMethod, String spritePath) {
        if ((textureLoadMethod == null && textureLookupMethod == null)
                || spritePath == null
                || spritePath.trim().length() == 0) {
            return false;
        }
        String trimmed = spritePath.trim();
        String noLeadingSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        LinkedHashSet<String> lookupKeys = new LinkedHashSet<String>();
        lookupKeys.add(trimmed);
        lookupKeys.add(noLeadingSlash);
        if (!trimmed.startsWith("/")) {
            lookupKeys.add("/" + trimmed);
        }

        LinkedHashSet<String> loadPaths = new LinkedHashSet<String>();
        loadPaths.add(trimmed);
        loadPaths.add(noLeadingSlash);
        if (!trimmed.startsWith("/")) {
            loadPaths.add("/" + trimmed);
        }
        if (noLeadingSlash.length() > 0) {
            loadPaths.add(APP_ROOT + "/" + noLeadingSlash);
        }

        boolean attemptedLoad = false;
        Iterator<String> keyIt = lookupKeys.iterator();
        while (keyIt.hasNext()) {
            String key = keyIt.next();
            if (key == null || key.length() == 0) {
                continue;
            }
            if (textureLookupMethod != null) {
                try {
                    Object existing = textureLookupMethod.invoke(null, key);
                    if (existing != null) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (textureLoadMethod == null) {
                continue;
            }
            Iterator<String> pathIt = loadPaths.iterator();
            while (pathIt.hasNext()) {
                String path = pathIt.next();
                if (path == null || path.length() == 0) {
                    continue;
                }
                try {
                    textureLoadMethod.invoke(null, key, path);
                    attemptedLoad = true;
                } catch (Throwable ignored) {
                }
                if (textureLookupMethod != null) {
                    try {
                        Object loaded = textureLookupMethod.invoke(null, key);
                        if (loaded != null) {
                            return true;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return attemptedLoad;
    }

    private static boolean isSpriteTextureMissing(Object sprite) {
        if (sprite == null) {
            return true;
        }
        try {
            Field textureField = findFieldRecursive(sprite.getClass(), "texture");
            if (textureField == null) {
                return false;
            }
            textureField.setAccessible(true);
            return textureField.get(sprite) == null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object resolveSettingsSpriteRootJson(Class<?> settingsClass) {
        if (settingsClass == null) {
            return null;
        }
        List<Field> jsonFields = new ArrayList<Field>();
        try {
            for (Field f : settingsClass.getDeclaredFields()) {
                if (f == null || !Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                Class<?> t = f.getType();
                if (t != null && "org.json.JSONObject".equals(t.getName())) {
                    f.setAccessible(true);
                    jsonFields.add(f);
                }
            }
        } catch (Throwable ignored) {
        }
        if (jsonFields.isEmpty()) {
            return null;
        }
        for (Field f : jsonFields) {
            try {
                Object candidate = f.get(null);
                if (candidate == null) {
                    continue;
                }
                Class<?> jsonClass = candidate.getClass();
                Method has = findMethodRecursive(jsonClass, "has", String.class);
                Method getJSONObject = findMethodRecursive(jsonClass, "getJSONObject", String.class);
                if (has == null || getJSONObject == null) {
                    continue;
                }
                has.setAccessible(true);
                boolean hasUi = ((Boolean) has.invoke(candidate, "ui")).booleanValue();
                if (hasUi) {
                    return candidate;
                }
            } catch (Throwable ignored) {
            }
        }
        for (Field f : jsonFields) {
            try {
                Object candidate = f.get(null);
                if (candidate != null) {
                    return candidate;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            Object created = jsonClass.getDeclaredConstructor().newInstance();
            jsonFields.get(0).set(null, created);
            return created;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String tryStartDirectNewGameWithTimeout(
            final DriverContext ctx, final Object titleState, long timeoutMs) {
        final long waitMs = Math.max(2000L, timeoutMs);
        Thread worker;
        synchronized (DIRECT_NEW_GAME_WORKER_LOCK) {
            if (directNewGameWorker != null) {
                if (directNewGameWorkerCompleted || !directNewGameWorker.isAlive()) {
                    return consumeDirectNewGameWorkerOutcomeLocked();
                }
                worker = directNewGameWorker;
            } else {
                final int workerId = ++directNewGameWorkerId;
                directNewGameWorkerCompleted = false;
                directNewGameWorkerResult = null;
                directNewGameWorkerError = null;
                directNewGameWorkerStartedAt = System.currentTimeMillis();
                worker =
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        String localResult = null;
                                        Throwable localError = null;
                                        try {
                                            localResult = tryStartDirectNewGame(ctx, titleState);
                                        } catch (Throwable t) {
                                            localError = t;
                                        } finally {
                                            synchronized (DIRECT_NEW_GAME_WORKER_LOCK) {
                                                if (directNewGameWorker == Thread.currentThread()) {
                                                    directNewGameWorkerResult = localResult;
                                                    directNewGameWorkerError = localError;
                                                    directNewGameWorkerCompleted = true;
                                                }
                                            }
                                        }
                                    }
                                },
                                "fixer-direct-newgame-attempt-" + workerId);
                worker.setDaemon(true);
                directNewGameWorker = worker;
                try {
                    worker.start();
                } catch (Throwable t) {
                    directNewGameWorker = null;
                    directNewGameWorkerStartedAt = 0L;
                    directNewGameWorkerCompleted = false;
                    directNewGameWorkerResult = null;
                    directNewGameWorkerError = null;
                    return "direct new-game wrapper failure: " + describeThrowableChain(t);
                }
            }
        }

        try {
            worker.join(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "direct new-game interrupted";
        } catch (Throwable t) {
            return "direct new-game wrapper failure: " + describeThrowableChain(t);
        }

        synchronized (DIRECT_NEW_GAME_WORKER_LOCK) {
            if (directNewGameWorker == worker
                    && (directNewGameWorkerCompleted || !worker.isAlive())) {
                return consumeDirectNewGameWorkerOutcomeLocked();
            }
            if (directNewGameWorker == worker && worker.isAlive()) {
                try {
                    worker.interrupt();
                } catch (Throwable ignored) {
                }
            }
            long now = System.currentTimeMillis();
            long ageMs =
                    directNewGameWorkerStartedAt > 0L
                            ? Math.max(0L, now - directNewGameWorkerStartedAt)
                            : waitMs;
            return "direct new-game preflight pending: active attempt still running (age="
                    + ageMs
                    + "ms)";
        }
    }

    private static String consumeDirectNewGameWorkerOutcomeLocked() {
        String result = directNewGameWorkerResult;
        Throwable error = directNewGameWorkerError;
        directNewGameWorker = null;
        directNewGameWorkerStartedAt = 0L;
        directNewGameWorkerCompleted = false;
        directNewGameWorkerResult = null;
        directNewGameWorkerError = null;
        if (error != null) {
            return "direct new-game wrapper error: " + describeThrowableChain(error);
        }
        return result;
    }

    private static String annotateDirectNewGameFailure(Throwable t, String baseMessage) {
        String message = baseMessage == null ? "" : baseMessage;
        if (isCampaignGameManagerNpe(t) && message.indexOf("[campaign-newgame-npe]") < 0) {
            if (message.length() == 0) {
                message = "[campaign-newgame-npe]";
            } else {
                message = message + " [campaign-newgame-npe]";
            }
        }
        if (isCampaignUiInitNpe(t) && message.indexOf("[campaign-ui-init-npe]") < 0) {
            if (message.length() == 0) {
                return "[campaign-ui-init-npe]";
            }
            return message + " [campaign-ui-init-npe]";
        }
        if (isSpriteGetImageWidthNpe(t) && message.indexOf("[sprite-getimagewidth-npe]") < 0) {
            if (message.length() == 0) {
                return "[sprite-getimagewidth-npe]";
            }
            return message + " [sprite-getimagewidth-npe]";
        }
        if (isOrbitalJunkSpecResolutionNpe(t)
                && message.indexOf("[orbital-junk-spec-npe]") < 0) {
            if (message.length() == 0) {
                return "[orbital-junk-spec-npe]";
            }
            return message + " [orbital-junk-spec-npe]";
        }
        return message;
    }

    private static boolean isCampaignGameManagerNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("campaigngamemanager") >= 0;
    }

    private static boolean isCampaignUiInitNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("campaigngamemanager") >= 0
                && (lower.indexOf("com.fs.starfarer.ui.d.settext") >= 0
                        || lower.indexOf("com.fs.starfarer.ui.d.<init>") >= 0);
    }

    private static boolean isSpriteGetImageWidthNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("sprite.getimagewidth") >= 0;
    }

    private static boolean isOrbitalJunkSpecResolutionNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("customcampaignentity.readresolve") >= 0
                && lower.indexOf("addorbitaljunk") >= 0;
    }

    private static boolean isGalatiaDerelictVariantNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("derelictshipentityplugin.readresolve") >= 0
                && lower.indexOf("data.scripts.world.systems.galatia.addderelict") >= 0;
    }

    private static String ensureFactionSpecsReadyForDirectNewGame() {
        return ensureFactionSpecsReadyForDirectNewGame(allowMutatingSpecPreflight());
    }

    private static String ensureFactionSpecsReadyForDirectNewGame(boolean allowRepair) {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            List<Class<?>> factionSpecClasses = new ArrayList<Class<?>>();
            try {
                factionSpecClasses.add(Class.forName("com.fs.starfarer.loading.if"));
            } catch (Throwable ignored) {
            }
            try {
                Class<?> fallbackClass = Class.forName("com.fs.starfarer.loading.specs.FactionSpec");
                if (!factionSpecClasses.contains(fallbackClass)) {
                    factionSpecClasses.add(fallbackClass);
                }
            } catch (Throwable ignored) {
            }
            if (factionSpecClasses.isEmpty()) {
                return "faction spec class unavailable";
            }
            List<String> missing = new ArrayList<String>();
            final String[] requiredFactionIds =
                    new String[] {
                        "neutral",
                        "player",
                        "pirates",
                        "hegemony",
                        "independent",
                        "tritachyon",
                        "sindrian_diktat",
                        "luddic_church",
                        "luddic_path",
                        "persean_league",
                        "derelict",
                        "remnants",
                        "omega",
                        "threat",
                        "dweller"
                    };
            Object playerSpec = null;
            Class<?> playerSpecClass = null;
            for (String factionId : requiredFactionIds) {
                Object spec = null;
                Class<?> resolvedSpecClass = null;
                for (Class<?> candidateClass : factionSpecClasses) {
                    if (candidateClass == null) {
                        continue;
                    }
                    Object candidateSpec = lookupSpecById(specStoreClass, candidateClass, factionId);
                    if (candidateSpec != null) {
                        spec = candidateSpec;
                        resolvedSpecClass = candidateClass;
                        break;
                    }
                }
                if (spec == null) {
                    missing.add("faction:" + factionId);
                    continue;
                }
                if ("player".equals(factionId)) {
                    playerSpec = spec;
                    playerSpecClass = resolvedSpecClass;
                }
            }
            String portraitsIssue =
                    ensurePlayerFactionPortraitsReadyForNewGame(playerSpecClass, playerSpec);
            if (portraitsIssue != null) {
                missing.add(portraitsIssue);
            }

            try {
                Class<?> commoditySpecClass =
                        Class.forName("com.fs.starfarer.loading.specs.CommoditySpec");
                if (lookupSpecById(specStoreClass, commoditySpecClass, "drugs") == null) {
                    missing.add("commodity:drugs");
                }
            } catch (Throwable ignored) {
            }

            boolean hasMissingFactionSpecs = false;
            for (String item : missing) {
                if (item != null && item.startsWith("faction:")) {
                    hasMissingFactionSpecs = true;
                    break;
                }
            }
            if (hasMissingFactionSpecs && allowRepair) {
                String repairIssue =
                        tryRepairMissingFactionSpecsForDirectNewGame(
                                specStoreClass, factionSpecClasses, missing);
                if (repairIssue != null) {
                    System.out.println(
                            "Fixer: faction preflight repair warning: " + repairIssue);
                }

                missing.clear();
                playerSpec = null;
                playerSpecClass = null;
                for (String factionId : requiredFactionIds) {
                    Object spec = null;
                    Class<?> resolvedSpecClass = null;
                    for (Class<?> candidateClass : factionSpecClasses) {
                        if (candidateClass == null) {
                            continue;
                        }
                        Object candidateSpec =
                                lookupSpecById(specStoreClass, candidateClass, factionId);
                        if (candidateSpec != null) {
                            spec = candidateSpec;
                            resolvedSpecClass = candidateClass;
                            break;
                        }
                    }
                    if (spec == null) {
                        missing.add("faction:" + factionId);
                        continue;
                    }
                    if ("player".equals(factionId)) {
                        playerSpec = spec;
                        playerSpecClass = resolvedSpecClass;
                    }
                }
                portraitsIssue =
                        ensurePlayerFactionPortraitsReadyForNewGame(playerSpecClass, playerSpec);
                if (portraitsIssue != null) {
                    missing.add(portraitsIssue);
                }
                try {
                    Class<?> commoditySpecClass =
                            Class.forName("com.fs.starfarer.loading.specs.CommoditySpec");
                    if (lookupSpecById(specStoreClass, commoditySpecClass, "drugs") == null) {
                        missing.add("commodity:drugs");
                    }
                } catch (Throwable ignored) {
                }
            }

            String runtimeFactionIssue =
                    ensureRuntimeFactionsPresentForDirectNewGame(requiredFactionIds, allowRepair);
            if (runtimeFactionIssue != null) {
                missing.add(runtimeFactionIssue);
            }

            if (!missing.isEmpty()) {
                return "spec store not ready: " + String.join(", ", missing);
            }
            return null;
        } catch (Throwable t) {
            return "faction preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureRuntimeFactionsPresentForDirectNewGame(
            String[] requiredFactionIds, boolean allowRepair) {
        if (requiredFactionIds == null || requiredFactionIds.length == 0) {
            return null;
        }
        try {
            Class<?> campaignEngineClass = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
            Method getInstance = findMethodRecursive(campaignEngineClass, "getInstance");
            if (getInstance == null) {
                return "runtime-faction-manager:getInstance-missing";
            }
            getInstance.setAccessible(true);
            Object engine = getInstance.invoke(null);
            if (engine == null) {
                return "runtime-faction-manager:campaign-engine-null";
            }

            Method getFactionManager = findMethodRecursive(campaignEngineClass, "getFactionManager");
            if (getFactionManager == null) {
                return "runtime-faction-manager:getFactionManager-missing";
            }
            getFactionManager.setAccessible(true);
            Object manager = getFactionManager.invoke(engine);
            if (manager == null) {
                return "runtime-faction-manager:null";
            }

            List<String> missing = collectMissingRuntimeFactionIds(manager, requiredFactionIds);
            if (missing.isEmpty()) {
                return null;
            }

            if (allowRepair) {
                invokeRuntimeFactionManagerReadResolve(manager);
                missing = collectMissingRuntimeFactionIds(manager, requiredFactionIds);
            }
            if (allowRepair && !missing.isEmpty()) {
                String injectIssue = injectMissingRuntimeFactions(manager, missing);
                if (injectIssue != null) {
                    return "runtime-faction-manager-repair:" + injectIssue;
                }
                invokeRuntimeFactionManagerReadResolve(manager);
                missing = collectMissingRuntimeFactionIds(manager, requiredFactionIds);
            }
            if (missing.isEmpty()) {
                return null;
            }
            return "faction:" + String.join(", faction:", missing) + " (runtime)";
        } catch (Throwable t) {
            return "runtime-faction-manager:" + describeThrowableChain(t);
        }
    }

    private static List<String> collectMissingRuntimeFactionIds(
            Object manager, String[] requiredFactionIds) {
        List<String> missing = new ArrayList<String>();
        if (manager == null || requiredFactionIds == null) {
            return missing;
        }
        try {
            Method getFaction = findMethodRecursive(manager.getClass(), "getFaction", String.class);
            if (getFaction == null) {
                missing.add("neutral");
                return missing;
            }
            getFaction.setAccessible(true);
            for (String factionId : requiredFactionIds) {
                if (factionId == null || factionId.trim().isEmpty()) {
                    continue;
                }
                String cleanId = factionId.trim();
                Object runtimeFaction = getFaction.invoke(manager, cleanId);
                if (!isUsableRuntimeFaction(runtimeFaction)) {
                    missing.add(cleanId);
                }
            }
        } catch (Throwable t) {
            missing.clear();
            for (String factionId : requiredFactionIds) {
                if (factionId == null || factionId.trim().isEmpty()) {
                    continue;
                }
                missing.add(factionId.trim());
            }
        }
        return missing;
    }

    private static boolean isUsableRuntimeFaction(Object faction) {
        if (faction == null) {
            return false;
        }
        try {
            Method getSpec = findMethodRecursive(faction.getClass(), "getSpec");
            if (getSpec == null) {
                return true;
            }
            getSpec.setAccessible(true);
            Object spec = getSpec.invoke(faction);
            return spec != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void invokeRuntimeFactionManagerReadResolve(Object manager) {
        if (manager == null) {
            return;
        }
        try {
            Method readResolve = findMethodRecursive(manager.getClass(), "readResolve");
            if (readResolve == null) {
                return;
            }
            readResolve.setAccessible(true);
            readResolve.invoke(manager);
        } catch (Throwable ignored) {
        }
    }

    private static String injectMissingRuntimeFactions(Object manager, List<String> missingIds) {
        if (manager == null) {
            return "manager-null";
        }
        if (missingIds == null || missingIds.isEmpty()) {
            return null;
        }
        try {
            Field factionsField = findFieldRecursive(manager.getClass(), "factions");
            if (factionsField == null) {
                return "factions-map-field-missing";
            }
            factionsField.setAccessible(true);
            Object mapObj = factionsField.get(manager);
            if (!(mapObj instanceof Map)) {
                return "factions-map-unavailable";
            }
            Map factionMap = (Map) mapObj;

            Class<?> factionClass = Class.forName("com.fs.starfarer.campaign.Faction");
            java.lang.reflect.Constructor<?> ctor = null;
            try {
                ctor = factionClass.getDeclaredConstructor(String.class);
            } catch (Throwable ignored) {
            }
            if (ctor == null) {
                try {
                    ctor = factionClass.getConstructor(String.class);
                } catch (Throwable ignored) {
                }
            }
            if (ctor == null) {
                return "faction-ctor-missing";
            }
            ctor.setAccessible(true);

            int inserted = 0;
            int replaced = 0;
            for (String factionId : missingIds) {
                if (factionId == null || factionId.trim().isEmpty()) {
                    continue;
                }
                String cleanId = factionId.trim();
                Object existingFaction = factionMap.get(cleanId);
                if (isUsableRuntimeFaction(existingFaction)) {
                    continue;
                }
                Object faction = ctor.newInstance(cleanId);
                factionMap.put(cleanId, faction);
                if (existingFaction == null) {
                    inserted++;
                } else {
                    replaced++;
                }
                if ("player".equals(cleanId)) {
                    try {
                        Field playerFactionField =
                                findFieldRecursive(manager.getClass(), "playerFaction");
                        if (playerFactionField != null) {
                            playerFactionField.setAccessible(true);
                            playerFactionField.set(manager, faction);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (inserted > 0) {
                System.out.println(
                        "Fixer: runtime faction-manager repair inserted="
                                + inserted
                                + " replaced="
                                + replaced
                                + " missing="
                                + missingIds.size());
            } else if (replaced > 0) {
                System.out.println(
                        "Fixer: runtime faction-manager repair replaced="
                                + replaced
                                + " missing="
                                + missingIds.size());
            }
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static String tryRepairMissingFactionSpecsForDirectNewGame(
            Class<?> specStoreClass, List<Class<?>> factionSpecClasses, List<String> missingItems) {
        if (missingItems == null || missingItems.isEmpty()) {
            return null;
        }
        try {
            Class<?> factionSpecClass = null;
            for (Class<?> candidate : factionSpecClasses) {
                if (candidate == null) {
                    continue;
                }
                if ("com.fs.starfarer.loading.if".equals(candidate.getName())) {
                    factionSpecClass = candidate;
                    break;
                }
            }
            if (factionSpecClass == null && !factionSpecClasses.isEmpty()) {
                factionSpecClass = factionSpecClasses.get(0);
            }
            if (factionSpecClass == null) {
                return "faction-repair:no-faction-spec-class";
            }
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "faction-repair:no-register-method";
            }
            registerSpec.setAccessible(true);

            String defaultsIssue = ensureFactionDefaultsForRepair();
            float defaultTariff = resolveFactionDefaultTariffForRepair();

            int inserted = 0;
            List<String> failed = new ArrayList<String>();
            for (String item : missingItems) {
                if (item == null || !item.startsWith("faction:")) {
                    continue;
                }
                String factionId = item.substring("faction:".length()).trim();
                if (factionId.length() == 0) {
                    continue;
                }
                Object existing = lookupSpecByIdAcrossClasses(specStoreClass, factionSpecClasses, factionId);
                if (existing != null) {
                    continue;
                }

                Object factionJson = loadFactionJsonForRepair(factionId);
                Object fallbackSpec =
                        createFallbackFactionSpecForDirectNewGame(
                                factionSpecClass, factionId, factionJson, defaultTariff);
                if (fallbackSpec == null) {
                    failed.add(factionId + "(build-null)");
                    continue;
                }

                registerSpec.invoke(null, factionSpecClass, factionId, fallbackSpec);
                Object after = lookupSpecByIdAcrossClasses(specStoreClass, factionSpecClasses, factionId);
                if (after == null) {
                    failed.add(factionId + "(verify-failed)");
                    continue;
                }
                inserted++;
            }

            if (inserted > 0 || defaultsIssue != null || !failed.isEmpty()) {
                System.out.println(
                        "Fixer: faction repair inserted="
                                + inserted
                                + " failed="
                                + failed.size()
                                + " defaults="
                                + (defaultsIssue == null ? "ok" : defaultsIssue));
            }
            if (!failed.isEmpty()) {
                return "faction-repair-failed:" + String.join(", ", failed);
            }
            return defaultsIssue;
        } catch (Throwable t) {
            return "faction-repair-exception:" + describeThrowableChain(t);
        }
    }

    private static Object loadFactionJsonForRepair(String factionId) {
        if (factionId == null || factionId.trim().isEmpty()) {
            return null;
        }
        String cleanId = factionId.trim();
        String directPath = "data/world/factions/" + cleanId + ".faction";
        Object direct = loadConfigJsonViaLoadingUtils(directPath);
        if (direct != null) {
            return direct;
        }
        try {
            List<String> entries = readIndexEntries("data/world/factions");
            for (String entry : entries) {
                if (entry == null) {
                    continue;
                }
                String rel = entry.replace('\\', '/').trim();
                if (rel.length() == 0 || !rel.toLowerCase().endsWith(".faction")) {
                    continue;
                }
                if (!rel.toLowerCase().endsWith(cleanId.toLowerCase() + ".faction")) {
                    continue;
                }
                String path =
                        rel.startsWith("data/") ? rel : ("data/world/factions/" + rel);
                Object fallback = loadConfigJsonViaLoadingUtils(path);
                if (fallback != null) {
                    return fallback;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String ensureFactionDefaultsForRepair() {
        try {
            Class<?> factionSpecClass = Class.forName("com.fs.starfarer.loading.if");
            Class<?> rolesClass = Class.forName("com.fs.starfarer.loading.V");
            Class<?> fleetNamesClass = Class.forName("com.fs.starfarer.loading.R");
            Class<?> ranksClass = Class.forName("com.fs.starfarer.loading.O");

            Object shipRolesJson =
                    loadConfigJsonViaLoadingUtils("data/world/factions/default_ship_roles.json");
            if (shipRolesJson != null) {
                java.lang.reflect.Constructor<?> ctor =
                        findJsonConstructorForRepair(rolesClass, shipRolesJson);
                Method setter = findMethodRecursive(factionSpecClass, "setDefaultShipRoles", rolesClass);
                if (ctor != null && setter != null) {
                    ctor.setAccessible(true);
                    setter.setAccessible(true);
                    setter.invoke(null, ctor.newInstance(shipRolesJson));
                }
            }

            Object fleetNamesJson =
                    loadConfigJsonViaLoadingUtils("data/world/factions/default_fleet_type_names.json");
            if (fleetNamesJson != null) {
                java.lang.reflect.Constructor<?> ctor =
                        findJsonConstructorForRepair(fleetNamesClass, fleetNamesJson);
                Method setter =
                        findMethodRecursive(factionSpecClass, "setDefaultFleetNames", fleetNamesClass);
                if (ctor != null && setter != null) {
                    ctor.setAccessible(true);
                    setter.setAccessible(true);
                    setter.invoke(null, ctor.newInstance(fleetNamesJson));
                }
            }

            Object ranksJson = loadConfigJsonViaLoadingUtils("data/world/factions/default_ranks.json");
            if (ranksJson != null) {
                java.lang.reflect.Constructor<?> ctor =
                        findJsonConstructorForRepair(ranksClass, ranksJson);
                Method setter = findMethodRecursive(factionSpecClass, "setDefaultRanks", ranksClass);
                if (ctor != null && setter != null) {
                    ctor.setAccessible(true);
                    setter.setAccessible(true);
                    setter.invoke(null, ctor.newInstance(ranksJson));
                }
            }
            return null;
        } catch (Throwable t) {
            return "defaults:" + describeThrowableChain(t);
        }
    }

    private static float resolveFactionDefaultTariffForRepair() {
        Object economy = loadConfigJsonViaLoadingUtils("data/campaign/econ/economy.json");
        return optJsonFloatForRepair(economy, "defaultTariff", 0f);
    }

    private static Object createFallbackFactionSpecForDirectNewGame(
            Class<?> factionSpecClass, String factionId, Object factionJson, float defaultTariff) {
        try {
            java.lang.reflect.Constructor<?> ctor = factionSpecClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object spec = ctor.newInstance();

            invokeInstanceMethodIfPresent(spec, "setId", String.class, factionId);
            String displayName = optJsonStringForRepair(factionJson, "displayName", factionId);
            String displayWithArticle =
                    optJsonStringForRepair(factionJson, "displayNameWithArticle", displayName);
            invokeInstanceMethodIfPresent(spec, "setDisplayName", String.class, displayName);
            invokeInstanceMethodIfPresent(
                    spec,
                    "setDisplayNameLong",
                    String.class,
                    optJsonStringForRepair(factionJson, "displayNameLong", displayName));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setDisplayNameWithArticle",
                    String.class,
                    displayWithArticle);
            invokeInstanceMethodIfPresent(
                    spec,
                    "setDisplayNameLongWithArticle",
                    String.class,
                    optJsonStringForRepair(
                            factionJson, "displayNameLongWithArticle", displayWithArticle));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setEntityNamePrefix",
                    String.class,
                    optJsonStringForRepair(factionJson, "entityNamePrefix", displayName));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setPersonNamePrefix",
                    String.class,
                    optJsonStringForRepair(factionJson, "personNamePrefix", displayName));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setPersonNamePrefixAOrAn",
                    String.class,
                    optJsonStringForRepair(factionJson, "personNamePrefixAOrAn", "a"));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setDisplayNameIsOrAre",
                    String.class,
                    optJsonStringForRepair(factionJson, "displayNameIsOrAre", "is"));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setShipNamePrefix",
                    String.class,
                    optJsonStringForRepair(factionJson, "shipNamePrefix", "ISS"));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setBarSound",
                    String.class,
                    optJsonStringForRepair(factionJson, "barSound", "bar_ambience"));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setShowInIntelTab",
                    Boolean.TYPE,
                    Boolean.valueOf(optJsonBooleanForRepair(factionJson, "showInIntelTab", true)));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setTariffFraction",
                    Float.TYPE,
                    Float.valueOf(optJsonFloatForRepair(factionJson, "tariffFraction", defaultTariff)));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setTollFraction",
                    Float.TYPE,
                    Float.valueOf(optJsonFloatForRepair(factionJson, "tollFraction", 0.1f)));
            invokeInstanceMethodIfPresent(
                    spec,
                    "setFineFraction",
                    Float.TYPE,
                    Float.valueOf(optJsonFloatForRepair(factionJson, "fineFraction", 0.25f)));

            String logo = optJsonStringForRepair(factionJson, "logo", null);
            if (logo != null && logo.trim().length() > 0) {
                invokeInstanceMethodIfPresent(spec, "setLogo", String.class, logo.trim());
            }
            String crest = optJsonStringForRepair(factionJson, "crest", null);
            if (crest != null && crest.trim().length() > 0) {
                invokeInstanceMethodIfPresent(spec, "setCrest", String.class, crest.trim());
            }

            java.awt.Color baseColor = resolveFactionColorForRepair(factionJson);
            invokeInstanceMethodIfPresent(spec, "setColor", java.awt.Color.class, baseColor);
            invokeInstanceMethodIfPresent(spec, "setBaseUIColor", java.awt.Color.class, baseColor);
            invokeInstanceMethodIfPresent(
                    spec, "setBrightUIColor", java.awt.Color.class, baseColor.brighter());
            invokeInstanceMethodIfPresent(spec, "setDarkUIColor", java.awt.Color.class, baseColor.darker());
            invokeInstanceMethodIfPresent(
                    spec,
                    "setGridUIColor",
                    java.awt.Color.class,
                    new java.awt.Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 75));
            invokeInstanceMethodIfPresent(spec, "setSecondaryUIColor", java.awt.Color.class, baseColor);
            invokeInstanceMethodIfPresent(spec, "setSecondarySegments", Integer.TYPE, Integer.valueOf(8));

            Object shipRolesJson = optJsonObjectForRepair(factionJson, "shipRoles");
            if (shipRolesJson != null) {
                Object doctrineJsonForRoles = optJsonObjectForRepair(factionJson, "doctrine");
                if (doctrineJsonForRoles != null) {
                    try {
                        Method putMethod =
                                findMethodRecursive(
                                        shipRolesJson.getClass(), "put", String.class, Object.class);
                        if (putMethod != null) {
                            putMethod.setAccessible(true);
                            putMethod.invoke(shipRolesJson, "doctrine", doctrineJsonForRoles);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    Class<?> rolesClass = Class.forName("com.fs.starfarer.loading.V");
                    java.lang.reflect.Constructor<?> rolesCtor =
                            findJsonConstructorForRepair(rolesClass, shipRolesJson);
                    Method setShipRoles =
                            findMethodRecursive(factionSpecClass, "setShipRoles", rolesClass);
                    if (rolesCtor != null && setShipRoles != null) {
                        rolesCtor.setAccessible(true);
                        setShipRoles.setAccessible(true);
                        setShipRoles.invoke(spec, rolesCtor.newInstance(shipRolesJson));
                    }
                } catch (Throwable ignored) {
                }
            }

            Object fleetTypeNamesJson = optJsonObjectForRepair(factionJson, "fleetTypeNames");
            if (fleetTypeNamesJson != null) {
                try {
                    Class<?> fleetNamesClass = Class.forName("com.fs.starfarer.loading.R");
                    java.lang.reflect.Constructor<?> fleetCtor =
                            findJsonConstructorForRepair(fleetNamesClass, fleetTypeNamesJson);
                    Method setFleetNames =
                            findMethodRecursive(factionSpecClass, "setFleetNames", fleetNamesClass);
                    if (fleetCtor != null && setFleetNames != null) {
                        fleetCtor.setAccessible(true);
                        setFleetNames.setAccessible(true);
                        setFleetNames.invoke(spec, fleetCtor.newInstance(fleetTypeNamesJson));
                    }
                } catch (Throwable ignored) {
                }
            }

            Object ranksJson = optJsonObjectForRepair(factionJson, "ranks");
            if (ranksJson != null) {
                try {
                    Class<?> ranksClass = Class.forName("com.fs.starfarer.loading.O");
                    java.lang.reflect.Constructor<?> ranksCtor =
                            findJsonConstructorForRepair(ranksClass, ranksJson);
                    Method setRanks =
                            findMethodRecursive(factionSpecClass, "setRanksAndPosts", ranksClass);
                    if (ranksCtor != null && setRanks != null) {
                        ranksCtor.setAccessible(true);
                        setRanks.setAccessible(true);
                        setRanks.invoke(spec, ranksCtor.newInstance(ranksJson));
                    }
                } catch (Throwable ignored) {
                }
            }

            Object factionDoctrineJson = optJsonObjectForRepair(factionJson, "factionDoctrine");
            if (factionDoctrineJson != null) {
                try {
                    Class<?> doctrineClass = Class.forName("com.fs.starfarer.loading.specs.FactionDoctrine");
                    java.lang.reflect.Constructor<?> doctrineCtor =
                            findJsonConstructorForRepair(doctrineClass, factionDoctrineJson);
                    Method getDoctrine = findMethodRecursive(factionSpecClass, "getDoctrine");
                    Method copyTo = findMethodRecursive(doctrineClass, "copyTo", doctrineClass);
                    if (doctrineCtor != null && getDoctrine != null && copyTo != null) {
                        doctrineCtor.setAccessible(true);
                        getDoctrine.setAccessible(true);
                        copyTo.setAccessible(true);
                        Object doctrine = doctrineCtor.newInstance(factionDoctrineJson);
                        Object targetDoctrine = getDoctrine.invoke(spec);
                        if (targetDoctrine != null) {
                            copyTo.invoke(doctrine, targetDoctrine);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            return spec;
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: faction fallback build failed for "
                            + factionId
                            + ": "
                            + describeThrowableChain(t));
            return null;
        }
    }

    private static java.awt.Color resolveFactionColorForRepair(Object factionJson) {
        java.awt.Color fallback = new java.awt.Color(96, 170, 220);
        if (factionJson == null) {
            return fallback;
        }
        try {
            Class<?> colorHelperClass = Class.forName("com.fs.starfarer.loading.O0OO");
            Method parser = findMethodRecursive(colorHelperClass, "return", factionJson.getClass(), String.class);
            if (parser == null) {
                parser =
                        findMethodRecursive(
                                colorHelperClass, "return", Class.forName("org.json.JSONObject"), String.class);
            }
            if (parser == null) {
                return fallback;
            }
            parser.setAccessible(true);
            Object parsed = parser.invoke(null, factionJson, "color");
            if (parsed instanceof java.awt.Color) {
                return (java.awt.Color) parsed;
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static java.lang.reflect.Constructor<?> findJsonConstructorForRepair(
            Class<?> targetClass, Object jsonObj) {
        if (targetClass == null || jsonObj == null) {
            return null;
        }
        try {
            return targetClass.getDeclaredConstructor(jsonObj.getClass());
        } catch (Throwable ignored) {
        }
        try {
            return targetClass.getDeclaredConstructor(Class.forName("org.json.JSONObject"));
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void invokeInstanceMethodIfPresent(
            Object target, String methodName, Class<?> paramType, Object value) {
        if (target == null || methodName == null || methodName.length() == 0 || paramType == null) {
            return;
        }
        try {
            Method method = findMethodRecursive(target.getClass(), methodName, paramType);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            method.invoke(target, value);
        } catch (Throwable ignored) {
        }
    }

    private static String optJsonStringForRepair(Object jsonObj, String key, String fallback) {
        if (jsonObj == null || key == null || key.length() == 0) {
            return fallback;
        }
        try {
            Method optString =
                    findMethodRecursive(jsonObj.getClass(), "optString", String.class, String.class);
            if (optString != null) {
                optString.setAccessible(true);
                Object value = optString.invoke(jsonObj, key, fallback);
                return value == null ? fallback : String.valueOf(value);
            }
            Method has = findMethodRecursive(jsonObj.getClass(), "has", String.class);
            Method getString = findMethodRecursive(jsonObj.getClass(), "getString", String.class);
            if (has != null && getString != null) {
                has.setAccessible(true);
                getString.setAccessible(true);
                Object exists = has.invoke(jsonObj, key);
                if (Boolean.TRUE.equals(exists)) {
                    Object value = getString.invoke(jsonObj, key);
                    return value == null ? fallback : String.valueOf(value);
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static boolean optJsonBooleanForRepair(Object jsonObj, String key, boolean fallback) {
        if (jsonObj == null || key == null || key.length() == 0) {
            return fallback;
        }
        try {
            Method optBoolean =
                    findMethodRecursive(jsonObj.getClass(), "optBoolean", String.class, Boolean.TYPE);
            if (optBoolean != null) {
                optBoolean.setAccessible(true);
                Object value = optBoolean.invoke(jsonObj, key, Boolean.valueOf(fallback));
                if (value instanceof Boolean) {
                    return ((Boolean) value).booleanValue();
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static float optJsonFloatForRepair(Object jsonObj, String key, float fallback) {
        if (jsonObj == null || key == null || key.length() == 0) {
            return fallback;
        }
        try {
            Method optDouble =
                    findMethodRecursive(jsonObj.getClass(), "optDouble", String.class, Double.TYPE);
            if (optDouble != null) {
                optDouble.setAccessible(true);
                Object value = optDouble.invoke(jsonObj, key, Double.valueOf((double) fallback));
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static Object optJsonObjectForRepair(Object jsonObj, String key) {
        if (jsonObj == null || key == null || key.length() == 0) {
            return null;
        }
        try {
            Method optJSONObject = findMethodRecursive(jsonObj.getClass(), "optJSONObject", String.class);
            if (optJSONObject != null) {
                optJSONObject.setAccessible(true);
                Object value = optJSONObject.invoke(jsonObj, key);
                if (value != null) {
                    return value;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Method has = findMethodRecursive(jsonObj.getClass(), "has", String.class);
            Method getJSONObject = findMethodRecursive(jsonObj.getClass(), "getJSONObject", String.class);
            if (has != null && getJSONObject != null) {
                has.setAccessible(true);
                getJSONObject.setAccessible(true);
                Object exists = has.invoke(jsonObj, key);
                if (Boolean.TRUE.equals(exists)) {
                    return getJSONObject.invoke(jsonObj, key);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String ensurePlayerFactionPortraitsReadyForNewGame(
            Class<?> factionSpecClass, Object playerSpec) {
        if (playerSpec == null) {
            return "faction:player";
        }
        if (factionSpecClass == null) {
            return "player-portraits:faction-spec-class-missing";
        }
        try {
            Method getAllPortraits = null;
            for (Method m : factionSpecClass.getMethods()) {
                if (m == null || !"getAllPortraits".equals(m.getName())) {
                    continue;
                }
                if (m.getParameterTypes().length == 2) {
                    getAllPortraits = m;
                    break;
                }
            }
            if (getAllPortraits == null) {
                return null;
            }
            getAllPortraits.setAccessible(true);
            Class<?> portraitCategoryClass = getAllPortraits.getParameterTypes()[0];
            Object portraitCategory = null;
            if (portraitCategoryClass != null && portraitCategoryClass.isEnum()) {
                Object[] constants = portraitCategoryClass.getEnumConstants();
                if (constants != null && constants.length > 0) {
                    portraitCategory = constants[0];
                }
            }
            if (portraitCategory == null) {
                return "player-portraits:category-missing";
            }
            Class<?> genderClass = Class.forName("com.fs.starfarer.api.characters.FullName$Gender");
            @SuppressWarnings("unchecked")
            Class<? extends Enum> genderEnumClass = (Class<? extends Enum>) genderClass.asSubclass(Enum.class);
            Object male = Enum.valueOf(genderEnumClass, "MALE");
            Object female = Enum.valueOf(genderEnumClass, "FEMALE");
            int maleCount = countCollectionLike(getAllPortraits.invoke(playerSpec, portraitCategory, male));
            int femaleCount = countCollectionLike(getAllPortraits.invoke(playerSpec, portraitCategory, female));
            if (maleCount <= 0 || femaleCount <= 0) {
                return "player-portraits:" + maleCount + "/" + femaleCount;
            }
            return null;
        } catch (Throwable t) {
            return "player-portraits-check:" + t.getClass().getSimpleName();
        }
    }

    private static int countCollectionLike(Object value) {
        if (value == null) {
            return -1;
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            return Array.getLength(value);
        }
        return -1;
    }

    private static String ensureUiFontStateReadyForDirectNewGame() {
        try {
            Class<?> settingsClass = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
            final String fallbackDefaultFont = "graphics/fonts/orbitron12condensed.fnt";
            String defaultFontPath = fallbackDefaultFont;

            Field defaultFontField = findFieldRecursive(settingsClass, "\u00f8\u00d20000");
            if (defaultFontField == null) {
                for (Field f : settingsClass.getDeclaredFields()) {
                    if (f == null || f.getType() != String.class) {
                        continue;
                    }
                    int mods = f.getModifiers();
                    if (!Modifier.isStatic(mods) || Modifier.isFinal(mods)) {
                        continue;
                    }
                    f.setAccessible(true);
                    Object value = f.get(null);
                    if (!(value instanceof String)
                            || ((String) value).trim().isEmpty()
                            || ((String) value).indexOf("graphics/fonts/") >= 0) {
                        defaultFontField = f;
                        break;
                    }
                }
            }

            if (defaultFontField != null) {
                defaultFontField.setAccessible(true);
                Object current = defaultFontField.get(null);
                if (current instanceof String && !((String) current).trim().isEmpty()) {
                    defaultFontPath = ((String) current).trim();
                } else {
                    defaultFontField.set(null, fallbackDefaultFont);
                    defaultFontPath = fallbackDefaultFont;
                    System.out.println(
                            "Fixer: direct new-game ui font preflight set default font field "
                                    + defaultFontField.getName()
                                    + " -> "
                                    + fallbackDefaultFont);
                }
            } else {
                System.out.println(
                        "Fixer: direct new-game ui font preflight could not resolve StarfarerSettings default font field; using fallback path "
                                + fallbackDefaultFont);
            }

            int defaultedFields = 0;
            for (Field f : settingsClass.getDeclaredFields()) {
                if (f == null || f.getType() != String.class) {
                    continue;
                }
                int mods = f.getModifiers();
                if (!Modifier.isStatic(mods) || Modifier.isFinal(mods)) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object value = f.get(null);
                    if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                        f.set(null, defaultFontPath);
                        defaultedFields++;
                    }
                } catch (Throwable ignored) {
                }
            }

            Class<?> fontManagerClass = Class.forName("com.fs.graphics.super.D");
            Class<?> fontClass = Class.forName("com.fs.graphics.super.return");
            Method lookupMethod = null;
            Method loadMethod = null;
            for (Method m : fontManagerClass.getDeclaredMethods()) {
                if (m == null || !Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0] == String.class && m.getReturnType() == fontClass) {
                    lookupMethod = m;
                } else if (params.length == 2
                        && params[0] == String.class
                        && params[1] == String.class
                        && m.getReturnType() == Void.TYPE) {
                    loadMethod = m;
                }
            }
            if (lookupMethod == null || loadMethod == null) {
                return "font manager reflection methods unavailable";
            }
            lookupMethod.setAccessible(true);
            loadMethod.setAccessible(true);

            LinkedHashSet<String> requiredFonts = new LinkedHashSet<String>();
            requiredFonts.add(defaultFontPath);
            requiredFonts.add("graphics/fonts/orbitron24aabold.fnt");
            requiredFonts.add("graphics/fonts/orbitron20aabold.fnt");
            requiredFonts.add("graphics/fonts/insignia21LTaa.fnt");
            for (Field f : settingsClass.getDeclaredFields()) {
                if (f == null || f.getType() != String.class) {
                    continue;
                }
                int mods = f.getModifiers();
                if (!Modifier.isStatic(mods)) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object value = f.get(null);
                    if (!(value instanceof String)) {
                        continue;
                    }
                    String text = ((String) value).trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    String lower = text.toLowerCase();
                    if (text.startsWith("graphics/fonts/") && lower.endsWith(".fnt")) {
                        requiredFonts.add(text);
                    }
                } catch (Throwable ignored) {
                }
            }
            requiredFonts.addAll(collectFontPathsFromIndex("graphics/fonts"));

            List<String> missingBefore = new ArrayList<String>();
            for (String font : requiredFonts) {
                if (font == null || font.trim().isEmpty()) {
                    continue;
                }
                Object loaded = lookupMethod.invoke(null, font);
                if (loaded == null) {
                    missingBefore.add(font);
                }
            }
            if (missingBefore.isEmpty()) {
                return null;
            }

            int loadedCount = 0;
            List<String> unresolved = new ArrayList<String>();
            for (String font : missingBefore) {
                try {
                    loadMethod.invoke(null, font, font);
                } catch (Throwable t) {
                    unresolved.add(font + "(load-failed:" + describeThrowableChain(t) + ")");
                    continue;
                }
                try {
                    Object loaded = lookupMethod.invoke(null, font);
                    if (loaded == null) {
                        unresolved.add(font + "(still-null)");
                    } else {
                        loadedCount++;
                    }
                } catch (Throwable t) {
                    unresolved.add(font + "(verify-failed:" + describeThrowableChain(t) + ")");
                }
            }

            System.out.println(
                    "Fixer: ui font preflight default="
                            + defaultFontPath
                            + " required="
                            + requiredFonts.size()
                            + " missingBefore="
                            + missingBefore.size()
                            + " loaded="
                            + loadedCount
                            + " defaultedFields="
                            + defaultedFields
                            + " unresolved="
                            + unresolved.size());

            if (!unresolved.isEmpty()) {
                return "unresolved ui fonts: " + String.join(", ", unresolved);
            }
            return null;
        } catch (Throwable t) {
            return "ui font preflight exception: " + describeThrowableChain(t);
        }
    }

    private static List<String> collectFontPathsFromIndex(String dir) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        try {
            List<String> entries = readIndexEntries(dir);
            for (String entry : entries) {
                if (entry == null) {
                    continue;
                }
                String clean = entry.replace('\\', '/').trim();
                if (clean.isEmpty()) {
                    continue;
                }
                String lower = clean.toLowerCase();
                if (!lower.endsWith(".fnt")) {
                    continue;
                }
                if (clean.startsWith("graphics/fonts/")) {
                    out.add(clean);
                } else {
                    out.add("graphics/fonts/" + clean);
                }
            }
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: ui font preflight could not enumerate "
                            + dir
                            + "/index.list: "
                            + describeThrowableChain(t));
        }
        return new ArrayList<String>(out);
    }

    private static String ensureCustomEntitySpecsReadyForDirectNewGame() {
        try {
            Class<?> customEntitySpecClass = resolveCustomEntitySpecClass();
            if (customEntitySpecClass == null) {
                return "custom entity spec class unavailable";
            }
            Class<?> specStoreClass =
                    resolveSpecStoreClassForCustomEntityClass(customEntitySpecClass);
            if (specStoreClass == null) {
                return "SpecStore class unavailable for custom entity preflight";
            }
            System.out.println(
                    "Fixer: custom entity preflight using class=" + customEntitySpecClass.getName());

            List<String> criticalIds = new ArrayList<String>();
            criticalIds.add("orbital_junk");
            criticalIds.add("base_campaign_objective");
            criticalIds.add("debris_field_shared");

            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, customEntitySpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            Object root = loadConfigJsonViaLoadingUtils("data/config/custom_entities.json");
            if (root == null) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "custom_entities.json unavailable, missing critical custom entity specs: "
                        + String.join(", ", missingBefore);
            }

            Class<?> jsonObjectClass = root.getClass();
            Method getNames = findMethodRecursive(jsonObjectClass, "getNames", jsonObjectClass);
            Method getJSONObject = findMethodRecursive(jsonObjectClass, "getJSONObject", String.class);
            Method optString =
                    findMethodRecursive(jsonObjectClass, "optString", String.class, String.class);
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (getNames == null
                    || getJSONObject == null
                    || optString == null
                    || registerSpec == null) {
                return "custom entity preload reflection methods unavailable";
            }
            getNames.setAccessible(true);
            getJSONObject.setAccessible(true);
            optString.setAccessible(true);
            registerSpec.setAccessible(true);

            Object namesObj = getNames.invoke(null, root);
            if (!(namesObj instanceof String[])) {
                return "custom entity key list unavailable";
            }
            String[] names = (String[]) namesObj;
            if (names.length == 0) {
                return "custom entity key list empty";
            }

            java.lang.reflect.Constructor<?> specCtor =
                    customEntitySpecClass.getConstructor(String.class, jsonObjectClass);
            Method cloneSpec = findMethodRecursive(customEntitySpecClass, "clone");
            Method setId = findMethodRecursive(customEntitySpecClass, "setId", String.class);
            Method loadSpec = findMethodRecursive(customEntitySpecClass, "load", jsonObjectClass);
            Method readResolveSpec = findMethodRecursive(customEntitySpecClass, "readResolve");
            if (cloneSpec != null) cloneSpec.setAccessible(true);
            if (setId != null) setId.setAccessible(true);
            if (loadSpec != null) loadSpec.setAccessible(true);
            if (readResolveSpec != null) readResolveSpec.setAccessible(true);

            LinkedHashSet<String> pending = new LinkedHashSet<String>();
            List<String> failed = new ArrayList<String>();
            int inserted = 0;
            int inherited = 0;
            int alreadyPresent = 0;

            for (String id : names) {
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, customEntitySpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }
                try {
                    Object specJson = getJSONObject.invoke(root, id);
                    String baseId = String.valueOf(optString.invoke(specJson, "baseId", ""));
                    if (baseId == null || baseId.trim().isEmpty()) {
                        Object created = specCtor.newInstance(id, specJson);
                        if (loadSpec != null) {
                            try {
                                loadSpec.invoke(created, specJson);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (readResolveSpec != null) {
                            try {
                                Object resolved = readResolveSpec.invoke(created);
                                if (resolved != null) {
                                    created = resolved;
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                        registerSpec.invoke(null, customEntitySpecClass, id, created);
                        inserted++;
                    } else {
                        pending.add(id);
                    }
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, customEntitySpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            int guard = names.length + 4;
            boolean progress = true;
            while (progress && !pending.isEmpty() && guard-- > 0) {
                progress = false;
                for (Iterator<String> it = pending.iterator(); it.hasNext(); ) {
                    String id = it.next();
                    if (id == null || id.trim().isEmpty()) {
                        it.remove();
                        continue;
                    }
                    if (lookupSpecById(specStoreClass, customEntitySpecClass, id) != null) {
                        alreadyPresent++;
                        it.remove();
                        progress = true;
                        continue;
                    }
                    try {
                        Object specJson = getJSONObject.invoke(root, id);
                        String baseId = String.valueOf(optString.invoke(specJson, "baseId", ""));
                        if (baseId == null || baseId.trim().isEmpty()) {
                            Object created = specCtor.newInstance(id, specJson);
                            if (loadSpec != null) {
                                try {
                                    loadSpec.invoke(created, specJson);
                                } catch (Throwable ignored) {
                                }
                            }
                            if (readResolveSpec != null) {
                                try {
                                    Object resolved = readResolveSpec.invoke(created);
                                    if (resolved != null) {
                                        created = resolved;
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                            registerSpec.invoke(null, customEntitySpecClass, id, created);
                            inserted++;
                            it.remove();
                            progress = true;
                            continue;
                        }
                        Object base = lookupSpecById(specStoreClass, customEntitySpecClass, baseId);
                        if (base == null) {
                            continue;
                        }
                        Object created = null;
                        if (cloneSpec != null) {
                            try {
                                created = cloneSpec.invoke(base);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (created != null && setId != null) {
                            try {
                                setId.invoke(created, id);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (created != null && loadSpec != null) {
                            try {
                                loadSpec.invoke(created, specJson);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (created == null) {
                            created = specCtor.newInstance(id, specJson);
                            if (loadSpec != null) {
                                try {
                                    loadSpec.invoke(created, specJson);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        if (created != null && readResolveSpec != null) {
                            try {
                                Object resolved = readResolveSpec.invoke(created);
                                if (resolved != null) {
                                    created = resolved;
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                        registerSpec.invoke(null, customEntitySpecClass, id, created);
                        inherited++;
                        it.remove();
                        progress = true;
                    } catch (Throwable t) {
                        String chain = describeThrowableChain(t);
                        String lower = chain == null ? "" : chain.toLowerCase();
                        if (lower.indexOf("already exists") >= 0) {
                            try {
                                if (lookupSpecById(specStoreClass, customEntitySpecClass, id) != null) {
                                    alreadyPresent++;
                                    it.remove();
                                    progress = true;
                                    continue;
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                        failed.add(id + "(" + chain + ")");
                        it.remove();
                        progress = true;
                    }
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, customEntitySpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            System.out.println(
                    "Fixer: custom entity spec preflight inserted="
                            + inserted
                            + " inherited="
                            + inherited
                            + " alreadyPresent="
                            + alreadyPresent
                            + " failed="
                            + failed.size()
                            + " pending="
                            + pending.size());

            if (!missingAfter.isEmpty()) {
                return "missing critical custom entity specs after preload: "
                        + String.join(", ", missingAfter)
                        + " pending="
                        + pending.size();
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "custom entity preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            if (!pending.isEmpty()) {
                int limit = Math.min(6, pending.size());
                List<String> pendingSample = new ArrayList<String>(pending).subList(0, limit);
                return "custom entity preload unresolved inheritance: "
                        + String.join(", ", pendingSample)
                        + (pending.size() > limit ? " ... +" + (pending.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "custom entity spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static Object buildOrbitalJunkSpecJsonForClass(Class<?> customEntitySpecClass)
            throws Exception {
        if (customEntitySpecClass == null) {
            return null;
        }
        ClassLoader loader = customEntitySpecClass.getClassLoader();
        Class<?> jsonObjectClass = null;
        Class<?> jsonArrayClass = null;
        try {
            jsonObjectClass = Class.forName("org.json.JSONObject", true, loader);
        } catch (Throwable ignored) {
        }
        if (jsonObjectClass == null) {
            jsonObjectClass = Class.forName("org.json.JSONObject");
        }
        try {
            jsonArrayClass = Class.forName("org.json.JSONArray", true, loader);
        } catch (Throwable ignored) {
        }
        if (jsonArrayClass == null) {
            jsonArrayClass = Class.forName("org.json.JSONArray");
        }

        java.lang.reflect.Constructor<?> jsonObjectCtor = jsonObjectClass.getDeclaredConstructor();
        java.lang.reflect.Constructor<?> jsonArrayCtor = jsonArrayClass.getDeclaredConstructor();
        jsonObjectCtor.setAccessible(true);
        jsonArrayCtor.setAccessible(true);
        Method jsonPut = findMethodRecursive(jsonObjectClass, "put", String.class, Object.class);
        Method jsonGetObject = findMethodRecursive(jsonObjectClass, "getJSONObject", String.class);
        Method arrayPut = findMethodRecursive(jsonArrayClass, "put", Object.class);
        if (jsonPut == null || arrayPut == null) {
            return null;
        }
        jsonPut.setAccessible(true);
        if (jsonGetObject != null) {
            jsonGetObject.setAccessible(true);
        }
        arrayPut.setAccessible(true);

        Object specJson = null;
        try {
            Object root = loadConfigJsonViaLoadingUtils("data/config/custom_entities.json");
            if (root != null && jsonGetObject != null) {
                Object rootForClass = root;
                if (!jsonObjectClass.isInstance(rootForClass)) {
                    java.lang.reflect.Constructor<?> jsonStringCtor = null;
                    try {
                        jsonStringCtor = jsonObjectClass.getDeclaredConstructor(String.class);
                    } catch (Throwable ignored) {
                    }
                    if (jsonStringCtor != null) {
                        jsonStringCtor.setAccessible(true);
                        rootForClass = jsonStringCtor.newInstance(String.valueOf(root));
                    } else {
                        rootForClass = null;
                    }
                }
                if (rootForClass != null) {
                    specJson = jsonGetObject.invoke(rootForClass, "orbital_junk");
                }
            }
        } catch (Throwable ignored) {
        }
        if (specJson != null) {
            normalizeOrbitalJunkSpecJson(
                    specJson, jsonObjectClass, jsonArrayCtor, jsonPut, arrayPut);
            return specJson;
        }

        specJson = jsonObjectCtor.newInstance();
        jsonPut.invoke(specJson, "defaultName", "Habitat");
        jsonPut.invoke(specJson, "defaultRadius", Double.valueOf(0d));
        jsonPut.invoke(specJson, "sheet", "graphics/stations/ministations_sheet00.png");
        jsonPut.invoke(specJson, "sheetCellSize", Double.valueOf(32d));
        jsonPut.invoke(specJson, "renderShadow", Boolean.TRUE);
        jsonPut.invoke(specJson, "useLightColor", Boolean.TRUE);
        jsonPut.invoke(specJson, "showInCampaign", Boolean.TRUE);
        jsonPut.invoke(specJson, "showIconOnMap", Boolean.FALSE);
        jsonPut.invoke(specJson, "showNameOnMap", Boolean.FALSE);
        jsonPut.invoke(specJson, "interactable", Boolean.FALSE);

        Object tags = jsonArrayCtor.newInstance();
        arrayPut.invoke(tags, "orbital_junk");
        jsonPut.invoke(specJson, "tags", tags);

        Object layers = jsonArrayCtor.newInstance();
        arrayPut.invoke(layers, "STATIONS");
        jsonPut.invoke(specJson, "layers", layers);
        normalizeOrbitalJunkSpecJson(specJson, jsonObjectClass, jsonArrayCtor, jsonPut, arrayPut);
        return specJson;
    }

    private static void normalizeOrbitalJunkSpecJson(
            Object specJson,
            Class<?> jsonObjectClass,
            java.lang.reflect.Constructor<?> jsonArrayCtor,
            Method jsonPut,
            Method arrayPut) {
        if (specJson == null || jsonObjectClass == null || jsonPut == null) {
            return;
        }
        try {
            Method jsonHas = findMethodRecursive(jsonObjectClass, "has", String.class);
            if (jsonHas != null) {
                jsonHas.setAccessible(true);
            }
            jsonPut.setAccessible(true);
            if (arrayPut != null) {
                arrayPut.setAccessible(true);
            }

            jsonPut.invoke(specJson, "defaultName", "Habitat");
            jsonPut.invoke(specJson, "defaultRadius", Double.valueOf(0d));
            jsonPut.invoke(specJson, "interactable", Boolean.FALSE);
            jsonPut.invoke(specJson, "showInCampaign", Boolean.TRUE);
            jsonPut.invoke(specJson, "showIconOnMap", Boolean.FALSE);
            jsonPut.invoke(specJson, "showNameOnMap", Boolean.FALSE);
            jsonPut.invoke(specJson, "renderShadow", Boolean.TRUE);
            jsonPut.invoke(specJson, "useLightColor", Boolean.TRUE);

            // Prefer a simple sprite path for startup safety; if unavailable, the original spec still
            // remains in place and runtime fallback logic can retry.
            jsonPut.invoke(specJson, "sprite", "graphics/stations/probe.png");
            jsonPut.invoke(specJson, "spriteWidth", Double.valueOf(36d));
            jsonPut.invoke(specJson, "spriteHeight", Double.valueOf(36d));

            boolean hasLayers = false;
            boolean hasTags = false;
            if (jsonHas != null) {
                try {
                    hasLayers = Boolean.TRUE.equals(jsonHas.invoke(specJson, "layers"));
                } catch (Throwable ignored) {
                }
                try {
                    hasTags = Boolean.TRUE.equals(jsonHas.invoke(specJson, "tags"));
                } catch (Throwable ignored) {
                }
            }
            if (!hasTags && jsonArrayCtor != null && arrayPut != null) {
                Object tags = jsonArrayCtor.newInstance();
                arrayPut.invoke(tags, "orbital_junk");
                jsonPut.invoke(specJson, "tags", tags);
            }
            if (!hasLayers && jsonArrayCtor != null && arrayPut != null) {
                Object layers = jsonArrayCtor.newInstance();
                arrayPut.invoke(layers, "STATIONS");
                jsonPut.invoke(specJson, "layers", layers);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean hasUsableCustomEntitySpecLayers(Object spec) {
        if (spec == null) {
            return false;
        }
        try {
            Method getLayers = findMethodRecursive(spec.getClass(), "getLayers");
            if (getLayers == null) {
                return true;
            }
            getLayers.setAccessible(true);
            Object layers = getLayers.invoke(spec);
            if (layers == null) {
                return false;
            }
            if (layers instanceof java.util.Collection) {
                java.util.Collection<?> c = (java.util.Collection<?>) layers;
                if (c.isEmpty()) {
                    return false;
                }
                for (Object entry : c) {
                    if (entry != null) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasUsableCustomEntitySpecTags(Object spec) {
        if (spec == null) {
            return false;
        }
        try {
            Method getTags = findMethodRecursive(spec.getClass(), "getTags");
            if (getTags == null) {
                return true;
            }
            getTags.setAccessible(true);
            Object tags = getTags.invoke(spec);
            if (tags == null) {
                return false;
            }
            if (tags instanceof java.util.Collection) {
                java.util.Collection<?> c = (java.util.Collection<?>) tags;
                if (c.isEmpty()) {
                    return false;
                }
                for (Object entry : c) {
                    if (entry != null) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String describeCustomEntitySpecLayerState(Object spec) {
        if (spec == null) {
            return "spec=null";
        }
        try {
            Method getLayers = findMethodRecursive(spec.getClass(), "getLayers");
            if (getLayers == null) {
                return "layersMethod=missing";
            }
            getLayers.setAccessible(true);
            Object layers = getLayers.invoke(spec);
            if (layers == null) {
                return "layers=null";
            }
            if (layers instanceof java.util.Collection) {
                java.util.Collection<?> c = (java.util.Collection<?>) layers;
                int nullCount = 0;
                String firstType = null;
                for (Object entry : c) {
                    if (entry == null) {
                        nullCount++;
                    } else if (firstType == null) {
                        firstType = entry.getClass().getName();
                    }
                }
                return "layersSize="
                        + c.size()
                        + " nullEntries="
                        + nullCount
                        + (firstType == null ? "" : " firstType=" + firstType);
            }
            return "layersType=" + layers.getClass().getName();
        } catch (Throwable t) {
            return "layersProbeErr=" + describeThrowableChain(t);
        }
    }

    private static String describeCustomEntitySpecTagState(Object spec) {
        if (spec == null) {
            return "spec=null";
        }
        try {
            Method getTags = findMethodRecursive(spec.getClass(), "getTags");
            if (getTags == null) {
                return "tagsMethod=missing";
            }
            getTags.setAccessible(true);
            Object tags = getTags.invoke(spec);
            if (tags == null) {
                return "tags=null";
            }
            if (tags instanceof java.util.Collection) {
                java.util.Collection<?> c = (java.util.Collection<?>) tags;
                int nullCount = 0;
                String firstType = null;
                for (Object entry : c) {
                    if (entry == null) {
                        nullCount++;
                    } else if (firstType == null) {
                        firstType = entry.getClass().getName();
                    }
                }
                return "tagsSize="
                        + c.size()
                        + " nullEntries="
                        + nullCount
                        + (firstType == null ? "" : " firstType=" + firstType);
            }
            return "tagsType=" + tags.getClass().getName();
        } catch (Throwable t) {
            return "tagsProbeErr=" + describeThrowableChain(t);
        }
    }

    private static String describeOrbitalJunkCustomEntitySpecState(Object spec) {
        return describeCustomEntitySpecLayerState(spec) + ", " + describeCustomEntitySpecTagState(spec);
    }

    private static boolean isUsableOrbitalJunkCustomEntitySpec(Object spec, Method readResolveSpec) {
        if (spec == null) {
            return false;
        }
        if (!hasUsableCustomEntitySpecLayers(spec) || !hasUsableCustomEntitySpecTags(spec)) {
            return false;
        }
        if (readResolveSpec != null) {
            try {
                Object resolved = readResolveSpec.invoke(spec);
                if (resolved != null && resolved != spec) {
                    spec = resolved;
                }
            } catch (Throwable t) {
                return false;
            }
        }
        return hasUsableCustomEntitySpecLayers(spec) && hasUsableCustomEntitySpecTags(spec);
    }

    private static String ensureOrbitalJunkCustomEntitySpecFallbackForClass(
            Class<?> specStoreClass, Class<?> customEntitySpecClass) {
        try {
            if (specStoreClass == null) {
                return "SpecStore class unavailable";
            }
            if (customEntitySpecClass == null) {
                return "custom entity spec class unavailable";
            }
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "custom entity register method unavailable";
            }
            registerSpec.setAccessible(true);

            Object specJson = buildOrbitalJunkSpecJsonForClass(customEntitySpecClass);
            if (specJson == null) {
                return "orbital_junk fallback json build failed";
            }
            Class<?> jsonObjectClass = specJson.getClass();

            Method cloneSpec = findMethodRecursive(customEntitySpecClass, "clone");
            Method setId = findMethodRecursive(customEntitySpecClass, "setId", String.class);
            Method loadSpec = findMethodRecursive(customEntitySpecClass, "load", jsonObjectClass);
            Method readResolveSpec = findMethodRecursive(customEntitySpecClass, "readResolve");
            if (cloneSpec != null) cloneSpec.setAccessible(true);
            if (setId != null) setId.setAccessible(true);
            if (loadSpec != null) loadSpec.setAccessible(true);
            if (readResolveSpec != null) readResolveSpec.setAccessible(true);

            Object existing = lookupSpecById(specStoreClass, customEntitySpecClass, "orbital_junk");
            if (existing != null) {
                Object hydrated = existing;
                if (setId != null) {
                    try {
                        setId.invoke(hydrated, "orbital_junk");
                    } catch (Throwable ignored) {
                    }
                }
                if (loadSpec != null) {
                    try {
                        loadSpec.invoke(hydrated, specJson);
                    } catch (Throwable ignored) {
                    }
                }
                if (readResolveSpec != null) {
                    try {
                        Object resolved = readResolveSpec.invoke(hydrated);
                        if (resolved != null) {
                            hydrated = resolved;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                if (isUsableOrbitalJunkCustomEntitySpec(hydrated, readResolveSpec)) {
                    if (!upsertSpecForClass(specStoreClass, customEntitySpecClass, registerSpec, "orbital_junk", hydrated)) {
                        return "orbital_junk fallback upsert failed for primary key";
                    }
                    upsertSpecForClass(specStoreClass, customEntitySpecClass, registerSpec, null, hydrated);
                    upsertSpecForClass(specStoreClass, customEntitySpecClass, registerSpec, "", hydrated);
                    return null;
                }
            }

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = customEntitySpecClass.getDeclaredConstructor(String.class, jsonObjectClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor = customEntitySpecClass.getConstructor(String.class, jsonObjectClass);
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "custom entity spec constructor unavailable for orbital_junk fallback";
            }
            specCtor.setAccessible(true);

            Object created = null;
            if (cloneSpec != null) {
                Object base = lookupSpecById(specStoreClass, customEntitySpecClass, "comm_relay");
                if (base == null) {
                    base = lookupSpecById(specStoreClass, customEntitySpecClass, "base_campaign_objective");
                }
                if (base != null) {
                    try {
                        created = cloneSpec.invoke(base);
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (created == null) {
                created = specCtor.newInstance("orbital_junk", specJson);
            }
            if (setId != null) {
                try {
                    setId.invoke(created, "orbital_junk");
                } catch (Throwable ignored) {
                }
            }
            if (loadSpec != null) {
                try {
                    loadSpec.invoke(created, specJson);
                } catch (Throwable ignored) {
                }
            }
            if (readResolveSpec != null) {
                try {
                    Object resolved = readResolveSpec.invoke(created);
                    if (resolved != null) {
                        created = resolved;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (!isUsableOrbitalJunkCustomEntitySpec(created, readResolveSpec)) {
                return "orbital_junk fallback created spec unusable: "
                        + describeOrbitalJunkCustomEntitySpecState(created);
            }

            if (!upsertSpecForClass(specStoreClass, customEntitySpecClass, registerSpec, "orbital_junk", created)) {
                return "orbital_junk fallback register failure for primary key";
            }
            upsertSpecForClass(specStoreClass, customEntitySpecClass, registerSpec, null, created);
            upsertSpecForClass(specStoreClass, customEntitySpecClass, registerSpec, "", created);

            Object verify = lookupSpecById(specStoreClass, customEntitySpecClass, "orbital_junk");
            if (verify == null) {
                return "orbital_junk fallback verify failed";
            }
            if (!isUsableOrbitalJunkCustomEntitySpec(verify, readResolveSpec)) {
                return "orbital_junk fallback verify unusable: "
                        + describeOrbitalJunkCustomEntitySpecState(verify);
            }
            return null;
        } catch (Throwable t) {
            return "orbital_junk fallback exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureOrbitalJunkCustomEntitySpecFallback() {
        return ensureOrbitalJunkCustomEntitySpecFallback(true);
    }

    private static String ensureOrbitalJunkCustomEntitySpecFallback(boolean verbose) {
        try {
            List<Class<?>> classCandidates = resolveCustomEntitySpecClassCandidates();
            if (classCandidates.isEmpty()) {
                return "custom entity spec class unavailable for orbital_junk fallback";
            }
            if (verbose) {
                System.out.println(
                        "Fixer: orbital_junk fallback class candidates="
                                + summarizeClassCandidates(classCandidates));
            }

            int repaired = 0;
            List<String> failures = new ArrayList<String>();
            for (Class<?> customEntitySpecClass : classCandidates) {
                if (customEntitySpecClass == null) {
                    continue;
                }
                Class<?> specStoreClass =
                        resolveSpecStoreClassForCustomEntityClass(customEntitySpecClass);
                String issue =
                        ensureOrbitalJunkCustomEntitySpecFallbackForClass(
                                specStoreClass, customEntitySpecClass);
                if (issue == null) {
                    repaired++;
                } else {
                    failures.add(
                            customEntitySpecClass.getName()
                                    + "@"
                                    + shortLoader(customEntitySpecClass.getClassLoader())
                                    + ":"
                                    + issue);
                }
            }
            if (repaired <= 0) {
                if (failures.isEmpty()) {
                    return "orbital_junk fallback failed for all class candidates";
                }
                int limit = Math.min(3, failures.size());
                return "orbital_junk fallback failed for all class candidates: "
                        + String.join(", ", failures.subList(0, limit))
                        + (failures.size() > limit ? " ... +" + (failures.size() - limit) : "");
            }
            if (verbose && !failures.isEmpty()) {
                int limit = Math.min(2, failures.size());
                System.out.println(
                        "Fixer: orbital_junk fallback partial class-candidate failures: "
                                + String.join(", ", failures.subList(0, limit))
                                + (failures.size() > limit ? " ... +" + (failures.size() - limit) : ""));
            }
            return null;
        } catch (Throwable t) {
            return "orbital_junk fallback exception: " + describeThrowableChain(t);
        }
    }

    private static String ensurePlanetSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> planetSpecClass = Class.forName("com.fs.starfarer.loading.specs.PlanetSpec");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "planet preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds = new String[] {"star_yellow", "arid", "gas_giant", "barren"};
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, planetSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            Object root = loadConfigJsonViaLoadingUtils("data/config/planets.json");
            if (root == null) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "planets.json unavailable, missing critical planet specs: "
                        + String.join(", ", missingBefore);
            }

            Class<?> jsonObjectClass = root.getClass();
            Method getNames = findMethodRecursive(jsonObjectClass, "getNames", jsonObjectClass);
            Method keys = findMethodRecursive(jsonObjectClass, "keys");
            Method getJSONObject = findMethodRecursive(jsonObjectClass, "getJSONObject", String.class);
            if (getJSONObject == null) {
                return "planet preflight JSON access methods unavailable";
            }
            getJSONObject.setAccessible(true);

            LinkedHashSet<String> names = new LinkedHashSet<String>();
            if (getNames != null) {
                try {
                    getNames.setAccessible(true);
                    Object namesObj = getNames.invoke(null, root);
                    if (namesObj instanceof String[]) {
                        String[] arr = (String[]) namesObj;
                        for (String id : arr) {
                            if (id != null && !id.trim().isEmpty()) {
                                names.add(id.trim());
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (names.isEmpty() && keys != null) {
                try {
                    keys.setAccessible(true);
                    Object itObj = keys.invoke(root);
                    if (itObj instanceof Iterator) {
                        Iterator it = (Iterator) itObj;
                        while (it.hasNext()) {
                            Object next = it.next();
                            if (next == null) {
                                continue;
                            }
                            String id = String.valueOf(next).trim();
                            if (!id.isEmpty()) {
                                names.add(id);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (names.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "planet key list unavailable, missing critical planet specs: "
                        + String.join(", ", missingBefore);
            }

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = planetSpecClass.getDeclaredConstructor(String.class, jsonObjectClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor =
                            planetSpecClass.getDeclaredConstructor(
                                    String.class, Class.forName("org.json.JSONObject"));
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "planet spec constructor unavailable";
            }
            specCtor.setAccessible(true);

            int inserted = 0;
            int alreadyPresent = 0;
            List<String> failed = new ArrayList<String>();

            for (String id : names) {
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, planetSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }
                Object specJson = getJSONObject.invoke(root, id);
                if (specJson == null) {
                    failed.add(id + "(json-null)");
                    continue;
                }
                try {
                    Object created = specCtor.newInstance(id, specJson);
                    registerSpec.invoke(null, planetSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, planetSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, planetSpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, planetSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: planet spec preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical planet specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "planet spec preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "planet spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String probeOrbitalJunkSettingsAvailability() {
        try {
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSettings = findMethodRecursive(globalClass, "getSettings");
            if (getSettings == null) {
                return "Global.getSettings unavailable";
            }
            getSettings.setAccessible(true);
            Object settings = null;
            try {
                settings = getSettings.invoke(null);
            } catch (Throwable ignored) {
            }
            if (settings == null) {
                return "Global.getSettings returned null";
            }
            Method getCustomEntitySpec =
                    findMethodRecursive(settings.getClass(), "getCustomEntitySpec", String.class);
            if (getCustomEntitySpec == null) {
                return "settings.getCustomEntitySpec unavailable";
            }
            getCustomEntitySpec.setAccessible(true);
            Object spec = getCustomEntitySpec.invoke(settings, "orbital_junk");
            if (spec == null) {
                return "settings orbital_junk spec is null";
            }
            if (!isUsableOrbitalJunkCustomEntitySpec(spec, null)) {
                return "settings orbital_junk spec unusable: "
                        + describeOrbitalJunkCustomEntitySpecState(spec)
                        + " class="
                        + spec.getClass().getName()
                        + "@"
                        + shortLoader(spec.getClass().getClassLoader());
            }
            return null;
        } catch (Throwable t) {
            return "settings probe exception: " + describeThrowableChain(t);
        }
    }

    private static String probeOrbitalJunkSpecStoreAvailability() {
        try {
            List<Class<?>> classCandidates = resolveCustomEntitySpecClassCandidates();
            if (classCandidates.isEmpty()) {
                return "custom entity spec class unavailable";
            }
            boolean anyUsable = false;
            List<String> states = new ArrayList<String>();
            for (Class<?> customEntitySpecClass : classCandidates) {
                if (customEntitySpecClass == null) {
                    continue;
                }
                Class<?> specStoreClass =
                        resolveSpecStoreClassForCustomEntityClass(customEntitySpecClass);
                if (specStoreClass == null) {
                    states.add(
                            customEntitySpecClass.getName()
                                    + "@"
                                    + shortLoader(customEntitySpecClass.getClassLoader())
                                    + ":specStore=null");
                    continue;
                }
                Object spec = lookupSpecById(specStoreClass, customEntitySpecClass, "orbital_junk");
                if (spec == null) {
                    Object nullAlias = null;
                    Object emptyAlias = null;
                    try {
                        nullAlias = lookupSpecById(specStoreClass, customEntitySpecClass, null);
                    } catch (Throwable ignored) {
                    }
                    try {
                        emptyAlias = lookupSpecById(specStoreClass, customEntitySpecClass, "");
                    } catch (Throwable ignored) {
                    }
                    states.add(
                            customEntitySpecClass.getName()
                                    + "@"
                                    + shortLoader(customEntitySpecClass.getClassLoader())
                                    + ":spec=null"
                                    + " nullKey="
                                    + describeOrbitalJunkCustomEntitySpecState(nullAlias)
                                    + " emptyKey="
                                    + describeOrbitalJunkCustomEntitySpecState(emptyAlias));
                    if (isUsableOrbitalJunkCustomEntitySpec(nullAlias, null)
                            || isUsableOrbitalJunkCustomEntitySpec(emptyAlias, null)) {
                        anyUsable = true;
                    }
                    continue;
                }
                String state =
                        customEntitySpecClass.getName()
                                + "@"
                                + shortLoader(customEntitySpecClass.getClassLoader())
                                + ":"
                                + describeOrbitalJunkCustomEntitySpecState(spec);
                states.add(state);
                if (isUsableOrbitalJunkCustomEntitySpec(spec, null)) {
                    anyUsable = true;
                }
            }
            if (anyUsable) {
                int limit = Math.min(4, states.size());
                return "resolved: "
                        + String.join(", ", states.subList(0, limit))
                        + (states.size() > limit ? " ... +" + (states.size() - limit) : "");
            }
            if (states.isEmpty()) {
                return "SpecStore probe had no class candidates";
            }
            int limit = Math.min(4, states.size());
            return "no usable orbital_junk spec across class candidates: "
                    + String.join(", ", states.subList(0, limit))
                    + (states.size() > limit ? " ... +" + (states.size() - limit) : "");
        } catch (Throwable t) {
            return "SpecStore probe exception: " + describeThrowableChain(t);
        }
    }

    private static String probeOrbitalJunkEntityInstantiation() {
        try {
            Class<?> entityClass = Class.forName("com.fs.starfarer.campaign.CustomCampaignEntity");
            java.lang.reflect.Constructor<?> ctor = null;
            for (java.lang.reflect.Constructor<?> candidate : entityClass.getDeclaredConstructors()) {
                if (candidate == null) {
                    continue;
                }
                Class<?>[] p = candidate.getParameterTypes();
                if (p == null
                        || p.length != 9
                        || p[0] != String.class
                        || p[1] != String.class
                        || p[2] != String.class
                        || p[3] != String.class
                        || p[4] != Float.TYPE
                        || p[5] != Float.TYPE
                        || p[6] != Float.TYPE) {
                    continue;
                }
                ctor = candidate;
                break;
            }
            if (ctor == null) {
                return "constructor unavailable";
            }
            ctor.setAccessible(true);
            String neutralCtorIssue = attemptOrbitalJunkEntityCtor(ctor, "neutral");
            if (neutralCtorIssue == null) {
                return null;
            }
            String playerCtorIssue = attemptOrbitalJunkEntityCtor(ctor, "player");
            return "neutralCtor="
                    + neutralCtorIssue
                    + " playerCtor="
                    + playerCtorIssue
                    + " neutral="
                    + describeRuntimeFactionState("neutral")
                    + " player="
                    + describeRuntimeFactionState("player")
                    + " noFaction="
                    + describeNoFactionState();
        } catch (Throwable t) {
            return "probe exception: " + describeThrowableChain(t);
        }
    }

    private static String attemptOrbitalJunkEntityCtor(
            java.lang.reflect.Constructor<?> ctor, String factionId) {
        if (ctor == null) {
            return "ctor-null";
        }
        try {
            Object probe =
                    ctor.newInstance(
                            "__fixer_orbital_probe__",
                            null,
                            "orbital_junk",
                            factionId,
                            -1f,
                            -1f,
                            -1f,
                            null,
                            null);
            if (probe == null) {
                return "constructor returned null probe";
            }
            Class<?> entityClass = probe.getClass();

            Method getSpec = findMethodRecursive(entityClass, "getSpec");
            Method getFaction = findMethodRecursive(entityClass, "getFaction");
            if (getSpec == null || getFaction == null) {
                return "probe reflection incomplete: getSpec="
                        + (getSpec == null ? "null" : "ok")
                        + " getFaction="
                        + (getFaction == null ? "null" : "ok");
            }
            getSpec.setAccessible(true);
            getFaction.setAccessible(true);

            Object spec = getSpec.invoke(probe);
            Object faction = getFaction.invoke(probe);
            if (!isUsableOrbitalJunkCustomEntitySpec(spec, null)) {
                return "probe spec unusable: "
                        + describeOrbitalJunkCustomEntitySpecState(spec)
                        + " faction="
                        + describeFactionState(faction);
            }
            String factionIssue = describeFactionIssue(faction);
            if (factionIssue != null) {
                return factionIssue;
            }
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static String describeFactionIssue(Object faction) {
        if (faction == null) {
            return "probe faction is null";
        }
        try {
            Method getSpec = findMethodRecursive(faction.getClass(), "getSpec");
            if (getSpec == null) {
                return null;
            }
            getSpec.setAccessible(true);
            Object spec = getSpec.invoke(faction);
            if (spec == null) {
                return "probe faction spec is null";
            }
            Method getColor = findMethodRecursive(spec.getClass(), "getColor");
            if (getColor != null) {
                getColor.setAccessible(true);
                Object color = getColor.invoke(spec);
                if (color == null) {
                    return "probe faction color is null";
                }
            }
            Method getSecondaryUIColor =
                    findMethodRecursive(spec.getClass(), "getSecondaryUIColor");
            if (getSecondaryUIColor != null) {
                getSecondaryUIColor.setAccessible(true);
                Object ui = getSecondaryUIColor.invoke(spec);
                if (ui == null) {
                    return "probe faction secondary UI color is null";
                }
            }
            return null;
        } catch (Throwable t) {
            return "probe faction inspection failed: " + describeThrowableChain(t);
        }
    }

    private static String describeFactionState(Object faction) {
        if (faction == null) {
            return "null";
        }
        String id = null;
        Object spec = null;
        try {
            Method getId = findMethodRecursive(faction.getClass(), "getId");
            if (getId != null) {
                getId.setAccessible(true);
                Object idObj = getId.invoke(faction);
                if (idObj != null) {
                    id = String.valueOf(idObj);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Method getSpec = findMethodRecursive(faction.getClass(), "getSpec");
            if (getSpec != null) {
                getSpec.setAccessible(true);
                spec = getSpec.invoke(faction);
            }
        } catch (Throwable ignored) {
        }
        return "id="
                + (id == null ? "?" : id)
                + " spec="
                + (spec == null
                        ? "null"
                        : (spec.getClass().getName()
                                + "@"
                                + shortLoader(spec.getClass().getClassLoader())));
    }

    private static String describeRuntimeFactionState(String factionId) {
        if (factionId == null || factionId.trim().isEmpty()) {
            return "id=empty";
        }
        String cleanId = factionId.trim();
        try {
            Class<?> campaignEngineClass = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
            Method getInstance = findMethodRecursive(campaignEngineClass, "getInstance");
            if (getInstance == null) {
                return "id=" + cleanId + " engine=getInstance-missing";
            }
            getInstance.setAccessible(true);
            Object engine = getInstance.invoke(null);
            if (engine == null) {
                return "id=" + cleanId + " engine=null";
            }
            Method getFaction = findMethodRecursive(campaignEngineClass, "getFaction", String.class);
            if (getFaction == null) {
                return "id=" + cleanId + " getFaction-missing";
            }
            getFaction.setAccessible(true);
            Object faction = getFaction.invoke(engine, cleanId);
            String issue = describeFactionIssue(faction);
            return "id="
                    + cleanId
                    + " "
                    + describeFactionState(faction)
                    + (issue == null ? "" : " issue=" + issue);
        } catch (Throwable t) {
            return "id=" + cleanId + " err=" + describeThrowableChain(t);
        }
    }

    private static String describeNoFactionState() {
        try {
            Class<?> factionClass = Class.forName("com.fs.starfarer.campaign.Faction");
            Field noFactionField = findFieldRecursive(factionClass, "NO_FACTION");
            if (noFactionField == null) {
                return "NO_FACTION field missing";
            }
            noFactionField.setAccessible(true);
            Object noFaction = noFactionField.get(null);
            return describeFactionState(noFaction);
        } catch (Throwable t) {
            return "err=" + describeThrowableChain(t);
        }
    }

    private static String ensureTerrainSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> terrainSpecClass = Class.forName("com.fs.starfarer.loading.specs.Stringsuper");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "terrain preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds =
                    new String[] {
                        "corona", "nebula", "ring", "asteroid_belt", "asteroid_field", "hyperspace"
                    };
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, terrainSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            Object root = loadConfigJsonViaLoadingUtils("data/campaign/terrain.json");
            if (root == null) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "terrain.json unavailable, missing critical terrain specs: "
                        + String.join(", ", missingBefore);
            }

            Class<?> jsonObjectClass = root.getClass();
            Method getNames = findMethodRecursive(jsonObjectClass, "getNames", jsonObjectClass);
            Method keys = findMethodRecursive(jsonObjectClass, "keys");
            Method getJSONObject = findMethodRecursive(jsonObjectClass, "getJSONObject", String.class);
            if (getJSONObject == null) {
                return "terrain preflight JSON access methods unavailable";
            }
            getJSONObject.setAccessible(true);

            LinkedHashSet<String> names = new LinkedHashSet<String>();
            if (getNames != null) {
                try {
                    getNames.setAccessible(true);
                    Object namesObj = getNames.invoke(null, root);
                    if (namesObj instanceof String[]) {
                        String[] arr = (String[]) namesObj;
                        for (String id : arr) {
                            if (id != null && !id.trim().isEmpty()) {
                                names.add(id.trim());
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (names.isEmpty() && keys != null) {
                try {
                    keys.setAccessible(true);
                    Object itObj = keys.invoke(root);
                    if (itObj instanceof Iterator) {
                        Iterator it = (Iterator) itObj;
                        while (it.hasNext()) {
                            Object next = it.next();
                            if (next == null) {
                                continue;
                            }
                            String id = String.valueOf(next).trim();
                            if (!id.isEmpty()) {
                                names.add(id);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (names.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "terrain key list unavailable, missing critical terrain specs: "
                        + String.join(", ", missingBefore);
            }

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = terrainSpecClass.getDeclaredConstructor(String.class, jsonObjectClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor =
                            terrainSpecClass.getDeclaredConstructor(
                                    String.class, Class.forName("org.json.JSONObject"));
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "terrain spec constructor unavailable";
            }
            specCtor.setAccessible(true);

            int inserted = 0;
            int alreadyPresent = 0;
            List<String> failed = new ArrayList<String>();

            for (String id : names) {
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, terrainSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }
                Object specJson = getJSONObject.invoke(root, id);
                if (specJson == null) {
                    failed.add(id + "(json-null)");
                    continue;
                }
                try {
                    Object created = specCtor.newInstance(id, specJson);
                    registerSpec.invoke(null, terrainSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, terrainSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, terrainSpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, terrainSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: terrain spec preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical terrain specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "terrain spec preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "terrain spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureSalvageEntityGenSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> salvageSpecClass =
                    Class.forName("com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "salvage spec preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds =
                    new String[] {"derelict_probe", "derelict_survey_ship", "derelict_mothership"};
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, salvageSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText =
                    readResourceTextForRepair("data/campaign/procgen/salvage_entity_gen_data.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "salvage_entity_gen_data.csv unavailable, missing critical salvage specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "salvage_entity_gen_data.csv parse returned no rows, missing critical salvage specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id") || !headerIndex.containsKey("stationrole")) {
                return "salvage csv missing required headers (id/stationRole)";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "salvage json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = salvageSpecClass.getDeclaredConstructor(jsonClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor =
                            salvageSpecClass.getDeclaredConstructor(
                                    Class.forName("org.json.JSONObject"));
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "salvage spec constructor unavailable";
            }
            specCtor.setAccessible(true);

            int inserted = 0;
            int alreadyPresent = 0;
            int skipped = 0;
            List<String> failed = new ArrayList<String>();

            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }
                String id = getCsvValueForRepair(row, headerIndex, "id");
                if (id == null || id.trim().isEmpty()) {
                    skipped++;
                    continue;
                }
                id = id.trim();
                if (id.startsWith("#")) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, salvageSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                try {
                    Object rowJson = jsonCtor.newInstance();
                    jsonPut.invoke(rowJson, "id", id);

                    String stationRole = getCsvValueForRepair(row, headerIndex, "stationrole");
                    if (stationRole == null) {
                        stationRole = "";
                    }
                    jsonPut.invoke(rowJson, "stationRole", stationRole);

                    addCsvStringToJsonForRepair(rowJson, jsonPut, "name", row, headerIndex, "name");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "type", row, headerIndex, "type");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "tags", row, headerIndex, "tags");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "drop_value", row, headerIndex, "drop_value");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "drop_random", row, headerIndex, "drop_random");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "defFaction", row, headerIndex, "deffaction");

                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "rating", row, headerIndex, "rating");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "detection_range", row, headerIndex, "detection_range");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "xpDiscover", row, headerIndex, "xpdiscover");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "xpSalvage", row, headerIndex, "xpsalvage");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "radius", row, headerIndex, "radius");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "defQuality", row, headerIndex, "defquality");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "probDefenders", row, headerIndex, "probdefenders");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minStr", row, headerIndex, "minstr");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxStr", row, headerIndex, "maxstr");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minSize", row, headerIndex, "minsize");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxSize", row, headerIndex, "maxsize");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "probStation", row, headerIndex, "probstation");

                    Object created = specCtor.newInstance(rowJson);
                    registerSpec.invoke(null, salvageSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, salvageSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, salvageSpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, salvageSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: salvage spec preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical salvage specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "salvage spec preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "salvage spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static void addCsvStringToJsonForRepair(
            Object rowJson,
            Method jsonPut,
            String jsonKey,
            List<String> row,
            Map<String, Integer> headerIndex,
            String csvHeader) {
        if (rowJson == null || jsonPut == null || jsonKey == null || csvHeader == null) {
            return;
        }
        try {
            String value = getCsvValueForRepair(row, headerIndex, csvHeader);
            if (value == null) {
                return;
            }
            jsonPut.invoke(rowJson, jsonKey, value);
        } catch (Throwable ignored) {
        }
    }

    private static void addCsvDoubleToJsonForRepair(
            Object rowJson,
            Method jsonPut,
            String jsonKey,
            List<String> row,
            Map<String, Integer> headerIndex,
            String csvHeader) {
        if (rowJson == null || jsonPut == null || jsonKey == null || csvHeader == null) {
            return;
        }
        try {
            Double value = parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, csvHeader));
            if (value == null) {
                return;
            }
            jsonPut.invoke(rowJson, jsonKey, value);
        } catch (Throwable ignored) {
        }
    }

    private static String ensureMarketConditionSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> marketConditionSpecClass = Class.forName("com.fs.starfarer.loading.T");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "market condition preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds =
                    new String[] {
                        "arid", "desert", "terran", "population_5", "urbanized_polity", "uninhabitable"
                    };
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, marketConditionSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText = readResourceTextForRepair("data/campaign/market_conditions.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "market_conditions.csv unavailable, missing critical market condition specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "market_conditions.csv parse returned no rows, missing critical market condition specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id")
                    || !headerIndex.containsKey("name")
                    || !headerIndex.containsKey("script")
                    || !headerIndex.containsKey("icon")) {
                return "market condition csv missing required headers (id/name/script/icon)";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "market condition json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor =
                    marketConditionSpecClass.getDeclaredConstructor(jsonClass);
            specCtor.setAccessible(true);

            int inserted = 0;
            int alreadyPresent = 0;
            int skipped = 0;
            List<String> failed = new ArrayList<String>();

            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }

                String id = getCsvValueForRepair(row, headerIndex, "id");
                if (id == null || id.length() == 0) {
                    skipped++;
                    continue;
                }
                if (id.startsWith("#")) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, marketConditionSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                String name = getCsvValueForRepair(row, headerIndex, "name");
                if (name == null || name.length() == 0) {
                    name = id;
                }
                String script = getCsvValueForRepair(row, headerIndex, "script");
                String icon = getCsvValueForRepair(row, headerIndex, "icon");
                if (script == null || script.length() == 0 || icon == null || icon.length() == 0) {
                    skipped++;
                    if (isCriticalIdForRepair(criticalIds, id)) {
                        failed.add(id + "(missing-script-or-icon)");
                    }
                    continue;
                }

                try {
                    Object specJson = jsonCtor.newInstance();
                    jsonPut.invoke(specJson, "id", id);
                    jsonPut.invoke(specJson, "name", name);
                    jsonPut.invoke(specJson, "script", script);
                    jsonPut.invoke(specJson, "icon", icon);

                    String desc = getCsvValueForRepair(row, headerIndex, "desc");
                    if (desc != null && desc.length() > 0) {
                        jsonPut.invoke(specJson, "desc", desc);
                    }
                    String tags = getCsvValueForRepair(row, headerIndex, "tags");
                    if (tags != null && tags.length() > 0) {
                        jsonPut.invoke(specJson, "tags", tags);
                    }

                    Boolean planetary =
                            parseCsvBooleanForRepair(getCsvValueForRepair(row, headerIndex, "planetary"));
                    if (planetary != null) {
                        jsonPut.invoke(specJson, "planetary", planetary);
                    }
                    Boolean decivRemove =
                            parseCsvBooleanForRepair(
                                    getCsvValueForRepair(row, headerIndex, "decivremove"));
                    if (decivRemove != null) {
                        jsonPut.invoke(specJson, "decivRemove", decivRemove);
                    }
                    Double order =
                            parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, "order"));
                    if (order != null) {
                        jsonPut.invoke(specJson, "order", order);
                    }

                    Object created = specCtor.newInstance(specJson);
                    registerSpec.invoke(null, marketConditionSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, marketConditionSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, marketConditionSpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, marketConditionSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: market condition preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical market condition specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "market condition preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "market condition preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureIndustrySpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> industrySpecClass = Class.forName("com.fs.starfarer.loading.specs.H");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "industry preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds =
                    new String[] {"population", "spaceport", "farming", "lightindustry", "heavyindustry"};
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, industrySpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText = readResourceTextForRepair("data/campaign/industries.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "industries.csv unavailable, missing critical industry specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "industries.csv parse returned no rows, missing critical industry specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id")
                    || !headerIndex.containsKey("name")
                    || !headerIndex.containsKey("plugin")) {
                return "industry csv missing required headers (id/name/plugin)";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "industry json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor =
                    industrySpecClass.getDeclaredConstructor(jsonClass);
            specCtor.setAccessible(true);

            int inserted = 0;
            int alreadyPresent = 0;
            int skipped = 0;
            List<String> failed = new ArrayList<String>();

            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }
                String id = getCsvValueForRepair(row, headerIndex, "id");
                if (id == null || id.length() == 0) {
                    skipped++;
                    continue;
                }
                if (id.startsWith("#")) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, industrySpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                String name = getCsvValueForRepair(row, headerIndex, "name");
                if (name == null || name.length() == 0) {
                    name = id;
                }
                String plugin = getCsvValueForRepair(row, headerIndex, "plugin");
                if (plugin == null || plugin.length() == 0) {
                    skipped++;
                    if (isCriticalIdForRepair(criticalIds, id)) {
                        failed.add(id + "(missing-plugin)");
                    }
                    continue;
                }

                try {
                    Object specJson = jsonCtor.newInstance();
                    jsonPut.invoke(specJson, "id", id);
                    jsonPut.invoke(specJson, "name", name);
                    jsonPut.invoke(specJson, "plugin", plugin);

                    String image = getCsvValueForRepair(row, headerIndex, "image");
                    if (image != null && image.length() > 0) {
                        jsonPut.invoke(specJson, "image", image);
                    }
                    String desc = getCsvValueForRepair(row, headerIndex, "desc");
                    if (desc != null && desc.length() > 0) {
                        jsonPut.invoke(specJson, "desc", desc);
                    }
                    String tags = getCsvValueForRepair(row, headerIndex, "tags");
                    if (tags != null && tags.length() > 0) {
                        jsonPut.invoke(specJson, "tags", tags);
                    }
                    String downgrade = getCsvValueForRepair(row, headerIndex, "downgrade");
                    if (downgrade != null && downgrade.length() > 0) {
                        jsonPut.invoke(specJson, "downgrade", downgrade);
                    }
                    String upgrade = getCsvValueForRepair(row, headerIndex, "upgrade");
                    if (upgrade != null && upgrade.length() > 0) {
                        jsonPut.invoke(specJson, "upgrade", upgrade);
                    }
                    String data = getCsvValueForRepair(row, headerIndex, "data");
                    if (data != null && data.length() > 0) {
                        jsonPut.invoke(specJson, "data", data);
                    }
                    String disruptDanger = getCsvValueForRepair(row, headerIndex, "disruptdanger");
                    if (disruptDanger != null && disruptDanger.length() > 0) {
                        jsonPut.invoke(specJson, "disruptDanger", disruptDanger);
                    }

                    Double costMult =
                            parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, "cost mult"));
                    if (costMult != null) {
                        jsonPut.invoke(specJson, "cost mult", costMult);
                    }
                    Double buildTime =
                            parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, "build time"));
                    if (buildTime != null) {
                        jsonPut.invoke(specJson, "build time", buildTime);
                    }
                    Double income =
                            parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, "income"));
                    if (income != null) {
                        jsonPut.invoke(specJson, "income", income);
                    }
                    Double upkeep =
                            parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, "upkeep"));
                    if (upkeep != null) {
                        jsonPut.invoke(specJson, "upkeep", upkeep);
                    }
                    Integer order = parseCsvIntForRepair(getCsvValueForRepair(row, headerIndex, "order"));
                    if (order != null) {
                        jsonPut.invoke(specJson, "order", order);
                    }

                    Object created = specCtor.newInstance(specJson);
                    registerSpec.invoke(null, industrySpecClass, id, created);
                    if (lookupSpecById(specStoreClass, industrySpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, industrySpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, industrySpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: industry preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical industry specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "industry preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "industry preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureCommoditySpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> commoditySpecClass = Class.forName("com.fs.starfarer.loading.F");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "commodity preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds =
                    new String[] {
                        "domestic_goods",
                        "food",
                        "supplies",
                        "drugs",
                        "fuel",
                        "metals",
                        "heavy_machinery",
                        "crew",
                        "marines"
                    };
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, commoditySpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText = readResourceTextForRepair("data/campaign/commodities.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "commodities.csv unavailable, missing critical commodity specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "commodities.csv parse returned no rows, missing critical commodity specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id")
                    || !headerIndex.containsKey("name")
                    || !headerIndex.containsKey("base price")) {
                return "commodity csv missing required headers (id/name/base price)";
            }

            java.lang.reflect.Constructor<?> specCtor =
                    commoditySpecClass.getDeclaredConstructor(String.class, String.class, Float.TYPE);
            specCtor.setAccessible(true);

            Method setDemandClass = findMethodRecursive(commoditySpecClass, "setDemandClass", String.class);
            Method setExportValue =
                    findMethodRecursive(commoditySpecClass, "setExportValue", Float.TYPE);
            Method setOrder = findMethodRecursive(commoditySpecClass, "setOrder", Float.TYPE);
            Method setOrigin = findMethodRecursive(commoditySpecClass, "setOrigin", String.class);
            Method addTag = findMethodRecursive(commoditySpecClass, "addTag", String.class);
            Method setStackSize =
                    findMethodRecursive(commoditySpecClass, "setStackSize", Integer.TYPE);
            Method setCargoSpace =
                    findMethodRecursive(commoditySpecClass, "setCargoSpace", Float.TYPE);
            Method setIconName =
                    findMethodRecursive(commoditySpecClass, "setIconName", String.class);
            Method setIconLargeName =
                    findMethodRecursive(commoditySpecClass, "setIconLargeName", String.class);
            Method setEconomyTier =
                    findMethodRecursive(commoditySpecClass, "setEconomyTier", Float.TYPE);
            Method setEconUnit = findMethodRecursive(commoditySpecClass, "setEconUnit", Float.TYPE);
            if (setDemandClass != null) setDemandClass.setAccessible(true);
            if (setExportValue != null) setExportValue.setAccessible(true);
            if (setOrder != null) setOrder.setAccessible(true);
            if (setOrigin != null) setOrigin.setAccessible(true);
            if (addTag != null) addTag.setAccessible(true);
            if (setStackSize != null) setStackSize.setAccessible(true);
            if (setCargoSpace != null) setCargoSpace.setAccessible(true);
            if (setIconName != null) setIconName.setAccessible(true);
            if (setIconLargeName != null) setIconLargeName.setAccessible(true);
            if (setEconomyTier != null) setEconomyTier.setAccessible(true);
            if (setEconUnit != null) setEconUnit.setAccessible(true);

            int inserted = 0;
            int alreadyPresent = 0;
            int skipped = 0;
            List<String> failed = new ArrayList<String>();

            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }
                String id = getCsvValueForRepair(row, headerIndex, "id");
                if (id == null || id.length() == 0) {
                    skipped++;
                    continue;
                }
                if (id.startsWith("#")) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, commoditySpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                String name = getCsvValueForRepair(row, headerIndex, "name");
                if (name == null || name.length() == 0) {
                    name = id;
                }
                Float basePrice =
                        parseCsvFloatForRepair(getCsvValueForRepair(row, headerIndex, "base price"));
                if (basePrice == null) {
                    basePrice = Float.valueOf(100f);
                }

                try {
                    Object created = specCtor.newInstance(id, name, basePrice.floatValue());
                    String demandClass = getCsvValueForRepair(row, headerIndex, "demand class");
                    if (setDemandClass != null && demandClass != null && demandClass.length() > 0) {
                        setDemandClass.invoke(created, demandClass);
                    }
                    Float exportValue =
                            parseCsvFloatForRepair(getCsvValueForRepair(row, headerIndex, "export value"));
                    if (setExportValue != null && exportValue != null) {
                        setExportValue.invoke(created, exportValue.floatValue());
                    }
                    Float order = parseCsvFloatForRepair(getCsvValueForRepair(row, headerIndex, "order"));
                    if (setOrder != null && order != null) {
                        setOrder.invoke(created, order.floatValue());
                    }
                    String origin = getCsvValueForRepair(row, headerIndex, "origin");
                    if (setOrigin != null && origin != null && origin.length() > 0) {
                        setOrigin.invoke(created, origin);
                    }
                    Integer stackSize =
                            parseCsvIntForRepair(getCsvValueForRepair(row, headerIndex, "stack size"));
                    if (setStackSize != null && stackSize != null) {
                        setStackSize.invoke(created, stackSize.intValue());
                    }
                    Float cargoSpace =
                            parseCsvFloatForRepair(getCsvValueForRepair(row, headerIndex, "cargo space"));
                    if (setCargoSpace != null && cargoSpace != null) {
                        setCargoSpace.invoke(created, cargoSpace.floatValue());
                    }
                    String icon = getCsvValueForRepair(row, headerIndex, "icon");
                    if (setIconName != null && icon != null && icon.length() > 0) {
                        setIconName.invoke(created, icon);
                    }
                    if (setIconLargeName != null && icon != null && icon.length() > 0) {
                        setIconLargeName.invoke(created, icon);
                    }
                    Float economyTier =
                            parseCsvFloatForRepair(getCsvValueForRepair(row, headerIndex, "economytier"));
                    if (setEconomyTier != null && economyTier != null) {
                        setEconomyTier.invoke(created, economyTier.floatValue());
                    }
                    Float econUnit = parseCsvFloatForRepair(getCsvValueForRepair(row, headerIndex, "econunit"));
                    if (setEconUnit != null && econUnit != null) {
                        setEconUnit.invoke(created, econUnit.floatValue());
                    }
                    if (addTag != null) {
                        String tags = getCsvValueForRepair(row, headerIndex, "tags");
                        if (tags != null && tags.length() > 0) {
                            String[] parts = tags.split(",");
                            for (String part : parts) {
                                if (part == null) {
                                    continue;
                                }
                                String tag = part.trim();
                                if (!tag.isEmpty()) {
                                    addTag.invoke(created, tag);
                                }
                            }
                        }
                    }

                    registerSpec.invoke(null, commoditySpecClass, id, created);
                    if (lookupSpecById(specStoreClass, commoditySpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, commoditySpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, commoditySpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: commodity preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical commodity specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "commodity preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "commodity preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureSubmarketSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> submarketSpecClass = Class.forName("com.fs.starfarer.loading.for");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "submarket preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds =
                    new String[] {
                        "open_market",
                        "black_market",
                        "storage",
                        "generic_military",
                        "local_resources"
                    };
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, submarketSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText = readResourceTextForRepair("data/campaign/submarkets.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "submarkets.csv unavailable, missing critical submarket specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "submarkets.csv parse returned no rows, missing critical submarket specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id")
                    || !headerIndex.containsKey("name")
                    || !headerIndex.containsKey("script")
                    || !headerIndex.containsKey("order")) {
                return "submarket csv missing required headers (id/name/script/order)";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "submarket json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor =
                    submarketSpecClass.getDeclaredConstructor(jsonClass);
            specCtor.setAccessible(true);

            int inserted = 0;
            int alreadyPresent = 0;
            int skipped = 0;
            List<String> failed = new ArrayList<String>();

            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }
                String id = getCsvValueForRepair(row, headerIndex, "id");
                if (id == null || id.length() == 0) {
                    skipped++;
                    continue;
                }
                if (id.startsWith("#")) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, submarketSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                String name = getCsvValueForRepair(row, headerIndex, "name");
                if (name == null || name.length() == 0) {
                    name = id;
                }
                String desc = getCsvValueForRepair(row, headerIndex, "desc");
                if (desc == null) {
                    desc = "";
                }
                String icon = getCsvValueForRepair(row, headerIndex, "icon");
                if (icon == null) {
                    icon = "";
                }
                String faction = getCsvValueForRepair(row, headerIndex, "faction");
                if (faction == null) {
                    faction = "";
                }
                Double order = parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, "order"));
                if (order == null) {
                    order = Double.valueOf(0d);
                }

                String script = getCsvValueForRepair(row, headerIndex, "script");
                if (script == null || script.length() == 0) {
                    script = inferSubmarketPluginClassForRepair(id);
                }
                if (script == null || script.length() == 0) {
                    skipped++;
                    if (isCriticalIdForRepair(criticalIds, id)) {
                        failed.add(id + "(missing-script)");
                    }
                    continue;
                }

                try {
                    Object specJson = jsonCtor.newInstance();
                    jsonPut.invoke(specJson, "id", id);
                    jsonPut.invoke(specJson, "name", name);
                    jsonPut.invoke(specJson, "desc", desc);
                    jsonPut.invoke(specJson, "icon", icon);
                    jsonPut.invoke(specJson, "faction", faction);
                    jsonPut.invoke(specJson, "order", order.doubleValue());
                    jsonPut.invoke(specJson, "script", script);

                    Object created = specCtor.newInstance(specJson);
                    registerSpec.invoke(null, submarketSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, submarketSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, submarketSpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            List<String> missingAfter = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, submarketSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: submarket preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical submarket specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "submarket preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "submarket preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String inferSubmarketPluginClassForRepair(String id) {
        if (id == null) {
            return null;
        }
        if ("open_market".equals(id)) {
            return "com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin";
        }
        if ("black_market".equals(id)) {
            return "com.fs.starfarer.api.impl.campaign.submarkets.BlackMarketPlugin";
        }
        if ("storage".equals(id)) {
            return "com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin";
        }
        if ("generic_military".equals(id)) {
            return "com.fs.starfarer.api.impl.campaign.submarkets.MilitarySubmarketPlugin";
        }
        if ("local_resources".equals(id)) {
            return "com.fs.starfarer.api.impl.campaign.submarkets.LocalResourcesSubmarketPlugin";
        }
        return null;
    }

    private static String ensureStarSystemLocationMapReadyForDirectNewGame() {
        try {
            Class<?> starMapClass = Class.forName("com.fs.starfarer.loading.supersuper");
            Method lookupMethod = findMethodRecursive(starMapClass, "new", String.class);
            Method loadMethod = findMethodRecursive(starMapClass, "o00000", String.class);
            if (lookupMethod == null || loadMethod == null) {
                return "starmap loader methods unavailable";
            }
            lookupMethod.setAccessible(true);
            loadMethod.setAccessible(true);

            String probeKey = "corvus";
            if (isStarMapLookupReady(lookupMethod, probeKey)) {
                return null;
            }

            LinkedHashSet<String> candidates = new LinkedHashSet<String>();
            candidates.add("data/campaign/starmap.json");
            candidates.add("data/campaign/econ/starmap.json");

            Object economyJson = loadConfigJsonViaLoadingUtils("data/campaign/econ/economy.json");
            String mapRef = optJsonStringForRepair(economyJson, "map", null);
            String resolvedFromEconomy =
                    resolveRelativeResourcePathForRepair("data/campaign/econ", mapRef);
            if (resolvedFromEconomy != null && resolvedFromEconomy.length() > 0) {
                candidates.add(resolvedFromEconomy);
            }

            List<String> loadErrors = new ArrayList<String>();
            for (String path : candidates) {
                if (path == null || path.trim().isEmpty()) {
                    continue;
                }
                String cleanPath = path.trim().replace('\\', '/');
                InputStream probe = null;
                try {
                    probe = openResourceStream(cleanPath);
                    if (probe == null) {
                        continue;
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (probe != null) {
                        try {
                            probe.close();
                        } catch (Throwable ignored) {
                        }
                    }
                }

                try {
                    loadMethod.invoke(null, cleanPath);
                    if (isStarMapLookupReady(lookupMethod, probeKey)) {
                        System.out.println(
                                "Fixer: starmap preflight initialized from " + cleanPath + ".");
                        return null;
                    }
                } catch (Throwable t) {
                    loadErrors.add(cleanPath + "(" + describeThrowableChain(t) + ")");
                }
            }

            if (!loadErrors.isEmpty()) {
                return "starmap init failures: " + String.join(", ", loadErrors);
            }
            return "starmap not initialized; tried " + String.join(", ", candidates);
        } catch (Throwable t) {
            return "starmap preflight exception: " + describeThrowableChain(t);
        }
    }

    private static boolean isStarMapLookupReady(Method lookupMethod, String probeKey) {
        if (lookupMethod == null) {
            return false;
        }
        String key = probeKey == null || probeKey.trim().isEmpty() ? "corvus" : probeKey.trim();
        try {
            lookupMethod.invoke(null, key);
            return true;
        } catch (Throwable t) {
            String chain = describeThrowableChain(t);
            String lower = chain == null ? "" : chain.toLowerCase();
            if (lower.indexOf("nullpointerexception") >= 0) {
                return false;
            }
            return true;
        }
    }

    private static String resolveRelativeResourcePathForRepair(String baseDir, String refPath) {
        if (refPath == null) {
            return null;
        }
        String ref = refPath.trim().replace('\\', '/');
        if (ref.length() == 0) {
            return null;
        }
        if (ref.startsWith("data/")) {
            return ref;
        }
        String base = baseDir == null ? "" : baseDir.trim().replace('\\', '/');
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String combined = (base.length() == 0 ? ref : (base + "/" + ref)).replace('\\', '/');
        while (combined.contains("//")) {
            combined = combined.replace("//", "/");
        }
        String[] parts = combined.split("/");
        ArrayList<String> stack = new ArrayList<String>();
        for (String part : parts) {
            if (part == null || part.length() == 0 || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
                continue;
            }
            stack.add(part);
        }
        if (stack.isEmpty()) {
            return null;
        }
        return String.join("/", stack);
    }

    private static Object loadConfigJsonViaLoadingUtils(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        try {
            Class<?> loadingUtilsClass = Class.forName("com.fs.starfarer.loading.LoadingUtils");
            Method jsonLoader = null;
            for (Method m : loadingUtilsClass.getDeclaredMethods()) {
                if (m == null || !Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 1 || params[0] != String.class) {
                    continue;
                }
                Class<?> ret = m.getReturnType();
                if (ret != null && "org.json.JSONObject".equals(ret.getName())) {
                    jsonLoader = m;
                    break;
                }
            }
            if (jsonLoader == null) {
                return null;
            }
            jsonLoader.setAccessible(true);
            return jsonLoader.invoke(null, path);
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: failed LoadingUtils JSON load for "
                            + path
                            + ": "
                            + describeThrowableChain(t));
            return null;
        }
    }

    private static String readResourceTextForRepair(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        InputStream in = null;
        try {
            in = openResourceStream(path.trim().replace('\\', '/'));
            if (in == null) {
                return null;
            }
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = in.read(buf)) >= 0) {
                if (n == 0) {
                    continue;
                }
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: failed to read resource text for " + path + ": " + describeThrowableChain(t));
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static List<List<String>> parseCsvRowsForRepair(String text) {
        ArrayList<List<String>> rows = new ArrayList<List<String>>();
        if (text == null || text.length() == 0) {
            return rows;
        }

        ArrayList<String> row = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < len && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (c == ',' && !inQuotes) {
                row.add(field.toString());
                field.setLength(0);
                continue;
            }
            if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < len && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                if (!isCsvRowBlankForRepair(row)) {
                    rows.add(row);
                }
                row = new ArrayList<String>();
                continue;
            }
            field.append(c);
        }
        row.add(field.toString());
        if (!isCsvRowBlankForRepair(row)) {
            rows.add(row);
        }

        if (!rows.isEmpty() && !rows.get(0).isEmpty()) {
            String header0 = rows.get(0).get(0);
            if (header0 != null && header0.length() > 0 && header0.charAt(0) == '\uFEFF') {
                rows.get(0).set(0, header0.substring(1));
            }
        }
        return rows;
    }

    private static boolean isCsvRowBlankForRepair(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String cell : row) {
            if (cell != null && cell.trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> buildCsvHeaderIndexForRepair(List<String> headerRow) {
        LinkedHashMap<String, Integer> index = new LinkedHashMap<String, Integer>();
        if (headerRow == null) {
            return index;
        }
        for (int i = 0; i < headerRow.size(); i++) {
            String raw = headerRow.get(i);
            if (raw == null) {
                continue;
            }
            String key = raw.trim().toLowerCase();
            if (key.length() == 0) {
                continue;
            }
            index.put(key, Integer.valueOf(i));
        }
        return index;
    }

    private static String getCsvValueForRepair(
            List<String> row, Map<String, Integer> headerIndex, String header) {
        if (row == null || headerIndex == null || header == null) {
            return null;
        }
        Integer idx = headerIndex.get(header.trim().toLowerCase());
        if (idx == null) {
            return null;
        }
        int i = idx.intValue();
        if (i < 0 || i >= row.size()) {
            return null;
        }
        String value = row.get(i);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static Boolean parseCsvBooleanForRepair(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim().toLowerCase();
        if (s.length() == 0) {
            return null;
        }
        if ("true".equals(s) || "yes".equals(s) || "1".equals(s)) {
            return Boolean.TRUE;
        }
        if ("false".equals(s) || "no".equals(s) || "0".equals(s)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static Double parseCsvDoubleForRepair(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.length() == 0) {
            return null;
        }
        try {
            return Double.valueOf(Double.parseDouble(s));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer parseCsvIntForRepair(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(s));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Float parseCsvFloatForRepair(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.length() == 0) {
            return null;
        }
        try {
            return Float.valueOf(Float.parseFloat(s));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isCriticalIdForRepair(String[] ids, String target) {
        if (ids == null || target == null) {
            return false;
        }
        for (String id : ids) {
            if (id != null && id.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static Object lookupSpecById(Class<?> specStoreClass, Class<?> specClass, String id)
            throws Exception {
        Method lookupMethod =
                findMethodRecursive(specStoreClass, "o00000", Class.class, String.class);
        if (lookupMethod != null) {
            lookupMethod.setAccessible(true);
            return lookupMethod.invoke(null, specClass, id);
        }

        lookupMethod =
                findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Boolean.TYPE);
        if (lookupMethod != null) {
            lookupMethod.setAccessible(true);
            return lookupMethod.invoke(null, specClass, id, Boolean.FALSE);
        }

        return null;
    }

    private static Object lookupSpecByIdAcrossClasses(
            Class<?> specStoreClass, List<Class<?>> specClasses, String id) throws Exception {
        if (specClasses == null || specClasses.isEmpty()) {
            return null;
        }
        for (Class<?> specClass : specClasses) {
            if (specClass == null) {
                continue;
            }
            Object found = lookupSpecById(specStoreClass, specClass, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean upsertSpecForClass(
            Class<?> specStoreClass,
            Class<?> specClass,
            Method registerSpecMethod,
            String id,
            Object spec) {
        if (specStoreClass == null || specClass == null || spec == null) {
            return false;
        }

        if (registerSpecMethod != null) {
            try {
                registerSpecMethod.setAccessible(true);
                registerSpecMethod.invoke(null, specClass, id, spec);
                return true;
            } catch (Throwable registerFailure) {
                String chain = describeThrowableChain(registerFailure);
                String lower = chain == null ? "" : chain.toLowerCase();
                if (lower.indexOf("already exists") < 0
                        && lower.indexOf("duplicate") < 0
                        && lower.indexOf("exists") < 0) {
                    return false;
                }
            }
        }

        try {
            Field classToSpecsField = findFieldRecursive(specStoreClass, "int");
            if (classToSpecsField != null) {
                classToSpecsField.setAccessible(true);
                Object classToSpecsObj = classToSpecsField.get(null);
                if (classToSpecsObj instanceof Map) {
                    Map classToSpecs = (Map) classToSpecsObj;
                    Object perClassObj = classToSpecs.get(specClass);
                    Map perClassMap;
                    if (perClassObj instanceof Map) {
                        perClassMap = (Map) perClassObj;
                    } else {
                        perClassMap = new LinkedHashMap();
                        classToSpecs.put(specClass, perClassMap);
                    }
                    perClassMap.put(id, spec);
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private static int countSpecsForClass(Class<?> specStoreClass, Class<?> specClass) {
        try {
            Method valuesMethod = findMethodRecursive(specStoreClass, "o00000", Class.class);
            if (valuesMethod == null) {
                return -1;
            }
            valuesMethod.setAccessible(true);
            Object values = valuesMethod.invoke(null, specClass);
            if (values instanceof java.util.Collection) {
                return ((java.util.Collection<?>) values).size();
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static void clearSpecsForClass(Class<?> specStoreClass, Class<?> specClass) {
        try {
            Method clearMethod = findMethodRecursive(specStoreClass, "Object", Class.class);
            if (clearMethod == null) {
                return;
            }
            clearMethod.setAccessible(true);
            clearMethod.invoke(null, specClass);
        } catch (Throwable t) {
            System.out.println("Fixer: unable to clear spec map for " + specClass.getName() + ": " + t);
        }
    }

    private static boolean isPendingColonyTargetResult(String result) {
        if (result == null || result.length() == 0) {
            return false;
        }
        String lower = result.toLowerCase();
        return lower.startsWith("pending:");
    }

    private static String tryPrimeColonyInteractionTarget() {
        try {
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSector = globalClass.getMethod("getSector");
            Object sector = getSector.invoke(null);
            if (sector == null) {
                return "pending:sector-null";
            }

            Method getEconomy = findMethodRecursive(sector.getClass(), "getEconomy");
            if (getEconomy == null) {
                return "Sector.getEconomy method not found";
            }
            getEconomy.setAccessible(true);
            Object economy = getEconomy.invoke(sector);
            if (economy == null) {
                return "pending:sector-economy-null";
            }

            Method getMarketsCopy = findMethodRecursive(economy.getClass(), "getMarketsCopy");
            if (getMarketsCopy == null) {
                return "Economy.getMarketsCopy method not found";
            }
            getMarketsCopy.setAccessible(true);
            Object marketsObj = getMarketsCopy.invoke(economy);
            if (!(marketsObj instanceof List)) {
                return "pending:markets-list-unavailable";
            }
            List markets = (List) marketsObj;
            if (markets.isEmpty()) {
                return "pending:no-markets-available";
            }

            String preferredName =
                    System.getProperty(AUTO_VISIT_COLONY_NAME_PROPERTY, "").trim().toLowerCase();
            Object selectedMarket = null;
            Object selectedEntity = null;
            for (Object market : markets) {
                if (market == null) {
                    continue;
                }
                Method isHidden = findMethodRecursive(market.getClass(), "isHidden");
                if (isHidden != null) {
                    isHidden.setAccessible(true);
                    Object hiddenObj = isHidden.invoke(market);
                    if (hiddenObj instanceof Boolean && ((Boolean) hiddenObj).booleanValue()) {
                        continue;
                    }
                }

                Method getPrimaryEntity = findMethodRecursive(market.getClass(), "getPrimaryEntity");
                if (getPrimaryEntity == null) {
                    continue;
                }
                getPrimaryEntity.setAccessible(true);
                Object entity = getPrimaryEntity.invoke(market);
                if (entity == null) {
                    continue;
                }

                Method getName = findMethodRecursive(market.getClass(), "getName");
                String marketName = null;
                if (getName != null) {
                    getName.setAccessible(true);
                    Object nameObj = getName.invoke(market);
                    if (nameObj != null) {
                        marketName = String.valueOf(nameObj);
                    }
                }

                if (!preferredName.isEmpty() && marketName != null) {
                    if (marketName.toLowerCase().indexOf(preferredName) >= 0) {
                        selectedMarket = market;
                        selectedEntity = entity;
                        break;
                    }
                }

                if (selectedMarket == null) {
                    selectedMarket = market;
                    selectedEntity = entity;
                }
            }

            if (selectedEntity == null || selectedMarket == null) {
                return "pending:no-usable-colony-market-entity";
            }

            Method getPlayerFleet = findMethodRecursive(sector.getClass(), "getPlayerFleet");
            if (getPlayerFleet == null) {
                return "Sector.getPlayerFleet method not found";
            }
            getPlayerFleet.setAccessible(true);
            Object playerFleet = getPlayerFleet.invoke(sector);
            if (playerFleet == null) {
                return "pending:player-fleet-null";
            }

            Method getContainingLocation =
                    findMethodRecursive(selectedEntity.getClass(), "getContainingLocation");
            Object targetLocation = null;
            if (getContainingLocation != null) {
                getContainingLocation.setAccessible(true);
                targetLocation = getContainingLocation.invoke(selectedEntity);
            }

            if (targetLocation != null) {
                Method setCurrentLocation = findSingleArgMethod(sector.getClass(), "setCurrentLocation");
                if (setCurrentLocation != null) {
                    setCurrentLocation.setAccessible(true);
                    setCurrentLocation.invoke(sector, targetLocation);
                }
                Method setContainingLocation =
                        findSingleArgMethod(playerFleet.getClass(), "setContainingLocation");
                if (setContainingLocation != null) {
                    setContainingLocation.setAccessible(true);
                    setContainingLocation.invoke(playerFleet, targetLocation);
                }
            }

            Method getLocation = findMethodRecursive(selectedEntity.getClass(), "getLocation");
            if (getLocation != null) {
                getLocation.setAccessible(true);
                Object loc = getLocation.invoke(selectedEntity);
                if (loc != null) {
                    float x = readFloatField(loc, "x", 0f);
                    float y = readFloatField(loc, "y", 0f);
                    float radius = 0f;
                    Method getRadius = findMethodRecursive(selectedEntity.getClass(), "getRadius");
                    if (getRadius != null) {
                        getRadius.setAccessible(true);
                        Object radiusObj = getRadius.invoke(selectedEntity);
                        if (radiusObj instanceof Number) {
                            radius = ((Number) radiusObj).floatValue();
                        }
                    }
                    float offset = Math.max(250f, radius * 1.5f);
                    Method setLocation =
                            findMethodRecursive(playerFleet.getClass(), "setLocation", Float.TYPE, Float.TYPE);
                    if (setLocation != null) {
                        setLocation.setAccessible(true);
                        setLocation.invoke(playerFleet, Float.valueOf(x + offset), Float.valueOf(y));
                    }
                }
            }

            Method setInteractionTarget = findSingleArgMethod(playerFleet.getClass(), "setInteractionTarget");
            if (setInteractionTarget != null) {
                setInteractionTarget.setAccessible(true);
                setInteractionTarget.invoke(playerFleet, selectedEntity);
            }

            Object campaignUI = null;
            Method getCampaignUI = findMethodRecursive(sector.getClass(), "getCampaignUI");
            if (getCampaignUI != null) {
                getCampaignUI.setAccessible(true);
                campaignUI = getCampaignUI.invoke(sector);
            }
            if (campaignUI != null) {
                try {
                    Method showInteractionDialog =
                            findSingleArgCompatibleMethod(
                                    campaignUI.getClass(), "showInteractionDialog", selectedEntity.getClass());
                    if (showInteractionDialog != null) {
                        showInteractionDialog.setAccessible(true);
                        Object shown = showInteractionDialog.invoke(campaignUI, selectedEntity);
                        System.out.println(
                                "Fixer: auto campaign requested colony interaction dialog result="
                                        + String.valueOf(shown));
                    }
                } catch (Throwable t) {
                    System.out.println(
                            "Fixer: auto campaign showInteractionDialog request failed: "
                                    + describeThrowableChain(t));
                }
            }

            String marketName = "unknown";
            Method getName = findMethodRecursive(selectedMarket.getClass(), "getName");
            if (getName != null) {
                getName.setAccessible(true);
                Object nameObj = getName.invoke(selectedMarket);
                if (nameObj != null) {
                    marketName = String.valueOf(nameObj);
                }
            }

            System.out.println("Fixer: auto campaign colony target primed market=" + marketName);
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static Object resolveCampaignStateFromDriver(DriverContext ctx) {
        if (ctx == null) {
            return null;
        }
        try {
            Method getState = findMethodRecursive(ctx.driverClass, "getState", String.class);
            if (getState != null) {
                getState.setAccessible(true);
                Object state = getState.invoke(ctx.driver, CAMPAIGN_STATE_ID);
                if (state != null) {
                    return state;
                }
            }
        } catch (Throwable ignored) {
        }
        if (ctx.states != null) {
            try {
                Object fromMap = ctx.states.get(CAMPAIGN_STATE_ID);
                if (fromMap != null) {
                    return fromMap;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Method findDirectNewGameMethod(Class<?> managerClass) {
        Method[] methods = managerClass.getDeclaredMethods();
        for (Method m : methods) {
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (!"o00000".equals(m.getName())) {
                continue;
            }
            if (m.getReturnType() != String.class) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 2) {
                continue;
            }
            if (!"com.fs.starfarer.campaign.CampaignState".equals(params[1].getName())) {
                continue;
            }
            if (!params[0].getName().startsWith("com.fs.starfarer.campaign.save.")) {
                continue;
            }
            Method getter = findMethodRecursive(params[0], "getCharacterData");
            if (getter == null) {
                continue;
            }
            return m;
        }
        return null;
    }

    private static Object pickMenuEnumFallback(Class<?> enumType, String label) {
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }

        String lower = label == null ? "" : label.trim().toLowerCase();
        // Match by explicit token first when constants are readable (e.g. NEW_GAME).
        String[] tokens = lower.split("\\s+");
        for (Object c : constants) {
            String name = String.valueOf(c).toLowerCase();
            boolean anyTokenMatch = false;
            for (String token : tokens) {
                if (token == null || token.length() < 3) {
                    continue;
                }
                if (name.indexOf(token) >= 0) {
                    anyTokenMatch = true;
                    break;
                }
            }
            if (anyTokenMatch) {
                return c;
            }
        }

        boolean modernTitleMenuLayout = constants.length >= 13;
        int[] preferredOrdinals = null;
        if (lower.indexOf("continue") >= 0) {
            preferredOrdinals = new int[] {0};
        } else if (lower.indexOf("missions") >= 0 || lower.indexOf("mission") >= 0) {
            preferredOrdinals =
                    modernTitleMenuLayout ? new int[] {5, 2} : new int[] {2, 5};
        } else if (lower.indexOf("new") >= 0) {
            preferredOrdinals =
                    modernTitleMenuLayout ? new int[] {6, 3} : new int[] {3, 6};
        } else if (lower.indexOf("load") >= 0) {
            preferredOrdinals =
                    modernTitleMenuLayout ? new int[] {7, 4} : new int[] {4, 7};
        } else if (lower.indexOf("codex") >= 0) {
            preferredOrdinals =
                    modernTitleMenuLayout ? new int[] {8, 5} : new int[] {5, 8};
        } else if (lower.indexOf("settings") >= 0) {
            preferredOrdinals =
                    modernTitleMenuLayout ? new int[] {9, 6} : new int[] {6, 9};
        } else if (lower.indexOf("credits") >= 0) {
            preferredOrdinals =
                    modernTitleMenuLayout ? new int[] {10, 7} : new int[] {7, 10};
        } else if (lower.indexOf("quit") >= 0 || lower.indexOf("exit") >= 0) {
            preferredOrdinals =
                    modernTitleMenuLayout ? new int[] {12, 8} : new int[] {8, 12};
        }

        if (preferredOrdinals != null) {
            for (int ordinal : preferredOrdinals) {
                if (ordinal >= 0 && ordinal < constants.length) {
                    return constants[ordinal];
                }
            }
        }

        for (Object c : constants) {
            String name = String.valueOf(c).toLowerCase();
            if (!lower.isEmpty() && name.indexOf(lower) >= 0) {
                return c;
            }
        }
        return constants[0];
    }

    private static String summarizeEnumConstants(Class<?> enumType) {
        try {
            Object[] constants = enumType.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < constants.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (i >= 12) {
                    sb.append("...");
                    break;
                }
                sb.append(i).append(':').append(String.valueOf(constants[i]));
            }
            sb.append(']');
            return sb.toString();
        } catch (Throwable t) {
            return "[error:" + t + "]";
        }
    }

    private static boolean wouldSelectLoadMenuEntry(Object enumValue, String label) {
        if (enumValue == null) {
            return false;
        }
        String lowerLabel = label == null ? "" : label.toLowerCase();
        if (lowerLabel.indexOf("load") >= 0) {
            return false;
        }
        String name = String.valueOf(enumValue).toLowerCase();
        if (name.indexOf("load") >= 0 || name.indexOf("save") >= 0) {
            return true;
        }
        if (enumValue instanceof Enum) {
            int ordinal = ((Enum) enumValue).ordinal();
            // Known title menu ordering differs across builds.
            // Legacy: 0 Continue, 2 Missions, 3 New Game, 4 Load Game.
            // Current obfuscated build: 0 Continue, 5 Missions, 6 New Game, 7 Load Game.
            if ((ordinal == 4 || ordinal == 7)
                    && (lowerLabel.indexOf("new") >= 0
                            || lowerLabel.indexOf("continue") >= 0
                            || lowerLabel.indexOf("mission") >= 0)) {
                return true;
            }
        }
        return false;
    }

    private static String describeThrowableChain(Throwable t) {
        if (t == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 8) {
            if (depth > 0) {
                sb.append(" <- ");
            }
            sb.append(cur.getClass().getName());
            String msg = cur.getMessage();
            if (msg != null && !msg.isEmpty()) {
                sb.append(": ").append(msg);
            }
            cur = cur.getCause();
            depth++;
        }
        return sb.toString();
    }

    private static boolean isFatalAutoCampaignFailure(String message) {
        if (message == null || message.length() == 0) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                || lower.indexOf("unsatisfiedlinkerror") >= 0
                || lower.indexOf("inaccessibleobjectexception") >= 0
                || lower.indexOf("exceptionininitializererror") >= 0
                || lower.indexOf("missingfieldexception") >= 0
                || lower.indexOf("noclassdeffounderror") >= 0
                || lower.indexOf("classnotfoundexception") >= 0;
    }

    private static String classifyAutoCampaignFailure(String message) {
        if (message == null || message.length() == 0) {
            return "empty";
        }
        String lower = message.toLowerCase();
        if (lower.indexOf("java_jdk_internal_misc_unsafe_putchar") >= 0
                || lower.indexOf("java_sun_misc_unsafe_putchar") >= 0) {
            return "unsafe-putchar-unsatisfied-link";
        }
        if (lower.indexOf("no npefix in java.library.path") >= 0) {
            return "npefix-library-missing";
        }
        if (lower.indexOf("missingfieldexception") >= 0) {
            return "xstream-missing-field";
        }
        if (lower.indexOf("noclassdeffounderror") >= 0) {
            return "class-load-noclassdeffound";
        }
        if (lower.indexOf("classnotfoundexception") >= 0) {
            return "class-load-classnotfound";
        }
        if (lower.indexOf("sprite.getimagewidth") >= 0) {
            return "sprite-getimagewidth-npe";
        }
        if (lower.indexOf("shownewgamedialog") >= 0
                || lower.indexOf("newgamedialogpluginimpl.init") >= 0) {
            return "title-newgame-menu-npe";
        }
        if (lower.indexOf("[title-newgame-menu-npe]") >= 0) {
            return "title-newgame-menu-npe";
        }
        if (lower.indexOf("[campaign-ui-init-npe]") >= 0) {
            return "campaign-ui-init-npe";
        }
        if (lower.indexOf("[sprite-getimagewidth-npe]") >= 0) {
            return "sprite-getimagewidth-npe";
        }
        if (lower.indexOf("[campaign-newgame-npe]") >= 0) {
            return "campaign-newgame-npe";
        }
        if (lower.indexOf("[orbital-junk-spec-npe]") >= 0) {
            return "orbital-junk-spec-npe";
        }
        if (lower.indexOf("customcampaignentity.readresolve") >= 0
                && lower.indexOf("addorbitaljunk") >= 0) {
            return "orbital-junk-spec-npe";
        }
        if (lower.indexOf("unsatisfiedlinkerror") >= 0) {
            return "unsatisfied-link";
        }
        if (lower.indexOf("inaccessibleobjectexception") >= 0) {
            return "inaccessible-object";
        }
        if (lower.indexOf("nullpointerexception") >= 0) {
            return "generic-npe";
        }
        return "other-fatal";
    }

    private static boolean isRetryableAutoCampaignSignature(String signature) {
        if (signature == null || signature.length() == 0) {
            return false;
        }
        return "campaign-ui-init-npe".equals(signature)
                || "sprite-getimagewidth-npe".equals(signature)
                || "campaign-newgame-npe".equals(signature)
                || "orbital-junk-spec-npe".equals(signature);
    }

    private static boolean isRetryableMenuAutoCampaignSignature(String signature) {
        if (signature == null || signature.length() == 0) {
            return false;
        }
        return "title-newgame-menu-npe".equals(signature);
    }

    private static int incrementNamedCounter(Map<String, Integer> counters, String key) {
        if (counters == null) {
            return 0;
        }
        String safeKey = key == null ? "null" : key;
        Integer cur = counters.get(safeKey);
        int next = (cur == null ? 0 : cur.intValue()) + 1;
        counters.put(safeKey, Integer.valueOf(next));
        return next;
    }

    private static String sanitizeStartVariantId(String requested) {
        String candidate = requested == null ? "" : requested.trim();
        if (candidate.isEmpty()) {
            return "lasher_Standard";
        }
        if (candidate.length() > 96
                || candidate.indexOf('/') >= 0
                || candidate.indexOf('\\') >= 0
                || candidate.indexOf(':') >= 0
                || candidate.indexOf("..") >= 0) {
            System.out.println(
                    "Fixer: auto campaign invalid start variant '" + requested + "', using lasher_Standard.");
            return "lasher_Standard";
        }
        return candidate;
    }

    private static String resolveStartVariantForNewGame(String requestedVariant) {
        String sanitized = sanitizeStartVariantId(requestedVariant);
        final String fallback = "lasher_Standard";
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> hullVariantClass = Class.forName("com.fs.starfarer.loading.specs.HullVariantSpec");
            Object requested = lookupSpecById(specStoreClass, hullVariantClass, sanitized);
            if (requested != null) {
                return sanitized;
            }
            Object fallbackSpec = lookupSpecById(specStoreClass, hullVariantClass, fallback);
            if (fallbackSpec != null) {
                System.out.println(
                        "Fixer: auto campaign start variant '"
                                + sanitized
                                + "' not available, falling back to "
                                + fallback
                                + ".");
                return fallback;
            }
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign variant preflight skipped: " + describeThrowableChain(t));
        }
        return sanitized;
    }

    private static Object buildDefaultCharacterCreationData(Class<?> dataClass) {
        try {
            java.lang.reflect.Constructor<?> ctor = dataClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object data = ctor.newInstance();

            String startVariant =
                    System.getProperty(AUTO_CAMPAIGN_START_VARIANT_PROPERTY, "lasher_Standard").trim();
            startVariant = resolveStartVariantForNewGame(startVariant);
            String captainName =
                    System.getProperty(AUTO_CAMPAIGN_CAPTAIN_NAME_PROPERTY, "Web Captain").trim();
            if (captainName.isEmpty()) {
                captainName = "Web Captain";
            }

            Method setDifficulty = findMethodRecursive(dataClass, "setDifficulty", String.class);
            if (setDifficulty != null) {
                setDifficulty.setAccessible(true);
                setDifficulty.invoke(data, "normal");
            }
            Method setCampaignHelpEnabled =
                    findMethodRecursive(dataClass, "setCampaignHelpEnabled", Boolean.TYPE);
            if (setCampaignHelpEnabled != null) {
                setCampaignHelpEnabled.setAccessible(true);
                setCampaignHelpEnabled.invoke(data, Boolean.FALSE);
            }
            Method setDone = findMethodRecursive(dataClass, "setDone", Boolean.TYPE);
            if (setDone != null) {
                setDone.setAccessible(true);
                setDone.invoke(data, Boolean.TRUE);
            }
            Method setStartingShip = findMethodRecursive(dataClass, "setStartingShip", String.class);
            if (setStartingShip != null) {
                setStartingShip.setAccessible(true);
                setStartingShip.invoke(data, startVariant);
            }
            Method addStartingShipChoice =
                    findMethodRecursive(dataClass, "addStartingShipChoice", String.class);
            if (addStartingShipChoice != null) {
                addStartingShipChoice.setAccessible(true);
                addStartingShipChoice.invoke(data, startVariant);
            }

            boolean fleetMemberAdded = addStartingFleetMemberSafely(dataClass, data, startVariant);
            if (!fleetMemberAdded) {
                System.out.println(
                        "Fixer: auto campaign warning: addStartingFleetMember unavailable for "
                                + startVariant);
            }
            int additionalShips = getAdditionalShipsCount(data);
            if (additionalShips <= 0 && !"lasher_Standard".equals(startVariant)) {
                addStartingFleetMemberSafely(dataClass, data, "lasher_Standard");
                additionalShips = getAdditionalShipsCount(data);
            }
            if (additionalShips <= 0) {
                System.out.println(
                        "Fixer: auto campaign warning: character data has no additional ships after setup.");
            }

            Method setSectorSize = findMethodRecursive(dataClass, "setSectorSize", String.class);
            String sectorSize =
                    System.getProperty(AUTO_CAMPAIGN_SECTOR_SIZE_PROPERTY, "normal").trim();
            if (sectorSize.isEmpty()) {
                sectorSize = "normal";
            }
            if (setSectorSize != null) {
                setSectorSize.setAccessible(true);
                setSectorSize.invoke(data, sectorSize);
            }

            Method setWithTimePass = findMethodRecursive(dataClass, "setWithTimePass", Boolean.TYPE);
            boolean withTimePass =
                    !"false"
                            .equalsIgnoreCase(
                                    System.getProperty(AUTO_CAMPAIGN_WITH_TIME_PASS_PROPERTY, "true"));
            if (setWithTimePass != null) {
                setWithTimePass.setAccessible(true);
                setWithTimePass.invoke(data, Boolean.valueOf(withTimePass));
            }

            String seedString =
                    System.getProperty(AUTO_CAMPAIGN_SEED_STRING_PROPERTY, "").trim();
            if (seedString.isEmpty()) {
                long generatedSeed = Math.abs(System.currentTimeMillis() % 1000000000L);
                seedString = "SEK" + generatedSeed;
            }
            Method setSeedString = findMethodRecursive(dataClass, "setSeedString", String.class);
            if (setSeedString != null) {
                setSeedString.setAccessible(true);
                setSeedString.invoke(data, seedString);
            }
            Method setSeed = findMethodRecursive(dataClass, "setSeed", Long.TYPE);
            if (setSeed != null) {
                setSeed.setAccessible(true);
                long seedLong = Math.abs((long) seedString.hashCode());
                if (seedLong == 0L) {
                    seedLong = 1L;
                }
                setSeed.invoke(data, Long.valueOf(seedLong));
            }

            Method setSectorAge = findSingleArgMethod(dataClass, "setSectorAge");
            if (setSectorAge != null) {
                Class<?> ageType = setSectorAge.getParameterTypes()[0];
                if (ageType != null && ageType.isEnum()) {
                    String ageName =
                            System.getProperty(AUTO_CAMPAIGN_SECTOR_AGE_PROPERTY, "AVERAGE")
                                    .trim()
                                    .toUpperCase();
                    Object age = pickEnumConstantIgnoreCase(ageType, ageName);
                    if (age == null) {
                        Object[] values = ageType.getEnumConstants();
                        if (values != null && values.length > 0) {
                            age = values[Math.min(1, values.length - 1)];
                        }
                    }
                    if (age != null) {
                        setSectorAge.setAccessible(true);
                        setSectorAge.invoke(data, age);
                    }
                }
            }
            Method setStartingLocationName =
                    findMethodRecursive(dataClass, "setStartingLocationName", String.class);
            String startingLocation =
                    System.getProperty(AUTO_CAMPAIGN_STARTING_LOCATION_PROPERTY, "Corvus").trim();
            if (startingLocation.isEmpty()) {
                startingLocation = "Corvus";
            }
            if (setStartingLocationName != null) {
                setStartingLocationName.setAccessible(true);
                setStartingLocationName.invoke(data, startingLocation);
            }
            Method getStartingCoordinates =
                    findMethodRecursive(dataClass, "getStartingCoordinates");
            if (getStartingCoordinates != null) {
                getStartingCoordinates.setAccessible(true);
                Object coords = getStartingCoordinates.invoke(data);
                if (coords != null) {
                    float startX =
                            parseFloatProperty(AUTO_CAMPAIGN_START_X_PROPERTY, -2500f);
                    float startY =
                            parseFloatProperty(AUTO_CAMPAIGN_START_Y_PROPERTY, 3000f);
                    writeFloatField(coords, "x", startX);
                    writeFloatField(coords, "y", startY);
                }
            }

            Method getCharacterData = findMethodRecursive(dataClass, "getCharacterData");
            if (getCharacterData != null) {
                getCharacterData.setAccessible(true);
                Object charData = getCharacterData.invoke(data);
                if (charData != null) {
                    Method setName = findMethodRecursive(charData.getClass(), "setName", String.class);
                    if (setName != null) {
                        setName.setAccessible(true);
                        setName.invoke(charData, captainName);
                    }
                }
            }

            System.out.println(
                    "Fixer: auto campaign data prepared startVariant="
                            + startVariant
                            + " startLocation="
                            + startingLocation
                            + " sectorSize="
                            + sectorSize
                            + " withTimePass="
                            + withTimePass
                            + " seed="
                            + seedString
                            + " additionalShips="
                            + getAdditionalShipsCount(data));
            return data;
        } catch (Throwable t) {
            System.out.println("Fixer: auto campaign data build failure: " + stackTraceToString(t));
            return null;
        }
    }

    private static boolean addStartingFleetMemberSafely(Class<?> dataClass, Object data, String variantId) {
        if (dataClass == null || data == null || variantId == null || variantId.trim().isEmpty()) {
            return false;
        }
        Method method = null;
        for (Method m : dataClass.getMethods()) {
            if (!"addStartingFleetMember".equals(m.getName())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0] == String.class) {
                method = m;
                break;
            }
        }
        if (method == null) {
            for (Method m : dataClass.getDeclaredMethods()) {
                if (!"addStartingFleetMember".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == String.class) {
                    method = m;
                    break;
                }
            }
        }
        if (method == null) {
            return false;
        }

        try {
            method.setAccessible(true);
            Class<?> enumType = method.getParameterTypes()[1];
            if (enumType == null || !enumType.isEnum()) {
                return false;
            }
            Object shipType = null;
            try {
                @SuppressWarnings("rawtypes")
                Object byName = Enum.valueOf((Class) enumType, "SHIP");
                shipType = byName;
            } catch (Throwable ignored) {
            }
            if (shipType == null) {
                Object[] constants = enumType.getEnumConstants();
                if (constants != null && constants.length > 0) {
                    shipType = constants[0];
                }
            }
            if (shipType == null) {
                return false;
            }
            method.invoke(data, variantId, shipType);
            return true;
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign addStartingFleetMember failed for "
                            + variantId
                            + ": "
                            + t);
            return false;
        }
    }

    private static int getAdditionalShipsCount(Object data) {
        if (data == null) {
            return -1;
        }
        try {
            Method getter = findMethodRecursive(data.getClass(), "getAdditionalShips");
            if (getter == null) {
                return -1;
            }
            getter.setAccessible(true);
            Object list = getter.invoke(data);
            if (list instanceof java.util.Collection) {
                return ((java.util.Collection) list).size();
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static void publishAutoCampaignDirectNewGameCommittedAt(long atMs) {
        if (atMs <= 0L) {
            return;
        }
        if (autoCampaignDirectNewGameCommittedAt < atMs) {
            autoCampaignDirectNewGameCommittedAt = atMs;
        }
        try {
            System.setProperty(
                    AUTO_CAMPAIGN_DIRECT_NEW_GAME_COMMITTED_AT_PROPERTY, String.valueOf(atMs));
        } catch (Throwable ignored) {
        }
    }

    private static void resetAutoCampaignDirectNewGameCommittedAt() {
        autoCampaignDirectNewGameCommittedAt = -1L;
        try {
            System.clearProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_COMMITTED_AT_PROPERTY);
        } catch (Throwable ignored) {
        }
    }

    private static boolean claimDirectNewGameLease(String owner, long now, long leaseMs) {
        if (owner == null || owner.length() == 0) {
            return true;
        }
        long safeNow = now <= 0L ? System.currentTimeMillis() : now;
        long safeLeaseMs = Math.max(5000L, leaseMs);
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_OWNER_PROPERTY, "");
            long activeAt =
                    parseLongProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_AT_PROPERTY, -1L);
            boolean active =
                    activeAt > 0L
                            && safeNow >= activeAt
                            && (safeNow - activeAt) <= safeLeaseMs
                            && activeOwner != null
                            && activeOwner.length() > 0
                            && !owner.equals(activeOwner);
            if (active) {
                return false;
            }
            System.setProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_OWNER_PROPERTY, owner);
            System.setProperty(
                    AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_AT_PROPERTY, String.valueOf(safeNow));
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void releaseDirectNewGameLease(String owner) {
        if (owner == null || owner.length() == 0) {
            return;
        }
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_OWNER_PROPERTY, "");
            if (owner.equals(activeOwner)) {
                System.clearProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_OWNER_PROPERTY);
                System.clearProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_LEASE_AT_PROPERTY);
            }
        } catch (Throwable ignored) {
        }
    }

    private static float readFloatField(Object target, String fieldName, float fallback) {
        if (target == null || fieldName == null) {
            return fallback;
        }
        try {
            Field field = target.getClass().getField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static void writeFloatField(Object target, String fieldName, float value) {
        if (target == null || fieldName == null) {
            return;
        }
        try {
            Field field = findFieldRecursive(target.getClass(), fieldName);
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type == Float.TYPE || type == Float.class) {
                field.set(target, Float.valueOf(value));
            } else if (Number.class.isAssignableFrom(type)) {
                field.set(target, Float.valueOf(value));
            }
        } catch (Throwable ignored) {
        }
    }

    private static Method findSingleArgMethod(Class<?> cls, String name) {
        if (cls == null || name == null) {
            return null;
        }
        for (Method m : cls.getMethods()) {
            if (name.equals(m.getName()) && m.getParameterTypes().length == 1) {
                return m;
            }
        }
        for (Method m : cls.getDeclaredMethods()) {
            if (name.equals(m.getName()) && m.getParameterTypes().length == 1) {
                return m;
            }
        }
        return null;
    }

    private static Method findSingleArgCompatibleMethod(Class<?> cls, String name, Class<?> argType) {
        if (cls == null || name == null || argType == null) {
            return null;
        }
        for (Method m : cls.getMethods()) {
            if (!name.equals(m.getName()) || m.getParameterTypes().length != 1) {
                continue;
            }
            Class<?> param = m.getParameterTypes()[0];
            if (param.isAssignableFrom(argType)) {
                return m;
            }
        }
        for (Method m : cls.getDeclaredMethods()) {
            if (!name.equals(m.getName()) || m.getParameterTypes().length != 1) {
                continue;
            }
            Class<?> param = m.getParameterTypes()[0];
            if (param.isAssignableFrom(argType)) {
                return m;
            }
        }
        return null;
    }

    private static void warmupTitleRenderTicks(Object titleState, int count) {
        if (titleState == null || count <= 0) {
            return;
        }
        if (!Boolean.parseBoolean(
                System.getProperty(AUTO_CAMPAIGN_TITLE_RENDER_WARMUP_PROPERTY, "false"))) {
            return;
        }
        try {
            Method render = findMethodRecursive(titleState.getClass(), "render", Float.TYPE);
            Method getFader = findMethodRecursive(titleState.getClass(), "getFader");
            if (render == null || getFader == null) {
                return;
            }
            render.setAccessible(true);
            getFader.setAccessible(true);
            Object fader = getFader.invoke(titleState);
            float brightness = 1.0f;
            if (fader != null) {
                Method getBrightness = findMethodRecursive(fader.getClass(), "getBrightness");
                if (getBrightness != null) {
                    getBrightness.setAccessible(true);
                    Object value = getBrightness.invoke(fader);
                    if (value instanceof Number) {
                        brightness = ((Number) value).floatValue();
                    }
                }
            }
            Method displayUpdate = findMethodRecursive(Class.forName("org.lwjgl.opengl.Display"), "update");
            if (displayUpdate != null) {
                displayUpdate.setAccessible(true);
            }
            for (int i = 0; i < count; i++) {
                render.invoke(titleState, Float.valueOf(brightness));
                if (displayUpdate != null) {
                    displayUpdate.invoke(null);
                }
            }
        } catch (Throwable t) {
            System.out.println("Fixer: title render warmup skipped: " + describeThrowableChain(t));
        }
    }

    private static Object pickEnumConstantIgnoreCase(Class<?> enumType, String name) {
        if (enumType == null || !enumType.isEnum()) {
            return null;
        }
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        String target = name == null ? "" : name.trim();
        if (target.isEmpty()) {
            return constants[0];
        }
        for (Object constant : constants) {
            if (constant == null) continue;
            String value = String.valueOf(constant);
            if (target.equalsIgnoreCase(value)) {
                return constant;
            }
        }
        return null;
    }

    private static String stackTraceToString(Throwable t) {
        if (t == null) {
            return "null";
        }
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            Throwable cur = t;
            int depth = 0;
            while (cur != null && depth < 6) {
                if (depth > 0) {
                    pw.println("Caused by:");
                }
                cur.printStackTrace(pw);
                cur = cur.getCause();
                depth++;
            }
            pw.flush();
            return sw.toString();
        } catch (Throwable ignored) {
            return String.valueOf(t);
        }
    }

    private static String waitForDirectNewGameRuntimeReadiness() {
        long maxWaitMs =
                Math.max(
                        500L,
                        parseLongProperty("starsector.autoCampaignDirectPostInitWaitMs", 8000L));
        long pollMs =
                Math.max(
                        100L,
                        Math.min(
                                1000L,
                                parseLongProperty(
                                        "starsector.autoCampaignDirectPostInitPollMs", 250L)));
        long deadline = System.currentTimeMillis() + maxWaitMs;
        String lastIssue = null;
        while (System.currentTimeMillis() <= deadline) {
            lastIssue = checkDirectNewGameRuntimeReadiness();
            if (lastIssue == null) {
                return null;
            }
            try {
                Thread.sleep(pollMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return "interrupted";
            }
        }
        return lastIssue == null ? "runtime-not-ready" : lastIssue;
    }

    private static String checkDirectNewGameRuntimeReadiness() {
        try {
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSector = findMethodRecursive(globalClass, "getSector");
            if (getSector == null) {
                return "global-getsector-missing";
            }
            getSector.setAccessible(true);
            Object sector = getSector.invoke(null);
            if (sector == null) {
                return "sector-null";
            }

            Method getEconomy = findMethodRecursive(sector.getClass(), "getEconomy");
            if (getEconomy == null) {
                return "sector-geteconomy-missing";
            }
            getEconomy.setAccessible(true);
            Object economy = getEconomy.invoke(sector);
            if (economy == null) {
                return "sector-economy-null";
            }

            Method getPlayerFleet = findMethodRecursive(sector.getClass(), "getPlayerFleet");
            if (getPlayerFleet == null) {
                return "sector-getplayerfleet-missing";
            }
            getPlayerFleet.setAccessible(true);
            Object playerFleet = getPlayerFleet.invoke(sector);
            if (playerFleet == null) {
                return "player-fleet-null";
            }

            return null;
        } catch (Throwable t) {
            return "runtime-readiness-exception: " + describeThrowableChain(t);
        }
    }

    private static String checkCampaignStateTransitionReadiness(DriverContext ctx) {
        try {
            Object campaignState = resolveCampaignStateFromDriver(ctx);
            if (campaignState == null) {
                return "campaign-state-null";
            }
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSector = findMethodRecursive(globalClass, "getSector");
            if (getSector == null) {
                return "global-getsector-missing";
            }
            getSector.setAccessible(true);
            Object sector = getSector.invoke(null);
            if (sector == null) {
                return "sector-null";
            }

            Method getEconomy = findMethodRecursive(sector.getClass(), "getEconomy");
            if (getEconomy != null) {
                getEconomy.setAccessible(true);
                Object economy = getEconomy.invoke(sector);
                if (economy == null) {
                    return "sector-economy-null";
                }
            }

            Method getPlayerFleet = findMethodRecursive(sector.getClass(), "getPlayerFleet");
            if (getPlayerFleet == null) {
                return "sector-getplayerfleet-missing";
            }
            getPlayerFleet.setAccessible(true);
            Object playerFleet = getPlayerFleet.invoke(sector);
            if (playerFleet == null) {
                return null;
            }
            return null;
        } catch (Throwable t) {
            return "transition-readiness-exception: " + describeThrowableChain(t);
        }
    }

    private static boolean shouldForceCampaignTransitionForReadinessIssue(String issue) {
        if (issue == null) {
            return false;
        }
        String lowered = issue.toLowerCase();
        return lowered.indexOf("player-fleet-null") >= 0
                || lowered.indexOf("sector-null") >= 0
                || lowered.indexOf("sector-economy-null") >= 0;
    }

    private static void maybePrepareUiForCampaignTransition() {
        long now = System.currentTimeMillis();
        if (campaignTransitionUiPreflightReady
                && (now - campaignTransitionUiPreflightLastAttemptAt) < 30000L) {
            return;
        }
        if (campaignTransitionUiPreflightLastAttemptAt > 0L
                && (now - campaignTransitionUiPreflightLastAttemptAt) < 5000L) {
            return;
        }

        campaignTransitionUiPreflightLastAttemptAt = now;
        String previousIssue = campaignTransitionUiPreflightLastIssue;
        String issue = prepareUiForCampaignTransition();
        campaignTransitionUiPreflightLastIssue = issue;
        if (issue == null) {
            campaignTransitionUiPreflightReady = true;
            if ((now - campaignTransitionUiPreflightLastLogAt) >= 10000L) {
                System.out.println("Fixer: campaign transition UI preflight ready.");
                campaignTransitionUiPreflightLastLogAt = now;
            }
            return;
        }

        campaignTransitionUiPreflightReady = false;
        if (campaignTransitionUiPreflightLastLogAt == 0L
                || (now - campaignTransitionUiPreflightLastLogAt) >= 5000L
                || !issue.equals(previousIssue)) {
            System.out.println("Fixer: campaign transition UI preflight issue: " + issue);
            campaignTransitionUiPreflightLastLogAt = now;
        }
    }

    private static String prepareUiForCampaignTransition() {
        String uiFontIssue = ensureUiFontStateReadyForDirectNewGame();
        String uiTextureIssue = ensureUiPanelTextureObjectsReadyForDirectNewGame();
        String uiSpriteIssue = ensureUiBorderSpriteMappingsReady();
        if (uiFontIssue == null && uiTextureIssue == null && uiSpriteIssue == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (uiFontIssue != null) {
            sb.append("ui-font=").append(uiFontIssue);
        }
        if (uiTextureIssue != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("ui-texture=").append(uiTextureIssue);
        }
        if (uiSpriteIssue != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("ui-sprite=").append(uiSpriteIssue);
        }
        return sb.toString();
    }

    private static boolean setCampaignStateIfPossible(DriverContext ctx, Object titleState) {
        maybePrepareUiForCampaignTransition();
        Object beforeState = readCurrentStateFromDriver(ctx);
        if (ctx != null && ctx.driver != null && ctx.driverClass != null) {
            try {
                Method driverGoToState =
                        findMethodRecursive(ctx.driverClass, "goToState", String.class);
                if (driverGoToState != null) {
                    driverGoToState.setAccessible(true);
                    String invokeIssue =
                            invokeMethodWithTimeout(
                                    ctx.driver, driverGoToState, CAMPAIGN_STATE_ID, 1500L);
                    if (invokeIssue != null) {
                        System.out.println(
                                "Fixer: driver goToState transition attempt issue: " + invokeIssue);
                        return false;
                    }
                    forceStateFaderOut(titleState);
                    if (!didCampaignTransitionAdvance(ctx, titleState, beforeState)) {
                        System.out.println(
                                "Fixer: requested Campaign State via driver.goToState, but state has not advanced yet.");
                        return false;
                    }
                    System.out.println(
                            "Fixer: requested transition to Campaign State (driver.goToState).");
                    return true;
                }
            } catch (Throwable t) {
                System.out.println(
                        "Fixer: driver goToState transition attempt failed: "
                                + describeThrowableChain(t));
            }
        }
        if (titleState == null) {
            return false;
        }
        try {
            Method goToState = findMethodRecursive(titleState.getClass(), "goToState", String.class);
            if (goToState == null) {
                return false;
            }
            goToState.setAccessible(true);
            String invokeIssue =
                    invokeMethodWithTimeout(titleState, goToState, CAMPAIGN_STATE_ID, 1500L);
            if (invokeIssue != null) {
                System.out.println(
                        "Fixer: titleState.goToState transition attempt issue: " + invokeIssue);
                return false;
            }
            forceStateFaderOut(titleState);
            if (!didCampaignTransitionAdvance(ctx, titleState, beforeState)) {
                System.out.println(
                        "Fixer: requested Campaign State via titleState.goToState, but state has not advanced yet.");
                return false;
            }
            System.out.println("Fixer: requested transition to Campaign State (titleState.goToState).");
            return true;
        } catch (Throwable t) {
            System.out.println("Fixer: unable to request Campaign State transition: " + t);
            return false;
        }
    }

    private static boolean didCampaignTransitionAdvance(
            DriverContext ctx, Object titleState, Object beforeState) {
        Object afterState = readCurrentStateFromDriver(ctx);
        if (afterState == null) {
            return false;
        }
        if (titleState != null && afterState == titleState) {
            return false;
        }
        if (beforeState != null && afterState == beforeState && isTitleState(null, afterState)) {
            return false;
        }
        return !isTitleState(null, afterState);
    }

    private static Object readCurrentStateFromDriver(DriverContext ctx) {
        if (ctx == null || ctx.driver == null || ctx.driverClass == null) {
            return null;
        }
        try {
            Method getCurrentState = findMethodRecursive(ctx.driverClass, "getCurrentState");
            if (getCurrentState == null) {
                return null;
            }
            getCurrentState.setAccessible(true);
            return getCurrentState.invoke(ctx.driver);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String invokeMethodWithTimeout(
            final Object target, final Method method, final Object arg, long timeoutMs) {
        final Throwable[] errorHolder = new Throwable[1];
        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    method.invoke(target, arg);
                                } catch (Throwable t) {
                                    errorHolder[0] = t;
                                }
                            }
                        },
                        "fixer-transition-invoke");
        worker.setDaemon(true);
        long waitMs = Math.max(250L, timeoutMs);
        try {
            worker.start();
            worker.join(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "interrupted";
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
        if (worker.isAlive()) {
            try {
                worker.interrupt();
            } catch (Throwable ignored) {
            }
            return "timeout(" + waitMs + "ms)";
        }
        if (errorHolder[0] != null) {
            return describeThrowableChain(errorHolder[0]);
        }
        return null;
    }

    private static void forceStateFaderOut(Object state) {
        if (state == null) {
            return;
        }
        try {
            Method getFader = findMethodRecursive(state.getClass(), "getFader");
            if (getFader == null) {
                return;
            }
            getFader.setAccessible(true);
            Object fader = getFader.invoke(state);
            if (fader == null) {
                return;
            }
            Method forceOut = findMethodRecursive(fader.getClass(), "forceOut");
            if (forceOut == null) {
                return;
            }
            forceOut.setAccessible(true);
            forceOut.invoke(fader);
        } catch (Throwable ignored) {
        }
    }

    private static Method findMethodRecursive(Class<?> cls, String name, Class<?>... params) {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {
            }
            c = c.getSuperclass();
        }
        try {
            return cls.getMethod(name, params);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Field findFieldRecursive(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object readStaticFieldRecursive(Class<?> cls, String name) {
        try {
            Field field = findFieldRecursive(cls, name);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object readFieldRecursive(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = findFieldRecursive(target.getClass(), name);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static long parseLongProperty(String key, long fallback) {
        String raw = System.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int parseIntProperty(String key, int fallback) {
        String raw = System.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static float parseFloatProperty(String key, float fallback) {
        String raw = System.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static void initializeResourceManager() {
        final String[] roots =
                new String[] {
                    "/app/starsector/starsector/.",
                    "/app/starsector/starsector",
                    "/app/starsector/starsector/",
                    "/files",
                    "../starfarer.res/res",
                    "/starfarer.res/res"
                };

        final String[] probes =
                new String[] {
                    "graphics/ui/s_icon16.png",
                    "/graphics/ui/s_icon16.png",
                    "data/config/settings.json",
                    "/data/config/settings.json"
                };

        try {
            Set<String> candidates = findResourceManagerCandidates();
            Object fallbackManager = null;
            Method fallbackInit = null;
            String fallbackRoot = null;
            String fallbackClass = null;
            String fallbackProbe = null;
            for (String className : candidates) {
                try {
                    Class<?> cls = Class.forName(className);
                    Method singleton = findSingleton(cls);
                    List<Method> initWithStringMethods = findInitWithStringCandidates(cls);
                    if (singleton == null || initWithStringMethods.isEmpty()) {
                        continue;
                    }

                    singleton.setAccessible(true);
                    for (Method m : initWithStringMethods) {
                        m.setAccessible(true);
                    }
                    Object manager = singleton.invoke(null);
                    if (manager == null) {
                        continue;
                    }

                    Method openResource = findOpenResource(cls);
                    if (openResource != null) {
                        openResource.setAccessible(true);
                    }

                    for (String root : roots) {
                        for (Method initWithString : initWithStringMethods) {
                            try {
                                initWithString.invoke(manager, root);
                            } catch (Throwable ignored) {
                            }
                        }

                        if (openResource == null) {
                            continue;
                        }

                        boolean rootMatched = false;
                        String matchedProbe = null;
                        for (String probe : probes) {
                            if (canOpen(manager, openResource, probe)) {
                                rootMatched = true;
                                matchedProbe = probe;
                                break;
                            }
                        }

                        if (!rootMatched) {
                            continue;
                        }

                        if (fallbackManager == null) {
                            fallbackManager = manager;
                            fallbackInit = initWithStringMethods.get(0);
                            fallbackRoot = root;
                            fallbackClass = className;
                            fallbackProbe = matchedProbe;
                            resourceManagerInstance = manager;
                            resourceManagerOpenResource = openResource;
                        }
                    }

                    if (fallbackManager != null && fallbackInit != null && fallbackRoot != null) {
                        try {
                            fallbackInit.invoke(fallbackManager, fallbackRoot);
                        } catch (Throwable ignored) {
                        }
                        System.out.println(
                                "Fixer: Resource manager root ready via "
                                        + fallbackClass
                                        + " root="
                                        + fallbackRoot
                                        + " probe="
                                        + fallbackProbe
                                        + " (fallback)");
                        resourceManagerInstance = fallbackManager;
                        resourceManagerOpenResource = openResource;
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            System.out.println("Fixer: Resource manager bootstrap failed: " + t);
            return;
        }

        System.out.println("Fixer: Resource manager bootstrap unavailable (continuing).");
    }

    private static Set<String> findResourceManagerCandidates() {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        String classPath = System.getProperty("java.class.path", "");
        String[] entries = classPath.split(File.pathSeparator);

        for (String entry : entries) {
            if (entry == null || !entry.endsWith(".jar")) {
                continue;
            }
            JarFile jar = null;
            try {
                jar = new JarFile(entry);
                Enumeration<JarEntry> e = jar.entries();
                while (e.hasMoreElements()) {
                    JarEntry je = e.nextElement();
                    String name = je.getName();
                    if (!name.startsWith("com/fs/util/oo") || !name.endsWith(".class")) {
                        continue;
                    }
                    if (name.indexOf('$') >= 0) {
                        continue;
                    }
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    out.add(className);
                }
            } catch (Throwable ignored) {
            } finally {
                if (jar != null) {
                    try {
                        jar.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        return out;
    }

    private static Method findSingleton(Class<?> cls) {
        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            if (!Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length != 0) {
                continue;
            }
            if (cls.isAssignableFrom(m.getReturnType())) {
                return m;
            }
        }
        return null;
    }

    private static List<Method> findInitWithStringCandidates(Class<?> cls) {
        LinkedHashSet<Method> out = new LinkedHashSet<Method>();
        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getReturnType() != Void.TYPE) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1 && params[0] == String.class) {
                out.add(m);
            }
        }
        return new java.util.ArrayList<Method>(out);
    }

    private static Method findOpenResource(Class<?> cls) {
        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1 || params[0] != String.class) {
                continue;
            }
            if (InputStream.class.isAssignableFrom(m.getReturnType())) {
                return m;
            }
        }
        return null;
    }

    private static boolean canOpen(Object manager, Method openResource, String path) {
        InputStream in = null;
        try {
            Object result = openResource.invoke(manager, path);
            if (!(result instanceof InputStream)) {
                return false;
            }
            in = (InputStream) result;
            return in.read() >= 0;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void ensureDirectory(String path) {
        try {
            File dir = new File(path);
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("Fixer: Failed to create directory: " + path);
            }
        } catch (Throwable t) {
            System.out.println("Fixer: Failed to prepare directory " + path + ": " + t);
        }
    }

    private static void configureJvmIdentityProperties() {
        ensureSystemProperty(JAVA_VM_VENDOR_PROPERTY, DEFAULT_JAVA_VM_VENDOR);
        ensureSystemProperty(JAVA_VENDOR_PROPERTY, DEFAULT_JAVA_VENDOR);
        ensureSystemProperty(JAVA_VM_NAME_PROPERTY, DEFAULT_JAVA_VM_NAME);
        ensureSystemProperty(JAVA_SPEC_VERSION_PROPERTY, DEFAULT_JAVA_SPEC_VERSION);
        ensureSystemProperty(JAVA_SPEC_VENDOR_PROPERTY, DEFAULT_JAVA_SPEC_VENDOR);
        ensureSystemProperty(JAVA_SPEC_NAME_PROPERTY, DEFAULT_JAVA_SPEC_NAME);
    }

    private static void ensureSystemProperty(String property, String defaultValue) {
        try {
            String existing = System.getProperty(property);
            if (existing == null || existing.trim().length() == 0) {
                System.setProperty(property, defaultValue);
                System.out.println("Fixer: defaulted " + property + "=" + defaultValue);
            }
        } catch (Throwable t) {
            System.out.println("Fixer: failed to configure property " + property + ": " + t);
        }
    }

    private static void configureFilesystemPaths() {
        ensurePathProperty(LOGS_PROPERTY, LOGS_PATH, false);
        ensurePathProperty(SAVES_PROPERTY, SAVES_PATH, false);
        ensurePathProperty(SCREENSHOTS_PROPERTY, SCREENSHOTS_PATH, false);
        // Mods are always mounted from /files/mods in this web launcher flow.
        ensurePathProperty(MODS_PROPERTY, MODS_PATH, true);
    }

    private static void ensurePathProperty(String property, String defaultPath, boolean forceDefault) {
        try {
            String existing = System.getProperty(property);
            String normalizedExisting = existing == null ? null : existing.trim();
            String path =
                    forceDefault
                            ? defaultPath
                            : (normalizedExisting == null || normalizedExisting.length() == 0
                                    ? defaultPath
                                    : normalizedExisting);
            System.setProperty(property, path);
            ensureDirectory(path);
            System.out.println(
                    "Fixer: path "
                            + property
                            + "="
                            + path
                            + (forceDefault ? " (forced)" : ""));
        } catch (Throwable t) {
            System.out.println("Fixer: failed to set path property " + property + ": " + t);
        }
    }

    private static void mirrorCoreSpecDirectoriesToFiles() {
        try {
            if (resourceManagerInstance == null || resourceManagerOpenResource == null) {
                System.out.println("Fixer: mirror skipped, resource manager stream unavailable.");
                return;
            }

            Set<String> roots = new LinkedHashSet<String>();
            roots.add("data/weapons");
            roots.add("data/shipsystems");
            roots.add("data/hulls");
            roots.add("data/variants");
            roots.add("data/characters/skills");

            Set<String> files = new LinkedHashSet<String>();
            Set<String> seenDirs = new HashSet<String>();
            for (String root : roots) {
                collectSpecFilesFromIndex(root, seenDirs, files);
            }
            synchronized (mirroredSpecPaths) {
                mirroredSpecPaths.clear();
                mirroredSpecPaths.addAll(files);
            }

            int copied = 0;
            int failed = 0;
            for (String rel : files) {
                try {
                    if (copyResourceToFiles(rel)) {
                        copied++;
                    }
                } catch (Throwable t) {
                    failed++;
                }
            }

            File check = new File(FILES_ROOT + "/data/hulls/onslaught.ship");
            System.out.println(
                    "Fixer: mirrored core specs into /files copied="
                            + copied
                            + " failed="
                            + failed
                            + ", sentinel exists="
                            + check.exists()
                            + " len="
                            + (check.exists() ? check.length() : -1));
        } catch (Throwable t) {
            System.out.println("Fixer: unable to mirror core specs into /files: " + t);
        }
    }

    private static void collectSpecFilesFromIndex(
            String dir, Set<String> seenDirs, Set<String> outFiles) throws Exception {
        if (!seenDirs.add(dir)) {
            return;
        }
        List<String> entries = readIndexEntries(dir);
        for (String entry : entries) {
            String clean = entry.replace('\\', '/');
            while (clean.startsWith("/")) clean = clean.substring(1);
            if (clean.isEmpty()) continue;
            String rel = clean.startsWith("data/") ? clean : (dir + "/" + clean);
            while (rel.contains("//")) rel = rel.replace("//", "/");
            if (rel.endsWith("/")) rel = rel.substring(0, rel.length() - 1);
            int dot = rel.lastIndexOf('.');
            if (dot > 0) {
                String ext = rel.substring(dot + 1).toLowerCase();
                if ("wpn".equals(ext)
                        || "proj".equals(ext)
                        || "ship".equals(ext)
                        || "system".equals(ext)
                        || "skin".equals(ext)
                        || "variant".equals(ext)
                        || "skill".equals(ext)) {
                    outFiles.add(rel);
                }
            } else {
                collectSpecFilesFromIndex(rel, seenDirs, outFiles);
            }
        }
    }

    private static List<String> readIndexEntries(String dir) throws Exception {
        ArrayList<String> out = new ArrayList<String>();
        InputStream in = openResourceStream(dir + "/index.list");
        if (in == null) return out;
        byte[] buf = new byte[8192];
        StringBuilder sb = new StringBuilder();
        try {
            int n;
            while ((n = in.read(buf)) >= 0) {
                if (n == 0) continue;
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
        String[] lines = sb.toString().replace("\r", "").split("\n");
        for (String line : lines) {
            if (line == null) continue;
            String clean = line;
            int tab = clean.indexOf('\t');
            if (tab >= 0) clean = clean.substring(0, tab);
            clean = clean.trim();
            if (!clean.isEmpty()) out.add(clean);
        }
        return out;
    }

    private static InputStream openResourceStream(String path) {
        try {
            Object result = resourceManagerOpenResource.invoke(resourceManagerInstance, path);
            if (result instanceof InputStream) {
                return (InputStream) result;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean copyResourceToFiles(String relPath) throws Exception {
        InputStream src = openResourceStream(relPath);
        if (src == null) return false;
        File dstFile = new File(FILES_ROOT + "/" + relPath);
        if (dstFile.exists() && dstFile.isDirectory()) {
            deleteRecursively(dstFile);
        }
        File parent = dstFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        if (dstFile.exists() && dstFile.length() > 100) {
            try {
                src.close();
            } catch (Throwable ignored) {
            }
            return false;
        }

        byte[] buf = new byte[16384];
        OutputStream out = null;
        try {
            out = new FileOutputStream(dstFile, false);
            int n;
            while ((n = src.read(buf)) >= 0) {
                if (n == 0) continue;
                out.write(buf, 0, n);
            }
            out.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
            if (src != null) {
                try {
                    src.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return true;
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File kid : kids) {
                    deleteRecursively(kid);
                }
            }
        }
        try {
            f.delete();
        } catch (Throwable ignored) {
        }
    }

    private static void primeProjectileSpecs() {
        int loaded = 0;
        int failed = 0;
        try {
            List<String> projs = new ArrayList<String>();
            synchronized (mirroredSpecPaths) {
                for (String rel : mirroredSpecPaths) {
                    if (rel != null && rel.toLowerCase().endsWith(".proj")) {
                        projs.add(rel);
                    }
                }
            }
            for (String proj : projs) {
                try {
                    com.fs.starfarer.loading.WeaponSpecLoader.o00000(proj);
                    loaded++;
                } catch (Throwable t) {
                    failed++;
                    if (failed <= 5) {
                        System.out.println("Fixer: projectile prime failed for " + proj + ": " + t);
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("Fixer: projectile prime unavailable: " + t);
            return;
        }
        System.out.println("Fixer: projectile prime complete loaded=" + loaded + " failed=" + failed);
    }

    private static void configureLog4jFallback() {
        if (!Boolean.parseBoolean(System.getProperty(LOG4J_FORCE_BASIC_PROPERTY, "false"))) {
            return;
        }
        try {
            org.apache.log4j.BasicConfigurator.configure();
            System.out.println("Fixer: log4j BasicConfigurator enabled via " + LOG4J_FORCE_BASIC_PROPERTY);
        } catch (Throwable ignored) {
        }
    }

    private static void applyStartupPreferenceOverrides() {
        if (!Boolean.parseBoolean(System.getProperty(SUPPRESS_STARTUP_PROMPTS_PROPERTY, "true"))) {
            System.out.println(
                    "Fixer: startup prompt suppression disabled via " + SUPPRESS_STARTUP_PROMPTS_PROPERTY);
            return;
        }

        String controlsVersion =
                System.getProperty(FORCE_CONTROLS_VERSION_PROPERTY, CONTROLS_VERSION_FALLBACK);
        if (controlsVersion == null || controlsVersion.trim().isEmpty()) {
            controlsVersion = CONTROLS_VERSION_FALLBACK;
        } else {
            controlsVersion = controlsVersion.trim();
        }

        try {
            Preferences prefs = Preferences.userRoot().node(SERIAL_PREF_NODE);
            prefs.putBoolean("firstGameRun", false);
            prefs.put("controlsVersion", controlsVersion);
            System.out.println(
                    "Fixer: startup prefs updated firstGameRun=false controlsVersion=" + controlsVersion);
        } catch (Throwable t) {
            System.out.println("Fixer: unable to persist startup prefs: " + t);
        }

        // Keep title-controls data in sync with controlsVersion to avoid first-launch dialogs.
        try {
            Class<?> cls = Class.forName("com.fs.starfarer.title.B.B");
            invokeStaticNoArg(cls, "return");
            invokeStaticNoArg(cls, "o00000");
            System.out.println("Fixer: title controls remap/state refresh applied.");
        } catch (Throwable t) {
            System.out.println("Fixer: unable to refresh title controls state: " + t);
        }
    }

    private static void maybeDisableLauncherWarnings() {
        if (!Boolean.parseBoolean(System.getProperty(DISABLE_LAUNCHER_WARNINGS_PROPERTY, "true"))) {
            return;
        }

        try {
            Class<?> cls =
                    Class.forName(
                            "com.fs.starfarer.launcher.opengl.GLLauncher",
                            false,
                            Fixer.class.getClassLoader());
            Field warnings = cls.getDeclaredField("WARNINGS");
            warnings.setAccessible(true);
            warnings.setBoolean(null, false);
            System.out.println(
                    "Fixer: GLLauncher.WARNINGS=false via "
                            + DISABLE_LAUNCHER_WARNINGS_PROPERTY);
        } catch (Throwable t) {
            System.out.println("Fixer: unable to disable launcher warnings: " + t);
        }
    }

    private static void invokeStaticNoArg(Class<?> cls, String name) throws Exception {
        Method m = cls.getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(null);
    }

    private static void disableShipHullSpreadsheetPostPass() {
        try {
            Class<?> cls = Class.forName("com.fs.starfarer.loading.ShipHullSpreadsheetLoader");
            java.lang.reflect.Field[] fields = cls.getDeclaredFields();
            for (java.lang.reflect.Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers()) || f.getType() != Boolean.TYPE) {
                    continue;
                }
                f.setAccessible(true);
                boolean value = f.getBoolean(null);
                if (value) {
                    f.setBoolean(null, false);
                    System.out.println("Fixer: disabled ShipHullSpreadsheetLoader static post-pass via field " + f.getName());
                    return;
                }
            }
        } catch (Throwable t) {
            System.out.println("Fixer: unable to disable ShipHullSpreadsheetLoader post-pass: " + t);
        }
    }

    private static void maybeDisableShipHullSpreadsheetPostPass() {
        if (!Boolean.parseBoolean(System.getProperty(DISABLE_SHIP_HULL_POST_PASS_PROPERTY, "false"))) {
            return;
        }
        System.out.println(
                "Fixer: "
                        + DISABLE_SHIP_HULL_POST_PASS_PROPERTY
                        + "=true, forcing ShipHullSpreadsheetLoader post-pass disable");
        disableShipHullSpreadsheetPostPass();
    }

    private static void installUncaughtExceptionLogging() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(
                    new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread thread, Throwable throwable) {
                            try {
                                System.out.println(
                                        "Fixer: uncaught exception in thread "
                                                + (thread == null ? "<null>" : thread.getName()));
                                if (throwable != null) {
                                    throwable.printStackTrace(System.out);
                                } else {
                                    System.out.println("Fixer: uncaught throwable is null.");
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
            System.out.println("Fixer: default uncaught exception handler installed.");
        } catch (Throwable t) {
            System.out.println("Fixer: unable to install uncaught exception handler: " + t);
        }
    }

    private static void maybePreloadRuleCommandClasses() {
        if (!Boolean.parseBoolean(System.getProperty(PRELOAD_RULE_COMMANDS_PROPERTY, "true"))) {
            System.out.println("Fixer: rule command preload disabled via " + PRELOAD_RULE_COMMANDS_PROPERTY);
            return;
        }

        int loaded = 0;
        int failed = 0;
        LinkedHashSet<String> classes = new LinkedHashSet<String>();
        try {
            String cp = System.getProperty("java.class.path", "");
            String[] entries = cp.split(File.pathSeparator);
            for (String entry : entries) {
                if (entry == null || !entry.endsWith(".jar")) {
                    continue;
                }
                JarFile jar = null;
                try {
                    jar = new JarFile(entry);
                    Enumeration<JarEntry> e = jar.entries();
                    while (e.hasMoreElements()) {
                        JarEntry je = e.nextElement();
                        String name = je.getName();
                        if (!name.startsWith("com/fs/starfarer/api/impl/campaign/rulecmd/")
                                || !name.endsWith(".class")
                                || name.indexOf('$') >= 0) {
                            continue;
                        }
                        String clsName = name.substring(0, name.length() - 6).replace('/', '.');
                        classes.add(clsName);
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (jar != null) {
                        try {
                            jar.close();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("Fixer: unable to enumerate rule command classes: " + t);
            return;
        }

        ClassLoader cl = Fixer.class.getClassLoader();
        for (String className : classes) {
            try {
                Class.forName(className, false, cl);
                loaded++;
            } catch (Throwable t) {
                failed++;
                if (failed <= 10) {
                    System.out.println("Fixer: failed to preload rule command " + className + ": " + t);
                }
            }
        }

        System.out.println(
                "Fixer: rule command preload complete loaded="
                        + loaded
                        + " failed="
                        + failed
                        + " discovered="
                        + classes.size());
    }

    private static void applySerialFromProperty() {
        String serial = System.getProperty(SERIAL_PROPERTY);
        String source = "property";
        if (serial == null || serial.trim().isEmpty()) {
            serial = readSerialFromFile();
            source = "file";
        }
        int serialLen = serial == null ? 0 : serial.trim().length();
        System.out.println("Fixer: serial source=" + source + " present=" + (serialLen > 0) + " len=" + serialLen);
        if (serialLen <= 0) return;
        serial = serial.trim();
        try {
            Preferences.userRoot().node(SERIAL_PREF_NODE).put(SERIAL_PREF_KEY, serial);
            System.out.println("Fixer: serial injected into preferences from " + source + ".");
        } catch (Throwable t) {
            System.out.println("Fixer: unable to inject serial into preferences: " + t);
        }
    }

    private static String readSerialFromFile() {
        try {
            if (!Files.exists(Paths.get(SERIAL_FILE_PATH))) return null;
            String raw = new String(Files.readAllBytes(Paths.get(SERIAL_FILE_PATH)), StandardCharsets.UTF_8);
            return raw == null ? null : raw.trim();
        } catch (Throwable t) {
            System.out.println("Fixer: unable to read serial file: " + t);
            return null;
        }
    }

    private static void debugSpecDirectoryListing() {
        final String[] dirs =
                new String[] {
                    "/app/starsector/starsector/data/weapons",
                    "/app/starsector/starsector/data/hulls",
                    "/app/starsector/starsector/data/shipsystems",
                    "/app/starsector/starsector/data/variants",
                    "/app/starsector/starsector/data/characters/skills",
                    "/files/data/weapons",
                    "/files/data/hulls",
                    "/files/data/shipsystems",
                    "/files/data/variants",
                    "/files/data/characters/skills"
                };
        final String[] probes =
                new String[] {
                    "/app/starsector/starsector/data/weapons/amsrm.wpn",
                    "/app/starsector/starsector/data/hulls/drone_pd.ship",
                    "/app/starsector/starsector/data/shipsystems/emp.system",
                    "/app/starsector/starsector/data/variants/ziggurat_Strike.variant",
                    "/app/starsector/starsector/data/variants/dweller/shrouded_maw_Ravenous.variant",
                    "/app/starsector/starsector/data/characters/skills/helmsmanship.skill",
                    "/files/data/weapons/amsrm.wpn",
                    "/files/data/hulls/drone_pd.ship",
                    "/files/data/shipsystems/emp.system",
                    "/files/data/variants/ziggurat_Strike.variant",
                    "/files/data/variants/dweller/shrouded_maw_Ravenous.variant",
                    "/files/data/characters/skills/helmsmanship.skill"
                };
        try {
            for (String dirPath : dirs) {
                File dir = new File(dirPath);
                File[] files = dir.listFiles();
                int total = files == null ? -1 : files.length;
                int spec = 0;
                if (files != null) {
                    for (File f : files) {
                        String n = f.getName().toLowerCase();
                        if (n.endsWith(".wpn")
                                || n.endsWith(".proj")
                                || n.endsWith(".ship")
                                || n.endsWith(".system")
                                || n.endsWith(".skin")
                                || n.endsWith(".variant")
                                || n.endsWith(".skill")) {
                            spec++;
                        }
                    }
                }
                System.out.println(
                        "Fixer: listFiles dir="
                                + dirPath
                                + " exists="
                                + dir.exists()
                                + " isDir="
                                + dir.isDirectory()
                                + " total="
                                + total
                                + " spec="
                                + spec);
                String[] variants =
                        new String[] {dirPath, dirPath + "/", dirPath + "/.", dirPath + "/./"};
                for (String variant : variants) {
                    File v = new File(variant);
                    System.out.println(
                            "Fixer: dirVariant path="
                                    + variant
                                    + " exists="
                                    + v.exists()
                                    + " isDir="
                                    + v.isDirectory()
                                    + " abs="
                                    + v.getAbsolutePath());
                }
            }
            for (String probe : probes) {
                File f = new File(probe);
                System.out.println(
                        "Fixer: file probe="
                                + probe
                                + " exists="
                                + f.exists()
                                + " len="
                                + (f.exists() ? f.length() : -1));
            }

            try {
                printLoadingUtilsList("data/weapons", "wpn");
                printLoadingUtilsList("data/hulls", "ship");
                printLoadingUtilsList("data/shipsystems", "system");
                printLoadingUtilsList("data/variants", "variant");
                printLoadingUtilsList("data/characters/skills", "skill");
            } catch (Throwable t) {
                System.out.println("Fixer: LoadingUtils list diagnostics failed: " + t);
            }
        } catch (Throwable t) {
            System.out.println("Fixer: spec directory diagnostics failed: " + t);
        }
    }

    private static void printLoadingUtilsList(String dir, String suffix) throws Exception {
        Class<?> cls = Class.forName("com.fs.starfarer.loading.LoadingUtils");
        Method target = null;
        for (Method m : cls.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (!java.util.List.class.isAssignableFrom(m.getReturnType())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0] == String.class && p[1] == String.class) {
                target = m;
                break;
            }
        }
        if (target == null) {
            throw new RuntimeException("2-arg String list method not found on LoadingUtils");
        }
        target.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<String> list = (java.util.List<String>) target.invoke(null, dir, suffix);
        int size = list == null ? -1 : list.size();
        String sample = "";
        if (list != null && !list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int n = Math.min(3, list.size());
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                sb.append(list.get(i));
            }
            sample = sb.toString();
        }
        System.out.println(
                "Fixer: LoadingUtils list dir="
                        + dir
                        + " suffix="
                        + suffix
                        + " size="
                        + size
                        + " sample=["
                        + sample
                        + "]");
    }
}
