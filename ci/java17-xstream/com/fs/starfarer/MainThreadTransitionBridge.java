package com.fs.starfarer;

import java.lang.reflect.Method;

/**
 * Moves watcher-requested state changes onto BaseGameState.traverse(), i.e. the
 * thread that owns rendering and AppDriver state progression.
 */
public final class MainThreadTransitionBridge {
    private static final String KEY = "starsector.pendingStateTransition";
    private static final String CONSUMED = "__consumed__";
    private static boolean successLogged;

    private MainThreadTransitionBridge() {}

    public static void drain(Object state) {
        String next = System.getProperty(KEY);
        if (next == null || next.length() == 0 || CONSUMED.equals(next) || state == null) {
            return;
        }
        Method transition = findMethod(state.getClass(), "goToState", String.class);
        if (transition == null) {
            return;
        }
        try {
            transition.setAccessible(true);
            transition.invoke(state, next);
            // Do not clear the marker: the watcher polls faster than a fade can
            // complete and would otherwise continuously restart goToState().
            System.setProperty(KEY, CONSUMED);
            if (!successLogged) {
                successLogged = true;
                System.out.println(
                        "Fixer: AppDriver render thread consumed queued state transition -> " + next);
            }
        } catch (Throwable t) {
            System.out.println(
                    "Fixer: AppDriver render-thread transition failed: " + t.getClass().getName()
                            + (t.getMessage() == null ? "" : ": " + t.getMessage()));
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?> arg) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredMethod(name, arg);
            } catch (NoSuchMethodException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
