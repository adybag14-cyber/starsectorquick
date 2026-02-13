package javax.xml.bind;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;

/**
 * Minimal JAXB DatatypeConverter compatibility shim for Java runtimes where
 * javax.xml.bind is absent (Java 9+). Only common methods are implemented.
 */
public final class DatatypeConverter {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private DatatypeConverter() {}

    public static String printString(String val) {
        return val;
    }

    public static String parseString(String val) {
        return val;
    }

    public static BigInteger parseInteger(String lexicalXSDInteger) {
        return new BigInteger(lexicalXSDInteger.trim());
    }

    public static int parseInt(String lexicalXSDInt) {
        return Integer.parseInt(lexicalXSDInt.trim());
    }

    public static long parseLong(String lexicalXSDLong) {
        return Long.parseLong(lexicalXSDLong.trim());
    }

    public static short parseShort(String lexicalXSDShort) {
        return Short.parseShort(lexicalXSDShort.trim());
    }

    public static byte parseByte(String lexicalXSDByte) {
        return Byte.parseByte(lexicalXSDByte.trim());
    }

    public static BigDecimal parseDecimal(String lexicalXSDDecimal) {
        return new BigDecimal(lexicalXSDDecimal.trim());
    }

    public static float parseFloat(String lexicalXSDFloat) {
        return Float.parseFloat(lexicalXSDFloat.trim());
    }

    public static double parseDouble(String lexicalXSDDouble) {
        return Double.parseDouble(lexicalXSDDouble.trim());
    }

    public static boolean parseBoolean(String lexicalXSDBoolean) {
        String v = lexicalXSDBoolean == null ? "" : lexicalXSDBoolean.trim();
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    public static int parseUnsignedInt(String lexicalXSDUnsignedInt) {
        long v = Long.parseLong(lexicalXSDUnsignedInt.trim());
        if (v < 0L || v > 0xFFFFFFFFL) {
            throw new NumberFormatException("unsigned int out of range: " + lexicalXSDUnsignedInt);
        }
        return (int) v;
    }

    public static short parseUnsignedByte(String lexicalXSDUnsignedByte) {
        int v = Integer.parseInt(lexicalXSDUnsignedByte.trim());
        if (v < 0 || v > 255) {
            throw new NumberFormatException("unsigned byte out of range: " + lexicalXSDUnsignedByte);
        }
        return (short) v;
    }

    public static byte[] parseHexBinary(String s) {
        if (s == null) {
            throw new IllegalArgumentException("hex string is null");
        }
        String v = s.trim();
        int len = v.length();
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("hex string length must be even");
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(v.charAt(i), 16);
            int lo = Character.digit(v.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex character in: " + s);
            }
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static String printHexBinary(byte[] data) {
        if (data == null) {
            return null;
        }
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            out[i * 2] = HEX[(b >>> 4) & 0x0F];
            out[i * 2 + 1] = HEX[b & 0x0F];
        }
        return new String(out);
    }

    public static byte[] parseBase64Binary(String lexicalXSDBase64Binary) {
        if (lexicalXSDBase64Binary == null) {
            throw new IllegalArgumentException("base64 string is null");
        }
        String v = lexicalXSDBase64Binary.trim();
        if (v.isEmpty()) {
            return new byte[0];
        }
        // JAXB DatatypeConverter behavior is permissive; implement tolerant decode
        // that accepts whitespace, URL-safe chars, missing padding, and trailing junk.
        ByteArrayOutputStream out = new ByteArrayOutputStream((v.length() * 3) / 4);
        int[] quad = new int[4];
        int q = 0;
        int pad = 0;
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if (ch == '=') {
                quad[q++] = 0;
                pad++;
            } else {
                int val = decodeBase64Char(ch);
                if (val < 0) {
                    continue;
                }
                quad[q++] = val;
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

        // Handle dangling partial chunk (common when padding is omitted).
        if (q == 2) {
            int chunk = (quad[0] << 18) | (quad[1] << 12);
            out.write((chunk >>> 16) & 0xFF);
        } else if (q == 3) {
            int chunk = (quad[0] << 18) | (quad[1] << 12) | (quad[2] << 6);
            out.write((chunk >>> 16) & 0xFF);
            out.write((chunk >>> 8) & 0xFF);
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

    public static String printBase64Binary(byte[] val) {
        if (val == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(val);
    }

    public static String printInt(int val) {
        return String.valueOf(val);
    }

    public static String printLong(long val) {
        return String.valueOf(val);
    }

    public static String printShort(short val) {
        return String.valueOf(val);
    }

    public static String printByte(byte val) {
        return String.valueOf(val);
    }

    public static String printDecimal(BigDecimal val) {
        return val == null ? null : val.toPlainString();
    }

    public static String printInteger(BigInteger val) {
        return val == null ? null : val.toString();
    }

    public static String printFloat(float val) {
        return String.valueOf(val);
    }

    public static String printDouble(double val) {
        return String.valueOf(val);
    }

    public static String printBoolean(boolean val) {
        return String.valueOf(val);
    }
}
