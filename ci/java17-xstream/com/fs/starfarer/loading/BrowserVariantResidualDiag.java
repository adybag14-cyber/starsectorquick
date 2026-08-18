package com.fs.starfarer.loading;

/** Diagnostic-only aggregate timer for residual work in SpecStore's variant loop. */
public final class BrowserVariantResidualDiag {
    private static long loopStart, activeStart;
    private static long loadLogNs, skipLogNs, duplicateNs, settingsNs, hullNs;
    private static int loadLogs, skipLogs, duplicates, settingsChecks, hullChecks;
    private static int activeKind;
    private BrowserVariantResidualDiag() {}
    public static void begin(){loopStart=System.nanoTime();activeStart=0L;activeKind=0;loadLogNs=skipLogNs=duplicateNs=settingsNs=hullNs=0L;loadLogs=skipLogs=duplicates=settingsChecks=hullChecks=0;}
    private static void start(int kind){activeKind=kind;activeStart=System.nanoTime();}
    private static long stop(int kind){if(activeStart==0L||activeKind!=kind)return 0L;long ns=System.nanoTime()-activeStart;activeStart=0L;activeKind=0;return ns;}
    public static void loadLogStart(){start(1);} public static void loadLogEnd(){long n=stop(1);if(n>0){loadLogNs+=n;loadLogs++;}}
    public static void skipLogStart(){start(2);} public static void skipLogEnd(){long n=stop(2);if(n>0){skipLogNs+=n;skipLogs++;}}
    public static void duplicateStart(){start(3);} public static void duplicateEnd(){long n=stop(3);if(n>0){duplicateNs+=n;duplicates++;}}
    public static void settingsStart(){start(4);} public static void settingsEnd(){long n=stop(4);if(n>0){settingsNs+=n;settingsChecks++;}}
    public static void hullStart(){start(5);} public static void hullEnd(){long n=stop(5);if(n>0){hullNs+=n;hullChecks++;}}
    public static void finish(){if(loopStart==0L)return;long total=System.nanoTime()-loopStart;long measured=loadLogNs+skipLogNs+duplicateNs+settingsNs+hullNs;System.out.println("BrowserVariantResidualDiag: totalMs="+ms(total)+" loadLogs="+loadLogs+" loadLogMs="+ms(loadLogNs)+" skipLogs="+skipLogs+" skipLogMs="+ms(skipLogNs)+" duplicateChecks="+duplicates+" duplicateMs="+ms(duplicateNs)+" settingsChecks="+settingsChecks+" settingsMs="+ms(settingsNs)+" hullChecks="+hullChecks+" hullMs="+ms(hullNs)+" residualMs="+ms(Math.max(0L,total-measured)));loopStart=0L;activeStart=0L;activeKind=0;}
    private static double ms(long n){return ((double)n)/1000000.0d;}
}
