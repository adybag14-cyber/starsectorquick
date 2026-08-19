import java.lang.reflect.Method;
import org.lwjgl.input.Mouse;

public final class ProbeMousePollSnapshot {
    public static void main(String[] args) throws Exception {
        Method decode = Mouse.class.getDeclaredMethod("decodeSigned20", long.class);
        decode.setAccessible(true);
        check(decode, 0x00000L, 0);
        check(decode, 0x00001L, 1);
        check(decode, 0x7ffffL, 524287);
        check(decode, 0x80000L, -524288);
        check(decode, 0xfffffL, -1);
        check(decode, 0x180001L, -524287);
        System.out.println("ProbeMousePollSnapshot: OK signed20 range=-524288..524287");
    }
    private static void check(Method decode, long encoded, int expected) throws Exception {
        int actual = ((Integer) decode.invoke(null, Long.valueOf(encoded))).intValue();
        if (actual != expected) throw new AssertionError("decode " + encoded + " -> " + actual + " expected " + expected);
    }
}
