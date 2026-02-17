const fs = require('fs');
const { chromium } = require('playwright');

(async () => {
  const serial = process.env.STARSECTOR_SERIAL || '';
  const colonyName = process.env.STARSECTOR_COLONY_NAME || 'Jangala';
  const variant = process.env.STARSECTOR_START_VARIANT || 'lasher_Standard';
  const autoMode =
    process.env.STARSECTOR_AUTO_MODE || 'continue_then_new_visit_colony';
  const modeText = String(autoMode || '').toLowerCase();
  const allowNewGameInput = modeText.indexOf('new') >= 0;
  const passiveInput =
    String(process.env.STARSECTOR_PASSIVE_INPUT || 'true').trim().toLowerCase() !== 'false';
  const disableTitleMenuClicks =
    String(process.env.STARSECTOR_DISABLE_TITLE_MENU_CLICKS || 'true')
      .trim()
      .toLowerCase() !== 'false';
  const enableRetryBurst =
    String(process.env.STARSECTOR_ENABLE_RETRY_BURST || 'false').trim().toLowerCase() === 'true';
  const readEnvNumber = (name, fallback) => {
    const raw = process.env[name];
    if (raw == null) return fallback;
    const trimmed = String(raw).trim();
    if (!trimmed.length) return fallback;
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : fallback;
  };
  const activeInputEveryLoops =
    Math.max(2, readEnvNumber('STARSECTOR_ACTIVE_INPUT_EVERY_LOOPS', 6));
  const forceLauncherSweep =
    String(process.env.STARSECTOR_FORCE_LAUNCHER_SWEEP || 'true').trim().toLowerCase() !== 'false';
  const disableLauncherInput =
    String(process.env.STARSECTOR_DISABLE_LAUNCHER_INPUT || 'false').trim().toLowerCase() === 'true';
  const launcherSweepLimitWhenDisabled = Math.max(
    0,
    readEnvNumber('STARSECTOR_LAUNCHER_SWEEP_LIMIT_WHEN_DISABLED', 2)
  );
  const directLaunch =
    String(process.env.STARSECTOR_DIRECT_LAUNCH || 'true').trim().toLowerCase() === 'true';
  const useLoadingPatch =
    String(process.env.STARSECTOR_USE_LOADING_PATCH || 'false').trim().toLowerCase() === 'true';
  const preloadSpecFiles =
    String(process.env.STARSECTOR_PRELOAD_SPEC_FILES || 'false').trim().toLowerCase() === 'true';
  const disableShipHullPostPass =
    String(process.env.STARSECTOR_DISABLE_SHIP_HULL_POST_PASS || 'false')
      .trim()
      .toLowerCase() === 'true';
  const headless =
    String(process.env.STARSECTOR_HEADLESS || 'true').trim().toLowerCase() !== 'false';
  const disableScreenshots =
    String(process.env.STARSECTOR_DISABLE_SCREENSHOTS || 'false').trim().toLowerCase() === 'true';
  const lwjglModule = (process.env.STARSECTOR_LWJGL_MODULE || 'lwjgl_clean.js').trim() || 'lwjgl_clean.js';
  const bootMode = (process.env.STARSECTOR_BOOT || 'launcher').trim().toLowerCase() || 'launcher';
  const autoCampaignSectorSize =
    (process.env.STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE || 'small').trim().toLowerCase() || 'small';
  const autoCampaignTitleSettleMs = Math.max(
    0,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_TITLE_SETTLE_MS', 25000)
  );
  const fixerAutoCampaignEnabled =
    String(process.env.STARSECTOR_FIXER_AUTO_CAMPAIGN || 'true').trim().toLowerCase() !== 'false';
  const autoCampaignAllowDirectEscape =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_ALLOW_DIRECT_ESCAPE || 'false')
      .trim()
      .toLowerCase() === 'true';
  const autoCampaignTitleRenderWarmup =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_TITLE_RENDER_WARMUP || 'false')
      .trim()
      .toLowerCase() === 'true';
  const autoCampaignFallbackMs = Math.max(
    1500,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_FALLBACK_MS', 7000)
  );
  const autoCampaignTimeoutMs = Math.max(
    60000,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_TIMEOUT_MS', 900000)
  );
  const autoCampaignDirectAttemptTimeoutMs = Math.max(
    2000,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_DIRECT_ATTEMPT_TIMEOUT_MS', 20000)
  );
  const autoCampaignDirectPostInitWaitMs = Math.max(
    8000,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_DIRECT_POST_INIT_WAIT_MS', 45000)
  );
  const autoCampaignDirectTimeoutFailureLimit = Math.max(
    1,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_DIRECT_TIMEOUT_FAILURE_LIMIT', 3)
  );
  const autoCampaignCoreSpecBaselineNonMutatingWaitMs = Math.max(
    15000,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_CORE_SPEC_NON_MUTATING_WAIT_MS', 45000)
  );
  const sectorInitHoldWindowMs = Math.max(
    20000,
    readEnvNumber('STARSECTOR_SECTOR_INIT_HOLD_WINDOW_MS', 180000)
  );
  const directInitHoldWindowMs = Math.max(
    10000,
    readEnvNumber('STARSECTOR_DIRECT_INIT_HOLD_WINDOW_MS', 120000)
  );
  const campaignTransitionCooldownMs = Math.max(
    12000,
    readEnvNumber('STARSECTOR_CAMPAIGN_TRANSITION_COOLDOWN_MS', 120000)
  );
  const launcherPlayRetryMs = Math.max(
    10000,
    readEnvNumber('STARSECTOR_LAUNCHER_PLAY_RETRY_MS', 120000)
  );
  const autoCampaignMutatingSpecPreflight =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_MUTATING_SPEC_PREFLIGHT || 'true')
      .trim()
      .toLowerCase() === 'true';
  const autoCampaignSkipFactionPreflight =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_SKIP_FACTION_PREFLIGHT || 'false')
      .trim()
      .toLowerCase() !== 'false';
  const autoCampaignWithTimePass =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_WITH_TIME_PASS || 'true')
      .trim()
      .toLowerCase() !== 'false';
  const autoCampaignInvokeCreateOnPlayerFleetNull =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_INVOKE_CREATE_ON_PLAYER_FLEET_NULL || 'false')
      .trim()
      .toLowerCase() === 'true';
  const autoCampaignInvokeCreateInCampaignState =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_INVOKE_CREATE_IN_CAMPAIGN_STATE || 'false')
      .trim()
      .toLowerCase() === 'true';
  const autoCampaignEnableSyntheticPlayerFleetFallback =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_ENABLE_SYNTHETIC_PLAYER_FLEET_FALLBACK || 'true')
      .trim()
      .toLowerCase() === 'true';
  const autoCampaignAllowDialogWithoutPlayerFleet =
    String(process.env.STARSECTOR_AUTO_CAMPAIGN_ALLOW_DIALOG_WITHOUT_PLAYER_FLEET || 'true')
      .trim()
      .toLowerCase() === 'true';
  const autoCampaignPlayerFleetNullTransitionForceAfterMs = Math.max(
    30000,
    readEnvNumber('STARSECTOR_AUTO_CAMPAIGN_PLAYER_FLEET_NULL_FORCE_AFTER_MS', 120000)
  );
  const useNpeFixLib =
    String(process.env.STARSECTOR_USE_NPE_FIX_LIB || 'false').trim().toLowerCase() === 'true';
  const avoidPostInitKeys =
    String(process.env.STARSECTOR_AVOID_POST_INIT_KEYS || 'true').trim().toLowerCase() !== 'false';
  const warmupLoops = Math.max(1, readEnvNumber('STARSECTOR_WARMUP_LOOPS', 24));
  const maxLoops = Math.max(1, readEnvNumber('STARSECTOR_MAX_LOOPS', 480));
  const loopDelayMs = Math.max(250, readEnvNumber('STARSECTOR_LOOP_DELAY_MS', 2200));
  const initFreezeRecoveryTicks = Math.max(
    8,
    readEnvNumber('STARSECTOR_INIT_FREEZE_RECOVERY_TICKS', 30)
  );
  const maxInitFreezeRecoveries = Math.max(
    0,
    readEnvNumber('STARSECTOR_MAX_INIT_FREEZE_RECOVERIES', 2)
  );
  const hardTimeoutMs =
    Math.max(
      60000,
      readEnvNumber('STARSECTOR_HARD_TIMEOUT_MS', 26 * 60 * 1000)
    );
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const out = `tmp_visit_colony_autowatcher_v19_${stamp}.json`;
  const shots = [];
  const events = [];
  let browser;
  let lastState = null;
  let persisted = false;

  let campaignSeen = false;
  let colonyPrimedSeen = false;
  let dialogRequestSeen = false;
  let colonyVisitLikely = false;
  let successAt = 0;
  let loadGameMisrouteSeen = false;
  let titleStateSeen = false;
  let directNewGameInitSeen = false;
  let generatingSectorSeen = false;
  let directNewGameInitSeenAt = 0;
  let generatingSectorSeenAt = 0;
  let campaignSeenAt = 0;
  let colonyPrimedAt = 0;
  let dialogRequestAt = 0;
  let directNewGameFailCount = 0;
  let inputCycles = 0;
  let loadingBarStuckTicks = 0;
  let loadingBarRetryBurstDone = false;
  let initFreezeTicks = 0;
  let blankRuntimeTicks = 0;
  let initFreezeRecoveries = 0;
  let launcherKickoffSweeps = 0;
  let launcherPlayIssued = false;
  let launcherPlayIssuedAt = 0;
  let lastSwapCount = -1;
  let lastDrawCount = -1;
  let lastCampaignTransitionRequestAt = 0;
  const actionCounts = {};
  const netIssueCounts = Object.create(null);

  const persistResult = (extra = {}) => {
    if (persisted) return;
    persisted = true;
    try {
      fs.writeFileSync(
        out,
        JSON.stringify(
          {
            shots,
            events,
            campaignSeen,
            colonyPrimedSeen,
            dialogRequestSeen,
            colonyVisitLikely,
            loadGameMisrouteSeen,
            titleStateSeen,
            directNewGameFailCount,
            campaignSeenAt,
            colonyPrimedAt,
            dialogRequestAt,
            finalStatus: lastState?.status || null,
            finalNative: lastState?.native || null,
            colonyName,
            variant,
            autoMode,
            headless,
            directLaunch,
            useLoadingPatch,
            disableShipHullPostPass,
            lwjglModule,
            bootMode,
            autoCampaignTitleSettleMs,
            autoCampaignSectorSize,
            autoCampaignDirectAttemptTimeoutMs,
            autoCampaignDirectPostInitWaitMs,
            autoCampaignDirectTimeoutFailureLimit,
            autoCampaignCoreSpecBaselineNonMutatingWaitMs,
            inputCycles,
            loadingBarStuckTicks,
            initFreezeTicks,
            blankRuntimeTicks,
            initFreezeRecoveries,
            launcherKickoffSweeps,
            launcherPlayIssued,
            launcherPlayIssuedAt,
            actionCounts,
            fixerAutoCampaignEnabled,
            autoCampaignAllowDirectEscape,
            autoCampaignTitleRenderWarmup,
            disableTitleMenuClicks,
            enableRetryBurst,
            autoCampaignMutatingSpecPreflight,
            autoCampaignSkipFactionPreflight,
            autoCampaignWithTimePass,
            autoCampaignInvokeCreateOnPlayerFleetNull,
            autoCampaignInvokeCreateInCampaignState,
            autoCampaignEnableSyntheticPlayerFleetFallback,
            autoCampaignAllowDialogWithoutPlayerFleet,
            autoCampaignPlayerFleetNullTransitionForceAfterMs,
            activeInputEveryLoops,
            forceLauncherSweep,
            disableLauncherInput,
            launcherSweepLimitWhenDisabled,
            initFreezeRecoveryTicks,
            maxInitFreezeRecoveries,
            ...extra,
          },
          null,
          2
        )
      );
    } catch {}
  };

  const hardStop = setTimeout(() => {
    try {
      persistResult({ error: 'hard-timeout', hardTimeoutReached: true, reason: 'hard-timeout' });
    } catch {}
    process.exit(2);
  }, hardTimeoutMs);

  process.on('SIGINT', () => {
    persistResult({ error: 'terminated', reason: 'sigint' });
    process.exit(130);
  });
  process.on('SIGTERM', () => {
    persistResult({ error: 'terminated', reason: 'sigterm' });
    process.exit(143);
  });

  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  const countAction = (name) => {
    actionCounts[name] = (actionCounts[name] || 0) + 1;
  };

  const maybeLogAction = (action) => {
    const n = actionCounts[action] || 0;
    if (n <= 3 || n % 20 === 0) {
      console.log(`[input] action=${action} count=${n}`);
    }
  };

  const trackNetIssue = (type, url, detail) => {
    const safeUrl = String(url || '');
    if (!safeUrl) return;
    const key = `${type}:${safeUrl}`;
    const count = (netIssueCounts[key] || 0) + 1;
    netIssueCounts[key] = count;
    if (count <= 3 || count % 20 === 0) {
      events.push({
        ts: new Date().toISOString(),
        type,
        url: safeUrl,
        detail: detail == null ? null : String(detail),
        count,
      });
    }
  };

  const updateMilestonesFromText = (text) => {
    if (!text) return;
    const t = String(text);

    if (
      /Fixer: auto campaign watcher state=Campaign State|Fixer: auto campaign watcher reached Campaign State/i.test(
        t
      )
    ) {
      if (!campaignSeenAt) campaignSeenAt = Date.now();
      campaignSeen = true;
      lastCampaignTransitionRequestAt = 0;
    }
    if (/Fixer: auto campaign watcher state=Title Screen State/i.test(t)) {
      titleStateSeen = true;
    }
    if (
      /Fixer: direct new-game init succeeded|Fixer: direct-new-game stage=invoke-create-return|Fixer: auto campaign data prepared startVariant=|Fixer: direct new-game runtime readiness still sector-null|Fixer: direct new-game returned without campaign readiness/i.test(
        t
      )
    ) {
      directNewGameInitSeen = true;
      directNewGameInitSeenAt = Date.now();
    }
    if (/Generating sector/i.test(t)) {
      generatingSectorSeen = true;
      generatingSectorSeenAt = Date.now();
    }
    if (
      /Fixer: auto campaign colony target primed market=|Fixer: auto campaign watcher primed colony visit target/i.test(
        t
      )
    ) {
      if (!colonyPrimedAt) colonyPrimedAt = Date.now();
      colonyPrimedSeen = true;
      colonyVisitLikely = true;
    }
    if (/Fixer: auto campaign requested colony interaction dialog result=/i.test(t)) {
      if (!dialogRequestAt) dialogRequestAt = Date.now();
      dialogRequestSeen = true;
      colonyVisitLikely = true;
    }
    if (!successAt && (dialogRequestSeen || colonyPrimedSeen || colonyVisitLikely)) {
      successAt = Date.now();
      events.push({
        ts: new Date().toISOString(),
        type: 'success_gate',
        source: 'console-log',
        milestones: { campaignSeen, colonyPrimedSeen, dialogRequestSeen, colonyVisitLikely },
      });
    }
    if (/LoadGameDialog|showLoadGameDialog/i.test(t)) {
      loadGameMisrouteSeen = true;
    }
    if (/Fixer: direct new-game init failed/i.test(t)) {
      directNewGameFailCount += 1;
    }
    if (
      /Fixer: requested transition to Campaign State|Fixer: requested Campaign State via .*goToState|Fixer: auto campaign .*Campaign State transition|Fixer: auto campaign direct new-game transition retry pending: goToState unavailable/i.test(
        t
      )
    ) {
      lastCampaignTransitionRequestAt = Date.now();
    }
  };

  const shouldHoldDuringSectorInit = (state) => {
    const now = Date.now();
    const recentDirectInit =
      directNewGameInitSeenAt > 0 && now - directNewGameInitSeenAt <= directInitHoldWindowMs;
    const recentGenerating =
      generatingSectorSeenAt > 0 && now - generatingSectorSeenAt <= sectorInitHoldWindowMs;
    // Title-state markers can remain stale while direct new-game init is already in progress.
    // Hold input whenever direct-init/generation is recent to avoid fighting campaign transition.
    const holdSignal = state?.hasGeneratingSector || recentGenerating || recentDirectInit;
    return (
      !campaignSeen &&
      holdSignal &&
      !state?.hasLauncherPlay &&
      !state?.hasExitPrompt &&
      !state?.hasPreloading
    );
  };

  const withTimeout = async (promise, ms, tag) => {
    let to;
    try {
      return await Promise.race([
        promise,
        new Promise((_, reject) => {
          to = setTimeout(() => reject(new Error(`timeout:${tag}:${ms}ms`)), ms);
        }),
      ]);
    } finally {
      if (to) clearTimeout(to);
    }
  };

  const readState = async (page) => {
    try {
      const state = await withTimeout(
        page.evaluate(() => {
          const logTail = Array.from(document.querySelectorAll('#log > div'))
            .slice(-80)
            .map((e) => e.textContent || '');
          const canvas =
            document.querySelector('#lwjglCanvas') ||
            document.querySelector('#game-container canvas') ||
            document.querySelector('canvas');
          const stats = window.__lwjglNativeStats || {};
          const c = stats.callsByName || {};
          const r = canvas ? canvas.getBoundingClientRect() : null;
          const status = document.getElementById('cheerpjDisplay')?.getAttribute('data-status') || null;
          const bodyText = (document.body && document.body.innerText) || '';
          const logText = (document.getElementById('log') && document.getElementById('log').innerText) || '';
          // Exclude debug log text to avoid false-positive UI state detection (e.g. "Spec preloading ...").
          const uiText = logText ? bodyText.replace(logText, '') : bodyText;
          const hasPreloading = /preload|initializing runtime|class is loaded/i.test(String(status || ''));
          const hasLauncherPlay = /play\\s*starsector/i.test(uiText);
          const hasExitPrompt = /exit game\\?/i.test(uiText);
          const hasMemoryWarning =
            /memory recommended for the enabled mod set exceeds|memory related issues/i.test(uiText);
          const hasGeneratingSector = /generating\s+sector/i.test(uiText);
          return {
            status,
            logTail,
            hasCanvas: !!canvas,
            hasPreloading,
            hasGeneratingSector,
            hasLauncherPlay,
            hasExitPrompt,
            hasMemoryWarning,
            canvasSize: canvas ? { w: canvas.width || null, h: canvas.height || null } : null,
            canvasRect: r
              ? {
                  x: r.left,
                  y: r.top,
                  w: r.width,
                  h: r.height,
                }
              : null,
            native: {
              totalCalls: stats.totalCalls || 0,
              swap: c.Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers || 0,
              begin: c.Java_org_lwjgl_opengl_GL11_nglBegin || 0,
              draw: c.Java_org_lwjgl_opengl_GL11_nglDrawArrays || 0,
              texImage: c.Java_org_lwjgl_opengl_GL11_nglTexImage2D || 0,
              keyPoll: c.Java_org_lwjgl_input_Keyboard_poll || 0,
              mousePoll: c.Java_org_lwjgl_input_Mouse_poll || 0,
              lastName: stats.lastName || null,
              lastAt: stats.lastAt || 0,
            },
          };
        }),
        12000,
        'readState'
      );
      for (const line of state.logTail || []) updateMilestonesFromText(line);
      lastState = state;
      return state;
    } catch (e) {
      events.push({ ts: new Date().toISOString(), type: 'read_state_error', text: String(e) });
      return null;
    }
  };

  const mark = async (page, label, extra = {}) => {
    const file = `tmp_visit_colony_autowatcher_v19_${stamp}_${label}.png`;
    if (!disableScreenshots) {
      try {
        await page.screenshot({ path: file, fullPage: true, timeout: 30000 });
        shots.push(file);
      } catch (e) {
        events.push({ ts: new Date().toISOString(), type: 'screenshot_error', label, text: String(e) });
      }
    }
    const state = await readState(page);
    events.push({
      ts: new Date().toISOString(),
      type: 'mark',
      label,
      state,
      milestones: { campaignSeen, colonyPrimedSeen, dialogRequestSeen, colonyVisitLikely },
      loadGameMisrouteSeen,
      directNewGameFailCount,
      inputCycles,
      loadingBarStuckTicks,
      actionCounts: { ...actionCounts },
      ...extra,
    });
  };

  const safeCanvasClick = async (page, state, rx, ry, action) => {
    if (!state || !state.canvasRect || !state.canvasRect.w || !state.canvasRect.h) {
      return false;
    }
    const x = Math.round(state.canvasRect.x + state.canvasRect.w * rx);
    const y = Math.round(state.canvasRect.y + state.canvasRect.h * ry);
    try {
      await withTimeout(page.mouse.click(x, y, { delay: 40 }), 5000, `click:${action}`);
      countAction(action);
      maybeLogAction(action);
      events.push({
        ts: new Date().toISOString(),
        type: 'input:click',
        action,
        x,
        y,
        rx,
        ry,
      });
      return true;
    } catch (e) {
      events.push({
        ts: new Date().toISOString(),
        type: 'input:click_error',
        action,
        text: String(e),
      });
      return false;
    }
  };

  const safePress = async (page, key, action) => {
    try {
      await withTimeout(page.keyboard.press(key), 5000, `key:${action}`);
      countAction(action);
      maybeLogAction(action);
      events.push({
        ts: new Date().toISOString(),
        type: 'input:key',
        action,
        key,
      });
      return true;
    } catch (e) {
      events.push({
        ts: new Date().toISOString(),
        type: 'input:key_error',
        action,
        key,
        text: String(e),
      });
      return false;
    }
  };

  const runInputCycle = async (page, state, i) => {
    inputCycles++;
    await safeCanvasClick(page, state, 0.50, 0.50, 'focus_canvas');
    await sleep(100);

    const holdDuringSectorInit = shouldHoldDuringSectorInit(state);
    if (holdDuringSectorInit) {
      if (i % 24 === 0) {
        await mark(page, `input_${i}`, { loop: i, mode: 'sector_generation_hold' });
      }
      return;
    }

    // Explicitly dismiss "Exit game?" when it appears.
    if (state?.hasExitPrompt) {
      await safeCanvasClick(page, state, 0.78, 0.59, 'exit_no_click');
      await sleep(120);
      await safePress(page, 'KeyN', 'exit_no_key');
      await sleep(120);
      await safePress(page, 'Enter', 'exit_no_enter');
      if (i % 12 === 0) {
        await mark(page, `input_${i}`, { loop: i, mode: 'exit_prompt' });
      }
      return;
    }

    // Early graphics-init phase often shows modal warnings ("memory exceeds recommended") with an OK button near bottom.
    if (
      !!state?.hasCanvas &&
      (state?.native?.swap || 0) > 20 &&
      (state?.native?.draw || 0) <= 2 &&
      (state?.native?.begin || 0) <= 24
    ) {
      await safeCanvasClick(page, state, 0.43, 0.89, 'dialog_ok_bottom_right');
      await sleep(100);
      await safePress(page, 'Enter', 'dialog_ok_enter');
      await sleep(100);
      await safePress(page, 'Space', 'dialog_ok_space');
      await sleep(100);
    }

    // Launcher/title state: sweep likely "Play Starsector" region even when DOM text is unavailable.
    const statusText = String(state?.status || '');
    const recentDirectInitForLauncherSweep =
      directNewGameInitSeenAt > 0 && Date.now() - directNewGameInitSeenAt <= directInitHoldWindowMs;
    const likelyInitCanvasStall =
      !!state?.hasCanvas &&
      !state?.hasPreloading &&
      !state?.hasExitPrompt &&
      !campaignSeen &&
      !colonyPrimedSeen &&
      !dialogRequestSeen &&
      !recentDirectInitForLauncherSweep &&
      /graphics system is initializing/i.test(statusText) &&
      (state?.native?.swap || 0) > 20 &&
      ((state?.native?.begin || 0) > 200 ||
        (state?.native?.keyPoll || 0) > 40 ||
        (state?.native?.mousePoll || 0) > 40);
    const launcherSweepWindowOpen = !titleStateSeen || likelyInitCanvasStall;
    const likelyLauncherCanvas =
      (forceLauncherSweep || likelyInitCanvasStall) &&
      launcherSweepWindowOpen &&
      !state?.hasExitPrompt &&
      !!state?.hasCanvas &&
      !campaignSeen &&
      !colonyPrimedSeen &&
      !dialogRequestSeen &&
      (state?.native?.swap || 0) > 10;
    if (state?.hasLauncherPlay || likelyLauncherCanvas) {
      const nowMs = Date.now();
      const launcherRetryCooldownActive =
        launcherPlayIssued && nowMs - launcherPlayIssuedAt < launcherPlayRetryMs;
      if (launcherRetryCooldownActive) {
        await safeCanvasClick(page, state, 0.50, 0.50, 'launcher_retry_cooldown_focus');
        if (i % 12 === 0) {
          await mark(page, `input_${i}`, {
            loop: i,
            mode: 'launcher_retry_cooldown',
            launcherPlayIssuedAt,
            launcherPlayRetryMs,
          });
        }
        return;
      }
      const suppressLauncherAutomation =
        titleStateSeen || directNewGameInitSeen || campaignSeen || lastCampaignTransitionRequestAt > 0;
      if (suppressLauncherAutomation) {
        await safeCanvasClick(page, state, 0.50, 0.50, 'launcher_suppressed_focus');
        if (i % 12 === 0) {
          await mark(page, `input_${i}`, {
            loop: i,
            mode: 'launcher_suppressed_after_title_or_direct_init',
            titleStateSeen,
            directNewGameInitSeen,
            campaignSeen,
          });
        }
        return;
      }
      const canRunLauncherKickoff =
        disableLauncherInput &&
        !titleStateSeen &&
        launcherKickoffSweeps < launcherSweepLimitWhenDisabled;
      if (disableLauncherInput && !canRunLauncherKickoff) {
        if (i % 12 === 0) {
          await mark(page, `input_${i}`, {
            loop: i,
            mode: 'launcher_observe_only',
            titleStateSeen,
            launcherKickoffSweeps,
          });
        }
        return;
      }
      if (canRunLauncherKickoff) {
        launcherKickoffSweeps += 1;
      }
      const launcherModalTargets = [
        [0.46, 0.67, 'launcher_modal_ok_legacy'],
        [0.40, 0.78, 'launcher_modal_ok_low_1'],
        [0.36, 0.80, 'launcher_modal_ok_low_2'],
        [0.32, 0.82, 'launcher_modal_ok_low_3'],
        [0.28, 0.82, 'launcher_modal_ok_low_4'],
        [0.38, 0.89, 'launcher_modal_ok_low_5'],
        [0.42, 0.90, 'launcher_modal_ok_low_6'],
        [0.46, 0.90, 'launcher_modal_ok_low_7'],
      ];
      if (state?.hasMemoryWarning) {
        for (const [mx, my, label] of launcherModalTargets) {
          await safeCanvasClick(page, state, mx, my, `launcher_memory_${label}`);
          await sleep(70);
        }
        await safePress(page, 'Enter', 'launcher_memory_ok_enter');
        await sleep(100);
        await safePress(page, 'Space', 'launcher_memory_ok_space');
        await sleep(100);
      }
      for (const [mx, my, label] of launcherModalTargets) {
        await safeCanvasClick(page, state, mx, my, label);
        await sleep(70);
      }
      await safeCanvasClick(page, state, 0.25, 0.73, 'launcher_panel_focus');
      await sleep(90);
      const launcherPlayTargets = [
        // Primary cluster centered on the visible "Play Starsector" button.
        [0.24, 0.64, 'launcher_play_click'],
        [0.28, 0.64, 'launcher_play_click_alt'],
        [0.32, 0.64, 'launcher_play_click_alt2'],
        [0.24, 0.67, 'launcher_play_click_alt3'],
        [0.28, 0.67, 'launcher_play_click_alt4'],
        [0.32, 0.67, 'launcher_play_click_alt5'],
        // Lower-y fallback cluster for drift after warning dialogs.
        [0.22, 0.71, 'launcher_play_click_low_1'],
        [0.25, 0.71, 'launcher_play_click_low_2'],
        [0.28, 0.71, 'launcher_play_click_low_3'],
        [0.22, 0.74, 'launcher_play_click_low_4'],
        [0.25, 0.74, 'launcher_play_click_low_5'],
        [0.28, 0.74, 'launcher_play_click_low_6'],
        [0.22, 0.77, 'launcher_play_click_low_7'],
        [0.25, 0.77, 'launcher_play_click_low_8'],
        [0.28, 0.77, 'launcher_play_click_low_9'],
        [0.25, 0.81, 'launcher_play_click_low_10'],
        [0.28, 0.81, 'launcher_play_click_low_11'],
        [0.31, 0.81, 'launcher_play_click_low_12'],
        [0.25, 0.84, 'launcher_play_click_low_13'],
        [0.28, 0.84, 'launcher_play_click_low_14'],
        [0.31, 0.84, 'launcher_play_click_low_15'],
      ];
      for (const [tx, ty, label] of launcherPlayTargets) {
        await safeCanvasClick(page, state, tx, ty, label);
        await sleep(80);
      }
      await sleep(120);
      await safePress(page, 'Enter', 'launcher_play_enter');
      launcherPlayIssued = true;
      launcherPlayIssuedAt = Date.now();
      if (i % 12 === 0) {
        await mark(page, `input_${i}`, {
          loop: i,
          mode: canRunLauncherKickoff
            ? 'launcher_kickoff'
            : state?.hasLauncherPlay
              ? 'launcher_play'
              : 'launcher_drive',
          titleStateSeen,
          launcherKickoffSweeps,
        });
      }
      return;
    }

    // Preloading phase: avoid key spam; just keep focus occasionally.
    if (state?.hasPreloading) {
      if (i % 24 === 0) {
        await safeCanvasClick(page, state, 0.48, 0.49, 'preload_focus_click');
      }
      if (i % 12 === 0) {
        await mark(page, `input_${i}`, { loop: i, mode: 'preloading' });
      }
      return;
    }

    await safeCanvasClick(page, state, 0.63, 0.57, 'dialog_ok_center');
    await sleep(100);
    await safeCanvasClick(page, state, 0.63, 0.81, 'dialog_next_tip');
    await sleep(100);

    // Title/menu fallback.
    // In strict mode, avoid right-side menu clicks entirely; use keys only.
    if (!disableTitleMenuClicks) {
      const titleMenuY = allowNewGameInput ? [0.46] : [0.30];
      for (const y of titleMenuY) {
        await safeCanvasClick(page, state, 0.93, y, `title_menu_click_${Math.round(y * 100)}`);
        await sleep(80);
      }
    }

    const allowTitleKeysNow =
      !(avoidPostInitKeys && directNewGameInitSeen) &&
      !/graphics system is initializing/i.test(statusText);
    if (allowTitleKeysNow) {
      await safePress(page, 'Enter', 'flow_enter_focus');
      await sleep(80);
      if (allowNewGameInput) {
        await safePress(page, 'KeyN', 'title_new_game_key');
        await sleep(80);
      }
      await safePress(page, 'Enter', 'flow_enter_1');
      await sleep(80);
    }

    if (i % 12 === 0) {
      await mark(page, `input_${i}`, { loop: i, mode: 'title_fallback' });
    }
  };

  const runLoadingRetryBurst = async (page, state, i) => {
    await safeCanvasClick(page, state, 0.50, 0.50, 'retry_focus_center');
    await sleep(100);
    await safeCanvasClick(page, state, 0.50, 0.63, 'retry_focus_bottom');
    await sleep(100);
    await safePress(page, 'Enter', 'retry_enter_1');
    await sleep(120);
    await safePress(page, 'Enter', 'retry_enter_2');
    await sleep(120);
    await safePress(page, 'Space', 'retry_space');
    if (allowNewGameInput) {
      await sleep(120);
      await safePress(page, 'KeyN', 'retry_key_n');
    }
    await sleep(120);
    await safePress(page, 'Enter', 'retry_enter_3');
    if (!disableTitleMenuClicks) {
      await sleep(120);
      await safeCanvasClick(page, state, 0.93, 0.30, 'retry_title_continue');
      await sleep(120);
      await safeCanvasClick(page, state, 0.93, 0.46, 'retry_title_new_game');
    }
    if (!disableLauncherInput) {
      await sleep(120);
      await safeCanvasClick(page, state, 0.36, 0.80, 'retry_launcher_modal_ok_low_1');
      await sleep(90);
      await safeCanvasClick(page, state, 0.32, 0.82, 'retry_launcher_modal_ok_low_2');
      await sleep(90);
      await safeCanvasClick(page, state, 0.24, 0.64, 'retry_launcher_play');
      await sleep(120);
      await safeCanvasClick(page, state, 0.28, 0.64, 'retry_launcher_play_alt');
      await sleep(120);
      await safeCanvasClick(page, state, 0.32, 0.64, 'retry_launcher_play_alt2');
      await sleep(120);
      await safeCanvasClick(page, state, 0.28, 0.67, 'retry_launcher_play_alt3');
      await sleep(120);
      await safeCanvasClick(page, state, 0.25, 0.74, 'retry_launcher_play_low_1');
      await sleep(120);
      await safeCanvasClick(page, state, 0.25, 0.77, 'retry_launcher_play_low_2');
      await sleep(120);
      await safeCanvasClick(page, state, 0.28, 0.81, 'retry_launcher_play_low_3');
      await sleep(120);
      await safeCanvasClick(page, state, 0.31, 0.81, 'retry_launcher_play_low_4');
      await sleep(120);
      await safeCanvasClick(page, state, 0.28, 0.84, 'retry_launcher_play_low_5');
      await sleep(120);
    }
    events.push({
      ts: new Date().toISOString(),
      type: 'loading_retry_burst',
      i,
      loadingBarStuckTicks,
    });
  };

  try {
    browser = await chromium.launch({
      headless,
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
    });

    const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });

    page.on('console', (msg) => {
      let text = '';
      try {
        text = msg.text();
      } catch {
        text = String(msg);
      }
      const type = msg.type ? msg.type() : 'log';
      updateMilestonesFromText(text);
      if (
        type === 'error' ||
          /Fixer:|Campaign State|colony|interaction|New Game|Continue|Error creating new game|NullPointer|Exception|Open Market|Comms Directory|LoadGameDialog|direct-new-game|orbital_junk/i.test(
          text
        )
      ) {
        events.push({ ts: new Date().toISOString(), type: `console:${type}`, text });
        if (
          /Fixer: auto campaign|Fixer: direct new-game|Fixer: direct-new-game|Campaign State|colony|NullPointer|LoadGameDialog|Title Screen State|menu select failed|orbital_junk|runtime readiness/i.test(
            text
          )
        ) {
          console.log(`[watch] ${text}`);
        }
      }
    });

    page.on('requestfailed', (req) => {
      try {
        trackNetIssue('requestfailed', req.url(), req.failure()?.errorText || null);
      } catch {}
    });

    page.on('response', (resp) => {
      try {
        const status = resp.status();
        if (status >= 400) {
          trackNetIssue('http_error', resp.url(), status);
        }
      } catch {}
    });

    await page.addInitScript(
      ({
        serial,
        colonyName,
        variant,
        fixerAutoCampaignEnabled,
        autoMode,
        directLaunch,
        useLoadingPatch,
        lwjglModule,
        bootMode,
        preloadSpecFiles,
        disableShipHullPostPass,
        autoCampaignTitleSettleMs,
        autoCampaignSectorSize,
        autoCampaignAllowDirectEscape,
        autoCampaignTitleRenderWarmup,
        autoCampaignFallbackMs,
        autoCampaignTimeoutMs,
        autoCampaignDirectAttemptTimeoutMs,
        autoCampaignDirectPostInitWaitMs,
        autoCampaignDirectTimeoutFailureLimit,
        autoCampaignCoreSpecBaselineNonMutatingWaitMs,
        autoCampaignMutatingSpecPreflight,
        autoCampaignSkipFactionPreflight,
        autoCampaignWithTimePass,
        autoCampaignInvokeCreateOnPlayerFleetNull,
        autoCampaignInvokeCreateInCampaignState,
        autoCampaignEnableSyntheticPlayerFleetFallback,
        autoCampaignAllowDialogWithoutPlayerFleet,
        autoCampaignPlayerFleetNullTransitionForceAfterMs,
        useNpeFixLib,
      }) => {
        window.__LWJGL_MODULE__ = lwjglModule || 'lwjgl_clean.js';
        // Keep filter enabled; full native trace logging can stall rendering progression.
        window.__DISABLE_LWJGL_CONSOLE_FILTER = false;
        window.__LWJGL_NATIVE_TRACE_LOG__ = false;
        window.__LWJGL_NATIVE_TRACE_LIMIT__ = 24;
        window.__LWJGL_FIRST_LOG_LIMIT__ = 160;
        window.__STARSECTOR_SERIAL__ = serial || window.__STARSECTOR_SERIAL__ || '';
        window.__USE_LOADING_PATCH__ = !!useLoadingPatch;
        window.__PRELOAD_SPEC_FILES__ = !!preloadSpecFiles;
        window.__DISABLE_SHIP_HULL_POST_PASS__ = !!disableShipHullPostPass;
        window.__USE_NPE_FIX_LIB__ = !!useNpeFixLib;

        window.__STARSECTOR_AUTO_CAMPAIGN__ = !!fixerAutoCampaignEnabled;
        window.__STARSECTOR_AUTO_CAMPAIGN_MODE__ = autoMode;
        window.__STARSECTOR_AUTO_CAMPAIGN_START_VARIANT__ = variant;
        window.__STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__ = autoCampaignSectorSize;
        window.__STARSECTOR_AUTO_CAMPAIGN_TITLE_SETTLE_MS__ = autoCampaignTitleSettleMs;
        window.__STARSECTOR_AUTO_CAMPAIGN_ALLOW_DIRECT_ESCAPE__ = !!autoCampaignAllowDirectEscape;
        window.__STARSECTOR_AUTO_CAMPAIGN_TITLE_RENDER_WARMUP__ = !!autoCampaignTitleRenderWarmup;
        window.__STARSECTOR_AUTO_CAMPAIGN_FALLBACK_MS__ = autoCampaignFallbackMs;
        window.__STARSECTOR_AUTO_CAMPAIGN_TIMEOUT_MS__ = autoCampaignTimeoutMs;
        window.__STARSECTOR_AUTO_CAMPAIGN_DIRECT_ATTEMPT_TIMEOUT_MS__ =
          autoCampaignDirectAttemptTimeoutMs;
        window.__STARSECTOR_AUTO_CAMPAIGN_DIRECT_POST_INIT_WAIT_MS__ =
          autoCampaignDirectPostInitWaitMs;
        window.__STARSECTOR_AUTO_CAMPAIGN_DIRECT_TIMEOUT_FAILURE_LIMIT__ =
          autoCampaignDirectTimeoutFailureLimit;
        window.__STARSECTOR_AUTO_CAMPAIGN_CORE_SPEC_NON_MUTATING_WAIT_MS__ =
          autoCampaignCoreSpecBaselineNonMutatingWaitMs;
        window.__STARSECTOR_AUTO_CAMPAIGN_MUTATING_SPEC_PREFLIGHT__ =
          !!autoCampaignMutatingSpecPreflight;
        window.__STARSECTOR_AUTO_CAMPAIGN_SKIP_FACTION_PREFLIGHT__ =
          !!autoCampaignSkipFactionPreflight;
        window.__STARSECTOR_AUTO_CAMPAIGN_WITH_TIME_PASS__ = !!autoCampaignWithTimePass;
        window.__STARSECTOR_AUTO_CAMPAIGN_INVOKE_CREATE_ON_PLAYER_FLEET_NULL__ =
          !!autoCampaignInvokeCreateOnPlayerFleetNull;
        window.__STARSECTOR_AUTO_CAMPAIGN_INVOKE_CREATE_IN_CAMPAIGN_STATE__ =
          !!autoCampaignInvokeCreateInCampaignState;
        window.__STARSECTOR_AUTO_CAMPAIGN_ENABLE_SYNTHETIC_PLAYER_FLEET_FALLBACK__ =
          !!autoCampaignEnableSyntheticPlayerFleetFallback;
        window.__STARSECTOR_AUTO_CAMPAIGN_ALLOW_DIALOG_WITHOUT_PLAYER_FLEET__ =
          !!autoCampaignAllowDialogWithoutPlayerFleet;
        window.__STARSECTOR_AUTO_CAMPAIGN_PLAYER_FLEET_NULL_FORCE_AFTER_MS__ =
          autoCampaignPlayerFleetNullTransitionForceAfterMs;
        window.__STARSECTOR_AUTO_VISIT_COLONY__ = true;
        window.__STARSECTOR_AUTO_VISIT_COLONY_NAME__ = colonyName;

        // Respect runtime mode so we can compare launcher-driven vs direct-launch starts.
        window.__STARSECTOR_DIRECT_LAUNCH__ = !!directLaunch;
        window.__STARSECTOR_BOOT__ = bootMode || 'launcher';
      },
      {
        serial,
        colonyName,
        variant,
        fixerAutoCampaignEnabled,
        autoMode,
        directLaunch,
        useLoadingPatch,
        lwjglModule,
        bootMode,
        preloadSpecFiles,
        disableShipHullPostPass,
        autoCampaignTitleSettleMs,
        autoCampaignSectorSize,
        autoCampaignAllowDirectEscape,
        autoCampaignTitleRenderWarmup,
        autoCampaignFallbackMs,
        autoCampaignTimeoutMs,
        autoCampaignDirectAttemptTimeoutMs,
        autoCampaignDirectPostInitWaitMs,
        autoCampaignDirectTimeoutFailureLimit,
        autoCampaignCoreSpecBaselineNonMutatingWaitMs,
        autoCampaignMutatingSpecPreflight,
        autoCampaignSkipFactionPreflight,
        autoCampaignWithTimePass,
        autoCampaignInvokeCreateOnPlayerFleetNull,
        autoCampaignInvokeCreateInCampaignState,
        autoCampaignEnableSyntheticPlayerFleetFallback,
        autoCampaignAllowDialogWithoutPlayerFleet,
        autoCampaignPlayerFleetNullTransitionForceAfterMs,
        useNpeFixLib,
      }
    );

    await page.goto('http://localhost:8888/launch.html', {
      waitUntil: 'domcontentloaded',
      timeout: 60000,
    });

    await page.click('#startBtn', { timeout: 20000 });
    await mark(page, 't0', {
      colonyName,
      serialLen: serial.length,
      variant,
      autoMode,
      directLaunch,
      useLoadingPatch,
      lwjglModule,
      bootMode,
      disableLauncherInput,
      autoCampaignFallbackMs,
      autoCampaignTimeoutMs,
      autoCampaignDirectAttemptTimeoutMs,
      autoCampaignDirectPostInitWaitMs,
      autoCampaignDirectTimeoutFailureLimit,
      autoCampaignSectorSize,
    });

    for (let i = 0; i < warmupLoops; i++) {
      await sleep(2500);
      const s = await readState(page);
      events.push({
        ts: new Date().toISOString(),
        type: 'warmup',
        i,
        status: s?.status || null,
        native: s?.native || null,
        milestones: { campaignSeen, colonyPrimedSeen, dialogRequestSeen, colonyVisitLikely },
        directNewGameFailCount,
      });
      if ((s?.native?.swap || 0) > 20) break;
    }

    for (let i = 0; i < maxLoops; i++) {
      await sleep(loopDelayMs);
      const state = await readState(page);

      const isLikelyLoadingBarStall =
        !!state?.hasCanvas &&
        !state?.hasLauncherPlay &&
        !state?.hasExitPrompt &&
        !state?.hasPreloading &&
        (state?.native?.swap || 0) > 10 &&
        !campaignSeen &&
        !colonyPrimedSeen &&
        !dialogRequestSeen;
      if (isLikelyLoadingBarStall) {
        loadingBarStuckTicks += 1;
      } else {
        loadingBarStuckTicks = 0;
        loadingBarRetryBurstDone = false;
      }
      if (enableRetryBurst && !loadingBarRetryBurstDone && loadingBarStuckTicks >= 18) {
        loadingBarRetryBurstDone = true;
        await runLoadingRetryBurst(page, state, i);
      }

      const swapCount = state?.native?.swap || 0;
      const drawCount = state?.native?.draw || 0;
      const statusText = String(state?.status || '');
      let initFreezeReason = null;
      const isGraphicsInitDeadStall =
        !campaignSeen &&
        !colonyPrimedSeen &&
        !dialogRequestSeen &&
        !state?.hasLauncherPlay &&
        !state?.hasExitPrompt &&
        !state?.hasPreloading &&
        !state?.hasGeneratingSector &&
        /graphics system is initializing/i.test(statusText) &&
        swapCount > 80 &&
        drawCount <= 2 &&
        swapCount === lastSwapCount &&
        drawCount === lastDrawCount;
      const isGraphicsInitLowSwapDeadStall =
        !campaignSeen &&
        !colonyPrimedSeen &&
        !dialogRequestSeen &&
        i >= 24 &&
        (titleStateSeen || directNewGameInitSeen) &&
        !state?.hasLauncherPlay &&
        !state?.hasExitPrompt &&
        !state?.hasPreloading &&
        !state?.hasGeneratingSector &&
        /graphics system is initializing/i.test(statusText) &&
        swapCount > 0 &&
        swapCount <= 8 &&
        drawCount <= 2 &&
        swapCount === lastSwapCount &&
        drawCount === lastDrawCount;
      if (isGraphicsInitDeadStall || isGraphicsInitLowSwapDeadStall) {
        initFreezeReason = isGraphicsInitLowSwapDeadStall
          ? 'graphics_init_low_swap_stall'
          : 'graphics_init_dead_stall';
        initFreezeTicks += 1;
      } else {
        initFreezeTicks = 0;
      }
      const isBlankRuntimeStall =
        !campaignSeen &&
        !colonyPrimedSeen &&
        !dialogRequestSeen &&
        !state?.hasCanvas &&
        !state?.hasLauncherPlay &&
        !state?.hasExitPrompt &&
        !state?.hasPreloading &&
        !state?.hasGeneratingSector &&
        !String(state?.status || '').trim();
      if (isBlankRuntimeStall) {
        blankRuntimeTicks += 1;
      } else {
        blankRuntimeTicks = 0;
      }
      lastSwapCount = swapCount;
      lastDrawCount = drawCount;

      if (
        (initFreezeTicks >= initFreezeRecoveryTicks ||
          blankRuntimeTicks >= initFreezeRecoveryTicks) &&
        initFreezeRecoveries < maxInitFreezeRecoveries
      ) {
        initFreezeRecoveries += 1;
        const recoveryReason =
          initFreezeTicks >= initFreezeRecoveryTicks
            ? initFreezeReason || 'graphics_init_dead_stall'
            : 'blank_runtime_stall';
        console.log(
          `[watch] init-freeze recovery ${initFreezeRecoveries}/${maxInitFreezeRecoveries} reason=${recoveryReason} loop=${i} swap=${swapCount} draw=${drawCount}`
        );
        events.push({
          ts: new Date().toISOString(),
          type: 'init_freeze_recovery',
          i,
          recoveryReason,
          initFreezeTicks,
          blankRuntimeTicks,
          initFreezeRecoveries,
          status: state?.status || null,
          native: state?.native || null,
        });
        await mark(page, `freeze_recover_${initFreezeRecoveries}_pre`, {
          loop: i,
          initFreezeTicks,
          initFreezeRecoveries,
        });
        try {
          await withTimeout(
            page.goto('http://localhost:8888/launch.html', {
              waitUntil: 'domcontentloaded',
              timeout: 70000,
            }),
            75000,
            'goto:init-freeze'
          );
          await withTimeout(page.click('#startBtn', { timeout: 30000 }), 35000, 'start:init-freeze');
        } catch (e) {
          events.push({
            ts: new Date().toISOString(),
            type: 'init_freeze_reload_error',
            i,
            text: String(e),
          });
        }
        await sleep(3500);
        loadingBarStuckTicks = 0;
        loadingBarRetryBurstDone = false;
        initFreezeTicks = 0;
        blankRuntimeTicks = 0;
        launcherKickoffSweeps = 0;
        directNewGameInitSeen = false;
        generatingSectorSeen = false;
        directNewGameInitSeenAt = 0;
        generatingSectorSeenAt = 0;
        titleStateSeen = false;
        lastCampaignTransitionRequestAt = 0;
        lastSwapCount = -1;
        lastDrawCount = -1;
        await mark(page, `freeze_recover_${initFreezeRecoveries}_post`, {
          loop: i,
          initFreezeRecoveries,
        });
        continue;
      }

      const shouldNudgeTitleWithKeys =
        allowNewGameInput &&
        !campaignSeen &&
        !colonyPrimedSeen &&
        !dialogRequestSeen &&
        !state?.hasLauncherPlay &&
        !state?.hasExitPrompt &&
        !state?.hasPreloading &&
        (state?.native?.swap || 0) > 20 &&
        (state?.native?.draw || 0) <= 2 &&
        (state?.native?.begin || 0) <= 24 &&
        i % 12 === 0;
      const likelyInteractiveCanvas =
        !campaignSeen &&
        !!state?.hasCanvas &&
        !state?.hasExitPrompt &&
        !state?.hasPreloading &&
        (state?.native?.swap || 0) > 20;
      const transitionCooldownActive =
        !campaignSeen &&
        lastCampaignTransitionRequestAt > 0 &&
        Date.now() - lastCampaignTransitionRequestAt < campaignTransitionCooldownMs &&
        !state?.hasLauncherPlay &&
        !state?.hasExitPrompt &&
        !state?.hasPreloading;

    // In passive mode, avoid broad active driving once direct new-game init is underway.
    const holdDuringSectorInit = shouldHoldDuringSectorInit(state);

    // Allow active driving while campaign is not reached, but keep sector-generation phase quiet.
    const shouldTryInput =
      !campaignSeen &&
      !transitionCooldownActive &&
      !holdDuringSectorInit &&
      (i % activeInputEveryLoops === 0 ||
        state?.hasLauncherPlay ||
        state?.hasExitPrompt ||
        (state?.hasPreloading && i % 6 === 0) ||
        shouldNudgeTitleWithKeys ||
        (!passiveInput && likelyInteractiveCanvas) ||
        (!passiveInput &&
          ((isLikelyLoadingBarStall && loadingBarStuckTicks >= 8 && i % 2 === 0) || i % 30 === 0)));
    if (shouldTryInput) {
      await runInputCycle(page, state, i);
    } else if (holdDuringSectorInit && i % 24 === 0) {
      await safeCanvasClick(page, state, 0.50, 0.50, 'sector_hold_focus');
    }

      events.push({
        ts: new Date().toISOString(),
        type: 'loop',
        i,
        status: state?.status || null,
        native: state?.native || null,
        milestones: { campaignSeen, colonyPrimedSeen, dialogRequestSeen, colonyVisitLikely },
        loadGameMisrouteSeen,
        directNewGameFailCount,
        inputCycles,
        loadingBarStuckTicks,
        initFreezeTicks,
        blankRuntimeTicks,
        initFreezeRecoveries,
        launcherKickoffSweeps,
      });

      if (i % 10 === 0) {
        console.log(
          `[loop ${i}] status=${state?.status || 'n/a'} swap=${state?.native?.swap || 0} draw=${state?.native?.draw || 0} campaignSeen=${campaignSeen} colonyPrimedSeen=${colonyPrimedSeen} dialogRequestSeen=${dialogRequestSeen} loadGameMisrouteSeen=${loadGameMisrouteSeen} directNewGameFailCount=${directNewGameFailCount}`
        );
      }

      if (i % 20 === 0) {
        await mark(page, `loop_${i}`);
      }

      if (!successAt && (dialogRequestSeen || colonyPrimedSeen || colonyVisitLikely)) {
        successAt = Date.now();
        events.push({
          ts: new Date().toISOString(),
          type: 'success_gate',
          i,
          status: state?.status || null,
          milestones: { campaignSeen, colonyPrimedSeen, dialogRequestSeen, colonyVisitLikely },
          directNewGameFailCount,
          inputCycles,
        });
      }

      if (successAt && Date.now() - successAt > 15000) {
        break;
      }
    }

    await mark(page, 'final', { colonyName, variant, autoMode, directLaunch });
    const finalState = await readState(page);
    persistResult({
      finalStatus: finalState?.status || null,
      finalNative: finalState?.native || null,
      reason: 'completed',
    });

    console.log('out=' + out);
    console.log('shots=' + shots.length);
    console.log('campaignSeen=' + campaignSeen);
    console.log('colonyPrimedSeen=' + colonyPrimedSeen);
    console.log('dialogRequestSeen=' + dialogRequestSeen);
    console.log('colonyVisitLikely=' + colonyVisitLikely);
    console.log('loadGameMisrouteSeen=' + loadGameMisrouteSeen);
    console.log('directNewGameFailCount=' + directNewGameFailCount);
    console.log('inputCycles=' + inputCycles);
    console.log('loadingBarStuckTicks=' + loadingBarStuckTicks);
    console.log('initFreezeTicks=' + initFreezeTicks);
    console.log('blankRuntimeTicks=' + blankRuntimeTicks);
    console.log('initFreezeRecoveries=' + initFreezeRecoveries);
    console.log('launcherKickoffSweeps=' + launcherKickoffSweeps);
    console.log('lwjglModule=' + lwjglModule);
    console.log('bootMode=' + bootMode);
  } catch (e) {
    events.push({
      ts: new Date().toISOString(),
      type: 'runner_error',
      text: String(e && e.stack ? e.stack : e),
    });
    try {
      persistResult({ error: String(e), reason: 'runner_error' });
    } catch {}
    console.log('out=' + out);
    console.log('error=' + String(e));
    process.exitCode = 1;
  } finally {
    clearTimeout(hardStop);
    try {
      if (browser) await browser.close();
    } catch {}
    setTimeout(() => process.exit(process.exitCode || 0), 250);
  }
})();
