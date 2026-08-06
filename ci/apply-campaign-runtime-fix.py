#!/usr/bin/env python3
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
fixer_path = ROOT / 'jars' / 'Fixer.java'
lwjgl_path = ROOT / 'build' / 'final' / 'wasm-modules' / 'lwjgl.js'
launch_path = ROOT / 'launch.html'


def replace_exact(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


fixer = fixer_path.read_text(encoding='utf-8-sig')
old_false_ready = '''            String preInvokeReadiness = checkDirectNewGameRuntimeReadiness();
            if (preInvokeReadiness == null) {
                System.out.println(
                        "Fixer: direct-new-game pre-invoke readiness="
                                + String.valueOf(preInvokeReadiness)
                                + "; skipping invoke-create and continuing transition flow.");
                directNewGameLastNullMarker = "pre-invoke-ready";
                System.out.println("Fixer: direct-new-game return-null marker=pre-invoke-ready");
                return null;
            }
            if ("player-fleet-null".equals(preInvokeReadiness)) {'''
new_false_ready = '''            String preInvokeReadiness = checkDirectNewGameRuntimeReadiness();
            if (preInvokeReadiness == null) {
                // A synthetic fleet/market can satisfy the lightweight readiness probe before
                // CampaignGameManager has actually run create(). Treat that as partial runtime
                // state, not a completed new game, or the driver enters an uninitialized black
                // CampaignState that never owns the render loop.
                System.out.println(
                        "Fixer: direct-new-game pre-invoke readiness is superficially ready; "
                                + "requiring the real campaign create() path before transition.");
                directNewGameLastNullMarker = "pre-invoke-partial-runtime-require-create";
            }
            if ("player-fleet-null".equals(preInvokeReadiness)) {'''
fixer = replace_exact(fixer, old_false_ready, new_false_ready, 'false-ready early return')

# StarfarerSettings exposes an anonymous SettingsAPI implementation before the
# AppDriver has a current state. Its isInCampaignState() method dereferences that
# missing state and kills BaseGameState.traverse during launcher startup. Keep the
# real provider for every method, but make that single readiness query null-safe
# until the driver has finished initialising.
old_settings_install = '''            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method setSettings = findGlobalSetSettingsMethod(globalClass);'''
new_settings_install = '''            settingsApi = wrapSettingsApiNullSafe(settingsApi);

            Class<?> globalClass = Class.forName("com.fs.starfarer.api.Global");
            Method setSettings = findGlobalSetSettingsMethod(globalClass);'''
fixer = replace_exact(
    fixer,
    old_settings_install,
    new_settings_install,
    'null-safe SettingsAPI installation',
)

settings_helper_anchor = '''    private static Method findGlobalSetSettingsMethod(Class<?> globalClass) {'''
settings_helper = '''    private static Object wrapSettingsApiNullSafe(final Object delegate) {
        if (delegate == null) {
            return null;
        }
        try {
            final Class<?> settingsApiInterface =
                    Class.forName("com.fs.starfarer.api.SettingsAPI");
            if (!settingsApiInterface.isInstance(delegate)) {
                return delegate;
            }
            ClassLoader loader = settingsApiInterface.getClassLoader();
            if (loader == null) {
                loader = delegate.getClass().getClassLoader();
            }
            if (loader == null) {
                loader = Fixer.class.getClassLoader();
            }
            final boolean[] campaignStateNullLogged = new boolean[] {false};
            return java.lang.reflect.Proxy.newProxyInstance(
                    loader,
                    new Class<?>[] {settingsApiInterface},
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args)
                                throws Throwable {
                            try {
                                return method.invoke(delegate, args);
                            } catch (java.lang.reflect.InvocationTargetException invokeError) {
                                Throwable cause = invokeError.getCause();
                                if ("isInCampaignState".equals(method.getName())
                                        && cause instanceof NullPointerException) {
                                    if (!campaignStateNullLogged[0]) {
                                        campaignStateNullLogged[0] = true;
                                        System.out.println(
                                                "Fixer: SettingsAPI.isInCampaignState returned false while AppDriver state was not yet initialized.");
                                    }
                                    return Boolean.FALSE;
                                }
                                throw cause == null ? invokeError : cause;
                            }
                        }
                    });
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: unable to install null-safe SettingsAPI proxy; using original provider: "
                            + describeThrowableChain(t));
            return delegate;
        }
    }

'''
fixer = replace_exact(
    fixer,
    settings_helper_anchor,
    settings_helper + settings_helper_anchor,
    'null-safe SettingsAPI helper',
)

# Do not invent an active Title Screen State merely because the state object is
# present in AppDriver.states. During ResourceLoaderState the real current state
# is still null, but the old watcher substituted states["Title Screen State"] and
# started campaign creation while the loader thread was still populating SpecStore.
# That race is the root cause behind missing variants/conditions/procgen tables.
old_state_fallback = '''            if (currentState == null && states != null && startState instanceof String) {
                try {
                    Object titleState = states.get(TITLE_STATE_ID);
                    if (titleState != null) {
                        currentState = titleState;
                    }
                } catch (Throwable ignored) {
                }
            }

'''
new_state_fallback = '''            // currentState intentionally remains null until AppDriver itself enters a state.
            // Presence in the states map is construction/registration, not state ownership.

'''
fixer = replace_exact(
    fixer,
    old_state_fallback,
    new_state_fallback,
    'premature title-state fallback',
)

# CampaignGameManager.create() mutates global campaign state. Running it on a
# daemon worker while TitleScreenState.advance() continues on AppDriver races the
# title combat engine and currently crashes CombatEngine.recreateAiGridsIfNeeded.
# Submit the create call to the existing BaseGameState render-thread drain instead.
invoke_thread_pattern = re.compile(
    r'''                final Object\[\] invokeResultHolder = new Object\[1\];\n'''
    r'''                final Throwable\[\] invokeErrorHolder = new Throwable\[1\];\n'''
    r'''                Thread invokeCreateThread =\n'''
    r'''.*?'''
    r'''                Object result = invokeResultHolder\[0\];''',
    re.S,
)
invoke_thread_replacement = '''                Object result;
                try {
                    result = com.fs.starfarer.MainThreadTransitionBridge.invokeOnRenderThread(
                            invokeCreateMethod,
                            invokeCreateData,
                            invokeCreateCampaignState,
                            invokeCallTimeoutMs);
                } catch (java.util.concurrent.TimeoutException timeout) {
                    retainInvokeCreateLease = true;
                    maybeLogNewGamePreflightIssue(
                            "render-thread invoke-create pickup timed out after "
                                    + invokeCallTimeoutMs
                                    + "ms; retaining create lease.");
                    return "direct new-game preflight pending: render-thread-invoke-create-timeout("
                            + invokeCallTimeoutMs
                            + "ms)";
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    retainInvokeCreateLease = true;
                    return "direct new-game preflight pending: render-thread-invoke-create-interrupted";
                }'''
fixer, count = invoke_thread_pattern.subn(invoke_thread_replacement, fixer, count=1)
if count != 1:
    raise RuntimeError(f'render-thread campaign create block: expected one match, found {count}')

if os.environ.get('KEEP_UNSAFE_FORCE_ACTIVATION', '0') != '1':
    driver_pattern = re.compile(
        r'''                    forceStateFaderOut\(resolvedTitleState\);\n'''
        r'''                    if \(!didCampaignTransitionAdvance\(ctx, resolvedTitleState, beforeState\)\) \{.*?'''
        r'''                    System\.out\.println\(\n'''
        r'''                            "Fixer: requested transition to Campaign State \(driver\.goToState\)\."\);\n'''
        r'''                    return true;''',
        re.S,
    )
    driver_replacement = '''                    forceStateFaderOut(resolvedTitleState);
                    System.out.println(
                            "Fixer: requested Campaign State via driver.goToState; "
                                    + "leaving state ownership on the AppDriver render thread.");
                    return true;'''
    fixer, count = driver_pattern.subn(driver_replacement, fixer, count=1)
    if count != 1:
        raise RuntimeError(f'driver transition block: expected one match, found {count}')

    title_pattern = re.compile(
        r'''            forceStateFaderOut\(resolvedTitleState\);\n'''
        r'''            if \(!didCampaignTransitionAdvance\(ctx, resolvedTitleState, beforeState\)\) \{.*?'''
        r'''            System\.out\.println\("Fixer: requested transition to Campaign State \(titleState\.goToState\)\."\);\n'''
        r'''            return true;''',
        re.S,
    )
    title_replacement = '''            forceStateFaderOut(resolvedTitleState);
            System.out.println(
                    "Fixer: requested Campaign State via titleState.goToState; "
                            + "leaving state ownership on the AppDriver render thread.");
            return true;'''
    fixer, count = title_pattern.subn(title_replacement, fixer, count=1)
    if count != 1:
        raise RuntimeError(f'title transition block: expected one match, found {count}')

fixer_path.write_text(fixer, encoding='utf-8', newline='\n')

launch = launch_path.read_text(encoding='utf-8')
launch = launch.replace(
    '/watcher state=Campaign State|reached Campaign State|Campaign State transition fallback succeeded/i',
    '/watcher state=Campaign State|reached Campaign State/i',
)
# Even after AppDriver genuinely enters Title Screen State, leave a short settle
# window for late loader/render initialization before starting the direct campaign.
old_title_settle = '''                const autoCampaignTitleSettleMs = Math.max(
                    0,
                    Number(window.__STARSECTOR_AUTO_CAMPAIGN_TITLE_SETTLE_MS__ ?? 1000) || 1000
                );'''
new_title_settle = '''                const autoCampaignTitleSettleMs = Math.max(
                    0,
                    Number(window.__STARSECTOR_AUTO_CAMPAIGN_TITLE_SETTLE_MS__ ?? 25000) || 25000
                );'''
launch = replace_exact(
    launch,
    old_title_settle,
    new_title_settle,
    'title/resource-loader settle window',
)
launch_path.write_text(launch, encoding='utf-8', newline='\n')

swap_mode = os.environ.get('SWAP_YIELD_MODE', 'raf').strip().lower()
lwjgl = lwjgl_path.read_text(encoding='utf-8')
old_swap_tail = '''\t// CheerpJ custom JNI calls must not keep the Java VM suspended on a browser
\t// animation-frame Promise. The framebuffer has already been blitted above.
\treturn;'''
if swap_mode == 'sync':
    new_swap_tail = old_swap_tail
elif swap_mode == 'raf':
    new_swap_tail = '''\t// Yield the CheerpJ Java render thread to the browser and resume on the next frame.
\treturn new Promise(function(resolve) { requestAnimationFrame(resolve); });'''
elif swap_mode == 'timeout0':
    new_swap_tail = '''\t// Yield the CheerpJ Java render thread without imposing a frame-rate delay.
\treturn new Promise(function(resolve) { setTimeout(resolve, 0); });'''
elif swap_mode == 'timeout16':
    new_swap_tail = '''\t// Yield the CheerpJ Java render thread at approximately 60 Hz.
\treturn new Promise(function(resolve) { setTimeout(resolve, 16); });'''
else:
    raise RuntimeError(f'unsupported SWAP_YIELD_MODE={swap_mode!r}')
if swap_mode != 'sync':
    lwjgl = replace_exact(lwjgl, old_swap_tail, new_swap_tail, f'LWJGL swap mode {swap_mode}')
lwjgl_path.write_text(lwjgl, encoding='utf-8', newline='\n')

print(f'Applied campaign runtime fix; swap mode={swap_mode}, unsafe force activation={os.environ.get("KEEP_UNSAFE_FORCE_ACTIVATION", "0")}')
