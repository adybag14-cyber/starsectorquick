package org.lwjgl.input;

/** Browser-backed LWJGL 2 keyboard compatibility bridge. */
public class Keyboard {
    public static final int KEY_NONE = 0;
    public static final int KEY_ESCAPE = 1;
    public static final int KEY_1 = 2;
    public static final int KEY_2 = 3;
    public static final int KEY_3 = 4;
    public static final int KEY_4 = 5;
    public static final int KEY_5 = 6;
    public static final int KEY_6 = 7;
    public static final int KEY_7 = 8;
    public static final int KEY_8 = 9;
    public static final int KEY_9 = 10;
    public static final int KEY_0 = 11;
    public static final int KEY_MINUS = 12;
    public static final int KEY_EQUALS = 13;
    public static final int KEY_BACK = 14;
    public static final int KEY_TAB = 15;
    public static final int KEY_Q = 16;
    public static final int KEY_W = 17;
    public static final int KEY_E = 18;
    public static final int KEY_R = 19;
    public static final int KEY_T = 20;
    public static final int KEY_Y = 21;
    public static final int KEY_U = 22;
    public static final int KEY_I = 23;
    public static final int KEY_O = 24;
    public static final int KEY_P = 25;
    public static final int KEY_LBRACKET = 26;
    public static final int KEY_RBRACKET = 27;
    public static final int KEY_RETURN = 28;
    public static final int KEY_LCONTROL = 29;
    public static final int KEY_A = 30;
    public static final int KEY_S = 31;
    public static final int KEY_D = 32;
    public static final int KEY_F = 33;
    public static final int KEY_G = 34;
    public static final int KEY_H = 35;
    public static final int KEY_J = 36;
    public static final int KEY_K = 37;
    public static final int KEY_L = 38;
    public static final int KEY_SEMICOLON = 39;
    public static final int KEY_APOSTROPHE = 40;
    public static final int KEY_GRAVE = 41;
    public static final int KEY_LSHIFT = 42;
    public static final int KEY_BACKSLASH = 43;
    public static final int KEY_Z = 44;
    public static final int KEY_X = 45;
    public static final int KEY_C = 46;
    public static final int KEY_V = 47;
    public static final int KEY_B = 48;
    public static final int KEY_N = 49;
    public static final int KEY_M = 50;
    public static final int KEY_COMMA = 51;
    public static final int KEY_PERIOD = 52;
    public static final int KEY_SLASH = 53;
    public static final int KEY_RSHIFT = 54;
    public static final int KEY_MULTIPLY = 55;
    public static final int KEY_LMENU = 56;
    public static final int KEY_SPACE = 57;
    public static final int KEY_CAPITAL = 58;
    public static final int KEY_F1 = 59;
    public static final int KEY_F2 = 60;
    public static final int KEY_F3 = 61;
    public static final int KEY_F4 = 62;
    public static final int KEY_F5 = 63;
    public static final int KEY_F6 = 64;
    public static final int KEY_F7 = 65;
    public static final int KEY_F8 = 66;
    public static final int KEY_F9 = 67;
    public static final int KEY_F10 = 68;
    public static final int KEY_NUMLOCK = 69;
    public static final int KEY_SCROLL = 70;
    public static final int KEY_NUMPAD7 = 71;
    public static final int KEY_NUMPAD8 = 72;
    public static final int KEY_NUMPAD9 = 73;
    public static final int KEY_SUBTRACT = 74;
    public static final int KEY_NUMPAD4 = 75;
    public static final int KEY_NUMPAD5 = 76;
    public static final int KEY_NUMPAD6 = 77;
    public static final int KEY_ADD = 78;
    public static final int KEY_NUMPAD1 = 79;
    public static final int KEY_NUMPAD2 = 80;
    public static final int KEY_NUMPAD3 = 81;
    public static final int KEY_NUMPAD0 = 82;
    public static final int KEY_DECIMAL = 83;
    public static final int KEY_F11 = 87;
    public static final int KEY_F12 = 88;
    public static final int KEY_F13 = 100;
    public static final int KEY_F14 = 101;
    public static final int KEY_F15 = 102;
    public static final int KEY_F16 = 103;
    public static final int KEY_F17 = 104;
    public static final int KEY_F18 = 105;
    public static final int KEY_KANA = 112;
    public static final int KEY_F19 = 113;
    public static final int KEY_CONVERT = 121;
    public static final int KEY_NOCONVERT = 123;
    public static final int KEY_YEN = 125;
    public static final int KEY_NUMPADEQUALS = 141;
    public static final int KEY_CIRCUMFLEX = 144;
    public static final int KEY_AT = 145;
    public static final int KEY_COLON = 146;
    public static final int KEY_UNDERLINE = 147;
    public static final int KEY_KANJI = 148;
    public static final int KEY_STOP = 149;
    public static final int KEY_AX = 150;
    public static final int KEY_UNLABELED = 151;
    public static final int KEY_NUMPADENTER = 156;
    public static final int KEY_RCONTROL = 157;
    public static final int KEY_SECTION = 167;
    public static final int KEY_NUMPADCOMMA = 179;
    public static final int KEY_DIVIDE = 181;
    public static final int KEY_SYSRQ = 183;
    public static final int KEY_RMENU = 184;
    public static final int KEY_FUNCTION = 196;
    public static final int KEY_PAUSE = 197;
    public static final int KEY_HOME = 199;
    public static final int KEY_UP = 200;
    public static final int KEY_PRIOR = 201;
    public static final int KEY_LEFT = 203;
    public static final int KEY_RIGHT = 205;
    public static final int KEY_END = 207;
    public static final int KEY_DOWN = 208;
    public static final int KEY_NEXT = 209;
    public static final int KEY_INSERT = 210;
    public static final int KEY_DELETE = 211;
    public static final int KEY_CLEAR = 218;
    public static final int KEY_LMETA = 219;
    public static final int KEY_LWIN = 219;
    public static final int KEY_RMETA = 220;
    public static final int KEY_RWIN = 220;
    public static final int KEY_APPS = 221;
    public static final int KEY_POWER = 222;
    public static final int KEY_SLEEP = 223;
    private static boolean created = false;
    private static boolean repeatEvents = false;
    private static boolean repeatLogPrinted = false;
    private static final String[] KEY_NAMES = new String[256];
    private static final java.util.Map<String, Integer> KEY_INDICES = new java.util.HashMap<>();

