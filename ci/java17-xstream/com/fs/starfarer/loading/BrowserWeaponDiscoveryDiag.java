package com.fs.starfarer.loading;
/** Diagnostic-only aggregate timing for weapon/projectile path discovery. */
public final class BrowserWeaponDiscoveryDiag {
  public static final int PROJECTILES=1, WEAPONS=2;
  private static int active, discoveryCalls; private static long totalStart, discoveryStart, discoveryNs;
  private BrowserWeaponDiscoveryDiag() {}
  public static void begin(int kind){active=kind;totalStart=System.nanoTime();discoveryStart=0L;discoveryNs=0L;discoveryCalls=0;}
  public static void discoveryStart(int kind){if(active!=kind)return;discoveryStart=System.nanoTime();}
  public static void discoveryEnd(int kind){if(active!=kind||discoveryStart==0L)return;discoveryNs+=System.nanoTime()-discoveryStart;discoveryStart=0L;discoveryCalls++;}
  public static void finish(int kind){if(active!=kind||totalStart==0L)return;long total=System.nanoTime()-totalStart;System.out.println("BrowserWeaponDiscoveryDiag: kind="+(kind==PROJECTILES?"projectiles":"weapons")+" calls="+discoveryCalls+" totalMs="+ms(total)+" discoveryMs="+ms(discoveryNs)+" loadMs="+ms(Math.max(0L,total-discoveryNs)));active=0;totalStart=discoveryStart=discoveryNs=0L;discoveryCalls=0;}
  private static double ms(long ns){return ((double)ns)/1000000.0d;}
}
