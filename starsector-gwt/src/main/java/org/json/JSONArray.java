package org.json;

import java.util.ArrayList;
import java.util.List;

public class JSONArray {
    private List<Object> list = new ArrayList<>();
    public JSONArray() {}
    public JSONArray(String json) throws JSONException {}
    public int length() { return list.size(); }
    public Object get(int index) throws JSONException { 
        if (index < 0 || index >= list.size()) throw new JSONException("Index out of bounds");
        return list.get(index);
    }
    public Object opt(int index) {
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }
    public String getString(int index) throws JSONException { return String.valueOf(get(index)); }
    public String optString(int index) { return optString(index, ""); }
    public String optString(int index, String defaultValue) {
        Object o = opt(index);
        return o != null ? String.valueOf(o) : defaultValue;
    }
    public int getInt(int index) throws JSONException { return ((Number)get(index)).intValue(); }
    public int optInt(int index) { return optInt(index, 0); }
    public int optInt(int index, int defaultValue) {
        Object o = opt(index);
        return o instanceof Number ? ((Number)o).intValue() : defaultValue;
    }
    public double getDouble(int index) throws JSONException {
        return ((Number)get(index)).doubleValue();
    }
    public double optDouble(int index) {
        return optDouble(index, 0.0);
    }
    public double optDouble(int index, double defaultValue) {
        Object o = opt(index);
        return o instanceof Number ? ((Number)o).doubleValue() : defaultValue;
    }
    public JSONObject getJSONObject(int index) throws JSONException { return (JSONObject)get(index); }
    public JSONObject optJSONObject(int index) {
        Object o = opt(index);
        return o instanceof JSONObject ? (JSONObject)o : null;
    }
    public void put(Object value) { list.add(value); }
}