    static {
        KEY_NAMES[0] = "NONE";
        KEY_NAMES[1] = "ESCAPE";
        KEY_NAMES[2] = "1";
        KEY_NAMES[3] = "2";
        KEY_NAMES[4] = "3";
        KEY_NAMES[5] = "4";
        KEY_NAMES[6] = "5";
        KEY_NAMES[7] = "6";
        KEY_NAMES[8] = "7";
        KEY_NAMES[9] = "8";
        KEY_NAMES[10] = "9";
        KEY_NAMES[11] = "0";
        KEY_NAMES[12] = "MINUS";
        KEY_NAMES[13] = "EQUALS";
        KEY_NAMES[14] = "BACK";
        KEY_NAMES[15] = "TAB";
        KEY_NAMES[16] = "Q";
        KEY_NAMES[17] = "W";
        KEY_NAMES[18] = "E";
        KEY_NAMES[19] = "R";
        KEY_NAMES[20] = "T";
        KEY_NAMES[21] = "Y";
        KEY_NAMES[22] = "U";
        KEY_NAMES[23] = "I";
        KEY_NAMES[24] = "O";
        KEY_NAMES[25] = "P";
        KEY_NAMES[26] = "LBRACKET";
        KEY_NAMES[27] = "RBRACKET";
        KEY_NAMES[28] = "RETURN";
        KEY_NAMES[29] = "LCONTROL";
        KEY_NAMES[30] = "A";
        KEY_NAMES[31] = "S";
        KEY_NAMES[32] = "D";
        KEY_NAMES[33] = "F";
        KEY_NAMES[34] = "G";
        KEY_NAMES[35] = "H";
        KEY_NAMES[36] = "J";
        KEY_NAMES[37] = "K";
        KEY_NAMES[38] = "L";
        KEY_NAMES[39] = "SEMICOLON";
        KEY_NAMES[40] = "APOSTROPHE";
        KEY_NAMES[41] = "GRAVE";
        KEY_NAMES[42] = "LSHIFT";
        KEY_NAMES[43] = "BACKSLASH";
        KEY_NAMES[44] = "Z";
        KEY_NAMES[45] = "X";
        KEY_NAMES[46] = "C";
        KEY_NAMES[47] = "V";
        KEY_NAMES[48] = "B";
        KEY_NAMES[49] = "N";
        KEY_NAMES[50] = "M";
        KEY_NAMES[51] = "COMMA";
        KEY_NAMES[52] = "PERIOD";
        KEY_NAMES[53] = "SLASH";
        KEY_NAMES[54] = "RSHIFT";
        KEY_NAMES[55] = "MULTIPLY";
        KEY_NAMES[56] = "LMENU";
        KEY_NAMES[57] = "SPACE";
        KEY_NAMES[58] = "CAPITAL";
        KEY_NAMES[59] = "F1";
        KEY_NAMES[60] = "F2";
        KEY_NAMES[61] = "F3";
        KEY_NAMES[62] = "F4";
        KEY_NAMES[63] = "F5";
        KEY_NAMES[64] = "F6";
        KEY_NAMES[65] = "F7";
        KEY_NAMES[66] = "F8";
        KEY_NAMES[67] = "F9";
        KEY_NAMES[68] = "F10";
        KEY_NAMES[69] = "NUMLOCK";
        KEY_NAMES[70] = "SCROLL";
        KEY_NAMES[71] = "NUMPAD7";
        KEY_NAMES[72] = "NUMPAD8";
        KEY_NAMES[73] = "NUMPAD9";
        KEY_NAMES[74] = "SUBTRACT";
        KEY_NAMES[75] = "NUMPAD4";
        KEY_NAMES[76] = "NUMPAD5";
        KEY_NAMES[77] = "NUMPAD6";
        KEY_NAMES[78] = "ADD";
        KEY_NAMES[79] = "NUMPAD1";
        KEY_NAMES[80] = "NUMPAD2";
        KEY_NAMES[81] = "NUMPAD3";
        KEY_NAMES[82] = "NUMPAD0";
        KEY_NAMES[83] = "DECIMAL";
        KEY_NAMES[87] = "F11";
        KEY_NAMES[88] = "F12";
        KEY_NAMES[100] = "F13";
        KEY_NAMES[101] = "F14";
        KEY_NAMES[102] = "F15";
        KEY_NAMES[103] = "F16";
        KEY_NAMES[104] = "F17";
        KEY_NAMES[105] = "F18";
        KEY_NAMES[112] = "KANA";
        KEY_NAMES[113] = "F19";
        KEY_NAMES[121] = "CONVERT";
        KEY_NAMES[123] = "NOCONVERT";
        KEY_NAMES[125] = "YEN";
        KEY_NAMES[141] = "NUMPADEQUALS";
        KEY_NAMES[144] = "CIRCUMFLEX";
        KEY_NAMES[145] = "AT";
        KEY_NAMES[146] = "COLON";
        KEY_NAMES[147] = "UNDERLINE";
        KEY_NAMES[148] = "KANJI";
        KEY_NAMES[149] = "STOP";
        KEY_NAMES[150] = "AX";
        KEY_NAMES[151] = "UNLABELED";
        KEY_NAMES[156] = "NUMPADENTER";
        KEY_NAMES[157] = "RCONTROL";
        KEY_NAMES[167] = "SECTION";
        KEY_NAMES[179] = "NUMPADCOMMA";
        KEY_NAMES[181] = "DIVIDE";
        KEY_NAMES[183] = "SYSRQ";
        KEY_NAMES[184] = "RMENU";
        KEY_NAMES[196] = "FUNCTION";
        KEY_NAMES[197] = "PAUSE";
        KEY_NAMES[199] = "HOME";
        KEY_NAMES[200] = "UP";
        KEY_NAMES[201] = "PRIOR";
        KEY_NAMES[203] = "LEFT";
        KEY_NAMES[205] = "RIGHT";
        KEY_NAMES[207] = "END";
        KEY_NAMES[208] = "DOWN";
        KEY_NAMES[209] = "NEXT";
        KEY_NAMES[210] = "INSERT";
        KEY_NAMES[211] = "DELETE";
        KEY_NAMES[218] = "CLEAR";
        KEY_NAMES[219] = "LMETA";
        KEY_NAMES[220] = "RMETA";
        KEY_NAMES[221] = "APPS";
        KEY_NAMES[222] = "POWER";
        KEY_NAMES[223] = "SLEEP";
        for (int i = 0; i < KEY_NAMES.length; i++) {
            String name = KEY_NAMES[i];
            if (name != null) KEY_INDICES.put(name, Integer.valueOf(i));
        }
    }

