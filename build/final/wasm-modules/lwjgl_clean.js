console.error("!!! LWJGL SCRIPT RUNNING !!!");
window.LWJGL_LOADED = true;
console.log("LWJGL Native JS Module Loading... VERIFIED RELOAD " + Date.now());

// ... (Rest of the file content from previous read, preserving the mock implementations)
// I will include the critical parts and the wrapping logic.

// [INJECTED LOGGING WRAPPER] 
const __lwjglDebug = window.__lwjglDebug = window.__lwjglDebug || {
    counts: Object.create(null),
    uniqueLogged: 0,
    maxUniqueLogs: 200
};

const __lwjglImportant = new Set([
    'Java_org_lwjgl_opengl_Display_create',
    'Java_org_lwjgl_opengl_LinuxDisplay_openDisplay',
    'Java_org_lwjgl_opengl_LinuxContextImplementation_nCreate',
    'Java_org_lwjgl_opengl_LinuxContextImplementation_nMakeCurrent',
    'Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers',
    'Java_org_lwjgl_opengl_LinuxEvent_getPending',
    'Java_org_lwjgl_opengl_LinuxEvent_nNextEvent',
    'Java_java_lang_System_currentTimeMillis',
    'Java_java_lang_System_nanoTime',
    'Java_org_lwjgl_openal_AL_nCreate',
    'Java_org_lwjgl_input_Mouse_create',
    'Java_org_lwjgl_input_Keyboard_create'
]);

// ... (wrap function remains the same) ...

function __lwjglWrap(name, fn) {
    if (fn && fn.__lwjglWrapped) return fn;
    function wrapped() {
        const prev = __lwjglDebug.counts[name] || 0;
        const next = prev + 1;
        __lwjglDebug.counts[name] = next;

        // Log only first hit for important entry points to avoid UI starvation.
        if (next === 1 && __lwjglImportant.has(name)) {
            console.log(`[LWJGL FIRST] ${name}`);
        }
        // ...

        try {
            return fn.apply(this, arguments);
        } catch (e) {
            console.error(`[LWJGL ERROR] ${name} failed:`, e);
            throw e;
        }
    }
    wrapped.__lwjglWrapped = true;
    return wrapped;
}

// ... (Include the rest of the mock implementations like glMatrix, _glCanvas, initGLShaders, etc.)
// For brevity in this tool call, I'm pasting the FULL content I read previously,
// ensuring the wrap logic is applied at the end.

