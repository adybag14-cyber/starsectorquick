package com.fs.starfarer.loading;

import java.io.*;
import java.util.*;
import java.util.Base64;
import org.json.*;

public class LoadingUtils {
    public static String _super = "fs_rowSource"; 

    public static JSONObject _super(String path, Set<String> set) throws IOException, JSONException {
        System.out.println("LoadingUtils: Loading JSON " + path);
        String text = _super(path);
        text = stripComments(text);
        return new JSONObject(text);
    }

    public static String _super(String path) throws IOException {
        System.out.println("LoadingUtils: Loading text " + path);
        InputStream is = _void(path);
        if (is == null) throw new FileNotFoundException(path);
        return readStream(is);
    }

    public static InputStream _void(String path) throws IOException {
        System.out.println("LoadingUtils: Opening stream " + path);
        File f = new File(path);
        if (f.exists()) return new FileInputStream(f);
        
        InputStream is = LoadingUtils.class.getResourceAsStream("/" + path);
        if (is != null) return is;
        
        f = new File("/files/" + path);
        if (f.exists()) return new FileInputStream(f);

        System.out.println("LoadingUtils: Asset not found, using fallback: " + path);
        String b64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
        byte[] data = Base64.getDecoder().decode(b64);
        return new ByteArrayInputStream(data);
    }

    private static String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return new String(baos.toByteArray(), "UTF-8");
    }
    
    private static String stripComments(String string2) {
        StringBuffer stringBuffer = new StringBuffer();
        boolean bl = false;
        boolean bl2 = false;
        int n2 = 0;
        while (n2 < string2.length()) {
            char c2 = string2.charAt(n2);
            if (c2 == '\"') {
                bl2 = !bl2;
            }
            if (c2 == '\n' || c2 == '\r') {
                bl = false;
                bl2 = false;
                if (c2 == '\n') {
                    stringBuffer.append('\n');
                }
            } else if (c2 == '#' && !bl2) {
                bl = true;
            } else if (!bl) {
                stringBuffer.append(c2);
            }
            ++n2;
        }
        return stringBuffer.toString();
    }

    public static JSONObject \u00f400000(String string) throws IOException, JSONException {
        return new JSONObject(stripComments(_super(string)));
    }
    
    public static JSONObject \u00d300000(String string) throws IOException, JSONException {
        return _super(string, (Set<String>)null);
    }

    public static void \u00f500000(String string) {
    }
    
    public static JSONObject _super(String string, InputStream is) throws IOException, JSONException {
        String text = readStream(is);
        return new JSONObject(stripComments(text));
    }
    
    public static String \u00d200000(InputStream is) throws IOException {
        return readStream(is);
    }
    
    public static JSONObject \u00d600000(String string, String content) throws JSONException {
        return new JSONObject(stripComments(content));
    }
    
    public static List<String> \u00d600000(String s) { return new ArrayList<String>(); }
    public static List<String> _super(String s1, String s2) { return new ArrayList<String>(); }
    public static List<String> \u00d500000(String s1, String s2) { return new ArrayList<String>(); }
    public static void \u00d200000(String s) { }
    public static void \u00d200000(String s1, String s2) { }
    public static void \u00d300000(String s1, String s2, boolean b) { }
    public static JSONArray \u00d300000(String s1, String s2) { return new JSONArray(); }
    public static JSONArray \u00d200000(String s1, String s2, boolean b) { return new JSONArray(); }
    public static JSONArray _super(List<String> l, String s, boolean b) { return new JSONArray(); }
    public static JSONArray _super(List<String> l, String s, boolean b, boolean b2) { return new JSONArray(); }
    public static String String(String s) { return s; }
    public static List<String> _super(String s1, String s2, boolean b) { return new ArrayList<String>(); }
    public static List<String> _super(String s1, String s2, String s3) { return new ArrayList<String>(); }
    public static JSONArray \u00d200000(String s1, String s2, String s3) { return new JSONArray(); }
    public static JSONObject String(String s1, String s2) { return new JSONObject(); }
}