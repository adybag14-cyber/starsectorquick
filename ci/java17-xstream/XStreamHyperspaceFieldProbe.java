import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public final class XStreamHyperspaceFieldProbe {
    public static final class Wrapper {
        com.fs.starfarer.campaign.Hyperspace hyperspace;
    }

    public static void main(String[] args) throws Exception {
        configureJvmIdentity();
        System.out.println("XStreamHyperspaceFieldProbe: START");
        final XStream xstream = new XStream(new StaxDriver());
        xstream.alias("Wrapper", Wrapper.class);
        xstream.setMode(XStream.ID_REFERENCES);
        System.out.println("XStreamHyperspaceFieldProbe: provider="
                + xstream.getReflectionProvider().getClass().getName());
        final String xml = "<Wrapper><hyperspace></hyperspace></Wrapper>";
        System.out.println("XStreamHyperspaceFieldProbe: fromXML-before");
        final Wrapper result = (Wrapper) xstream.fromXML(xml);
        System.out.println("XStreamHyperspaceFieldProbe: fromXML-after hyperspace="
                + (result == null || result.hyperspace == null
                        ? "<null>" : result.hyperspace.getClass().getName()));
        if (result == null || result.hyperspace == null) {
            throw new AssertionError("Hyperspace field was not restored");
        }
        System.out.println("XStreamHyperspaceFieldProbe: DONE");
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
