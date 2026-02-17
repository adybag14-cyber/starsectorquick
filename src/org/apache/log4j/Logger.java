package org.apache.log4j;

public class Logger {
    static { System.out.println("STUB Logger loaded!"); }
    public static Logger getLogger(Class clazz) { return new Logger(); }
    public static Logger getLogger(String name) { return new Logger(); }
    public static Logger getRootLogger() { return new Logger(); }
    
    private void dumpStack() {
        // This is the stack trace of the Logger.error call itself
        // StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        // for (StackTraceElement ste : trace) {
        //     System.out.println("  at " + ste);
        // }
    }

    public void info(Object msg) { System.out.println("LOG INFO: " + msg); }
    
    public void error(Object msg) { 
        System.out.println("LOG ERROR: " + msg); 
        if (msg instanceof Throwable) {
            ((Throwable)msg).printStackTrace(System.out);
        } else {
            // If it's not a throwable, still dump current stack to see who called it
            Thread.dumpStack();
        }
    }
    
    public void error(Object msg, Throwable t) { 
        System.out.println("LOG ERROR: " + msg); 
        if (t != null) t.printStackTrace(System.out);
        else Thread.dumpStack();
    }
    
    public void warn(Object msg) { System.out.println("LOG WARN: " + msg); }
    public void debug(Object msg) { System.out.println("LOG DEBUG: " + msg); }
    
    public void fatal(Object msg) { 
        System.out.println("LOG FATAL: " + msg); 
        if (msg instanceof Throwable) {
            ((Throwable)msg).printStackTrace(System.out);
        } else {
            Thread.dumpStack();
        }
    }
    
    public void fatal(Object msg, Throwable t) { 
        System.out.println("LOG FATAL: " + msg); 
        if (t != null) t.printStackTrace(System.out);
        else Thread.dumpStack();
    }
    
    public boolean isDebugEnabled() { return true; }
    public boolean isInfoEnabled() { return true; }
    public void setLevel(Object level) {}
}