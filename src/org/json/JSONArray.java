package org.json;

import java.util.*;

public class JSONArray {
    private List<Object> list = new ArrayList<Object>();
    public JSONArray() {}
    public JSONArray(String s) {}
    public int length() { return list.size(); }
    public Object get(int i) throws JSONException { return list.get(i); }
    public String getString(int i) throws JSONException { return get(i).toString(); }
    public JSONObject getJSONObject(int i) throws JSONException { return (JSONObject)get(i); }
}
