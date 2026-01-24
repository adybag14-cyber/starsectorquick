package org.apache.log4j;

import elemental2.dom.DomGlobal;

public class Logger {
    public static Logger getLogger(Class<?> clazz) { return new Logger(); }
    public static Logger getLogger(String name) { return new Logger(); }
    public void info(Object msg) { DomGlobal.console.info(msg); }
    public void warn(Object msg) { DomGlobal.console.warn(msg); }
    public void warn(Object msg, Throwable t) { DomGlobal.console.warn(msg, t); }
    public void error(Object msg) { DomGlobal.console.error(msg); }
    public void error(Object msg, Throwable t) { DomGlobal.console.error(msg, t); }
    public void debug(Object msg) { DomGlobal.console.debug(msg); }
    public void fatal(Object msg) { DomGlobal.console.error("FATAL: " + msg); }
    public boolean isDebugEnabled() { return true; }
    public boolean isTraceEnabled() { return false; }
}