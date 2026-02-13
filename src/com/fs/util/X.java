package com.fs.util;

import java.io.*;
import java.util.*;
import com.fs.util.container.Pair;
import com.fs.starfarer.launcher.ModManager;

public class X {
    private static X instance = new X();

    public static X Ó00000() { // getInstance
        return instance;
    }

    public static class Oo {
        public boolean String = false; 
        public ModManager.ModSpec Ò00000 = null;
        public java.lang.String abcde = "CORE"; 
        public java.lang.String Ó00000 = ""; 
    }

    public InputStream String(java.lang.String path) throws IOException {
        System.out.println("ResLoader: Opening " + path);
        if (path.endsWith("settings.json")) {
             System.out.println("ResLoader: Using minimal settings.json");
             return getSettingsStream();
        }
        
        File f = new File(path);
        if (f.exists()) return new FileInputStream(f);
        InputStream is = getClass().getResourceAsStream("/" + path);
        if (is != null) return is;
        throw new FileNotFoundException(path);
    }

    public List<java.lang.String> Ó00000(java.lang.String path) throws IOException {
        System.out.println("ResLoader: Listing " + path);
        return new ArrayList<java.lang.String>();
    }

    public void Ö00000(java.lang.String path) {
        System.out.println("ResLoader: Invalidate " + path);
    }

    public List<Pair> Ò00000(java.lang.String path) throws IOException {
        System.out.println("ResLoader: GetMergedList " + path);
        List<Pair> list = new ArrayList<Pair>();
        
        Oo oo = new Oo();
        oo.String = false;
        oo.Ò00000 = null;
        oo.abcde = "CORE";
        oo.Ó00000 = path;
        
        try {
            InputStream is = String(path);
            list.add(new Pair(oo, is));
        } catch (IOException e) {
            System.out.println("ResLoader: Base file not found: " + path);
        }
        
        return list;
    }

    public List<java.lang.String> abcde(java.lang.String s1, java.lang.String s2, boolean b) { 
        System.out.println("ResLoader: GetFromJar " + s1 + ", " + s2);
        return new ArrayList<java.lang.String>();
    }
    
    public void Õ00000() { System.out.println("ResLoader: Init1"); }
    public void Õ00000(java.lang.String s) { System.out.println("ResLoader: Init2 " + s); }
    public void Ò00000() { System.out.println("ResLoader: Init3"); }

    private InputStream getSettingsStream() {
        String s = "{}";
        return new ByteArrayInputStream(s.getBytes());
    }
}