    private Keyboard() {}

    public static void create() {
        created = true;
        nReset();
    }

    public static void destroy() {
        created = false;
        repeatEvents = false;
        nReset();
    }

    public static void poll() { nPoll(); }
    public static boolean isCreated() { return created; }
    public static boolean isKeyDown(int key) { return created && nIsKeyDown(key); }
    public static synchronized String getKeyName(int key) { return KEY_NAMES[key]; }
    public static synchronized int getKeyIndex(String keyName) {
        Integer key = KEY_INDICES.get(keyName);
        return key == null ? KEY_NONE : key.intValue();
    }
    public static int getNumKeys() { return 256; }
    public static boolean next() { return created && nNext(); }
    public static int getEventKey() { return nGetEventKey(); }
    public static boolean getEventKeyState() { return nGetEventKeyState(); }
    public static char getEventCharacter() { return (char) nGetEventCharacter(); }
    public static long getEventNanoseconds() { return nGetEventNanoseconds(); }
    public static boolean isRepeatEvent() { return nIsRepeatEvent(); }

    public static void enableRepeatEvents(boolean enable) {
        if (!repeatLogPrinted) {
            repeatLogPrinted = true;
            System.out.println("Bridge Keyboard.enableRepeatEvents(" + enable + ")");
        }
        repeatEvents = enable;
        nSetRepeatEvents(enable);
    }

    public static boolean areRepeatEventsEnabled() { return repeatEvents; }

    private static native void nReset();
    private static native void nPoll();
    private static native boolean nIsKeyDown(int key);
    private static native boolean nNext();
    private static native int nGetEventKey();
    private static native boolean nGetEventKeyState();
    private static native int nGetEventCharacter();
    private static native long nGetEventNanoseconds();
    private static native boolean nIsRepeatEvent();
    private static native void nSetRepeatEvents(boolean enable);
}
