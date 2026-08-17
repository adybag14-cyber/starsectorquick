import com.thoughtworks.xstream.core.util.FastField;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public final class XStreamCampaignEngineFastFieldProbe {
    public static void main(String[] args) throws Exception {
        configureJvmIdentity();
        System.out.println("XStreamCampaignEngineFastFieldProbe: START");
        final Class<?> type = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
        System.out.println("XStreamCampaignEngineFastFieldProbe: class-loaded name=" + type.getName());
        final Field field = type.getDeclaredField("isFastForwardIteration");
        System.out.println("XStreamCampaignEngineFastFieldProbe: field-resolved name=" + field.getName());
        System.out.println("XStreamCampaignEngineFastFieldProbe: declaring-before");
        final Class<?> definedIn = field.getDeclaringClass();
        System.out.println("XStreamCampaignEngineFastFieldProbe: declaring-after name=" + definedIn.getName());
        System.out.println("XStreamCampaignEngineFastFieldProbe: fastfield-before");
        final FastField fast = new FastField(definedIn, field.getName());
        System.out.println("XStreamCampaignEngineFastFieldProbe: fastfield-after hash=" + fast.hashCode());
        final Set<FastField> seen = new HashSet<FastField>();
        System.out.println("XStreamCampaignEngineFastFieldProbe: hashset-add-before");
        final boolean added = seen.add(fast);
        System.out.println("XStreamCampaignEngineFastFieldProbe: hashset-add-after added=" + added + " size=" + seen.size());
        if (!added || seen.size() != 1 || !seen.contains(new FastField(type, "isFastForwardIteration"))) {
            throw new AssertionError("FastField/HashSet semantics failed");
        }
        System.out.println("XStreamCampaignEngineFastFieldProbe: DONE");
    }

    private static void configureJvmIdentity() {
        setIfMissing("java.vm.vendor", "CheerpJ");
        setIfMissing("java.vendor", "CheerpJ");
        setIfMissing("java.vm.name", "CheerpJ Runtime");
        setIfMissing("java.specification.version", "1.8");
        setIfMissing("java.specification.vendor", "Oracle Corporation");
        setIfMissing("java.specification.name", "Java Platform API Specification");
    }

    private static void setIfMissing(final String key, final String value) {
        final String current = System.getProperty(key);
        if (current == null || current.trim().length() == 0) System.setProperty(key, value);
    }
}
