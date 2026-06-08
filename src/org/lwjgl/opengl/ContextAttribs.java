package org.lwjgl.opengl;

import java.nio.IntBuffer;

public final class ContextAttribs {
    private static boolean attribListLogged = false;
    public static final int CONTEXT_MAJOR_VERSION_ARB = 8337;
    public static final int CONTEXT_MINOR_VERSION_ARB = 8338;
    public static final int CONTEXT_PROFILE_MASK_ARB = 37158;
    public static final int CONTEXT_CORE_PROFILE_BIT_ARB = 1;
    public static final int CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB = 2;
    public static final int CONTEXT_ES2_PROFILE_BIT_EXT = 4;
    public static final int CONTEXT_FLAGS_ARB = 8340;
    public static final int CONTEXT_DEBUG_BIT_ARB = 1;
    public static final int CONTEXT_FORWARD_COMPATIBLE_BIT_ARB = 2;
    public static final int CONTEXT_ROBUST_ACCESS_BIT_ARB = 4;
    public static final int CONTEXT_RESET_ISOLATION_BIT_ARB = 8;
    public static final int CONTEXT_RESET_NOTIFICATION_STRATEGY_ARB = 33366;
    public static final int NO_RESET_NOTIFICATION_ARB = 33377;
    public static final int LOSE_CONTEXT_ON_RESET_ARB = 33362;
    public static final int CONTEXT_RELEASE_BEHABIOR_ARB = 33531;
    public static final int CONTEXT_RELEASE_BEHAVIOR_NONE_ARB = 0;
    public static final int CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB = 33531;
    public static final int CONTEXT_LAYER_PLANE_ARB = 8339;

    private final int majorVersion;
    private final int minorVersion;
    private final int profileMask;
    private final int contextFlags;
    private final int contextResetNotificationStrategy;
    private final int contextReleaseBehavior;
    private final int layerPlane;

    public ContextAttribs() {
        this(1, 0);
    }

    public ContextAttribs(int major, int minor) {
        this(major, minor, 0, 0);
    }

    public ContextAttribs(int major, int minor, int layer) {
        this(major, minor, layer, 0);
    }

    public ContextAttribs(int major, int minor, int layer, int flags) {
        this.majorVersion = major;
        this.minorVersion = minor;
        this.layerPlane = layer;
        this.contextFlags = flags;
        this.profileMask = 0;
        this.contextResetNotificationStrategy = NO_RESET_NOTIFICATION_ARB;
        this.contextReleaseBehavior = CONTEXT_RELEASE_BEHAVIOR_NONE_ARB;
    }

    private ContextAttribs(ContextAttribs other) {
        this.majorVersion = other.majorVersion;
        this.minorVersion = other.minorVersion;
        this.layerPlane = other.layerPlane;
        this.contextFlags = other.contextFlags;
        this.profileMask = other.profileMask;
        this.contextResetNotificationStrategy = other.contextResetNotificationStrategy;
        this.contextReleaseBehavior = other.contextReleaseBehavior;
    }

    public int getMajorVersion() { return majorVersion; }
    public int getMinorVersion() { return minorVersion; }
    public int getProfileMask() { return profileMask; }
    public boolean isProfileCore() { return false; }
    public boolean isProfileCompatibility() { return false; }
    public boolean isProfileES() { return false; }
    public int getContextFlags() { return contextFlags; }
    public boolean isDebug() { return false; }
    public boolean isForwardCompatible() { return false; }
    public boolean isRobustAccess() { return false; }
    public boolean isContextResetIsolation() { return false; }
    public int getContextResetNotificationStrategy() { return contextResetNotificationStrategy; }
    public boolean isLoseContextOnReset() { return false; }
    public int getContextReleaseBehavior() { return contextReleaseBehavior; }
    public int getLayerPlane() { return layerPlane; }
    public ContextAttribs withProfileCore(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withProfileCompatibility(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withProfileES(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withDebug(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withForwardCompatible(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withRobustAccess(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withContextResetIsolation(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withResetNotificationStrategy(int value) { return new ContextAttribs(this); }
    public ContextAttribs withLoseContextOnReset(boolean value) { return new ContextAttribs(this); }
    public ContextAttribs withContextReleaseBehavior(int value) { return new ContextAttribs(this); }
    public ContextAttribs withLayer(int value) { return new ContextAttribs(this); }
    IntBuffer getAttribList() {
        if (!attribListLogged) {
            attribListLogged = true;
            System.out.println("Bridge ContextAttribs.getAttribList()");
        }
        return null;
    }
    @Override
    public String toString() { return "ContextAttribs[" + majorVersion + "." + minorVersion + "]"; }
}
