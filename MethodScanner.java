import java.lang.reflect.Method;

public class MethodScanner {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
            for (Method m : clazz.getDeclaredMethods()) {
                String name = m.getName();
                StringBuilder sb = new StringBuilder();
                for (char c : name.toCharArray()) {
                    if (c > 127) sb.append(String.format("\\u%04x", (int)c));
                    else sb.append(c);
                }
                System.out.println("Method: " + sb.toString() + " (" + m.getParameterCount() + " args)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}