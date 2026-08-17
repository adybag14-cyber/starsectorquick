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
}
