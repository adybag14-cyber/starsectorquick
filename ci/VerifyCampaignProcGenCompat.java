import com.fs.starfarer.CampaignInitCompat;
import com.fs.starfarer.api.campaign.SectorProcGenPlugin;
import java.lang.reflect.Proxy;

/** Proves browser campaign procgen delegates to stock generation unless explicitly disabled. */
public final class VerifyCampaignProcGenCompat {
    public static void main(String[] args) throws Exception {
        final int[] generateCalls = new int[] {0};
        SectorProcGenPlugin plugin = (SectorProcGenPlugin) Proxy.newProxyInstance(
                SectorProcGenPlugin.class.getClassLoader(),
                new Class<?>[] {SectorProcGenPlugin.class},
                (proxy, method, methodArgs) -> {
                    if ("generate".equals(method.getName())) {
                        generateCalls[0]++;
                        return null;
                    }
                    if ("toString".equals(method.getName())) return "ProcGenProbe";
                    if ("hashCode".equals(method.getName())) return Integer.valueOf(1);
                    if ("equals".equals(method.getName())) return Boolean.valueOf(proxy == methodArgs[0]);
                    return null;
                });

        System.clearProperty("starsector.browserSkipOuterSectorProcGen");
        CampaignInitCompat.generateSectorProcGen(plugin, null, null);
        if (generateCalls[0] != 1) {
            throw new AssertionError("full-map default did not delegate to stock procgen; calls=" + generateCalls[0]);
        }

        System.setProperty("starsector.browserSkipOuterSectorProcGen", "true");
        try {
            CampaignInitCompat.generateSectorProcGen(plugin, null, null);
            if (generateCalls[0] != 1) {
                throw new AssertionError("explicit diagnostic skip still invoked procgen; calls=" + generateCalls[0]);
            }
        } finally {
            System.clearProperty("starsector.browserSkipOuterSectorProcGen");
        }

        boolean nullRejected = false;
        try {
            CampaignInitCompat.generateSectorProcGen(null, null, null);
        } catch (IllegalStateException expected) {
            nullRejected = true;
        }
        if (!nullRejected) throw new AssertionError("null procgen plugin silently produced a partial world");

        System.out.println("VerifyCampaignProcGenCompat: OK full-map delegation calls=" + generateCalls[0]);
    }
}
