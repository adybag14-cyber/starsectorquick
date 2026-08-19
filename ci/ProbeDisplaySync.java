import org.lwjgl.opengl.Display;

public final class ProbeDisplaySync {
    public static void main(String[] args) {
        System.setProperty("starsector.browserFramePacing", "true");
        long pacedStart = System.nanoTime();
        for (int i = 0; i < 7; i++) {
            Display.sync(60);
        }
        long pacedMs = (System.nanoTime() - pacedStart) / 1000000L;
        if (pacedMs < 70L || pacedMs > 2000L) {
            throw new AssertionError("60 Hz pacing window unexpected: " + pacedMs + "ms");
        }

        System.setProperty("starsector.browserFramePacing", "false");
        long disabledStart = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            Display.sync(60);
        }
        long disabledMs = (System.nanoTime() - disabledStart) / 1000000L;
        if (disabledMs > 500L) {
            throw new AssertionError("disabled pacing still waits: " + disabledMs + "ms");
        }

        System.out.println("ProbeDisplaySync: OK pacedMs=" + pacedMs + " disabledMs=" + disabledMs);
    }
}
