import java.lang.reflect.Method;
import org.lwjgl.input.Mouse;

public final class ProbeMousePollSnapshot {
    public static void main(String[] args) throws Exception {
        Method decode = Mouse.class.getDeclaredMethod("decodeSigned16", int.class);
        decode.setAccessible(true);
        check(decode, 0x0000, 0); check(decode, 0x0001, 1); check(decode, 0x7fff, 32767);
        check(decode, 0x8000, -32768); check(decode, 0xffff, -1); check(decode, 0x18001, -32767);
        System.out.println("ProbeMousePollSnapshot: OK signed16 range=-32768..32767");
    }
    private static void check(Method decode, int encoded, int expected) throws Exception {
        int actual = ((Integer) decode.invoke(null, Integer.valueOf(encoded))).intValue();
        if (actual != expected) throw new AssertionError("decode " + encoded + " -> " + actual + " expected " + expected);
    }
}
