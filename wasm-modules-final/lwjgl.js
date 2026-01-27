
console.error("Hybrid LWJGL Stub Execution Started V6!");

const JNI_VERSION_1_6 = 0x00010006;

// Implementation functions
async function cheerpjInitLibrary(env) {
    console.error("Hybrid Stub: cheerpjInitLibrary");
    return 0;
}

async function cheerpjVerifyLibrary() {
    console.error("Hybrid Stub: cheerpjVerifyLibrary");
    return true;
}

async function JNI_OnLoad(vm, reserved) {
    console.error("Hybrid Stub: JNI_OnLoad");
    return JNI_VERSION_1_6;
}

async function JNI_OnLoad_lwjgl(vm, reserved) {
    console.error("Hybrid Stub: JNI_OnLoad_lwjgl");
    return JNI_VERSION_1_6;
}

async function JNI_OnLoad_lwjgl64(vm, reserved) {
    console.error("Hybrid Stub: JNI_OnLoad_lwjgl64");
    return JNI_VERSION_1_6;
}

async function Java_org_lwjgl_Sys_ngetNativeTime(env, clazz) {
    return BigInt(Date.now()) * BigInt(1000000);
}

async function Java_org_lwjgl_DefaultSysImplementation_getPointerSize() {
    return 4;
}

async function Java_org_lwjgl_Sys_getTimerResolution() {
    return BigInt(1000);
}

async function Java_org_lwjgl_Sys_alert(env, clazz, title, message) {
    console.error("!!! NATIVE ALERT TRAPPED !!!");
    console.error("Title:", title);
    console.error("Message:", message);

    // Attempt to stringify if they are objects
    if (typeof title === 'object' && title !== null) {
        try { console.error("Title Debug:", JSON.stringify(title)); } catch (e) { }
    }
    if (typeof message === 'object' && message !== null) {
        try { console.error("Message Debug:", JSON.stringify(message)); } catch (e) { }
    }
}

// 1. Export for Module System (CheerpJ loadLibrary)
export {
    cheerpjInitLibrary,
    cheerpjVerifyLibrary,
    JNI_OnLoad,
    JNI_OnLoad_lwjgl,
    JNI_OnLoad_lwjgl64,
    Java_org_lwjgl_Sys_ngetNativeTime,
    Java_org_lwjgl_DefaultSysImplementation_getPointerSize,
    Java_org_lwjgl_Sys_getTimerResolution,
    Java_org_lwjgl_Sys_alert
};

// 2. Assign to Window for Global Lookup
window.cheerpjInitLibrary = cheerpjInitLibrary;
window.cheerpjVerifyLibrary = cheerpjVerifyLibrary;
window.JNI_OnLoad = JNI_OnLoad;
window.JNI_OnLoad_lwjgl = JNI_OnLoad_lwjgl;
window.JNI_OnLoad_lwjgl64 = JNI_OnLoad_lwjgl64;
window.Java_org_lwjgl_Sys_ngetNativeTime = Java_org_lwjgl_Sys_ngetNativeTime;
window.Java_org_lwjgl_DefaultSysImplementation_getPointerSize = Java_org_lwjgl_DefaultSysImplementation_getPointerSize;
window.Java_org_lwjgl_Sys_getTimerResolution = Java_org_lwjgl_Sys_getTimerResolution;
window.Java_org_lwjgl_Sys_alert = Java_org_lwjgl_Sys_alert;

// 3. Assign to 'lwjgl' global object
window.lwjgl = {
    JNI_OnLoad,
    JNI_OnLoad_lwjgl
};

console.error("Hybrid LWJGL Stub Fully Loaded V6!");
