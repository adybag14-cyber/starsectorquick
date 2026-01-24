package com.thoughtworks.xstream;

public class XStream {
    public XStream() {}
    public void alias(String name, Class<?> type) {}
    public void aliasAttribute(Class<?> type, String fieldName, String alias) {}
    public String toXML(Object obj) { return ""; }
    public Object fromXML(String xml) { return null; }
}