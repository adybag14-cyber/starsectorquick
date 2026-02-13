package org.json;

import java.util.*;

public class JSONObject {
    public static final Object NULL = new Object();
    private Map<String, Object> map = new HashMap<String, Object>();

    public JSONObject() {}
    public JSONObject(String s) {}
    public JSONObject(Map m) { if (m != null) this.map.putAll(m); }

    public Object get(String key) throws JSONException {
        Object o = map.get(key);
        if (o == null) return JSONObject.NULL;
        return o;
    }

    public JSONObject getJSONObject(String key) throws JSONException {
        Object o = map.get(key);
        if (o instanceof JSONObject) return (JSONObject)o;
        return new JSONObject();
    }

    public JSONArray getJSONArray(String key) throws JSONException {
        return new JSONArray();
    }

    public boolean getBoolean(String key) throws JSONException {
        Object o = map.get(key);
        if (o instanceof Boolean) return (Boolean)o;
        return false;
    }

    public String getString(String key) throws JSONException {
        Object o = map.get(key);
        if (o != null) return o.toString();
        return "";
    }

    public int getInt(String key) throws JSONException {
        Object o = map.get(key);
        if (o instanceof Number) return ((Number)o).intValue();
        return 0;
    }

    public long getLong(String key) throws JSONException {
        Object o = map.get(key);
        if (o instanceof Number) return ((Number)o).longValue();
        return 0L;
    }

    public double getDouble(String key) throws JSONException {
        Object o = map.get(key);
        if (o instanceof Number) return ((Number)o).doubleValue();
        return 0.0;
    }

    public boolean has(String key) { return map.containsKey(key); }
    public boolean isNull(String key) { return map.get(key) == NULL; }

    public String optString(String key) { return optString(key, ""); }
    public String optString(String key, String def) {
        Object o = map.get(key);
        return o != null ? o.toString() : def;
    }

    public boolean optBoolean(String key) { return optBoolean(key, false); }
    public boolean optBoolean(String key, boolean def) {
        Object o = map.get(key);
        return o instanceof Boolean ? (Boolean)o : def;
    }

    public JSONObject put(String key, Object val) throws JSONException {
        map.put(key, val);
        return this;
    }
    public JSONObject put(String key, boolean val) throws JSONException { return put(key, (Boolean)val); }
    public JSONObject put(String key, int val) throws JSONException { return put(key, (Integer)val); }
    
    public Iterator keys() { return map.keySet().iterator(); }
    public int length() { return map.size(); }
    
    public static String[] getNames(JSONObject jo) {
        Set<String> keys = jo.map.keySet();
        return keys.toArray(new String[0]);
    }
}