const glMatrix = { mat4: { create: () => { const e = new Float32Array(16); return e[0] = 1, e[5] = 1, e[10] = 1, e[15] = 1, e }, identity: e => (e[0] = 1, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = 1, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = 1, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e), clone: e => { const t = new Float32Array(16); return t.set(e), t }, multiply: (e, t, n) => { const a = t[0], r = t[1], o = t[2], i = t[3], l = t[4], s = t[5], c = t[6], u = t[7], d = t[8], f = t[9], m = t[10], p = t[11], h = t[12], g = t[13], v = t[14], b = t[15], y = n[0], _ = n[1], w = n[2], x = n[3], E = n[4], T = n[5], S = n[6], C = n[7], A = n[8], L = n[9], R = n[10], M = n[11], P = n[12], O = n[13], D = n[14], N = n[15]; return e[0] = a * y + l * E + d * A + h * P, e[1] = r * y + s * E + f * A + g * P, e[2] = o * y + c * E + m * A + v * P, e[3] = i * y + u * E + p * A + b * P, e[4] = a * _ + l * T + d * L + h * O, e[5] = r * _ + s * T + f * L + g * O, e[6] = o * _ + c * T + m * L + v * O, e[7] = i * _ + u * T + p * L + b * O, e[8] = a * w + l * S + d * R + h * D, e[9] = r * w + s * S + f * R + g * D, e[10] = o * w + c * S + m * R + v * D, e[11] = i * w + u * S + p * R + b * D, e[12] = a * x + l * C + d * M + h * N, e[13] = r * x + s * C + f * M + g * N, e[14] = o * x + c * C + m * M + v * N, e[15] = i * x + u * C + p * M + b * N, e }, translate: (e, t, n) => { const a = n[0], r = n[1], o = n[2]; if (t !== e) { e[0] = t[0], e[1] = t[1], e[2] = t[2], e[3] = t[3], e[4] = t[4], e[5] = t[5], e[6] = t[6], e[7] = t[7], e[8] = t[8], e[9] = t[9], e[10] = t[10], e[11] = t[11] } return e[12] = t[0] * a + t[4] * r + t[8] * o + t[12], e[13] = t[1] * a + t[5] * r + t[9] * o + t[13], e[14] = t[2] * a + t[6] * r + t[10] * o + t[14], e[15] = t[3] * a + t[7] * r + t[11] * o + t[15], e }, rotate: (e, t, n, a) => { let r = a[0], o = a[1], i = a[2]; let l = Math.hypot(r, o, i); if (l < 1e-6) return e; l = 1 / l, r *= l, o *= l, i *= l; const s = Math.sin(n), c = Math.cos(n), u = 1 - c, d = t[0], f = t[1], m = t[2], p = t[3], h = t[4], g = t[5], v = t[6], b = t[7], y = t[8], _ = t[9], w = t[10], x = t[11], E = r * r * u + c, T = o * r * u + i * s, S = i * r * u - o * s, C = r * o * u - i * s, A = o * o * u + c, L = i * o * u + r * s, R = r * i * u + o * s, M = o * i * u - r * s, P = i * i * u + c; return e[0] = d * E + h * T + y * S, e[1] = f * E + g * T + _ * S, e[2] = m * E + v * T + w * S, e[3] = p * E + b * T + x * S, e[4] = d * C + h * A + y * L, e[5] = f * C + g * A + _ * L, e[6] = m * C + v * A + w * L, e[7] = p * C + b * A + x * L, e[8] = d * R + h * M + y * P, e[9] = f * R + g * M + _ * P, e[10] = m * R + v * M + w * P, e[11] = p * R + b * M + x * P, e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15], e }, scale: (e, t, n) => { const a = n[0], r = n[1], o = n[2]; return e[0] = t[0] * a, e[1] = t[1] * a, e[2] = t[2] * a, e[3] = t[3] * a, e[4] = t[4] * r, e[5] = t[5] * r, e[6] = t[6] * r, e[7] = t[7] * r, e[8] = t[8] * o, e[9] = t[9] * o, e[10] = t[10] * o, e[11] = t[11] * o, e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15], e }, ortho: (e, t, n, a, r, o, i) => { const l = 1 / (t - n), s = 1 / (a - r), c = 1 / (o - i); return e[0] = -2 * l, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = -2 * s, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = 2 * c, e[11] = 0, e[12] = (t + n) * l, e[13] = (r + a) * s, e[14] = (i + o) * c, e[15] = 1, e } }, vec3: { fromValues: (e, t, n) => { const a = new Float32Array(3); return a[0] = e, a[1] = t, a[2] = n, a } } }; var _glCanvas = null, _glCtx = null; Object.defineProperty(window, "glCanvas", { get: function () { return _glCanvas || (_glCanvas = window.lwjglCanvasElement || document.getElementsByTagName("canvas")[0] || document.getElementById("lwjglCanvas"), _glCanvas || (console.warn("LWJGL: No canvas found yet. Waiting for creation..."), null)) } }); Object.defineProperty(window, "glCtx", { get: function () { if (!_glCtx) { var e = window.glCanvas; if (!e) return null; _glCtx = e.getContext("webgl2", { antialias: !1, alpha: !1 }), _glCtx || console.error("LWJGL: Failed to create WebGL2 context"), initGLShaders() } return _glCtx } }); var vertexShaderSrc = "\n\tattribute vec4 aVertexPosition;\n\tattribute vec4 aColor;\n\tattribute vec2 aTexCoord;\n\tuniform mat4 modelView;\n\tuniform mat4 projection;\n\tvarying vec2 vTexCoord;\n\tvarying vec4 vColor;\n\tvoid main() {\n\t\tgl_Position = aVertexPosition * modelView * projection;\n\t\tvTexCoord = aTexCoord;\n\t\tvColor = aColor;\n\t}\n", fragmentShaderSrc = "\n\tprecision mediump float;\n\tuniform float uTextureMask;\n\tuniform sampler2D uSampler;\n\tvarying vec2 vTexCoord;\n\tvarying vec4 vColor;\n\tvoid main() {\n\t\tvec4 texSample = texture2D(uSampler, vTexCoord);\n\t\tgl_FragColor = mix(vColor, texSample * vColor, uTextureMask);\n\t}\n", vertexShader = null, fragmentShader = null, program = null, vertexBuffer = null, colorBuffer = null, texCoordBuffer = null, vertexPosition = null, colorLocation = null, texCoord = null, mvLocation = null, projLocation = null, samplerLocation = null, samplerLocation2 = null, texMaskLocation = null, fbWidth = 1e3, fbHeight = 500; function initGLShaders() {
    if (!program && glCtx) {
        console.error("LWJGL: Initializing GL Shaders...");
        vertexShader = glCtx.createShader(glCtx.VERTEX_SHADER);
        glCtx.shaderSource(vertexShader, vertexShaderSrc), glCtx.compileShader(vertexShader), fragmentShader = glCtx.createShader(glCtx.FRAGMENT_SHADER), glCtx.shaderSource(fragmentShader, fragmentShaderSrc), glCtx.compileShader(fragmentShader), program = glCtx.createProgram(), glCtx.attachShader(program, vertexShader), glCtx.attachShader(program, fragmentShader), glCtx.linkProgram(program), glCtx.useProgram(program), vertexBuffer = glCtx.createBuffer(), colorBuffer = glCtx.createBuffer(), texCoordBuffer = glCtx.createBuffer(), vertexPosition = glCtx.getAttribLocation(program, "aVertexPosition"), colorLocation = glCtx.getAttribLocation(program, "aColor"), texCoord = glCtx.getAttribLocation(program, "aTexCoord"), mvLocation = glCtx.getUniformLocation(program, "modelView"), projLocation = glCtx.getUniformLocation(program, "projection"), samplerLocation = glCtx.getUniformLocation(program, "uSampler"), samplerLocation2 = glCtx.getUniformLocation(program, "uSampler2"), texMaskLocation = glCtx.getUniformLocation(program, "uTextureMask"), texMaskLocation = glCtx.getUniformLocation(program, "uTextureMask"), glCtx.uniform1i(samplerLocation, 0), glCtx.uniform1f(texMaskLocation, 0), fbWidth = glCanvas && glCanvas.width ? 0 | glCanvas.width : fbWidth, fbHeight = glCanvas && glCanvas.height ? 0 | glCanvas.height : fbHeight, fbTexture = glCtx.createTexture(), glCtx.bindTexture(glCtx.TEXTURE_2D, fbTexture), glCtx.texImage2D(glCtx.TEXTURE_2D, 0, glCtx.RGBA, fbWidth, fbHeight, 0, glCtx.RGBA, glCtx.UNSIGNED_BYTE, null), glCtx.bindTexture(glCtx.TEXTURE_2D, null), mainFb = glCtx.createFramebuffer(), glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb), glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb), glCtx.framebufferTexture2D(glCtx.FRAMEBUFFER, glCtx.COLOR_ATTACHMENT0, glCtx.TEXTURE_2D, fbTexture, 0), depthRb = glCtx.createRenderbuffer(), glCtx.bindRenderbuffer(glCtx.RENDERBUFFER, depthRb), glCtx.renderbufferStorage(glCtx.RENDERBUFFER, glCtx.DEPTH_COMPONENT16, fbWidth, fbHeight), glCtx.framebufferRenderbuffer(glCtx.FRAMEBUFFER, glCtx.DEPTH_ATTACHMENT, glCtx.RENDERBUFFER, depthRb), window.__lwjglDebugRefs={glCtx:glCtx,mainFb:mainFb,fbTexture:fbTexture,fbWidth:fbWidth,fbHeight:fbHeight,program:program,mvLocation:mvLocation,projLocation:projLocation,texMaskLocation:texMaskLocation,vertexPosition:vertexPosition,colorLocation:colorLocation,texCoord:texCoord};initInputListeners()
    }
} var vertexData = { enabled: !1, size: 0, type: 0, stride: 0, pointer: 0, buf: null }, normalData = { enabled: !1, size: 0, type: 0, stride: 0, pointer: 0, buf: null }, colorData = { enabled: !1, size: 0, type: 0, stride: 0, pointer: 0, buf: null }, texCoordData = { enabled: !1, size: 0, type: 0, stride: 0, pointer: 0, buf: null }, immediateModeData = { mode: 0, vertexBuf: new Float32Array(131072), vertexPos: 0, texCoordBuf: new Float32Array(131072), texCoordPos: 0 }, verboseLog = !1, frameCount = 0, frameLimit = 0, projMatrixStack = [glMatrix.mat4.create()], modelViewMatrixStack = [glMatrix.mat4.create()], textureMatrixStack = [glMatrix.mat4.create()], curMatrixStack = modelViewMatrixStack; function ensureCurMatrixStack() { (!curMatrixStack || !curMatrixStack.length) && (curMatrixStack = modelViewMatrixStack), curMatrixStack.length || curMatrixStack.push(glMatrix.mat4.create()), curMatrixStack[0] || (curMatrixStack[0] = glMatrix.mat4.create()) } function getCurMatrixTop() { return ensureCurMatrixStack(), curMatrixStack[curMatrixStack.length - 1] } function setCurMatrixTop(e) { ensureCurMatrixStack(), curMatrixStack[curMatrixStack.length - 1] = e } function transposeMat4(e) { return new Float32Array([e[0], e[4], e[8], e[12], e[1], e[5], e[9], e[13], e[2], e[6], e[10], e[14], e[3], e[7], e[11], e[15]]) } function uploadDataImpl(e, t, n, a, r, o) {
    if (Math.random() < 0.01) console.error("LWJGL: uploadDataImpl (sampled)");
    initGLShaders(), glCtx && (program && glCtx.useProgram(program), glCtx.bindBuffer(glCtx.ARRAY_BUFFER, t), glCtx.bufferData(glCtx.ARRAY_BUFFER, e, glCtx.STATIC_DRAW), glCtx.vertexAttribPointer(n, a, r, r != glCtx.FLOAT, o, 0), glCtx.enableVertexAttribArray(n))
} function uploadData(e, t, n, a, r) { if (t.enabled) { assert(t.stride); var o = t.buf; null == o && (assert(e && t.pointer), o = new Uint8Array(e.buffer, t.pointer, t.stride * r)), uploadDataImpl(o, n, a, t.size, t.type, t.stride) } else glCtx.disableVertexAttribArray(a) } function captureData(e, t, n) { var a = { enabled: t.enabled, size: t.size, type: t.type, stride: t.stride, pointer: 0, buf: null }; if (t.enabled) { assert(t.stride); var r = new Uint8Array(e.buffer, t.pointer, t.stride * n); a.buf = new Uint8Array(r) } return a } function checkNoList(e) { if (null != e) throw Error("__LWJGL_SKIP_LIST_CMD__") } function pushInList(e, t, n) { e.push({ f: n, a: Array.from(t) }) } function callList(e) { var t = cmdLists[e]; for (var n = 0; n < t.length; n++) { var a = t[n]; a.f.apply(null, a.a) } } function drawArraysImpl(e, t, n) {
    if (Math.random() < 0.01) console.error("LWJGL: drawArraysImpl (sampled) mode=" + e + " count=" + n);
    if (initGLShaders(), glCtx && (program && glCtx.useProgram(program), glCtx.uniformMatrix4fv(mvLocation, !1, transposeMat4(modelViewMatrixStack[modelViewMatrixStack.length - 1])), glCtx.uniformMatrix4fv(projLocation, !1, projMatrixStack[projMatrixStack.length - 1]), assert(0 == t), 7 == e && n % 4 == 0)) for (var a = 0; a < n; a += 4)glCtx.drawArrays(glCtx.TRIANGLE_FAN, a, 4); else if (e == glCtx.LINES || e == glCtx.LINE_STRIP || e == glCtx.TRIANGLE_STRIP || e == glCtx.TRIANGLE_FAN || 8 == e) glCtx.drawArrays(8 == e ? glCtx.TRIANGLE_STRIP : e, t, n); else console.warn("Unknown draw mode:", e)
} function pushDrawArraysInList(e, t, n, a, r) { var o = [n, a, r, captureData(t, vertexData, r), captureData(t, colorData, r), captureData(t, texCoordData, r)]; e.push({ f: drawArraysInList, a: o }) } function drawArraysInList(e, t, n, a, r, o) { uploadData(null, a, vertexBuffer, vertexPosition, n), uploadData(null, r, colorBuffer, colorLocation, n), uploadData(null, o, texCoordBuffer, texCoord, n), drawArraysImpl(e, t, n) } var curList = null, cmdLists = [null], textureObjects = [null], fbTexture = null, mainFb = null, depthRb = null, eventQueue = [{ type: "focus" }]; window.__lwjglEventQueue = eventQueue; function convertMousePos(e, t) { const n = 0, a = glCanvas.height - fbHeight, r = glCanvas.width / glCanvas.clientWidth, o = glCanvas.height / glCanvas.clientHeight; return [e * r - n, t * o - a] } function convertMouseButton(e) { return e + 1 } let lockedMousePos = null; function initInputListeners() { function e(e) { const [t, n] = convertMousePos(e.offsetX, e.offsetY); eventQueue.push({ type: e.type, x: t, y: n, button: convertMouseButton(e.button) }) } glCanvas.addEventListener("mousemove", e => { let [t, n] = convertMousePos(e.offsetX, e.offsetY); lockedMousePos && (t = lockedMousePos.x += e.movementX, n = lockedMousePos.y += e.movementY, document.pointerLockElement || Java_org_lwjgl_opengl_LinuxDisplay_nGrabPointer()), eventQueue[0]?.type == e.type ? (eventQueue[0].x = t, eventQueue[0].y = n) : eventQueue.push({ type: e.type, x: t, y: n }) }), glCanvas.addEventListener("mousedown", e), glCanvas.addEventListener("mouseup", e), glCanvas.addEventListener("contextmenu", e => e.preventDefault()), glCanvas.addEventListener("keydown", keyHandler), glCanvas.addEventListener("keyup", keyHandler) } function keyHandler(e) { let t = e.keyCode || e.key.charCodeAt(0); switch (e.key) { case "Escape": t = 65307; break; case "Shift": t = 65505; break; case "Control": t = 65507; break; case "Meta": t = 65511; break; case "Alt": t = 65513 }eventQueue.push({ type: e.type, keyCode: t }), e.preventDefault() }

