import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fs.starfarer.BrowserZeroIndentXMLStreamWriter;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxWriter;

public final class VerifyBrowserZeroIndentWriter {
    private static final class Graph {
        String name = "alpha<&>\"'";
        List<Object> list = new ArrayList<Object>();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
    }

    private static final class SaveDriver extends StaxDriver {
        private final boolean specialized;
        SaveDriver(boolean specialized) { this.specialized = specialized; }

        @Override
        public HierarchicalStreamWriter createWriter(OutputStream out) {
            try {
                final OutputStreamWriter text = new OutputStreamWriter(out, "UTF-8");
                final XMLStreamWriter raw = getOutputFactory().createXMLStreamWriter(text);
                final IndentingXMLStreamWriter xml = specialized
                        ? new BrowserZeroIndentXMLStreamWriter(raw)
                        : new IndentingXMLStreamWriter(raw);
                xml.setIndentStep("");
                return new StaxWriter(getQnameMap(), xml, true, isRepairingNamespace());
            } catch (Exception e) {
                throw new StreamException(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final Graph graph = new Graph();
        graph.list.add("first");
        graph.list.add(graph.name);
        graph.map.put("name", graph.name);
        graph.map.put("list", graph.list);
        graph.list.add(graph.map);

        final String stock = render(graph, false, false);
        final String compatOff = render(graph, true, false);
        final String compatOn = render(graph, true, true);
        requireExact("compat disabled", stock, compatOff);
        requireExact("compat enabled", stock, compatOn);

        final String rawStock = exerciseRawWriter(false, false);
        final String rawOff = exerciseRawWriter(true, false);
        final String rawOn = exerciseRawWriter(true, true);
        requireExact("raw compat disabled", rawStock, rawOff);
        requireExact("raw compat enabled", rawStock, rawOn);

        System.out.println("zero-indent-xml-sha256=" + sha256(stock));
        System.out.println("VerifyBrowserZeroIndentWriter: OK bytes=" + stock.getBytes(StandardCharsets.UTF_8).length);
    }

    private static String render(Graph graph, boolean specialized, boolean enabled) throws Exception {
        System.setProperty("starsector.browserZeroIndentXmlWriter", Boolean.toString(enabled));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final XStream xstream = new XStream(new SaveDriver(specialized));
        xstream.toXML(graph, out);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String exerciseRawWriter(boolean specialized, boolean enabled) throws Exception {
        System.setProperty("starsector.browserZeroIndentXmlWriter", Boolean.toString(enabled));
        final java.io.StringWriter out = new java.io.StringWriter();
        final XMLStreamWriter raw = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
        final IndentingXMLStreamWriter xml = specialized
                ? new BrowserZeroIndentXMLStreamWriter(raw)
                : new IndentingXMLStreamWriter(raw);
        xml.setIndentStep("");
        xml.writeStartDocument("UTF-8", "1.0");
        xml.writeStartElement("root");
        xml.writeAttribute("a", "1&2");
        xml.writeStartElement("text");
        xml.writeCharacters("hello<&>");
        xml.writeEndElement();
        xml.writeStartElement("container");
        xml.writeEmptyElement("empty");
        xml.writeStartElement("cdata");
        xml.writeCData("x<y&z");
        xml.writeEndElement();
        xml.writeEndElement();
        xml.writeEndElement();
        xml.writeEndDocument();
        xml.flush();
        return out.toString();
    }

    private static void requireExact(String label, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " output differs\nEXPECTED:\n" + expected + "\nACTUAL:\n" + actual);
        }
    }

    private static String sha256(String value) throws Exception {
        final byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        final StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }
}
