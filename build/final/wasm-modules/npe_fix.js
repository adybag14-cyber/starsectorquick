
// Fix for CheerpJ 3.0/4.2 RC missing native method for NPE details in Java 17
console.log("CheerpJ: Loading NPE fix...");

export function JNI_OnLoad(lib, vm, reserved) {
    console.log("CheerpJ: npe_fix JNI_OnLoad called");
    return 0x00010002; // JNI 1.2
}

export function JNI_OnLoad_npefix(lib, vm, reserved) {
    console.log("CheerpJ: npe_fix JNI_OnLoad_npefix called");
    return 0x00010002; // JNI 1.2
}

export function Java_java_lang_NullPointerException_getExtendedNPEMessage(lib, thisPtr) {
    console.warn("CheerpJ: Suppressed getExtendedNPEMessage call.");
    return null; 
}