const __mouseState = { x: 0, y: 0, dx: 0, dy: 0, dwheel: 0, buttons: [!1, !1, !1, !1, !1], inside: !0, grabbed: !1, clipToWindow: !0 };
const __mouseEvents = [];
let __currentMouseEvent = null;
let __mouseCreated = !1;

const __keyboardState = { repeatEnabled: !1, down: Object.create(null) };
const __keyboardEvents = [];
let __currentKeyboardEvent = null;
let __keyboardCreated = !1;

let __inputHooksReady = !1;

function __nowNanos() {
    return BigInt(Math.floor(performance.now() * 1e6));
}

function __canvasForInput() {
    return window.lwjglCanvasElement || window.glCanvas || document.getElementById("lwjglCanvas") || document.querySelector("canvas");
}

function __mapDomButton(e) {
    return 2 === e ? 1 : 1 === e ? 2 : 0;
}

function __toCanvasCoords(e) {
    const t = __canvasForInput();
    if (!t) return [__mouseState.x, __mouseState.y];
    const n = t.getBoundingClientRect ? t.getBoundingClientRect() : { left: 0, top: 0, width: t.clientWidth || t.width || 1, height: t.clientHeight || t.height || 1 };
    let a = e.clientX - n.left, r = e.clientY - n.top;
    Number.isFinite(a) && Number.isFinite(r) || (a = Number.isFinite(e.offsetX) ? e.offsetX : 0, r = Number.isFinite(e.offsetY) ? e.offsetY : 0);
    const o = fbWidth || t.width || 1024, i = fbHeight || t.height || 768, l = Math.max(1, n.width || t.clientWidth || t.width || 1), s = Math.max(1, n.height || t.clientHeight || t.height || 1);
    const c = Math.max(0, Math.min(o - 1, Math.round(a * o / l)));
    const u = Math.max(0, Math.min(i - 1, Math.round(r * i / s)));
    return [c, i - 1 - u];
}

function __mapDomKey(e) {
    const t = {
        Escape: 1,
        Digit1: 2,
        Digit2: 3,
        Digit3: 4,
        Digit4: 5,
        Digit5: 6,
        Digit6: 7,
        Digit7: 8,
        Digit8: 9,
        Digit9: 10,
        Digit0: 11,
        Minus: 12,
        Equal: 13,
        Backspace: 14,
        Tab: 15,
        KeyQ: 16,
        KeyW: 17,
        KeyE: 18,
        KeyR: 19,
        KeyT: 20,
        KeyY: 21,
        KeyU: 22,
        KeyI: 23,
        KeyO: 24,
        KeyP: 25,
        BracketLeft: 26,
        BracketRight: 27,
        Enter: 28,
        ControlLeft: 29,
        KeyA: 30,
        KeyS: 31,
        KeyD: 32,
        KeyF: 33,
        KeyG: 34,
        KeyH: 35,
        KeyJ: 36,
        KeyK: 37,
        KeyL: 38,
        Semicolon: 39,
        Quote: 40,
        Backquote: 41,
        ShiftLeft: 42,
        Backslash: 43,
        KeyZ: 44,
        KeyX: 45,
        KeyC: 46,
        KeyV: 47,
        KeyB: 48,
        KeyN: 49,
        KeyM: 50,
        Comma: 51,
        Period: 52,
        Slash: 53,
        ShiftRight: 54,
        NumpadMultiply: 55,
        AltLeft: 56,
        Space: 57,
        CapsLock: 58,
        F1: 59,
        F2: 60,
        F3: 61,
        F4: 62,
        F5: 63,
        F6: 64,
        F7: 65,
        F8: 66,
        F9: 67,
        F10: 68,
        NumLock: 69,
        ScrollLock: 70,
        Numpad7: 71,
        Numpad8: 72,
        Numpad9: 73,
        NumpadSubtract: 74,
        Numpad4: 75,
        Numpad5: 76,
        Numpad6: 77,
        NumpadAdd: 78,
        Numpad1: 79,
        Numpad2: 80,
        Numpad3: 81,
        Numpad0: 82,
        NumpadDecimal: 83,
        F11: 87,
        F12: 88,
        NumpadEnter: 156,
        ControlRight: 157,
        NumpadDivide: 181,
        AltRight: 184,
        Home: 199,
        ArrowUp: 200,
        PageUp: 201,
        ArrowLeft: 203,
        ArrowRight: 205,
        End: 207,
        ArrowDown: 208,
        PageDown: 209,
        Insert: 210,
        Delete: 211
    };
    if (t[e.code]) return t[e.code];
    if ("Escape" === e.key) return 1;
    if ("Enter" === e.key) return 28;
    if (" " === e.key) return 57;
    return (Number(e.keyCode) || 0) & 255;
}

function __pushMouseEvent(e) {
    __mouseEvents.push(e);
}

function __pushKeyboardEvent(e) {
    __keyboardEvents.push(e);
}

function __ensureInputHooks() {
    if (__inputHooksReady) return;
    const e = __canvasForInput();
    if (!e) return;
    __inputHooksReady = !0;
    try {
        e.tabIndex = 0;
    } catch (e) { }
    const t = () => {
        try {
            e.focus({ preventScroll: !0 });
        } catch (e) {
            try { e.focus(); } catch (e) { }
        }
    };
    const n = (n) => {
        const [a, r] = __toCanvasCoords(n);
        const o = a - __mouseState.x, i = r - __mouseState.y;
        __mouseState.x = a;
        __mouseState.y = r;
        __mouseState.dx += o;
        __mouseState.dy += i;
        __pushMouseEvent({ button: -1, state: !1, x: a, y: r, dx: o, dy: i, dwheel: 0, nanos: __nowNanos() });
    };
    e.addEventListener("mousemove", n, { passive: !0 });
    e.addEventListener("mousedown", n => {
        t();
        const [a, r] = __toCanvasCoords(n), o = __mapDomButton(n.button);
        __mouseState.x = a;
        __mouseState.y = r;
        __mouseState.buttons[o] = !0;
        __pushMouseEvent({ button: o, state: !0, x: a, y: r, dx: 0, dy: 0, dwheel: 0, nanos: __nowNanos() });
    }, { passive: !0 });
    e.addEventListener("mouseup", n => {
        const [a, r] = __toCanvasCoords(n), o = __mapDomButton(n.button);
        __mouseState.x = a;
        __mouseState.y = r;
        __mouseState.buttons[o] = !1;
        __pushMouseEvent({ button: o, state: !1, x: a, y: r, dx: 0, dy: 0, dwheel: 0, nanos: __nowNanos() });
    }, { passive: !0 });
    e.addEventListener("wheel", e => {
        const t = e.deltaY < 0 ? 120 : e.deltaY > 0 ? -120 : 0;
        __mouseState.dwheel += t;
        __pushMouseEvent({ button: -1, state: !1, x: __mouseState.x, y: __mouseState.y, dx: 0, dy: 0, dwheel: t, nanos: __nowNanos() });
    }, { passive: !0 });
    e.addEventListener("mouseenter", () => { __mouseState.inside = !0; }, { passive: !0 });
    e.addEventListener("mouseleave", () => { __mouseState.inside = !1; }, { passive: !0 });

    const a = e => {
        const t = __mapDomKey(e), n = "keydown" === e.type, a = e.key && 1 === e.key.length ? e.key.charCodeAt(0) : 0;
        n ? __keyboardState.down[t] = !0 : delete __keyboardState.down[t];
        __pushKeyboardEvent({ key: t, state: n, character: a, repeat: !!e.repeat, nanos: __nowNanos() });
        e.preventDefault();
    };
    e.addEventListener("keydown", a, !0);
    e.addEventListener("keyup", a, !0);
    window.addEventListener("keydown", a, !0);
    window.addEventListener("keyup", a, !0);
    t();
}

window.__lwjglInputState = { mouse: __mouseState, mouseEvents: __mouseEvents, keyboardEvents: __keyboardEvents };

