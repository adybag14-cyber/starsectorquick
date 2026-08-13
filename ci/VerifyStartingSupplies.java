/** Verifies the browser new-game supplies top-up without booting the full campaign. */
public final class VerifyStartingSupplies {
    public static final class Cargo {
        private float supplies;
        private final float maxCapacity;
        Cargo(float maxCapacity) { this.maxCapacity = maxCapacity; }
        public float getSupplies() { return supplies; }
        public void addSupplies(float value) { supplies += value; }
        public float getMaxCapacity() { return maxCapacity; }
    }

    public static final class Data {
        final Cargo cargo;
        Data(float maxCapacity) { cargo = new Cargo(maxCapacity); }
        public Cargo getStartingCargo() { return cargo; }
    }

    public static void main(String[] args) throws Exception {
        java.lang.reflect.Method seed =
                Fixer.class.getDeclaredMethod("seedAutoCampaignStartingCargo", Object.class);
        seed.setAccessible(true);

        Data lasher = new Data(40f);
        seed.invoke(null, lasher);
        assertSupplies(lasher, 30f, "40-capacity starter");
        seed.invoke(null, lasher);
        assertSupplies(lasher, 30f, "idempotent 40-capacity starter");

        Data largerFleet = new Data(200f);
        seed.invoke(null, largerFleet);
        assertSupplies(largerFleet, 80f, "larger starter fleet ceiling");

        System.out.println(
                "VerifyStartingSupplies: OK lasher=" + lasher.cargo.getSupplies()
                        + " largerFleet=" + largerFleet.cargo.getSupplies());
    }

    private static void assertSupplies(Data data, float expected, String label) {
        float actual = data.cargo.getSupplies();
        if (Math.abs(actual - expected) > 0.01f) {
            throw new AssertionError(label + " expected " + expected + " supplies, got " + actual);
        }
    }
}
