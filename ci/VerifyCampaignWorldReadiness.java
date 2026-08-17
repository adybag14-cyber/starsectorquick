import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Regression gate: browser campaign success requires a real populated sector. */
public final class VerifyCampaignWorldReadiness {
    public static final class Economy {
        final List<Object> markets = new ArrayList<Object>();
        public List<Object> getMarketsCopy() { return markets; }
    }
    public static final class StarSystem {
        final List<Object> planets = new ArrayList<Object>();
        public List<Object> getPlanets() { return planets; }
    }
    public static final class Fleet {
        Object location;
        public Object getContainingLocation() { return location; }
    }
    public static final class Sector {
        final Economy economy = new Economy();
        final List<StarSystem> systems = new ArrayList<StarSystem>();
        final List<Object> factions = new ArrayList<Object>();
        Fleet fleet;
        public Economy getEconomy() { return economy; }
        public List<StarSystem> getStarSystems() { return systems; }
        public List<Object> getAllFactions() { return factions; }
        public Fleet getPlayerFleet() { return fleet; }
    }

    private static String issue(Method method, Sector sector, boolean requirePlayerLocation) throws Exception {
        return (String) method.invoke(null, sector, Boolean.valueOf(requirePlayerLocation));
    }

    public static void main(String[] args) throws Exception {
        Method method = Fixer.class.getDeclaredMethod(
                "describeCampaignWorldPopulationIssue", Object.class, Boolean.TYPE);
        method.setAccessible(true);

        Sector empty = new Sector();
        String emptyIssue = issue(method, empty, false);
        if (emptyIssue == null || emptyIssue.indexOf("systems=0") < 0 || emptyIssue.indexOf("markets=0") < 0) {
            throw new AssertionError("empty sector incorrectly playable: " + emptyIssue);
        }

        Sector full = new Sector();
        for (int i = 0; i < 24; i++) {
            StarSystem system = new StarSystem();
            system.planets.add(new Object());
            full.systems.add(system);
        }
        for (int i = 0; i < 8; i++) full.economy.markets.add(new Object());
        for (int i = 0; i < 8; i++) full.factions.add(new Object());
        full.fleet = new Fleet();
        full.fleet.location = new Object();
        if (issue(method, full, true) != null) {
            throw new AssertionError("populated sector rejected: " + issue(method, full, true));
        }

        full.fleet.location = null;
        String noLocation = issue(method, full, true);
        if (noLocation == null || noLocation.indexOf("playerLocation=false") < 0) {
            throw new AssertionError("missing player location incorrectly playable: " + noLocation);
        }
        if (issue(method, full, false) != null) {
            throw new AssertionError("pre-fleet transition should accept populated world without a location");
        }

        full.fleet.location = new Object();
        full.factions.remove(full.factions.size() - 1);
        String lowFactions = issue(method, full, true);
        if (lowFactions == null || lowFactions.indexOf("factions=7") < 0) {
            throw new AssertionError("under-populated factions incorrectly playable: " + lowFactions);
        }

        System.out.println("VerifyCampaignWorldReadiness: OK");
    }
}
