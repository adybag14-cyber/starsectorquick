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
import java.net.URL;
import java.net.URLClassLoader;
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
    private static final String AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_OWNER_PROPERTY =
            "starsector.autoCampaignDirectNewGameWorkerLeaseOwner";
    private static final String AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_AT_PROPERTY =
            "starsector.autoCampaignDirectNewGameWorkerLeaseAt";
    private static final String AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_OWNER_PROPERTY =
            "starsector.autoCampaignDirectCreateLeaseOwner";
    private static final String AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_AT_PROPERTY =
            "starsector.autoCampaignDirectCreateLeaseAt";
    private static final String AUTO_CAMPAIGN_DIRECT_CREATE_INVOKE_AT_PROPERTY =
            "starsector.autoCampaignDirectCreateInvokeAt";
    private static final String AUTO_CAMPAIGN_DIRECT_CREATE_SETTLE_MS_PROPERTY =
            "starsector.autoCampaignDirectCreateSettleMs";
    private static final String AUTO_CAMPAIGN_WATCHER_LEASE_OWNER_PROPERTY =
            "starsector.autoCampaignWatcherLeaseOwner";
    private static final String AUTO_CAMPAIGN_WATCHER_LEASE_AT_PROPERTY =
            "starsector.autoCampaignWatcherLeaseAt";
    private static final String AUTO_CAMPAIGN_MUTATING_SPEC_PREFLIGHT_PROPERTY =
            "starsector.autoCampaignMutatingSpecPreflight";
    private static final String AUTO_CAMPAIGN_NON_MUTATING_ORBITAL_JUNK_FALLBACK_PROPERTY =
            "starsector.autoCampaignAllowOrbitalJunkFallbackWhenNonMutating";
    private static final String DEBUG_THREAD_DUMP_AFTER_MS_PROPERTY =
            "starsector.debugThreadDumpAfterMs";
    private static final String DEBUG_THREAD_DUMP_REPEAT_PROPERTY =
            "starsector.debugThreadDumpRepeat";
    private static final String DEBUG_THREAD_DUMP_REPEAT_MS_PROPERTY =
            "starsector.debugThreadDumpRepeatMs";
    private static final String AUTO_CAMPAIGN_ORBITAL_SPEC_WATCHDOG_PROPERTY =
            "starsector.autoCampaignOrbitalSpecWatchdog";
    private static final String ALLOW_THREAD_LOADER_SCAN_PROPERTY =
            "starsector.allowThreadLoaderScan";
    private static final String USER_DIR_OVERRIDE_PROPERTY = "starsector.userDir";
    private static final String SPEC_DIAGNOSTICS_PROPERTY = "starsector.specDiagnostics";
    private static final String CAMPAIGN_SESSION_KEY = "campaign state in session";
    private static final String TITLE_STATE_ID = "Title Screen State";
    private static final String CAMPAIGN_STATE_ID = "Campaign State";
    private static final String CONTROLS_VERSION_FALLBACK = "6.3";
    private static final String LOG4J_CONFIG_PROPERTY = "starsector.log4jConfig";
    private static final String LOG4J_FORCE_BASIC_PROPERTY = "starsector.forceBasicLog4j";
    private static final String NPEFIX_NATIVE_PROPERTY = "starsector.npefix.native";
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
    private static long autoCampaignSectorBootstrapAttemptAt = 0L;
    private static int autoCampaignSectorBootstrapAttempts = 0;
    private static volatile ClassLoader autoCampaignSectorGenFallbackLoader;
    private static volatile String autoCampaignSectorGenClassSource = "unresolved";
    private static long autoCampaignColonyNoMarketsLogAt = 0L;
    private static String autoCampaignColonyNoMarketsSignature = null;
    private static long autoCampaignPlayerFleetNullTransitionDeferredLogAt = 0L;
    private static long autoCampaignPlayerFleetNullTransitionDeferredSince = 0L;
    private static long autoCampaignSyntheticPlayerFleetLogAt = 0L;
    private static String autoCampaignSyntheticPlayerFleetSignature = null;
    private static long autoCampaignDialogPluginFallbackLogAt = 0L;
    private static String autoCampaignDialogPluginFallbackSignature = null;
    private static long autoCampaignFallbackMarketLogAt = 0L;
    private static String autoCampaignFallbackMarketSignature = null;
    private static boolean fallbackMarketSpecPreflightAttempted = false;
    private static String fallbackMarketSpecPreflightSignature = null;
    private static boolean threadLoaderScanDisabledLogged = false;
    private static boolean directNewGameOrbitalEntityCtorProbeDisabledLogged = false;
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
    private static String directNewGameWorkerMarker;
    private static int directNewGameWorkerId = 0;
    private static int directNewGameWorkerActiveId = 0;
    private static String directNewGameWorkerLeaseOwner;
    private static long directNewGameWorkerPendingLogAt = 0L;
    private static volatile Object lastDirectNewGameData;
    private static volatile String directNewGameLastNullMarker;
    private static volatile long lastDirectNewGameCreateInvokeAt = 0L;
    private static long autoCampaignDirectCreateSettleLogAt = 0L;
    private static long autoCampaignDirectCreateLeaseLogAt = 0L;
    private static volatile long lastCampaignStateCreateInvokeAt = 0L;
    private static long autoCampaignCampaignStateCreateCooldownLogAt = 0L;
    private static final Object AUTO_CAMPAIGN_COLONY_PROBE_LOCK = new Object();
    private static Thread autoCampaignColonyProbeWorker;
    private static long autoCampaignColonyProbeStartedAt = 0L;
    private static boolean autoCampaignColonyProbeCompleted = false;
    private static String autoCampaignColonyProbeResult;
    private static Throwable autoCampaignColonyProbeError;
    private static int autoCampaignColonyProbeWorkerId = 0;
    private static int autoCampaignColonyProbeActiveId = 0;
    private static long autoCampaignColonyProbePendingLogAt = 0L;
    private static volatile boolean autoCampaignColonyDialogSafeMode = false;
    private static final Object AUTO_CAMPAIGN_COLONY_VISIT_WORKER_LOCK = new Object();
    private static Thread autoCampaignColonyVisitWorker;
    private static long autoCampaignColonyVisitWorkerStartedAt = 0L;
    private static int autoCampaignColonyVisitWorkerId = 0;
    private static int autoCampaignColonyVisitWorkerActiveId = 0;
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
        System.out.println("Fixer: build marker synthdiag-20260215-1");
        String resolvedUserDir = resolveRuntimeUserDir();
        System.setProperty("user.dir", resolvedUserDir);
        System.setProperty(USER_DIR_OVERRIDE_PROPERTY, resolvedUserDir);
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
        logLwjglSysClockDiagnostics();
        patchGraphicsTimerResolutionGuard();

        configureFilesystemPaths();

        if (Boolean.parseBoolean(System.getProperty(NPEFIX_NATIVE_PROPERTY, "false"))) {
            loadNpeFixNativeLibrary();
        } else {
            System.out.println(
                    "Fixer: skipping optional npefix native library load "
                            + "(set -D"
                            + NPEFIX_NATIVE_PROPERTY
                            + "=true to enable).");
        }

        initializeResourceManager();
        if (Boolean.parseBoolean(System.getProperty("starsector.mirrorCoreSpecsToFiles", "true"))) {
            mirrorCoreSpecDirectoriesToFiles();
        } else {
            System.out.println(
                    "Fixer: core spec mirroring to /files disabled "
                            + "(set -Dstarsector.mirrorCoreSpecsToFiles=true to enable, false to disable).");
        }
        installUncaughtExceptionLogging();
        maybeStartThreadDumpWatchdog();
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

    private static void logLwjglSysClockDiagnostics() {
        try {
            Class<?> sysClass = Class.forName("org.lwjgl.Sys");
            Method getTimerResolution = sysClass.getMethod("getTimerResolution");
            Method getTime = sysClass.getMethod("getTime");
            long timerResolution = ((Long) getTimerResolution.invoke(null)).longValue();
            long t0 = ((Long) getTime.invoke(null)).longValue();
            long t1 = ((Long) getTime.invoke(null)).longValue();
            System.out.println(
                    "Fixer: LWJGL Sys clock timerResolution="
                            + timerResolution
                            + " t0="
                            + t0
                            + " t1="
                            + t1);
        } catch (Throwable t) {
            System.out.println("Fixer: LWJGL Sys clock diagnostics failed: " + t);
        }
    }

    private static void patchGraphicsTimerResolutionGuard() {
        try {
            Class<?> graphicsUtilClass = Class.forName("com.fs.graphics.util.B");
            Field[] fields = graphicsUtilClass.getDeclaredFields();
            Field timerResolutionField = null;
            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Long.TYPE) {
                    continue;
                }
                if ("super".equals(field.getName())) {
                    timerResolutionField = field;
                    break;
                }
            }
            if (timerResolutionField == null) {
                System.out.println(
                        "Fixer: graphics timer guard skipped (timer resolution field not found).");
                return;
            }
            timerResolutionField.setAccessible(true);
            long timerResolution = timerResolutionField.getLong(null);
            if (timerResolution > 0L) {
                System.out.println(
                        "Fixer: graphics timer guard not needed (timerResolution="
                                + timerResolution
                                + ").");
                return;
            }
            timerResolutionField.setLong(null, 1000L);
            System.out.println(
                    "Fixer: graphics timer guard patched com.fs.graphics.util.B."
                            + timerResolutionField.getName()
                            + "="
                            + timerResolution
                            + " -> 1000");
        } catch (Throwable t) {
            System.out.println("Fixer: graphics timer guard failed: " + t);
        }
    }

    private static void maybeStartThreadDumpWatchdog() {
        final long delayMs = Math.max(0L, parseLongProperty(DEBUG_THREAD_DUMP_AFTER_MS_PROPERTY, 0L));
        if (delayMs <= 0L) {
            return;
        }
        final boolean repeat =
                Boolean.parseBoolean(System.getProperty(DEBUG_THREAD_DUMP_REPEAT_PROPERTY, "false"));
        final long repeatMs =
                Math.max(10000L, parseLongProperty(DEBUG_THREAD_DUMP_REPEAT_MS_PROPERTY, 60000L));
        Thread dumpThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(delayMs);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                                dumpAllThreads("delay-elapsed");
                                while (repeat) {
                                    try {
                                        Thread.sleep(repeatMs);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                    dumpAllThreads("repeat");
                                }
                            }
                        },
                        "fixer-thread-dump-watchdog");
        dumpThread.setDaemon(true);
        dumpThread.start();
        System.out.println(
                "Fixer: debug thread dump watchdog enabled delay="
                        + delayMs
                        + "ms repeat="
                        + repeat
                        + " repeatMs="
                        + repeatMs
                        + "ms.");
    }

    private static void dumpAllThreads(String reason) {
        try {
            Map traces = Thread.getAllStackTraces();
            int size = traces == null ? 0 : traces.size();
            System.out.println(
                    "Fixer: thread dump begin reason=" + reason + " threads=" + size);
            if (traces == null) {
                System.out.println("Fixer: thread dump end reason=" + reason);
                return;
            }
            for (Object entryObj : traces.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObj;
                Thread thread = (Thread) entry.getKey();
                StackTraceElement[] stack = (StackTraceElement[]) entry.getValue();
                if (thread == null) {
                    continue;
                }
                System.out.println(
                        "Fixer: thread "
                                + thread.getName()
                                + " id="
                                + thread.getId()
                                + " state="
                                + thread.getState()
                                + " daemon="
                                + thread.isDaemon());
                if (stack != null) {
                    int limit = Math.min(stack.length, 48);
                    for (int i = 0; i < limit; i++) {
                        System.out.println("Fixer:   at " + stack[i]);
                    }
                    if (stack.length > limit) {
                        System.out.println(
                                "Fixer:   ... " + (stack.length - limit) + " more frames");
                    }
                }
            }
            System.out.println("Fixer: thread dump end reason=" + reason);
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: thread dump failed reason="
                            + reason
                            + " error="
                            + describeThrowableChain(t));
            dumpAllThreadsFallback(reason);
        }
    }

    private static void dumpAllThreadsFallback(String reason) {
        try {
            ThreadGroup root = Thread.currentThread().getThreadGroup();
            while (root != null && root.getParent() != null) {
                root = root.getParent();
            }
            if (root == null) {
                System.out.println("Fixer: thread fallback dump unavailable (no root group).");
                return;
            }
            int alloc = Math.max(64, root.activeCount() * 2 + 32);
            Thread[] threads = new Thread[alloc];
            int count = root.enumerate(threads, true);
            System.out.println(
                    "Fixer: thread fallback dump begin reason="
                            + reason
                            + " threads="
                            + count);
            for (int i = 0; i < count && i < threads.length; i++) {
                Thread thread = threads[i];
                if (thread == null) {
                    continue;
                }
                System.out.println(
                        "Fixer: fallback thread "
                                + thread.getName()
                                + " id="
                                + thread.getId()
                                + " state="
                                + thread.getState()
                                + " daemon="
                                + thread.isDaemon());
                try {
                    StackTraceElement[] stack = thread.getStackTrace();
                    if (stack == null) {
                        continue;
                    }
                    int limit = Math.min(stack.length, 48);
                    for (int j = 0; j < limit; j++) {
                        System.out.println("Fixer:   at " + stack[j]);
                    }
                    if (stack.length > limit) {
                        System.out.println(
                                "Fixer:   ... " + (stack.length - limit) + " more frames");
                    }
                } catch (Throwable stackErr) {
                    System.out.println(
                            "Fixer:   stack unavailable: " + describeThrowableChain(stackErr));
                }
            }
            System.out.println("Fixer: thread fallback dump end reason=" + reason);
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: thread fallback dump failed reason="
                            + reason
                            + " error="
                            + describeThrowableChain(t));
        }
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
            final String watcherGlobalLeaseOwner =
                    "fixer-auto-campaign-"
                            + watcherSerial
                            + "@"
                            + System.currentTimeMillis()
                            + "/cl@"
                            + Integer.toHexString(
                                    System.identityHashCode(Fixer.class.getClassLoader()));
            final long watcherGlobalLeaseMs = Math.max(120000L, timeoutMs + 120000L);
            if (!claimAutoCampaignWatcherLease(
                    watcherGlobalLeaseOwner, System.currentTimeMillis(), watcherGlobalLeaseMs)) {
                System.out.println(
                        "Fixer: auto campaign global watcher lease is active; skipping duplicate start.");
                return;
            }
            autoCampaignWatcherThread =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        runAutoCampaignWatcher(mode, timeoutMs, pollMs, fallbackMs);
                                    } finally {
                                        releaseAutoCampaignWatcherLease(watcherGlobalLeaseOwner);
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
        String normalizedMode = mode == null ? "" : mode.toLowerCase();
        final boolean visitColonyMode = normalizedMode.indexOf("visit_colony") >= 0;
        final Thread watcherThread = Thread.currentThread();
        final String watcherLeaseOwner =
                watcherThread.getName()
                        + "#"
                        + watcherThread.getId()
                        + "@"
                        + String.valueOf(watcherStartedAt)
                        + "/cl@"
                        + Integer.toHexString(
                                System.identityHashCode(Fixer.class.getClassLoader()));
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
                                "starsector.autoCampaignDirectTransitionForceAfterMs",
                                visitColonyMode ? 120000L : 30000L));
        final long campaignTransitionAttemptCooldownMs =
                Math.max(
                        1000L,
                        parseLongProperty(
                                "starsector.autoCampaignTransitionAttemptCooldownMs",
                                visitColonyMode ? 10000L : 5000L));
        final long passiveDirectRearmAfterMs =
                Math.max(
                        15000L,
                        parseLongProperty(
                                "starsector.autoCampaignPassiveDirectRearmAfterMs", 120000L));
        final int passiveDirectRearmLimit =
                Math.max(
                        0,
                        parseIntProperty(
                                "starsector.autoCampaignPassiveDirectRearmLimit",
                                visitColonyMode ? 0 : 2));
        final long directSectorNullRearmAfterMs =
                Math.max(
                        30000L,
                        parseLongProperty(
                                "starsector.autoCampaignDirectSectorNullRearmAfterMs", 60000L));
        final int directSectorNullRearmLimit =
                Math.max(
                        0,
                        parseIntProperty(
                                "starsector.autoCampaignDirectSectorNullRearmLimit",
                                visitColonyMode ? 0 : 3));
        final long directNoAdvanceRearmAfterMs =
                Math.max(
                        30000L,
                        parseLongProperty(
                                "starsector.autoCampaignDirectNoAdvanceRearmAfterMs", 45000L));
        final int directNoAdvanceRearmAttemptLimit =
                Math.max(
                        0,
                        parseIntProperty(
                                "starsector.autoCampaignDirectNoAdvanceRearmAttempts",
                                visitColonyMode ? 0 : 3));
        final boolean allowModeDirectEscape =
                Boolean.parseBoolean(
                        System.getProperty(AUTO_CAMPAIGN_ALLOW_DIRECT_ESCAPE_PROPERTY, "false"));
        final boolean passiveCampaignStateTransitions =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignPassiveTransitions", "false"));
        final boolean directLaunchMode =
                Boolean.parseBoolean(
                        System.getProperty(DIRECT_LAUNCH_PROPERTY, "false"));
        final String immediatePlayerFleetNullDefault = visitColonyMode ? "true" : "false";
        final boolean allowImmediatePlayerFleetNullTransition =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignAllowImmediatePlayerFleetNullTransition",
                                immediatePlayerFleetNullDefault));
        final boolean allowUnsafeDirectContinueState =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignUnsafeDirectContinueState",
                                "false"));
        boolean disableDirectNewGame =
                normalizedMode.indexOf("no_direct") >= 0
                        || normalizedMode.indexOf("nodirect") >= 0
                        || normalizedMode.indexOf("menu_only") >= 0
                        || normalizedMode.indexOf("menuonly") >= 0;
        final boolean modeDisablesDirectNewGame = disableDirectNewGame;
        boolean enableColonyVisit =
                visitColonyMode
                        || Boolean.parseBoolean(System.getProperty(AUTO_VISIT_COLONY_PROPERTY, "false"));
        System.out.println(
                "Fixer: auto campaign transition config passive="
                        + passiveCampaignStateTransitions
                        + " allowImmediatePlayerFleetNull="
                        + allowImmediatePlayerFleetNullTransition
                        + " enableColonyVisit="
                        + enableColonyVisit
                        + " mode="
                        + normalizedMode
                        + " leaseOwner="
                        + watcherLeaseOwner);
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
        boolean colonyProbeSequenceStarted = false;
        autoCampaignColonyDialogSafeMode = false;
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
        long campaignNoMarketsSince = -1L;
        long lastCampaignNoMarketsRecoveryAttemptAt = 0L;
        int campaignNoMarketsRecoveryAttempts = 0;
        long campaignPlayerFleetNullSince = -1L;
        long lastCampaignPlayerFleetNullRecoveryAttemptAt = 0L;
        int campaignPlayerFleetNullRecoveryAttempts = 0;
        long campaignStateSince = -1L;
        boolean campaignStateResourcesEnsured = false;
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
                    if (campaignStateSince <= 0L) {
                        campaignStateSince = System.currentTimeMillis();
                        campaignStateResourcesEnsured = false;
                    }
                    if (!campaignStateResourcesEnsured) {
                        ensureCampaignStateResources();
                        campaignStateResourcesEnsured = true;
                    }
                    if (!enableColonyVisit) {
                        System.out.println("Fixer: auto campaign watcher reached Campaign State.");
                        return;
                    }
                    boolean useDeferredColonyWorker =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignDeferredColonyWorker", "true"));
                    if (useDeferredColonyWorker) {
                        long colonyProbeDelayMs =
                                Math.max(
                                        0L,
                                        parseLongProperty(
                                                "starsector.autoCampaignColonyProbeDelayMs", 8000L));
                        long colonyProbeTimeoutMs =
                                Math.max(
                                        1000L,
                                        parseLongProperty(
                                                "starsector.autoCampaignColonyProbeTimeoutMs", 2500L));
                        String deferredQueueResult =
                                startDeferredColonyVisitWorker(
                                        colonyProbeDelayMs, colonyProbeTimeoutMs);
                        if (deferredQueueResult == null) {
                            System.out.println(
                                    "Fixer: auto campaign queued deferred colony probe (delay="
                                            + colonyProbeDelayMs
                                            + "ms, timeout="
                                            + colonyProbeTimeoutMs
                                            + "ms).");
                        } else {
                            System.out.println(
                                    "Fixer: auto campaign deferred colony probe status: "
                                            + deferredQueueResult);
                        }
                        return;
                    }
                    long now = System.currentTimeMillis();
                    long colonyProbeDelayMs =
                            Math.max(
                                    0L,
                                    parseLongProperty(
                                            "starsector.autoCampaignColonyProbeDelayMs", 8000L));
                    long inCampaignMs = Math.max(0L, now - campaignStateSince);
                    if (!colonyVisitDone && inCampaignMs < colonyProbeDelayMs) {
                        if (now - campaignTransitionPendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign waiting before colony probe after Campaign State entry (remaining="
                                            + (colonyProbeDelayMs - inCampaignMs)
                                            + "ms).");
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
                    if (!colonyVisitDone && !colonyProbeSequenceStarted) {
                        System.out.println(
                                "Fixer: auto campaign entering colony probe sequence in Campaign State.");
                        colonyProbeSequenceStarted = true;
                    }
                    String campaignCreateSettleIssue = checkDirectNewGameCreateSettleWindow();
                    if (campaignCreateSettleIssue != null) {
                        if (now - campaignTransitionPendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign delaying colony-target probing until direct create settles: "
                                            + campaignCreateSettleIssue);
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
                    if (!colonyVisitDone
                            && (colonyVisitAttempts == 0 || (now - lastColonyVisitAttemptAt) >= 3000L)) {
                        lastColonyVisitAttemptAt = now;
                        long colonyProbeTimeoutMs =
                                Math.max(
                                        1000L,
                                        parseLongProperty(
                                                "starsector.autoCampaignColonyProbeTimeoutMs", 2500L));
                        String result = tryPrimeColonyInteractionTargetWithTimeout(colonyProbeTimeoutMs);
                        if (result == null) {
                            colonyVisitAttempts++;
                            colonyVisitDone = true;
                            campaignNoMarketsSince = -1L;
                            System.out.println(
                                    "Fixer: auto campaign watcher primed colony visit target (attempt "
                                            + colonyVisitAttempts
                                            + ").");
                            return;
                        } else if (isPendingColonyTargetResult(result)) {
                            System.out.println(
                                    "Fixer: auto campaign colony-target pending: " + result);
                            String pendingLower = result.toLowerCase();
                            if (pendingLower.indexOf("no-markets-available") >= 0) {
                                campaignPlayerFleetNullSince = -1L;
                                if (campaignNoMarketsSince <= 0L) {
                                    campaignNoMarketsSince = now;
                                }
                                long stalledMs = now - campaignNoMarketsSince;
                                long recoveryCooldownMs = Math.max(15000L, directAttemptTimeoutMs);
                                if (stalledMs >= 15000L
                                        && (now - lastCampaignNoMarketsRecoveryAttemptAt)
                                                >= recoveryCooldownMs) {
                                    lastCampaignNoMarketsRecoveryAttemptAt = now;
                                    campaignNoMarketsRecoveryAttempts++;
                                    String recoveryResult =
                                            tryStartDirectNewGameWithTimeout(
                                                    ctx, currentState, directAttemptTimeoutMs);
                                    if (recoveryResult == null) {
                                        System.out.println(
                                                "Fixer: auto campaign no-markets recovery triggered direct new-game create attempt "
                                                        + campaignNoMarketsRecoveryAttempts
                                                        + " after stall="
                                                        + stalledMs
                                                        + "ms.");
                                    } else if (isPendingNewGamePreflightFailure(recoveryResult)) {
                                        maybeLogNewGamePreflightIssue(
                                                "campaign-state no-markets recovery pending: "
                                                        + recoveryResult);
                                    } else {
                                        System.out.println(
                                                "Fixer: auto campaign no-markets recovery attempt "
                                                        + campaignNoMarketsRecoveryAttempts
                                                        + " returned: "
                                                        + recoveryResult);
                                    }
                                }
                            } else if (pendingLower.indexOf("player-fleet-null") >= 0) {
                                campaignNoMarketsSince = -1L;
                                if (campaignPlayerFleetNullSince <= 0L) {
                                    campaignPlayerFleetNullSince = now;
                                }
                                long stalledMs = now - campaignPlayerFleetNullSince;
                                long recoveryCooldownMs = Math.max(15000L, directAttemptTimeoutMs);
                                if (stalledMs >= 15000L
                                        && (now - lastCampaignPlayerFleetNullRecoveryAttemptAt)
                                                >= recoveryCooldownMs) {
                                    lastCampaignPlayerFleetNullRecoveryAttemptAt = now;
                                    campaignPlayerFleetNullRecoveryAttempts++;
                                    String recoveryResult =
                                            tryStartDirectNewGameWithTimeout(
                                                    ctx, currentState, directAttemptTimeoutMs);
                                    if (recoveryResult == null) {
                                        System.out.println(
                                                "Fixer: auto campaign player-fleet-null recovery triggered direct new-game create attempt "
                                                        + campaignPlayerFleetNullRecoveryAttempts
                                                        + " after stall="
                                                        + stalledMs
                                                        + "ms.");
                                    } else if (isPendingNewGamePreflightFailure(recoveryResult)) {
                                        maybeLogNewGamePreflightIssue(
                                                "campaign-state player-fleet-null recovery pending: "
                                                        + recoveryResult);
                                    } else {
                                        System.out.println(
                                                "Fixer: auto campaign player-fleet-null recovery attempt "
                                                        + campaignPlayerFleetNullRecoveryAttempts
                                                        + " returned: "
                                                        + recoveryResult);
                                    }
                                }
                            } else {
                                campaignNoMarketsSince = -1L;
                                campaignPlayerFleetNullSince = -1L;
                            }
                        } else {
                            campaignNoMarketsSince = -1L;
                            campaignPlayerFleetNullSince = -1L;
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
                    try {
                        Thread.sleep(pollMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }
                campaignStateSince = -1L;
                campaignStateResourcesEnsured = false;
                Object titleState =
                        isTitleState(stateId, currentState) ? currentState : titleStateFromMap;
                long now = System.currentTimeMillis();
                boolean titleStateAvailable = isTitleState(stateId, titleState) && titleState != null;
                boolean allowTransitionWithoutTitleState =
                        !passiveCampaignStateTransitions && newGameStarted;
                if (!titleStateAvailable && !allowTransitionWithoutTitleState) {
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
                if (!titleStateAvailable
                        && allowTransitionWithoutTitleState
                        && now - campaignTransitionPendingLogAt >= 5000L) {
                    System.out.println(
                            "Fixer: auto campaign title-state unavailable after direct new-game; attempting Campaign State transition via driver fallback.");
                    campaignTransitionPendingLogAt = now;
                }
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

                if (!passiveCampaignStateTransitions
                        && newGameStarted
                        && !isCampaignState(stateId, currentState)) {
                    long sinceDirectNewGameSuccess =
                            directNewGameSuccessAt > 0L ? (now - directNewGameSuccessAt) : 0L;
                    long transitionReadinessCheckTimeoutMs =
                            Math.max(
                                    250L,
                                    Math.min(
                                            5000L,
                                            parseLongProperty(
                                                    "starsector.autoCampaignTransitionReadinessCheckTimeoutMs",
                                                    1500L)));
                    String createSettleIssueBeforeTransition = checkDirectNewGameCreateSettleWindow();
                    if (createSettleIssueBeforeTransition != null) {
                        if (now - campaignTransitionPendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign waiting for direct create settle before transition checks: "
                                            + createSettleIssueBeforeTransition);
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
                    String transitionReadinessIssue =
                            checkCampaignStateTransitionReadinessWithTimeout(
                                    ctx, transitionReadinessCheckTimeoutMs);
                    boolean playerFleetNullTransitionIssue =
                            transitionReadinessIssue != null
                                    && transitionReadinessIssue
                                            .toLowerCase()
                                            .indexOf("player-fleet-null")
                                            >= 0;
                    boolean allowTransitionForPlayerFleetNull =
                            playerFleetNullTransitionIssue
                                    && shouldAllowImmediatePlayerFleetNullTransition(
                                            allowImmediatePlayerFleetNullTransition, ctx);
                    boolean sectorNullTransitionIssue =
                            transitionReadinessIssue != null
                                    && transitionReadinessIssue.toLowerCase().indexOf("sector-null")
                                            >= 0;
                    boolean skipSectorNullReadyDueToPreflight =
                            sectorNullTransitionIssue && isNewGamePreflightPendingRecently(now);
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
                    } else if (skipSectorNullReadyDueToPreflight) {
                        if (now - campaignTransitionPendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign waiting for new-game preflight resolution before treating sector-null readiness as committed.");
                            campaignTransitionPendingLogAt = now;
                        }
                        newGameStarted = false;
                        directNewGameSuccessAt = -1L;
                        continueTriggered = false;
                        continueAt = -1L;
                        continueNoTransitionFailures = 0;
                        releaseDirectNewGameLease(watcherLeaseOwner);
                        resetAutoCampaignDirectNewGameCommittedAt();
                        directNewGameSuppressedLogged = false;
                        lastCampaignTransitionAttemptAt = 0L;
                        autoCampaignLastNewGamePreflightIssue = null;
                        autoCampaignNewGamePreflightLogAt = 0L;
                        continue;
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
                                        || normalizedMode.indexOf("new") < 0)
                                && !(visitColonyMode && normalizedMode.indexOf("new") >= 0);
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
                                        newGameMenuDisabled = true;
                                        if (disableDirectNewGame
                                                && (!modeDisablesDirectNewGame
                                                        || allowModeDirectEscape)) {
                                            disableDirectNewGame = false;
                                            directNewGameSuppressedLogged = false;
                                        }
                                        System.out.println(
                                                "Fixer: auto campaign disabling New Game menu automation immediately for signature "
                                                        + signature
                                                        + " and switching to direct new-game fallback.");
                                        continue;
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
                                long transitionReadinessCheckTimeoutMs =
                                        Math.max(
                                                250L,
                                                Math.min(
                                                        5000L,
                                                        parseLongProperty(
                                                                "starsector.autoCampaignTransitionReadinessCheckTimeoutMs",
                                                                1500L)));
                                String transitionReadinessIssue =
                                        checkCampaignStateTransitionReadinessWithTimeout(
                                                ctx, transitionReadinessCheckTimeoutMs);
                                if (transitionReadinessIssue != null) {
                                    String transitionReadinessIssueLower =
                                            transitionReadinessIssue.toLowerCase();
                                    boolean playerFleetNullReadiness =
                                            transitionReadinessIssueLower.indexOf("player-fleet-null")
                                                    >= 0;
                                    boolean sectorNullReadiness =
                                            transitionReadinessIssueLower.indexOf("sector-null")
                                                    >= 0;
                                    if (sectorNullReadiness) {
                                        newGameStarted = false;
                                        directNewGameSuccessAt = -1L;
                                        resetAutoCampaignDirectNewGameCommittedAt();
                                    }
                                    if (playerFleetNullReadiness
                                            && now - campaignTransitionPendingLogAt >= 5000L) {
                                        newGameStarted = true;
                                        if (directNewGameSuccessAt <= 0L) {
                                            directNewGameSuccessAt = now;
                                        }
                                        publishAutoCampaignDirectNewGameCommittedAt(now);
                                        System.out.println(
                                                "Fixer: direct new-game readiness is player-fleet-null; keeping direct-init committed so forced Campaign State transition can proceed.");
                                        campaignTransitionPendingLogAt = now;
                                    }
                                    if (sectorNullReadiness
                                            && now - campaignTransitionPendingLogAt >= 5000L) {
                                        System.out.println(
                                                "Fixer: direct new-game reached sector-null readiness; keeping direct init retry path active.");
                                    }
                                    if (sectorNullReadiness
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
                                    boolean immediateTransitionAllowed =
                                            (playerFleetNullReadiness
                                                            && shouldAllowImmediatePlayerFleetNullTransition(
                                                                    allowImmediatePlayerFleetNullTransition,
                                                                    ctx))
                                                    || isImmediateCampaignTransitionReadinessIssue(
                                                            transitionReadinessIssue);
                                    if (!passiveCampaignStateTransitions
                                            && immediateTransitionAllowed
                                            && now - lastCampaignTransitionAttemptAt
                                                    >= campaignTransitionAttemptCooldownMs) {
                                        lastCampaignTransitionAttemptAt = now;
                                        if (setCampaignStateIfPossible(ctx, titleState)) {
                                            continueTriggered = true;
                                            continueAt = now;
                                            continueNoTransitionFailures = 0;
                                        } else if (now - campaignTransitionPendingLogAt >= 5000L) {
                                            System.out.println(
                                                    "Fixer: direct new-game immediate Campaign State transition attempt did not invoke goToState.");
                                            campaignTransitionPendingLogAt = now;
                                        }
                                    } else if (!passiveCampaignStateTransitions
                                            && !immediateTransitionAllowed
                                            && now - campaignTransitionPendingLogAt >= 5000L) {
                                        System.out.println(
                                                "Fixer: direct new-game transition deferred until readiness improves: "
                                                        + transitionReadinessIssue);
                                        campaignTransitionPendingLogAt = now;
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
        addThreadContextLoadersIfAllowed(loaders);

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
            Class<?> driverClass = com.fs.state.AppDriver.class;
            // Do not call AppDriver.getInstance() here: forcing instance creation on the wrong
            // classloader can strand the watcher on an empty driver context.
            Object driver = readStaticFieldRecursive(driverClass, "instance");
            if (driver == null) {
                return null;
            }
            Object currentState = null;
            try {
                Method getCurrentState = findMethodRecursive(driverClass, "getCurrentState");
                if (getCurrentState != null) {
                    getCurrentState.setAccessible(true);
                    currentState = getCurrentState.invoke(driver);
                }
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
                    driverClass,
                    currentState,
                    driverClass.getClassLoader(),
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
            addThreadContextLoadersIfAllowed(loaders);
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

    private static boolean shouldScanThreadContextLoaders() {
        String override = System.getProperty(ALLOW_THREAD_LOADER_SCAN_PROPERTY, "").trim();
        if (!override.isEmpty()) {
            return Boolean.parseBoolean(override);
        }
        String vmVendor = System.getProperty(JAVA_VM_VENDOR_PROPERTY, "");
        String vmName = System.getProperty(JAVA_VM_NAME_PROPERTY, "");
        String haystack = (vmVendor + " " + vmName).toLowerCase();
        // Thread.getAllStackTraces() is expensive/unimplemented in CheerpJ runtimes.
        return haystack.indexOf("cheerpj") < 0;
    }

    private static void addThreadContextLoadersIfAllowed(LinkedHashSet<ClassLoader> loaders) {
        if (loaders == null) {
            return;
        }
        if (!shouldScanThreadContextLoaders()) {
            if (!threadLoaderScanDisabledLogged) {
                threadLoaderScanDisabledLogged = true;
                System.out.println(
                        "Fixer: thread context loader scan disabled (set -D"
                                + ALLOW_THREAD_LOADER_SCAN_PROPERTY
                                + "=true to enable).");
            }
            return;
        }
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t != null) {
                    loaders.add(t.getContextClassLoader());
                }
            }
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
        addThreadContextLoadersIfAllowed(loaders);

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
                String zigguratIssue = ensureSingleVariantPresentForFactionWarmup("ziggurat_Strike");
                if (zigguratIssue != null && shouldAttemptWarmupVariantAlias(zigguratIssue)) {
                    String aliasIssue = ensureVariantAliasForWarmup("ziggurat_Strike", "lasher_Standard");
                    if (aliasIssue == null) {
                        zigguratIssue = null;
                    } else {
                        zigguratIssue = zigguratIssue + ";variant-alias=" + aliasIssue;
                    }
                }
                if (zigguratIssue != null) {
                    System.out.println(
                            "Fixer: auto campaign ziggurat variant preflight issue before menu New Game: "
                                    + zigguratIssue);
                }
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
                            "Fixer: auto campaign skipping New Game faction warmup; running non-mutating faction preflight.");
                    String factionIssue = ensureFactionSpecsReadyForDirectNewGame(false);
                    if (factionIssue != null) {
                        maybeLogNewGamePreflightIssue(factionIssue);
                        System.out.println(
                                "Fixer: auto campaign proceeding with menu New Game despite non-mutating faction preflight issue: "
                                        + factionIssue);
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

    private static String ensureTitleNewGameFactionManagerReady() {
        try {
            Class<?> managerClass = Class.forName("com.fs.starfarer.campaign.FactionManager");
            Method getInstance = findMethodRecursive(managerClass, "getInstance");
            if (getInstance == null) {
                return "getInstance-missing";
            }
            getInstance.setAccessible(true);
            Object manager = getInstance.invoke(null);

            if (manager == null) {
                java.lang.reflect.Constructor<?> ctor = managerClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object created = ctor.newInstance();
                for (Field f : managerClass.getDeclaredFields()) {
                    if (f == null
                            || !Modifier.isStatic(f.getModifiers())
                            || !managerClass.isAssignableFrom(f.getType())) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                        Object cur = f.get(null);
                        if (cur == null) {
                            f.set(null, created);
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                manager = created;
            }
            if (manager == null) {
                return "manager-null";
            }

            Map factions = null;
            Field factionsField = findFieldRecursive(managerClass, "factions");
            if (factionsField != null) {
                factionsField.setAccessible(true);
                Object mapObj = factionsField.get(manager);
                if (!(mapObj instanceof Map)) {
                    mapObj = new LinkedHashMap();
                    factionsField.set(manager, mapObj);
                }
                factions = (Map) mapObj;
            }

            Method getFaction = findMethodRecursive(managerClass, "getFaction", String.class);
            if (getFaction != null) {
                getFaction.setAccessible(true);
            }

            Class<?> factionClass = Class.forName("com.fs.starfarer.campaign.Faction");
            java.lang.reflect.Constructor<?> factionCtor =
                    factionClass.getDeclaredConstructor(String.class);
            factionCtor.setAccessible(true);

            String[] requiredIds = new String[] {"player", "pirates", "hegemony"};
            for (String id : requiredIds) {
                Object faction = null;
                if (getFaction != null) {
                    try {
                        faction = getFaction.invoke(manager, id);
                    } catch (Throwable ignored) {
                    }
                }
                if (faction == null && factions != null) {
                    try {
                        faction = factions.get(id);
                    } catch (Throwable ignored) {
                    }
                }
                if (faction == null) {
                    try {
                        faction = factionCtor.newInstance(id);
                    } catch (Throwable t) {
                        return "faction-init:" + id + ":" + describeThrowableChain(t);
                    }
                }
                if (factions != null && faction != null) {
                    try {
                        factions.put(id, faction);
                    } catch (Throwable ignored) {
                    }
                }
                ensureFactionManagerRoleField(managerClass, manager, id, faction);
                String specIssue = ensureFactionSpecInitialized(factionClass, faction);
                if (specIssue != null && "player".equals(id)) {
                    return "player-faction-" + specIssue;
                }
            }

            Object playerFaction = null;
            Method getPlayerFaction = findMethodRecursive(managerClass, "getPlayerFaction");
            if (getPlayerFaction != null) {
                getPlayerFaction.setAccessible(true);
                try {
                    playerFaction = getPlayerFaction.invoke(manager);
                } catch (Throwable ignored) {
                }
            }
            if (playerFaction == null) {
                Field playerField = findFieldRecursive(managerClass, "playerFaction");
                if (playerField != null) {
                    playerField.setAccessible(true);
                    playerFaction = playerField.get(manager);
                }
            }
            if (playerFaction == null) {
                return "player-faction-null";
            }

            String playerSpecIssue = ensureFactionSpecInitialized(factionClass, playerFaction);
            if (playerSpecIssue != null) {
                return "player-faction-" + playerSpecIssue;
            }
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static void ensureFactionManagerRoleField(
            Class<?> managerClass, Object manager, String id, Object faction) {
        if (managerClass == null
                || manager == null
                || id == null
                || id.length() == 0
                || faction == null) {
            return;
        }
        String fieldName = null;
        if ("player".equals(id)) {
            fieldName = "playerFaction";
        } else if ("pirates".equals(id)) {
            fieldName = "pirateFaction";
        } else if ("hegemony".equals(id)) {
            fieldName = "hegemonyFaction";
        }
        if (fieldName == null) {
            return;
        }
        try {
            Field roleField = findFieldRecursive(managerClass, fieldName);
            if (roleField != null) {
                roleField.setAccessible(true);
                if (roleField.get(manager) == null) {
                    roleField.set(manager, faction);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static String ensureFactionSpecInitialized(Class<?> factionClass, Object faction) {
        if (factionClass == null || faction == null) {
            return "spec-null";
        }
        try {
            Method initSpec = findMethodRecursive(factionClass, "initSpecIfNeeded");
            if (initSpec != null) {
                initSpec.setAccessible(true);
                initSpec.invoke(faction);
            }
            Method getSpec = findMethodRecursive(factionClass, "getSpec");
            if (getSpec != null) {
                getSpec.setAccessible(true);
                Object spec = getSpec.invoke(faction);
                if (spec == null) {
                    return "spec-null";
                }
            }
            return null;
        } catch (Throwable t) {
            return "spec-init:" + describeThrowableChain(t);
        }
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

    private static boolean isNewGamePreflightPendingRecently(long now) {
        return autoCampaignNewGamePreflightLogAt > 0L
                && (now - autoCampaignNewGamePreflightLogAt) <= 15000L;
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

    private static Thread startInvokeCreateWatchdog(
            final Thread target, final boolean[] stopFlag, final String label) {
        if (target == null || stopFlag == null || stopFlag.length == 0) {
            return null;
        }
        final long pollMs =
                Math.max(
                        1000L,
                        parseLongProperty(
                                "starsector.autoCampaignInvokeCreateWatchdogPollMs", 5000L));
        final long logAfterMs =
                Math.max(
                        2000L,
                        parseLongProperty(
                                "starsector.autoCampaignInvokeCreateWatchdogLogAfterMs", 15000L));
        final long maxDurationMs =
                Math.max(
                        logAfterMs,
                        parseLongProperty(
                                "starsector.autoCampaignInvokeCreateWatchdogMaxMs", 240000L));
        final String safeLabel =
                (label == null || label.trim().isEmpty())
                        ? "direct-new-game invoke-create"
                        : label.trim();

        Thread watchdog =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                long startedAt = System.currentTimeMillis();
                                long nextLogAt = startedAt + logAfterMs;
                                while (!stopFlag[0]) {
                                    if (!target.isAlive()) {
                                        return;
                                    }
                                    long now = System.currentTimeMillis();
                                    long elapsed = Math.max(0L, now - startedAt);
                                    if (elapsed >= maxDurationMs) {
                                        System.out.println(
                                                "Fixer: "
                                                        + safeLabel
                                                        + " watchdog max duration reached ("
                                                        + maxDurationMs
                                                        + "ms); latest stack="
                                                        + summarizeThreadStack(target, 16));
                                        return;
                                    }
                                    if (now >= nextLogAt) {
                                        System.out.println(
                                                "Fixer: "
                                                        + safeLabel
                                                        + " watchdog elapsed="
                                                        + elapsed
                                                        + "ms stack="
                                                        + summarizeThreadStack(target, 16));
                                        nextLogAt = now + pollMs;
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
                        "Fixer-InvokeCreateWatchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        return watchdog;
    }

    private static String summarizeThreadStack(Thread target, int maxFrames) {
        if (target == null) {
            return "thread=null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(target.getName())
                .append(" state=")
                .append(target.getState())
                .append(" stack=");
        StackTraceElement[] trace = target.getStackTrace();
        if (trace == null || trace.length == 0) {
            sb.append("<empty>");
            return sb.toString();
        }
        int limit = Math.max(1, Math.min(trace.length, Math.max(1, maxFrames)));
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(" <- ");
            }
            sb.append(trace[i].toString());
        }
        if (trace.length > limit) {
            sb.append(" <- ... +").append(trace.length - limit);
        }
        return sb.toString();
    }

    private static String maybeWaitForCoreSpecBaselineBeforeDirectNewGame() {
        String baselineIssue = checkCoreSpecStoreBaselineForDirectNewGame();
        if (baselineIssue == null) {
            autoCampaignCoreSpecBaselinePendingSince = 0L;
            autoCampaignCoreSpecBaselinePendingCount = 0;
            return null;
        }

        if (!allowMutatingSpecPreflight()) {
            long now = System.currentTimeMillis();
            if (autoCampaignCoreSpecBaselinePendingSince <= 0L) {
                autoCampaignCoreSpecBaselinePendingSince = now;
            }
            autoCampaignCoreSpecBaselinePendingCount++;

            long pendingMs = Math.max(0L, now - autoCampaignCoreSpecBaselinePendingSince);
            long nonMutatingWaitMs =
                    Math.max(
                            15000L,
                            parseLongProperty(
                                    "starsector.autoCampaignCoreSpecBaselineNonMutatingWaitMs",
                                    120000L));
            boolean coreFactionMissing = hasMissingCoreFactionSpecIssue(baselineIssue);
            if (coreFactionMissing && !shouldSkipFactionPreflight()) {
                String warmupOutcome =
                        maybeWarmupFactionSpecsForNewGame("core-spec-baseline:" + baselineIssue);
                if (warmupOutcome == null) {
                    String afterWarmupIssue = checkCoreSpecStoreBaselineForDirectNewGame();
                    if (afterWarmupIssue == null) {
                        System.out.println(
                                "Fixer: direct-new-game core spec baseline recovered in non-mutating mode after warmup.");
                        autoCampaignCoreSpecBaselinePendingSince = 0L;
                        autoCampaignCoreSpecBaselinePendingCount = 0;
                        return null;
                    }
                    baselineIssue = afterWarmupIssue;
                    coreFactionMissing = hasMissingCoreFactionSpecIssue(baselineIssue);
                } else if (!"cooldown".equals(warmupOutcome)
                        && !"attempt-limit".equals(warmupOutcome)) {
                    System.out.println(
                            "Fixer: direct-new-game core baseline warmup issue in non-mutating mode: "
                                    + warmupOutcome);
                }
            } else if (coreFactionMissing) {
                if (autoCampaignCoreSpecBaselinePendingCount == 1
                        || autoCampaignCoreSpecBaselinePendingCount % 30 == 0) {
                    System.out.println(
                            "Fixer: direct-new-game core baseline missing factions in skip mode; skipping warmup mutations and waiting for create path.");
                }
            }

            boolean shouldKeepWaiting = pendingMs < nonMutatingWaitMs;
            if (shouldKeepWaiting) {
                if (autoCampaignCoreSpecBaselinePendingCount == 1
                        || autoCampaignCoreSpecBaselinePendingCount % 30 == 0) {
                    System.out.println(
                            "Fixer: direct-new-game core spec baseline unresolved in non-mutating mode (waiting, coreFactionMissing="
                                    + coreFactionMissing
                                    + ", pendingMs="
                                    + pendingMs
                                    + "/"
                                    + nonMutatingWaitMs
                                    + "): "
                                    + baselineIssue);
                }
                return "core spec baseline pending in non-mutating mode (coreFactionMissing="
                        + coreFactionMissing
                        + ", pending="
                        + pendingMs
                        + "ms/"
                        + nonMutatingWaitMs
                        + "ms, attempt "
                        + autoCampaignCoreSpecBaselinePendingCount
                        + "): "
                        + baselineIssue;
            }

            System.out.println(
                    "Fixer: direct-new-game core spec baseline unresolved after non-mutating wait window; proceeding cautiously: "
                            + baselineIssue);
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
            // In non-mutating mode avoid inserting preload specs; ResourceLoaderState.init
            // registers core specs and duplicate inserts can crash startup with already-exists errors.
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
        addThreadContextLoadersIfAllowed(loaders);
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
                System.getProperty("starsector.autoCampaignSkipFactionPreflight", "false"));
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
            if (Boolean.parseBoolean(
                    System.getProperty(
                            "starsector.autoCampaignSkipGalatiaDerelictVariantPreflight",
                            "true"))) {
                System.out.println(
                        "Fixer: galatia-variant-check skipped by starsector.autoCampaignSkipGalatiaDerelictVariantPreflight.");
                return null;
            }
            System.out.println(
                    "Fixer: galatia-variant-check begin thread="
                            + Thread.currentThread().getName()
                            + "#"
                            + Thread.currentThread().getId());
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
                String issue = String.join(", ", unresolved);
                System.out.println(
                        "Fixer: galatia-variant-check unresolved thread="
                                + Thread.currentThread().getName()
                                + "#"
                                + Thread.currentThread().getId()
                                + " issue="
                                + issue);
                return issue;
            }
            System.out.println(
                    "Fixer: galatia-variant-check resolved thread="
                            + Thread.currentThread().getName()
                            + "#"
                            + Thread.currentThread().getId());
            return null;
        } catch (Throwable t) {
            String issue = "galatia-derelict-variant-check:" + describeThrowableChain(t);
            System.out.println(
                    "Fixer: galatia-variant-check exception thread="
                            + Thread.currentThread().getName()
                            + "#"
                            + Thread.currentThread().getId()
                            + " issue="
                            + issue);
            return issue;
        }
    }

    private static String forceGalatiaDerelictVariantAliasesForWarmup() {
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
                String issue = forceVariantAliasForWarmup(variantId, "lasher_Standard");
                if (issue != null) {
                    unresolved.add(variantId + "(" + issue + ")");
                }
            }
            if (!unresolved.isEmpty()) {
                return String.join(", ", unresolved);
            }
            return null;
        } catch (Throwable t) {
            return "galatia-derelict-variant-force-alias:" + describeThrowableChain(t);
        }
    }

    private static String ensureVariantAliasForWarmup(String targetVariantId, String fallbackVariantId) {
        return applyVariantAliasForWarmup(targetVariantId, fallbackVariantId, false);
    }

    private static String forceVariantAliasForWarmup(String targetVariantId, String fallbackVariantId) {
        return applyVariantAliasForWarmup(targetVariantId, fallbackVariantId, true);
    }

    private static String applyVariantAliasForWarmup(
            String targetVariantId, String fallbackVariantId, boolean replaceExisting) {
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
            if (existing != null && !replaceExisting) {
                return null;
            }
            if (existing != null && replaceExisting) {
                System.out.println(
                        "Fixer: warmup forcing variant alias overwrite for " + targetId + ".");
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
            directNewGameLastNullMarker = "entered";
            System.out.println(
                    "Fixer: direct-new-game stage=begin thread="
                            + Thread.currentThread().getName()
                            + "#"
                            + Thread.currentThread().getId()
                            + " cl@"
                            + Integer.toHexString(
                                    System.identityHashCode(Fixer.class.getClassLoader())));
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
            directNewGameLastNullMarker = "after-session";

            directNewGameLastNullMarker = "before-baseline-call";
            String baselineIssue = maybeWaitForCoreSpecBaselineBeforeDirectNewGame();
            directNewGameLastNullMarker = "after-baseline-call";
            if (baselineIssue != null) {
                maybeLogNewGamePreflightIssue(baselineIssue);
                return "direct new-game preflight pending";
            }
            directNewGameLastNullMarker = "after-baseline-ok";

            String resetDecisionIssue = checkDirectNewGameRuntimeReadiness();
            boolean shouldResetCampaignEngine =
                    shouldResetCampaignEngineBeforeDirectNewGame(resetDecisionIssue);
            if (shouldResetCampaignEngine) {
                try {
                    invokeStaticNoArg(
                            Class.forName("com.fs.starfarer.campaign.CampaignEngine"),
                            "resetInstance");
                    System.out.println(
                            "Fixer: direct-new-game reset CampaignEngine before create (runtimeReadiness="
                                    + String.valueOf(resetDecisionIssue)
                                    + ").");
                } catch (Throwable t) {
                    System.out.println(
                            "Fixer: direct-new-game CampaignEngine reset failed: "
                                    + describeThrowableChain(t));
                }
            } else {
                System.out.println(
                        "Fixer: direct-new-game preserved CampaignEngine before create (runtimeReadiness="
                                + String.valueOf(resetDecisionIssue)
                                + ").");
            }

            Class<?> managerClass = Class.forName("com.fs.starfarer.campaign.save.CampaignGameManager");
            Method createMethod = findDirectNewGameMethod(managerClass);
            if (createMethod == null) {
                return "CampaignGameManager new-game method not found";
            }
            directNewGameLastNullMarker = "after-create-method";

            boolean mutatingSpecPreflight = allowMutatingSpecPreflight();
            boolean clearForcedFactionPreloadBeforeCreate = false;
            LinkedHashSet<String> forcedFactionIdsForCleanup = new LinkedHashSet<String>();
            Class<?> forcedFactionSpecStoreClassForCleanup = null;
            Class<?> forcedFactionSpecClassForCleanup = null;
            System.out.println(
                    "Fixer: direct-new-game preflight flags mutatingSpecPreflight="
                            + mutatingSpecPreflight
                            + " skipFactionPreflight="
                            + shouldSkipFactionPreflight());
            System.out.println("Fixer: direct-new-game stage=faction-preflight");
            if (!shouldSkipFactionPreflight()) {
                String factionPreflight =
                        ensureFactionSpecsReadyForDirectNewGame(mutatingSpecPreflight);
                System.out.println(
                        "Fixer: direct-new-game faction-preflight result="
                                + String.valueOf(factionPreflight));
                if (factionPreflight != null) {
                    String factionPreflightLower = factionPreflight.toLowerCase();
                    boolean specStoreNotReady =
                            !mutatingSpecPreflight
                                    && factionPreflightLower.indexOf("spec store not ready") >= 0;
                    if (!mutatingSpecPreflight) {
                        System.out.println(
                                "Fixer: direct-new-game faction-preflight unresolved in non-mutating mode; skipping warmup mutations and continuing: "
                                        + factionPreflight);
                    } else if (specStoreNotReady) {
                        System.out.println(
                                "Fixer: direct-new-game faction-preflight spec store not ready in non-mutating mode; continuing without faction warmup.");
                    } else {
                        String warmupResult = maybeWarmupFactionSpecsForNewGame(factionPreflight);
                        System.out.println(
                                "Fixer: direct-new-game faction-preflight warmup result="
                                        + String.valueOf(warmupResult));
                        if (warmupResult != null) {
                            maybeLogNewGamePreflightIssue(
                                    factionPreflight + " [warmup:" + warmupResult + "]");
                            return "direct new-game preflight pending";
                        }
                    }
                }
            } else {
                System.out.println("Fixer: direct-new-game skipping faction preflight.");
                String factionRepairIssue = ensureFactionSpecsReadyForDirectNewGame(false);
                if (factionRepairIssue != null) {
                    System.out.println(
                            "Fixer: direct-new-game faction repair warning (skip mode): "
                                    + factionRepairIssue);
                    boolean coreFactionMissing = hasMissingCoreFactionSpecIssue(factionRepairIssue);
                    System.out.println(
                            "Fixer: direct-new-game skip-mode core-faction-missing="
                                    + coreFactionMissing);
                    if (coreFactionMissing) {
                        System.out.println(
                                "Fixer: direct-new-game skip-mode core-faction repair requested (mutatingSpecPreflight="
                                        + mutatingSpecPreflight
                                        + ").");
                        String forcedRepairIssue = ensureFactionSpecsReadyForDirectNewGame(true);
                        if (!mutatingSpecPreflight) {
                            forcedFactionIdsForCleanup.clear();
                            String[] temporaryForcedFactionIds =
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
                                        "knights_of_ludd",
                                        "persean",
                                        "remnant",
                                        "derelict",
                                        "omega",
                                        "threat",
                                        "dweller"
                                    };
                            for (String factionId : temporaryForcedFactionIds) {
                                if (factionId != null && factionId.trim().length() > 0) {
                                    forcedFactionIdsForCleanup.add(factionId.trim());
                                }
                            }
                            try {
                                forcedFactionSpecStoreClassForCleanup =
                                        Class.forName("com.fs.starfarer.loading.SpecStore");
                                forcedFactionSpecClassForCleanup =
                                        Class.forName("com.fs.starfarer.loading.if");
                                clearForcedFactionPreloadBeforeCreate = true;
                                System.out.println(
                                        "Fixer: direct-new-game armed temporary faction preload cleanup before create (ids="
                                                + forcedFactionIdsForCleanup.size()
                                                + ").");
                            } catch (Throwable t) {
                                clearForcedFactionPreloadBeforeCreate = false;
                                forcedFactionSpecStoreClassForCleanup = null;
                                forcedFactionSpecClassForCleanup = null;
                                System.out.println(
                                        "Fixer: direct-new-game unable to arm temporary faction preload cleanup: "
                                                + describeThrowableChain(t));
                            }
                        }
                        if (forcedRepairIssue != null
                                && hasMissingCoreFactionSpecIssue(forcedRepairIssue)) {
                            maybeLogNewGamePreflightIssue(
                                    "core faction preflight unresolved in skip mode: "
                                            + forcedRepairIssue
                                            + " [forced-repair]");
                            return "direct new-game preflight pending";
                        }
                        if (forcedRepairIssue == null) {
                            System.out.println(
                                    "Fixer: direct-new-game core faction preflight recovered in skip mode after forced repair.");
                            factionRepairIssue = null;
                        } else {
                            System.out.println(
                                    "Fixer: direct-new-game core faction forced repair residual issue: "
                                            + forcedRepairIssue);
                            factionRepairIssue = forcedRepairIssue;
                        }
                    }
                    String factionRepairIssueLower =
                            factionRepairIssue == null ? "" : factionRepairIssue.toLowerCase();
                    boolean specStoreNotReady =
                            factionRepairIssueLower.indexOf("spec store not ready") >= 0;
                    if (factionRepairIssue != null
                            && factionRepairIssue.indexOf("faction:") >= 0
                            && !specStoreNotReady) {
                        if (mutatingSpecPreflight) {
                            maybeLogNewGamePreflightIssue(factionRepairIssue);
                            return "direct new-game preflight pending";
                        }
                        System.out.println(
                                "Fixer: direct-new-game skip-mode faction issue ignored in non-mutating mode: "
                                        + factionRepairIssue);
                    }
                } else {
                    System.out.println(
                            "Fixer: direct-new-game faction repair completed (skip mode).");
                }
            }
            directNewGameLastNullMarker = "after-faction-preflight";
            System.out.println("Fixer: direct-new-game stage=galatia-derelict-variant-preflight");
            String galatiaDerelictVariantPreflight = ensureGalatiaDerelictVariantsForWarmup();
            System.out.println(
                    "Fixer: direct-new-game galatia-derelict-variant-preflight result="
                            + String.valueOf(galatiaDerelictVariantPreflight));
            if (galatiaDerelictVariantPreflight != null) {
                String galatiaIssueLower = galatiaDerelictVariantPreflight.toLowerCase();
                boolean specStoreNotReady =
                        !mutatingSpecPreflight
                                && galatiaIssueLower.indexOf("spec store not ready") >= 0;
                if (specStoreNotReady) {
                    System.out.println(
                            "Fixer: direct-new-game Galatia variant preflight not ready in non-mutating mode; continuing: "
                                    + galatiaDerelictVariantPreflight);
                } else {
                    maybeLogNewGamePreflightIssue(
                            "galatia derelict variant preflight issue: "
                                    + galatiaDerelictVariantPreflight);
                    return "direct new-game preflight pending";
                }
            }
            directNewGameLastNullMarker = "after-galatia-preflight";
            boolean runBroadSpecPreflight =
                    mutatingSpecPreflight
                            && Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignRunBroadSpecPreflight",
                                            "false"));
            // Non-mutating mode should not pre-insert broad spec sets because the loader
            // will register them later and can fail on duplicate IDs (e.g. asteroid_field).
            boolean allowTemporarySpecInjection = mutatingSpecPreflight;
            System.out.println(
                    "Fixer: direct-new-game preflight mode runBroadSpecPreflight="
                            + runBroadSpecPreflight
                            + " allowTemporarySpecInjection="
                            + allowTemporarySpecInjection);
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
                System.out.println("Fixer: direct-new-game stage=planet-gen-preflight");
                String planetGenPreflight = ensurePlanetGenSpecsReadyForDirectNewGame();
                if (planetGenPreflight != null) {
                    boolean strictPlanetGenPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictPlanetGenSpecPreflight",
                                            "false"));
                    if (strictPlanetGenPreflight) {
                        return "planet-gen spec preflight failed: " + planetGenPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game planet-gen preflight warning (continuing): "
                                    + planetGenPreflight);
                }
                System.out.println("Fixer: direct-new-game stage=star-gen-preflight");
                String starGenPreflight = ensureStarGenSpecsReadyForDirectNewGame();
                if (starGenPreflight != null) {
                    boolean strictStarGenPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictStarGenSpecPreflight",
                                            "false"));
                    if (strictStarGenPreflight) {
                        return "star-gen spec preflight failed: " + starGenPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game star-gen preflight warning (continuing): "
                                    + starGenPreflight);
                }
                System.out.println("Fixer: direct-new-game stage=age-gen-preflight");
                String ageGenPreflight = ensureAgeGenSpecsReadyForDirectNewGame();
                if (ageGenPreflight != null) {
                    boolean strictAgeGenPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictAgeGenSpecPreflight",
                                            "false"));
                    if (strictAgeGenPreflight) {
                        return "age-gen spec preflight failed: " + ageGenPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game age-gen preflight warning (continuing): "
                                    + ageGenPreflight);
                }
                System.out.println("Fixer: direct-new-game stage=name-gen-preflight");
                String nameGenPreflight = ensureNameGenSpecsReadyForDirectNewGame();
                if (nameGenPreflight != null) {
                    boolean strictNameGenPreflight =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignStrictNameGenSpecPreflight",
                                            "false"));
                    if (strictNameGenPreflight) {
                        return "name-gen spec preflight failed: " + nameGenPreflight;
                    }
                    System.out.println(
                            "Fixer: direct new-game name-gen preflight warning (continuing): "
                                    + nameGenPreflight);
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
            if (mutatingSpecPreflight) {
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
            } else {
                System.out.println(
                        "Fixer: direct-new-game non-mutating mode; skipping salvage-spec preflight injection.");
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
            boolean runOrbitalJunkEntityCtorProbe =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignOrbitalJunkEntityCtorProbe", "false"));
            if (!runOrbitalJunkEntityCtorProbe) {
                if (!directNewGameOrbitalEntityCtorProbeDisabledLogged) {
                    directNewGameOrbitalEntityCtorProbeDisabledLogged = true;
                    System.out.println(
                            "Fixer: direct new-game orbital_junk entity ctor probe disabled (set -Dstarsector.autoCampaignOrbitalJunkEntityCtorProbe=true to enable).");
                }
            } else {
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
                        if (!mutatingSpecPreflight) {
                            System.out.println(
                                    "Fixer: direct new-game non-mutating mode continuing despite orbital_junk entity probe issue: "
                                            + retriedOrbitalJunkEntityProbe);
                        } else {
                            maybeLogNewGamePreflightIssue(
                                    "orbital_junk entity probe unresolved: "
                                            + retriedOrbitalJunkEntityProbe);
                            return "direct new-game preflight pending";
                        }
                    }
                }
            }

            System.out.println("Fixer: direct-new-game stage=pre-invoke-readiness-check");
            directNewGameLastNullMarker = "before-preinvoke-readiness-check";
            String preInvokeReadiness = checkDirectNewGameRuntimeReadiness();
            if (preInvokeReadiness == null) {
                System.out.println(
                        "Fixer: direct-new-game pre-invoke readiness="
                                + String.valueOf(preInvokeReadiness)
                                + "; skipping invoke-create and continuing transition flow.");
                directNewGameLastNullMarker = "pre-invoke-ready";
                System.out.println("Fixer: direct-new-game return-null marker=pre-invoke-ready");
                return null;
            }
            if ("player-fleet-null".equals(preInvokeReadiness)) {
                String settleWindowIssue = checkDirectNewGameCreateSettleWindow();
                if (settleWindowIssue != null) {
                    return settleWindowIssue;
                }
            }
            Object currentStateBeforeInvoke = readCurrentStateFromDriver(ctx);
            boolean forceInvokeCreateForCampaignState = false;
            if (currentStateBeforeInvoke != null && !isTitleState(null, currentStateBeforeInvoke)) {
                boolean currentStateIsCampaign = isCampaignState(null, currentStateBeforeInvoke);
                if (currentStateIsCampaign && "player-fleet-null".equals(preInvokeReadiness)) {
                    String campaignWorldPopulationIssue =
                            checkCampaignWorldPopulationForPlayerFleetNullTransition(ctx);
                    boolean campaignWorldPopulated = campaignWorldPopulationIssue == null;
                    boolean allowCampaignStateInvokeCreate =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignInvokeCreateInCampaignState",
                                            "false"));
                    if (!allowCampaignStateInvokeCreate) {
                        System.out.println(
                                "Fixer: direct-new-game pre-invoke readiness=player-fleet-null while current state is CampaignState; skipping create() to let campaign runtime settle (worldPopulation="
                                        + String.valueOf(campaignWorldPopulationIssue)
                                        + ").");
                        return "direct new-game preflight pending: campaign-state-player-fleet-null";
                    }
                    String campaignStateCreateCooldownIssue = checkCampaignStateCreateCooldownWindow();
                    if (campaignStateCreateCooldownIssue != null) {
                        return campaignStateCreateCooldownIssue;
                    }
                    System.out.println(
                            "Fixer: direct-new-game pre-invoke readiness=player-fleet-null while current state is CampaignState; invoking create() to populate runtime (worldPopulation="
                                    + (campaignWorldPopulated ? "ready" : String.valueOf(campaignWorldPopulationIssue))
                                    + ").");
                    markCampaignStateCreateInvokeAt(System.currentTimeMillis());
                    forceInvokeCreateForCampaignState = true;
                } else {
                    System.out.println(
                            "Fixer: direct-new-game pre-invoke readiness="
                                    + String.valueOf(preInvokeReadiness)
                                    + "; skipping invoke-create because current state left title ("
                                    + currentStateBeforeInvoke.getClass().getName()
                                    + ").");
                    return "direct new-game preflight pending: title-state-left-before-create";
                }
            }
            if ("player-fleet-null".equals(preInvokeReadiness)) {
            boolean allowPlayerFleetNullInvokeCreate =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignInvokeCreateOnPlayerFleetNull",
                                    "true"));
                if (!allowPlayerFleetNullInvokeCreate && !forceInvokeCreateForCampaignState) {
                    if (clearForcedFactionPreloadBeforeCreate
                            && forcedFactionSpecStoreClassForCleanup != null
                            && forcedFactionSpecClassForCleanup != null
                            && !forcedFactionIdsForCleanup.isEmpty()) {
                        boolean cleanupForcedFactionPreloadBeforeCreate =
                                Boolean.parseBoolean(
                                        System.getProperty(
                                                "starsector.autoCampaignCleanupForcedFactionPreloadBeforeCreate",
                                                "true"));
                        if (cleanupForcedFactionPreloadBeforeCreate) {
                            int removedTemporaryFactionSpecs =
                                    removeSpecIdsForClass(
                                            forcedFactionSpecStoreClassForCleanup,
                                            forcedFactionSpecClassForCleanup,
                                            forcedFactionIdsForCleanup);
                            clearForcedFactionPreloadBeforeCreate = false;
                            System.out.println(
                                    "Fixer: direct-new-game temporary forced faction preload cleanup pre-return removed="
                                            + removedTemporaryFactionSpecs
                                            + " requested="
                                            + forcedFactionIdsForCleanup.size()
                                            + ".");
                        }
                    }
                    System.out.println(
                            "Fixer: direct-new-game pre-invoke readiness=player-fleet-null; skipping create() and deferring to campaign transition/synthetic-fleet recovery.");
                    directNewGameLastNullMarker = "pre-invoke-ready";
                    return "direct new-game preflight pending: player-fleet-null-create-deferred";
                }
                System.out.println(
                        "Fixer: direct-new-game pre-invoke readiness=player-fleet-null; invoking create() to build campaign runtime.");
            }

            probeFactionClassInitialization("pre-create");
            if (clearForcedFactionPreloadBeforeCreate
                    && forcedFactionSpecStoreClassForCleanup != null
                    && forcedFactionSpecClassForCleanup != null
                    && !forcedFactionIdsForCleanup.isEmpty()) {
                boolean cleanupForcedFactionPreloadBeforeCreate =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignCleanupForcedFactionPreloadBeforeCreate",
                                        "true"));
                if (cleanupForcedFactionPreloadBeforeCreate) {
                    int removedTemporaryFactionSpecs =
                            removeSpecIdsForClass(
                                    forcedFactionSpecStoreClassForCleanup,
                                    forcedFactionSpecClassForCleanup,
                                    forcedFactionIdsForCleanup);
                    clearForcedFactionPreloadBeforeCreate = false;
                    System.out.println(
                            "Fixer: direct-new-game temporary forced faction preload cleanup pre-create removed="
                                    + removedTemporaryFactionSpecs
                                    + " requested="
                                    + forcedFactionIdsForCleanup.size()
                                    + ".");
                } else {
                    System.out.println(
                            "Fixer: direct-new-game temporary forced faction preload cleanup pre-create skipped (set -Dstarsector.autoCampaignCleanupForcedFactionPreloadBeforeCreate=true to remove before create).");
                }
            }

            Object data = buildDefaultCharacterCreationData(createMethod.getParameterTypes()[0]);
            if (data == null) {
                return "character creation data build failed";
            }
            lastDirectNewGameData = data;
            int additionalShips = getAdditionalShipsCount(data);
            if (additionalShips <= 0) {
                return "character creation data has no starting fleet members";
            }
            logDirectNewGameDataSnapshot(data);

            warmupTitleRenderTicks(titleState, 12);
            createMethod.setAccessible(true);
            final Method invokeCreateMethod = createMethod;
            final Object invokeCreateData = data;
            final Object invokeCreateCampaignState = campaignState;
            final String invokeCreateLeaseOwner = buildDirectNewGameCreateLeaseOwner();
            final long invokeCreateLeaseMs =
                    Math.max(
                            15000L,
                            parseLongProperty("starsector.autoCampaignInvokeCreateLeaseMs", 180000L));
            if (!claimDirectNewGameCreateLease(
                    invokeCreateLeaseOwner, System.currentTimeMillis(), invokeCreateLeaseMs)) {
                return "direct new-game preflight pending: invoke-create lease active";
            }
            markDirectNewGameCreateInvokeAt(System.currentTimeMillis());
            System.out.println(
                    "Fixer: direct-new-game stage=invoke-create thread="
                            + Thread.currentThread().getName()
                            + "#"
                            + Thread.currentThread().getId()
                            + " cl@"
                            + Integer.toHexString(
                                    System.identityHashCode(Fixer.class.getClassLoader())));
            final boolean[] orbitalSpecWatchdogStop = new boolean[] {false};
            Thread orbitalSpecWatchdogThread = null;
            final boolean[] invokeCreateWatchdogStop = new boolean[] {false};
            Thread invokeCreateWatchdogThread = null;
            if (allowOrbitalJunkFallback && allowOrbitalJunkSpecWatchdog()) {
                orbitalSpecWatchdogThread =
                        startOrbitalJunkSpecWatchdog(
                                orbitalSpecWatchdogStop, mutatingSpecPreflight);
            }
            if (Boolean.parseBoolean(
                    System.getProperty("starsector.autoCampaignInvokeCreateWatchdog", "true"))) {
                invokeCreateWatchdogThread =
                        startInvokeCreateWatchdog(
                                Thread.currentThread(),
                                invokeCreateWatchdogStop,
                                "direct-new-game invoke-create");
            }
            boolean retainInvokeCreateLease = false;
            try {
                long invokeCallTimeoutMs =
                        Math.max(
                                5000L,
                                parseLongProperty(
                                        "starsector.autoCampaignInvokeCreateCallTimeoutMs",
                                        120000L));
                final Object[] invokeResultHolder = new Object[1];
                final Throwable[] invokeErrorHolder = new Throwable[1];
                Thread invokeCreateThread =
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            invokeResultHolder[0] =
                                                    invokeCreateMethod.invoke(
                                                            null,
                                                            invokeCreateData,
                                                            invokeCreateCampaignState);
                                        } catch (Throwable t) {
                                            invokeErrorHolder[0] = t;
                                        }
                                    }
                                },
                                "FixerInvokeCreateCall");
                invokeCreateThread.setDaemon(true);
                try {
                    invokeCreateThread.start();
                    invokeCreateThread.join(invokeCallTimeoutMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    retainInvokeCreateLease = true;
                    return "direct new-game preflight pending: invoke-create-call-interrupted";
                } catch (Throwable t) {
                    retainInvokeCreateLease = true;
                    return "direct new-game preflight pending: invoke-create-call-wrapper-failure("
                            + describeThrowableChain(t)
                            + ")";
                }
                if (invokeCreateThread.isAlive()) {
                    retainInvokeCreateLease = true;
                    try {
                        invokeCreateThread.interrupt();
                    } catch (Throwable ignored) {
                    }
                    maybeLogNewGamePreflightIssue(
                            "invoke-create call timed out after " + invokeCallTimeoutMs + "ms; retaining create lease.");
                    return "direct new-game preflight pending: invoke-create-call-timeout("
                            + invokeCallTimeoutMs
                            + "ms)";
                }
                if (invokeErrorHolder[0] != null) {
                    throw invokeErrorHolder[0];
                }
                Object result = invokeResultHolder[0];
                System.out.println("Fixer: direct-new-game stage=invoke-create-return");
                if ("player-fleet-null".equals(preInvokeReadiness)) {
                    try {
                        Object postCreateSector = readGlobalSectorForSectorGen();
                        Object postCreateFleet =
                                postCreateSector == null
                                        ? null
                                        : invokeNoArgIfPresent(postCreateSector, "getPlayerFleet");
                        if (postCreateSector != null && postCreateFleet == null) {
                            Object bootstrappedFleet =
                                    tryCreateSyntheticPlayerFleetForInteraction(
                                            postCreateSector, null);
                            if (bootstrappedFleet != null) {
                                System.out.println(
                                        "Fixer: direct-new-game post-create synthetic player fleet bootstrap succeeded.");
                            } else {
                                System.out.println(
                                        "Fixer: direct-new-game post-create synthetic player fleet bootstrap unavailable.");
                            }
                        }
                    } catch (Throwable t) {
                        System.out.println(
                                "Fixer: direct-new-game post-create synthetic player fleet bootstrap failed: "
                                        + describeThrowableChain(t));
                    }
                }
                if (result == null) {
                    String runtimeReadyIssue = waitForDirectNewGameRuntimeReadiness();
                    if (runtimeReadyIssue != null) {
                        if ("player-fleet-null".equals(runtimeReadyIssue)) {
                            String settleWindowIssue = checkDirectNewGameCreateSettleWindow();
                            if (settleWindowIssue != null) {
                                maybeLogNewGamePreflightIssue(
                                        "post-init campaign runtime still settling: "
                                                + runtimeReadyIssue);
                                return settleWindowIssue;
                            }
                            maybeLogNewGamePreflightIssue(
                                    "post-init campaign runtime still player-fleet-null after settle window.");
                            return "direct new-game preflight pending";
                        }
                        maybeLogNewGamePreflightIssue(
                                "post-init campaign runtime not ready: " + runtimeReadyIssue);
                        return "direct new-game preflight pending";
                    }
                    directNewGameLastNullMarker = "post-invoke-ready";
                    System.out.println("Fixer: direct-new-game return-null marker=post-invoke-ready");
                    return null;
                }
                return String.valueOf(result);
            } finally {
                if (!retainInvokeCreateLease) {
                    releaseDirectNewGameCreateLease(invokeCreateLeaseOwner);
                } else {
                    System.out.println(
                            "Fixer: retaining direct-new-game create lease for settle window (owner="
                                    + invokeCreateLeaseOwner
                                    + ").");
                }
                if (invokeCreateWatchdogThread != null) {
                    invokeCreateWatchdogStop[0] = true;
                    invokeCreateWatchdogThread.interrupt();
                    try {
                        invokeCreateWatchdogThread.join(600L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (orbitalSpecWatchdogThread != null) {
                    orbitalSpecWatchdogStop[0] = true;
                    orbitalSpecWatchdogThread.interrupt();
                    try {
                        orbitalSpecWatchdogThread.join(600L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (clearForcedFactionPreloadBeforeCreate
                        && forcedFactionSpecStoreClassForCleanup != null
                        && forcedFactionSpecClassForCleanup != null
                        && !forcedFactionIdsForCleanup.isEmpty()) {
                    boolean cleanupForcedFactionPreload =
                            Boolean.parseBoolean(
                                    System.getProperty(
                                            "starsector.autoCampaignCleanupForcedFactionPreload",
                                            "true"));
                    if (cleanupForcedFactionPreload) {
                        int removedTemporaryFactionSpecs =
                                removeSpecIdsForClass(
                                        forcedFactionSpecStoreClassForCleanup,
                                        forcedFactionSpecClassForCleanup,
                                        forcedFactionIdsForCleanup);
                        clearForcedFactionPreloadBeforeCreate = false;
                        System.out.println(
                                "Fixer: direct-new-game temporary forced faction preload cleanup post-create removed="
                                        + removedTemporaryFactionSpecs
                                        + " requested="
                                        + forcedFactionIdsForCleanup.size()
                                        + ".");
                    } else {
                        System.out.println(
                                "Fixer: direct-new-game temporary forced faction preload cleanup post-create skipped (set -Dstarsector.autoCampaignCleanupForcedFactionPreload=false to keep injected specs).");
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
                                "Fixer: orbital_junk custom-entity spec repaired; deferring direct new-game retry to next watcher tick.");
                        return "direct new-game preflight pending";
                    }
                    maybeLogNewGamePreflightIssue(
                            repairIssue == null
                                    ? "orbital_junk custom-entity spec repaired; retrying direct new-game."
                                    : "orbital_junk custom-entity spec repair issue: " + repairIssue);
                    return "direct new-game preflight pending";
                }
                if (allowOrbitalJunkImmediateRetry) {
                    System.out.println(
                            "Fixer: orbital_junk resolution NPE observed in non-mutating mode; deferring direct new-game retry to next watcher tick.");
                    return "direct new-game preflight pending";
                }
                maybeLogNewGamePreflightIssue(
                        "orbital_junk resolution NPE observed in non-mutating mode; waiting for loader readiness.");
                return "direct new-game preflight pending";
            }
            if (isGalatiaDerelictVariantNpe(t)) {
                System.out.println(
                        "Fixer: Galatia derelict variant NPE detail: " + describeThrowableChain(t));
                String galatiaIssue = ensureGalatiaDerelictVariantsForWarmup();
                if (galatiaIssue == null) {
                    String forceAliasIssue = forceGalatiaDerelictVariantAliasesForWarmup();
                    if (forceAliasIssue == null && allowOrbitalJunkImmediateRetry) {
                        System.out.println(
                                "Fixer: direct new-game Galatia derelict variant repair succeeded; deferring retry to next watcher tick.");
                        return "direct new-game preflight pending";
                    }
                    if (forceAliasIssue != null) {
                        maybeLogNewGamePreflightIssue(
                                "galatia derelict forced-alias issue: " + forceAliasIssue);
                    } else {
                        maybeLogNewGamePreflightIssue(
                                "galatia derelict variant NPE observed after forced alias; retry pending.");
                    }
                    return "direct new-game preflight pending";
                }
                if (allowOrbitalJunkImmediateRetry) {
                    System.out.println(
                            "Fixer: direct new-game Galatia derelict variant repair succeeded; deferring retry to next watcher tick.");
                    return "direct new-game preflight pending";
                }
                maybeLogNewGamePreflightIssue(
                        galatiaIssue == null
                                ? "galatia derelict variant NPE observed; retry pending."
                                : "galatia derelict variant preflight issue: " + galatiaIssue);
                return "direct new-game preflight pending";
            }
            if (isProcgenCampaignPlanetInitNpe(t)) {
                System.out.println(
                        "Fixer: procgen CampaignPlanet init NPE detail: " + stackTraceToString(t));
                String planetIssue = ensurePlanetSpecsReadyForDirectNewGame();
                String planetGenIssue = ensurePlanetGenSpecsReadyForDirectNewGame();
                String starIssue = ensureStarGenSpecsReadyForDirectNewGame();
                String ageIssue = ensureAgeGenSpecsReadyForDirectNewGame();
                String terrainIssue = ensureTerrainSpecsReadyForDirectNewGame();
                String pickerIssue = refreshProcgenBackgroundPickersForDirectNewGame();
                if (planetIssue == null
                        && planetGenIssue == null
                        && starIssue == null
                        && ageIssue == null
                        && terrainIssue == null
                        && pickerIssue == null) {
                    maybeLogNewGamePreflightIssue(
                            "procgen CampaignPlanet init repair succeeded; retry pending.");
                    return "direct new-game preflight pending";
                }
                maybeLogNewGamePreflightIssue(
                        "procgen CampaignPlanet init repair issues: planet="
                                + String.valueOf(planetIssue)
                                + ", planetGen="
                                + String.valueOf(planetGenIssue)
                                + ", star="
                                + String.valueOf(starIssue)
                                + ", age="
                                + String.valueOf(ageIssue)
                                + ", terrain="
                                + String.valueOf(terrainIssue)
                                + ", picker="
                                + String.valueOf(pickerIssue));
                return "direct new-game preflight pending";
            }
            if (isGalatiaCustomEntityReadResolveNpe(t)) {
                System.out.println(
                        "Fixer: Galatia custom-entity readResolve NPE detail: "
                                + stackTraceToString(t));
                String customEntityIssue = ensureCustomEntitySpecsReadyForDirectNewGame();
                String fallbackIssue = ensureOrbitalJunkCustomEntitySpecFallback();
                String runtimeFactionIssue =
                        ensureRuntimeFactionsPresentForDirectNewGame(
                                new String[] {"neutral", "player"}, true);
                boolean runtimeOk =
                        runtimeFactionIssue == null
                                || isNonFatalFactionPreflightIssue(runtimeFactionIssue);
                if (customEntityIssue == null && fallbackIssue == null && runtimeOk) {
                    maybeLogNewGamePreflightIssue(
                            "galatia custom-entity readResolve repair succeeded; retry pending.");
                    return "direct new-game preflight pending";
                }
                maybeLogNewGamePreflightIssue(
                        "galatia custom-entity readResolve repair issues: customEntity="
                                + String.valueOf(customEntityIssue)
                                + ", fallback="
                                + String.valueOf(fallbackIssue)
                                + ", runtimeFaction="
                                + String.valueOf(runtimeFactionIssue));
                return "direct new-game preflight pending";
            }
            if (isProcgenNebulaBackgroundNpe(t)) {
                System.out.println(
                        "Fixer: procgen nebula/background NPE detail: " + describeThrowableChain(t));
                String ageIssue = ensureAgeGenSpecsReadyForDirectNewGame();
                String starIssue = ensureStarGenSpecsReadyForDirectNewGame();
                String pickerIssue = refreshProcgenBackgroundPickersForDirectNewGame();
                if (ageIssue == null && starIssue == null && pickerIssue == null) {
                    if (allowOrbitalJunkImmediateRetry) {
                        System.out.println(
                                "Fixer: procgen nebula/background repair succeeded; deferring retry to next watcher tick.");
                        return "direct new-game preflight pending";
                    }
                    maybeLogNewGamePreflightIssue(
                            "procgen nebula/background repair succeeded; retry pending.");
                    return "direct new-game preflight pending";
                }
                maybeLogNewGamePreflightIssue(
                        "procgen nebula/background repair issues: age="
                                + String.valueOf(ageIssue)
                                + ", star="
                                + String.valueOf(starIssue)
                                + ", picker="
                                + String.valueOf(pickerIssue));
                return "direct new-game preflight pending";
            }
            if (isProcgenNameAssignerNpe(t)) {
                System.out.println(
                        "Fixer: procgen NameAssigner NPE detail: " + describeThrowableChain(t));
                String ageIssue = ensureAgeGenSpecsReadyForDirectNewGame();
                String starIssue = ensureStarGenSpecsReadyForDirectNewGame();
                String nameIssue = ensureNameGenSpecsReadyForDirectNewGame();
                if (ageIssue == null && starIssue == null && nameIssue == null) {
                    if (allowOrbitalJunkImmediateRetry) {
                        System.out.println(
                                "Fixer: procgen NameAssigner repair succeeded; deferring retry to next watcher tick.");
                        return "direct new-game preflight pending";
                    }
                    maybeLogNewGamePreflightIssue(
                            "procgen NameAssigner repair succeeded; retry pending.");
                    return "direct new-game preflight pending";
                }
                maybeLogNewGamePreflightIssue(
                        "procgen NameAssigner repair issues: age="
                                + String.valueOf(ageIssue)
                                + ", star="
                                + String.valueOf(starIssue)
                                + ", name="
                                + String.valueOf(nameIssue));
                return "direct new-game preflight pending";
            }
            if (isProcgenPlanetGenSpecMissing(t)) {
                System.out.println(
                        "Fixer: procgen PlanetGenDataSpec missing detail: "
                                + describeThrowableChain(t));
                String ageIssue = ensureAgeGenSpecsReadyForDirectNewGame();
                String starIssue = ensureStarGenSpecsReadyForDirectNewGame();
                String planetGenIssue = ensurePlanetGenSpecsReadyForDirectNewGame();
                if (ageIssue == null && starIssue == null && planetGenIssue == null) {
                    if (allowOrbitalJunkImmediateRetry) {
                        System.out.println(
                                "Fixer: procgen PlanetGenDataSpec repair succeeded; deferring retry to next watcher tick.");
                        return "direct new-game preflight pending";
                    }
                    maybeLogNewGamePreflightIssue(
                            "procgen PlanetGenDataSpec repair succeeded; retry pending.");
                    return "direct new-game preflight pending";
                }
                maybeLogNewGamePreflightIssue(
                        "procgen PlanetGenDataSpec repair issues: age="
                                + String.valueOf(ageIssue)
                                + ", star="
                                + String.valueOf(starIssue)
                                + ", planetGen="
                                + String.valueOf(planetGenIssue));
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
                                    + "; deferring retry to next watcher tick.");
                    return "direct new-game preflight pending";
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
            logDirectNewGameDataSnapshot(lastDirectNewGameData);
            logDirectNewGameRuntimeSnapshot();
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
        String createLeaseIssue = checkDirectNewGameCreateLeaseWindow();
        if (createLeaseIssue != null) {
            return createLeaseIssue;
        }
        String createSettleIssue = checkDirectNewGameCreateSettleWindow();
        if (createSettleIssue != null) {
            return createSettleIssue;
        }
        boolean useWorkerThread =
                Boolean.parseBoolean(
                        System.getProperty("starsector.autoCampaignDirectUseWorkerThread", "false"));
        if (!useWorkerThread) {
            try {
                return tryStartDirectNewGame(ctx, titleState);
            } catch (Throwable t) {
                return "direct new-game wrapper error: " + describeThrowableChain(t);
            }
        }
        Thread worker;
        synchronized (DIRECT_NEW_GAME_WORKER_LOCK) {
            if (directNewGameWorker != null) {
                if (directNewGameWorkerCompleted || !directNewGameWorker.isAlive()) {
                    return consumeDirectNewGameWorkerOutcomeLocked();
                }
                worker = directNewGameWorker;
            } else {
                final int workerId = ++directNewGameWorkerId;
                final String workerLeaseOwner = buildDirectNewGameWorkerLeaseOwner(workerId);
                long workerLeaseMs =
                        Math.max(
                                60000L,
                                Math.max(
                                        waitMs * 12L,
                                        parseLongProperty(
                                                "starsector.autoCampaignDirectWorkerLeaseMs",
                                                900000L)));
                if (!claimDirectNewGameWorkerLease(
                        workerLeaseOwner, System.currentTimeMillis(), workerLeaseMs)) {
                    return "direct new-game preflight pending: external worker lease active";
                }
                directNewGameWorkerCompleted = false;
                directNewGameWorkerResult = null;
                directNewGameWorkerError = null;
                directNewGameWorkerMarker = null;
                directNewGameWorkerStartedAt = System.currentTimeMillis();
                directNewGameWorkerActiveId = workerId;
                directNewGameWorkerLeaseOwner = workerLeaseOwner;
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
                                            releaseDirectNewGameWorkerLease(workerLeaseOwner);
                                            synchronized (DIRECT_NEW_GAME_WORKER_LOCK) {
                                                boolean ownsWorkerSlot =
                                                        directNewGameWorker
                                                                == Thread.currentThread();
                                                boolean ownsWorkerId =
                                                        directNewGameWorkerActiveId == workerId;
                                                if (ownsWorkerSlot || ownsWorkerId) {
                                                    if (!ownsWorkerSlot) {
                                                        System.out.println(
                                                                "Fixer: direct-new-game worker recovered outcome publish by workerId (worker="
                                                                        + Thread.currentThread().getName()
                                                                        + "#"
                                                                        + Thread.currentThread().getId()
                                                                        + ", activeWorkerId="
                                                                        + directNewGameWorkerActiveId
                                                                        + ").");
                                                    }
                                                    if (directNewGameWorker == null) {
                                                        directNewGameWorker = Thread.currentThread();
                                                    }
                                                    directNewGameWorkerResult = localResult;
                                                    directNewGameWorkerError = localError;
                                                    directNewGameWorkerMarker =
                                                            directNewGameLastNullMarker;
                                                    directNewGameWorkerCompleted = true;
                                                } else {
                                                    System.out.println(
                                                            "Fixer: direct-new-game worker dropped outcome publish due ownership mismatch (worker="
                                                                    + Thread.currentThread().getName()
                                                                    + "#"
                                                                    + Thread.currentThread().getId()
                                                                    + ", activeWorkerId="
                                                                    + directNewGameWorkerActiveId
                                                                    + ", localResult="
                                                                    + String.valueOf(localResult)
                                                                    + ", localError="
                                                                    + (localError == null
                                                                            ? "<none>"
                                                                            : describeThrowableChain(
                                                                                    localError))
                                                                    + ", marker="
                                                                    + String.valueOf(
                                                                            directNewGameLastNullMarker)
                                                                    + ").");
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
                    directNewGameWorkerMarker = null;
                    directNewGameWorkerActiveId = 0;
                    if (workerLeaseOwner.equals(directNewGameWorkerLeaseOwner)) {
                        directNewGameWorkerLeaseOwner = null;
                    }
                    releaseDirectNewGameWorkerLease(workerLeaseOwner);
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
            long now = System.currentTimeMillis();
            long ageMs =
                    directNewGameWorkerStartedAt > 0L
                            ? Math.max(0L, now - directNewGameWorkerStartedAt)
                            : waitMs;
            long hardInterruptMs =
                    Math.max(
                            waitMs * 8L,
                            parseLongProperty(
                                    "starsector.autoCampaignDirectWorkerHardInterruptMs", 120000L));
            boolean interruptStaleWorker =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignDirectWorkerInterruptWhenStale",
                                    "true"));
            if (directNewGameWorker == worker
                    && worker.isAlive()
                    && interruptStaleWorker
                    && ageMs >= hardInterruptMs) {
                try {
                    worker.interrupt();
                } catch (Throwable ignored) {
                }
                return "direct new-game preflight pending: stale worker interrupted (age="
                        + ageMs
                        + "ms, hardInterrupt="
                        + hardInterruptMs
                        + "ms)";
            }
            touchDirectNewGameWorkerLease(directNewGameWorkerLeaseOwner, now);
            if (now - directNewGameWorkerPendingLogAt >= 5000L) {
                System.out.println(
                        "Fixer: direct-new-game worker still running after timeout; preserving attempt (age="
                                + ageMs
                                + "ms, hardInterrupt="
                                + hardInterruptMs
                                + "ms).");
                directNewGameWorkerPendingLogAt = now;
            }
            return "direct new-game preflight pending: active attempt still running (age="
                    + ageMs
                    + "ms, hardInterrupt="
                    + hardInterruptMs
                    + "ms)";
        }
    }

    private static String consumeDirectNewGameWorkerOutcomeLocked() {
        Thread consumedWorker = directNewGameWorker;
        String consumedWorkerName =
                consumedWorker == null
                        ? "<null>"
                        : consumedWorker.getName() + "#" + consumedWorker.getId();
        String result = directNewGameWorkerResult;
        Throwable error = directNewGameWorkerError;
        String resultMarker = directNewGameWorkerMarker;
        String leaseOwner = directNewGameWorkerLeaseOwner;
        directNewGameWorker = null;
        directNewGameWorkerStartedAt = 0L;
        directNewGameWorkerCompleted = false;
        directNewGameWorkerResult = null;
        directNewGameWorkerError = null;
        directNewGameWorkerMarker = null;
        directNewGameWorkerActiveId = 0;
        directNewGameWorkerLeaseOwner = null;
        directNewGameWorkerPendingLogAt = 0L;
        if (leaseOwner != null && leaseOwner.length() > 0) {
            releaseDirectNewGameWorkerLease(leaseOwner);
        }
        if (error != null) {
            return "direct new-game wrapper error: " + describeThrowableChain(error);
        }
        if (result == null) {
            String nullMarker = resultMarker != null ? resultMarker : directNewGameLastNullMarker;
            System.out.println(
                    "Fixer: direct-new-game worker outcome completed with null result (worker="
                            + consumedWorkerName
                            + ", marker="
                            + String.valueOf(nullMarker)
                            + ").");
            String normalizedMarker =
                    nullMarker == null ? "" : nullMarker.trim().toLowerCase();
            boolean expectedNullOutcome =
                    "pre-invoke-ready".equals(normalizedMarker)
                            || "post-invoke-ready".equals(normalizedMarker);
            if (!expectedNullOutcome) {
                directNewGameLastNullMarker = null;
                return "direct new-game preflight pending: worker-completed-without-result(marker="
                        + String.valueOf(nullMarker)
                        + ")";
            }
        }
        directNewGameLastNullMarker = null;
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

    private static boolean isProcgenNebulaBackgroundNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("starsystemgenerator.picknebulaandbackground") >= 0;
    }

    private static boolean isProcgenCampaignPlanetInitNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("campaignplanet.<init>") >= 0
                && lower.indexOf("starsystem.initstar") >= 0
                && lower.indexOf("data.scripts.world.systems.galatia.generate") >= 0;
    }

    private static boolean isProcgenNameAssignerNpe(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        return lower.indexOf("nullpointerexception") >= 0
                && lower.indexOf("nameassigner.assignnames") >= 0;
    }

    private static boolean isGalatiaCustomEntityReadResolveNpe(Throwable t) {
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
                && lower.indexOf("data.scripts.world.systems.galatia.generate") >= 0;
    }

    private static boolean isProcgenPlanetGenSpecMissing(Throwable t) {
        if (t == null) {
            return false;
        }
        String trace = stackTraceToString(t);
        if (trace == null || trace.length() == 0) {
            return false;
        }
        String lower = trace.toLowerCase();
        boolean hasPlanetGenMarker =
                lower.indexOf("planetgendataspec") >= 0
                        || lower.indexOf("planetconditiongenerator.createcontext") >= 0;
        boolean hasNotFound =
                (lower.indexOf("spec of class") >= 0 && lower.indexOf("not found") >= 0)
                        || lower.indexOf("runtimeexception: spec of class") >= 0;
        return hasPlanetGenMarker && hasNotFound;
    }

    private static String refreshProcgenBackgroundPickersForDirectNewGame() {
        try {
            Class<?> generatorClass =
                    Class.forName("com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator");
            Method refresh = findMethodRecursive(generatorClass, "updateBackgroundPickers");
            if (refresh == null) {
                return "updateBackgroundPickers method unavailable";
            }
            refresh.setAccessible(true);
            refresh.invoke(null);
            return null;
        } catch (Throwable t) {
            return "updateBackgroundPickers refresh exception: " + describeThrowableChain(t);
        }
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
                        "knights_of_ludd",
                        "persean",
                        "remnant",
                        "derelict",
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

            final String[] runtimeRequiredFactionIds =
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
                        "knights_of_ludd",
                        "persean",
                        "remnant"
                    };
            String runtimeFactionIssue =
                    ensureRuntimeFactionsPresentForDirectNewGame(
                            runtimeRequiredFactionIds, allowRepair);
            if (runtimeFactionIssue != null) {
                missing.add(runtimeFactionIssue);
            }

            if (!missing.isEmpty()) {
                List<String> fatalMissing = new ArrayList<String>();
                List<String> nonFatalMissing = new ArrayList<String>();
                for (String item : missing) {
                    if (isNonFatalFactionPreflightIssue(item)) {
                        nonFatalMissing.add(item);
                    } else {
                        fatalMissing.add(item);
                    }
                }
                if (!nonFatalMissing.isEmpty()) {
                    System.out.println(
                            "Fixer: non-fatal faction preflight issues: "
                                    + String.join(", ", nonFatalMissing));
                }
                if (!fatalMissing.isEmpty()) {
                    return "spec store not ready: " + String.join(", ", fatalMissing);
                }
            }
            return null;
        } catch (Throwable t) {
            return "faction preflight exception: " + describeThrowableChain(t);
        }
    }

    private static boolean isNonFatalFactionPreflightIssue(String issue) {
        if (issue == null) {
            return false;
        }
        String lower = issue.trim().toLowerCase();
        if (lower.length() == 0) {
            return false;
        }
        if (lower.startsWith("faction:threat")
                || lower.startsWith("faction:dweller")
                || lower.startsWith("faction:omega")
                || lower.startsWith("faction:derelict")
                || lower.startsWith("faction:remnant")
                || lower.startsWith("faction:remnants")) {
            return true;
        }
        if (lower.startsWith("player-portraits:")) {
            return true;
        }
        if (lower.startsWith("runtime-faction-manager")) {
            return true;
        }
        return false;
    }

    private static boolean hasMissingCoreFactionSpecIssue(String issue) {
        if (issue == null) {
            return false;
        }
        String lower = issue.trim().toLowerCase();
        if (lower.length() == 0) {
            return false;
        }
        if (lower.indexOf("faction:neutral") >= 0 || lower.indexOf("faction:player") >= 0) {
            return true;
        }
        if (lower.indexOf("runtime-faction-manager") >= 0
                && (lower.indexOf("neutral") >= 0 || lower.indexOf("player") >= 0)) {
            return true;
        }
        return false;
    }

    private static String ensureRuntimeFactionsPresentForDirectNewGame(
            String[] requiredFactionIds, boolean allowRepair) {
        if (requiredFactionIds == null || requiredFactionIds.length == 0) {
            return null;
        }
        try {
            String defaultsIssue = ensureFactionDefaultsForRepair();
            if (defaultsIssue != null) {
                System.out.println(
                        "Fixer: runtime faction defaults precheck warning: " + defaultsIssue);
            }

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

            if (allowRepair && isRuntimeFactionManagerRepairReady(manager)) {
                invokeRuntimeFactionManagerReadResolve(manager);
                missing = collectMissingRuntimeFactionIds(manager, requiredFactionIds);
            }
            String runtimeRepairIssue = null;
            if (allowRepair && !missing.isEmpty() && isRuntimeFactionManagerRepairReady(manager)) {
                runtimeRepairIssue = injectMissingRuntimeFactions(manager, missing);
                if (runtimeRepairIssue != null) {
                    System.out.println(
                            "Fixer: runtime faction-manager repair warning: " + runtimeRepairIssue);
                }
                invokeRuntimeFactionManagerReadResolve(manager);
                missing = collectMissingRuntimeFactionIds(manager, requiredFactionIds);
            }
            if (missing.isEmpty()) {
                if (runtimeRepairIssue != null) {
                    return "runtime-faction-manager-repair:" + runtimeRepairIssue;
                }
                return null;
            }
            // Runtime faction manager can remain partially empty until campaign creation fully settles.
            // Report as non-fatal runtime detail so direct new-game flow can continue.
            return "runtime-faction-manager:missing=" + String.join(", ", missing);
        } catch (Throwable t) {
            return "runtime-faction-manager:" + describeThrowableChain(t);
        }
    }

    private static void probeFactionClassInitialization(String stage) {
        String safeStage = stage == null ? "unknown" : stage;
        try {
            Class.forName("com.fs.starfarer.campaign.Faction");
            System.out.println(
                    "Fixer: faction class init probe (" + safeStage + ") ready.");
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: faction class init probe (" + safeStage + ") failed: "
                            + describeThrowableChain(t));
            System.out.println(
                    "Fixer: faction class init probe (" + safeStage + ") stack:\n"
                            + stackTraceToString(t));
            Throwable cause = t.getCause();
            if (cause != null) {
                System.out.println(
                        "Fixer: faction class init probe (" + safeStage + ") cause stack:\n"
                                + stackTraceToString(cause));
            }
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

    private static boolean isRuntimeFactionManagerRepairReady(Object manager) {
        if (manager == null) {
            return false;
        }
        try {
            Field factionsField = findFieldRecursive(manager.getClass(), "factions");
            if (factionsField == null) {
                return false;
            }
            factionsField.setAccessible(true);
            Object mapObj = factionsField.get(manager);
            if (!(mapObj instanceof Map)) {
                return false;
            }
            // Allow runtime-faction bootstrap even when the map is currently empty.
            // The previous non-empty gate prevented injection in the exact startup state
            // where synthetic fleet recovery needs it the most.
            return true;
        } catch (Throwable t) {
            return false;
        }
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
            // Treat reflective getSpec failures as usable to avoid repeatedly injecting
            // runtime factions while the faction class is still initializing.
            return true;
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

            Class<?> factionClass;
            try {
                factionClass = Class.forName("com.fs.starfarer.campaign.Faction");
            } catch (Throwable factionLoadFailure) {
                System.out.println(
                        "Fixer: runtime faction-manager repair failed loading Faction class:\n"
                                + stackTraceToString(factionLoadFailure));
                return describeThrowableChain(factionLoadFailure);
            }
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
            String chain = describeThrowableChain(t);
            String lower = chain == null ? "" : chain.toLowerCase();
            if (lower.indexOf("noclassdeffounderror") >= 0
                    && lower.indexOf("com/fs/starfarer/campaign/faction") >= 0) {
                System.out.println(
                        "Fixer: runtime faction-manager repair exception stack:\n"
                                + stackTraceToString(t));
            }
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
            String chain = describeThrowableChain(t);
            String lower = chain == null ? "" : chain.toLowerCase();
            if (lower.indexOf("already exists") >= 0) {
                // Idempotent duplicate registration during repeated warmup attempts.
                return null;
            }
            return "faction-repair-exception:" + chain;
        }
    }

    private static Object loadFactionJsonForRepair(String factionId) {
        if (factionId == null || factionId.trim().isEmpty()) {
            return null;
        }
        String cleanId = factionId.trim();
        Set<String> directPaths = new LinkedHashSet<String>();
        addFactionConfigPathCandidatesForRepair(directPaths, cleanId);
        for (String directPath : directPaths) {
            if (directPath == null || directPath.trim().isEmpty()) {
                continue;
            }
            Object direct = loadConfigJsonViaLoadingUtils(directPath);
            if (direct != null) {
                return direct;
            }
        }
        try {
            List<String> entries = readIndexEntries("data/world/factions");
            Set<String> suffixCandidates = resolveFactionConfigSuffixCandidatesForRepair(cleanId);
            for (String entry : entries) {
                if (entry == null) {
                    continue;
                }
                String rel = entry.replace('\\', '/').trim();
                if (rel.length() == 0 || !rel.toLowerCase().endsWith(".faction")) {
                    continue;
                }
                boolean suffixMatch = false;
                String relLower = rel.toLowerCase();
                for (String suffix : suffixCandidates) {
                    if (suffix != null && relLower.endsWith(suffix)) {
                        suffixMatch = true;
                        break;
                    }
                }
                if (!suffixMatch) {
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
                    try {
                        ctor.setAccessible(true);
                        setter.setAccessible(true);
                        setter.invoke(null, ctor.newInstance(shipRolesJson));
                    } catch (Throwable shipRoleFailure) {
                        String chain = describeThrowableChain(shipRoleFailure);
                        String lower = chain == null ? "" : chain.toLowerCase();
                        if (lower.indexOf("ship hull variant [") >= 0
                                && lower.indexOf("not found") >= 0) {
                            System.out.println(
                                    "Fixer: default ship-role warmup tolerated missing variant: "
                                            + chain);
                        } else {
                            throw shipRoleFailure;
                        }
                    }
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

    private static String ensurePlanetGenSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> planetGenSpecClass =
                    Class.forName("com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "planet-gen spec preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds = new String[] {"gas_giant", "arid", "terran", "barren"};
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, planetGenSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText = readResourceTextForRepair("data/campaign/procgen/planet_gen_data.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "planet_gen_data.csv unavailable, missing critical planet-gen specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "planet_gen_data.csv parse returned no rows, missing critical planet-gen specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id")
                    || !headerIndex.containsKey("category")
                    || !headerIndex.containsKey("frequency")) {
                return "planet-gen csv missing required headers (id/category/frequency)";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "planet-gen json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = planetGenSpecClass.getDeclaredConstructor(jsonClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor =
                            planetGenSpecClass.getDeclaredConstructor(
                                    Class.forName("org.json.JSONObject"));
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "planet-gen spec constructor unavailable";
            }
            specCtor.setAccessible(true);

            LinkedHashSet<String> reservedHeaders = new LinkedHashSet<String>();
            reservedHeaders.add("id");
            reservedHeaders.add("type");
            reservedHeaders.add("category");
            reservedHeaders.add("frequency");
            reservedHeaders.add("haboffsetmin");
            reservedHeaders.add("haboffsetmax");
            reservedHeaders.add("haboffsetyoung");
            reservedHeaders.add("haboffsetaverage");
            reservedHeaders.add("haboffsetold");
            reservedHeaders.add("tags");
            reservedHeaders.add("proborbits");
            reservedHeaders.add("minorbits");
            reservedHeaders.add("maxorbits");
            reservedHeaders.add("minradius");
            reservedHeaders.add("maxradius");
            reservedHeaders.add("mincolor");
            reservedHeaders.add("maxcolor");

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
                if (lookupSpecById(specStoreClass, planetGenSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                String category = getCsvValueForRepair(row, headerIndex, "category");
                Double frequency = parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, "frequency"));
                if (category == null || category.trim().isEmpty() || frequency == null) {
                    skipped++;
                    if (isCriticalIdForRepair(criticalIds, id)) {
                        failed.add(id + "(missing-category-or-frequency)");
                    }
                    continue;
                }

                try {
                    Object rowJson = jsonCtor.newInstance();
                    jsonPut.invoke(rowJson, "id", id);
                    jsonPut.invoke(rowJson, "category", category);
                    jsonPut.invoke(rowJson, "frequency", frequency);

                    addCsvStringToJsonForRepair(rowJson, jsonPut, "tags", row, headerIndex, "tags");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "minColor", row, headerIndex, "mincolor");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "maxColor", row, headerIndex, "maxcolor");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "habOffsetMin", row, headerIndex, "haboffsetmin");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "habOffsetMax", row, headerIndex, "haboffsetmax");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "habOffsetYOUNG", row, headerIndex, "haboffsetyoung");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "habOffsetAVERAGE", row, headerIndex, "haboffsetaverage");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "habOffsetOLD", row, headerIndex, "haboffsetold");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "probOrbits", row, headerIndex, "proborbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minOrbits", row, headerIndex, "minorbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxOrbits", row, headerIndex, "maxorbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minRadius", row, headerIndex, "minradius");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxRadius", row, headerIndex, "maxradius");

                    for (Map.Entry<String, Integer> header : headerIndex.entrySet()) {
                        if (header == null) {
                            continue;
                        }
                        String key = header.getKey();
                        if (key == null || reservedHeaders.contains(key)) {
                            continue;
                        }
                        Double multiplier =
                                parseCsvDoubleForRepair(getCsvValueForRepair(row, headerIndex, key));
                        if (multiplier == null) {
                            continue;
                        }
                        jsonPut.invoke(rowJson, key, multiplier);
                    }

                    Object created = specCtor.newInstance(rowJson);
                    registerSpec.invoke(null, planetGenSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, planetGenSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, planetGenSpecClass, id) != null) {
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
                if (lookupSpecById(specStoreClass, planetGenSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: planet-gen spec preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical planet-gen specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "planet-gen spec preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "planet-gen spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureStarGenSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> starGenSpecClass =
                    Class.forName("com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "star-gen spec preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds = new String[] {"star_yellow", "star_orange", "star_red_dwarf"};
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, starGenSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText = readResourceTextForRepair("data/campaign/procgen/star_gen_data.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "star_gen_data.csv unavailable, missing critical star-gen specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "star_gen_data.csv parse returned no rows, missing critical star-gen specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id")
                    || !headerIndex.containsKey("minradius")
                    || !headerIndex.containsKey("maxradius")
                    || !headerIndex.containsKey("coronamin")
                    || !headerIndex.containsKey("coronamult")
                    || !headerIndex.containsKey("coronavar")
                    || !headerIndex.containsKey("solarwind")
                    || !headerIndex.containsKey("minflare")
                    || !headerIndex.containsKey("maxflare")
                    || !headerIndex.containsKey("crlossmult")
                    || !headerIndex.containsKey("freqyoung")
                    || !headerIndex.containsKey("freqaverage")
                    || !headerIndex.containsKey("freqold")) {
                return "star-gen csv missing required headers";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "star-gen json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = starGenSpecClass.getDeclaredConstructor(jsonClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor =
                            starGenSpecClass.getDeclaredConstructor(
                                    Class.forName("org.json.JSONObject"));
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "star-gen spec constructor unavailable";
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
                if (lookupSpecById(specStoreClass, starGenSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                try {
                    Object rowJson = jsonCtor.newInstance();
                    jsonPut.invoke(rowJson, "id", id);
                    addCsvStringToJsonForRepair(rowJson, jsonPut, "age", row, headerIndex, "age");
                    addCsvStringToJsonForRepair(rowJson, jsonPut, "tags", row, headerIndex, "tags");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "lightColorMin", row, headerIndex, "lightcolormin");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "lightColorMax", row, headerIndex, "lightcolormax");

                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "freqYOUNG", row, headerIndex, "freqyoung");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "freqAVERAGE", row, headerIndex, "freqaverage");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "freqOLD", row, headerIndex, "freqold");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minRadius", row, headerIndex, "minradius");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxRadius", row, headerIndex, "maxradius");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "habZoneStart", row, headerIndex, "habzonestart");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "probOrbits", row, headerIndex, "proborbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minOrbits", row, headerIndex, "minorbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxOrbits", row, headerIndex, "maxorbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "coronaMin", row, headerIndex, "coronamin");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "coronaMult", row, headerIndex, "coronamult");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "coronaVar", row, headerIndex, "coronavar");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "solarWind", row, headerIndex, "solarwind");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minFlare", row, headerIndex, "minflare");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxFlare", row, headerIndex, "maxflare");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "crLossMult", row, headerIndex, "crlossmult");

                    Object created = specCtor.newInstance(rowJson);
                    registerSpec.invoke(null, starGenSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, starGenSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, starGenSpecClass, id) != null) {
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
                if (lookupSpecById(specStoreClass, starGenSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: star-gen spec preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical star-gen specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "star-gen spec preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "star-gen spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureAgeGenSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> ageGenSpecClass =
                    Class.forName("com.fs.starfarer.api.impl.campaign.procgen.AgeGenDataSpec");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "age-gen spec preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            final String[] criticalIds = new String[] {"YOUNG", "AVERAGE", "OLD"};
            List<String> missingBefore = new ArrayList<String>();
            for (String id : criticalIds) {
                if (lookupSpecById(specStoreClass, ageGenSpecClass, id) == null) {
                    missingBefore.add(id);
                }
            }

            String csvText = readResourceTextForRepair("data/campaign/procgen/age_gen_data.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "age_gen_data.csv unavailable, missing critical age-gen specs: "
                        + String.join(", ", missingBefore);
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                if (missingBefore.isEmpty()) {
                    return null;
                }
                return "age_gen_data.csv parse returned no rows, missing critical age-gen specs: "
                        + String.join(", ", missingBefore);
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("id")) {
                return "age-gen csv missing required id header";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "age-gen json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = ageGenSpecClass.getDeclaredConstructor(jsonClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor =
                            ageGenSpecClass.getDeclaredConstructor(
                                    Class.forName("org.json.JSONObject"));
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "age-gen spec constructor unavailable";
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
                if (lookupSpecById(specStoreClass, ageGenSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                try {
                    Object rowJson = jsonCtor.newInstance();
                    jsonPut.invoke(rowJson, "id", id);
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "minExtraOrbits", row, headerIndex, "minextraorbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "maxExtraOrbits", row, headerIndex, "maxextraorbits");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "probNebula", row, headerIndex, "probnebula");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "nebulaDensity", row, headerIndex, "nebuladensity");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "freqNormal", row, headerIndex, "freqnormal");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "freqBinary", row, headerIndex, "freqbinary");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "freqTrinary", row, headerIndex, "freqtrinary");

                    Object created = specCtor.newInstance(rowJson);
                    registerSpec.invoke(null, ageGenSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, ageGenSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, ageGenSpecClass, id) != null) {
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
                if (lookupSpecById(specStoreClass, ageGenSpecClass, id) == null) {
                    missingAfter.add(id);
                }
            }

            if (inserted > 0 || !failed.isEmpty() || !missingBefore.isEmpty()) {
                System.out.println(
                        "Fixer: age-gen spec preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size());
            }

            if (!missingAfter.isEmpty()) {
                return "missing critical age-gen specs after preload: "
                        + String.join(", ", missingAfter);
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "age-gen spec preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "age-gen spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String ensureNameGenSpecsReadyForDirectNewGame() {
        try {
            Class<?> specStoreClass = Class.forName("com.fs.starfarer.loading.SpecStore");
            Class<?> nameGenSpecClass =
                    Class.forName("com.fs.starfarer.api.impl.campaign.procgen.NameGenData");
            Method registerSpec =
                    findMethodRecursive(specStoreClass, "o00000", Class.class, String.class, Object.class);
            if (registerSpec == null) {
                return "name-gen spec preflight register method unavailable";
            }
            registerSpec.setAccessible(true);

            String coverageBefore = probeNameGenCoverageIssue(nameGenSpecClass);
            if (coverageBefore == null) {
                return null;
            }

            String csvText = readResourceTextForRepair("data/campaign/procgen/name_gen_data.csv");
            if (csvText == null || csvText.trim().isEmpty()) {
                return "name_gen_data.csv unavailable, coverage issue: " + coverageBefore;
            }

            List<List<String>> rows = parseCsvRowsForRepair(csvText);
            if (rows.isEmpty()) {
                return "name_gen_data.csv parse returned no rows, coverage issue: " + coverageBefore;
            }

            Map<String, Integer> headerIndex = buildCsvHeaderIndexForRepair(rows.get(0));
            if (!headerIndex.containsKey("name")) {
                return "name-gen csv missing required name header";
            }

            Class<?> jsonClass = Class.forName("org.json.JSONObject");
            java.lang.reflect.Constructor<?> jsonCtor = jsonClass.getDeclaredConstructor();
            jsonCtor.setAccessible(true);
            Method jsonPut = findMethodRecursive(jsonClass, "put", String.class, Object.class);
            if (jsonPut == null) {
                return "name-gen json put method unavailable";
            }
            jsonPut.setAccessible(true);

            java.lang.reflect.Constructor<?> specCtor = null;
            try {
                specCtor = nameGenSpecClass.getDeclaredConstructor(jsonClass);
            } catch (Throwable ignored) {
            }
            if (specCtor == null) {
                try {
                    specCtor =
                            nameGenSpecClass.getDeclaredConstructor(
                                    Class.forName("org.json.JSONObject"));
                } catch (Throwable ignored) {
                }
            }
            if (specCtor == null) {
                return "name-gen spec constructor unavailable";
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
                String id = getCsvValueForRepair(row, headerIndex, "name");
                if (id == null || id.trim().isEmpty()) {
                    skipped++;
                    continue;
                }
                id = id.trim();
                if (id.startsWith("#")) {
                    continue;
                }
                if (lookupSpecById(specStoreClass, nameGenSpecClass, id) != null) {
                    alreadyPresent++;
                    continue;
                }

                try {
                    Object rowJson = jsonCtor.newInstance();
                    jsonPut.invoke(rowJson, "name", id);
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "secondary", row, headerIndex, "secondary");
                    addCsvStringToJsonForRepair(rowJson, jsonPut, "tags", row, headerIndex, "tags");
                    addCsvStringToJsonForRepair(
                            rowJson, jsonPut, "parents", row, headerIndex, "parents");
                    addCsvDoubleToJsonForRepair(
                            rowJson, jsonPut, "frequency", row, headerIndex, "frequency");

                    Boolean reusable =
                            parseCsvBooleanForRepair(getCsvValueForRepair(row, headerIndex, "reusable"));
                    if (reusable != null) {
                        jsonPut.invoke(rowJson, "reusable", reusable.booleanValue());
                    }

                    Object created = specCtor.newInstance(rowJson);
                    registerSpec.invoke(null, nameGenSpecClass, id, created);
                    if (lookupSpecById(specStoreClass, nameGenSpecClass, id) == null) {
                        failed.add(id + "(verify-failed)");
                        continue;
                    }
                    inserted++;
                } catch (Throwable t) {
                    String chain = describeThrowableChain(t);
                    String lower = chain == null ? "" : chain.toLowerCase();
                    if (lower.indexOf("already exists") >= 0) {
                        try {
                            if (lookupSpecById(specStoreClass, nameGenSpecClass, id) != null) {
                                alreadyPresent++;
                                continue;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    failed.add(id + "(" + chain + ")");
                }
            }

            String coverageAfter = probeNameGenCoverageIssue(nameGenSpecClass);

            if (inserted > 0 || !failed.isEmpty()) {
                System.out.println(
                        "Fixer: name-gen spec preflight inserted="
                                + inserted
                                + " alreadyPresent="
                                + alreadyPresent
                                + " skipped="
                                + skipped
                                + " failed="
                                + failed.size()
                                + " coverageBefore="
                                + coverageBefore
                                + " coverageAfter="
                                + coverageAfter);
            }

            if (coverageAfter != null) {
                return "name-gen coverage unresolved after preload: " + coverageAfter;
            }
            if (!failed.isEmpty()) {
                int limit = Math.min(6, failed.size());
                return "name-gen spec preload partial failures: "
                        + String.join(", ", failed.subList(0, limit))
                        + (failed.size() > limit ? " ... +" + (failed.size() - limit) : "");
            }
            return null;
        } catch (Throwable t) {
            return "name-gen spec preflight exception: " + describeThrowableChain(t);
        }
    }

    private static String probeNameGenCoverageIssue(Class<?> nameGenSpecClass) {
        if (nameGenSpecClass == null) {
            return "name-gen spec class unavailable";
        }
        try {
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSettings = findMethodRecursive(globalClass, "getSettings");
            if (getSettings == null) {
                return "Global.getSettings unavailable";
            }
            getSettings.setAccessible(true);
            Object settings = getSettings.invoke(null);
            if (settings == null) {
                return "Global.getSettings returned null";
            }
            Method getAllSpecs = findMethodRecursive(settings.getClass(), "getAllSpecs", Class.class);
            if (getAllSpecs == null) {
                return "SettingsAPI.getAllSpecs unavailable";
            }
            getAllSpecs.setAccessible(true);
            Object allSpecsObj = getAllSpecs.invoke(settings, nameGenSpecClass);
            if (!(allSpecsObj instanceof Collection)) {
                return "SettingsAPI.getAllSpecs returned non-collection";
            }

            Collection allSpecs = (Collection) allSpecsObj;
            Method hasTag = findMethodRecursive(nameGenSpecClass, "hasTag", String.class);
            if (hasTag == null) {
                return "NameGenData.hasTag unavailable";
            }
            hasTag.setAccessible(true);

            int total = 0;
            int constellationCount = 0;
            int starCount = 0;
            int planetCount = 0;
            for (Object spec : allSpecs) {
                if (spec == null || !nameGenSpecClass.isInstance(spec)) {
                    continue;
                }
                total++;
                if (Boolean.TRUE.equals(hasTag.invoke(spec, "constellation"))) {
                    constellationCount++;
                }
                if (Boolean.TRUE.equals(hasTag.invoke(spec, "star"))) {
                    starCount++;
                }
                if (Boolean.TRUE.equals(hasTag.invoke(spec, "planet"))) {
                    planetCount++;
                }
            }
            if (total <= 0) {
                return "no name-gen specs available";
            }
            ArrayList<String> missingTags = new ArrayList<String>();
            if (constellationCount <= 0) {
                missingTags.add("constellation");
            }
            if (starCount <= 0) {
                missingTags.add("star");
            }
            if (planetCount <= 0) {
                missingTags.add("planet");
            }
            if (!missingTags.isEmpty()) {
                return "missing required name-gen tags: "
                        + String.join(", ", missingTags)
                        + " (total="
                        + total
                        + ")";
            }
            return null;
        } catch (Throwable t) {
            return "name-gen coverage probe exception: " + describeThrowableChain(t);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int removeSpecIdsForClass(
            Class<?> specStoreClass, Class<?> specClass, Set<String> ids) {
        if (specStoreClass == null || specClass == null || ids == null || ids.isEmpty()) {
            return 0;
        }
        int removed = 0;
        try {
            Field classToSpecsField = findFieldRecursive(specStoreClass, "int");
            if (classToSpecsField == null) {
                return 0;
            }
            classToSpecsField.setAccessible(true);
            Object classToSpecsObj = classToSpecsField.get(null);
            if (!(classToSpecsObj instanceof Map)) {
                return 0;
            }
            Map classToSpecs = (Map) classToSpecsObj;
            Object perClassObj = classToSpecs.get(specClass);
            if (!(perClassObj instanceof Map)) {
                return 0;
            }
            Map perClassMap = (Map) perClassObj;
            for (String id : ids) {
                if (id == null) {
                    continue;
                }
                String cleanId = id.trim();
                if (cleanId.length() == 0) {
                    continue;
                }
                if (perClassMap.remove(cleanId) != null) {
                    removed++;
                }
            }
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: unable to remove specific preload specs for "
                            + specClass.getName()
                            + ": "
                            + describeThrowableChain(t));
        }
        return removed;
    }

    private static boolean isPendingColonyTargetResult(String result) {
        if (result == null || result.length() == 0) {
            return false;
        }
        String lower = result.toLowerCase();
        return lower.startsWith("pending:");
    }

    private static String startDeferredColonyVisitWorker(
            final long colonyProbeDelayMs, final long colonyProbeTimeoutMs) {
        synchronized (AUTO_CAMPAIGN_COLONY_VISIT_WORKER_LOCK) {
            if (autoCampaignColonyVisitWorker != null && autoCampaignColonyVisitWorker.isAlive()) {
                long ageMs =
                        Math.max(
                                0L,
                                System.currentTimeMillis() - autoCampaignColonyVisitWorkerStartedAt);
                return "pending:deferred-colony-worker-active("
                        + ageMs
                        + "ms,id="
                        + autoCampaignColonyVisitWorkerActiveId
                        + ")";
            }
            autoCampaignColonyVisitWorkerStartedAt = System.currentTimeMillis();
            final int workerId = ++autoCampaignColonyVisitWorkerId;
            autoCampaignColonyVisitWorkerActiveId = workerId;
            autoCampaignColonyVisitWorker =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        long delayMs = Math.max(0L, colonyProbeDelayMs);
                                        if (delayMs > 0L) {
                                            try {
                                                Thread.sleep(delayMs);
                                            } catch (InterruptedException ie) {
                                                Thread.currentThread().interrupt();
                                                System.out.println(
                                                        "Fixer: deferred colony probe worker interrupted during delay.");
                                                return;
                                            }
                                        }
                                        String result =
                                                tryPrimeColonyInteractionTargetWithTimeout(
                                                        Math.max(1000L, colonyProbeTimeoutMs));
                                        if (result == null) {
                                            System.out.println(
                                                    "Fixer: deferred colony probe primed interaction target successfully.");
                                        } else {
                                            System.out.println(
                                                    "Fixer: deferred colony probe result=" + result);
                                        }
                                    } catch (Throwable t) {
                                        System.out.println(
                                                "Fixer: deferred colony probe worker error: "
                                                        + describeThrowableChain(t));
                                    }
                                }
                            },
                            "fixer-colony-visit-worker-" + workerId);
            autoCampaignColonyVisitWorker.setDaemon(true);
            autoCampaignColonyVisitWorker.start();
            return null;
        }
    }

    private static String tryPrimeColonyInteractionTargetWithTimeout(long timeoutMs) {
        boolean guardEnabled =
                Boolean.parseBoolean(
                        System.getProperty("starsector.autoCampaignGuardColonyProbe", "true"));
        if (!guardEnabled) {
            return tryPrimeColonyInteractionTargetImmediate();
        }
        long effectiveTimeoutMs = Math.max(500L, timeoutMs);
        Thread workerToJoin = null;
        int workerId = 0;
        synchronized (AUTO_CAMPAIGN_COLONY_PROBE_LOCK) {
            if (autoCampaignColonyProbeWorker != null) {
                if (autoCampaignColonyProbeWorker.isAlive()) {
                    long ageMs =
                            Math.max(0L, System.currentTimeMillis() - autoCampaignColonyProbeStartedAt);
                    long staleAfterMs =
                            Math.max(
                                    30000L,
                                    parseLongProperty(
                                            "starsector.autoCampaignColonyProbeStaleAfterMs",
                                            Math.max(120000L, effectiveTimeoutMs * 6L)));
                    if (ageMs >= staleAfterMs) {
                        System.out.println(
                                "Fixer: auto campaign interrupting stale colony probe worker id="
                                        + autoCampaignColonyProbeActiveId
                                        + " age="
                                        + ageMs
                                        + "ms.");
                        try {
                            autoCampaignColonyProbeWorker.interrupt();
                        } catch (Throwable ignored) {
                        }
                    } else {
                        long now = System.currentTimeMillis();
                        if (now - autoCampaignColonyProbePendingLogAt >= 5000L) {
                            System.out.println(
                                    "Fixer: auto campaign colony probe worker active id="
                                            + autoCampaignColonyProbeActiveId
                                            + " age="
                                            + ageMs
                                            + "ms.");
                            autoCampaignColonyProbePendingLogAt = now;
                        }
                        return "pending:colony-probe-active(" + ageMs + "ms)";
                    }
                } else if (autoCampaignColonyProbeCompleted) {
                    Throwable completedError = autoCampaignColonyProbeError;
                    String completedResult = autoCampaignColonyProbeResult;
                    int completedId = autoCampaignColonyProbeActiveId;
                    autoCampaignColonyProbeWorker = null;
                    autoCampaignColonyProbeStartedAt = 0L;
                    autoCampaignColonyProbeActiveId = 0;
                    if (completedError != null) {
                        return "pending:colony-probe-error:" + describeThrowableChain(completedError);
                    }
                    if (completedResult == null) {
                        System.out.println(
                                "Fixer: auto campaign adopting completed colony probe id="
                                        + completedId
                                        + " result=success.");
                    } else {
                        System.out.println(
                                "Fixer: auto campaign adopting completed colony probe id="
                                        + completedId
                                        + " result="
                                        + completedResult);
                    }
                    return completedResult;
                }
            }
            autoCampaignColonyProbeCompleted = false;
            autoCampaignColonyProbeResult = null;
            autoCampaignColonyProbeError = null;
            autoCampaignColonyProbeStartedAt = System.currentTimeMillis();
            workerId = ++autoCampaignColonyProbeWorkerId;
            autoCampaignColonyProbeActiveId = workerId;
            autoCampaignColonyProbeWorker =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        autoCampaignColonyProbeResult =
                                                tryPrimeColonyInteractionTargetImmediate();
                                    } catch (Throwable t) {
                                        autoCampaignColonyProbeError = t;
                                    } finally {
                                        autoCampaignColonyProbeCompleted = true;
                                    }
                                }
                            },
                            "fixer-colony-probe-" + workerId);
            autoCampaignColonyProbeWorker.setDaemon(true);
            System.out.println(
                    "Fixer: auto campaign starting guarded colony probe worker id="
                            + workerId
                            + " timeoutMs="
                            + effectiveTimeoutMs
                            + ".");
            autoCampaignColonyProbeWorker.start();
            workerToJoin = autoCampaignColonyProbeWorker;
        }
        try {
            workerToJoin.join(effectiveTimeoutMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "pending:colony-probe-interrupted";
        }
        synchronized (AUTO_CAMPAIGN_COLONY_PROBE_LOCK) {
            if (!autoCampaignColonyProbeCompleted) {
                boolean disableDialogAfterTimeout =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignDisableDialogAfterProbeTimeout", "true"));
                if (disableDialogAfterTimeout && !autoCampaignColonyDialogSafeMode) {
                    autoCampaignColonyDialogSafeMode = true;
                    System.out.println(
                            "Fixer: auto campaign enabling colony dialog safe mode after probe timeout.");
                }
                System.out.println(
                        "Fixer: auto campaign colony probe timed out id="
                                + workerId
                                + " timeoutMs="
                                + effectiveTimeoutMs
                                + ".");
                boolean interruptWorkerOnTimeout =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignInterruptColonyProbeOnTimeout",
                                        "false"));
                if (interruptWorkerOnTimeout) {
                    try {
                        if (autoCampaignColonyProbeWorker != null) {
                            autoCampaignColonyProbeWorker.interrupt();
                        }
                    } catch (Throwable ignored) {
                    }
                }
                return "pending:colony-probe-timeout(" + effectiveTimeoutMs + "ms)";
            }
            if (autoCampaignColonyProbeError != null) {
                return "pending:colony-probe-error:" + describeThrowableChain(autoCampaignColonyProbeError);
            }
            return autoCampaignColonyProbeResult;
        }
    }

    private static void ensureCampaignStateResources() {
        try {
            Object sector = readGlobalSectorForSectorGen();
            if (sector == null) {
                System.out.println(
                        "Fixer: auto campaign campaign-state ensure skipped: sector-null.");
                return;
            }

            Method getEconomy = findMethodRecursive(sector.getClass(), "getEconomy");
            if (getEconomy == null) {
                System.out.println(
                        "Fixer: auto campaign campaign-state ensure skipped: Sector.getEconomy missing.");
                return;
            }
            getEconomy.setAccessible(true);
            Object economy = getEconomy.invoke(sector);
            if (economy == null) {
                System.out.println(
                        "Fixer: auto campaign campaign-state ensure skipped: sector economy null.");
                return;
            }

            List markets = new ArrayList();
            Method getMarketsCopy = findMethodRecursive(economy.getClass(), "getMarketsCopy");
            if (getMarketsCopy != null) {
                getMarketsCopy.setAccessible(true);
                List currentMarkets = coerceToList(getMarketsCopy.invoke(economy));
                if (currentMarkets != null && !currentMarkets.isEmpty()) {
                    markets.addAll(currentMarkets);
                }
            }
            if (markets.isEmpty()) {
                List fallbackMarkets = collectMarketsFromSectorEntities(sector);
                if (!fallbackMarkets.isEmpty()) {
                    markets.addAll(fallbackMarkets);
                    System.out.println(
                            "Fixer: auto campaign campaign-state ensure recovered markets from sector entities count="
                                    + markets.size());
                }
            }
            if (markets.isEmpty()) {
                boolean allowSectorGenBootstrap =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignEnsureBootstrapSectorGen",
                                        "false"));
                if (allowSectorGenBootstrap) {
                    boolean bootstrapped = tryBootstrapSectorGenForNoMarkets(sector, economy, markets);
                    if (bootstrapped && getMarketsCopy != null) {
                        List refreshedMarkets = coerceToList(getMarketsCopy.invoke(economy));
                        if (refreshedMarkets != null && !refreshedMarkets.isEmpty()) {
                            markets.addAll(refreshedMarkets);
                        }
                        if (markets.isEmpty()) {
                            List fallbackMarkets = collectMarketsFromSectorEntities(sector);
                            if (!fallbackMarkets.isEmpty()) {
                                markets.addAll(fallbackMarkets);
                            }
                        }
                    }
                }
            }
            if (markets.isEmpty()) {
                Object fallbackMarket = tryCreateMinimalFallbackMarketInHyperspace(sector, economy);
                if (fallbackMarket != null) {
                    markets.add(fallbackMarket);
                    System.out.println(
                            "Fixer: auto campaign campaign-state ensure seeded fallback market entity="
                                    + describeEntityName(extractPrimaryEntityFromMarket(fallbackMarket)));
                }
            }

            Object playerFleet = null;
            Method getPlayerFleet = findMethodRecursive(sector.getClass(), "getPlayerFleet");
            if (getPlayerFleet != null) {
                getPlayerFleet.setAccessible(true);
                playerFleet = getPlayerFleet.invoke(sector);
            }
            if (playerFleet == null) {
                playerFleet = tryRecoverPlayerFleetFromSectorEntities(sector);
            }
            if (playerFleet == null) {
                playerFleet = tryRecoverPlayerFleetFromCampaignEngine(sector);
            }
            if (playerFleet == null) {
                boolean enableSyntheticFleetFallback =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignEnableSyntheticPlayerFleetFallback",
                                        "true"));
                if (enableSyntheticFleetFallback) {
                    Object preferredEntity = resolvePreferredCampaignTargetEntity(markets, sector);
                    playerFleet = tryCreateSyntheticPlayerFleetFastPath(sector, preferredEntity);
                    long syntheticTimeoutMs =
                            Math.max(
                                    3000L,
                                    Math.min(
                                            10000L,
                                            parseLongProperty(
                                                    "starsector.autoCampaignSyntheticFleetTimeoutMs",
                                                    12000L)));
                    if (playerFleet == null) {
                        playerFleet =
                                tryCreateSyntheticPlayerFleetWithTimeout(
                                        sector,
                                        preferredEntity,
                                        syntheticTimeoutMs,
                                        "campaign-state-ensure");
                    }
                }
            }
            if (playerFleet != null) {
                assignRecoveredPlayerFleetToSector(sector, playerFleet);
                assignRecoveredPlayerFleetToCampaignEngine(playerFleet);
            }

            int economyMarketsAfter = countEconomyMarketsSafe(economy);
            System.out.println(
                    "Fixer: auto campaign campaign-state ensure result economyMarkets="
                            + economyMarketsAfter
                            + " marketCandidates="
                            + markets.size()
                            + " playerFleet="
                            + (playerFleet != null));
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign campaign-state ensure exception: "
                            + describeThrowableChain(t));
        }
    }

    private static Object resolvePreferredCampaignTargetEntity(List markets, Object sector) {
        if (markets != null) {
            for (Object market : markets) {
                Object entity = extractPrimaryEntityFromMarket(market);
                if (entity != null) {
                    return entity;
                }
            }
        }
        return findPreferredColonyEntity(sector, "");
    }

    private static Object tryCreateSyntheticPlayerFleetWithTimeout(
            final Object sector,
            final Object preferredEntity,
            long timeoutMs,
            String reason) {
        final long effectiveTimeoutMs = Math.max(3000L, timeoutMs);
        final Object[] fleetHolder = new Object[1];
        final Throwable[] errorHolder = new Throwable[1];
        final boolean[] done = new boolean[] {false};
        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    fleetHolder[0] =
                                            tryCreateSyntheticPlayerFleetForInteraction(
                                                    sector, preferredEntity);
                                } catch (Throwable t) {
                                    errorHolder[0] = t;
                                } finally {
                                    done[0] = true;
                                }
                            }
                        },
                        "fixer-synthetic-fleet-" + String.valueOf(reason));
        worker.setDaemon(true);
        worker.start();
        try {
            worker.join(effectiveTimeoutMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        }
        if (!done[0]) {
            System.out.println(
                    "Fixer: synthetic fleet fallback timed out in "
                            + String.valueOf(reason)
                            + " after "
                            + effectiveTimeoutMs
                            + "ms.");
            try {
                worker.interrupt();
            } catch (Throwable ignored) {
            }
            return null;
        }
        if (errorHolder[0] != null) {
            System.out.println(
                    "Fixer: synthetic fleet fallback error in "
                            + String.valueOf(reason)
                            + ": "
                            + describeThrowableChain(errorHolder[0]));
            return null;
        }
        return fleetHolder[0];
    }

    private static String tryPrimeColonyInteractionTargetImmediate() {
        try {
            System.out.println("Fixer: colony probe step=begin");
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSector = globalClass.getMethod("getSector");
            Object sector = getSector.invoke(null);
            if (sector == null) {
                return "pending:sector-null";
            }
            System.out.println("Fixer: colony probe step=sector-ready");

            Method getEconomy = findMethodRecursive(sector.getClass(), "getEconomy");
            if (getEconomy == null) {
                return "Sector.getEconomy method not found";
            }
            getEconomy.setAccessible(true);
            System.out.println("Fixer: colony probe step=before-getEconomy");
            Object economy = getEconomy.invoke(sector);
            if (economy == null) {
                return "pending:sector-economy-null";
            }
            System.out.println("Fixer: colony probe step=economy-ready");

            Method getMarketsCopy = findMethodRecursive(economy.getClass(), "getMarketsCopy");
            if (getMarketsCopy == null) {
                return "Economy.getMarketsCopy method not found";
            }
            getMarketsCopy.setAccessible(true);
            System.out.println("Fixer: colony probe step=before-getMarketsCopy");
            Object marketsObj = getMarketsCopy.invoke(economy);
            List markets = coerceToList(marketsObj);
            if (markets == null) {
                return "pending:markets-list-unavailable";
            }
            System.out.println(
                    "Fixer: colony probe step=markets-ready size="
                            + markets.size());
            if (markets.isEmpty()) {
                List fallbackMarkets = collectMarketsFromSectorEntities(sector);
                if (!fallbackMarkets.isEmpty()) {
                    markets = fallbackMarkets;
                    System.out.println(
                            "Fixer: auto campaign colony-target using sector-entity fallback markets count="
                                    + markets.size());
                } else {
                    markets = new ArrayList();
                }
            }
            if (markets.isEmpty()) {
                boolean allowSectorGenBootstrapInColonyProbe =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignColonyProbeBootstrapSectorGen",
                                        "false"));
                if (allowSectorGenBootstrapInColonyProbe) {
                    boolean bootstrapped = tryBootstrapSectorGenForNoMarkets(sector, economy, markets);
                    if (bootstrapped) {
                        Object refreshedMarketsObj = getMarketsCopy.invoke(economy);
                        List refreshedMarkets = coerceToList(refreshedMarketsObj);
                        if (refreshedMarkets != null && !refreshedMarkets.isEmpty()) {
                            markets = refreshedMarkets;
                            System.out.println(
                                    "Fixer: auto campaign colony-target using SectorGen bootstrap markets count="
                                            + markets.size());
                        } else {
                            List fallbackMarkets = collectMarketsFromSectorEntities(sector);
                            if (!fallbackMarkets.isEmpty()) {
                                markets = fallbackMarkets;
                                System.out.println(
                                        "Fixer: auto campaign colony-target using SectorGen entity fallback markets count="
                                                + markets.size());
                            }
                        }
                    }
                } else {
                    System.out.println(
                            "Fixer: auto campaign skipping SectorGen bootstrap during colony probe (set -Dstarsector.autoCampaignColonyProbeBootstrapSectorGen=true to enable).");
                }
            }
            if (markets.isEmpty()) {
                Object fallbackMarket = tryCreateMinimalFallbackMarketInHyperspace(sector, economy);
                if (fallbackMarket != null) {
                    markets.add(fallbackMarket);
                    System.out.println(
                            "Fixer: auto campaign colony-target seeded minimal fallback market entity="
                                    + describeEntityName(extractPrimaryEntityFromMarket(fallbackMarket)));
                }
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
                Object fallbackEntity = findPreferredColonyEntity(sector, preferredName);
                if (fallbackEntity != null) {
                    selectedEntity = fallbackEntity;
                    selectedMarket = null;
                    System.out.println(
                            "Fixer: auto campaign colony target using entity-only fallback="
                                    + describeEntityName(fallbackEntity));
                } else {
                    maybeLogNoMarketsDiagnostics(sector, economy, markets);
                    return "pending:no-markets-available";
                }
            }
            if (selectedMarket == null) {
                maybeLogNoMarketsDiagnostics(sector, economy, markets);
                return "pending:no-markets-available";
            }

            Object campaignUI = null;
            Method getCampaignUI = findMethodRecursive(sector.getClass(), "getCampaignUI");
            if (getCampaignUI != null) {
                getCampaignUI.setAccessible(true);
                campaignUI = getCampaignUI.invoke(sector);
            }
            if (campaignUI == null) {
                return "pending:campaign-ui-null";
            }
            Object campaignUIEngine = readFieldRecursive(campaignUI, "engine");
            if (campaignUIEngine == null) {
                campaignUIEngine = invokeNoArgIfPresent(campaignUI, "getEngine");
            }
            if (campaignUIEngine == null) {
                try {
                    Class<?> campaignEngineClass =
                            Class.forName("com.fs.starfarer.campaign.CampaignEngine");
                    Method getInstance = findMethodRecursive(campaignEngineClass, "getInstance");
                    if (getInstance != null) {
                        getInstance.setAccessible(true);
                        Object engineInstance = getInstance.invoke(null);
                        if (engineInstance != null) {
                            Field engineField = findFieldRecursive(campaignUI.getClass(), "engine");
                            if (engineField != null) {
                                engineField.setAccessible(true);
                                engineField.set(campaignUI, engineInstance);
                            } else {
                                Method setEngine =
                                        findSingleArgCompatibleMethod(
                                                campaignUI.getClass(),
                                                "setEngine",
                                                engineInstance.getClass());
                                if (setEngine == null) {
                                    setEngine = findSingleArgMethod(campaignUI.getClass(), "setEngine");
                                }
                                if (setEngine != null) {
                                    setEngine.setAccessible(true);
                                    setEngine.invoke(campaignUI, engineInstance);
                                }
                            }
                            campaignUIEngine = readFieldRecursive(campaignUI, "engine");
                            if (campaignUIEngine == null) {
                                campaignUIEngine = invokeNoArgIfPresent(campaignUI, "getEngine");
                            }
                            if (campaignUIEngine != null) {
                                System.out.println(
                                        "Fixer: auto campaign recovered campaign UI engine from CampaignEngine.getInstance().");
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (campaignUIEngine == null) {
                try {
                    Class<?> campaignEngineClass =
                            Class.forName("com.fs.starfarer.campaign.CampaignEngine");
                    if (campaignEngineClass.isInstance(sector)) {
                        Method setInstance =
                                findSingleArgCompatibleMethod(
                                        campaignEngineClass, "setInstance", sector.getClass());
                        if (setInstance == null) {
                            setInstance = findSingleArgMethod(campaignEngineClass, "setInstance");
                        }
                        if (setInstance != null) {
                            setInstance.setAccessible(true);
                            setInstance.invoke(null, sector);
                        }

                        Method setCampaignUI =
                                findSingleArgCompatibleMethod(
                                        campaignEngineClass, "setCampaignUI", campaignUI.getClass());
                        if (setCampaignUI == null) {
                            setCampaignUI = findSingleArgMethod(campaignEngineClass, "setCampaignUI");
                        }
                        if (setCampaignUI != null) {
                            setCampaignUI.setAccessible(true);
                            setCampaignUI.invoke(sector, campaignUI);
                        }

                        if (assignCampaignUiEngine(campaignUI, sector)) {
                            campaignUIEngine = readFieldRecursive(campaignUI, "engine");
                            if (campaignUIEngine == null) {
                                campaignUIEngine = invokeNoArgIfPresent(campaignUI, "getEngine");
                            }
                            if (campaignUIEngine != null) {
                                System.out.println(
                                        "Fixer: auto campaign recovered campaign UI engine from live sector CampaignEngine.");
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (campaignUIEngine == null) {
                boolean allowNullCampaignUiEngine =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignAllowNullCampaignUiEngine",
                                        "true"));
                if (!allowNullCampaignUiEngine) {
                    return "pending:campaign-ui-engine-null";
                }
                System.out.println(
                        "Fixer: auto campaign continuing with null campaign UI engine (set -Dstarsector.autoCampaignAllowNullCampaignUiEngine=false to enforce).");
            }

            if (!canPickInteractionDialogPlugin(campaignUI, selectedEntity)) {
                Object pluginMarket = null;
                Object pluginEntity = null;
                for (Object market : markets) {
                    if (market == null || market == selectedMarket) {
                        continue;
                    }
                    Object candidateEntity = extractPrimaryEntityFromMarket(market);
                    if (candidateEntity == null) {
                        continue;
                    }
                    if (canPickInteractionDialogPlugin(campaignUI, candidateEntity)) {
                        pluginMarket = market;
                        pluginEntity = candidateEntity;
                        break;
                    }
                }
                if (pluginMarket != null && pluginEntity != null) {
                    selectedMarket = pluginMarket;
                    selectedEntity = pluginEntity;
                    System.out.println(
                            "Fixer: auto campaign switched colony target to plugin-capable entity="
                                    + describeEntityName(selectedEntity));
                }
            }

            Method getPlayerFleet = findMethodRecursive(sector.getClass(), "getPlayerFleet");
            if (getPlayerFleet == null) {
                return "Sector.getPlayerFleet method not found";
            }
            getPlayerFleet.setAccessible(true);
            Object playerFleet = getPlayerFleet.invoke(sector);
            if (playerFleet == null) {
                Object recoveredFleet = tryRecoverPlayerFleetFromSectorEntities(sector);
                if (recoveredFleet != null) {
                    playerFleet = recoveredFleet;
                    System.out.println(
                            "Fixer: auto campaign recovered player fleet via sector-entity fallback.");
                }
            }
            if (playerFleet == null) {
                boolean enableSyntheticFleetFallback =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignEnableSyntheticPlayerFleetFallback",
                                        "true"));
                if (enableSyntheticFleetFallback) {
                    System.out.println(
                            "Fixer: auto campaign player fleet missing; attempting synthetic fleet fallback.");
                    Object fastSynthFleet =
                            tryCreateSyntheticPlayerFleetFastPath(sector, selectedEntity);
                    if (fastSynthFleet != null) {
                        playerFleet = fastSynthFleet;
                    }
                    final Object syntheticSector = sector;
                    final Object syntheticTargetEntity = selectedEntity;
                    final long syntheticTimeoutMs =
                            Math.max(
                                    3000L,
                                    parseLongProperty(
                                            "starsector.autoCampaignSyntheticFleetTimeoutMs",
                                            12000L));
                    final Object[] syntheticFleetHolder = new Object[1];
                    final Throwable[] syntheticFleetError = new Throwable[1];
                    final boolean[] syntheticFleetDone = new boolean[] {false};
                    if (playerFleet == null) {
                        Thread syntheticFleetWorker =
                                new Thread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    syntheticFleetHolder[0] =
                                                            tryCreateSyntheticPlayerFleetForInteraction(
                                                                    syntheticSector,
                                                                    syntheticTargetEntity);
                                                } catch (Throwable t) {
                                                    syntheticFleetError[0] = t;
                                                } finally {
                                                    syntheticFleetDone[0] = true;
                                                }
                                            }
                                        },
                                        "fixer-synthetic-fleet-fallback");
                        syntheticFleetWorker.setDaemon(true);
                        syntheticFleetWorker.start();
                        try {
                            syntheticFleetWorker.join(syntheticTimeoutMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            System.out.println(
                                    "Fixer: synthetic fleet fallback interrupted while waiting for worker.");
                        }
                        if (!syntheticFleetDone[0]) {
                            System.out.println(
                                    "Fixer: synthetic fleet fallback timed out after "
                                            + syntheticTimeoutMs
                                            + "ms; continuing with dialog fallback.");
                            try {
                                syntheticFleetWorker.interrupt();
                            } catch (Throwable ignored) {
                            }
                        } else if (syntheticFleetError[0] != null) {
                            System.out.println(
                                    "Fixer: synthetic fleet fallback worker error: "
                                            + describeThrowableChain(syntheticFleetError[0]));
                        }
                        Object synthesizedFleet = syntheticFleetHolder[0];
                        System.out.println(
                                "Fixer: auto campaign synthetic fleet fallback result="
                                        + (synthesizedFleet == null
                                                ? "null"
                                                : synthesizedFleet.getClass().getName()));
                        if (synthesizedFleet != null) {
                            playerFleet = synthesizedFleet;
                            System.out.println(
                                    "Fixer: auto campaign synthesized player fleet for interaction fallback.");
                        }
                    }
                } else {
                    System.out.println(
                            "Fixer: auto campaign synthetic fleet fallback disabled (set -Dstarsector.autoCampaignEnableSyntheticPlayerFleetFallback=true to enable).");
                }
            }
            if (playerFleet == null) {
                boolean allowDialogWithoutPlayerFleet =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignAllowDialogWithoutPlayerFleet",
                                        "false"));
                if (!allowDialogWithoutPlayerFleet) {
                    return "pending:player-fleet-null";
                }
                if (autoCampaignColonyDialogSafeMode) {
                    System.out.println(
                            "Fixer: auto campaign colony dialog safe mode active without player fleet; accepting target without forcing dialog.");
                    return null;
                }
                if (requestCampaignInteractionDialog(
                        campaignUI, selectedEntity, "without-player-fleet")) {
                    System.out.println(
                            "Fixer: auto campaign colony target primed via dialog-only fallback entity="
                                    + describeEntityName(selectedEntity));
                    return null;
                }
                Object dialogFallbackEntity =
                        findDialogCapableEntityFromSector(
                                sector, campaignUI, preferredName, selectedEntity);
                if (dialogFallbackEntity != null
                        && requestCampaignInteractionDialog(
                                campaignUI,
                                dialogFallbackEntity,
                                "without-player-fleet")) {
                    System.out.println(
                            "Fixer: auto campaign colony target primed via dialog-only sector fallback entity="
                                    + describeEntityName(dialogFallbackEntity));
                    return null;
                }
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

            if (autoCampaignColonyDialogSafeMode) {
                System.out.println(
                        "Fixer: auto campaign colony dialog safe mode active; leaving interaction target set without forcing dialog.");
                return null;
            }

            boolean dialogShown =
                    requestCampaignInteractionDialog(campaignUI, selectedEntity, "with-player-fleet");
            if (!dialogShown && selectedMarket != null && selectedMarket != selectedEntity) {
                dialogShown =
                        requestCampaignInteractionDialog(
                                campaignUI, selectedMarket, "market-object-fallback");
            }
            if (!dialogShown) {
                dialogShown =
                        requestCampaignInteractionDialog(
                                campaignUI, selectedEntity, "without-player-fleet");
            }
            if (!dialogShown) {
                return "pending:dialog-not-shown";
            }

            String marketName = describeEntityName(selectedEntity);
            if (selectedMarket != null) {
                Method getName = findMethodRecursive(selectedMarket.getClass(), "getName");
                if (getName != null) {
                    getName.setAccessible(true);
                    Object nameObj = getName.invoke(selectedMarket);
                    if (nameObj != null) {
                        marketName = String.valueOf(nameObj);
                    }
                }
            }

            System.out.println("Fixer: auto campaign colony target primed market=" + marketName);
            return null;
        } catch (Throwable t) {
            return describeThrowableChain(t);
        }
    }

    private static boolean hasEntityMarket(Object entity) {
        if (entity == null) {
            return false;
        }
        try {
            Method getMarket = findMethodRecursive(entity.getClass(), "getMarket");
            if (getMarket == null) {
                return false;
            }
            getMarket.setAccessible(true);
            return getMarket.invoke(entity) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object extractPrimaryEntityFromMarket(Object market) {
        if (market == null) {
            return null;
        }
        try {
            Method getPrimaryEntity = findMethodRecursive(market.getClass(), "getPrimaryEntity");
            if (getPrimaryEntity == null) {
                return null;
            }
            getPrimaryEntity.setAccessible(true);
            return getPrimaryEntity.invoke(market);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void maybePrepareFallbackMarketSpecs() {
        if (fallbackMarketSpecPreflightAttempted) {
            return;
        }
        fallbackMarketSpecPreflightAttempted = true;
        List<String> issues = new ArrayList<String>();
        try {
            String runtimeFactionIssue =
                    ensureRuntimeFactionsPresentForDirectNewGame(
                            new String[] {"neutral", "player", "independent", "pirates"}, true);
            if (runtimeFactionIssue != null && !runtimeFactionIssue.trim().isEmpty()) {
                issues.add("runtime-factions=" + runtimeFactionIssue.trim());
            }
        } catch (Throwable t) {
            issues.add("runtime-factions-exception=" + describeThrowableChain(t));
        }
        if (!allowMutatingSpecPreflight()) {
            fallbackMarketSpecPreflightSignature =
                    issues.isEmpty()
                            ? "non-mutating-runtime-factions-only"
                            : "non-mutating-runtime-factions-only; " + String.join("; ", issues);
            System.out.println(
                    "Fixer: fallback market spec preflight "
                            + fallbackMarketSpecPreflightSignature);
            return;
        }
        try {
            String marketConditionIssue = ensureMarketConditionSpecsReadyForDirectNewGame();
            if (marketConditionIssue != null && !marketConditionIssue.trim().isEmpty()) {
                issues.add("market-conditions=" + marketConditionIssue.trim());
            }
        } catch (Throwable t) {
            issues.add("market-conditions-exception=" + describeThrowableChain(t));
        }
        try {
            String submarketIssue = ensureSubmarketSpecsReadyForDirectNewGame();
            if (submarketIssue != null && !submarketIssue.trim().isEmpty()) {
                issues.add("submarkets=" + submarketIssue.trim());
            }
        } catch (Throwable t) {
            issues.add("submarkets-exception=" + describeThrowableChain(t));
        }
        fallbackMarketSpecPreflightSignature =
                issues.isEmpty() ? "ok" : String.join("; ", issues);
        if ("ok".equals(fallbackMarketSpecPreflightSignature)) {
            System.out.println("Fixer: fallback market spec preflight ready.");
        } else {
            System.out.println(
                    "Fixer: fallback market spec preflight issues: "
                            + fallbackMarketSpecPreflightSignature);
        }
    }

    private static void applyFallbackMarketFaction(Object sector, Object market, String factionId) {
        if (market == null) {
            return;
        }
        String id = factionId == null || factionId.trim().isEmpty() ? "neutral" : factionId.trim();
        try {
            Method setFactionId = findMethodRecursive(market.getClass(), "setFactionId", String.class);
            if (setFactionId != null) {
                setFactionId.setAccessible(true);
                setFactionId.invoke(market, id);
            }
        } catch (Throwable t) {
            maybeLogFallbackMarketIssue("market-setFactionId(" + id + ") failed: " + describeThrowableChain(t), t);
        }
        if (sector == null) {
            return;
        }
        try {
            Method getFaction = findMethodRecursive(sector.getClass(), "getFaction", String.class);
            if (getFaction == null) {
                return;
            }
            getFaction.setAccessible(true);
            Object faction = getFaction.invoke(sector, id);
            if (faction == null) {
                return;
            }
            Method setFaction =
                    findSingleArgCompatibleMethod(market.getClass(), "setFaction", faction.getClass());
            if (setFaction == null) {
                setFaction = findSingleArgMethod(market.getClass(), "setFaction");
            }
            if (setFaction != null) {
                setFaction.setAccessible(true);
                setFaction.invoke(market, faction);
            }
        } catch (Throwable t) {
            maybeLogFallbackMarketIssue(
                    "market-setFaction-object(" + id + ") failed: " + describeThrowableChain(t), t);
        }
    }

    private static Object tryCreateMinimalFallbackMarketInHyperspace(Object sector, Object economy) {
        boolean enabled =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignCreateMinimalFallbackMarketOnNoMarkets",
                                "true"));
        if (!enabled || sector == null || economy == null) {
            return null;
        }
        try {
            Object hyperspace = invokeNoArgIfPresent(sector, "getHyperspace");
            if (hyperspace == null) {
                maybeLogFallbackMarketIssue("hyperspace-null", null);
                return null;
            }

            long marker = System.currentTimeMillis() % 1000000L;
            String entityId = "fixer_autocampaign_fallback_" + marker;
            String marketId = entityId + "_market";
            String entityName = "Fallback Colony";
            Object entity = createFallbackMarketEntity(hyperspace, entityId, entityName);
            if (entity == null) {
                maybeLogFallbackMarketIssue("entity-create-null", null);
                return null;
            }

            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getFactory = findMethodRecursive(globalClass, "getFactory");
            if (getFactory == null) {
                maybeLogFallbackMarketIssue("global-factory-method-missing", null);
                return null;
            }
            getFactory.setAccessible(true);
            Object factory = getFactory.invoke(null);
            if (factory == null) {
                maybeLogFallbackMarketIssue("global-factory-null", null);
                return null;
            }
            Method createMarket =
                    findMethodRecursive(
                            factory.getClass(), "createMarket", String.class, String.class, Integer.TYPE);
            if (createMarket == null) {
                createMarket =
                        findMethodRecursive(
                                factory.getClass(),
                                "createMarket",
                                String.class,
                                String.class,
                                Integer.class);
            }
            if (createMarket == null) {
                maybeLogFallbackMarketIssue("createMarket-method-missing", null);
                return null;
            }
            createMarket.setAccessible(true);
            Object market =
                    createMarket.getParameterTypes()[2] == Integer.TYPE
                            ? createMarket.invoke(factory, marketId, entityName, Integer.valueOf(3).intValue())
                            : createMarket.invoke(factory, marketId, entityName, Integer.valueOf(3));
            if (market == null) {
                maybeLogFallbackMarketIssue("createMarket-returned-null", null);
                return null;
            }

            maybePrepareFallbackMarketSpecs();
            applyFallbackMarketFaction(sector, market, "neutral");
            Method setName = findMethodRecursive(market.getClass(), "setName", String.class);
            if (setName != null) {
                try {
                    setName.setAccessible(true);
                    setName.invoke(market, entityName);
                } catch (Throwable ignored) {
                }
            }
            Method setPrimaryEntity =
                    findSingleArgCompatibleMethod(
                            market.getClass(), "setPrimaryEntity", entity.getClass());
            if (setPrimaryEntity != null) {
                setPrimaryEntity.setAccessible(true);
                setPrimaryEntity.invoke(market, entity);
            }
            Method setMarket =
                    findSingleArgCompatibleMethod(entity.getClass(), "setMarket", market.getClass());
            if (setMarket != null) {
                setMarket.setAccessible(true);
                setMarket.invoke(entity, market);
            }

            Method addCondition = findMethodRecursive(market.getClass(), "addCondition", String.class);
            if (addCondition != null) {
                try {
                    addCondition.setAccessible(true);
                    addCondition.invoke(market, "population_3");
                } catch (Throwable t) {
                    maybeLogFallbackMarketIssue(
                            "market-addCondition(population_3) failed: "
                                    + describeThrowableChain(t),
                            t);
                }
            }
            Method addSubmarket = findMethodRecursive(market.getClass(), "addSubmarket", String.class);
            if (addSubmarket != null) {
                try {
                    addSubmarket.setAccessible(true);
                    addSubmarket.invoke(market, "open_market");
                } catch (Throwable t) {
                    maybeLogFallbackMarketIssue(
                            "market-addSubmarket(open_market) failed: "
                                    + describeThrowableChain(t),
                            t);
                }
            }

            boolean marketRegistered = false;
            Method addMarket =
                    findMethodRecursive(economy.getClass(), "addMarket", market.getClass(), Boolean.TYPE);
            if (addMarket != null) {
                addMarket.setAccessible(true);
                addMarket.invoke(economy, market, Boolean.TRUE);
                marketRegistered = true;
            } else {
                Method addMarketSingle =
                        findSingleArgCompatibleMethod(economy.getClass(), "addMarket", market.getClass());
                if (addMarketSingle != null) {
                    addMarketSingle.setAccessible(true);
                    addMarketSingle.invoke(economy, market);
                    marketRegistered = true;
                }
            }
            if (!marketRegistered) {
                try {
                    Method getMarketsCopy = findMethodRecursive(economy.getClass(), "getMarketsCopy");
                    if (getMarketsCopy != null) {
                        getMarketsCopy.setAccessible(true);
                        Object marketsObj = getMarketsCopy.invoke(economy);
                        if (marketsObj instanceof Collection) {
                            Collection marketsCollection = (Collection) marketsObj;
                            marketsCollection.add(market);
                            marketRegistered = true;
                            maybeLogFallbackMarketIssue(
                                    "economy-addMarket-fallback: collection-add succeeded",
                                    null);
                        }
                    }
                } catch (Throwable t) {
                    maybeLogFallbackMarketIssue(
                            "economy-addMarket-fallback failed: " + describeThrowableChain(t), t);
                }
            }
            if (!marketRegistered) {
                maybeLogFallbackMarketIssue("economy-addMarket-method-missing", null);
                return null;
            }

            System.out.println(
                    "Fixer: auto campaign created minimal fallback market id="
                            + marketId
                            + " entity="
                            + describeEntityName(entity));
            return market;
        } catch (Throwable t) {
            maybeLogFallbackMarketIssue("minimal-fallback-market exception: " + describeThrowableChain(t), t);
            return null;
        }
    }

    private static Object createFallbackMarketEntity(
            Object hyperspace, String entityId, String entityName) {
        if (hyperspace == null) {
            return null;
        }

        // Prefer token creation because it avoids custom-entity spec dependencies.
        try {
            Method createToken =
                    findMethodRecursive(hyperspace.getClass(), "createToken", Float.TYPE, Float.TYPE);
            if (createToken != null) {
                createToken.setAccessible(true);
                Object token =
                        createToken.invoke(
                                hyperspace, Float.valueOf(0f).floatValue(), Float.valueOf(0f).floatValue());
                if (token != null) {
                    initializeFallbackEntityToken(hyperspace, token, entityId, entityName);
                    return token;
                }
            }
        } catch (Throwable t) {
            maybeLogFallbackMarketIssue("createToken(float,float) failed: " + describeThrowableChain(t), t);
        }
        try {
            Method createTokenBoxed =
                    findMethodRecursive(hyperspace.getClass(), "createToken", Float.class, Float.class);
            if (createTokenBoxed != null) {
                createTokenBoxed.setAccessible(true);
                Object token = createTokenBoxed.invoke(hyperspace, Float.valueOf(0f), Float.valueOf(0f));
                if (token != null) {
                    initializeFallbackEntityToken(hyperspace, token, entityId, entityName);
                    return token;
                }
            }
        } catch (Throwable t) {
            maybeLogFallbackMarketIssue("createToken(Float,Float) failed: " + describeThrowableChain(t), t);
        }

        try {
            Method addCustomEntity =
                    findMethodRecursive(
                            hyperspace.getClass(),
                            "addCustomEntity",
                            String.class,
                            String.class,
                            String.class,
                            String.class);
            if (addCustomEntity != null) {
                addCustomEntity.setAccessible(true);
                Object entity = addCustomEntity.invoke(hyperspace, entityId, entityName, "comm_relay", "neutral");
                if (entity != null) {
                    initializeFallbackEntityToken(hyperspace, entity, entityId, entityName);
                    return entity;
                }
            }
        } catch (Throwable t) {
            maybeLogFallbackMarketIssue("addCustomEntity fallback failed: " + describeThrowableChain(t), t);
        }
        return null;
    }

    private static void addFactionConfigPathCandidatesForRepair(
            Set<String> out, String factionIdOrAlias) {
        if (out == null || factionIdOrAlias == null) {
            return;
        }
        String clean = factionIdOrAlias.trim().toLowerCase();
        if (clean.isEmpty()) {
            return;
        }
        out.add("data/world/factions/" + clean + ".faction");
        if ("persean".equals(clean) || "persean_league".equals(clean)) {
            out.add("data/world/factions/persean_league.faction");
        }
        if ("remnant".equals(clean) || "remnants".equals(clean)) {
            out.add("data/world/factions/remnants.faction");
        }
        if ("diktat".equals(clean) || "sindrian_diktat".equals(clean)) {
            out.add("data/world/factions/sindrian_diktat.faction");
        }
    }

    private static Set<String> resolveFactionConfigSuffixCandidatesForRepair(String factionIdOrAlias) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        if (factionIdOrAlias == null) {
            return out;
        }
        String clean = factionIdOrAlias.trim().toLowerCase();
        if (clean.isEmpty()) {
            return out;
        }
        out.add(clean + ".faction");
        if ("persean".equals(clean) || "persean_league".equals(clean)) {
            out.add("persean_league.faction");
        }
        if ("remnant".equals(clean) || "remnants".equals(clean)) {
            out.add("remnants.faction");
        }
        if ("diktat".equals(clean) || "sindrian_diktat".equals(clean)) {
            out.add("sindrian_diktat.faction");
        }
        return out;
    }

    private static void initializeFallbackEntityToken(
            Object location, Object entity, String entityId, String entityName) {
        if (entity == null) {
            return;
        }
        try {
            Method setId = findMethodRecursive(entity.getClass(), "setId", String.class);
            if (setId != null && entityId != null) {
                setId.setAccessible(true);
                setId.invoke(entity, entityId);
            }
        } catch (Throwable ignored) {
        }
        try {
            Method setName = findMethodRecursive(entity.getClass(), "setName", String.class);
            if (setName != null && entityName != null) {
                setName.setAccessible(true);
                setName.invoke(entity, entityName);
            }
        } catch (Throwable ignored) {
        }
        try {
            Method setFaction = findMethodRecursive(entity.getClass(), "setFaction", String.class);
            if (setFaction != null) {
                setFaction.setAccessible(true);
                setFaction.invoke(entity, "neutral");
            }
        } catch (Throwable ignored) {
        }
        try {
            Method setLocation =
                    findMethodRecursive(entity.getClass(), "setLocation", Float.TYPE, Float.TYPE);
            if (setLocation != null) {
                setLocation.setAccessible(true);
                setLocation.invoke(entity, Float.valueOf(0f).floatValue(), Float.valueOf(0f).floatValue());
            }
        } catch (Throwable ignored) {
        }
        try {
            Method addEntity =
                    findSingleArgCompatibleMethod(location.getClass(), "addEntity", entity.getClass());
            if (addEntity == null) {
                addEntity = findSingleArgMethod(location.getClass(), "addEntity");
            }
            if (addEntity != null) {
                addEntity.setAccessible(true);
                addEntity.invoke(location, entity);
            }
        } catch (Throwable ignored) {
        }
    }

    private static List collectMarketsFromSectorEntities(Object sector) {
        List markets = new ArrayList();
        Set seen = new LinkedHashSet();
        if (sector == null) {
            return markets;
        }
        collectMarketsFromLocationObject(sector, markets, seen);
        addMarketsFromLocationCollection(sector, "getStarSystems", markets, seen);
        addMarketsFromLocationCollection(sector, "getAllLocations", markets, seen);
        addMarketsFromLocationCollection(sector, "getLocations", markets, seen);
        addMarketsFromLocationObjectByMethod(sector, "getHyperspace", markets, seen);
        return markets;
    }

    private static boolean assignCampaignUiEngine(Object campaignUI, Object engine) {
        if (campaignUI == null || engine == null) {
            return false;
        }
        try {
            Field engineField = findFieldRecursive(campaignUI.getClass(), "engine");
            if (engineField != null) {
                engineField.setAccessible(true);
                engineField.set(campaignUI, engine);
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method setEngine =
                    findSingleArgCompatibleMethod(campaignUI.getClass(), "setEngine", engine.getClass());
            if (setEngine == null) {
                setEngine = findSingleArgMethod(campaignUI.getClass(), "setEngine");
            }
            if (setEngine != null) {
                setEngine.setAccessible(true);
                setEngine.invoke(campaignUI, engine);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean requestCampaignInteractionDialog(
            Object campaignUI, Object selectedEntity, String modeLabel) {
        if (campaignUI == null || selectedEntity == null) {
            return false;
        }
        if (autoCampaignColonyDialogSafeMode) {
            return false;
        }
        if ("without-player-fleet".equalsIgnoreCase(String.valueOf(modeLabel))) {
            if (requestCampaignInteractionDialogViaPluginFallback(campaignUI, selectedEntity)) {
                return true;
            }
            boolean allowDirectWithoutPlayerFleet =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignAllowDirectDialogWithoutPlayerFleet",
                                    "true"));
            if (!allowDirectWithoutPlayerFleet) {
                System.out.println(
                        "Fixer: auto campaign skipping direct showInteractionDialog(entity) without player fleet after plugin fallback miss.");
                return false;
            }
            System.out.println(
                    "Fixer: auto campaign plugin fallback miss; trying direct showInteractionDialog(entity) without player fleet.");
        }
        try {
            Method showInteractionDialog =
                    findSingleArgCompatibleMethod(
                            campaignUI.getClass(), "showInteractionDialog", selectedEntity.getClass());
            if (showInteractionDialog == null) {
                if (!"without-player-fleet".equalsIgnoreCase(String.valueOf(modeLabel))) {
                    return requestCampaignInteractionDialogViaPluginFallback(campaignUI, selectedEntity);
                }
                return false;
            }
            showInteractionDialog.setAccessible(true);
            Object shown = showInteractionDialog.invoke(campaignUI, selectedEntity);
            System.out.println(
                    "Fixer: auto campaign requested colony interaction dialog result="
                            + String.valueOf(shown)
                            + " mode="
                            + String.valueOf(modeLabel));
            boolean shownOk = !(shown instanceof Boolean) || ((Boolean) shown).booleanValue();
            if (!shownOk
                    && !"without-player-fleet".equalsIgnoreCase(String.valueOf(modeLabel))
                    && requestCampaignInteractionDialogViaPluginFallback(campaignUI, selectedEntity)) {
                return true;
            }
            if (!shownOk
                    && "with-player-fleet".equalsIgnoreCase(String.valueOf(modeLabel))) {
                return requestCampaignInteractionDialog(
                        campaignUI, selectedEntity, "without-player-fleet");
            }
            return shownOk;
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign showInteractionDialog request failed mode="
                            + String.valueOf(modeLabel)
                            + ": "
                            + describeThrowableChain(t));
            System.out.println(
                    "Fixer: auto campaign showInteractionDialog stack mode="
                            + String.valueOf(modeLabel)
                            + ": "
                            + stackTraceToString(t));
            return false;
        }
    }

    private static boolean requestCampaignInteractionDialogViaPluginFallback(
            Object campaignUI, Object selectedEntity) {
        if (campaignUI == null || selectedEntity == null) {
            return false;
        }
        try {
            Object engine = readFieldRecursive(campaignUI, "engine");
            if (engine == null) {
                engine = invokeNoArgIfPresent(campaignUI, "getEngine");
            }
            if (engine == null) {
                maybeLogDialogPluginFallbackIssue("engine-null");
                return false;
            }
            Object modAndPluginData = invokeNoArgIfPresent(engine, "getModAndPluginData");
            if (modAndPluginData == null) {
                maybeLogDialogPluginFallbackIssue("modAndPluginData-null");
                return false;
            }
            Method pickInteractionDialogPlugin =
                    findSingleArgCompatibleMethod(
                            modAndPluginData.getClass(),
                            "pickInteractionDialogPlugin",
                            selectedEntity.getClass());
            if (pickInteractionDialogPlugin == null) {
                pickInteractionDialogPlugin =
                        findSingleArgMethod(modAndPluginData.getClass(), "pickInteractionDialogPlugin");
            }
            if (pickInteractionDialogPlugin == null) {
                maybeLogDialogPluginFallbackIssue("pickInteractionDialogPlugin-missing");
                return false;
            }
            pickInteractionDialogPlugin.setAccessible(true);
            Object plugin = pickInteractionDialogPlugin.invoke(modAndPluginData, selectedEntity);
            if (plugin == null) {
                maybeLogDialogPluginFallbackIssue(
                        "pickInteractionDialogPlugin-null entity=" + describeEntityName(selectedEntity));
                return false;
            }
            Method showInteractionDialog =
                    findTwoArgCompatibleMethod(
                            campaignUI.getClass(),
                            "showInteractionDialog",
                            plugin.getClass(),
                            selectedEntity.getClass());
            if (showInteractionDialog == null) {
                maybeLogDialogPluginFallbackIssue(
                        "showInteractionDialog(plugin,entity)-missing plugin="
                                + plugin.getClass().getName());
                return false;
            }
            showInteractionDialog.setAccessible(true);
            Object shown = showInteractionDialog.invoke(campaignUI, plugin, selectedEntity);
            System.out.println(
                    "Fixer: auto campaign requested colony interaction dialog via plugin fallback result="
                            + String.valueOf(shown)
                            + " plugin="
                            + plugin.getClass().getName());
            maybeLogDialogPluginFallbackIssue("success");
            return !(shown instanceof Boolean) || ((Boolean) shown).booleanValue();
        } catch (Throwable t) {
            maybeLogDialogPluginFallbackIssue("exception:" + describeThrowableChain(t));
            System.out.println(
                    "Fixer: auto campaign plugin fallback dialog request failed: "
                            + describeThrowableChain(t));
            return false;
        }
    }

    private static boolean canPickInteractionDialogPlugin(Object campaignUI, Object selectedEntity) {
        if (campaignUI == null || selectedEntity == null) {
            return false;
        }
        try {
            Object engine = readFieldRecursive(campaignUI, "engine");
            if (engine == null) {
                engine = invokeNoArgIfPresent(campaignUI, "getEngine");
            }
            if (engine == null) {
                return false;
            }
            Object modAndPluginData = invokeNoArgIfPresent(engine, "getModAndPluginData");
            if (modAndPluginData == null) {
                return false;
            }
            Method pickInteractionDialogPlugin =
                    findSingleArgCompatibleMethod(
                            modAndPluginData.getClass(),
                            "pickInteractionDialogPlugin",
                            selectedEntity.getClass());
            if (pickInteractionDialogPlugin == null) {
                pickInteractionDialogPlugin =
                        findSingleArgMethod(modAndPluginData.getClass(), "pickInteractionDialogPlugin");
            }
            if (pickInteractionDialogPlugin == null) {
                return false;
            }
            pickInteractionDialogPlugin.setAccessible(true);
            return pickInteractionDialogPlugin.invoke(modAndPluginData, selectedEntity) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object findDialogCapableEntityFromSector(
            Object sector, Object campaignUI, String preferredName, Object currentEntity) {
        if (sector == null || campaignUI == null) {
            return null;
        }
        String preferredLower =
                preferredName == null ? "" : preferredName.trim().toLowerCase();
        Object firstCandidate = null;
        List entities = collectSectorEntities(sector);
        for (int i = 0; i < entities.size(); i++) {
            Object entity = entities.get(i);
            if (entity == null || entity == currentEntity) {
                continue;
            }
            if (!canPickInteractionDialogPlugin(campaignUI, entity)) {
                continue;
            }
            if (firstCandidate == null) {
                firstCandidate = entity;
            }
            if (!preferredLower.isEmpty()) {
                String name = describeEntityName(entity);
                if (name != null && name.toLowerCase().indexOf(preferredLower) >= 0) {
                    return entity;
                }
            }
        }
        return firstCandidate;
    }

    private static String ensurePersonNameStoreReadyForSyntheticFleet() {
        try {
            Class<?> personNameStoreClass = Class.forName("com.fs.starfarer.loading.PersonNameStore");
            Method initMethod = findMethodRecursive(personNameStoreClass, "o00000");
            if (initMethod == null) {
                return "init-method-missing";
            }
            initMethod.setAccessible(true);
            initMethod.invoke(null);

            Field mapField = findFieldRecursive(personNameStoreClass, "o00000");
            if (mapField == null) {
                return null;
            }
            mapField.setAccessible(true);
            Object storeObj = mapField.get(null);
            if (!(storeObj instanceof Map)) {
                return null;
            }
            int entryCount = 0;
            Map genderMap = (Map) storeObj;
            for (Object usageObj : genderMap.values()) {
                if (!(usageObj instanceof Map)) {
                    continue;
                }
                Map usageMap = (Map) usageObj;
                for (Object categoryObj : usageMap.values()) {
                    if (!(categoryObj instanceof Map)) {
                        continue;
                    }
                    Map categoryMap = (Map) categoryObj;
                    for (Object namesObj : categoryMap.values()) {
                        if (namesObj instanceof Collection) {
                            entryCount += ((Collection) namesObj).size();
                        }
                    }
                }
            }
            if (entryCount <= 0) {
                return "empty-after-init";
            }
            return null;
        } catch (Throwable t) {
            return "exception:" + describeThrowableChain(t);
        }
    }

    private static Object tryCreateSyntheticPlayerFleetFastPath(
            Object sector, Object selectedEntity) {
        try {
            List<String> creationFailures = new ArrayList<String>();
            Object fleet = null;
            try {
                Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
                Method getFactory = findMethodRecursive(globalClass, "getFactory");
                if (getFactory != null) {
                    getFactory.setAccessible(true);
                    Object factory = getFactory.invoke(null);
                    if (factory != null) {
                        Method createEmptyFleetById =
                                findMethodRecursive(
                                        factory.getClass(),
                                        "createEmptyFleet",
                                        String.class,
                                        String.class,
                                        Boolean.TYPE);
                        if (createEmptyFleetById != null) {
                            createEmptyFleetById.setAccessible(true);
                            String[] factionIdCandidates =
                                    resolveSyntheticFactionIdCandidates(sector, selectedEntity);
                            String[] fleetTypeCandidates = resolveSyntheticFleetTypeCandidates();
                            for (int f = 0;
                                    f < factionIdCandidates.length && fleet == null;
                                    f++) {
                                String factionId = factionIdCandidates[f];
                                if (factionId == null || factionId.trim().isEmpty()) {
                                    continue;
                                }
                                for (int i = 0;
                                        i < fleetTypeCandidates.length && fleet == null;
                                        i++) {
                                    String fleetType = fleetTypeCandidates[i];
                                    try {
                                        fleet =
                                                createEmptyFleetById.invoke(
                                                        factory,
                                                        factionId,
                                                        fleetType,
                                                        Boolean.TRUE);
                                    } catch (Throwable t) {
                                        creationFailures.add(
                                                "fast-createEmptyFleet("
                                                        + String.valueOf(factionId)
                                                        + ","
                                                        + String.valueOf(fleetType)
                                                        + ") exception: "
                                                        + describeThrowableChain(t));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                creationFailures.add(
                        "fast-factory-path exception: " + describeThrowableChain(t));
            }
            if (fleet == null) {
                fleet =
                        tryCreateSyntheticPlayerFleetViaConstructor(
                                sector, selectedEntity, creationFailures);
            }
            if (fleet == null) {
                if (!creationFailures.isEmpty()) {
                    maybeLogSyntheticPlayerFleetIssue(
                            "synthetic-fast-path-failures:"
                                    + joinSyntheticCreationFailures(creationFailures));
                }
                return null;
            }
            try {
                Method setName = findMethodRecursive(fleet.getClass(), "setName", String.class);
                if (setName != null) {
                    setName.setAccessible(true);
                    setName.invoke(fleet, "Player Fleet");
                }
            } catch (Throwable ignored) {
            }
            try {
                Method setFactionWithApply =
                        findMethodRecursive(fleet.getClass(), "setFaction", String.class, Boolean.TYPE);
                if (setFactionWithApply != null) {
                    setFactionWithApply.setAccessible(true);
                    setFactionWithApply.invoke(fleet, "player", Boolean.FALSE);
                } else {
                    Method setFactionSimple =
                            findMethodRecursive(fleet.getClass(), "setFaction", String.class);
                    if (setFactionSimple != null) {
                        setFactionSimple.setAccessible(true);
                        setFactionSimple.invoke(fleet, "player");
                    }
                }
            } catch (Throwable ignored) {
            }
            bindSyntheticPlayerFleetCommander(sector, fleet);
            ensureSyntheticPlayerFleetHasStarterShip(fleet);
            placeSyntheticPlayerFleetForInteraction(sector, fleet, selectedEntity);
            if (!assignRecoveredPlayerFleetToSector(sector, fleet)) {
                return null;
            }
            assignRecoveredPlayerFleetToCampaignEngine(fleet);
            maybeLogSyntheticPlayerFleetIssue(
                    "synthetic-fleet-fast-ready class=" + fleet.getClass().getName());
            System.out.println(
                    "Fixer: synthetic player fleet fast-path success class="
                            + fleet.getClass().getName());
            return fleet;
        } catch (Throwable t) {
            maybeLogSyntheticPlayerFleetException("synthetic-player-fleet-fast-path", t);
            return null;
        }
    }

    private static Object tryCreateSyntheticPlayerFleetForInteraction(
            Object sector, Object selectedEntity) {
        if (sector == null) {
            System.out.println("Fixer: synthetic player fleet create failed detail=sector-null");
            return null;
        }
        List<String> creationFailures = new ArrayList<String>();
        try {
            String personNameIssue = ensurePersonNameStoreReadyForSyntheticFleet();
            if (personNameIssue != null && !personNameIssue.trim().isEmpty()) {
                creationFailures.add(
                        "synthetic-preflight-person-names:" + String.valueOf(personNameIssue));
            }
            boolean allowSyntheticFactionSpecRepair =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignSyntheticFleetAllowFactionSpecRepair",
                                    "false"));
            String factionSpecIssue =
                    ensureFactionSpecsReadyForDirectNewGame(allowSyntheticFactionSpecRepair);
            if (factionSpecIssue != null && !factionSpecIssue.trim().isEmpty()) {
                creationFailures.add(
                        "synthetic-preflight-faction-specs:" + String.valueOf(factionSpecIssue));
            }
            String runtimeFactionIssue =
                    ensureRuntimeFactionsPresentForDirectNewGame(
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
                                "knights_of_ludd",
                                "persean",
                                "remnant"
                            },
                            true);
            if (runtimeFactionIssue != null && !runtimeFactionIssue.trim().isEmpty()) {
                creationFailures.add(
                        "synthetic-preflight-runtime-factions:" + String.valueOf(runtimeFactionIssue));
            }
        } catch (Throwable t) {
            creationFailures.add("synthetic-preflight-exception:" + describeThrowableChain(t));
        }
        try {
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getFactory = findMethodRecursive(globalClass, "getFactory");
            if (getFactory == null) {
                System.out.println(
                        "Fixer: synthetic player fleet create failed detail=global.getFactory-missing");
                maybeLogSyntheticPlayerFleetIssue("global.getFactory-missing");
                return null;
            }
            getFactory.setAccessible(true);
            Object factory = getFactory.invoke(null);
            if (factory == null) {
                System.out.println(
                        "Fixer: synthetic player fleet create failed detail=global.factory-null");
                maybeLogSyntheticPlayerFleetIssue("global.factory-null");
                return null;
            }

            Object fleet = null;
            boolean preferConstructorPath =
                    Boolean.parseBoolean(
                            System.getProperty(
                                    "starsector.autoCampaignSyntheticFleetPreferConstructor",
                                    "true"));
            if (preferConstructorPath) {
                fleet =
                        tryCreateSyntheticPlayerFleetViaConstructor(
                                sector, selectedEntity, creationFailures);
            }
            if (fleet == null) {
                Method createEmptyFleetById =
                        findMethodRecursive(
                                factory.getClass(),
                                "createEmptyFleet",
                                String.class,
                                String.class,
                                        Boolean.TYPE);
                if (createEmptyFleetById != null) {
                    createEmptyFleetById.setAccessible(true);
                    String[] factionIdCandidates =
                            resolveSyntheticFactionIdCandidates(sector, selectedEntity);
                    String[] fleetTypeCandidates = resolveSyntheticFleetTypeCandidates();
                    for (int f = 0; f < factionIdCandidates.length && fleet == null; f++) {
                        String factionId = factionIdCandidates[f];
                        if (factionId == null || factionId.trim().isEmpty()) {
                            continue;
                        }
                        for (int i = 0; i < fleetTypeCandidates.length && fleet == null; i++) {
                            String fleetType = fleetTypeCandidates[i];
                            try {
                                fleet =
                                        createEmptyFleetById.invoke(
                                                factory, factionId, fleetType, Boolean.TRUE);
                            } catch (Throwable t) {
                                maybeLogSyntheticPlayerFleetException(
                                        "createEmptyFleetById("
                                                + String.valueOf(factionId)
                                                + ","
                                                + String.valueOf(fleetType)
                                                + ")",
                                        t);
                                creationFailures.add(
                                        "createEmptyFleet("
                                                + String.valueOf(factionId)
                                                + ","
                                                + String.valueOf(fleetType)
                                                + ") exception: "
                                                + describeThrowableChain(t));
                            }
                        }
                    }
                } else {
                    creationFailures.add("createEmptyFleet(String,String,boolean) method missing");
                }
            }
            if (fleet == null) {
                try {
                    Class<?> factionApiClass = Class.forName("com.fs.starfarer.api.campaign.FactionAPI");
                    Method createEmptyFleetByFaction =
                            findMethodRecursive(
                                    factory.getClass(),
                                    "createEmptyFleet",
                                    factionApiClass,
                                    Boolean.TYPE);
                    if (createEmptyFleetByFaction != null) {
                        createEmptyFleetByFaction.setAccessible(true);
                        Object[] factionCandidates =
                                resolveSyntheticFactionObjectCandidates(sector, selectedEntity);
                        for (int i = 0; i < factionCandidates.length && fleet == null; i++) {
                            Object factionCandidate = factionCandidates[i];
                            if (factionCandidate == null) {
                                continue;
                            }
                            try {
                                fleet =
                                        createEmptyFleetByFaction.invoke(
                                                factory, factionCandidate, Boolean.TRUE);
                            } catch (Throwable t) {
                                maybeLogSyntheticPlayerFleetException(
                                        "createEmptyFleetByFaction(index=" + i + ")",
                                        t);
                                creationFailures.add(
                                        "createEmptyFleet(faction-candidate-"
                                                + i
                                                + ") exception: "
                                                + describeThrowableChain(t));
                            }
                        }
                        if (fleet == null && factionCandidates.length == 0) {
                            creationFailures.add("createEmptyFleet(faction) skipped: no-faction-candidates");
                        }
                    } else {
                        creationFailures.add("createEmptyFleet(FactionAPI,boolean) method missing");
                    }
                } catch (Throwable ignored) {
                }
            }
            if (fleet == null) {
                fleet =
                        tryCreateSyntheticPlayerFleetViaConstructor(
                                sector, selectedEntity, creationFailures);
            }
            if (fleet == null) {
                String failureSignature = null;
                if (!creationFailures.isEmpty()) {
                    failureSignature = joinSyntheticCreationFailures(creationFailures);
                }
                if (failureSignature == null || failureSignature.trim().isEmpty()) {
                    failureSignature = "factory-createEmptyFleet-returned-null";
                }
                System.out.println(
                        "Fixer: synthetic player fleet create failed detail="
                                + String.valueOf(failureSignature));
                maybeLogSyntheticPlayerFleetIssue(failureSignature);
                return null;
            }

            try {
                Method setName = findMethodRecursive(fleet.getClass(), "setName", String.class);
                if (setName != null) {
                    setName.setAccessible(true);
                    setName.invoke(fleet, "Player Fleet");
                }
            } catch (Throwable ignored) {
            }
            try {
                Method setFactionWithApply =
                        findMethodRecursive(fleet.getClass(), "setFaction", String.class, Boolean.TYPE);
                if (setFactionWithApply != null) {
                    setFactionWithApply.setAccessible(true);
                    setFactionWithApply.invoke(fleet, "player", Boolean.FALSE);
                } else {
                    Method setFactionSimple =
                            findMethodRecursive(fleet.getClass(), "setFaction", String.class);
                    if (setFactionSimple != null) {
                        setFactionSimple.setAccessible(true);
                        setFactionSimple.invoke(fleet, "player");
                    }
                }
            } catch (Throwable ignored) {
            }
            bindSyntheticPlayerFleetCommander(sector, fleet);

            ensureSyntheticPlayerFleetHasStarterShip(fleet);
            placeSyntheticPlayerFleetForInteraction(sector, fleet, selectedEntity);
            if (!assignRecoveredPlayerFleetToSector(sector, fleet)) {
                String assignFailureSignature =
                        "synthetic-fleet-assign-to-sector-failed class="
                                + fleet.getClass().getName();
                System.out.println(
                        "Fixer: synthetic player fleet create failed detail="
                                + assignFailureSignature);
                maybeLogSyntheticPlayerFleetIssue(
                        assignFailureSignature);
                return null;
            }
            assignRecoveredPlayerFleetToCampaignEngine(fleet);
            String readySignature = "synthetic-fleet-ready class=" + fleet.getClass().getName();
            System.out.println(
                    "Fixer: synthetic player fleet create success detail=" + readySignature);
            maybeLogSyntheticPlayerFleetIssue(
                    readySignature);
            return fleet;
        } catch (Throwable t) {
            maybeLogSyntheticPlayerFleetException("synthetic-player-fleet-create", t);
            System.out.println(
                    "Fixer: synthetic player fleet creation failed: " + describeThrowableChain(t));
            return null;
        }
    }

    private static String[] resolveSyntheticFleetTypeCandidates() {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        addSyntheticFleetTypeCandidate(ids, "taskForce");
        addSyntheticFleetTypeCandidate(ids, "patrolSmall");
        addSyntheticFleetTypeCandidate(ids, "trade");
        addSyntheticFleetTypeCandidate(ids, "smallTrader");
        addSyntheticFleetTypeCandidate(ids, "academyFleet");
        try {
            Class<?> fleetTypesClass =
                    Class.forName("com.fs.starfarer.api.impl.campaign.ids.FleetTypes");
            addSyntheticFleetTypeFromConstant(ids, fleetTypesClass, "TASK_FORCE");
            addSyntheticFleetTypeFromConstant(ids, fleetTypesClass, "PATROL_SMALL");
            addSyntheticFleetTypeFromConstant(ids, fleetTypesClass, "TRADE");
            addSyntheticFleetTypeFromConstant(ids, fleetTypesClass, "TRADE_SMALL");
            addSyntheticFleetTypeFromConstant(ids, fleetTypesClass, "ACADEMY_FLEET");
        } catch (Throwable ignored) {
        }
        if (ids.isEmpty()) {
            addSyntheticFleetTypeCandidate(ids, "taskForce");
        }
        return ids.toArray(new String[ids.size()]);
    }

    private static String[] resolveSyntheticFactionIdCandidates(Object sector, Object selectedEntity) {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        addSyntheticFactionIdCandidate(ids, "player");
        addSyntheticFactionIdCandidate(ids, tryResolveFactionIdFromEntity(selectedEntity));
        try {
            Object playerFaction = invokeNoArgIfPresent(sector, "getPlayerFaction");
            addSyntheticFactionIdCandidate(ids, tryResolveFactionIdFromObject(playerFaction));
        } catch (Throwable ignored) {
        }
        addSyntheticFactionIdCandidate(ids, "independent");
        addSyntheticFactionIdCandidate(ids, "hegemony");
        addSyntheticFactionIdCandidate(ids, "tritachyon");
        addSyntheticFactionIdCandidate(ids, "persean");
        addSyntheticFactionIdCandidate(ids, "persean_league");
        addSyntheticFactionIdCandidate(ids, "sindrian_diktat");
        addSyntheticFactionIdCandidate(ids, "diktat");
        addSyntheticFactionIdCandidate(ids, "luddic_church");
        addSyntheticFactionIdCandidate(ids, "luddic_path");
        addSyntheticFactionIdCandidate(ids, "knights_of_ludd");
        addSyntheticFactionIdCandidate(ids, "pirates");
        if (ids.isEmpty()) {
            addSyntheticFactionIdCandidate(ids, "player");
        }
        return ids.toArray(new String[ids.size()]);
    }

    private static Object tryCreateSyntheticPlayerFleetViaConstructor(
            Object sector, Object selectedEntity, List<String> creationFailures) {
        try {
            Class<?> campaignFleetClass =
                    Class.forName("com.fs.starfarer.campaign.fleet.CampaignFleet");
            try {
                java.lang.reflect.Constructor<?> noArg = campaignFleetClass.getDeclaredConstructor();
                noArg.setAccessible(true);
                Object fleet = noArg.newInstance();
                if (fleet != null) {
                    maybeEnableSyntheticFleetAIMode(fleet);
                    return fleet;
                }
            } catch (Throwable t) {
                maybeLogSyntheticPlayerFleetException("new CampaignFleet()", t);
                if (creationFailures != null) {
                    creationFailures.add(
                            "new CampaignFleet() exception: " + describeThrowableChain(t));
                }
            }

            Object[] factionCandidates = resolveSyntheticFactionObjectCandidates(sector, selectedEntity);
            for (int i = 0; i < factionCandidates.length; i++) {
                Object factionCandidate = factionCandidates[i];
                if (factionCandidate == null) {
                    continue;
                }
                try {
                    java.lang.reflect.Constructor<?> ctor =
                            findSingleArgCompatibleConstructor(
                                    campaignFleetClass, factionCandidate.getClass());
                    if (ctor == null) {
                        continue;
                    }
                    ctor.setAccessible(true);
                    Object fleet = ctor.newInstance(factionCandidate);
                    if (fleet != null) {
                        maybeEnableSyntheticFleetAIMode(fleet);
                        return fleet;
                    }
                } catch (Throwable t) {
                    maybeLogSyntheticPlayerFleetException(
                            "new CampaignFleet(faction-candidate-" + i + ")", t);
                    if (creationFailures != null) {
                        creationFailures.add(
                                "new CampaignFleet(faction-candidate-"
                                        + i
                                        + ") exception: "
                                        + describeThrowableChain(t));
                    }
                }
            }
        } catch (Throwable t) {
            maybeLogSyntheticPlayerFleetException("CampaignFleet constructor path", t);
            if (creationFailures != null) {
                creationFailures.add(
                        "CampaignFleet constructor path unavailable: " + describeThrowableChain(t));
            }
        }
        return null;
    }

    private static java.lang.reflect.Constructor<?> findSingleArgCompatibleConstructor(
            Class<?> ownerClass, Class<?> argClass) {
        if (ownerClass == null || argClass == null) {
            return null;
        }
        java.lang.reflect.Constructor<?>[] ctors = ownerClass.getDeclaredConstructors();
        for (int i = 0; i < ctors.length; i++) {
            java.lang.reflect.Constructor<?> ctor = ctors[i];
            if (ctor == null) {
                continue;
            }
            Class<?>[] params = ctor.getParameterTypes();
            if (params == null || params.length != 1) {
                continue;
            }
            if (params[0].isAssignableFrom(argClass)) {
                return ctor;
            }
        }
        return null;
    }

    private static void maybeEnableSyntheticFleetAIMode(Object fleet) {
        if (fleet == null) {
            return;
        }
        try {
            Method setAIModeNoSync =
                    findMethodRecursive(fleet.getClass(), "setAIModeNoSync", Boolean.TYPE);
            if (setAIModeNoSync != null) {
                setAIModeNoSync.setAccessible(true);
                setAIModeNoSync.invoke(fleet, Boolean.TRUE);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method setAIMode = findMethodRecursive(fleet.getClass(), "setAIMode", Boolean.TYPE);
            if (setAIMode != null) {
                setAIMode.setAccessible(true);
                setAIMode.invoke(fleet, Boolean.TRUE);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object[] resolveSyntheticFactionObjectCandidates(Object sector, Object selectedEntity) {
        LinkedHashSet<Object> out = new LinkedHashSet<Object>();
        try {
            Object playerFaction = invokeNoArgIfPresent(sector, "getPlayerFaction");
            if (playerFaction != null) {
                out.add(playerFaction);
            }
        } catch (Throwable ignored) {
        }
        try {
            Method getFaction = findMethodRecursive(sector.getClass(), "getFaction", String.class);
            if (getFaction != null) {
                getFaction.setAccessible(true);
                String[] ids = resolveSyntheticFactionIdCandidates(sector, selectedEntity);
                for (int i = 0; i < ids.length; i++) {
                    String id = ids[i];
                    if (id == null || id.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        Object faction = getFaction.invoke(sector, id);
                        if (faction != null) {
                            out.add(faction);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out.toArray(new Object[out.size()]);
    }

    private static String tryResolveFactionIdFromEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            Method getFactionId = findMethodRecursive(entity.getClass(), "getFactionId");
            if (getFactionId != null) {
                getFactionId.setAccessible(true);
                Object id = getFactionId.invoke(entity);
                if (id != null) {
                    return String.valueOf(id);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Method getFaction = findMethodRecursive(entity.getClass(), "getFaction");
            if (getFaction != null) {
                getFaction.setAccessible(true);
                Object faction = getFaction.invoke(entity);
                String id = tryResolveFactionIdFromObject(faction);
                if (id != null && !id.trim().isEmpty()) {
                    return id;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object market = invokeNoArgIfPresent(entity, "getMarket");
            String marketId = tryResolveFactionIdFromEntity(market);
            if (marketId != null && !marketId.trim().isEmpty()) {
                return marketId;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String tryResolveFactionIdFromObject(Object faction) {
        if (faction == null) {
            return null;
        }
        try {
            Method getId = findMethodRecursive(faction.getClass(), "getId");
            if (getId != null) {
                getId.setAccessible(true);
                Object id = getId.invoke(faction);
                if (id != null) {
                    return String.valueOf(id);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void addSyntheticFactionIdCandidate(Set<String> out, String id) {
        if (out == null || id == null) {
            return;
        }
        String trimmed = id.trim();
        if (!trimmed.isEmpty()) {
            out.add(trimmed);
        }
    }

    private static void addSyntheticFleetTypeFromConstant(
            Set<String> out, Class<?> ownerClass, String fieldName) {
        if (out == null || ownerClass == null || fieldName == null || fieldName.trim().isEmpty()) {
            return;
        }
        try {
            Field field = findFieldRecursive(ownerClass, fieldName.trim());
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof String) {
                addSyntheticFleetTypeCandidate(out, String.valueOf(value));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addSyntheticFleetTypeCandidate(Set<String> out, String value) {
        if (out == null || value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            out.add(trimmed);
        }
    }

    private static String joinSyntheticCreationFailures(List<String> failures) {
        if (failures == null || failures.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(4, failures.size());
        for (int i = 0; i < limit; i++) {
            String item = failures.get(i);
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(item.trim());
        }
        if (sb.length() == 0) {
            return null;
        }
        if (failures.size() > limit) {
            sb.append(" | +");
            sb.append(failures.size() - limit);
            sb.append(" more");
            int tailCount = Math.min(2, failures.size() - limit);
            for (int i = 0; i < tailCount; i++) {
                String tail = failures.get(failures.size() - tailCount + i);
                if (tail == null || tail.trim().isEmpty()) {
                    continue;
                }
                sb.append(" | tail:");
                sb.append(tail.trim());
            }
        }
        return sb.toString();
    }

    private static void bindSyntheticPlayerFleetCommander(Object sector, Object fleet) {
        if (sector == null || fleet == null) {
            return;
        }
        try {
            Object person = invokeNoArgIfPresent(sector, "getPlayerPerson");
            if (person == null) {
                Object characterData = invokeNoArgIfPresent(sector, "getCharacterData");
                if (characterData != null) {
                    person = invokeNoArgIfPresent(characterData, "getPerson");
                }
            }
            if (person == null) {
                return;
            }
            Method setCommander = findSingleArgMethod(fleet.getClass(), "setCommander");
            if (setCommander != null) {
                setCommander.setAccessible(true);
                setCommander.invoke(fleet, person);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void ensureSyntheticPlayerFleetHasStarterShip(Object fleet) {
        if (fleet == null) {
            return;
        }
        try {
            Object fleetData = invokeNoArgIfPresent(fleet, "getFleetData");
            if (fleetData == null) {
                return;
            }
            Method getNumMembers = findMethodRecursive(fleetData.getClass(), "getNumMembers");
            if (getNumMembers != null) {
                getNumMembers.setAccessible(true);
                Object countObj = getNumMembers.invoke(fleetData);
                if (countObj instanceof Number && ((Number) countObj).intValue() > 0) {
                    return;
                }
            }
            Method addFleetMemberByVariant =
                    findMethodRecursive(fleetData.getClass(), "addFleetMember", String.class);
            if (addFleetMemberByVariant == null) {
                return;
            }
            addFleetMemberByVariant.setAccessible(true);
            String preferredVariant =
                    System.getProperty(AUTO_CAMPAIGN_START_VARIANT_PROPERTY, "lasher_Standard");
            String[] variants =
                    new String[] {
                        preferredVariant,
                        "lasher_Standard",
                        "wolf_Standard",
                        "vigilance_Standard",
                        "kite_Standard"
                    };
            for (int i = 0; i < variants.length; i++) {
                String variant = variants[i];
                if (variant == null || variant.trim().isEmpty()) {
                    continue;
                }
                try {
                    Object added = addFleetMemberByVariant.invoke(fleetData, variant.trim());
                    if (added != null) {
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void placeSyntheticPlayerFleetForInteraction(
            Object sector, Object fleet, Object selectedEntity) {
        if (sector == null || fleet == null) {
            return;
        }
        Object targetLocation = null;
        try {
            if (selectedEntity != null) {
                Method getContainingLocation =
                        findMethodRecursive(selectedEntity.getClass(), "getContainingLocation");
                if (getContainingLocation != null) {
                    getContainingLocation.setAccessible(true);
                    targetLocation = getContainingLocation.invoke(selectedEntity);
                }
            }
        } catch (Throwable ignored) {
        }
        if (targetLocation == null) {
            targetLocation = invokeNoArgIfPresent(sector, "getCurrentLocation");
        }
        if (targetLocation == null) {
            targetLocation = invokeNoArgIfPresent(sector, "getHyperspace");
        }

        try {
            if (targetLocation != null) {
                Method setCurrentLocation =
                        findSingleArgMethod(sector.getClass(), "setCurrentLocation");
                if (setCurrentLocation != null) {
                    setCurrentLocation.setAccessible(true);
                    setCurrentLocation.invoke(sector, targetLocation);
                }
                Method setContainingLocation =
                        findSingleArgMethod(fleet.getClass(), "setContainingLocation");
                if (setContainingLocation != null) {
                    setContainingLocation.setAccessible(true);
                    setContainingLocation.invoke(fleet, targetLocation);
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            float x = 0f;
            float y = 0f;
            if (selectedEntity != null) {
                Method getLocation = findMethodRecursive(selectedEntity.getClass(), "getLocation");
                if (getLocation != null) {
                    getLocation.setAccessible(true);
                    Object loc = getLocation.invoke(selectedEntity);
                    if (loc != null) {
                        x = readFloatField(loc, "x", 0f);
                        y = readFloatField(loc, "y", 0f);
                    }
                }
            }
            Method setLocation =
                    findMethodRecursive(fleet.getClass(), "setLocation", Float.TYPE, Float.TYPE);
            if (setLocation != null) {
                setLocation.setAccessible(true);
                setLocation.invoke(fleet, Float.valueOf(x + 250f), Float.valueOf(y));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void assignRecoveredPlayerFleetToCampaignEngine(Object playerFleet) {
        if (playerFleet == null) {
            return;
        }
        try {
            Class<?> campaignEngineClass = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
            Method getInstance = findMethodRecursive(campaignEngineClass, "getInstance");
            if (getInstance == null) {
                return;
            }
            getInstance.setAccessible(true);
            Object engine = getInstance.invoke(null);
            if (engine == null) {
                return;
            }
            Method setPlayerFleet =
                    findSingleArgCompatibleMethod(
                            engine.getClass(), "setPlayerFleet", playerFleet.getClass());
            if (setPlayerFleet == null) {
                setPlayerFleet = findSingleArgMethod(engine.getClass(), "setPlayerFleet");
            }
            if (setPlayerFleet != null) {
                setPlayerFleet.setAccessible(true);
                setPlayerFleet.invoke(engine, playerFleet);
                return;
            }
            Field playerFleetField = findFieldRecursive(engine.getClass(), "playerFleet");
            if (playerFleetField != null) {
                playerFleetField.setAccessible(true);
                playerFleetField.set(engine, playerFleet);
                return;
            }
            assignObjectToLikelyFleetField(engine, playerFleet);
        } catch (Throwable ignored) {
        }
    }

    private static void maybeLogSyntheticPlayerFleetIssue(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean changed = !signature.equals(autoCampaignSyntheticPlayerFleetSignature);
        if (!changed && (now - autoCampaignSyntheticPlayerFleetLogAt) < 8000L) {
            return;
        }
        autoCampaignSyntheticPlayerFleetSignature = signature;
        autoCampaignSyntheticPlayerFleetLogAt = now;
        System.out.println("Fixer: synthetic player fleet status: " + signature);
    }

    private static void maybeLogSyntheticPlayerFleetException(String context, Throwable t) {
        if (t == null) {
            return;
        }
        String chain = describeThrowableChain(t);
        String lower = chain == null ? "" : chain.toLowerCase();
        if (lower.indexOf("arrayindexoutofboundsexception") < 0
                && lower.indexOf("nullpointerexception") < 0
                && lower.indexOf("invocationtargetexception") < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (autoCampaignSyntheticPlayerFleetLogAt > 0L
                && (now - autoCampaignSyntheticPlayerFleetLogAt) < 2500L) {
            return;
        }
        autoCampaignSyntheticPlayerFleetLogAt = now;
        System.out.println(
                "Fixer: synthetic player fleet exception context="
                        + String.valueOf(context)
                        + " chain="
                        + chain);
        System.out.println(
                "Fixer: synthetic player fleet exception stack:\n" + stackTraceToString(t));
    }

    private static void maybeLogDialogPluginFallbackIssue(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean changed = !signature.equals(autoCampaignDialogPluginFallbackSignature);
        if (!changed && (now - autoCampaignDialogPluginFallbackLogAt) < 8000L) {
            return;
        }
        autoCampaignDialogPluginFallbackSignature = signature;
        autoCampaignDialogPluginFallbackLogAt = now;
        System.out.println("Fixer: dialog plugin fallback status: " + signature);
    }

    private static void maybeLogFallbackMarketIssue(String signature, Throwable cause) {
        if (signature == null || signature.trim().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean changed = !signature.equals(autoCampaignFallbackMarketSignature);
        if (!changed && (now - autoCampaignFallbackMarketLogAt) < 8000L) {
            return;
        }
        autoCampaignFallbackMarketSignature = signature;
        autoCampaignFallbackMarketLogAt = now;
        System.out.println("Fixer: fallback market status: " + signature);
        if (cause != null) {
            System.out.println("Fixer: fallback market stack:\n" + stackTraceToString(cause));
        }
    }

    private static List coerceToList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return (List) value;
        }
        if (value instanceof Collection) {
            return new ArrayList((Collection) value);
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List out = new ArrayList(len);
            for (int i = 0; i < len; i++) {
                out.add(Array.get(value, i));
            }
            return out;
        }
        return null;
    }

    private static int countEconomyMarketsSafe(Object economy) {
        if (economy == null) {
            return -1;
        }
        try {
            Method getMarketsCopy = findMethodRecursive(economy.getClass(), "getMarketsCopy");
            if (getMarketsCopy == null) {
                return -1;
            }
            getMarketsCopy.setAccessible(true);
            Object value = getMarketsCopy.invoke(economy);
            if (value instanceof Collection) {
                return ((Collection) value).size();
            }
            if (value != null && value.getClass().isArray()) {
                return Array.getLength(value);
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static boolean tryBootstrapSectorGenForNoMarkets(Object sector, Object economy, List markets) {
        if (sector == null) {
            return false;
        }
        boolean enabled =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignBootstrapSectorGenOnNoMarkets", "true"));
        if (!enabled) {
            return false;
        }
        long now = System.currentTimeMillis();
        long cooldownMs =
                Math.max(
                        15000L,
                        parseLongProperty(
                                "starsector.autoCampaignSectorGenBootstrapCooldownMs", 90000L));
        int maxAttempts =
                (int)
                        Math.max(
                                1L,
                                parseLongProperty(
                                        "starsector.autoCampaignSectorGenBootstrapMaxAttempts", 2L));
        if (autoCampaignSectorBootstrapAttempts >= maxAttempts) {
            return false;
        }
        if (autoCampaignSectorBootstrapAttemptAt > 0L
                && (now - autoCampaignSectorBootstrapAttemptAt) < cooldownMs) {
            return false;
        }

        int economyMarketsBefore = countEconomyMarketsSafe(economy);
        int marketCandidatesBefore = markets == null ? -1 : markets.size();
        int sectorEntitiesBefore = collectSectorEntities(sector).size();
        if (economyMarketsBefore > 0 || marketCandidatesBefore > 0) {
            return false;
        }
        if (sectorEntitiesBefore > 0) {
            return false;
        }

        autoCampaignSectorBootstrapAttempts++;
        autoCampaignSectorBootstrapAttemptAt = now;
        System.out.println(
                "Fixer: auto campaign no-markets bootstrap attempt "
                        + autoCampaignSectorBootstrapAttempts
                        + " invoking data.scripts.world.SectorGen.generate().");
        try {
            Class<?> sectorGenClass = resolveSectorGenClass();
            if (sectorGenClass == null) {
                System.out.println(
                        "Fixer: auto campaign no-markets bootstrap failed: unable to resolve data.scripts.world.SectorGen (source="
                                + autoCampaignSectorGenClassSource
                                + ").");
                return false;
            }
            System.out.println(
                    "Fixer: auto campaign no-markets bootstrap using SectorGen classloader="
                            + autoCampaignSectorGenClassSource);
            ensureGlobalSectorForSectorGen(sector);
            Object hyperspaceBefore = invokeNoArgIfPresent(sector, "getHyperspace");
            System.out.println(
                    "Fixer: auto campaign no-markets bootstrap precheck globalSector="
                            + (readGlobalSectorForSectorGen() != null)
                            + " hyperspace="
                            + (hyperspaceBefore != null));
            Object sectorGen = sectorGenClass.getDeclaredConstructor().newInstance();
            Method generate =
                    findSingleArgCompatibleMethod(
                            sectorGenClass, "generate", sector.getClass());
            if (generate == null) {
                generate = findSingleArgMethod(sectorGenClass, "generate");
            }
            if (generate == null) {
                System.out.println(
                        "Fixer: auto campaign no-markets bootstrap failed: SectorGen.generate method not found.");
                return false;
            }
            generate.setAccessible(true);
            final Method generateMethod = generate;
            final long generateTimeoutMs =
                    Math.max(
                            5000L,
                            parseLongProperty(
                                    "starsector.autoCampaignSectorGenBootstrapTimeoutMs",
                                    30000L));
            final Throwable[] generateError = new Throwable[1];
            final boolean[] generateDone = new boolean[] {false};
            Thread bootstrapWorker =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        generateMethod.invoke(sectorGen, sector);
                                    } catch (Throwable invokeError) {
                                        generateError[0] = invokeError;
                                    } finally {
                                        generateDone[0] = true;
                                    }
                                }
                            },
                            "fixer-sectorgen-bootstrap-" + autoCampaignSectorBootstrapAttempts);
            bootstrapWorker.setDaemon(true);
            bootstrapWorker.start();
            try {
                bootstrapWorker.join(generateTimeoutMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println(
                        "Fixer: auto campaign no-markets bootstrap interrupted while waiting for SectorGen.generate(); continuing with fallback market seeding.");
                return false;
            }
            if (!generateDone[0]) {
                System.out.println(
                        "Fixer: auto campaign no-markets bootstrap timed out after "
                                + generateTimeoutMs
                                + "ms while running SectorGen.generate(); continuing with fallback market seeding.");
                try {
                    bootstrapWorker.interrupt();
                } catch (Throwable ignored) {
                }
                return false;
            }
            if (generateError[0] != null) {
                throw generateError[0];
            }
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign no-markets bootstrap exception: "
                            + describeThrowableChain(t));
            System.out.println(
                    "Fixer: auto campaign no-markets bootstrap stack:\n" + stackTraceToString(t));
            return false;
        }

        int economyMarketsAfter = countEconomyMarketsSafe(economy);
        int sectorEntitiesAfter = collectSectorEntities(sector).size();
        System.out.println(
                "Fixer: auto campaign no-markets bootstrap result economyMarkets="
                        + economyMarketsAfter
                        + " sectorEntities="
                        + sectorEntitiesAfter);
        return economyMarketsAfter > 0 || sectorEntitiesAfter > 0;
    }

    private static Object readGlobalSectorForSectorGen() {
        try {
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSector = findMethodRecursive(globalClass, "getSector");
            if (getSector == null) {
                return null;
            }
            getSector.setAccessible(true);
            return getSector.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void ensureGlobalSectorForSectorGen(Object sector) {
        if (sector == null) {
            return;
        }
        try {
            Object currentSector = readGlobalSectorForSectorGen();
            if (currentSector == sector) {
                return;
            }
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method setSector = findSingleArgMethod(globalClass, "setSector");
            if (setSector == null) {
                setSector = findSingleArgCompatibleMethod(globalClass, "setSector", sector.getClass());
            }
            if (setSector == null) {
                return;
            }
            setSector.setAccessible(true);
            setSector.invoke(null, sector);
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign unable to bind Global sector before SectorGen bootstrap: "
                            + describeThrowableChain(t));
        }
    }

    private static Class<?> resolveSectorGenClass() {
        final String className = "data.scripts.world.SectorGen";
        Class<?> cls = tryLoadClassFromLoader(className, null, "default");
        if (cls != null) {
            return cls;
        }

        LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        loaders.add(Fixer.class.getClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        loaders.add(resolveGlobalSettingsScriptClassLoader());
        loaders.add(resolveScriptStoreClassLoader());
        loaders.add(resolveSectorGenFallbackClassLoader());
        addThreadContextLoadersIfAllowed(loaders);

        for (ClassLoader loader : loaders) {
            cls = tryLoadClassFromLoader(className, loader, shortLoader(loader));
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    private static Class<?> tryLoadClassFromLoader(
            String className, ClassLoader loader, String sourceLabel) {
        try {
            Class<?> cls;
            if (loader == null) {
                cls = Class.forName(className);
                autoCampaignSectorGenClassSource = sourceLabel == null ? "default" : sourceLabel;
            } else {
                cls = Class.forName(className, true, loader);
                autoCampaignSectorGenClassSource = sourceLabel == null ? shortLoader(loader) : sourceLabel;
            }
            return cls;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ClassLoader resolveGlobalSettingsScriptClassLoader() {
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
            Method getScriptClassLoader =
                    findMethodRecursive(settings.getClass(), "getScriptClassLoader");
            if (getScriptClassLoader == null) {
                return null;
            }
            getScriptClassLoader.setAccessible(true);
            Object loaderObj = getScriptClassLoader.invoke(settings);
            if (loaderObj instanceof ClassLoader) {
                return (ClassLoader) loaderObj;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ClassLoader resolveScriptStoreClassLoader() {
        try {
            Class<?> scriptStoreClass = Class.forName("com.fs.starfarer.loading.scripts.ScriptStore");
            for (Method m : scriptStoreClass.getDeclaredMethods()) {
                if (m == null || !Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                if (m.getParameterTypes() != null && m.getParameterTypes().length != 0) {
                    continue;
                }
                if (!ClassLoader.class.isAssignableFrom(m.getReturnType())) {
                    continue;
                }
                m.setAccessible(true);
                Object loaderObj = m.invoke(null);
                if (loaderObj instanceof ClassLoader) {
                    return (ClassLoader) loaderObj;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ClassLoader resolveSectorGenFallbackClassLoader() {
        ClassLoader cached = autoCampaignSectorGenFallbackLoader;
        if (cached != null) {
            return cached;
        }

        String rootsProp =
                System.getProperty(
                        "starsector.autoCampaignSectorGenClassRoots",
                        "/app/starsectorquick/tmp_compile_sectorgen_check,"
                                + "/app/tmp_compile_sectorgen_check,"
                                + "/app/starsector/tmp_compile_sectorgen_check,"
                                + "tmp_compile_sectorgen_check");
        if (rootsProp == null || rootsProp.trim().isEmpty()) {
            return null;
        }

        String[] roots = rootsProp.split(",");
        List<URL> urls = new ArrayList<URL>();
        for (int i = 0; i < roots.length; i++) {
            String root = roots[i];
            if (root == null) {
                continue;
            }
            String trimmed = root.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                File dir = new File(trimmed);
                if (!dir.isDirectory()) {
                    continue;
                }
                urls.add(dir.toURI().toURL());
            } catch (Throwable ignored) {
            }
        }
        if (urls.isEmpty()) {
            return null;
        }

        try {
            URLClassLoader loader =
                    new URLClassLoader(urls.toArray(new URL[urls.size()]), Fixer.class.getClassLoader());
            autoCampaignSectorGenFallbackLoader = loader;
            return loader;
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: auto campaign unable to build SectorGen fallback classloader: "
                            + describeThrowableChain(t));
            return null;
        }
    }

    private static void maybeLogNoMarketsDiagnostics(Object sector, Object economy, List markets) {
        try {
            int economyMarkets = -1;
            if (economy != null) {
                Method getMarketsCopy = findMethodRecursive(economy.getClass(), "getMarketsCopy");
                if (getMarketsCopy != null) {
                    getMarketsCopy.setAccessible(true);
                    Object economyMarketsObj = getMarketsCopy.invoke(economy);
                    if (economyMarketsObj instanceof Collection) {
                        economyMarkets = ((Collection) economyMarketsObj).size();
                    } else if (economyMarketsObj != null && economyMarketsObj.getClass().isArray()) {
                        economyMarkets = Array.getLength(economyMarketsObj);
                    }
                }
            }

            int candidateMarkets = -1;
            if (markets != null) {
                candidateMarkets = markets.size();
            }

            int sectorEntities = -1;
            if (sector != null) {
                sectorEntities = collectSectorEntities(sector).size();
            }

            boolean playerFleetPresent = false;
            if (sector != null) {
                Method getPlayerFleet = findMethodRecursive(sector.getClass(), "getPlayerFleet");
                if (getPlayerFleet != null) {
                    getPlayerFleet.setAccessible(true);
                    playerFleetPresent = getPlayerFleet.invoke(sector) != null;
                }
            }

            String signature =
                    economyMarkets
                            + "|"
                            + candidateMarkets
                            + "|"
                            + sectorEntities
                            + "|"
                            + playerFleetPresent;
            long now = System.currentTimeMillis();
            boolean unchanged =
                    signature.equals(autoCampaignColonyNoMarketsSignature)
                            && (now - autoCampaignColonyNoMarketsLogAt) < 8000L;
            if (unchanged) {
                return;
            }

            autoCampaignColonyNoMarketsSignature = signature;
            autoCampaignColonyNoMarketsLogAt = now;
            System.out.println(
                    "Fixer: auto campaign colony-target diagnostics no-markets economyMarkets="
                            + economyMarkets
                            + " candidateMarkets="
                            + candidateMarkets
                            + " sectorEntities="
                            + sectorEntities
                            + " playerFleetPresent="
                            + playerFleetPresent);
        } catch (Throwable ignored) {
        }
    }

    private static void addMarketsFromLocationCollection(
            Object sector, String methodName, List markets, Set seen) {
        try {
            Method method = findMethodRecursive(sector.getClass(), methodName);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            Object result = method.invoke(sector);
            if (result instanceof Iterable) {
                Iterator it = ((Iterable) result).iterator();
                while (it.hasNext()) {
                    collectMarketsFromLocationObject(it.next(), markets, seen);
                }
            } else if (result != null && result.getClass().isArray()) {
                int len = Array.getLength(result);
                for (int i = 0; i < len; i++) {
                    collectMarketsFromLocationObject(Array.get(result, i), markets, seen);
                }
            } else if (result != null) {
                collectMarketsFromLocationObject(result, markets, seen);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addMarketsFromLocationObjectByMethod(
            Object sector, String methodName, List markets, Set seen) {
        try {
            Method method = findMethodRecursive(sector.getClass(), methodName);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            Object location = method.invoke(sector);
            collectMarketsFromLocationObject(location, markets, seen);
        } catch (Throwable ignored) {
        }
    }

    private static void collectMarketsFromLocationObject(Object location, List markets, Set seen) {
        if (location == null) {
            return;
        }
        addMarketsFromEntityCollection(location, "getAllEntities", markets, seen);
        addMarketsFromEntityCollection(location, "getEntities", markets, seen);
        addMarketsFromEntityCollection(location, "getEntitiesCopy", markets, seen);
        addMarketsFromEntityCollection(location, "getAllEntitiesCopy", markets, seen);
    }

    private static void addMarketsFromEntityCollection(
            Object location, String methodName, List markets, Set seen) {
        try {
            Method method = findMethodRecursive(location.getClass(), methodName);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            Object entities = method.invoke(location);
            if (entities instanceof Iterable) {
                Iterator it = ((Iterable) entities).iterator();
                while (it.hasNext()) {
                    addMarketFromEntity(it.next(), markets, seen);
                }
            } else if (entities != null && entities.getClass().isArray()) {
                int len = Array.getLength(entities);
                for (int i = 0; i < len; i++) {
                    addMarketFromEntity(Array.get(entities, i), markets, seen);
                }
            } else if (entities != null) {
                addMarketFromEntity(entities, markets, seen);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addMarketFromEntity(Object entity, List markets, Set seen) {
        if (entity == null) {
            return;
        }
        try {
            Method getMarket = findMethodRecursive(entity.getClass(), "getMarket");
            if (getMarket == null) {
                return;
            }
            getMarket.setAccessible(true);
            Object market = getMarket.invoke(entity);
            if (market == null) {
                return;
            }
            if (seen.add(market)) {
                markets.add(market);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object findPreferredColonyEntity(Object sector, String preferredName) {
        List entities = collectSectorEntities(sector);
        if (entities.isEmpty()) {
            return null;
        }
        String preferred = preferredName == null ? "" : preferredName.trim().toLowerCase();
        Object firstEligible = null;
        for (Object entity : entities) {
            if (!isLikelyColonyInteractionEntity(entity)) {
                continue;
            }
            String name = describeEntityName(entity);
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            String lower = name.toLowerCase();
            if (!preferred.isEmpty() && lower.indexOf(preferred) >= 0) {
                return entity;
            }
            if (firstEligible == null) {
                firstEligible = entity;
            }
        }
        return firstEligible;
    }

    private static List collectSectorEntities(Object sector) {
        List entities = new ArrayList();
        Set seen = new LinkedHashSet();
        if (sector == null) {
            return entities;
        }
        collectEntitiesFromLocation(sector, entities, seen);
        collectEntitiesFromSectorCollectionMethod(sector, "getStarSystems", entities, seen);
        collectEntitiesFromSectorCollectionMethod(sector, "getAllLocations", entities, seen);
        collectEntitiesFromSectorCollectionMethod(sector, "getLocations", entities, seen);
        collectEntitiesFromSectorObjectMethod(sector, "getHyperspace", entities, seen);
        return entities;
    }

    private static void collectEntitiesFromSectorCollectionMethod(
            Object sector, String methodName, List entities, Set seen) {
        try {
            Method method = findMethodRecursive(sector.getClass(), methodName);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            Object result = method.invoke(sector);
            if (result instanceof Iterable) {
                Iterator it = ((Iterable) result).iterator();
                while (it.hasNext()) {
                    collectEntitiesFromLocation(it.next(), entities, seen);
                }
            } else if (result != null && result.getClass().isArray()) {
                int len = Array.getLength(result);
                for (int i = 0; i < len; i++) {
                    collectEntitiesFromLocation(Array.get(result, i), entities, seen);
                }
            } else if (result != null) {
                collectEntitiesFromLocation(result, entities, seen);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void collectEntitiesFromSectorObjectMethod(
            Object sector, String methodName, List entities, Set seen) {
        try {
            Method method = findMethodRecursive(sector.getClass(), methodName);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            Object result = method.invoke(sector);
            collectEntitiesFromLocation(result, entities, seen);
        } catch (Throwable ignored) {
        }
    }

    private static void collectEntitiesFromLocation(Object location, List entities, Set seen) {
        if (location == null) {
            return;
        }
        addEntitiesFromLocationMethod(location, "getAllEntities", entities, seen);
        addEntitiesFromLocationMethod(location, "getEntities", entities, seen);
        addEntitiesFromLocationMethod(location, "getEntitiesCopy", entities, seen);
        addEntitiesFromLocationMethod(location, "getAllEntitiesCopy", entities, seen);
    }

    private static void addEntitiesFromLocationMethod(
            Object location, String methodName, List entities, Set seen) {
        try {
            Method method = findMethodRecursive(location.getClass(), methodName);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            Object result = method.invoke(location);
            if (result instanceof Iterable) {
                Iterator it = ((Iterable) result).iterator();
                while (it.hasNext()) {
                    addEntityIfNew(it.next(), entities, seen);
                }
            } else if (result != null && result.getClass().isArray()) {
                int len = Array.getLength(result);
                for (int i = 0; i < len; i++) {
                    addEntityIfNew(Array.get(result, i), entities, seen);
                }
            } else if (result != null) {
                addEntityIfNew(result, entities, seen);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addEntityIfNew(Object entity, List entities, Set seen) {
        if (entity == null) {
            return;
        }
        if (seen.add(entity)) {
            entities.add(entity);
        }
    }

    private static Object tryRecoverPlayerFleetFromSectorEntities(Object sector) {
        List entities = collectSectorEntities(sector);
        if (entities.isEmpty()) {
            return tryRecoverPlayerFleetFromCampaignEngine(sector);
        }
        Object fallbackFleet = null;
        Object genericFleetCandidate = null;
        for (Object entity : entities) {
            if (entity == null) {
                continue;
            }
            String className = entity.getClass().getName().toLowerCase();
            if (className.indexOf("fleet") < 0) {
                continue;
            }
            if (genericFleetCandidate == null) {
                genericFleetCandidate = entity;
            }
            try {
                Method isPlayerFleet = findMethodRecursive(entity.getClass(), "isPlayerFleet");
                if (isPlayerFleet != null) {
                    isPlayerFleet.setAccessible(true);
                    Object result = isPlayerFleet.invoke(entity);
                    if (result instanceof Boolean && ((Boolean) result).booleanValue()) {
                        fallbackFleet = entity;
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
            if (fallbackFleet == null) {
                try {
                    Method getFaction = findMethodRecursive(entity.getClass(), "getFaction");
                    if (getFaction != null) {
                        getFaction.setAccessible(true);
                        Object faction = getFaction.invoke(entity);
                        if (faction != null) {
                            Method getId = findMethodRecursive(faction.getClass(), "getId");
                            if (getId != null) {
                                getId.setAccessible(true);
                                Object idObj = getId.invoke(faction);
                                if (idObj != null
                                        && "player".equalsIgnoreCase(String.valueOf(idObj))) {
                                    fallbackFleet = entity;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        if (fallbackFleet == null) {
            fallbackFleet = tryRecoverPlayerFleetFromCampaignEngine(sector);
            if (fallbackFleet == null && genericFleetCandidate != null) {
                fallbackFleet = genericFleetCandidate;
                System.out.println(
                        "Fixer: auto campaign adopting generic fleet as player fallback class="
                                + genericFleetCandidate.getClass().getName());
                tryForcePlayerFleetFaction(fallbackFleet);
            }
            if (fallbackFleet == null) {
                return null;
            }
        }
        if (assignRecoveredPlayerFleetToSector(sector, fallbackFleet)) {
            return fallbackFleet;
        }
        return null;
    }

    private static boolean assignRecoveredPlayerFleetToSector(Object sector, Object fallbackFleet) {
        if (sector == null || fallbackFleet == null) {
            return false;
        }
        try {
            Method setPlayerFleet =
                    findSingleArgCompatibleMethod(
                            sector.getClass(), "setPlayerFleet", fallbackFleet.getClass());
            if (setPlayerFleet == null) {
                setPlayerFleet = findSingleArgMethod(sector.getClass(), "setPlayerFleet");
            }
            if (setPlayerFleet != null) {
                setPlayerFleet.setAccessible(true);
                setPlayerFleet.invoke(sector, fallbackFleet);
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Field playerFleetField = findFieldRecursive(sector.getClass(), "playerFleet");
            if (playerFleetField != null) {
                playerFleetField.setAccessible(true);
                playerFleetField.set(sector, fallbackFleet);
                return true;
            }
        } catch (Throwable ignored) {
        }
        if (assignObjectToLikelyFleetField(sector, fallbackFleet)) {
            return true;
        }
        return false;
    }

    private static void tryForcePlayerFleetFaction(Object fleet) {
        if (fleet == null) {
            return;
        }
        try {
            Method setFactionWithApply =
                    findMethodRecursive(fleet.getClass(), "setFaction", String.class, Boolean.TYPE);
            if (setFactionWithApply != null) {
                setFactionWithApply.setAccessible(true);
                setFactionWithApply.invoke(fleet, "player", Boolean.FALSE);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method setFactionSimple =
                    findMethodRecursive(fleet.getClass(), "setFaction", String.class);
            if (setFactionSimple != null) {
                setFactionSimple.setAccessible(true);
                setFactionSimple.invoke(fleet, "player");
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean assignObjectToLikelyFleetField(Object owner, Object fleet) {
        if (owner == null || fleet == null) {
            return false;
        }
        try {
            List<Field> strongCandidates = new ArrayList<Field>();
            List<Field> weakCandidates = new ArrayList<Field>();
            Class<?> cursor = owner.getClass();
            while (cursor != null) {
                Field[] fields = cursor.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    Field f = fields[i];
                    if (f == null || Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    Class<?> fieldType = f.getType();
                    if (fieldType == null || !fieldType.isAssignableFrom(fleet.getClass())) {
                        continue;
                    }
                    String nameLower = String.valueOf(f.getName()).toLowerCase();
                    if (nameLower.indexOf("player") >= 0 || nameLower.indexOf("fleet") >= 0) {
                        strongCandidates.add(f);
                    } else {
                        weakCandidates.add(f);
                    }
                }
                cursor = cursor.getSuperclass();
            }

            Field selected = null;
            if (strongCandidates.size() == 1) {
                selected = strongCandidates.get(0);
            } else if (strongCandidates.size() > 1) {
                for (int i = 0; i < strongCandidates.size(); i++) {
                    Field candidate = strongCandidates.get(i);
                    if (candidate != null
                            && String.valueOf(candidate.getName()).toLowerCase().indexOf("player")
                                    >= 0) {
                        selected = candidate;
                        break;
                    }
                }
                if (selected == null) {
                    selected = strongCandidates.get(0);
                }
            } else if (weakCandidates.size() == 1) {
                selected = weakCandidates.get(0);
            }

            if (selected == null) {
                return false;
            }
            selected.setAccessible(true);
            selected.set(owner, fleet);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Object tryRecoverPlayerFleetFromCampaignEngine(Object sector) {
        try {
            Class<?> campaignEngineClass = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
            Method getInstance = findMethodRecursive(campaignEngineClass, "getInstance");
            if (getInstance == null) {
                return null;
            }
            getInstance.setAccessible(true);
            Object engine = getInstance.invoke(null);
            if (engine == null) {
                return null;
            }

            Object enginePlayerFleet = invokeNoArgIfPresent(engine, "getPlayerFleet");
            if (enginePlayerFleet == null) {
                enginePlayerFleet = readFieldRecursive(engine, "playerFleet");
            }
            if (enginePlayerFleet == null) {
                Object[] locations =
                        new Object[] {
                            invokeNoArgIfPresent(engine, "getCurrentLocation"),
                            invokeNoArgIfPresent(engine, "getHyperspace"),
                            invokeNoArgIfPresent(engine, "getCurrentContainingLocation")
                        };
                for (Object location : locations) {
                    if (location == null) {
                        continue;
                    }
                    List candidates = new ArrayList();
                    Set seen = new LinkedHashSet();
                    collectEntitiesFromLocation(location, candidates, seen);
                    for (Object candidate : candidates) {
                        if (candidate == null) {
                            continue;
                        }
                        String className = candidate.getClass().getName().toLowerCase();
                        if (className.indexOf("fleet") < 0) {
                            continue;
                        }
                        try {
                            Method isPlayerFleet =
                                    findMethodRecursive(candidate.getClass(), "isPlayerFleet");
                            if (isPlayerFleet != null) {
                                isPlayerFleet.setAccessible(true);
                                Object flag = isPlayerFleet.invoke(candidate);
                                if (flag instanceof Boolean
                                        && ((Boolean) flag).booleanValue()) {
                                    enginePlayerFleet = candidate;
                                    break;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    if (enginePlayerFleet != null) {
                        break;
                    }
                }
            }
            if (enginePlayerFleet == null) {
                return null;
            }
            if (sector != null) {
                assignRecoveredPlayerFleetToSector(sector, enginePlayerFleet);
            }
            System.out.println(
                    "Fixer: auto campaign recovered player fleet via CampaignEngine fallback.");
            return enginePlayerFleet;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isLikelyColonyInteractionEntity(Object entity) {
        if (entity == null) {
            return false;
        }
        try {
            Method getMarket = findMethodRecursive(entity.getClass(), "getMarket");
            if (getMarket != null) {
                getMarket.setAccessible(true);
                Object market = getMarket.invoke(entity);
                if (market != null) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Method isPlanet = findMethodRecursive(entity.getClass(), "isPlanet");
            if (isPlanet != null) {
                isPlanet.setAccessible(true);
                Object result = isPlanet.invoke(entity);
                if (result instanceof Boolean && ((Boolean) result).booleanValue()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        String className = entity.getClass().getName().toLowerCase();
        return className.indexOf("planet") >= 0 || className.indexOf("market") >= 0;
    }

    private static String describeEntityName(Object entity) {
        if (entity == null) {
            return "unknown";
        }
        try {
            Method getName = findMethodRecursive(entity.getClass(), "getName");
            if (getName != null) {
                getName.setAccessible(true);
                Object value = getName.invoke(entity);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Method getFullName = findMethodRecursive(entity.getClass(), "getFullName");
            if (getFullName != null) {
                getFullName.setAccessible(true);
                Object value = getFullName.invoke(entity);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        } catch (Throwable ignored) {
        }
        return entity.getClass().getSimpleName();
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
        if (lower.indexOf("npefix") >= 0
                && (lower.indexOf("unsatisfiedlinkerror") >= 0
                        || lower.indexOf("java.library.path") >= 0
                        || lower.indexOf("jvm_loadlibrary") >= 0)) {
            return false;
        }
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
                    System.getProperty(AUTO_CAMPAIGN_STARTING_LOCATION_PROPERTY, "hyperspace").trim();
            if (startingLocation.isEmpty()) {
                startingLocation = "hyperspace";
            }
            if (setStartingLocationName != null) {
                setStartingLocationName.setAccessible(true);
                setStartingLocationName.invoke(data, startingLocation);
            }
            Method getStartingCoordinates =
                    findMethodRecursive(dataClass, "getStartingCoordinates");
            float startX = parseFloatProperty(AUTO_CAMPAIGN_START_X_PROPERTY, -2500f);
            float startY = parseFloatProperty(AUTO_CAMPAIGN_START_Y_PROPERTY, 3000f);
            Object coords = null;
            if (getStartingCoordinates != null) {
                getStartingCoordinates.setAccessible(true);
                coords = getStartingCoordinates.invoke(data);
            }
            if (coords == null) {
                Method setStartingCoordinates = findSingleArgMethod(dataClass, "setStartingCoordinates");
                Class<?> coordsType = null;
                if (setStartingCoordinates != null) {
                    Class<?>[] p = setStartingCoordinates.getParameterTypes();
                    if (p != null && p.length == 1) {
                        coordsType = p[0];
                    }
                }
                if (coordsType == null && getStartingCoordinates != null) {
                    coordsType = getStartingCoordinates.getReturnType();
                }
                Object createdCoords = createStartingCoordinates(coordsType, startX, startY);
                if (createdCoords != null) {
                    if (setStartingCoordinates != null) {
                        setStartingCoordinates.setAccessible(true);
                        setStartingCoordinates.invoke(data, createdCoords);
                    } else {
                        writeObjectField(data, "startingCoordinates", createdCoords);
                    }
                    if (getStartingCoordinates != null) {
                        try {
                            coords = getStartingCoordinates.invoke(data);
                        } catch (Throwable ignored) {
                            coords = createdCoords;
                        }
                    } else {
                        coords = createdCoords;
                    }
                }
            }
            if (coords != null) {
                writeFloatField(coords, "x", startX);
                writeFloatField(coords, "y", startY);
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

    private static boolean claimAutoCampaignWatcherLease(String owner, long now, long leaseMs) {
        if (owner == null || owner.length() == 0) {
            return true;
        }
        long safeNow = now <= 0L ? System.currentTimeMillis() : now;
        long safeLeaseMs = Math.max(5000L, leaseMs);
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_WATCHER_LEASE_OWNER_PROPERTY, "");
            long activeAt = parseLongProperty(AUTO_CAMPAIGN_WATCHER_LEASE_AT_PROPERTY, -1L);
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
            System.setProperty(AUTO_CAMPAIGN_WATCHER_LEASE_OWNER_PROPERTY, owner);
            System.setProperty(AUTO_CAMPAIGN_WATCHER_LEASE_AT_PROPERTY, String.valueOf(safeNow));
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void releaseAutoCampaignWatcherLease(String owner) {
        if (owner == null || owner.length() == 0) {
            return;
        }
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_WATCHER_LEASE_OWNER_PROPERTY, "");
            if (owner.equals(activeOwner)) {
                System.clearProperty(AUTO_CAMPAIGN_WATCHER_LEASE_OWNER_PROPERTY);
                System.clearProperty(AUTO_CAMPAIGN_WATCHER_LEASE_AT_PROPERTY);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String buildDirectNewGameCreateLeaseOwner() {
        long now = System.currentTimeMillis();
        Thread thread = Thread.currentThread();
        String threadName = thread == null ? "unknown-thread" : String.valueOf(thread.getName());
        long threadId = thread == null ? -1L : thread.getId();
        String loaderId = Integer.toHexString(System.identityHashCode(Fixer.class.getClassLoader()));
        return "fixer-direct-create@"
                + now
                + "#"
                + threadName
                + ":"
                + threadId
                + "/cl@"
                + loaderId;
    }

    private static boolean claimDirectNewGameCreateLease(String owner, long now, long leaseMs) {
        if (owner == null || owner.length() == 0) {
            return true;
        }
        long safeNow = now <= 0L ? System.currentTimeMillis() : now;
        long safeLeaseMs = Math.max(5000L, leaseMs);
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_OWNER_PROPERTY, "");
            long activeAt = parseLongProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_AT_PROPERTY, -1L);
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
            System.setProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_OWNER_PROPERTY, owner);
            System.setProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_AT_PROPERTY, String.valueOf(safeNow));
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void releaseDirectNewGameCreateLease(String owner) {
        if (owner == null || owner.length() == 0) {
            return;
        }
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_OWNER_PROPERTY, "");
            if (owner.equals(activeOwner)) {
                System.clearProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_OWNER_PROPERTY);
                System.clearProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_AT_PROPERTY);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String buildDirectNewGameWorkerLeaseOwner(int workerId) {
        long now = System.currentTimeMillis();
        Thread thread = Thread.currentThread();
        String threadName = thread == null ? "unknown-thread" : String.valueOf(thread.getName());
        long threadId = thread == null ? -1L : thread.getId();
        String loaderId = Integer.toHexString(System.identityHashCode(Fixer.class.getClassLoader()));
        return "fixer-direct-worker-"
                + workerId
                + "@"
                + now
                + "#"
                + threadName
                + ":"
                + threadId
                + "/cl@"
                + loaderId;
    }

    private static boolean claimDirectNewGameWorkerLease(String owner, long now, long leaseMs) {
        if (owner == null || owner.length() == 0) {
            return true;
        }
        long safeNow = now <= 0L ? System.currentTimeMillis() : now;
        long safeLeaseMs = Math.max(5000L, leaseMs);
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_OWNER_PROPERTY, "");
            long activeAt =
                    parseLongProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_AT_PROPERTY, -1L);
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
            System.setProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_OWNER_PROPERTY, owner);
            System.setProperty(
                    AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_AT_PROPERTY, String.valueOf(safeNow));
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void touchDirectNewGameWorkerLease(String owner, long now) {
        if (owner == null || owner.length() == 0) {
            return;
        }
        long safeNow = now <= 0L ? System.currentTimeMillis() : now;
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_OWNER_PROPERTY, "");
            if (!owner.equals(activeOwner)) {
                return;
            }
            System.setProperty(
                    AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_AT_PROPERTY, String.valueOf(safeNow));
        } catch (Throwable ignored) {
        }
    }

    private static void releaseDirectNewGameWorkerLease(String owner) {
        if (owner == null || owner.length() == 0) {
            return;
        }
        try {
            String activeOwner =
                    System.getProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_OWNER_PROPERTY, "");
            if (owner.equals(activeOwner)) {
                System.clearProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_OWNER_PROPERTY);
                System.clearProperty(AUTO_CAMPAIGN_DIRECT_NEW_GAME_WORKER_LEASE_AT_PROPERTY);
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

    private static void writeObjectField(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null) {
            return;
        }
        try {
            Field field = findFieldRecursive(target.getClass(), fieldName);
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (Throwable ignored) {
        }
    }

    private static Object createStartingCoordinates(Class<?> coordsType, float startX, float startY) {
        if (coordsType == null) {
            return null;
        }
        try {
            java.lang.reflect.Constructor<?> ctor =
                    coordsType.getDeclaredConstructor(Float.TYPE, Float.TYPE);
            ctor.setAccessible(true);
            return ctor.newInstance(Float.valueOf(startX), Float.valueOf(startY));
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Constructor<?> ctor =
                    coordsType.getDeclaredConstructor(Double.TYPE, Double.TYPE);
            ctor.setAccessible(true);
            return ctor.newInstance(Double.valueOf(startX), Double.valueOf(startY));
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Constructor<?> ctor = coordsType.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object coords = ctor.newInstance();
            writeFloatField(coords, "x", startX);
            writeFloatField(coords, "y", startY);
            return coords;
        } catch (Throwable ignored) {
        }
        return null;
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

    private static Method findTwoArgCompatibleMethod(
            Class<?> cls, String name, Class<?> arg0Type, Class<?> arg1Type) {
        if (cls == null || name == null || arg0Type == null || arg1Type == null) {
            return null;
        }
        for (Method m : cls.getMethods()) {
            if (!name.equals(m.getName()) || m.getParameterTypes().length != 2) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params[0].isAssignableFrom(arg0Type) && params[1].isAssignableFrom(arg1Type)) {
                return m;
            }
        }
        for (Method m : cls.getDeclaredMethods()) {
            if (!name.equals(m.getName()) || m.getParameterTypes().length != 2) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params[0].isAssignableFrom(arg0Type) && params[1].isAssignableFrom(arg1Type)) {
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

    private static boolean shouldResetCampaignEngineBeforeDirectNewGame(String runtimeReadinessIssue) {
        if (Boolean.parseBoolean(
                System.getProperty("starsector.autoCampaignForceCampaignEngineReset", "false"))) {
            return true;
        }
        if (runtimeReadinessIssue == null || runtimeReadinessIssue.trim().isEmpty()) {
            return false;
        }
        String lower = runtimeReadinessIssue.trim().toLowerCase();
        if (lower.indexOf("sector-null") >= 0
                || lower.indexOf("global-getsector-missing") >= 0
                || lower.indexOf("sector-economy-null") >= 0) {
            return true;
        }
        if (lower.indexOf("player-fleet-null") >= 0) {
            return Boolean.parseBoolean(
                    System.getProperty(
                            "starsector.autoCampaignResetCampaignEngineOnPlayerFleetNull",
                            "false"));
        }
        return false;
    }

    private static long readLastDirectNewGameCreateInvokeAt() {
        long local = lastDirectNewGameCreateInvokeAt;
        long global = parseLongProperty(AUTO_CAMPAIGN_DIRECT_CREATE_INVOKE_AT_PROPERTY, -1L);
        return Math.max(local, global);
    }

    private static void markDirectNewGameCreateInvokeAt(long atMs) {
        long safeAt = atMs <= 0L ? System.currentTimeMillis() : atMs;
        lastDirectNewGameCreateInvokeAt = safeAt;
        try {
            System.setProperty(
                    AUTO_CAMPAIGN_DIRECT_CREATE_INVOKE_AT_PROPERTY, String.valueOf(safeAt));
        } catch (Throwable ignored) {
        }
    }

    private static long readLastCampaignStateCreateInvokeAt() {
        return lastCampaignStateCreateInvokeAt;
    }

    private static void markCampaignStateCreateInvokeAt(long atMs) {
        long safeAt = atMs <= 0L ? System.currentTimeMillis() : atMs;
        lastCampaignStateCreateInvokeAt = safeAt;
    }

    private static String checkCampaignStateCreateCooldownWindow() {
        long cooldownMs =
                Math.max(
                        1000L,
                        parseLongProperty(
                                "starsector.autoCampaignCampaignStateCreateCooldownMs", 15000L));
        long lastInvokeAt = readLastCampaignStateCreateInvokeAt();
        if (lastInvokeAt <= 0L) {
            return null;
        }
        long now = System.currentTimeMillis();
        long ageMs = Math.max(0L, now - lastInvokeAt);
        if (ageMs >= cooldownMs) {
            return null;
        }
        long remainingMs = Math.max(0L, cooldownMs - ageMs);
        if (now - autoCampaignCampaignStateCreateCooldownLogAt >= 5000L) {
            System.out.println(
                    "Fixer: direct-new-game campaign-state create cooldown active (remaining="
                            + remainingMs
                            + "ms, age="
                            + ageMs
                            + "ms).");
            autoCampaignCampaignStateCreateCooldownLogAt = now;
        }
        return "direct new-game preflight pending: campaign-state-create-cooldown(" + remainingMs + "ms)";
    }

    private static String checkDirectNewGameCreateSettleWindow() {
        long settleMs =
                Math.max(
                        5000L,
                        parseLongProperty(AUTO_CAMPAIGN_DIRECT_CREATE_SETTLE_MS_PROPERTY, 45000L));
        long lastInvokeAt = readLastDirectNewGameCreateInvokeAt();
        if (lastInvokeAt <= 0L) {
            return null;
        }
        long now = System.currentTimeMillis();
        long ageMs = Math.max(0L, now - lastInvokeAt);
        if (ageMs >= settleMs) {
            return null;
        }
        long remainingMs = Math.max(0L, settleMs - ageMs);
        if (now - autoCampaignDirectCreateSettleLogAt >= 5000L) {
            System.out.println(
                    "Fixer: direct-new-game create settle window active (remaining="
                            + remainingMs
                            + "ms, age="
                            + ageMs
                            + "ms).");
            autoCampaignDirectCreateSettleLogAt = now;
        }
        return "direct new-game preflight pending: waiting-for-create-settle(" + remainingMs + "ms)";
    }

    private static String checkDirectNewGameCreateLeaseWindow() {
        long leaseMs =
                Math.max(
                        5000L,
                        parseLongProperty("starsector.autoCampaignInvokeCreateLeaseMs", 180000L));
        long activeAt = parseLongProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_AT_PROPERTY, -1L);
        if (activeAt <= 0L) {
            return null;
        }
        String activeOwner = "";
        try {
            activeOwner = System.getProperty(AUTO_CAMPAIGN_DIRECT_CREATE_LEASE_OWNER_PROPERTY, "");
        } catch (Throwable ignored) {
            activeOwner = "";
        }
        if (activeOwner == null || activeOwner.trim().isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        long ageMs = Math.max(0L, now - activeAt);
        if (ageMs >= leaseMs) {
            return null;
        }
        long remainingMs = Math.max(0L, leaseMs - ageMs);
        if (now - autoCampaignDirectCreateLeaseLogAt >= 5000L) {
            System.out.println(
                    "Fixer: direct-new-game create lease active (owner="
                            + activeOwner
                            + ", remaining="
                            + remainingMs
                            + "ms, age="
                            + ageMs
                            + "ms).");
            autoCampaignDirectCreateLeaseLogAt = now;
        }
        return "direct new-game preflight pending: invoke-create lease active(" + remainingMs + "ms)";
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
        long readinessCheckTimeoutMs =
                Math.max(
                        250L,
                        Math.min(
                                5000L,
                                parseLongProperty(
                                        "starsector.autoCampaignRuntimeReadinessCheckTimeoutMs",
                                        1500L)));
        long deadline = System.currentTimeMillis() + maxWaitMs;
        String lastIssue = null;
        while (System.currentTimeMillis() <= deadline) {
            lastIssue = checkDirectNewGameRuntimeReadinessWithTimeout(readinessCheckTimeoutMs);
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

    private static String checkDirectNewGameRuntimeReadinessWithTimeout(long timeoutMs) {
        final String[] resultHolder = new String[1];
        final Throwable[] errorHolder = new Throwable[1];
        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    resultHolder[0] = checkDirectNewGameRuntimeReadiness();
                                } catch (Throwable t) {
                                    errorHolder[0] = t;
                                }
                            }
                        },
                        "FixerRuntimeReadinessCheck");
        worker.setDaemon(true);
        long waitMs = Math.max(250L, timeoutMs);
        try {
            worker.start();
            worker.join(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "interrupted";
        } catch (Throwable t) {
            return "runtime-readiness-wrapper-failure(" + describeThrowableChain(t) + ")";
        }
        if (worker.isAlive()) {
            try {
                worker.interrupt();
            } catch (Throwable ignored) {
            }
            return "timeout:runtime-readiness-check(" + waitMs + "ms)";
        }
        if (errorHolder[0] != null) {
            return "runtime-readiness-exception(" + describeThrowableChain(errorHolder[0]) + ")";
        }
        return resultHolder[0];
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
                Object recoveredFleet = tryRecoverPlayerFleetFromSectorEntities(sector);
                if (recoveredFleet != null) {
                    playerFleet = recoveredFleet;
                }
            }
            if (playerFleet == null) {
                boolean allowPlayerFleetNullPreInvoke =
                        Boolean.parseBoolean(
                                System.getProperty(
                                        "starsector.autoCampaignAllowPlayerFleetNullPreInvoke",
                                        "true"));
                if (allowPlayerFleetNullPreInvoke) {
                    System.out.println(
                            "Fixer: direct-new-game readiness allowing player-fleet-null pre-invoke (set -Dstarsector.autoCampaignAllowPlayerFleetNullPreInvoke=false to enforce).");
                    return null;
                }
                return "player-fleet-null";
            }

            String worldPopulationIssue = checkCampaignWorldPopulationForPlayerFleetNullTransition(null);
            if (worldPopulationIssue != null) {
                return worldPopulationIssue;
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
                Object recoveredFleet = tryRecoverPlayerFleetFromSectorEntities(sector);
                if (recoveredFleet != null) {
                    playerFleet = recoveredFleet;
                }
            }
            if (playerFleet == null) {
                return "player-fleet-null";
            }
            return null;
        } catch (Throwable t) {
            return "transition-readiness-exception: " + describeThrowableChain(t);
        }
    }

    private static String checkCampaignStateTransitionReadinessWithTimeout(
            final DriverContext ctx, long timeoutMs) {
        final String[] resultHolder = new String[1];
        final Throwable[] errorHolder = new Throwable[1];
        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    resultHolder[0] = checkCampaignStateTransitionReadiness(ctx);
                                } catch (Throwable t) {
                                    errorHolder[0] = t;
                                }
                            }
                        },
                        "FixerTransitionReadinessCheck");
        worker.setDaemon(true);
        long waitMs = Math.max(250L, timeoutMs);
        try {
            worker.start();
            worker.join(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "interrupted";
        } catch (Throwable t) {
            return "transition-readiness-wrapper-failure(" + describeThrowableChain(t) + ")";
        }
        if (worker.isAlive()) {
            try {
                worker.interrupt();
            } catch (Throwable ignored) {
            }
            return "timeout:campaign-transition-readiness(" + waitMs + "ms)";
        }
        if (errorHolder[0] != null) {
            return "transition-readiness-exception(" + describeThrowableChain(errorHolder[0]) + ")";
        }
        return resultHolder[0];
    }

    private static boolean shouldAllowImmediatePlayerFleetNullTransition(
            boolean allowImmediatePlayerFleetNullTransition, DriverContext ctx) {
        if (!allowImmediatePlayerFleetNullTransition) {
            autoCampaignPlayerFleetNullTransitionDeferredSince = 0L;
            return false;
        }
        boolean requireWorldPopulationGate =
                Boolean.parseBoolean(
                        System.getProperty(
                                "starsector.autoCampaignPlayerFleetNullRequireWorldPopulation",
                                "true"));
        if (!requireWorldPopulationGate) {
            long now = System.currentTimeMillis();
            if (autoCampaignPlayerFleetNullTransitionDeferredSince <= 0L) {
                autoCampaignPlayerFleetNullTransitionDeferredSince = now;
            }
            long minDeferMs =
                    Math.max(
                            2000L,
                            parseLongProperty(
                                    "starsector.autoCampaignPlayerFleetNullMinDeferMs", 5000L));
            long deferredFor = Math.max(0L, now - autoCampaignPlayerFleetNullTransitionDeferredSince);
            if (deferredFor < minDeferMs) {
                if (now - autoCampaignPlayerFleetNullTransitionDeferredLogAt >= 5000L) {
                    System.out.println(
                            "Fixer: waiting before immediate Campaign State transition for player-fleet-null (deferredFor="
                                    + deferredFor
                                    + "ms, minDefer="
                                    + minDeferMs
                                    + "ms).");
                    autoCampaignPlayerFleetNullTransitionDeferredLogAt = now;
                }
                return false;
            }
            if (now - autoCampaignPlayerFleetNullTransitionDeferredLogAt >= 5000L) {
                System.out.println(
                        "Fixer: allowing immediate Campaign State transition for player-fleet-null after deferredFor="
                                + deferredFor
                                + "ms (world-population gate disabled).");
                autoCampaignPlayerFleetNullTransitionDeferredLogAt = now;
            }
            return true;
        }
        String gateIssue = checkCampaignWorldPopulationForPlayerFleetNullTransition(ctx);
        if (gateIssue == null) {
            autoCampaignPlayerFleetNullTransitionDeferredSince = 0L;
            return true;
        }
        long now = System.currentTimeMillis();
        if (autoCampaignPlayerFleetNullTransitionDeferredSince <= 0L) {
            autoCampaignPlayerFleetNullTransitionDeferredSince = now;
        }
        long forceAfterMs =
                Math.max(
                        30000L,
                        parseLongProperty(
                                "starsector.autoCampaignPlayerFleetNullTransitionForceAfterMs",
                                120000L));
        long deferredFor =
                Math.max(0L, now - autoCampaignPlayerFleetNullTransitionDeferredSince);
        if (deferredFor >= forceAfterMs) {
            if (now - autoCampaignPlayerFleetNullTransitionDeferredLogAt >= 5000L) {
                System.out.println(
                        "Fixer: forcing immediate Campaign State transition for player-fleet-null after prolonged deferral (deferredFor="
                                + deferredFor
                                + "ms, gate="
                                + gateIssue
                                + ").");
                autoCampaignPlayerFleetNullTransitionDeferredLogAt = now;
            }
            return true;
        }
        if (now - autoCampaignPlayerFleetNullTransitionDeferredLogAt >= 5000L) {
            System.out.println(
                    "Fixer: deferring immediate Campaign State transition for player-fleet-null until campaign world is populated: "
                            + gateIssue
                            + " (deferredFor="
                            + deferredFor
                            + "ms).");
            autoCampaignPlayerFleetNullTransitionDeferredLogAt = now;
        }
        return false;
    }

    private static String checkCampaignWorldPopulationForPlayerFleetNullTransition(DriverContext ctx) {
        try {
            Object sector = null;
            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method getSector = findMethodRecursive(globalClass, "getSector");
            if (getSector != null) {
                getSector.setAccessible(true);
                sector = getSector.invoke(null);
            }
            if (sector == null && ctx != null) {
                Object campaignState = resolveCampaignStateFromDriver(ctx);
                if (campaignState != null) {
                    sector = invokeNoArgIfPresent(campaignState, "getSector");
                }
            }
            if (sector == null) {
                return "sector-null";
            }
            Object economy = null;
            List markets = null;
            try {
                Method getEconomy = findMethodRecursive(sector.getClass(), "getEconomy");
                if (getEconomy != null) {
                    getEconomy.setAccessible(true);
                    economy = getEconomy.invoke(sector);
                    if (economy != null) {
                        Method getMarketsCopy =
                                findMethodRecursive(economy.getClass(), "getMarketsCopy");
                        if (getMarketsCopy != null) {
                            getMarketsCopy.setAccessible(true);
                            markets = coerceToList(getMarketsCopy.invoke(economy));
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            int economyMarkets = countEconomyMarketsForDiagnostics(sector);
            int sectorEntities = collectSectorEntities(sector).size();
            if (economyMarkets <= 0 && sectorEntities <= 0 && economy != null) {
                boolean bootstrapped = tryBootstrapSectorGenForNoMarkets(sector, economy, markets);
                if (bootstrapped) {
                    economyMarkets = countEconomyMarketsForDiagnostics(sector);
                    sectorEntities = collectSectorEntities(sector).size();
                }
            }
            if (economyMarkets > 0) {
                return null;
            }
            return "campaign-world-empty(economyMarkets="
                    + economyMarkets
                    + ", sectorEntities="
                    + sectorEntities
                    + ")";
        } catch (Throwable t) {
            return "campaign-world-check-exception: " + describeThrowableChain(t);
        }
    }

    private static int countEconomyMarketsForDiagnostics(Object sector) {
        if (sector == null) {
            return -1;
        }
        try {
            Method getEconomy = findMethodRecursive(sector.getClass(), "getEconomy");
            if (getEconomy == null) {
                return -1;
            }
            getEconomy.setAccessible(true);
            Object economy = getEconomy.invoke(sector);
            if (economy == null) {
                return 0;
            }
            Method getMarketsCopy = findMethodRecursive(economy.getClass(), "getMarketsCopy");
            if (getMarketsCopy == null) {
                return -1;
            }
            getMarketsCopy.setAccessible(true);
            Object marketsObj = getMarketsCopy.invoke(economy);
            if (marketsObj instanceof Collection) {
                return ((Collection) marketsObj).size();
            }
            if (marketsObj != null && marketsObj.getClass().isArray()) {
                return Array.getLength(marketsObj);
            }
            if (marketsObj instanceof Iterable) {
                int count = 0;
                Iterator it = ((Iterable) marketsObj).iterator();
                while (it.hasNext()) {
                    it.next();
                    count++;
                }
                return count;
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static boolean isImmediateCampaignTransitionReadinessIssue(String issue) {
        if (issue == null) {
            return false;
        }
        String lowered = issue.toLowerCase();
        return lowered.indexOf("sector-economy-null") >= 0
                || lowered.indexOf("timeout:campaign-transition-readiness") >= 0;
    }

    private static boolean shouldForceCampaignTransitionForReadinessIssue(String issue) {
        if (issue == null) {
            return false;
        }
        String lowered = issue.toLowerCase();
        return lowered.indexOf("sector-economy-null") >= 0
                || lowered.indexOf("timeout:campaign-transition-readiness") >= 0;
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
        Object resolvedTitleState = resolveTitleStateForTransition(ctx, titleState);
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
                    forceStateFaderOut(resolvedTitleState);
                    if (!didCampaignTransitionAdvance(ctx, resolvedTitleState, beforeState)) {
                        if (forceCampaignStateActivation(ctx, resolvedTitleState, beforeState)) {
                            System.out.println(
                                    "Fixer: Campaign State transition fallback succeeded via direct state activation after driver.goToState no-advance.");
                            return true;
                        }
                        System.out.println(
                                "Fixer: requested Campaign State via driver.goToState; awaiting asynchronous state advance.");
                        return true;
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
        if (resolvedTitleState == null) {
            return false;
        }
        try {
            Method goToState =
                    findMethodRecursive(resolvedTitleState.getClass(), "goToState", String.class);
            if (goToState == null) {
                return false;
            }
            goToState.setAccessible(true);
            String invokeIssue =
                    invokeMethodWithTimeout(resolvedTitleState, goToState, CAMPAIGN_STATE_ID, 1500L);
            if (invokeIssue != null) {
                System.out.println(
                        "Fixer: titleState.goToState transition attempt issue: " + invokeIssue);
                return false;
            }
            forceStateFaderOut(resolvedTitleState);
            if (!didCampaignTransitionAdvance(ctx, resolvedTitleState, beforeState)) {
                if (forceCampaignStateActivation(ctx, resolvedTitleState, beforeState)) {
                    System.out.println(
                            "Fixer: Campaign State transition fallback succeeded via direct state activation after titleState.goToState no-advance.");
                    return true;
                }
                System.out.println(
                        "Fixer: requested Campaign State via titleState.goToState; awaiting asynchronous state advance.");
                return true;
            }
            System.out.println("Fixer: requested transition to Campaign State (titleState.goToState).");
            return true;
        } catch (Throwable t) {
            System.out.println("Fixer: unable to request Campaign State transition: " + t);
            return false;
        }
    }

    private static boolean forceCampaignStateActivation(
            DriverContext ctx, Object titleState, Object beforeState) {
        Object campaignState = resolveCampaignStateForActivation(ctx, titleState);
        if (campaignState == null) {
            return false;
        }
        if (ctx != null && ctx.driver != null && ctx.driverClass != null) {
            try {
                if (invokeDriverGoToStateWithStateObject(ctx, campaignState)) {
                    forceStateFaderOut(titleState);
                    if (didCampaignTransitionAdvance(ctx, titleState, beforeState)) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
            try {
                Method setStartState =
                        findMethodRecursive(ctx.driverClass, "setStartState", String.class);
                if (setStartState != null) {
                    setStartState.setAccessible(true);
                    setStartState.invoke(ctx.driver, CAMPAIGN_STATE_ID);
                }
                Field currentStateField = findFieldRecursive(ctx.driverClass, "currState");
                if (currentStateField == null) {
                    currentStateField = findFieldRecursive(ctx.driverClass, "currentState");
                }
                if (currentStateField != null) {
                    currentStateField.setAccessible(true);
                    currentStateField.set(ctx.driver, campaignState);
                }
                forceStateFaderOut(titleState);
                if (didCampaignTransitionAdvance(ctx, titleState, beforeState)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static Object resolveCampaignStateForActivation(DriverContext ctx, Object titleState) {
        if (ctx != null && ctx.states != null) {
            try {
                Object mapped = ctx.states.get(CAMPAIGN_STATE_ID);
                if (mapped != null) {
                    return mapped;
                }
            } catch (Throwable ignored) {
            }
        }
        if (ctx != null && ctx.session != null) {
            try {
                Object mapped = ctx.session.get(CAMPAIGN_SESSION_KEY);
                if (mapped != null) {
                    return mapped;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Object titleSession = readFieldRecursive(titleState, "session");
            if (titleSession instanceof Map) {
                Object mapped = ((Map) titleSession).get(CAMPAIGN_SESSION_KEY);
                if (mapped != null) {
                    return mapped;
                }
            }
        } catch (Throwable ignored) {
        }
        return resolveCampaignStateFromDriver(ctx);
    }

    private static boolean invokeDriverGoToStateWithStateObject(
            DriverContext ctx, Object campaignState) {
        if (ctx == null || ctx.driver == null || ctx.driverClass == null || campaignState == null) {
            return false;
        }
        Class<?> campaignClass = campaignState.getClass();
        Class<?> c = ctx.driverClass;
        while (c != null) {
            Method[] methods = c.getDeclaredMethods();
            for (Method m : methods) {
                if (m == null || !"goToState".equals(m.getName())) {
                    continue;
                }
                Class<?>[] params = m.getParameterTypes();
                if (params == null || params.length != 1) {
                    continue;
                }
                if (params[0] == String.class) {
                    continue;
                }
                if (!params[0].isAssignableFrom(campaignClass)) {
                    continue;
                }
                try {
                    m.setAccessible(true);
                    String issue = invokeMethodWithTimeout(ctx.driver, m, campaignState, 1500L);
                    if (issue == null) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static Object resolveTitleStateForTransition(DriverContext ctx, Object titleState) {
        if (isTitleState(null, titleState)) {
            return titleState;
        }
        if (ctx != null && ctx.states != null) {
            try {
                Object mapped = ctx.states.get(TITLE_STATE_ID);
                if (isTitleState(TITLE_STATE_ID, mapped)) {
                    return mapped;
                }
            } catch (Throwable ignored) {
            }
        }
        return titleState;
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

    private static Object invokeNoArgIfPresent(Object target, String name) {
        if (target == null || name == null || name.length() == 0) {
            return null;
        }
        try {
            Method method = findMethodRecursive(target.getClass(), name);
            if (method == null || method.getParameterTypes().length != 0) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void logDirectNewGameDataSnapshot(Object data) {
        if (data == null) {
            System.out.println("Fixer: direct-new-game data snapshot unavailable: data=null");
            return;
        }
        Object charData = invokeNoArgIfPresent(data, "getCharacterData");
        Object person = invokeNoArgIfPresent(charData, "getPerson");
        Object customData = invokeNoArgIfPresent(data, "getCustomData");
        Object scripts = invokeNoArgIfPresent(data, "getScripts");
        Object scriptsBefore = invokeNoArgIfPresent(data, "getScriptsBeforeTimePass");
        Object cargo = invokeNoArgIfPresent(data, "getStartingCargo");
        Object coords = invokeNoArgIfPresent(data, "getStartingCoordinates");
        Object additionalShips = invokeNoArgIfPresent(data, "getAdditionalShips");
        Object startLoc = invokeNoArgIfPresent(data, "getStartingLocationName");
        Object seedString = invokeNoArgIfPresent(data, "getSeedString");
        Object sectorSize = invokeNoArgIfPresent(data, "getSectorSize");
        Object sectorAge = invokeNoArgIfPresent(data, "getSectorAge");
        Object withTimePass = invokeNoArgIfPresent(data, "isWithTimePass");
        Object done = invokeNoArgIfPresent(data, "isDone");

        StringBuilder sb = new StringBuilder();
        sb.append("Fixer: direct-new-game data snapshot class=")
                .append(data.getClass().getName())
                .append(" startLocation=")
                .append(String.valueOf(startLoc))
                .append(" seedString=")
                .append(String.valueOf(seedString))
                .append(" sectorSize=")
                .append(String.valueOf(sectorSize))
                .append(" sectorAge=")
                .append(String.valueOf(sectorAge))
                .append(" withTimePass=")
                .append(String.valueOf(withTimePass))
                .append(" done=")
                .append(String.valueOf(done))
                .append(" charData=")
                .append(charData != null ? charData.getClass().getName() : "null")
                .append(" person=")
                .append(person != null ? person.getClass().getName() : "null")
                .append(" customDataType=")
                .append(customData != null ? customData.getClass().getName() : "null")
                .append(" customDataSize=")
                .append(customData instanceof Map ? ((Map) customData).size() : -1)
                .append(" scriptsSize=")
                .append(scripts instanceof Collection ? ((Collection) scripts).size() : -1)
                .append(" scriptsBeforeSize=")
                .append(scriptsBefore instanceof Collection ? ((Collection) scriptsBefore).size() : -1)
                .append(" additionalShipsSize=")
                .append(additionalShips instanceof Collection ? ((Collection) additionalShips).size() : -1)
                .append(" cargo=")
                .append(cargo != null ? cargo.getClass().getName() : "null")
                .append(" coords=")
                .append(coords != null ? coords.getClass().getName() : "null")
                .append(" coordsX=")
                .append(coords != null ? readFloatField(coords, "x", Float.NaN) : Float.NaN)
                .append(" coordsY=")
                .append(coords != null ? readFloatField(coords, "y", Float.NaN) : Float.NaN);
        System.out.println(sb.toString());
    }

    private static void logDirectNewGameRuntimeSnapshot() {
        try {
            Class<?> campaignEngineClass = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
            Method getInstance = findMethodRecursive(campaignEngineClass, "getInstance");
            if (getInstance == null) {
                System.out.println(
                        "Fixer: direct-new-game runtime snapshot unavailable: CampaignEngine.getInstance missing");
                return;
            }
            getInstance.setAccessible(true);
            Object engine = getInstance.invoke(null);
            if (engine == null) {
                System.out.println("Fixer: direct-new-game runtime snapshot: campaignEngine=null");
                return;
            }
            Object currentLocation = invokeNoArgIfPresent(engine, "getCurrentLocation");
            Object hyperspace = invokeNoArgIfPresent(engine, "getHyperspace");
            Object playerFleet = invokeNoArgIfPresent(engine, "getPlayerFleet");
            Object characterData = invokeNoArgIfPresent(engine, "getCharacterData");
            Object playerPerson = invokeNoArgIfPresent(engine, "getPlayerPerson");
            Object factionManager = invokeNoArgIfPresent(engine, "getFactionManager");
            Object playerFaction = invokeNoArgIfPresent(factionManager, "getPlayerFaction");
            Object economy = invokeNoArgIfPresent(engine, "getEconomy");
            Object clock = invokeNoArgIfPresent(engine, "getClock");
            Object modManager = null;
            Object enabledPlugins = null;
            Object enabledMods = null;
            int enabledPluginsSize = -1;
            int enabledPluginsNulls = -1;
            int enabledModsSize = -1;
            try {
                Class<?> modManagerClass = Class.forName("com.fs.starfarer.settings.ModManager");
                Method getModManagerInstance = findMethodRecursive(modManagerClass, "getInstance");
                if (getModManagerInstance != null) {
                    getModManagerInstance.setAccessible(true);
                    modManager = getModManagerInstance.invoke(null);
                    enabledPlugins = invokeNoArgIfPresent(modManager, "getEnabledModPlugins");
                    enabledMods = invokeNoArgIfPresent(modManager, "getEnabledMods");
                    if (enabledPlugins instanceof Collection) {
                        enabledPluginsSize = ((Collection) enabledPlugins).size();
                        int nulls = 0;
                        for (Object plugin : (Collection) enabledPlugins) {
                            if (plugin == null) {
                                nulls++;
                            }
                        }
                        enabledPluginsNulls = nulls;
                    }
                    if (enabledMods instanceof Collection) {
                        enabledModsSize = ((Collection) enabledMods).size();
                    }
                }
            } catch (Throwable ignored) {
            }
            Object newGameSectorProcGen = null;
            Object newGameCreationEntryPoint = null;
            try {
                Class<?> settingsClass = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
                Method getTyped = findMethodRecursive(settingsClass, "ÓO0000", String.class);
                if (getTyped != null) {
                    getTyped.setAccessible(true);
                    newGameSectorProcGen = getTyped.invoke(null, "newGameSectorProcGen");
                    newGameCreationEntryPoint = getTyped.invoke(null, "newGameCreationEntryPoint");
                }
            } catch (Throwable ignored) {
            }
            String sectorGenProbe;
            try {
                Class<?> scriptStoreClass = Class.forName("com.fs.starfarer.loading.scripts.ScriptStore");
                Method scriptLoaderGetter = null;
                for (Method m : scriptStoreClass.getDeclaredMethods()) {
                    if (!Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length != 0) {
                        continue;
                    }
                    if (ClassLoader.class.isAssignableFrom(m.getReturnType())) {
                        scriptLoaderGetter = m;
                        break;
                    }
                }
                ClassLoader scriptLoader = null;
                if (scriptLoaderGetter != null) {
                    scriptLoaderGetter.setAccessible(true);
                    Object loaderObj = scriptLoaderGetter.invoke(null);
                    if (loaderObj instanceof ClassLoader) {
                        scriptLoader = (ClassLoader) loaderObj;
                    }
                }
                if (scriptLoader == null) {
                    sectorGenProbe = "script-loader-null";
                } else {
                    try {
                        Class<?> sectorGenClass =
                                Class.forName("data.scripts.world.SectorGen", true, scriptLoader);
                        sectorGenProbe = "loaded:" + sectorGenClass.getName();
                    } catch (Throwable t) {
                        sectorGenProbe = "load-failed:" + describeThrowableChain(t);
                    }
                }
            } catch (Throwable t) {
                sectorGenProbe = "probe-failed:" + describeThrowableChain(t);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Fixer: direct-new-game runtime snapshot engine=")
                    .append(engine.getClass().getName())
                    .append(" currentLocation=")
                    .append(currentLocation != null ? currentLocation.getClass().getName() : "null")
                    .append(" hyperspace=")
                    .append(hyperspace != null ? hyperspace.getClass().getName() : "null")
                    .append(" playerFleet=")
                    .append(playerFleet != null ? playerFleet.getClass().getName() : "null")
                    .append(" characterData=")
                    .append(characterData != null ? characterData.getClass().getName() : "null")
                    .append(" playerPerson=")
                    .append(playerPerson != null ? playerPerson.getClass().getName() : "null")
                    .append(" factionManager=")
                    .append(factionManager != null ? factionManager.getClass().getName() : "null")
                    .append(" playerFaction=")
                    .append(playerFaction != null ? playerFaction.getClass().getName() : "null")
                    .append(" economy=")
                    .append(economy != null ? economy.getClass().getName() : "null")
                    .append(" clock=")
                    .append(clock != null ? clock.getClass().getName() : "null")
                    .append(" modManager=")
                    .append(modManager != null ? modManager.getClass().getName() : "null")
                    .append(" enabledPluginsSize=")
                    .append(enabledPluginsSize)
                    .append(" enabledPluginsNulls=")
                    .append(enabledPluginsNulls)
                    .append(" enabledModsSize=")
                    .append(enabledModsSize)
                    .append(" newGameSectorProcGen=")
                    .append(newGameSectorProcGen != null ? newGameSectorProcGen.getClass().getName() : "null")
                    .append(" newGameCreationEntryPoint=")
                    .append(
                            newGameCreationEntryPoint != null
                                    ? newGameCreationEntryPoint.getClass().getName()
                                    : "null")
                    .append(" sectorGenProbe=")
                    .append(sectorGenProbe);
            System.out.println(sb.toString());
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: direct-new-game runtime snapshot failed: " + describeThrowableChain(t));
        }
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

    private static String normalizePathLikeValue(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replace('\\', '/');
        if (normalized.length() == 0) {
            return null;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/.") && normalized.length() > 2) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return normalized.length() == 0 ? null : normalized;
    }

    private static String deriveUserDirFromClassPath() {
        String classPath = System.getProperty("java.class.path", "");
        if (classPath == null || classPath.length() == 0) {
            return null;
        }
        String[] entries = classPath.split(File.pathSeparator);
        for (String entry : entries) {
            String normalized = normalizePathLikeValue(entry);
            if (normalized == null) {
                continue;
            }
            int jarSegment = normalized.indexOf("/jars/");
            if (jarSegment <= 0) {
                continue;
            }
            String appPrefix = normalizePathLikeValue(normalized.substring(0, jarSegment));
            if (appPrefix == null) {
                continue;
            }
            return appPrefix + "/starsector/starsector";
        }
        return null;
    }

    private static String resolveRuntimeUserDir() {
        String fromOverride = normalizePathLikeValue(System.getProperty(USER_DIR_OVERRIDE_PROPERTY));
        if (fromOverride != null) {
            return fromOverride;
        }
        String fromClassPath = normalizePathLikeValue(deriveUserDirFromClassPath());
        if (fromClassPath != null) {
            return fromClassPath;
        }
        String fromUserDir = normalizePathLikeValue(System.getProperty("user.dir"));
        if (fromUserDir != null && !"/files".equals(fromUserDir) && !"/app".equals(fromUserDir)) {
            return fromUserDir;
        }
        return APP_ROOT;
    }

    private static String[] buildResourceManagerRoots() {
        LinkedHashSet<String> roots = new LinkedHashSet<String>();
        // Prefer runtime-derived roots so web subpath deployments (e.g. /starsectorquick/)
        // do not silently fall back to stale hardcoded paths.
        addResourceManagerRootVariants(roots, System.getProperty("starsector.userDir"));
        addResourceManagerRootVariants(roots, System.getProperty("user.dir"));
        addResourceManagerRootVariants(roots, System.getProperty("starsector.contentRoot"));

        roots.add("/app/starsector/starsector/.");
        roots.add("/app/starsector/starsector");
        roots.add("/app/starsector/starsector/");
        roots.add("/files");
        roots.add("../starfarer.res/res");
        roots.add("/starfarer.res/res");
        return roots.toArray(new String[roots.size()]);
    }

    private static void addResourceManagerRootVariants(Set<String> out, String rawRoot) {
        if (out == null || rawRoot == null) {
            return;
        }
        String normalized = rawRoot.trim().replace('\\', '/');
        if (normalized.length() == 0) {
            return;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/.")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        if (normalized.length() == 0) {
            return;
        }
        out.add(normalized + "/.");
        out.add(normalized);
        out.add(normalized + "/");
    }

    private static void initializeResourceManager() {
        final String[] roots = buildResourceManagerRoots();

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
            Set<String> supportRoots = new LinkedHashSet<String>();
            supportRoots.add("data/strings");
            supportRoots.add("data/config");
            supportRoots.add("data/world");
            supportRoots.add("data/campaign");

            Set<String> files = new LinkedHashSet<String>();
            Set<String> seenDirs = new HashSet<String>();
            for (String root : roots) {
                collectSpecFilesFromIndex(root, seenDirs, files, false);
            }
            for (String root : supportRoots) {
                collectSpecFilesFromIndex(root, seenDirs, files, true);
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
        collectSpecFilesFromIndex(dir, seenDirs, outFiles, false);
    }

    private static void collectSpecFilesFromIndex(
            String dir, Set<String> seenDirs, Set<String> outFiles, boolean includeAllFiles)
            throws Exception {
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
                if (includeAllFiles
                        || "wpn".equals(ext)
                        || "proj".equals(ext)
                        || "ship".equals(ext)
                        || "system".equals(ext)
                        || "skin".equals(ext)
                        || "variant".equals(ext)
                        || "skill".equals(ext)) {
                    outFiles.add(rel);
                }
            } else {
                collectSpecFilesFromIndex(rel, seenDirs, outFiles, includeAllFiles);
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
