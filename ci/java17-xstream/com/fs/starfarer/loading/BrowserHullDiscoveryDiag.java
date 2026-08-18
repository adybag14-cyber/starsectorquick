package com.fs.starfarer.loading;
/** Diagnostic-only timing of hull/skin discovery vs per-file load work. */
public final class BrowserHullDiscoveryDiag {
  public static final int HULLS=1, SKINS=2;
  private static long start,loop; private static int active;
  private BrowserHullDiscoveryDiag() {}
  public static void begin(int kind){active=kind;start=System.nanoTime();loop=0L;}
  public static void loopStart(int kind){if(active==kind&&loop==0L)loop=System.nanoTime();}
  public static void finish(int kind){if(active!=kind||start==0L)return;long end=System.nanoTime();long split=loop==0L?end:loop;System.out.println("BrowserHullDiscoveryDiag: kind="+(kind==HULLS?"hulls":"skins")+" totalMs="+ms(end-start)+" discoveryMs="+ms(split-start)+" loadMs="+ms(end-split));active=0;start=loop=0L;}
  private static double ms(long ns){return ((double)ns)/1000000.0d;}
}
