import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public final class XStreamSunUnsafeProbe {
    public static final class Model {
        boolean isFastForwardIteration;
        int nextValue;
        String label;
    }
    public static void main(String[] args) {
        final String xml = "<CampaignEngine><isFastForwardIteration>false</isFastForwardIteration><nextValue>7</nextValue><label>ok</label></CampaignEngine>";
        System.out.println("XStreamSunUnsafeProbe: START");
        final XStream xstream = new XStream(new StaxDriver());
        xstream.alias("CampaignEngine", Model.class);
        xstream.setMode(XStream.ID_REFERENCES);
        System.out.println("XStreamSunUnsafeProbe: provider=" + xstream.getReflectionProvider().getClass().getName());
        System.out.println("XStreamSunUnsafeProbe: fromXML-before");
        final Model model = (Model)xstream.fromXML(xml);
        System.out.println("XStreamSunUnsafeProbe: fromXML-after bool=" + model.isFastForwardIteration
                + " next=" + model.nextValue + " label=" + model.label);
        if (model.isFastForwardIteration || model.nextValue != 7 || !"ok".equals(model.label)) {
            throw new AssertionError("round-trip values wrong");
        }
        System.out.println("XStreamSunUnsafeProbe: DONE");
    }
}
