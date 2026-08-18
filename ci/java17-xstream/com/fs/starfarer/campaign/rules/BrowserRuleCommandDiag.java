package com.fs.starfarer.campaign.rules;
public final class BrowserRuleCommandDiag {
  private static boolean active; private static long start,total,cold,hot,max; private static int currentAttempts,calls,coldCalls,hotCalls,attempts;
  private BrowserRuleCommandDiag() {}
  public static void beginRules(){active=true;start=total=cold=hot=max=0L;currentAttempts=calls=coldCalls=hotCalls=attempts=0;}
  public static void lookupStart(){if(!active)return;start=System.nanoTime();currentAttempts=0;}
  public static void classAttempt(){if(!active)return;attempts++;currentAttempts++;}
  public static void lookupEnd(){if(!active||start==0L)return;long ns=System.nanoTime()-start;start=0L;calls++;total+=ns;if(ns>max)max=ns;if(currentAttempts>0){coldCalls++;cold+=ns;}else{hotCalls++;hot+=ns;}}
  public static void finishRules(){if(!active)return;System.out.println("BrowserRuleCommandDiag: calls="+calls+" coldCalls="+coldCalls+" hotCalls="+hotCalls+" attempts="+attempts+" totalMs="+ms(total)+" coldMs="+ms(cold)+" hotMs="+ms(hot)+" maxMs="+ms(max));active=false;start=0L;}
  private static double ms(long ns){return ((double)ns)/1000000.0d;}
}
