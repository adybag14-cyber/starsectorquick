import java.util.EnumMap;
import java.util.EnumSet;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.SerializationMembers;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public final class VerifyJava17XStreamCompat {
    private enum Example { A, B, C }

    public static void main(String[] args) {
        final SerializationMembers members = new SerializationMembers();
        final EnumSet<Example> emptySet = EnumSet.noneOf(Example.class);
        if (members.callWriteReplace(emptySet) != emptySet) {
            throw new AssertionError("module-blocked EnumSet.writeReplace should be treated as unavailable");
        }

        final XStream xstream = new XStream(new StaxDriver());
        final String populatedSetXml = xstream.toXML(EnumSet.of(Example.A, Example.C));
        final String emptySetXml = xstream.toXML(emptySet);

        final EnumMap<Example, String> populatedMap = new EnumMap<Example, String>(Example.class);
        populatedMap.put(Example.B, "ok");
        final String populatedMapXml = xstream.toXML(populatedMap);
        final String emptyMapXml = xstream.toXML(new EnumMap<Example, String>(Example.class));

        requireEnumType(populatedSetXml, "populated EnumSet");
        requireEnumType(emptySetXml, "empty EnumSet");
        requireEnumType(populatedMapXml, "populated EnumMap");
        requireEnumType(emptyMapXml, "empty EnumMap");
        System.out.println("VerifyJava17XStreamCompat: OK");
    }

    private static void requireEnumType(final String xml, final String label) {
        if (xml == null || xml.indexOf("enum-type=") < 0) {
            throw new AssertionError(label + " did not use XStream's dedicated enum converter: " + xml);
        }
    }
}
