package org.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JSONObject {
    private Map<String, Object> map = new HashMap<>();
    public JSONObject() {}
    public JSONObject(String json) throws JSONException {}
    public JSONObject(JSONObject other, String[] names) throws JSONException {
        for (String name : names) {
            this.put(name, other.opt(name));
        }
    }
    public Object get(String key) throws JSONException { 
        Object o = map.get(key);
        if (o == null) throw new JSONException("No value for " + key);
        return o;
    }
    public Object opt(String key) { return map.get(key); }
    public String getString(String key) throws JSONException { return String.valueOf(get(key)); }
    public String optString(String key) { return optString(key, ""); }
    public String optString(String key, String defaultValue) {
        Object o = map.get(key);
        return o != null ? String.valueOf(o) : defaultValue;
    }
    public int getInt(String key) throws JSONException { return ((Number)get(key)).intValue(); }
    public int optInt(String key) { return optInt(key, 0); }
    public int optInt(String key, int defaultValue) {
        Object o = map.get(key);
        return o instanceof Number ? ((Number)o).intValue() : defaultValue;
    }
    public long getLong(String key) throws JSONException { return ((Number)get(key)).longValue(); }
    public double getDouble(String key) throws JSONException { return ((Number)get(key)).doubleValue(); }
    public double optDouble(String key) { return optDouble(key, 0.0); }
    public double optDouble(String key, double defaultValue) {
        Object o = map.get(key);
        return o instanceof Number ? ((Number)o).doubleValue() : defaultValue;
    }
    public boolean getBoolean(String key) throws JSONException { return (Boolean)get(key); }
    public boolean optBoolean(String key) { return optBoolean(key, false); }
    public boolean optBoolean(String key, boolean defaultValue) {
        Object o = map.get(key);
        return o instanceof Boolean ? (Boolean)o : defaultValue;
    }
    public JSONObject getJSONObject(String key) throws JSONException { return (JSONObject)get(key); }
    public JSONObject optJSONObject(String key) { 
        Object o = map.get(key);
        return o instanceof JSONObject ? (JSONObject)o : null;
    }
    public JSONArray getJSONArray(String key) throws JSONException { return (JSONArray)get(key); }
    public JSONArray optJSONArray(String key) {
        Object o = map.get(key);
        return o instanceof JSONArray ? (JSONArray)o : null;
    }
    public void put(String key, Object value) throws JSONException { map.put(key, value); }
    public boolean has(String key) { return map.containsKey(key); }
    public Set<String> keySet() { return map.keySet(); }
    public static String[] getNames(JSONObject jo) { 
        if (jo == null || jo.map.isEmpty()) return null;
        return jo.keySet().toArray(new String[0]); 
    }
    public String toString() { return "{}"; }
}
