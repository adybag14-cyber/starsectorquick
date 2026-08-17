import com.fs.starfarer.BrowserGameplayProbe;
import com.fs.starfarer.api.characters.AbilityPlugin;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Verifies readiness telemetry requires three consecutive usable ability updates. */
public final class VerifyGameplayProbeReadiness {
    private static final class State {
        boolean usable;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("starsector.browserGameplayProbe", "true");
        final State state = new State();
        final AbilityPlugin ability = (AbilityPlugin) Proxy.newProxyInstance(
                VerifyGameplayProbeReadiness.class.getClassLoader(),
                new Class<?>[] { AbilityPlugin.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] methodArgs) {
                        String name = method.getName();
                        if ("hashCode".equals(name)) return Integer.valueOf(System.identityHashCode(proxy));
                        if ("equals".equals(name)) return Boolean.valueOf(proxy == methodArgs[0]);
                        if ("toString".equals(name)) return "ReadinessProbeAbility";
                        if ("getId".equals(name)) return "readiness_probe";
                        if ("getSpec".equals(name)) return null;
                        if ("isUsable".equals(name)) return Boolean.valueOf(state.usable);
                        if ("isActive".equals(name) || "isInProgress".equals(name) || "isOnCooldown".equals(name)) {
                            return Boolean.FALSE;
                        }
                        if ("getLevel".equals(name) || "getProgressFraction".equals(name)) return Float.valueOf(0f);
                        if ("getCooldownFraction".equals(name)) return Float.valueOf(1f);
                        Class<?> type = method.getReturnType();
                        if (type == Boolean.TYPE) return Boolean.FALSE;
                        if (type == Integer.TYPE) return Integer.valueOf(0);
                        if (type == Long.TYPE) return Long.valueOf(0L);
                        if (type == Float.TYPE) return Float.valueOf(0f);
                        if (type == Double.TYPE) return Double.valueOf(0d);
                        if (type == Short.TYPE) return Short.valueOf((short)0);
                        if (type == Byte.TYPE) return Byte.valueOf((byte)0);
                        if (type == Character.TYPE) return Character.valueOf((char)0);
                        return null;
                    }
                });

        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bytes, true, "UTF-8");
        System.setOut(capture);
        try {
            state.usable = false;
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-unready") == 1, "initial unready transition missing", bytes);
            require(count(bytes, "event=ability-ready-stable") == 0, "unready ability reported stable", bytes);

            state.usable = true;
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-ready") == 1, "ready transition missing", bytes);
            require(count(bytes, "event=ability-ready-stable") == 0, "stable emitted after one frame", bytes);
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-ready-stable") == 0, "stable emitted after two frames", bytes);
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-ready-stable") == 1, "stable missing after three frames", bytes);
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-ready-stable") == 1, "stable event duplicated without state change", bytes);

            state.usable = false;
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-unready") == 2, "unready reset transition missing", bytes);

            state.usable = true;
            BrowserGameplayProbe.abilityAdvance(ability);
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-ready-stable") == 1, "stable re-emitted before three reset frames", bytes);
            BrowserGameplayProbe.abilityAdvance(ability);
            require(count(bytes, "event=ability-ready-stable") == 2, "stable did not re-arm after reset", bytes);
        } finally {
            System.setOut(original);
            capture.close();
        }
        original.println("VerifyGameplayProbeReadiness: OK stableTransitions=2");
    }

    private static int count(ByteArrayOutputStream bytes, String token) throws Exception {
        String text = bytes.toString("UTF-8");
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static void require(boolean condition, String message, ByteArrayOutputStream bytes) throws Exception {
        if (!condition) throw new AssertionError(message + "\n" + bytes.toString("UTF-8"));
    }
}
