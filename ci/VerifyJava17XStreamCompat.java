import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.Fields;
import com.thoughtworks.xstream.core.util.SerializationMembers;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public final class VerifyJava17XStreamCompat {
    private enum Example { A, B, C }

    private static class BaseValues {
        private long inheritedLong = 0x123456789abcdefL;
    }

    private static final class Values extends BaseValues {
        private Object objectValue = "object-value";
        private String stringValue = "string-value";
        private int intValue = 0x13579bdf;
        private long longValue = 0x1020304050607080L;
        private short shortValue = (short)0x6a5b;
        private char charValue = '\ud7ff';
        private byte byteValue = (byte)0xa5;
        private float floatValue = Float.intBitsToFloat(0x7f012345);
        private double doubleValue = Double.longBitsToDouble(0x7fe0123456789abCL);
        private boolean booleanValue = true;
        private volatile int volatileInt = 0x2468ace0;
        private volatile Object volatileObject = "volatile-object";
        private final double finalDouble = Double.longBitsToDouble(0x00123456789abcdeL);
    }

    private static final class Graph {
        private Values values;
        private List<Object> list;
        private Map<String, Object> map;
        private EnumSet<Example> set;
        private EnumMap<Example, String> enumMap;
    }

    public static void main(String[] args) throws Exception {
        final boolean expectedFastPath = Boolean.parseBoolean(
                System.getProperty("starsector.browserXstreamUnsafeReadFastPath", "false"));
        if (Fields.isUnsafeReadFastPathEnabled() != expectedFastPath) {
            throw new AssertionError("Unsafe-read fast-path property mismatch");
        }

        final SerializationMembers members = new SerializationMembers();
        final EnumSet<Example> emptySet = EnumSet.noneOf(Example.class);
        if (members.callWriteReplace(emptySet) != emptySet) {
            throw new AssertionError("module-blocked EnumSet.writeReplace should be treated as unavailable");
        }

        final Values values = new Values();
        verifyFieldReads(values);

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

        final Graph graph = new Graph();
        graph.values = values;
        graph.list = new ArrayList<Object>();
        graph.list.add(values);
        graph.list.add("tail");
        graph.list.add(values);
        graph.map = new LinkedHashMap<String, Object>();
        graph.map.put("first", values);
        graph.map.put("list", graph.list);
        graph.map.put("again", values);
        graph.set = EnumSet.of(Example.A, Example.C);
        graph.enumMap = populatedMap;
        final String graphXml = xstream.toXML(graph);
        if (graphXml.indexOf("reference=") < 0) {
            throw new AssertionError("shared-reference graph was not preserved: " + graphXml);
        }
        System.out.println("graph-xml-sha256=" + sha256(graphXml));
        System.out.println("VerifyJava17XStreamCompat: OK");
    }

    private static void verifyFieldReads(final Values values) throws Exception {
        requireSame("objectValue", "object-value", read(Values.class, "objectValue", values));
        requireSame("stringValue", "string-value", read(Values.class, "stringValue", values));
        requireSame("intValue", Integer.valueOf(0x13579bdf), read(Values.class, "intValue", values));
        requireSame("longValue", Long.valueOf(0x1020304050607080L), read(Values.class, "longValue", values));
        requireSame("shortValue", Short.valueOf((short)0x6a5b), read(Values.class, "shortValue", values));
        requireSame("charValue", Character.valueOf('\ud7ff'), read(Values.class, "charValue", values));
        requireSame("byteValue", Byte.valueOf((byte)0xa5), read(Values.class, "byteValue", values));
        requireSame("booleanValue", Boolean.TRUE, read(Values.class, "booleanValue", values));
        requireSame("volatileInt", Integer.valueOf(0x2468ace0), read(Values.class, "volatileInt", values));
        requireSame("volatileObject", "volatile-object", read(Values.class, "volatileObject", values));
        requireSame("inheritedLong", Long.valueOf(0x123456789abcdefL),
                read(BaseValues.class, "inheritedLong", values));

        final Float floatRead = (Float)read(Values.class, "floatValue", values);
        if (Float.floatToRawIntBits(floatRead.floatValue()) != 0x7f012345) {
            throw new AssertionError("floatValue raw bits changed");
        }
        final Double doubleRead = (Double)read(Values.class, "doubleValue", values);
        if (Double.doubleToRawLongBits(doubleRead.doubleValue()) != 0x7fe0123456789abCL) {
            throw new AssertionError("doubleValue raw bits changed");
        }
        final Double finalDoubleRead = (Double)read(Values.class, "finalDouble", values);
        if (Double.doubleToRawLongBits(finalDoubleRead.doubleValue()) != 0x00123456789abcdeL) {
            throw new AssertionError("finalDouble raw bits changed");
        }
    }

    private static Object read(final Class<?> owner, final String name, final Object instance) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        final Object reflected = field.get(instance);
        final Object compat = Fields.read(field, instance);
        if (reflected instanceof Float) {
            if (Float.floatToRawIntBits(((Float)reflected).floatValue())
                    != Float.floatToRawIntBits(((Float)compat).floatValue())) {
                throw new AssertionError(name + " differs from reflection");
            }
        } else if (reflected instanceof Double) {
            if (Double.doubleToRawLongBits(((Double)reflected).doubleValue())
                    != Double.doubleToRawLongBits(((Double)compat).doubleValue())) {
                throw new AssertionError(name + " differs from reflection");
            }
        } else if (reflected == null ? compat != null : !reflected.equals(compat)) {
            throw new AssertionError(name + " differs from reflection: reflected=" + reflected + " compat=" + compat);
        }
        return compat;
    }

    private static void requireSame(final String label, final Object expected, final Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireEnumType(final String xml, final String label) {
        if (xml == null || xml.indexOf("enum-type=") < 0) {
            throw new AssertionError(label + " did not use XStream's dedicated enum converter: " + xml);
        }
    }

    private static String sha256(final String value) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        final StringBuilder out = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            out.append(String.format("%02x", bytes[i] & 0xff));
        }
        return out.toString();
    }
}