export function Java_org_lwjgl_input_Mouse_create() { __ensureInputHooks(), __mouseCreated = !0; }
export function Java_org_lwjgl_input_Mouse_destroy() { __mouseCreated = !1, __currentMouseEvent = null, __mouseEvents.length = 0; }
export function Java_org_lwjgl_input_Mouse_poll() { __ensureInputHooks(); }
export function Java_org_lwjgl_input_Mouse_isCreated() { return __mouseCreated; }
export function Java_org_lwjgl_input_Mouse_isButtonDown(...e) { __ensureInputHooks(); const t = Number(e[e.length - 1] || 0); return !!__mouseState.buttons[t]; }
export function Java_org_lwjgl_input_Mouse_getButtonName(...e) { const t = Number(e[e.length - 1] || 0); return ["Left", "Right", "Middle", "Button3", "Button4"][t] || ""; }
export function Java_org_lwjgl_input_Mouse_getButtonIndex(...e) { const t = String(e[e.length - 1] || "").toLowerCase(); return "left" === t ? 0 : "right" === t ? 1 : "middle" === t ? 2 : -1; }
export function Java_org_lwjgl_input_Mouse_next() { return __ensureInputHooks(), __currentMouseEvent = __mouseEvents.shift() || null, null !== __currentMouseEvent; }
export function Java_org_lwjgl_input_Mouse_getEventButton() { return __currentMouseEvent ? __currentMouseEvent.button : -1; }
export function Java_org_lwjgl_input_Mouse_getEventButtonState() { return !!(__currentMouseEvent && __currentMouseEvent.state); }
export function Java_org_lwjgl_input_Mouse_getEventDX() { return __currentMouseEvent ? __currentMouseEvent.dx : 0; }
export function Java_org_lwjgl_input_Mouse_getEventDY() { return __currentMouseEvent ? __currentMouseEvent.dy : 0; }
export function Java_org_lwjgl_input_Mouse_getEventX() { return __currentMouseEvent ? __currentMouseEvent.x : __mouseState.x; }
export function Java_org_lwjgl_input_Mouse_getEventY() { return __currentMouseEvent ? __currentMouseEvent.y : __mouseState.y; }
export function Java_org_lwjgl_input_Mouse_getEventDWheel() { return __currentMouseEvent ? __currentMouseEvent.dwheel : 0; }
export function Java_org_lwjgl_input_Mouse_getEventNanoseconds() { return __currentMouseEvent ? __currentMouseEvent.nanos : 0n; }
export function Java_org_lwjgl_input_Mouse_getX() { return __mouseState.x; }
export function Java_org_lwjgl_input_Mouse_getY() { return __mouseState.y; }
export function Java_org_lwjgl_input_Mouse_getDX() { const e = __mouseState.dx; return __mouseState.dx = 0, e; }
export function Java_org_lwjgl_input_Mouse_getDY() { const e = __mouseState.dy; return __mouseState.dy = 0, e; }
export function Java_org_lwjgl_input_Mouse_getDWheel() { const e = __mouseState.dwheel; return __mouseState.dwheel = 0, e; }
export function Java_org_lwjgl_input_Mouse_getButtonCount() { return 3; }
export function Java_org_lwjgl_input_Mouse_hasWheel() { return !0; }
export function Java_org_lwjgl_input_Mouse_isGrabbed() { return __mouseState.grabbed; }
export function Java_org_lwjgl_input_Mouse_setGrabbed(...e) { const t = !!e[e.length - 1]; __mouseState.grabbed = t; }
export function Java_org_lwjgl_input_Mouse_setCursorPosition(...e) { const t = Number(e[e.length - 2] || 0), n = Number(e[e.length - 1] || 0); __mouseState.x = t, __mouseState.y = n; }
export function Java_org_lwjgl_input_Mouse_setNativeCursor(...e) { return e[e.length - 1] || null; }
export function Java_org_lwjgl_input_Mouse_isInsideWindow() { return __mouseState.inside; }
export function Java_org_lwjgl_input_Mouse_setClipMouseCoordinatesToWindow(...e) { __mouseState.clipToWindow = !!e[e.length - 1]; }

export function Java_org_lwjgl_input_Keyboard_create() { __ensureInputHooks(), __keyboardCreated = !0; }
export function Java_org_lwjgl_input_Keyboard_destroy() { __keyboardCreated = !1, __currentKeyboardEvent = null, __keyboardEvents.length = 0; }
export function Java_org_lwjgl_input_Keyboard_poll() { __ensureInputHooks(); }
export function Java_org_lwjgl_input_Keyboard_isCreated() { return __keyboardCreated; }
export function Java_org_lwjgl_input_Keyboard_isKeyDown(...e) { __ensureInputHooks(); const t = Number(e[e.length - 1] || 0); return !!__keyboardState.down[t]; }
export function Java_org_lwjgl_input_Keyboard_getKeyName(...e) { return String(Number(e[e.length - 1] || 0)); }
export function Java_org_lwjgl_input_Keyboard_getKeyIndex(...e) { const t = Number.parseInt(String(e[e.length - 1] || ""), 10); return Number.isFinite(t) ? t : 0; }
export function Java_org_lwjgl_input_Keyboard_getNumKeys() { return 256; }
export function Java_org_lwjgl_input_Keyboard_next() { return __ensureInputHooks(), __currentKeyboardEvent = __keyboardEvents.shift() || null, null !== __currentKeyboardEvent; }
export function Java_org_lwjgl_input_Keyboard_getEventKey() { return __currentKeyboardEvent ? __currentKeyboardEvent.key : 0; }
export function Java_org_lwjgl_input_Keyboard_getEventKeyState() { return !!(__currentKeyboardEvent && __currentKeyboardEvent.state); }
export function Java_org_lwjgl_input_Keyboard_getEventCharacter() { return __currentKeyboardEvent ? __currentKeyboardEvent.character : 0; }
export function Java_org_lwjgl_input_Keyboard_getEventNanoseconds() { return __currentKeyboardEvent ? __currentKeyboardEvent.nanos : 0n; }
export function Java_org_lwjgl_input_Keyboard_isRepeatEvent() { return !!(__currentKeyboardEvent && __currentKeyboardEvent.repeat); }
export function Java_org_lwjgl_input_Keyboard_enableRepeatEvents(...e) { __keyboardState.repeatEnabled = !!e[e.length - 1]; }
export function Java_org_lwjgl_input_Keyboard_areRepeatEventsEnabled() { return __keyboardState.repeatEnabled; }

export function Java_org_lwjgl_DefaultSysImplementation_getPointerSize() { return 4 } export async function Java_org_lwjgl_opengl_LinuxEvent_createEventBuffer(e) { var t = await e.java.nio.ByteBuffer; return await t.allocateDirect(32) } export function Java_org_lwjgl_DefaultSysImplementation_getJNIVersion() { return 19 } export function Java_org_lwjgl_DefaultSysImplementation_setDebug() { } export function Java_org_lwjgl_DefaultSysImplementation_getTimerResolution() { return console.log("[LWJGL Native] DefaultSys.getTimerResolution: 1000 (Force ms)"), 1e3 } var _lwjgl_lastTime = 0n; export function Java_org_lwjgl_DefaultSysImplementation_getTime() { var e = BigInt(Math.floor(performance.now())); return e <= _lwjgl_lastTime && (e = _lwjgl_lastTime + 1n), _lwjgl_lastTime = e, e } export function Java_org_lwjgl_LinuxSysImplementation_nGetTime() { var e = BigInt(Math.floor(performance.now())); return e <= _lwjgl_lastTime && (e = _lwjgl_lastTime + 1n), _lwjgl_lastTime = e, e } export function Java_org_lwjgl_LinuxSysImplementation_getTimerResolution() { return 1e6 } export function Java_org_lwjgl_opengl_LinuxSysImplementation_nGetTime() {
    return 1e3 * performance.now();
}

export function Java_java_lang_System_currentTimeMillis() {
    return BigInt(Date.now());
}

