import com.fs.starfarer.BrowserGameplayProbe;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin;
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility;
import com.fs.starfarer.api.impl.campaign.abilities.TransponderAbility;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/** Uses the actual stock API classes. No runtime game state is changed. */
public final class VerifyGameplayProbeSettlement {
    private static int checks;
    private static final class State { boolean active, progress, transponder; float level; }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
    private static AbilityPlugin generic(final State state) {
        return (AbilityPlugin)Proxy.newProxyInstance(VerifyGameplayProbeSettlement.class.getClassLoader(),new Class<?>[]{AbilityPlugin.class},(proxy,method,args)->{
            switch(method.getName()) {
                case "isActive": return state.active;
                case "isInProgress": return state.progress;
                case "getLevel": return state.level;
                case "getId": return "transponder"; // ID alone must not get special semantics.
                case "isUsable": return true;
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                default: return defaultValue(method.getReturnType());
            }
        });
    }
    private static void set(Object target, Class<?> owner, String field, Object value) throws Exception {
        Field f=owner.getDeclaredField(field);f.setAccessible(true);f.set(target,value);
    }
    private static boolean settled(AbilityPlugin ability) throws Exception {
        Method m=BrowserGameplayProbe.class.getDeclaredMethod("isSettled",AbilityPlugin.class);m.setAccessible(true);
        return (Boolean)m.invoke(null,ability);
    }
    private static void require(boolean condition,String message) {checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        final State state=new State();
        final SectorEntityToken entity=(SectorEntityToken)Proxy.newProxyInstance(VerifyGameplayProbeSettlement.class.getClassLoader(),new Class<?>[]{SectorEntityToken.class},(proxy,method,arguments)->{
            if(method.getName().equals("isTransponderOn"))return state.transponder;
            if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
            if(method.getName().equals("equals"))return proxy==arguments[0];
            return defaultValue(method.getReturnType());
        });
        TransponderAbility stock=new TransponderAbility();
        set(stock,BaseAbilityPlugin.class,"entity",entity);set(stock,BaseAbilityPlugin.class,"id","transponder");
        set(stock,BaseToggleAbility.class,"turnedOn",false);set(stock,BaseToggleAbility.class,"level",0f);
        require(stock.getProgressFraction()==1f,"stock progress indicator semantic changed");
        require(stock.isInProgress(),"expected stock inherited isInProgress to remain true");
        require(!(!stock.isActive()&&!stock.isInProgress()&&stock.getLevel()<=.0001f),"negative control no longer reproduces");
        if(args.length>0 && args[0].equals("--negative-control")) {
            System.out.println("Stock API negative control: inactive transponder, level=0, entity off, but inherited inProgress=true; old predicate cannot settle");return;
        }
        require(settled(stock),"inactive fully faded stock transponder was not settled");
        state.transponder=true;require(!settled(stock),"entity still broadcasting must not settle");state.transponder=false;
        set(stock,BaseToggleAbility.class,"turnedOn",true);require(!settled(stock),"active toggle must not settle");set(stock,BaseToggleAbility.class,"turnedOn",false);
        for(float level:new float[]{1f,.1f,.0002f,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1f}) {
            set(stock,BaseToggleAbility.class,"level",level);require(!settled(stock),"invalid or unfinished level accepted: "+level);
        }
        set(stock,BaseToggleAbility.class,"level",0f);set(stock,BaseAbilityPlugin.class,"entity",null);
        require(!settled(stock),"missing entity must not prove transponder off");set(stock,BaseAbilityPlugin.class,"entity",entity);
        AbilityPlugin other=generic(state);
        state.progress=true;require(!settled(other),"same id in unknown class must keep generic inProgress rule");
        state.progress=false;require(settled(other),"generic inactive level zero should settle");
        state.active=true;require(!settled(other),"active generic ability accepted");state.active=false;
        state.level=Float.NaN;require(!settled(other),"NaN generic level accepted");state.level=0f;
        require(!settled(null),"null ability accepted");
        AbilityPlugin broken=(AbilityPlugin)Proxy.newProxyInstance(VerifyGameplayProbeSettlement.class.getClassLoader(),new Class<?>[]{AbilityPlugin.class},(proxy,method,a)->{throw new IllegalStateException("fixture");});
        require(!settled(broken),"throwing plugin accepted");
        TransponderAbility subclass=new TransponderAbility(){};
        set(subclass,BaseAbilityPlugin.class,"entity",entity);
        require(!settled(subclass),"unknown subclass must not inherit stock-specific exception");
        // The real probe must emit a transition, not just return a helper value.
        PrintStream original=System.out;ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        String previous=System.getProperty("starsector.browserGameplayProbe");
        try(PrintStream capture=new PrintStream(bytes,true,"UTF-8")) {
            System.setOut(capture);System.setProperty("starsector.browserGameplayProbe","true");
            BrowserGameplayProbe.abilityActivate(stock);BrowserGameplayProbe.abilityAdvance(stock);BrowserGameplayProbe.abilityAdvance(stock);
        } finally {
            System.setOut(original);
            if(previous==null)System.clearProperty("starsector.browserGameplayProbe");else System.setProperty("starsector.browserGameplayProbe",previous);
        }
        String log=bytes.toString("UTF-8");
        require(log.contains("event=ability-settled"),"real probe did not emit settled transition");
        require(log.indexOf("event=ability-settled")==log.lastIndexOf("event=ability-settled"),"duplicate settled transition");
        require(log.contains("settlementBasis=stock-transponder-off"),"settlement basis missing");
        require(stock.isInProgress(),"probe mutated stock progress semantics");
        System.out.println("VerifyGameplayProbeSettlement: OK checks="+checks+"; stock behavior unchanged, unknown plugins fail closed");
    }
}
