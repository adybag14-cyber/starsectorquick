const __npefixWarned = new Set();

function __npefixWarnOnce(symbol) {
    if (__npefixWarned.has(symbol)) return;
    __npefixWarned.add(symbol);
    console.warn(`[npefix] bridged ${symbol}`);
}

function __noop(symbol) {
    __npefixWarnOnce(symbol);
}

function __zero(symbol) {
    __npefixWarnOnce(symbol);
    return 0;
}

function __false(symbol) {
    __npefixWarnOnce(symbol);
    return false;
}

function __null(symbol) {
    __npefixWarnOnce(symbol);
    return null;
}

export function Java_java_lang_NullPointerException_getExtendedNPEMessage(env, obj) {
    return null;
}

export function Java_org_lwjgl_opengl_GL11_nglDisable(env, cap, addr) {
    __noop("Java_org_lwjgl_opengl_GL11_nglDisable");
}

export function Java_jdk_internal_misc_Unsafe_putByte() {
    __noop("Java_jdk_internal_misc_Unsafe_putByte");
}
export function Java_jdk_internal_misc_Unsafe_putByte__JB() {
    __noop("Java_jdk_internal_misc_Unsafe_putByte__JB");
}
export function Java_jdk_internal_misc_Unsafe_putByte__Ljava_lang_Object_2JB() {
    __noop("Java_jdk_internal_misc_Unsafe_putByte__Ljava_lang_Object_2JB");
}
export function Java_jdk_internal_misc_Unsafe_putByteVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putByteVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putByteVolatile__Ljava_lang_Object_2JB() {
    __noop("Java_jdk_internal_misc_Unsafe_putByteVolatile__Ljava_lang_Object_2JB");
}
export function Java_jdk_internal_misc_Unsafe_getByte() {
    return __zero("Java_jdk_internal_misc_Unsafe_getByte");
}
export function Java_jdk_internal_misc_Unsafe_getByte__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getByte__J");
}
export function Java_jdk_internal_misc_Unsafe_getByte__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getByte__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getByteVolatile() {
    return __zero("Java_jdk_internal_misc_Unsafe_getByteVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getByteVolatile__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getByteVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getByteVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getByteVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putShort() {
    __noop("Java_jdk_internal_misc_Unsafe_putShort");
}
export function Java_jdk_internal_misc_Unsafe_putShort__JS() {
    __noop("Java_jdk_internal_misc_Unsafe_putShort__JS");
}
export function Java_jdk_internal_misc_Unsafe_putShort__Ljava_lang_Object_2JS() {
    __noop("Java_jdk_internal_misc_Unsafe_putShort__Ljava_lang_Object_2JS");
}
export function Java_jdk_internal_misc_Unsafe_putShortVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putShortVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putShortVolatile__Ljava_lang_Object_2JS() {
    __noop("Java_jdk_internal_misc_Unsafe_putShortVolatile__Ljava_lang_Object_2JS");
}
export function Java_jdk_internal_misc_Unsafe_getShort() {
    return __zero("Java_jdk_internal_misc_Unsafe_getShort");
}
export function Java_jdk_internal_misc_Unsafe_getShort__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getShort__J");
}
export function Java_jdk_internal_misc_Unsafe_getShort__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getShort__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getShortVolatile() {
    return __zero("Java_jdk_internal_misc_Unsafe_getShortVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getShortVolatile__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getShortVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getShortVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getShortVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putChar() {
    __noop("Java_jdk_internal_misc_Unsafe_putChar");
}
export function Java_jdk_internal_misc_Unsafe_putChar__JC() {
    __noop("Java_jdk_internal_misc_Unsafe_putChar__JC");
}
export function Java_jdk_internal_misc_Unsafe_putChar__Ljava_lang_Object_2JC() {
    __noop("Java_jdk_internal_misc_Unsafe_putChar__Ljava_lang_Object_2JC");
}
export function Java_jdk_internal_misc_Unsafe_putCharVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putCharVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putCharVolatile__Ljava_lang_Object_2JC() {
    __noop("Java_jdk_internal_misc_Unsafe_putCharVolatile__Ljava_lang_Object_2JC");
}
export function Java_jdk_internal_misc_Unsafe_getChar() {
    return __zero("Java_jdk_internal_misc_Unsafe_getChar");
}
export function Java_jdk_internal_misc_Unsafe_getChar__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getChar__J");
}
export function Java_jdk_internal_misc_Unsafe_getChar__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getChar__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getCharVolatile() {
    return __zero("Java_jdk_internal_misc_Unsafe_getCharVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getCharVolatile__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getCharVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getCharVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getCharVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putInt() {
    __noop("Java_jdk_internal_misc_Unsafe_putInt");
}
export function Java_jdk_internal_misc_Unsafe_putInt__JI() {
    __noop("Java_jdk_internal_misc_Unsafe_putInt__JI");
}
export function Java_jdk_internal_misc_Unsafe_putInt__Ljava_lang_Object_2JI() {
    __noop("Java_jdk_internal_misc_Unsafe_putInt__Ljava_lang_Object_2JI");
}
export function Java_jdk_internal_misc_Unsafe_putIntVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putIntVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putIntVolatile__Ljava_lang_Object_2JI() {
    __noop("Java_jdk_internal_misc_Unsafe_putIntVolatile__Ljava_lang_Object_2JI");
}
export function Java_jdk_internal_misc_Unsafe_getInt() {
    return __zero("Java_jdk_internal_misc_Unsafe_getInt");
}
export function Java_jdk_internal_misc_Unsafe_getInt__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getInt__J");
}
export function Java_jdk_internal_misc_Unsafe_getInt__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getInt__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getIntVolatile() {
    return __zero("Java_jdk_internal_misc_Unsafe_getIntVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getIntVolatile__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getIntVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getIntVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getIntVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putLong() {
    __noop("Java_jdk_internal_misc_Unsafe_putLong");
}
export function Java_jdk_internal_misc_Unsafe_putLong__JJ() {
    __noop("Java_jdk_internal_misc_Unsafe_putLong__JJ");
}
export function Java_jdk_internal_misc_Unsafe_putLong__Ljava_lang_Object_2JJ() {
    __noop("Java_jdk_internal_misc_Unsafe_putLong__Ljava_lang_Object_2JJ");
}
export function Java_jdk_internal_misc_Unsafe_putLongVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putLongVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putLongVolatile__Ljava_lang_Object_2JJ() {
    __noop("Java_jdk_internal_misc_Unsafe_putLongVolatile__Ljava_lang_Object_2JJ");
}
export function Java_jdk_internal_misc_Unsafe_getLong() {
    return __zero("Java_jdk_internal_misc_Unsafe_getLong");
}
export function Java_jdk_internal_misc_Unsafe_getLong__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getLong__J");
}
export function Java_jdk_internal_misc_Unsafe_getLong__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getLong__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getLongVolatile() {
    return __zero("Java_jdk_internal_misc_Unsafe_getLongVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getLongVolatile__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getLongVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getLongVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getLongVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putFloat() {
    __noop("Java_jdk_internal_misc_Unsafe_putFloat");
}
export function Java_jdk_internal_misc_Unsafe_putFloat__JF() {
    __noop("Java_jdk_internal_misc_Unsafe_putFloat__JF");
}
export function Java_jdk_internal_misc_Unsafe_putFloat__Ljava_lang_Object_2JF() {
    __noop("Java_jdk_internal_misc_Unsafe_putFloat__Ljava_lang_Object_2JF");
}
export function Java_jdk_internal_misc_Unsafe_putFloatVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putFloatVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putFloatVolatile__Ljava_lang_Object_2JF() {
    __noop("Java_jdk_internal_misc_Unsafe_putFloatVolatile__Ljava_lang_Object_2JF");
}
export function Java_jdk_internal_misc_Unsafe_getFloat() {
    return __zero("Java_jdk_internal_misc_Unsafe_getFloat");
}
export function Java_jdk_internal_misc_Unsafe_getFloat__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getFloat__J");
}
export function Java_jdk_internal_misc_Unsafe_getFloat__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getFloat__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getFloatVolatile() {
    return __zero("Java_jdk_internal_misc_Unsafe_getFloatVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getFloatVolatile__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getFloatVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getFloatVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getFloatVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putDouble() {
    __noop("Java_jdk_internal_misc_Unsafe_putDouble");
}
export function Java_jdk_internal_misc_Unsafe_putDouble__JD() {
    __noop("Java_jdk_internal_misc_Unsafe_putDouble__JD");
}
export function Java_jdk_internal_misc_Unsafe_putDouble__Ljava_lang_Object_2JD() {
    __noop("Java_jdk_internal_misc_Unsafe_putDouble__Ljava_lang_Object_2JD");
}
export function Java_jdk_internal_misc_Unsafe_putDoubleVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putDoubleVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putDoubleVolatile__Ljava_lang_Object_2JD() {
    __noop("Java_jdk_internal_misc_Unsafe_putDoubleVolatile__Ljava_lang_Object_2JD");
}
export function Java_jdk_internal_misc_Unsafe_getDouble() {
    return __zero("Java_jdk_internal_misc_Unsafe_getDouble");
}
export function Java_jdk_internal_misc_Unsafe_getDouble__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getDouble__J");
}
export function Java_jdk_internal_misc_Unsafe_getDouble__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getDouble__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getDoubleVolatile() {
    return __zero("Java_jdk_internal_misc_Unsafe_getDoubleVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getDoubleVolatile__J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getDoubleVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getDoubleVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_jdk_internal_misc_Unsafe_getDoubleVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putBoolean() {
    __noop("Java_jdk_internal_misc_Unsafe_putBoolean");
}
export function Java_jdk_internal_misc_Unsafe_putBoolean__JZ() {
    __noop("Java_jdk_internal_misc_Unsafe_putBoolean__JZ");
}
export function Java_jdk_internal_misc_Unsafe_putBoolean__Ljava_lang_Object_2JZ() {
    __noop("Java_jdk_internal_misc_Unsafe_putBoolean__Ljava_lang_Object_2JZ");
}
export function Java_jdk_internal_misc_Unsafe_putBooleanVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putBooleanVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putBooleanVolatile__Ljava_lang_Object_2JZ() {
    __noop("Java_jdk_internal_misc_Unsafe_putBooleanVolatile__Ljava_lang_Object_2JZ");
}
export function Java_jdk_internal_misc_Unsafe_getBoolean() {
    return __false("Java_jdk_internal_misc_Unsafe_getBoolean");
}
export function Java_jdk_internal_misc_Unsafe_getBoolean__J() {
    return __false("Java_jdk_internal_misc_Unsafe_getBoolean__J");
}
export function Java_jdk_internal_misc_Unsafe_getBoolean__Ljava_lang_Object_2J() {
    return __false("Java_jdk_internal_misc_Unsafe_getBoolean__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getBooleanVolatile() {
    return __false("Java_jdk_internal_misc_Unsafe_getBooleanVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getBooleanVolatile__J() {
    return __false("Java_jdk_internal_misc_Unsafe_getBooleanVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getBooleanVolatile__Ljava_lang_Object_2J() {
    return __false("Java_jdk_internal_misc_Unsafe_getBooleanVolatile__Ljava_lang_Object_2J");
}

export function Java_jdk_internal_misc_Unsafe_putObject() {
    __noop("Java_jdk_internal_misc_Unsafe_putObject");
}
export function Java_jdk_internal_misc_Unsafe_putObject__JLjava_lang_Object_2() {
    __noop("Java_jdk_internal_misc_Unsafe_putObject__JLjava_lang_Object_2");
}
export function Java_jdk_internal_misc_Unsafe_putObject__Ljava_lang_Object_2JLjava_lang_Object_2() {
    __noop("Java_jdk_internal_misc_Unsafe_putObject__Ljava_lang_Object_2JLjava_lang_Object_2");
}
export function Java_jdk_internal_misc_Unsafe_putObjectVolatile() {
    __noop("Java_jdk_internal_misc_Unsafe_putObjectVolatile");
}
export function Java_jdk_internal_misc_Unsafe_putObjectVolatile__Ljava_lang_Object_2JLjava_lang_Object_2() {
    __noop("Java_jdk_internal_misc_Unsafe_putObjectVolatile__Ljava_lang_Object_2JLjava_lang_Object_2");
}
export function Java_jdk_internal_misc_Unsafe_getObject() {
    return __null("Java_jdk_internal_misc_Unsafe_getObject");
}
export function Java_jdk_internal_misc_Unsafe_getObject__J() {
    return __null("Java_jdk_internal_misc_Unsafe_getObject__J");
}
export function Java_jdk_internal_misc_Unsafe_getObject__Ljava_lang_Object_2J() {
    return __null("Java_jdk_internal_misc_Unsafe_getObject__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_getObjectVolatile() {
    return __null("Java_jdk_internal_misc_Unsafe_getObjectVolatile");
}
export function Java_jdk_internal_misc_Unsafe_getObjectVolatile__J() {
    return __null("Java_jdk_internal_misc_Unsafe_getObjectVolatile__J");
}
export function Java_jdk_internal_misc_Unsafe_getObjectVolatile__Ljava_lang_Object_2J() {
    return __null("Java_jdk_internal_misc_Unsafe_getObjectVolatile__Ljava_lang_Object_2J");
}
export function Java_jdk_internal_misc_Unsafe_compareAndSetInt() {
    return __false("Java_jdk_internal_misc_Unsafe_compareAndSetInt");
}
export function Java_jdk_internal_misc_Unsafe_compareAndSetLong() {
    return __false("Java_jdk_internal_misc_Unsafe_compareAndSetLong");
}
export function Java_jdk_internal_misc_Unsafe_compareAndSetReference() {
    return __false("Java_jdk_internal_misc_Unsafe_compareAndSetReference");
}

export function Java_sun_misc_Unsafe_putByte() {
    __noop("Java_sun_misc_Unsafe_putByte");
}
export function Java_sun_misc_Unsafe_putByte__JB() {
    __noop("Java_sun_misc_Unsafe_putByte__JB");
}
export function Java_sun_misc_Unsafe_putByte__Ljava_lang_Object_2JB() {
    __noop("Java_sun_misc_Unsafe_putByte__Ljava_lang_Object_2JB");
}
export function Java_sun_misc_Unsafe_putByteVolatile() {
    __noop("Java_sun_misc_Unsafe_putByteVolatile");
}
export function Java_sun_misc_Unsafe_putByteVolatile__Ljava_lang_Object_2JB() {
    __noop("Java_sun_misc_Unsafe_putByteVolatile__Ljava_lang_Object_2JB");
}
export function Java_sun_misc_Unsafe_getByte() {
    return __zero("Java_sun_misc_Unsafe_getByte");
}
export function Java_sun_misc_Unsafe_getByte__J() {
    return __zero("Java_sun_misc_Unsafe_getByte__J");
}
export function Java_sun_misc_Unsafe_getByte__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getByte__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getByteVolatile() {
    return __zero("Java_sun_misc_Unsafe_getByteVolatile");
}
export function Java_sun_misc_Unsafe_getByteVolatile__J() {
    return __zero("Java_sun_misc_Unsafe_getByteVolatile__J");
}
export function Java_sun_misc_Unsafe_getByteVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getByteVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putShort() {
    __noop("Java_sun_misc_Unsafe_putShort");
}
export function Java_sun_misc_Unsafe_putShort__JS() {
    __noop("Java_sun_misc_Unsafe_putShort__JS");
}
export function Java_sun_misc_Unsafe_putShort__Ljava_lang_Object_2JS() {
    __noop("Java_sun_misc_Unsafe_putShort__Ljava_lang_Object_2JS");
}
export function Java_sun_misc_Unsafe_putShortVolatile() {
    __noop("Java_sun_misc_Unsafe_putShortVolatile");
}
export function Java_sun_misc_Unsafe_putShortVolatile__Ljava_lang_Object_2JS() {
    __noop("Java_sun_misc_Unsafe_putShortVolatile__Ljava_lang_Object_2JS");
}
export function Java_sun_misc_Unsafe_getShort() {
    return __zero("Java_sun_misc_Unsafe_getShort");
}
export function Java_sun_misc_Unsafe_getShort__J() {
    return __zero("Java_sun_misc_Unsafe_getShort__J");
}
export function Java_sun_misc_Unsafe_getShort__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getShort__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getShortVolatile() {
    return __zero("Java_sun_misc_Unsafe_getShortVolatile");
}
export function Java_sun_misc_Unsafe_getShortVolatile__J() {
    return __zero("Java_sun_misc_Unsafe_getShortVolatile__J");
}
export function Java_sun_misc_Unsafe_getShortVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getShortVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putChar() {
    __noop("Java_sun_misc_Unsafe_putChar");
}
export function Java_sun_misc_Unsafe_putChar__JC() {
    __noop("Java_sun_misc_Unsafe_putChar__JC");
}
export function Java_sun_misc_Unsafe_putChar__Ljava_lang_Object_2JC() {
    __noop("Java_sun_misc_Unsafe_putChar__Ljava_lang_Object_2JC");
}
export function Java_sun_misc_Unsafe_putCharVolatile() {
    __noop("Java_sun_misc_Unsafe_putCharVolatile");
}
export function Java_sun_misc_Unsafe_putCharVolatile__Ljava_lang_Object_2JC() {
    __noop("Java_sun_misc_Unsafe_putCharVolatile__Ljava_lang_Object_2JC");
}
export function Java_sun_misc_Unsafe_getChar() {
    return __zero("Java_sun_misc_Unsafe_getChar");
}
export function Java_sun_misc_Unsafe_getChar__J() {
    return __zero("Java_sun_misc_Unsafe_getChar__J");
}
export function Java_sun_misc_Unsafe_getChar__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getChar__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getCharVolatile() {
    return __zero("Java_sun_misc_Unsafe_getCharVolatile");
}
export function Java_sun_misc_Unsafe_getCharVolatile__J() {
    return __zero("Java_sun_misc_Unsafe_getCharVolatile__J");
}
export function Java_sun_misc_Unsafe_getCharVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getCharVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putInt() {
    __noop("Java_sun_misc_Unsafe_putInt");
}
export function Java_sun_misc_Unsafe_putInt__JI() {
    __noop("Java_sun_misc_Unsafe_putInt__JI");
}
export function Java_sun_misc_Unsafe_putInt__Ljava_lang_Object_2JI() {
    __noop("Java_sun_misc_Unsafe_putInt__Ljava_lang_Object_2JI");
}
export function Java_sun_misc_Unsafe_putIntVolatile() {
    __noop("Java_sun_misc_Unsafe_putIntVolatile");
}
export function Java_sun_misc_Unsafe_putIntVolatile__Ljava_lang_Object_2JI() {
    __noop("Java_sun_misc_Unsafe_putIntVolatile__Ljava_lang_Object_2JI");
}
export function Java_sun_misc_Unsafe_getInt() {
    return __zero("Java_sun_misc_Unsafe_getInt");
}
export function Java_sun_misc_Unsafe_getInt__J() {
    return __zero("Java_sun_misc_Unsafe_getInt__J");
}
export function Java_sun_misc_Unsafe_getInt__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getInt__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getIntVolatile() {
    return __zero("Java_sun_misc_Unsafe_getIntVolatile");
}
export function Java_sun_misc_Unsafe_getIntVolatile__J() {
    return __zero("Java_sun_misc_Unsafe_getIntVolatile__J");
}
export function Java_sun_misc_Unsafe_getIntVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getIntVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putLong() {
    __noop("Java_sun_misc_Unsafe_putLong");
}
export function Java_sun_misc_Unsafe_putLong__JJ() {
    __noop("Java_sun_misc_Unsafe_putLong__JJ");
}
export function Java_sun_misc_Unsafe_putLong__Ljava_lang_Object_2JJ() {
    __noop("Java_sun_misc_Unsafe_putLong__Ljava_lang_Object_2JJ");
}
export function Java_sun_misc_Unsafe_putLongVolatile() {
    __noop("Java_sun_misc_Unsafe_putLongVolatile");
}
export function Java_sun_misc_Unsafe_putLongVolatile__Ljava_lang_Object_2JJ() {
    __noop("Java_sun_misc_Unsafe_putLongVolatile__Ljava_lang_Object_2JJ");
}
export function Java_sun_misc_Unsafe_getLong() {
    return __zero("Java_sun_misc_Unsafe_getLong");
}
export function Java_sun_misc_Unsafe_getLong__J() {
    return __zero("Java_sun_misc_Unsafe_getLong__J");
}
export function Java_sun_misc_Unsafe_getLong__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getLong__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getLongVolatile() {
    return __zero("Java_sun_misc_Unsafe_getLongVolatile");
}
export function Java_sun_misc_Unsafe_getLongVolatile__J() {
    return __zero("Java_sun_misc_Unsafe_getLongVolatile__J");
}
export function Java_sun_misc_Unsafe_getLongVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getLongVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putFloat() {
    __noop("Java_sun_misc_Unsafe_putFloat");
}
export function Java_sun_misc_Unsafe_putFloat__JF() {
    __noop("Java_sun_misc_Unsafe_putFloat__JF");
}
export function Java_sun_misc_Unsafe_putFloat__Ljava_lang_Object_2JF() {
    __noop("Java_sun_misc_Unsafe_putFloat__Ljava_lang_Object_2JF");
}
export function Java_sun_misc_Unsafe_putFloatVolatile() {
    __noop("Java_sun_misc_Unsafe_putFloatVolatile");
}
export function Java_sun_misc_Unsafe_putFloatVolatile__Ljava_lang_Object_2JF() {
    __noop("Java_sun_misc_Unsafe_putFloatVolatile__Ljava_lang_Object_2JF");
}
export function Java_sun_misc_Unsafe_getFloat() {
    return __zero("Java_sun_misc_Unsafe_getFloat");
}
export function Java_sun_misc_Unsafe_getFloat__J() {
    return __zero("Java_sun_misc_Unsafe_getFloat__J");
}
export function Java_sun_misc_Unsafe_getFloat__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getFloat__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getFloatVolatile() {
    return __zero("Java_sun_misc_Unsafe_getFloatVolatile");
}
export function Java_sun_misc_Unsafe_getFloatVolatile__J() {
    return __zero("Java_sun_misc_Unsafe_getFloatVolatile__J");
}
export function Java_sun_misc_Unsafe_getFloatVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getFloatVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putDouble() {
    __noop("Java_sun_misc_Unsafe_putDouble");
}
export function Java_sun_misc_Unsafe_putDouble__JD() {
    __noop("Java_sun_misc_Unsafe_putDouble__JD");
}
export function Java_sun_misc_Unsafe_putDouble__Ljava_lang_Object_2JD() {
    __noop("Java_sun_misc_Unsafe_putDouble__Ljava_lang_Object_2JD");
}
export function Java_sun_misc_Unsafe_putDoubleVolatile() {
    __noop("Java_sun_misc_Unsafe_putDoubleVolatile");
}
export function Java_sun_misc_Unsafe_putDoubleVolatile__Ljava_lang_Object_2JD() {
    __noop("Java_sun_misc_Unsafe_putDoubleVolatile__Ljava_lang_Object_2JD");
}
export function Java_sun_misc_Unsafe_getDouble() {
    return __zero("Java_sun_misc_Unsafe_getDouble");
}
export function Java_sun_misc_Unsafe_getDouble__J() {
    return __zero("Java_sun_misc_Unsafe_getDouble__J");
}
export function Java_sun_misc_Unsafe_getDouble__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getDouble__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getDoubleVolatile() {
    return __zero("Java_sun_misc_Unsafe_getDoubleVolatile");
}
export function Java_sun_misc_Unsafe_getDoubleVolatile__J() {
    return __zero("Java_sun_misc_Unsafe_getDoubleVolatile__J");
}
export function Java_sun_misc_Unsafe_getDoubleVolatile__Ljava_lang_Object_2J() {
    return __zero("Java_sun_misc_Unsafe_getDoubleVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putBoolean() {
    __noop("Java_sun_misc_Unsafe_putBoolean");
}
export function Java_sun_misc_Unsafe_putBoolean__JZ() {
    __noop("Java_sun_misc_Unsafe_putBoolean__JZ");
}
export function Java_sun_misc_Unsafe_putBoolean__Ljava_lang_Object_2JZ() {
    __noop("Java_sun_misc_Unsafe_putBoolean__Ljava_lang_Object_2JZ");
}
export function Java_sun_misc_Unsafe_putBooleanVolatile() {
    __noop("Java_sun_misc_Unsafe_putBooleanVolatile");
}
export function Java_sun_misc_Unsafe_putBooleanVolatile__Ljava_lang_Object_2JZ() {
    __noop("Java_sun_misc_Unsafe_putBooleanVolatile__Ljava_lang_Object_2JZ");
}
export function Java_sun_misc_Unsafe_getBoolean() {
    return __false("Java_sun_misc_Unsafe_getBoolean");
}
export function Java_sun_misc_Unsafe_getBoolean__J() {
    return __false("Java_sun_misc_Unsafe_getBoolean__J");
}
export function Java_sun_misc_Unsafe_getBoolean__Ljava_lang_Object_2J() {
    return __false("Java_sun_misc_Unsafe_getBoolean__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getBooleanVolatile() {
    return __false("Java_sun_misc_Unsafe_getBooleanVolatile");
}
export function Java_sun_misc_Unsafe_getBooleanVolatile__J() {
    return __false("Java_sun_misc_Unsafe_getBooleanVolatile__J");
}
export function Java_sun_misc_Unsafe_getBooleanVolatile__Ljava_lang_Object_2J() {
    return __false("Java_sun_misc_Unsafe_getBooleanVolatile__Ljava_lang_Object_2J");
}

export function Java_sun_misc_Unsafe_putObject() {
    __noop("Java_sun_misc_Unsafe_putObject");
}
export function Java_sun_misc_Unsafe_putObject__JLjava_lang_Object_2() {
    __noop("Java_sun_misc_Unsafe_putObject__JLjava_lang_Object_2");
}
export function Java_sun_misc_Unsafe_putObject__Ljava_lang_Object_2JLjava_lang_Object_2() {
    __noop("Java_sun_misc_Unsafe_putObject__Ljava_lang_Object_2JLjava_lang_Object_2");
}
export function Java_sun_misc_Unsafe_putObjectVolatile() {
    __noop("Java_sun_misc_Unsafe_putObjectVolatile");
}
export function Java_sun_misc_Unsafe_putObjectVolatile__Ljava_lang_Object_2JLjava_lang_Object_2() {
    __noop("Java_sun_misc_Unsafe_putObjectVolatile__Ljava_lang_Object_2JLjava_lang_Object_2");
}
export function Java_sun_misc_Unsafe_getObject() {
    return __null("Java_sun_misc_Unsafe_getObject");
}
export function Java_sun_misc_Unsafe_getObject__J() {
    return __null("Java_sun_misc_Unsafe_getObject__J");
}
export function Java_sun_misc_Unsafe_getObject__Ljava_lang_Object_2J() {
    return __null("Java_sun_misc_Unsafe_getObject__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_getObjectVolatile() {
    return __null("Java_sun_misc_Unsafe_getObjectVolatile");
}
export function Java_sun_misc_Unsafe_getObjectVolatile__J() {
    return __null("Java_sun_misc_Unsafe_getObjectVolatile__J");
}
export function Java_sun_misc_Unsafe_getObjectVolatile__Ljava_lang_Object_2J() {
    return __null("Java_sun_misc_Unsafe_getObjectVolatile__Ljava_lang_Object_2J");
}
export function Java_sun_misc_Unsafe_compareAndSetInt() {
    return __false("Java_sun_misc_Unsafe_compareAndSetInt");
}
export function Java_sun_misc_Unsafe_compareAndSetLong() {
    return __false("Java_sun_misc_Unsafe_compareAndSetLong");
}
export function Java_sun_misc_Unsafe_compareAndSetReference() {
    return __false("Java_sun_misc_Unsafe_compareAndSetReference");
}

export function JNI_OnLoad(lib, vm, reserved) {
    console.log("NPEFIX: JNI_OnLoad called");
    return 0x00010002;
}