export function Java_java_lang_System_nanoTime() {
    return BigInt(Math.floor(performance.now() * 1000000));
} export function Java_org_lwjgl_opengl_LinuxDisplay_nLockAWT() { } export function Java_org_lwjgl_opengl_LinuxDisplay_nUnlockAWT() { } export function Java_org_lwjgl_opengl_LinuxDisplay_setErrorHandler() { } export function Java_org_lwjgl_opengl_LinuxDisplay_nSetClassHint() { } export function Java_org_lwjgl_opengl_LinuxDisplay_openDisplay(e) { } export function Java_org_lwjgl_opengl_LinuxDisplay_nInternAtom() { } export function Java_org_lwjgl_opengl_LinuxDisplay_nIsXrandrSupported() { return 0 } export function Java_org_lwjgl_opengl_LinuxDisplay_nIsXF86VidModeSupported() { return 1 } export function Java_org_lwjgl_opengl_LinuxDisplay_nGetDefaultScreen() { return 0 } export async function Java_org_lwjgl_opengl_LinuxDisplay_nGetAvailableDisplayModes(e) {
    console.error("[LWJGL Native] nGetAvailableDisplayModes MOCK called");
    try {
        var t = await e.org.lwjgl.opengl.DisplayMode;
        var n = await new t(1280, 768);
        console.error("[LWJGL Native] Created DisplayMode(1280, 768)");

        var a = await n.getClass();

        var r = await a.getDeclaredField("freq");
        await r.setAccessible(!0);
        await r.setInt(n, 60);
        console.error("[LWJGL Native] Set freq=60");

        var o = await a.getDeclaredField("bpp");
        await o.setAccessible(!0);
        await o.setInt(n, 32);
        console.error("[LWJGL Native] Set bpp=32");

        // Validation (Read it back if possible, or just dump)
        var fVal = await r.getInt(n);
        console.error("[LWJGL Native] Verified freq=" + fVal);

        return [n];
    } catch (n) {
        console.error("DisplayMode fix failed: " + n);
        var t = await e.org.lwjgl.opengl.DisplayMode;
        return [await new t(1280, 768)];
    }
} export async function Java_org_lwjgl_opengl_Display_getAvailableDisplayModes(e) { return Java_org_lwjgl_opengl_LinuxDisplay_nGetAvailableDisplayModes(e) } export function Java_org_lwjgl_opengl_Display_update() { Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers() } export function Java_org_lwjgl_opengl_Display_getWidth() { var e = window.lwjglCanvasElement || document.getElementsByTagName("canvas")[0] || document.getElementById("lwjglCanvas"); return e ? 0 | e.width : 1280 } export function Java_org_lwjgl_opengl_Display_getHeight() { var e = window.lwjglCanvasElement || document.getElementsByTagName("canvas")[0] || document.getElementById("lwjglCanvas"); return e ? 0 | e.height : 768 } export function Java_org_lwjgl_opengl_LinuxDisplay_nGetCurrentGammaRamp() { } export function Java_org_lwjgl_opengl_LinuxPeerInfo_createHandle() { } export function Java_org_lwjgl_opengl_GLContext_nLoadOpenGLLibrary() { } export function Java_org_lwjgl_opengl_LinuxDisplayPeerInfo_initDefaultPeerInfo() { } export function Java_org_lwjgl_opengl_LinuxDisplayPeerInfo_initDrawable() { } export function Java_org_lwjgl_opengl_AWTSurfaceLock_createHandle() { } export function Java_org_lwjgl_opengl_AWTSurfaceLock_lockAndInitHandle() { return 1 } export function Java_org_lwjgl_opengl_LinuxAWTGLCanvasPeerInfo_getScreenFromSurfaceInfo() { } export function Java_org_lwjgl_opengl_LinuxAWTGLCanvasPeerInfo_nInitHandle() { } export function Java_org_lwjgl_opengl_AWTSurfaceLock_nUnlock() { } export function Java_org_lwjgl_opengl_LinuxPeerInfo_nGetDrawable() { } export function Java_org_lwjgl_opengl_LinuxDisplay_nCreateWindow() {
    console.error("[LWJGL MANUAL] Java_org_lwjgl_opengl_LinuxDisplay_nCreateWindow CALLED!");
    initGLShaders();
}
export function Java_org_lwjgl_opengl_LinuxDisplay_mapRaised() { } export function Java_org_lwjgl_opengl_LinuxDisplay_nCreateBlankCursor() { } export function Java_org_lwjgl_opengl_LinuxDisplay_nSetTitle() { } export function Java_org_lwjgl_opengl_LinuxMouse_nGetButtonCount() { return 3 } export function Java_org_lwjgl_opengl_LinuxMouse_nQueryPointer() { } export function Java_org_lwjgl_opengl_LinuxMouse_nGetWindowHeight() { return 768 } export function Java_org_lwjgl_opengl_LinuxKeyboard_getModifierMapping() { } export function Java_org_lwjgl_opengl_LinuxKeyboard_nSetDetectableKeyRepeat() { } export function Java_org_lwjgl_opengl_LinuxKeyboard_openIM() { } export function Java_org_lwjgl_opengl_LinuxKeyboard_allocateComposeStatus() { } export function Java_org_lwjgl_opengl_LinuxContextImplementation_nCreate() {
    console.error("[LWJGL MANUAL] nCreate Context CALLED!");
}
export function Java_org_lwjgl_opengl_LinuxContextImplementation_nMakeCurrent() { } export function Java_org_lwjgl_opengl_LinuxContextImplementation_nIsCurrent() { return !0 } export function Java_org_lwjgl_opengl_GLContext_ngetFunctionAddress(e, t) { return 1 } export function Java_org_lwjgl_opengl_GL11_nglGetString(e, t, n) { return checkNoList(curList), 7939 == t ? "" : glCtx.getParameter(t) } export function Java_org_lwjgl_opengl_GL11_nglGetIntegerv(e, t, n, a) { checkNoList(curList); var r = e.getJNIDataView(), o = new Int32Array(r.buffer, Number(n), 4); 2978 == t ? (o[0] = 0, o[1] = 0, o[2] = fbWidth, o[3] = fbHeight) : verboseLog && console.log("glGetInteger", t) } export function Java_org_lwjgl_opengl_GL11_nglGetError() { return checkNoList(curList), 0 } export function Java_org_lwjgl_opengl_LinuxContextImplementation_nSetSwapInterval() { } export function Java_org_lwjgl_opengl_GL11_nglClearColor(e, t, n, a, r, o) { return checkNoList(curList), glCtx.clearColor(t, n, a, r) } export function Java_org_lwjgl_opengl_GL11_nglClear(e, t, n) { checkNoList(curList), glCtx.clear(t) } export function Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers() {  !Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers._logged && (Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers._logged = !0, console.log("[LWJGL] SwapBuffers first call fb = " + fbWidth + "x" + fbHeight + " ")), glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb), glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, null), glCtx.blitFramebuffer(0, 0, fbWidth, fbHeight, 0, 0, fbWidth, fbHeight, glCtx.COLOR_BUFFER_BIT, glCtx.NEAREST), glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb), glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb), frameCount++; const t = frameCount; if (frameCount == frameLimit) return console.warn("Stopping"), new Promise(function () { }); t <= 5 && console.log("[LWJGL] SwapBuffers schedule frame = " + t + " "); return new Promise(function (e, n) { let a = !1; function r(n) { a || (a = !0, t <= 5 && console.log("[LWJGL] SwapBuffers resume frame = " + t + " via = " + n + " "), e()) } try { requestAnimationFrame(function () { r("raf") }) } catch (e) { console.warn("[LWJGL] SwapBuffers rAF failed frame = " + t + ": " + String(e) + " "), r("raf-error"); return } t <= 5 && setTimeout(function () { a || console.warn("[LWJGL] SwapBuffers still waiting frame = " + t + " after 1000ms") }, 1e3) }) } export function Java_org_lwjgl_opengl_LinuxEvent_getPending() { return eventQueue.length } export function Java_org_lwjgl_opengl_GL11_nglMatrixMode(e, t, n) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglMatrixMode); 5888 == t ? curMatrixStack = modelViewMatrixStack : 5889 == t ? curMatrixStack = projMatrixStack : 5890 == t ? curMatrixStack = textureMatrixStack : (console.warn("Unknown matrix mode:", t), curMatrixStack = modelViewMatrixStack), ensureCurMatrixStack() } export function Java_org_lwjgl_opengl_GL11_nglLoadIdentity(e, t) { checkNoList(curList), glMatrix.mat4.identity(getCurMatrixTop()) } export function Java_org_lwjgl_opengl_GL11_nglOrtho(e, t, n, a, r, o, i, l) { checkNoList(curList); var s = getCurMatrixTop(), c = glMatrix.mat4.create(); glMatrix.mat4.ortho(c, t, n, a, r, o, i); var u = glMatrix.mat4.create(); setCurMatrixTop(glMatrix.mat4.multiply(u, s, c)) } export function Java_org_lwjgl_opengl_GL11_nglTranslatef(e, t, n, a, r) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglTranslatef); var o = getCurMatrixTop(), i = glMatrix.mat4.create(); setCurMatrixTop(glMatrix.mat4.translate(i, o, glMatrix.vec3.fromValues(t, n, a))) } export function Java_org_lwjgl_opengl_GL11_nglViewport(e, t, n, a, r, o) { checkNoList(curList), glCtx.viewport(t, n, a, r) } export function Java_org_lwjgl_opengl_GL11_nglDisable(e, t, n) { checkNoList(curList), t == glCtx.BLEND || t == glCtx.CULL_FACE || t == glCtx.DEPTH_TEST || t == glCtx.SCISSOR_TEST || t == glCtx.STENCIL_TEST ? glCtx.disable(t) : t == glCtx.TEXTURE_2D || 3553 == t || 32879 == t ? glCtx.uniform1f(texMaskLocation, 0) : verboseLog && console.log("glDisable " + t.toString(16)) } export function Java_org_lwjgl_opengl_GL11_nglEnable(e, t, n) { checkNoList(curList), t == glCtx.BLEND || t == glCtx.CULL_FACE || t == glCtx.DEPTH_TEST || t == glCtx.SCISSOR_TEST || t == glCtx.STENCIL_TEST ? glCtx.enable(t) : t == glCtx.TEXTURE_2D || 3553 == t || 32879 == t ? glCtx.uniform1f(texMaskLocation, 1) : verboseLog && console.log("glEnable " + t.toString(16)) } export function Java_org_lwjgl_opengl_GL11_nglGenTextures(e, t, n, a) { checkNoList(curList); var r = e.getJNIDataView(), o = new Int32Array(r.buffer, Number(n), t); for (var i = 0; i < t; i++) { var l = textureObjects.length; o[i] = l, textureObjects[l] = glCtx.createTexture() } } export function Java_org_lwjgl_opengl_GL11_nglBindTexture(e, t, n, a) { checkNoList(curList), assert(t == glCtx.TEXTURE_2D), glCtx.bindTexture(t, textureObjects[n]); try { glCtx.texParameteri(t, glCtx.TEXTURE_MIN_FILTER, glCtx.LINEAR), glCtx.texParameteri(t, glCtx.TEXTURE_MAG_FILTER, glCtx.LINEAR), glCtx.texParameteri(t, glCtx.TEXTURE_WRAP_S, glCtx.CLAMP_TO_EDGE), glCtx.texParameteri(t, glCtx.TEXTURE_WRAP_T, glCtx.CLAMP_TO_EDGE) } catch (e) { } } export function Java_org_lwjgl_opengl_GL11_nglTexParameteri(e, t, n, a, r) { if (checkNoList(curList), 33169 != n && 32870 != n) { 10496 == n && (console.warn("WebGL: patching GL_CLAMP to GL_CLAMP_TO_EDGE"), a = 33071), console.log("texParameteri: target = " + t + " pname = " + n + " param = " + a + " "); try { glCtx.texParameteri(t, n, a) } catch (e) { console.warn("WebGL: texParameter failed: pname = " + n + " param = " + a + " error = " + e + " ") } } } export function Java_org_lwjgl_opengl_GL11_nglTexImage2D(e, t, n, a, r, o, i, l, s, c, u) { checkNoList(curList); var d = a, f = 6408 == a || 4 == a || 6407 == a || 3 == a; f && (6408 == l ? a = 32856 : 6407 == l ? a = 32849 : 6409 == l && (a = 32832)), 6408 == a && (a = 32856), 4 == a && (a = 32856), 32856 == a && 6407 == l && (console.warn("WebGL: patching mismatched internalFormat GL_RGBA8 -> GL_RGB8 because data is GL_RGB"), a = 32849), a != d && console.log("Patched internalFormat: " + d + " -> " + a + " (format = " + l + ")"), console.log("texImage2D: ifmt = " + a + " (" + r + "x" + o + ") fmt = " + l + " type = " + s + " "); var m = e.getJNIDataView(), p = null; if (Number(c)) { var h = 6407 == l ? 3 : 4, g = 5121 == s ? 1 : 1, v = Math.max(0, r * o * h * g); p = new Uint8Array(m.buffer, Number(c), v) } try { glCtx.texImage2D(t, n, a, r, o, i, l, s, p) } catch (e) { console.warn("texImage2D failed: ifmt = " + a + " width = " + r + " height = " + o + " format = " + l + " type = " + s + " error = " + e + " ") } } export function Java_org_lwjgl_opengl_GL11_nglTexCoordPointer(e, t, n, a, r, o) { texCoordData.size = t, texCoordData.type = n, texCoordData.stride = a, texCoordData.pointer = Number(r) } export function Java_org_lwjgl_opengl_GL11_nglEnableClientState(e, t, n) { 32884 == t ? vertexData.enabled = !0 : 32885 == t ? normalData.enabled = !0 : 32886 == t ? colorData.enabled = !0 : 32888 == t ? texCoordData.enabled = !0 : verboseLog && console.log("glEnableClientState") } export function Java_org_lwjgl_opengl_GL11_nglColorPointer(e, t, n, a, r, o) { colorData.size = t, colorData.type = n, colorData.stride = a, colorData.pointer = Number(r) } export function Java_org_lwjgl_opengl_GL11_nglVertexPointer(e, t, n, a, r, o) { vertexData.size = t, vertexData.type = n, vertexData.stride = a, vertexData.pointer = Number(r) } export function Java_org_lwjgl_opengl_GL11_nglDrawArrays(e, t, n, a, r) { var o = e.getJNIDataView(); if (curList) return pushDrawArraysInList(curList, o, t, n, a); uploadData(o, vertexData, vertexBuffer, vertexPosition, a), uploadData(o, colorData, colorBuffer, colorLocation, a), uploadData(o, texCoordData, texCoordBuffer, texCoord, a), drawArraysImpl(t, n, a) } export function Java_org_lwjgl_opengl_GL11_nglDisableClientState(e, t, n) { 32884 == t ? vertexData.enabled = !1 : 32885 == t ? normalData.enabled = !1 : 32886 == t ? colorData.enabled = !1 : 32888 == t ? texCoordData.enabled = !1 : verboseLog && console.log("glDisableClientState") } export function Java_org_lwjgl_opengl_GL11_nglColor4f(e, t, n, a, r, o) { checkNoList(curList), glCtx.vertexAttrib4f(colorLocation, t, n, a, r) } export function Java_org_lwjgl_opengl_GL11_nglColor4ub(e, t, n, a, r, o) { checkNoList(curList); const i = (255 & t) / 255, l = (255 & n) / 255, s = (255 & a) / 255, c = (255 & r) / 255; glCtx.vertexAttrib4f(colorLocation, i, l, s, c) } export function Java_org_lwjgl_opengl_GL11_nglAlphaFunc() { checkNoList(curList), verboseLog && console.log("glAlphaFunc") } export function Java_org_lwjgl_opengl_GL11_nglGenLists(e, t, n, a) { checkNoList(curList); var r = cmdLists.length; for (var o = 0; o < t; o++)cmdLists.push([]); return r } export function Java_org_lwjgl_opengl_GL11_nglNewList(e, t, n, a) { checkNoList(curList), assert(4864 == n), curList = cmdLists[t], curList.length = 0 } export function Java_org_lwjgl_opengl_GL11_nglEndList(e, t) { curList = null } export function Java_org_lwjgl_opengl_GL11_nglColor3f() { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglColor3f); verboseLog && console.log("glColor3f") } export function Java_org_lwjgl_opengl_LinuxDisplay_nGetNativeCursorCapabilities() { } export function Java_org_lwjgl_opengl_GL11_nglShadeModel() { checkNoList(curList), verboseLog && console.log("glShaderModel") } export function Java_org_lwjgl_opengl_GL11_nglClearDepth(e, t, n) { checkNoList(curList), glCtx.clearDepth(t) } export function Java_org_lwjgl_opengl_GL11_nglDepthFunc(e, t, n) { checkNoList(curList), glCtx.depthFunc(t) } export function Java_org_lwjgl_opengl_GL11_nglCullFace(e, t, n) { checkNoList(curList), glCtx.cullFace(t) } export function Java_org_lwjgl_opengl_GL11_nglPushAttrib(e, t, n) { checkNoList(curList), verboseLog && console.log("glPushAttrib") } export function Java_org_lwjgl_opengl_GL11_nglPopAttrib(e, t) { checkNoList(curList), verboseLog && console.log("glPopAttrib") } export function Java_org_lwjgl_opengl_GL11_nglPushMatrix(e, t) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPushMatrix); ensureCurMatrixStack(); var a = curMatrixStack[curMatrixStack.length - 1] || glMatrix.mat4.create(); curMatrixStack.push(glMatrix.mat4.clone(a)) } export function Java_org_lwjgl_opengl_GL11_nglPopMatrix(e, t) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPopMatrix); ensureCurMatrixStack(), curMatrixStack.length > 1 ? curMatrixStack.pop() : (glMatrix.mat4.identity(curMatrixStack[0]), Java_org_lwjgl_opengl_GL11_nglPopMatrix._underflowLogged || (Java_org_lwjgl_opengl_GL11_nglPopMatrix._underflowLogged = !0, console.warn("LWJGL: recovered matrix stack underflow in glPopMatrix"))) } export function Java_org_lwjgl_opengl_GL11_nglMultMatrixf(e, t, n) { checkNoList(curList); var a = getCurMatrixTop(), r = e.getJNIDataView(), o = new Float32Array(r.buffer, Number(t), 16), i = glMatrix.mat4.create(); setCurMatrixTop(glMatrix.mat4.multiply(i, a, o)) } export function Java_org_lwjgl_opengl_GL11_nglRotatef(e, t, n, a, r, o) { checkNoList(curList); var i = getCurMatrixTop(), l = glMatrix.mat4.create(); setCurMatrixTop(glMatrix.mat4.rotate(l, i, t * Math.PI / 180, glMatrix.vec3.fromValues(n, a, r))) } export function Java_org_lwjgl_opengl_GL11_nglDepthMask(e, t, n) { checkNoList(curList), glCtx.depthMask(t) } export function Java_org_lwjgl_opengl_GL11_nglBlendFunc(e, t, n) { checkNoList(curList), glCtx.blendFunc(t, n) } export function Java_org_lwjgl_opengl_GL11_nglColorMask(e, t, n, a, r, o) { checkNoList(curList), glCtx.colorMask(t, n, a, r) } export function Java_org_lwjgl_opengl_GL11_nglCopyTexSubImage2D(e, t, n, a, r, o, i, l, s, c) { checkNoList(curList), glCtx.copyTexSubImage2D(t, n, a, r, o, i, l, s) } export function Java_org_lwjgl_opengl_GL11_nglScalef(e, t, n, a, r) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglScalef); var o = getCurMatrixTop(), i = glMatrix.mat4.create(); setCurMatrixTop(glMatrix.mat4.scale(i, o, glMatrix.vec3.fromValues(t, n, a))) } export function Java_org_lwjgl_opengl_GL11_nglCallLists(e, t, n, a, r) { checkNoList(curList), assert(n == glCtx.UNSIGNED_INT); var o = e.getJNIDataView(), i = new Int32Array(o.buffer, Number(a), t); for (var l = 0; l < t; l++)callList(i[l]) } export function Java_org_lwjgl_opengl_GL11_nglFlush() { checkNoList(curList), glCtx.flush() } export function Java_org_lwjgl_opengl_GL11_nglTexSubImage2D(e, t, n, a, r, o, i, l, s, c, u) { checkNoList(curList), assert(t == glCtx.TEXTURE_2D); var d = e.getJNIDataView(), f = null; if (Number(c)) { var h = 6407 == l ? 3 : 4, g = 5121 == s ? 1 : 1, v = Math.max(0, o * i * h * g); f = new Uint8Array(d.buffer, Number(c), v) } glCtx.texSubImage2D(t, n, a, r, o, i, l, s, f) } export function Java_org_lwjgl_opengl_GL11_nglGetFloatv(e, t, n, a) { checkNoList(curList); var r = e.getJNIDataView(), o = new Float32Array(r.buffer, Number(n), 16); if (2982 == t) { var i = modelViewMatrixStack[modelViewMatrixStack.length - 1]; for (var l = 0; l < 16; l++)o[l] = i[l] } else if (2983 == t) { var i = projMatrixStack[projMatrixStack.length - 1]; for (var l = 0; l < 16; l++)o[l] = i[l] } else verboseLog && console.log("glGetFloat " + t) } export function Java_org_lwjgl_opengl_GL11_nglFogfv() { checkNoList(curList), verboseLog && console.log("glFog") } export function Java_org_lwjgl_opengl_GL11_nglNormal3f() { checkNoList(curList), verboseLog && console.log("glNormal3f") } export function Java_org_lwjgl_opengl_GL11_nglFogi() { checkNoList(curList), verboseLog && console.log("glFogi") } export function Java_org_lwjgl_opengl_GL11_nglFogf() { checkNoList(curList), verboseLog && console.log("glFogf") } export function Java_org_lwjgl_opengl_GL11_nglColorMaterial() { checkNoList(curList), verboseLog && console.log("glColorMaterial") } export function Java_org_lwjgl_opengl_GL11_nglCallList(e, t, n) { checkNoList(curList), callList(t) } export function Java_org_lwjgl_opengl_GL13_nglActiveTexture() { checkNoList(curList), verboseLog && console.log("glActiveTexture") } export function Java_org_lwjgl_opengl_GL11_nglLightfv() { checkNoList(curList), verboseLog && console.log("glLightfv") } export function Java_org_lwjgl_opengl_GL11_nglLightModelfv() { checkNoList(curList), verboseLog && console.log("glLightModelfv") } export function Java_org_lwjgl_opengl_GL11_nglNormalPointer(e, t, n, a, r) { normalData.size = 3, normalData.type = t, normalData.stride = n, normalData.pointer = Number(a) } export function Java_org_lwjgl_opengl_GL13_nglMultiTexCoord2f() { checkNoList(curList), verboseLog && console.log("glMultiTexCoord2f") } export function Java_org_lwjgl_opengl_GL13_nglClientActiveTexture() { verboseLog && console.log("glClientActiveTexture") } export function Java_org_lwjgl_opengl_GL11_nglLineWidth() { checkNoList(curList), verboseLog && console.log("glLineWidth") } export function Java_org_lwjgl_opengl_GL11_nglPolygonOffset() { checkNoList(curList), verboseLog && console.log("glPolygonOffset") } export function Java_org_lwjgl_opengl_GL11_nglBegin(e, t, n) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglBegin); immediateModeData.mode = t, immediateModeData.vertexPos = 0, immediateModeData.texCoordPos = 0 } export function Java_org_lwjgl_opengl_GL11_nglTexCoord2f(e, t, n, a) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglTexCoord2f); var r = immediateModeData.texCoordPos; if (r + 1 >= immediateModeData.texCoordBuf.length) { var o = new Float32Array(immediateModeData.texCoordBuf.length << 1); o.set(immediateModeData.texCoordBuf), immediateModeData.texCoordBuf = o } immediateModeData.texCoordBuf[r] = t, immediateModeData.texCoordBuf[r + 1] = n, immediateModeData.texCoordPos = r + 2 } export function Java_org_lwjgl_opengl_GL11_nglVertex2f(e, t, n, a) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglVertex2f); var r = immediateModeData.vertexPos; if (r + 2 >= immediateModeData.vertexBuf.length) { var o = new Float32Array(immediateModeData.vertexBuf.length << 1); o.set(immediateModeData.vertexBuf), immediateModeData.vertexBuf = o } immediateModeData.vertexBuf[r] = t, immediateModeData.vertexBuf[r + 1] = n, immediateModeData.vertexBuf[r + 2] = 0, immediateModeData.vertexPos = r + 3 } export function Java_org_lwjgl_opengl_GL11_nglVertex3f(e, t, n, a, r) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglVertex3f); var o = immediateModeData.vertexPos; if (o + 2 >= immediateModeData.vertexBuf.length) { var i = new Float32Array(immediateModeData.vertexBuf.length << 1); i.set(immediateModeData.vertexBuf), immediateModeData.vertexBuf = i } immediateModeData.vertexBuf[o] = t, immediateModeData.vertexBuf[o + 1] = n, immediateModeData.vertexBuf[o + 2] = a, immediateModeData.vertexPos = o + 3 } export function Java_org_lwjgl_opengl_GL11_nglEnd(e, t) { if (curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglEnd); uploadDataImpl(immediateModeData.vertexBuf.subarray(0, immediateModeData.vertexPos), vertexBuffer, vertexPosition, 3, glCtx.FLOAT, 12), uploadDataImpl(immediateModeData.texCoordBuf.subarray(0, immediateModeData.texCoordPos), texCoordBuffer, texCoord, 2, glCtx.FLOAT, 8), drawArraysImpl(immediateModeData.mode, 0, immediateModeData.vertexPos / 3) } export function Java_org_lwjgl_openal_AL_nCreate() { }
export function Java_org_lwjgl_openal_AL_resetNativeStubs() { }
export function Java_org_lwjgl_openal_AL_nDestroy() { }

