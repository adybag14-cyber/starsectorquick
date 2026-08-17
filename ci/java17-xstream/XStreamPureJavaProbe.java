import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public final class XStreamPureJavaProbe {
    public static final class Model {
        public boolean isFastForwardIteration;
        public int nextValue;
        public String label;
        public Model() {}
    }
    public static void main(String[] args) {
        configureJvmIdentity();
        final String xml = "<CampaignEngine><isFastForwardIteration>false</isFastForwardIteration><nextValue>7</nextValue><label>ok</label></CampaignEngine>";
        System.out.println("XStreamPureJavaProbe: START");
        final XStream xstream = new XStream(new PureJavaReflectionProvider(), new StaxDriver());
        xstream.alias("CampaignEngine", Model.class);
        xstream.setMode(XStream.ID_REFERENCES);
        System.out.println("XStreamPureJavaProbe: provider=" + xstream.getReflectionProvider().getClass().getName());
        System.out.println("XStreamPureJavaProbe: fromXML-before");
        final Model model = (Model)xstream.fromXML(xml);
        System.out.println("XStreamPureJavaProbe: fromXML-after bool=" + model.isFastForwardIteration
                + " next=" + model.nextValue + " label=" + model.label);
        if (model.isFastForwardIteration || model.nextValue != 7 || !"ok".equals(model.label)) {
            throw new AssertionError("round-trip values wrong");
        }
        System.out.println("XStreamPureJavaProbe: DONE");
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
