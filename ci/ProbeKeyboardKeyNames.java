import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.lwjgl.input.Keyboard;

/** Emits LWJGL key-name API and reflected KEY_* declaration surfaces for bridge parity checks. */
public final class ProbeKeyboardKeyNames {
    private ProbeKeyboardKeyNames() {}

    public static void main(String[] args) throws Exception {
        for (int key = 0; key < 256; key++) {
            String name = Keyboard.getKeyName(key);
            if (name != null) {
                System.out.println("API\t" + key + "\t" + name + "\t" + Keyboard.getKeyIndex(name));
            }
        }
        for (Field field : Keyboard.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers)
                    && Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers)
                    && field.getType() == Integer.TYPE
                    && field.getName().startsWith("KEY_")) {
                System.out.println("FIELD\t" + field.getName() + "\t" + field.getInt(null));
            }
        }
    }
}
