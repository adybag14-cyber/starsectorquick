package com.fs.starfarer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Moves watcher-requested campaign work onto BaseGameState.traverse(), i.e. the
 * thread that owns rendering and AppDriver state progression.
 *
 * This package contains an obfuscated com.fs.starfarer.String class, so all
 * references to the JDK string type are explicitly java.lang.String.
 */
public final class MainThreadTransitionBridge {
    private static final java.lang.String KEY = "starsector.pendingStateTransition";
    private static final java.lang.String TITLE_HANDOFF_KEY = "starsector.browserQuickTitleHandoff";
    private static final java.lang.String CONSUMED = "__consumed__";
    private static final Object INVOCATION_LOCK = new Object();

    private static Invocation pendingInvocation;
    private static Thread renderThread;
    private static boolean successLogged;
    private static boolean invocationWaitLogged;
    private static volatile boolean titleHandoffActive = Boolean.getBoolean(TITLE_HANDOFF_KEY);

    private MainThreadTransitionBridge() {}

    /** Return whether browser quick-start should suppress Title background combat. */
    public static boolean isTitleHandoffActive() {
        return titleHandoffActive;
    }

    /** Disable Title suppression after Campaign State is confirmed active. */
    public static void disableTitleHandoff() {
        titleHandoffActive = false;
    }

    /**
     * Submit a two-argument static method to the AppDriver/render thread.
     *
     * The timeout only applies while the task is waiting to be picked up. Once
     * the render thread begins the invocation, the caller waits for completion;
     * returning early would let Fixer clean up temporary campaign state while
     * CampaignGameManager.create() was still using it.
     */
    public static Object invokeOnRenderThread(
            Method method, Object arg0, Object arg1, long pickupTimeoutMs) throws Throwable {
        if (method == null) {
            throw new IllegalArgumentException("render-thread invocation method is null");
        }

        Thread current = Thread.currentThread();
        synchronized (INVOCATION_LOCK) {
            if (current != null && current == renderThread) {
                return invoke(method, arg0, arg1);
            }
            if (pendingInvocation != null && !pendingInvocation.done) {
                throw new IllegalStateException("render-thread invocation already pending");
            }

            Invocation task = new Invocation(method, arg0, arg1);
            pendingInvocation = task;
            invocationWaitLogged = false;
            INVOCATION_LOCK.notifyAll();
            System.out.println(
                    "Fixer: queued CampaignGameManager.create() for the AppDriver render thread.");

            long timeoutMs = Math.max(1000L, pickupTimeoutMs);
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!task.done) {
                if (!task.running) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0L) {
                        if (pendingInvocation == task) {
                            pendingInvocation = null;
                        }
                        throw new java.util.concurrent.TimeoutException(
                                "render thread did not pick up campaign create within "
                                        + timeoutMs
                                        + "ms");
                    }
                    try {
                        INVOCATION_LOCK.wait(Math.min(remaining, 1000L));
                    } catch (InterruptedException ie) {
                        if (pendingInvocation == task && !task.running) {
                            pendingInvocation = null;
                        }
                        throw ie;
                    }
                } else {
                    if (!invocationWaitLogged) {
                        invocationWaitLogged = true;
                        System.out.println(
                                "Fixer: CampaignGameManager.create() is executing on the AppDriver render thread; waiting for completion.");
                    }
                    try {
                        INVOCATION_LOCK.wait(1000L);
                    } catch (InterruptedException ie) {
                        // Once create() is executing, do not let the watcher unwind and clean
                        // temporary specs out from underneath the render-thread invocation.
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (pendingInvocation == task) {
                pendingInvocation = null;
            }
            if (task.error != null) {
                throw task.error;
            }
            return task.result;
        }
    }

    public static void drain(Object state) {
        drainInvocation();

        java.lang.String next = System.getProperty(KEY);
        if (next == null || next.length() == 0 || CONSUMED.equals(next) || state == null) {
            return;
        }
        Method transition = findMethod(state.getClass(), "goToState", java.lang.String.class);
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

    private static void drainInvocation() {
        Invocation task;
        synchronized (INVOCATION_LOCK) {
            renderThread = Thread.currentThread();
            task = pendingInvocation;
            if (task == null || task.done || task.running) {
                return;
            }
            task.running = true;
            INVOCATION_LOCK.notifyAll();
        }

        Object result = null;
        Throwable error = null;
        try {
            System.out.println(
                    "Fixer: AppDriver render thread executing queued CampaignGameManager.create().");
            result = invoke(task.method, task.arg0, task.arg1);
        } catch (Throwable t) {
            error = t;
        }

        synchronized (INVOCATION_LOCK) {
            task.result = result;
            task.error = error;
            task.done = true;
            INVOCATION_LOCK.notifyAll();
        }
    }

    private static Object invoke(Method method, Object arg0, Object arg1) throws Throwable {
        try {
            method.setAccessible(true);
            return method.invoke(null, arg0, arg1);
        } catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            throw cause == null ? wrapped : cause;
        }
    }

    private static Method findMethod(
            Class<?> type, java.lang.String name, Class<?> arg) {
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

    private static final class Invocation {
        final Method method;
        final Object arg0;
        final Object arg1;
        boolean running;
        boolean done;
        Object result;
        Throwable error;

        Invocation(Method method, Object arg0, Object arg1) {
            this.method = method;
            this.arg0 = arg0;
            this.arg1 = arg1;
        }
    }
}
