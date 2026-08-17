package javax.xml.bind;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Calendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public final class DatatypeConverter {
    private static volatile DatatypeFactory datatypeFactory;

    private DatatypeConverter() {}

    private static DatatypeFactory getDatatypeFactory() {
        DatatypeFactory local = datatypeFactory;
        if (local != null) {
            return local;
        }
        try {
            local = DatatypeFactory.newInstance();
            datatypeFactory = local;
            return local;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize DatatypeFactory", e);
        }
    }

    private static String safe(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value == null");
        }
        return value;
    }

    public static String parseString(String lexicalXSDString) {
        return safe(lexicalXSDString);
    }

    public static BigInteger parseInteger(String lexicalXSDInteger) {
        return new BigInteger(safe(lexicalXSDInteger).trim());
    }

    public static int parseInt(String lexicalXSDInt) {
        return Integer.parseInt(safe(lexicalXSDInt).trim());
    }

    public static long parseLong(String lexicalXSDLong) {
        return Long.parseLong(safe(lexicalXSDLong).trim());
    }

    public static short parseShort(String lexicalXSDShort) {
        return Short.parseShort(safe(lexicalXSDShort).trim());
    }

    public static BigDecimal parseDecimal(String lexicalXSDDecimal) {
        return new BigDecimal(safe(lexicalXSDDecimal).trim());
    }

    public static float parseFloat(String lexicalXSDFloat) {
        return Float.parseFloat(safe(lexicalXSDFloat).trim());
    }

    public static double parseDouble(String lexicalXSDDouble) {
        return Double.parseDouble(safe(lexicalXSDDouble).trim());
    }

    public static boolean parseBoolean(String lexicalXSDBoolean) {
        String value = safe(lexicalXSDBoolean).trim();
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean: " + lexicalXSDBoolean);
    }

    public static byte parseByte(String lexicalXSDByte) {
        return Byte.parseByte(safe(lexicalXSDByte).trim());
    }

    public static QName parseQName(String lexicalXSDQName, NamespaceContext nsc) {
        String value = safe(lexicalXSDQName).trim();
        int colon = value.indexOf(':');
        if (colon > 0 && colon < (value.length() - 1) && nsc != null) {
            String prefix = value.substring(0, colon);
            String local = value.substring(colon + 1);
            String uri = nsc.getNamespaceURI(prefix);
            if (uri != null) {
                return new QName(uri, local, prefix);
            }
        }
        if (value.startsWith("{")) {
            return QName.valueOf(value);
        }
        return new QName(value);
    }

    public static Calendar parseDateTime(String lexicalXSDDateTime) {
        XMLGregorianCalendar xgc =
                getDatatypeFactory().newXMLGregorianCalendar(safe(lexicalXSDDateTime).trim());
        return xgc.toGregorianCalendar();
    }

    public static byte[] parseBase64Binary(String lexicalXSDBase64Binary) {
        String value = safe(lexicalXSDBase64Binary).trim();
        if (value.length() == 0) {
            return new byte[0];
        }

        // Starsector's tiled-terrain codec Base64-encodes each deflater output
        // chunk independently and concatenates those chunks. JAXB's decoder is
        // permissive enough for that representation, including padding in the
        // middle of the overall string. java.util.Base64.Decoder is not. Decode
        // one quartet at a time so every padded chunk boundary is preserved.
        ByteArrayOutputStream out = new ByteArrayOutputStream((value.length() * 3) / 4);
        int[] quad = new int[4];
        int q = 0;
        int pad = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '=') {
                quad[q++] = 0;
                pad++;
            } else {
                int decoded = decodeBase64Char(ch);
                if (decoded < 0) {
                    continue;
                }
                quad[q++] = decoded;
            }

            if (q == 4) {
                int chunk =
                        (quad[0] << 18)
                                | (quad[1] << 12)
                                | (quad[2] << 6)
                                | quad[3];
                out.write((chunk >>> 16) & 0xFF);
                if (pad < 2) {
                    out.write((chunk >>> 8) & 0xFF);
                }
                if (pad == 0) {
                    out.write(chunk & 0xFF);
                }
                q = 0;
                pad = 0;
            }
        }

        if (q == 2) {
            int chunk = (quad[0] << 18) | (quad[1] << 12);
            out.write((chunk >>> 16) & 0xFF);
        } else if (q == 3) {
            int chunk = (quad[0] << 18) | (quad[1] << 12) | (quad[2] << 6);
            out.write((chunk >>> 16) & 0xFF);
            out.write((chunk >>> 8) & 0xFF);
        } else if (q != 0) {
            throw new IllegalArgumentException("Invalid base64 length");
        }
        return out.toByteArray();
    }

    private static int decodeBase64Char(char ch) {
        if (ch >= 'A' && ch <= 'Z') return ch - 'A';
        if (ch >= 'a' && ch <= 'z') return (ch - 'a') + 26;
        if (ch >= '0' && ch <= '9') return (ch - '0') + 52;
        if (ch == '+' || ch == '-') return 62;
        if (ch == '/' || ch == '_') return 63;
        return -1;
    }

    public static byte[] parseHexBinary(String lexicalXSDHexBinary) {
        String value = safe(lexicalXSDHexBinary).trim();
        int len = value.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("hexBinary must have even length");
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(value.charAt(i), 16);
            int lo = Character.digit(value.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hexBinary: " + lexicalXSDHexBinary);
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }

    public static long parseUnsignedInt(String lexicalXSDUnsignedInt) {
        return Long.parseLong(safe(lexicalXSDUnsignedInt).trim());
    }

    public static int parseUnsignedShort(String lexicalXSDUnsignedShort) {
        return Integer.parseInt(safe(lexicalXSDUnsignedShort).trim());
    }

    public static Calendar parseTime(String lexicalXSDTime) {
        XMLGregorianCalendar xgc =
                getDatatypeFactory().newXMLGregorianCalendar(safe(lexicalXSDTime).trim());
        return xgc.toGregorianCalendar();
    }

    public static Calendar parseDate(String lexicalXSDDate) {
        XMLGregorianCalendar xgc =
                getDatatypeFactory().newXMLGregorianCalendar(safe(lexicalXSDDate).trim());
        return xgc.toGregorianCalendar();
    }

    public static String parseAnySimpleType(String lexicalXSDAnySimpleType) {
        return safe(lexicalXSDAnySimpleType);
    }

    public static String printString(String val) {
        return safe(val);
    }

    public static String printInteger(BigInteger val) {
        return val.toString();
    }

    public static String printInt(int val) {
        return Integer.toString(val);
    }

    public static String printLong(long val) {
        return Long.toString(val);
    }

    public static String printShort(short val) {
        return Short.toString(val);
    }

    public static String printDecimal(BigDecimal val) {
        return val.toPlainString();
    }

    public static String printFloat(float val) {
        return Float.toString(val);
    }

    public static String printDouble(double val) {
        return Double.toString(val);
    }

    public static String printBoolean(boolean val) {
        return Boolean.toString(val);
    }

    public static String printByte(byte val) {
        return Byte.toString(val);
    }

    public static String printQName(QName val, NamespaceContext nsc) {
        if (val == null) {
            throw new IllegalArgumentException("val == null");
        }
        String prefix = val.getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + ":" + val.getLocalPart();
        }
        return val.toString();
    }

    public static String printDateTime(Calendar val) {
        if (val == null) {
            throw new IllegalArgumentException("val == null");
        }
        java.util.GregorianCalendar gc;
        if (val instanceof java.util.GregorianCalendar) {
            gc = (java.util.GregorianCalendar) val;
        } else {
            gc = new java.util.GregorianCalendar();
            gc.setTimeInMillis(val.getTimeInMillis());
        }
        XMLGregorianCalendar xgc = getDatatypeFactory().newXMLGregorianCalendar(gc);
        return xgc.toXMLFormat();
    }

    public static String printBase64Binary(byte[] val) {
        if (val == null) {
            throw new IllegalArgumentException("val == null");
        }
        return Base64.getEncoder().encodeToString(val);
    }

    public static String printHexBinary(byte[] val) {
        if (val == null) {
            throw new IllegalArgumentException("val == null");
        }
        char[] out = new char[val.length * 2];
        final char[] hex = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < val.length; i++) {
            int b = val[i] & 0xFF;
            out[i * 2] = hex[b >>> 4];
            out[i * 2 + 1] = hex[b & 0x0F];
        }
        return new String(out);
    }

    public static String printUnsignedInt(long val) {
        return Long.toString(val);
    }

    public static String printUnsignedShort(int val) {
        return Integer.toString(val);
    }

    public static String printTime(Calendar val) {
        return printDateTime(val);
    }

    public static String printDate(Calendar val) {
        return printDateTime(val);
    }

    public static String printAnySimpleType(String val) {
        return safe(val);
    }
}