export function Java_org_lwjgl_openal_AL10_initNativeStubs() { }
export function Java_org_lwjgl_openal_AL10_nalEnable() { }
export function Java_org_lwjgl_openal_AL10_nalDisable() { }
export function Java_org_lwjgl_openal_AL10_nalIsEnabled() { return !1; }
export function Java_org_lwjgl_openal_AL10_nalGetBoolean() { return !1; }
export function Java_org_lwjgl_openal_AL10_nalGetInteger() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetFloat() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetDouble() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetIntegerv() { }
export function Java_org_lwjgl_openal_AL10_nalGetFloatv() { }
export function Java_org_lwjgl_openal_AL10_nalGetDoublev() { }
export function Java_org_lwjgl_openal_AL10_nalGetString() { return ''; }
export function Java_org_lwjgl_openal_AL10_nalGetError() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalIsExtensionPresent() { return !1; }
export function Java_org_lwjgl_openal_AL10_nalGetEnumValue() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalListeneri() { }
export function Java_org_lwjgl_openal_AL10_nalListenerf() { }
export function Java_org_lwjgl_openal_AL10_nalListenerfv() { }
export function Java_org_lwjgl_openal_AL10_nalListener3f() { }
export function Java_org_lwjgl_openal_AL10_nalGetListeneri() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetListenerf() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetListenerfv() { }
export function Java_org_lwjgl_openal_AL10_nalGenSources() { }
export function Java_org_lwjgl_openal_AL10_nalGenSources2() { return 1; }
export function Java_org_lwjgl_openal_AL10_nalDeleteSources() { }
export function Java_org_lwjgl_openal_AL10_nalDeleteSources2() { }
export function Java_org_lwjgl_openal_AL10_nalIsSource() { return !1; }
export function Java_org_lwjgl_openal_AL10_nalSourcei() { }
export function Java_org_lwjgl_openal_AL10_nalSourcef() { }
export function Java_org_lwjgl_openal_AL10_nalSourcefv() { }
export function Java_org_lwjgl_openal_AL10_nalSource3f() { }
export function Java_org_lwjgl_openal_AL10_nalGetSourcei() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetSourcef() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetSourcefv() { }
export function Java_org_lwjgl_openal_AL10_nalSourcePlayv() { }
export function Java_org_lwjgl_openal_AL10_nalSourcePausev() { }
export function Java_org_lwjgl_openal_AL10_nalSourceStopv() { }
export function Java_org_lwjgl_openal_AL10_nalSourceRewindv() { }
export function Java_org_lwjgl_openal_AL10_nalSourcePlay() { }
export function Java_org_lwjgl_openal_AL10_nalSourcePause() { }
export function Java_org_lwjgl_openal_AL10_nalSourceStop() { }
export function Java_org_lwjgl_openal_AL10_nalSourceRewind() { }
export function Java_org_lwjgl_openal_AL10_nalGenBuffers() { }
export function Java_org_lwjgl_openal_AL10_nalGenBuffers2() { return 1; }
export function Java_org_lwjgl_openal_AL10_nalDeleteBuffers() { }
export function Java_org_lwjgl_openal_AL10_nalDeleteBuffers2() { }
export function Java_org_lwjgl_openal_AL10_nalIsBuffer() { return !1; }
export function Java_org_lwjgl_openal_AL10_nalBufferData() { }
export function Java_org_lwjgl_openal_AL10_nalGetBufferi() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalGetBufferf() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalSourceQueueBuffers() { }
export function Java_org_lwjgl_openal_AL10_nalSourceQueueBuffers2() { }
export function Java_org_lwjgl_openal_AL10_nalSourceUnqueueBuffers() { }
export function Java_org_lwjgl_openal_AL10_nalSourceUnqueueBuffers2() { return 0; }
export function Java_org_lwjgl_openal_AL10_nalDistanceModel() { }
export function Java_org_lwjgl_openal_AL10_nalDopplerFactor() { }
export function Java_org_lwjgl_openal_AL10_nalDopplerVelocity() { }

