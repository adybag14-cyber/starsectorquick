/** Verifies the browser direct-start resource repair without launching Campaign. */
public final class VerifyPlayableStartingResources {
    public static final class Credits {
        float value;
        public float get() { return value; }
        public void add(float amount) { value += amount; }
    }
    public static final class Cargo {
        float supplies;
        float fuel;
        int crew;
        final float maxCapacity;
        final float maxFuel;
        final float maxPersonnel;
        final float otherCargo;
        final Credits credits = new Credits();
        Cargo(float maxCapacity, float maxFuel, float maxPersonnel, float otherCargo) {
            this.maxCapacity=maxCapacity; this.maxFuel=maxFuel; this.maxPersonnel=maxPersonnel; this.otherCargo=otherCargo;
        }
        public float getSupplies(){ return supplies; }
        public void addSupplies(float v){ supplies += v; }
        public void removeSupplies(float v){ supplies = Math.max(0f, supplies - v); }
        public float getMaxCapacity(){ return maxCapacity; }
        public float getSpaceUsed(){ return otherCargo + supplies; }
        public float getFuel(){ return fuel; }
        public void addFuel(float v){ fuel = Math.min(maxFuel, fuel + v); }
        public float getMaxFuel(){ return maxFuel; }
        public int getCrew(){ return crew; }
        public void addCrew(int v){ crew = Math.min((int)maxPersonnel, crew + v); }
        public float getMaxPersonnel(){ return maxPersonnel; }
        public Credits getCredits(){ return credits; }
    }
    public static final class FleetData {
        final float minCrew;
        FleetData(float minCrew){ this.minCrew=minCrew; }
        public float getMinCrew(){ return minCrew; }
    }
    public static final class Fleet {
        final Cargo cargo;
        final FleetData data;
        Fleet(Cargo cargo, float minCrew){ this.cargo=cargo; data=new FleetData(minCrew); }
        public Cargo getCargo(){ return cargo; }
        public FleetData getFleetData(){ return data; }
    }
    public static void main(String[] args) throws Exception {
        java.lang.reflect.Method balance = Fixer.class.getDeclaredMethod(
                "balanceAutoCampaignPlayerResources", Object.class, Object.class);
        balance.setAccessible(true);

        Cargo cargo = new Cargo(40f, 50f, 20f, 20f);
        Fleet fleet = new Fleet(cargo, 10f);
        boolean ready = ((Boolean)balance.invoke(null, fleet, cargo)).booleanValue();
        check(ready, "normal starter was not made playable");
        near(cargo.supplies, 20f, "supplies bounded by remaining cargo");
        near(cargo.fuel, 37.5f, "75% fuel target");
        check(cargo.crew == 18, "crew target expected 18 got " + cargo.crew);
        near(cargo.credits.value, 2000f, "credits target");
        check(cargo.getSpaceUsed() <= cargo.maxCapacity + 0.01f, "starter cargo overflowed");

        float s=cargo.supplies, f=cargo.fuel, cr=cargo.credits.value; int crew=cargo.crew;
        ready = ((Boolean)balance.invoke(null, fleet, cargo)).booleanValue();
        check(ready, "idempotent pass became unready");
        near(cargo.supplies,s,"idempotent supplies"); near(cargo.fuel,f,"idempotent fuel");
        near(cargo.credits.value,cr,"idempotent credits"); check(cargo.crew==crew,"idempotent crew");

        Cargo over = new Cargo(40f, 50f, 20f, 20f);
        over.supplies = 35f;
        Fleet overFleet = new Fleet(over, 10f);
        ready = ((Boolean)balance.invoke(null, overFleet, over)).booleanValue();
        check(ready, "overfilled starter was not repaired");
        near(over.supplies,20f,"overflow trim preserves non-supply cargo");
        check(over.getSpaceUsed() <= over.maxCapacity + 0.01f,"overflow remained after repair");

        System.out.println("VerifyPlayableStartingResources: OK supplies="+cargo.supplies
                +" fuel="+cargo.fuel+" crew="+cargo.crew+" credits="+cargo.credits.value
                +" cargo="+cargo.getSpaceUsed()+"/"+cargo.maxCapacity);
    }
    static void check(boolean v,String m){ if(!v) throw new AssertionError(m); }
    static void near(float a,float e,String m){ if(Math.abs(a-e)>0.01f) throw new AssertionError(m+" expected="+e+" actual="+a); }
}
