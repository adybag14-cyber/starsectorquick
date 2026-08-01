#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).resolve().parents[1] / "jars" / "Fixer.java"
text = path.read_text(encoding="utf-8")

old_catch = """                            } catch (java.lang.reflect.InvocationTargetException invokeError) {
                                Throwable cause = invokeError.getCause();"""
new_catch = """                            } catch (Throwable invokeError) {
                                // CheerpJ can surface the delegate's exception directly instead of
                                // wrapping it in InvocationTargetException. Handle both forms.
                                Throwable cause = invokeError;
                                if (invokeError instanceof java.lang.reflect.InvocationTargetException) {
                                    Throwable reflectedCause =
                                            ((java.lang.reflect.InvocationTargetException) invokeError).getCause();
                                    if (reflectedCause != null) {
                                        cause = reflectedCause;
                                    }
                                }"""

count = text.count(old_catch)
if count != 1:
    raise RuntimeError(f"SettingsAPI proxy catch block: expected one match, found {count}")
text = text.replace(old_catch, new_catch, 1)

# Watcher threads may prepare campaign data, but AppDriver/BaseGameState owns state
# changes. Queue the state ID in a system property; a bytecode hook in
# BaseGameState.traverse consumes it immediately before Display.update() on the
# render thread. The consumed marker prevents the watcher from restarting the
# fade every polling interval.
transition_anchor = """        Object resolvedTitleState = resolveTitleStateForTransition(ctx, titleState);
        maybePrepareUiForCampaignTransition();
        Object beforeState = readCurrentStateFromDriver(ctx);"""
transition_replacement = """        Object resolvedTitleState = resolveTitleStateForTransition(ctx, titleState);
        maybePrepareUiForCampaignTransition();
        if (resolvedTitleState != null) {
            final String transitionKey = "starsector.pendingStateTransition";
            String pendingTransition = System.getProperty(transitionKey);
            if (pendingTransition == null || pendingTransition.length() == 0) {
                System.setProperty(transitionKey, CAMPAIGN_STATE_ID);
                System.out.println(
                        "Fixer: queued Campaign State transition for the AppDriver render thread.");
            }
            return true;
        }
        Object beforeState = readCurrentStateFromDriver(ctx);"""
count = text.count(transition_anchor)
if count != 1:
    raise RuntimeError(f"render-thread transition queue: expected one match, found {count}")
text = text.replace(transition_anchor, transition_replacement, 1)

path.write_text(text, encoding="utf-8", newline="\n")
print("Hardened SettingsAPI proxy and queued campaign transitions for the render thread")
