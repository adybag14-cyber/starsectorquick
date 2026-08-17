import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import java.io.StringReader;

public final class XStreamStaxValueProbe {
    public static void main(String[] args) {
        final String xml = "<CampaignEngine><isFastForwardIteration>false</isFastForwardIteration><nextValue>7</nextValue></CampaignEngine>";
        System.out.println("XStreamStaxValueProbe: START");
        final HierarchicalStreamReader reader = new StaxDriver().createReader(new StringReader(xml));
        System.out.println("XStreamStaxValueProbe: root=" + reader.getNodeName());
        reader.moveDown();
        System.out.println("XStreamStaxValueProbe: first-node=" + reader.getNodeName());
        System.out.println("XStreamStaxValueProbe: getValue-before");
        final String value = reader.getValue();
        System.out.println("XStreamStaxValueProbe: getValue-after value=" + value);
        reader.moveUp();
        reader.moveDown();
        System.out.println("XStreamStaxValueProbe: second-node=" + reader.getNodeName() + " value=" + reader.getValue());
        reader.close();
        System.out.println("XStreamStaxValueProbe: DONE");
    }
}
