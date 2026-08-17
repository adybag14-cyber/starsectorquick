import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.core.util.FastField;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public final class XStreamPostFieldProbe {
    public static final class Model {
        boolean isFastForwardIteration;
        int nextValue;
    }

    public static void main(String[] args) {
        configureJvmIdentity();
        final String xml = "<CampaignEngine><isFastForwardIteration>false</isFastForwardIteration><nextValue>7</nextValue></CampaignEngine>";
        System.out.println("XStreamPostFieldProbe: START");
        final HierarchicalStreamReader reader = new StaxDriver().createReader(new StringReader(xml));
        final SunUnsafeReflectionProvider provider = new SunUnsafeReflectionProvider();
        final Model model = new Model();
        final Set<FastField> seen = new HashSet<FastField>();
        reader.moveDown();
        System.out.println("XStreamPostFieldProbe: getValue-before node=" + reader.getNodeName());
        final String text = reader.getValue();
        System.out.println("XStreamPostFieldProbe: getValue-after value=" + text);
        System.out.println("XStreamPostFieldProbe: writeField-before");
        provider.writeField(model, "isFastForwardIteration", Boolean.valueOf(text), Model.class);
        System.out.println("XStreamPostFieldProbe: writeField-after value=" + model.isFastForwardIteration);
        System.out.println("XStreamPostFieldProbe: fastField-add-before");
        seen.add(new FastField(Model.class, "isFastForwardIteration"));
        System.out.println("XStreamPostFieldProbe: fastField-add-after size=" + seen.size());
        System.out.println("XStreamPostFieldProbe: moveUp-before");
        reader.moveUp();
        System.out.println("XStreamPostFieldProbe: moveUp-after node=" + reader.getNodeName());
        System.out.println("XStreamPostFieldProbe: hasMore-before");
        final boolean more = reader.hasMoreChildren();
        System.out.println("XStreamPostFieldProbe: hasMore-after value=" + more);
        if (!more) throw new AssertionError("expected second child");
        System.out.println("XStreamPostFieldProbe: moveDown2-before");
        reader.moveDown();
        System.out.println("XStreamPostFieldProbe: moveDown2-after node=" + reader.getNodeName() + " value=" + reader.getValue());
        reader.close();
        System.out.println("XStreamPostFieldProbe: DONE");
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
