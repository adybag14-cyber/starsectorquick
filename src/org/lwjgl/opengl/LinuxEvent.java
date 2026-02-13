package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class LinuxEvent {
    public static final int FocusIn = 9;
    public static final int FocusOut = 10;
    public static final int KeyPress = 2;
    public static final int KeyRelease = 3;
    public static final int ButtonPress = 4;
    public static final int ButtonRelease = 5;
    public static final int MotionNotify = 6;
    public static final int EnterNotify = 7;
    public static final int LeaveNotify = 8;
    public static final int UnmapNotify = 18;
    public static final int MapNotify = 19;
    public static final int Expose = 12;
    public static final int ConfigureNotify = 22;
    public static final int ClientMessage = 33;
    private final ByteBuffer event_buffer = LinuxEvent.createEventBuffer();

    LinuxEvent() {
    }

    private static ByteBuffer createEventBuffer() {
        return ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder());
    }

    public void copyFrom(LinuxEvent event) {
        int pos = this.event_buffer.position();
        int event_pos = event.event_buffer.position();
        this.event_buffer.put(event.event_buffer);
        this.event_buffer.position(pos);
        event.event_buffer.position(event_pos);
    }

    public static int getPending(long var0) { return 0; }

    public void sendEvent(long display, long window, boolean propagate, long event_mask) {
    }

    public boolean filterEvent(long window) {
        return false;
    }

    public void nextEvent(long display) {
        try { Thread.sleep(16); } catch (Exception e) {}
    }

    public int getType() { return 0; }
    public long getWindow() { return 0; }
    public void setWindow(long window) { }
    public int getFocusMode() { return 0; }
    public int getFocusDetail() { return 0; }
    public long getClientMessageType() { return 0; }
    public int getClientData(int index) { return 0; }
    public int getClientFormat() { return 0; }
    public long getButtonTime() { return 0; }
    public int getButtonState() { return 0; }
    public int getButtonType() { return 0; }
    public int getButtonButton() { return 0; }
    public long getButtonRoot() { return 0; }
    public int getButtonXRoot() { return 0; }
    public int getButtonYRoot() { return 0; }
    public int getButtonX() { return 0; }
    public int getButtonY() { return 0; }
    public long getKeyAddress() { return 0; }
    public long getKeyTime() { return 0; }
    public int getKeyType() { return 0; }
    public int getKeyKeyCode() { return 0; }
    public int getKeyState() { return 0; }
}