export function Java_org_lwjgl_openal_ALC10_initNativeStubs() { }
export function Java_org_lwjgl_openal_ALC10_nalcOpenDevice() { return 1; }
export function Java_org_lwjgl_openal_ALC10_nalcCloseDevice() { return !0; }
export function Java_org_lwjgl_openal_ALC10_nalcCreateContext() { return 1; }
export function Java_org_lwjgl_openal_ALC10_nalcMakeContextCurrent() { return 1; }
export function Java_org_lwjgl_openal_ALC10_nalcProcessContext() { }
export function Java_org_lwjgl_openal_ALC10_nalcGetCurrentContext() { return 1; }
export function Java_org_lwjgl_openal_ALC10_nalcGetContextsDevice() { return 1; }
export function Java_org_lwjgl_openal_ALC10_nalcSuspendContext() { }
export function Java_org_lwjgl_openal_ALC10_nalcDestroyContext() { }
export function Java_org_lwjgl_openal_ALC10_nalcGetError() { return 0; }
export function Java_org_lwjgl_openal_ALC10_nalcGetIntegerv() { }
export function Java_org_lwjgl_openal_ALC10_nalcGetString() { return 0; }
export function Java_org_lwjgl_openal_ALC10_nalcIsExtensionPresent() { return !1; }
export function Java_org_lwjgl_openal_ALC10_nalcGetEnumValue() { return 0; } export async function Java_org_lwjgl_opengl_LinuxEvent_nNextEvent(e, t, n) { var a = Number(await n.address()), r = e.getJNIDataView(), o = eventQueue.shift(); if (!o) return void r.setInt32(a + 0, 0, !0); switch (o.type) { case "focus": r.setInt32(a + 0, 9, !0); break; case "mousedown": r.setInt32(a + 0, 4, !0), r.setInt32(a + 4, o.x, !0), r.setInt32(a + 8, o.y, !0), r.setInt32(a + 12, o.button, !0); break; case "mouseup": r.setInt32(a + 0, 5, !0), r.setInt32(a + 4, o.x, !0), r.setInt32(a + 8, o.y, !0), r.setInt32(a + 12, o.button, !0); break; case "mousemove": r.setInt32(a + 0, 6, !0), r.setInt32(a + 4, o.x, !0), r.setInt32(a + 8, o.y, !0); break; case "keydown": r.setInt32(a + 0, 2, !0), r.setInt32(a + 4, o.keyCode, !0); break; case "keyup": r.setInt32(a + 0, 3, !0), r.setInt32(a + 4, o.keyCode, !0); break; default: break } } export function Java_org_lwjgl_opengl_LinuxEvent_nGetWindow() { return 0 } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetType(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 0, !0) } export async function Java_org_lwjgl_Sys_alert(e, t, n) { t && t.toString && (t = t.toString()), n && n.toString && (n = n.toString()), console.warn("[LWJGL SYS ALERT] " + t + ": " + n), "undefined" != typeof window && window.alert && window.alert(t + "\n" + n) } export function Java_org_lwjgl_opengl_LinuxEvent_nFilterEvent() { } export function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonTime() { } export function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonRoot() { } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonXRoot(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 4, !0) } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonYRoot(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 8, !0) } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonX(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 4, !0) } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonY(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 8, !0) } export function Java_org_lwjgl_opengl_LinuxEvent_nGetFocusDetail() { } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonType(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 0, !0) } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonButton(e, t) { var n = Number(await t.address()); return e.getJNIDataView().getInt32(n + 12, !0) } export function Java_org_lwjgl_opengl_LinuxDisplay_nGrabPointer() { glCanvas.requestPointerLock(), lockedMousePos = { x: 0, y: 0 } } export function Java_org_lwjgl_opengl_LinuxDisplay_nUngrabPointer() { document.exitPointerLock(), lockedMousePos = null } export function Java_org_lwjgl_opengl_LinuxDisplay_nDefineCursor() { } export function Java_org_lwjgl_opengl_LinuxDisplay_getRootWindow() { } export function Java_org_lwjgl_opengl_LinuxDisplay_nSetWindowIcon() { } export function Java_org_lwjgl_opengl_LinuxMouse_nGetWindowWidth() { return 1280 } export function Java_org_lwjgl_opengl_LinuxMouse_nSendWarpEvent() { } export function Java_org_lwjgl_opengl_LinuxMouse_nWarpCursor() { } export function Java_org_lwjgl_opengl_LinuxEvent_nSetWindow() { } export function Java_org_lwjgl_opengl_LinuxEvent_nSendEvent() { } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyAddress(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 4, !0) } export function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyTime() { } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyType(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 0, !0) } export async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyKeyCode(e, t) { var n = Number(await t.address()), a = e.getJNIDataView(); return a.getInt32(n + 4, !0) } export function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyState() { } export function Java_org_lwjgl_opengl_LinuxKeyboard_lookupKeysym(e, t, n) { return Number(t) } export async function Java_org_lwjgl_opengl_LinuxKeyboard_lookupString(e, t, n) { var a = Number(await n.address()), r = e.getJNIDataView(); return r.setInt8(a, Number(t)), 1 } export function Java_org_lwjgl_opengl_Display_create(e, t, n) { console.log("[Mock] Java_org_lwjgl_opengl_Display_create called"), initGLShaders() }// Registration with Wrapped Functions
if (typeof window !== "undefined") {
    window.CheerpJ_LWJGL_Natives = window.CheerpJ_LWJGL_Natives || {};
    let registeredCount = 0;
    // Iterate over global scope to find natives
    for (const key of Object.getOwnPropertyNames(window)) {
        if (
            key.startsWith('Java_org_lwjgl_') ||
            key.startsWith('Java_java_lang_') ||
            key.startsWith('JNI_OnLoad_') ||
            key.startsWith('JVM_') ||
            key.startsWith('_JVM_')
        ) {
            const fn = window[key];
            if (typeof fn === 'function') {
                // Apply wrapper
                const wrapped = __lwjglWrap(key, fn);
                window.CheerpJ_LWJGL_Natives[key] = wrapped;
                try { window[key] = wrapped; } catch (e) { }
                registeredCount++;
            }
        }
    }
    console.log(`[LWJGL] Registered natives count = ${registeredCount}`);
}

