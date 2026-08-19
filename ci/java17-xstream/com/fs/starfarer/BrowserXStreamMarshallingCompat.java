package com.fs.starfarer;

import com.fs.starfarer.launcher.ModManager;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.BrowserFastReferenceByIdMarshallingStrategy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Selects the exact-tested path-light ID marshaller for stock browser saves only. */
public final class BrowserXStreamMarshallingCompat {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserXstreamFastIdMarshaller";
    private static final AtomicBoolean ENABLED_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean MODDED_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean FAILED_LOGGED = new AtomicBoolean();

    private BrowserXStreamMarshallingCompat() {}

    public static void install(XStream xstream) {
        if (xstream == null || !Boolean.getBoolean(ENABLE_PROPERTY)) return;
        try {
            List<ModManager.ModSpec> enabled = ModManager.getInstance().getEnabledMods();
            if (enabled != null && !enabled.isEmpty()) {
                if (MODDED_LOGGED.compareAndSet(false, true)) {
                    System.out.println("BrowserXStreamFastId: stock fast path disabled because external mods are enabled count="
                            + enabled.size());
                }
                return;
            }
            xstream.setMarshallingStrategy(new BrowserFastReferenceByIdMarshallingStrategy());
            if (ENABLED_LOGGED.compareAndSet(false, true)) {
                System.out.println("BrowserXStreamFastId: enabled path-light ID-reference marshaller");
            }
        } catch (Throwable error) {
            if (FAILED_LOGGED.compareAndSet(false, true)) {
                System.out.println("BrowserXStreamFastId: preserving stock strategy after setup failure: "
                        + error.getClass().getName()
                        + (error.getMessage() == null ? "" : ": " + error.getMessage()));
            }
        }
    }
}
