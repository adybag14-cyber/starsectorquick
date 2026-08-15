import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Verifies direct browser quick-start mirrors the standard tutorial-complete ability bar. */
public final class VerifyStartingAbilities {
    private static final String[] EXPECTED = {
        "transponder", "go_dark", "sensor_burst", "emergency_burn",
        "sustained_burn", "scavenge", "interdiction_pulse", "distress_call"
    };

    public static final class CharacterData {
        final Set<String> abilities = new LinkedHashSet<String>();
        public void addAbility(String id) { abilities.add(id); }
    }
    public static final class Slot {
        String abilityId;
        public String getAbilityId() { return abilityId; }
        public void setAbilityId(String id) { abilityId = id; }
    }
    public static final class Slots {
        int bar;
        final List<Slot> values = new ArrayList<Slot>();
        Slots() { for (int i = 0; i < 10; i++) values.add(new Slot()); }
        public int getCurrBarIndex() { return bar; }
        public void setCurrBarIndex(int value) { bar = value; }
        public List<Slot> getCurrSlotsCopy() { return values; }
    }
    public static final class UIData {
        final Slots slots = new Slots();
        public Slots getAbilitySlotsAPI() { return slots; }
    }
    public static final class Sector {
        final CharacterData characterData = new CharacterData();
        final UIData uiData = new UIData();
        public CharacterData getCharacterData() { return characterData; }
        public UIData getUIData() { return uiData; }
    }
    public static final class Fleet {
        final Map<String,Object> abilities = new LinkedHashMap<String,Object>();
        public void addAbility(String id) { if (!abilities.containsKey(id)) abilities.put(id, new Object()); }
        public Object getAbility(String id) { return abilities.get(id); }
    }

    public static void main(String[] args) throws Exception {
        Sector sector = new Sector();
        Fleet fleet = new Fleet();
        Method method = Fixer.class.getDeclaredMethod("ensureAutoCampaignStarterAbilities", Object.class, Object.class);
        method.setAccessible(true);
        Object result = method.invoke(null, sector, fleet);
        if (!Boolean.TRUE.equals(result)) throw new AssertionError("starter ability setup returned " + result);
        for (int i = 0; i < EXPECTED.length; i++) {
            String id = EXPECTED[i];
            if (!sector.characterData.abilities.contains(id)) throw new AssertionError("character missing " + id);
            if (fleet.getAbility(id) == null) throw new AssertionError("fleet missing " + id);
            String mapped = sector.uiData.slots.values.get(i).getAbilityId();
            if (!id.equals(mapped)) throw new AssertionError("slot " + (i + 1) + " expected=" + id + " actual=" + mapped);
        }
        if (sector.characterData.abilities.size() != EXPECTED.length || fleet.abilities.size() != EXPECTED.length) {
            throw new AssertionError("unexpected ability counts character=" + sector.characterData.abilities.size() + " fleet=" + fleet.abilities.size());
        }
        System.out.println("VerifyStartingAbilities: OK slots=1-8 abilities=" + String.join(",", EXPECTED));
    }
}
