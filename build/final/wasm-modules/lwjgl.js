// Load the glMatrix library from the same origin to satisfy the page CSP.
const { default: glMatrix } = await import("./gl-matrix-loader.js");

const glCanvas = window.lwjglCanvasElement;
if (!(glCanvas instanceof HTMLCanvasElement)) throw new Error("window.lwjglCanvasElement is not set or is not a canvas");
const glCtx = glCanvas.getContext("webgl2", {antialias: false, alpha: false});
const defaultWindowWidth = 1000;
const defaultWindowHeight = 500;

function getCanvasWidth()
{
	return glCanvas.width || glCanvas.clientWidth || defaultWindowWidth;
}

function getCanvasHeight()
{
	return glCanvas.height || glCanvas.clientHeight || defaultWindowHeight;
}

function warnOnce(cache, key, message)
{
	if(cache.has(key))
		return;
	cache.add(key);
	console.warn(message);
}

var texImageWarnings = new Set();

function getTexelComponentCount(format)
{
	if(format == glCtx.RED || format == 0x1903/*GL_RED*/ || format == 0x1906/*GL_ALPHA*/ || format == 0x1909/*GL_LUMINANCE*/)
		return 1;
	if(format == glCtx.RG || format == 0x8227/*GL_RG*/ || format == 0x190A/*GL_LUMINANCE_ALPHA*/)
		return 2;
	if(format == glCtx.RGB || format == 0x1907/*GL_RGB*/)
		return 3;
	return 4;
}

function buildTextureUploadArray(v, memPtr, width, height, format, type)
{
	if(!memPtr)
		return null;
	var pixelCount = Math.max(0, width * height);
	var comps = getTexelComponentCount(format);
	var ctor = Uint8Array;
	var elementCount = pixelCount * comps;
	if(type == glCtx.UNSIGNED_SHORT || type == glCtx.SHORT)
	{
		ctor = Uint16Array;
	}
	else if(
		type == glCtx.UNSIGNED_SHORT_5_6_5 ||
		type == glCtx.UNSIGNED_SHORT_4_4_4_4 ||
		type == glCtx.UNSIGNED_SHORT_5_5_5_1
	)
	{
		ctor = Uint16Array;
		elementCount = pixelCount;
	}
	else if(type == glCtx.FLOAT)
	{
		ctor = Float32Array;
	}
	else if(type == glCtx.UNSIGNED_INT)
	{
		ctor = Uint32Array;
	}
	try
	{
		return new ctor(v.buffer, Number(memPtr), elementCount);
	}
	catch(err)
	{
		warnOnce(
			texImageWarnings,
			"upload-array-" + type + "-" + format,
			"LWJGL texImage upload array fallback type=" + type + " format=" + format + " ctor=" + ctor.name + " err=" + String(err)
		);
		return new Uint8Array(v.buffer, Number(memPtr));
	}
}

function isLegacyWebGl1TextureFormat(format)
{
	return format == 0x1906/*GL_ALPHA*/ || format == 0x1909/*GL_LUMINANCE*/ || format == 0x190A/*GL_LUMINANCE_ALPHA*/;
}

function expandLegacyTextureDataToRgba(data, width, height, format)
{
	if(data == null)
		return null;
	var pixelCount = Math.max(0, width * height);
	var out = new Uint8Array(pixelCount * 4);
	for(var i=0;i<pixelCount;i++)
	{
		if(format == 0x1906/*GL_ALPHA*/)
		{
			out[i * 4 + 0] = 255;
			out[i * 4 + 1] = 255;
			out[i * 4 + 2] = 255;
			out[i * 4 + 3] = data[i];
		}
		else if(format == 0x1909/*GL_LUMINANCE*/)
		{
			var lum = data[i];
			out[i * 4 + 0] = lum;
			out[i * 4 + 1] = lum;
			out[i * 4 + 2] = lum;
			out[i * 4 + 3] = 255;
		}
		else
		{
			var base = i * 2;
			var l = data[base];
			out[i * 4 + 0] = l;
			out[i * 4 + 1] = l;
			out[i * 4 + 2] = l;
			out[i * 4 + 3] = data[base + 1];
		}
	}
	return out;
}

function expandRgbTextureDataToRgba(data, width, height)
{
	if(data == null)
		return null;
	var pixelCount = Math.max(0, width * height);
	var out = new Uint8Array(pixelCount * 4);
	for(var i=0;i<pixelCount;i++)
	{
		var srcBase = i * 3;
		var dstBase = i * 4;
		out[dstBase + 0] = data[srcBase + 0];
		out[dstBase + 1] = data[srcBase + 1];
		out[dstBase + 2] = data[srcBase + 2];
		out[dstBase + 3] = 255;
	}
	return out;
}

function normalizeTextureUpload(v, memPtr, width, height, internalFormat, format, type)
{
	var result =
	{
		internalFormat: internalFormat,
		format: format,
		type: type,
		data: buildTextureUploadArray(v, memPtr, width, height, format, type)
	};
	if(isLegacyWebGl1TextureFormat(format) && type == glCtx.UNSIGNED_BYTE)
	{
		result.data = expandLegacyTextureDataToRgba(result.data, width, height, format);
		result.internalFormat = glCtx.RGBA;
		result.format = glCtx.RGBA;
		result.type = glCtx.UNSIGNED_BYTE;
		warnOnce(
			texImageWarnings,
			"legacy-format-" + format,
			"LWJGL texImage2D remapping legacy format=" + format + " to RGBA upload for WebGL2 compatibility."
		);
	}
	else if(internalFormat == glCtx.RGBA && format == glCtx.RGB && type == glCtx.UNSIGNED_BYTE)
	{
		result.data = expandRgbTextureDataToRgba(result.data, width, height);
		result.format = glCtx.RGBA;
		result.type = glCtx.UNSIGNED_BYTE;
		warnOnce(
			texImageWarnings,
			"rgba-rgb-upload-" + width + "x" + height,
			"LWJGL texImage2D expanding RGB data to RGBA to satisfy RGBA internalFormat uploads."
		);
	}
	return result;
}

var vertexShaderSrc = `
	attribute vec4 aVertexPosition;
	attribute vec4 aColor;
	attribute vec2 aTexCoord;
	uniform mat4 modelView;
	uniform mat4 projection;
	varying vec2 vTexCoord;
	varying vec4 vColor;
	void main() {
		gl_Position = projection * modelView * aVertexPosition;
		vTexCoord = aTexCoord;
		vColor = aColor;
	}
`;
// NOTE: Only the default GL_MODULATE texEnv is supported here
var fragmentShaderSrc = `
	precision mediump float;
	uniform float uTextureMask;
	uniform sampler2D uSampler;
	varying vec2 vTexCoord;
	varying vec4 vColor;
	void main() {
		vec4 texSample = texture2D(uSampler, vTexCoord);
		gl_FragColor = mix(vColor, texSample * vColor, uTextureMask);
	}
`;
var vertexShader = glCtx.createShader(glCtx.VERTEX_SHADER);
glCtx.shaderSource(vertexShader, vertexShaderSrc);
glCtx.compileShader(vertexShader);
var fragmentShader = glCtx.createShader(glCtx.FRAGMENT_SHADER);
glCtx.shaderSource(fragmentShader, fragmentShaderSrc);
glCtx.compileShader(fragmentShader);
glCtx.pixelStorei(glCtx.UNPACK_ALIGNMENT, 1);
var program = glCtx.createProgram();
glCtx.attachShader(program, vertexShader);
glCtx.attachShader(program, fragmentShader);
glCtx.linkProgram(program);
glCtx.useProgram(program);
var vertexBuffer = glCtx.createBuffer();
var colorBuffer = glCtx.createBuffer();
var texCoordBuffer = glCtx.createBuffer();
var vertexPosition = glCtx.getAttribLocation(program, "aVertexPosition");
var colorLocation = glCtx.getAttribLocation(program, "aColor");
var texCoord = glCtx.getAttribLocation(program, "aTexCoord");
var mvLocation = glCtx.getUniformLocation(program, "modelView");
var projLocation = glCtx.getUniformLocation(program, "projection");
var samplerLocation = glCtx.getUniformLocation(program, "uSampler");
var samplerLocation2 = glCtx.getUniformLocation(program, "uSampler2");
var texMaskLocation = glCtx.getUniformLocation(program, "uTextureMask");
var vertexData =
{
	enabled: false,
	size: 0,
	type: 0,
	stride: 0,
	pointer: 0,
	buf: null
};
var normalData =
{
	enabled: false,
	size: 0,
	type: 0,
	stride: 0,
	pointer: 0,
	buf: null
};
var colorData =
{
	enabled: false,
	size: 0,
	type: 0,
	stride: 0,
	pointer: 0,
	buf: null
};
var texCoordData =
{
	enabled: false,
	size: 0,
	type: 0,
	stride: 0,
	pointer: 0,
	buf: null
};
var immediateModeData =
{
	mode: 0,
	vertexBuf: new Float32Array(32),
	vertexPos: 0,
	colorBuf: new Float32Array(32),
	colorPos: 0,
	currentColor: [1, 1, 1, 1],
	texCoordBuf: new Float32Array(32),
	texCoordPos: 0
};
var verboseLog = false;
var frameCount = 0;
var presentationStats = {
	swapCount: 0,
	samples: [],
	lastFramebufferStatus: null,
	lastViewport: null
};
if(typeof window !== "undefined")
	window.__lwjglPresentationStats = presentationStats;
// Set to a non-zero value to stop after a certain number of frames
var frameLimit = 0;
var unsupportedDrawModes = new Set();
var unsupportedMatrixModes = new Set();
var unsupportedEventTypes = new Set();
var fbWidth = 0;
var fbHeight = 0;
var fbTexture = null;
var mainFb = null;
var depthRb = null;
// NOTE: These initializes to identity
var projMatrixStack = [glMatrix.mat4.create()];
var modelViewMatrixStack = [glMatrix.mat4.create()];
var textureMatrixStack = [glMatrix.mat4.create()];
var curMatrixStack = modelViewMatrixStack;
function getCurMatrixTop()
{
	return curMatrixStack[curMatrixStack.length - 1];
}
function setCurMatrixTop(m)
{
	curMatrixStack[curMatrixStack.length - 1] = m;
}

function ensureFramebufferSize()
{
	if(!fbTexture || !mainFb || !depthRb)
		return;
	var nextWidth = getCanvasWidth();
	var nextHeight = getCanvasHeight();
	if(nextWidth == fbWidth && nextHeight == fbHeight)
		return;
	fbWidth = nextWidth;
	fbHeight = nextHeight;
	glCtx.bindTexture(glCtx.TEXTURE_2D, fbTexture);
	glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_MIN_FILTER, glCtx.NEAREST);
	glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_MAG_FILTER, glCtx.NEAREST);
	glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_WRAP_S, glCtx.CLAMP_TO_EDGE);
	glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_WRAP_T, glCtx.CLAMP_TO_EDGE);
	glCtx.texImage2D(glCtx.TEXTURE_2D, 0, glCtx.RGBA, fbWidth, fbHeight, 0, glCtx.RGBA, glCtx.UNSIGNED_BYTE, null);
	glCtx.bindTexture(glCtx.TEXTURE_2D, null);
	glCtx.bindRenderbuffer(glCtx.RENDERBUFFER, depthRb);
	glCtx.renderbufferStorage(glCtx.RENDERBUFFER, glCtx.DEPTH_COMPONENT16, fbWidth, fbHeight);
	glCtx.bindRenderbuffer(glCtx.RENDERBUFFER, null);
	glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);
	glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb);
}
function uploadDataImpl(buf, buffer, attributeLocation, size, type, stride)
{
	glCtx.bindBuffer(glCtx.ARRAY_BUFFER, buffer);
	glCtx.bufferData(glCtx.ARRAY_BUFFER, buf, glCtx.STATIC_DRAW);
	glCtx.vertexAttribPointer(attributeLocation, size, type, type != glCtx.FLOAT, stride, 0);
	glCtx.enableVertexAttribArray(attributeLocation);
}
function applyCurrentColorAttrib()
{
	glCtx.disableVertexAttribArray(colorLocation);
	glCtx.vertexAttrib4f(colorLocation,
		immediateModeData.currentColor[0],
		immediateModeData.currentColor[1],
		immediateModeData.currentColor[2],
		immediateModeData.currentColor[3]);
}
function uploadData(v, data, buffer, attributeLocation, count)
{
	if(data.enabled)
	{
		assert(data.stride);
		var buf = data.buf;
		if(buf == null)
		{
			assert(v && data.pointer);
			buf = new Uint8Array(v.buffer, data.pointer, data.stride * count);
		}
		uploadDataImpl(buf, buffer, attributeLocation, data.size, data.type, data.stride);
	}
	else
	{
		if(attributeLocation == colorLocation)
		{
			applyCurrentColorAttrib();
		}
		else
		{
			glCtx.disableVertexAttribArray(attributeLocation);
			if(attributeLocation == texCoord)
				glCtx.vertexAttrib2f(texCoord, 0, 0);
		}
	}
}
function captureData(v, data, count)
{
	var ret = { enabled: data.enabled, size: data.size, type: data.type, stride: data.stride, pointer: 0, buf: null };
	if(data.enabled)
	{
		assert(data.stride);
		var buf = new Uint8Array(v.buffer, data.pointer, data.stride * count);
		// Capture the current data
		ret.buf = new Uint8Array(buf);
	}
	return ret;
}
function ensureImmediateArrayCapacity(buf, neededLength)
{
	if(neededLength <= buf.length)
		return buf;
	var nextLength = buf.length > 0 ? buf.length : 32;
	while(nextLength < neededLength)
		nextLength *= 2;
	var nextBuf = new Float32Array(nextLength);
	nextBuf.set(buf);
	return nextBuf;
}
function checkNoList(list)
{
	if(list != null)
		throw new Error("Unsupported command in list");
}
function pushInList(list, args, callee)
{
	// Not an elegant solution, but it works
	// It would be nicer to extract the actual implementation from native interfaces
	// to avoid bringing around the library object
	list.push({f: callee, a: Array.from(args)});
}
function callList(listId)
{
	var l = cmdLists[listId];
	if(l == null)
		return;
	for(var i=0;i<l.length;i++)
	{
		var c = l[i];
		c.f.apply(null, c.a);
	}
}
function drawArraysImpl(mode, first, count)
{
	// TODO: Conditional
	glCtx.uniformMatrix4fv(mvLocation, false, modelViewMatrixStack[modelViewMatrixStack.length - 1]);
	glCtx.uniformMatrix4fv(projLocation, false, projMatrixStack[projMatrixStack.length - 1]);
	assert(first == 0);
	// We can render each quad a separate GL_TRIANGLE_FAN
	if(mode == 7/*QUADS*/ && (count % 4) == 0)
	{
		for(var i=0;i<count;i+=4)
			glCtx.drawArrays(glCtx.TRIANGLE_FAN, i, 4);
	}
	else if(mode == 8/*QUAD_STRIP*/)
	{
		glCtx.drawArrays(glCtx.TRIANGLE_STRIP, first, count);
	}
	else if(mode == 9/*POLYGON*/)
	{
		glCtx.drawArrays(glCtx.TRIANGLE_FAN, first, count);
	}
	else if(
		mode == glCtx.POINTS ||
		mode == glCtx.LINES ||
		mode == glCtx.LINE_LOOP ||
		mode == glCtx.LINE_STRIP ||
		mode == glCtx.TRIANGLES ||
		mode == glCtx.TRIANGLE_STRIP ||
		mode == glCtx.TRIANGLE_FAN
	)
	{
		glCtx.drawArrays(mode, first, count);
	}
	else
	{
		warnOnce(unsupportedDrawModes, mode, "Unsupported glDrawArrays mode=" + mode + " first=" + first + " count=" + count);
	}
}
function pushDrawArraysInList(list, v, mode, first, count)
{
	var args = [mode, first, count, captureData(v, vertexData, count), captureData(v, colorData, count), captureData(v, texCoordData, count)];
	list.push({f: drawArraysInList, a: args});
}
function drawArraysInList(mode, first, count, capturedVertexData, capturedColorData, capturedTexCoordData)
{
	// Upload vertex data
	uploadData(null, capturedVertexData, vertexBuffer, vertexPosition, count);
	// Upload color data
	uploadData(null, capturedColorData, colorBuffer, colorLocation, count);
	// Upload tex coord data
	uploadData(null, capturedTexCoordData, texCoordBuffer, texCoord, count);
	drawArraysImpl(mode, first, count);
}
// Fix the sampler to texture unit 0
glCtx.uniform1i(samplerLocation, 0);
glCtx.uniform1f(texMaskLocation, 0);
var curList = null;
var listBase = 0;
var cmdLists = [null];
// The first null implicitly solves resetting on 0 id
var textureObjects = [null];
// We need to use an FBO as the main target to support copyTexSubImage2D that seems broken otherwise
fbTexture = glCtx.createTexture();
glCtx.bindTexture(glCtx.TEXTURE_2D, fbTexture);
glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_MIN_FILTER, glCtx.NEAREST);
glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_MAG_FILTER, glCtx.NEAREST);
glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_WRAP_S, glCtx.CLAMP_TO_EDGE);
glCtx.texParameteri(glCtx.TEXTURE_2D, glCtx.TEXTURE_WRAP_T, glCtx.CLAMP_TO_EDGE);
glCtx.texImage2D(glCtx.TEXTURE_2D, 0, glCtx.RGBA, getCanvasWidth(), getCanvasHeight(), 0, glCtx.RGBA, glCtx.UNSIGNED_BYTE, null);
glCtx.bindTexture(glCtx.TEXTURE_2D, null);
mainFb = glCtx.createFramebuffer();
glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);
glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb);
glCtx.framebufferTexture2D(glCtx.FRAMEBUFFER, glCtx.COLOR_ATTACHMENT0, glCtx.TEXTURE_2D, fbTexture, 0);
// Add a depth render buffer
depthRb = glCtx.createRenderbuffer();
glCtx.bindRenderbuffer(glCtx.RENDERBUFFER, depthRb);
glCtx.renderbufferStorage(glCtx.RENDERBUFFER, glCtx.DEPTH_COMPONENT16, getCanvasWidth(), getCanvasHeight());
glCtx.framebufferRenderbuffer(glCtx.FRAMEBUFFER, glCtx.DEPTH_ATTACHMENT, glCtx.RENDERBUFFER, depthRb);
ensureFramebufferSize();
// Synthetize a focus event, it's needed for LWJGL logic
var eventQueue = [{type:"focus"}];

function convertMousePos(x, y) {
	const fbWidthNow = getCanvasWidth();
	const fbHeightNow = getCanvasHeight();
	const offsetX = glCanvas.width - fbWidthNow;
	const offsetY = glCanvas.height - fbHeightNow;
	const clientWidth = Math.max(1, glCanvas.clientWidth || fbWidthNow);
	const clientHeight = Math.max(1, glCanvas.clientHeight || fbHeightNow);
	const xRatio = glCanvas.width / clientWidth;
	const yRatio = glCanvas.height / clientHeight;

	return [x * xRatio - offsetX, y * yRatio - offsetY];
}

/** Convert from MouseEvent.button to X11 mouse button */
function convertMouseButton(button) {
	return button + 1;
}

/**
 * If null, the game does not want the mouse pointer locked.
 * @type {{ x: number, y: number } | null}
 */
let lockedMousePos = null;

glCanvas.addEventListener("mousemove", evt => {
	let [x, y] = convertMousePos(evt.offsetX, evt.offsetY);

	// If the pointer is locked, we can't use offsetX/offsetY
	if (lockedMousePos) {
		x = lockedMousePos.x += evt.movementX;
		y = lockedMousePos.y += evt.movementY;

		if (!document.pointerLockElement) {
			// Game still wants the pointer locked, but it's not
			Java_org_lwjgl_opengl_LinuxDisplay_nGrabPointer();
		}
	}

	if (eventQueue[0]?.type == evt.type) {
		// Update unhandled event
		eventQueue[0].x = x;
		eventQueue[0].y = y;
	} else {
		eventQueue.push({ type: evt.type, x, y });
	}
});
function mouseHandler(evt) {
	const [x, y] = convertMousePos(evt.offsetX, evt.offsetY);
	eventQueue.push({ type: evt.type, x, y, button: convertMouseButton(evt.button) });
}
glCanvas.addEventListener("mousedown", mouseHandler);
glCanvas.addEventListener("mouseup", mouseHandler);
glCanvas.addEventListener("contextmenu", evt => evt.preventDefault());

/** @param {KeyboardEvent} e */
function keyHandler(e)
{
	// Convert to LinuxKeycodes.java keycodes
	// https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/opengl/LinuxKeycodes.java
	let keyCode = e.keyCode || e.key.charCodeAt(0); // most map to ASCII
	switch (e.key) {
		case "Escape": // note will have to press twice if pointer is locked
			keyCode = 0xff1b;
			break;
		case "Shift":
			keyCode = 0xffe1;
			break;
		case "Control":
			keyCode = 0xffe3;
			break;
		case "Meta":
			keyCode = 0xffe7;
			break;
		case "Alt":
			keyCode = 0xffe9;
			break;
	}
	console.log(e.key, keyCode);

	eventQueue.push({ type: e.type, keyCode });
	e.preventDefault();
}
glCanvas.addEventListener("keydown", keyHandler);
glCanvas.addEventListener("keyup", keyHandler);
function Java_org_lwjgl_DefaultSysImplementation_getPointerSize()
{
	return 4;
}

function Java_org_lwjgl_DefaultSysImplementation_getJNIVersion()
{
	return 19;
}

function Java_org_lwjgl_DefaultSysImplementation_setDebug()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nLockAWT()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nUnlockAWT()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_setErrorHandler()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_openDisplay(lib)
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nInternAtom()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nIsXrandrSupported()
{
	return 0;
}

function Java_org_lwjgl_opengl_LinuxDisplay_nIsXF86VidModeSupported()
{
	return 1;
}

function Java_org_lwjgl_opengl_LinuxDisplay_nGetDefaultScreen()
{
	return 0;
}

async function Java_org_lwjgl_opengl_LinuxDisplay_nGetAvailableDisplayModes(lib)
{
	var DisplayMode = await lib.org.lwjgl.opengl.DisplayMode;
	var d = await new DisplayMode(getCanvasWidth(), getCanvasHeight());
	return [d];
}

function Java_org_lwjgl_opengl_LinuxDisplay_nGetCurrentGammaRamp()
{
}

function Java_org_lwjgl_opengl_LinuxPeerInfo_createHandle()
{
}

function Java_org_lwjgl_opengl_GLContext_nLoadOpenGLLibrary()
{
}

function Java_org_lwjgl_opengl_LinuxDisplayPeerInfo_initDefaultPeerInfo()
{
}

function Java_org_lwjgl_opengl_LinuxDisplayPeerInfo_initDrawable()
{
}

function Java_org_lwjgl_opengl_AWTSurfaceLock_createHandle()
{
}

function Java_org_lwjgl_opengl_AWTSurfaceLock_lockAndInitHandle()
{
	return 1;
}

function Java_org_lwjgl_opengl_LinuxAWTGLCanvasPeerInfo_getScreenFromSurfaceInfo()
{
}

function Java_org_lwjgl_opengl_LinuxAWTGLCanvasPeerInfo_nInitHandle()
{
}

function Java_org_lwjgl_opengl_AWTSurfaceLock_nUnlock()
{
}

function Java_org_lwjgl_opengl_LinuxPeerInfo_nGetDrawable()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nCreateWindow()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_mapRaised()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nCreateBlankCursor()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nSetTitle()
{
}

function Java_org_lwjgl_opengl_LinuxMouse_nGetButtonCount()
{
	return 3;
}

function Java_org_lwjgl_opengl_LinuxMouse_nQueryPointer()
{
}

function Java_org_lwjgl_opengl_LinuxMouse_nGetWindowHeight()
{
	return getCanvasHeight();
}

function Java_org_lwjgl_opengl_LinuxKeyboard_getModifierMapping()
{
}

function Java_org_lwjgl_opengl_LinuxKeyboard_nSetDetectableKeyRepeat()
{
}

function Java_org_lwjgl_opengl_LinuxKeyboard_openIM()
{
}

function Java_org_lwjgl_opengl_LinuxKeyboard_allocateComposeStatus()
{
}

function Java_org_lwjgl_opengl_LinuxContextImplementation_nCreate()
{
}

function Java_org_lwjgl_opengl_LinuxContextImplementation_nMakeCurrent()
{
}

function Java_org_lwjgl_opengl_LinuxContextImplementation_nIsCurrent()
{
	return true;
}

function Java_org_lwjgl_opengl_GLContext_ngetFunctionAddress(lib, stringPtr)
{
	// Return any non-zero address, methods are called by name anyway
	return 1;
}

function Java_org_lwjgl_opengl_GL11_nglGetString(lib, id, funcPtr)
{
	checkNoList(curList);
	// Special case GL_EXTENSION for now
	if(id == 0x1F03)
	{
		// TODO: Do we need any?
		return "";
	}
	else
	{
		return glCtx.getParameter(id);
	}
}

function Java_org_lwjgl_opengl_GL11_nglGetIntegerv(lib, id, memPtr, funcPtr)
{
	checkNoList(curList);
	var v = lib.getJNIDataView();
	var buf = new Int32Array(v.buffer, Number(memPtr), 4);
	if(id == /*GL_VIEWPORT*/0xba2)
	{
		ensureFramebufferSize();
		buf[0] = 0;
		buf[1] = 0;
		buf[2] = fbWidth;
		buf[3] = fbHeight;
	}
	else if(verboseLog)
	{
		console.log("glGetInteger", id);
	}
}

function Java_org_lwjgl_opengl_GL11_nglGetError()
{
	checkNoList(curList);
	// We like living dangerously
	return 0;
}

function Java_org_lwjgl_opengl_LinuxContextImplementation_nSetSwapInterval()
{
}

function Java_org_lwjgl_opengl_GL11_nglClearColor(lib, r, g, b, a, funcPtr)
{
	checkNoList(curList);
	return glCtx.clearColor(r, g, b, a);
}

function Java_org_lwjgl_opengl_GL11_nglClear(lib, a, funcPtr)
{
	checkNoList(curList);
	glCtx.clear(a);
}

function Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers()
{
	if(verboseLog)
		console.warn("SwapBuffer");
	ensureFramebufferSize();
	glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);
	presentationStats.swapCount++;
	if(presentationStats.samples.length < 8 && (presentationStats.swapCount == 1 || (presentationStats.swapCount % 300) == 0))
	{
		try
		{
			var sampleWidth = Math.max(1, Math.min(fbWidth, 256));
			var sampleHeight = Math.max(1, Math.min(fbHeight, 192));
			var sampleX = Math.max(0, Math.floor((fbWidth - sampleWidth) / 2));
			var sampleY = Math.max(0, Math.floor((fbHeight - sampleHeight) / 2));
			var sample = new Uint8Array(sampleWidth * sampleHeight * 4);
			glCtx.readPixels(sampleX, sampleY, sampleWidth, sampleHeight, glCtx.RGBA, glCtx.UNSIGNED_BYTE, sample);
			var nonBlack = 0;
			var sum = 0;
			for(var i=0;i<sample.length;i+=4)
			{
				var r = sample[i], g = sample[i + 1], b = sample[i + 2];
				if(r > 8 || g > 8 || b > 8) nonBlack++;
				sum += r + g + b;
			}
			presentationStats.lastFramebufferStatus = glCtx.checkFramebufferStatus(glCtx.READ_FRAMEBUFFER);
			presentationStats.lastViewport = Array.from(glCtx.getParameter(glCtx.VIEWPORT));
			presentationStats.samples.push({
				swap: presentationStats.swapCount,
				fbWidth,
				fbHeight,
				nonBlack,
				pixels: sampleWidth * sampleHeight,
				meanRgb: sum / Math.max(1, sampleWidth * sampleHeight * 3),
				framebufferStatus: presentationStats.lastFramebufferStatus,
				viewport: presentationStats.lastViewport
			});
		}
		catch(err)
		{
			presentationStats.samples.push({swap: presentationStats.swapCount, error: String(err)});
		}
	}
	glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, null);
	glCtx.blitFramebuffer(0, 0, fbWidth, fbHeight, 0, 0, fbWidth, fbHeight, glCtx.COLOR_BUFFER_BIT, glCtx.NEAREST);
	glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);
	glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb);
	frameCount++;
	if(frameLimit && frameCount >= frameLimit)
	{
		console.warn("Frame limit reached");
		return;
	}
	// CheerpJ custom JNI calls must not keep the Java VM suspended on a browser
	// animation-frame Promise. The framebuffer has already been blitted above.
	return;
}

function Java_org_lwjgl_opengl_LinuxEvent_getPending()
{
	return eventQueue.length;
}

function Java_org_lwjgl_opengl_GL11_nglMatrixMode(lib, matrixMode, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglMatrixMode);
	if(matrixMode == 0x1700/*GL_MODELVIEW*/)
		curMatrixStack = modelViewMatrixStack;
	else if(matrixMode == 0x1701/*GL_PROJECTION*/)
		curMatrixStack = projMatrixStack;
	else if(matrixMode == 0x1702/*GL_TEXTURE*/)
		curMatrixStack = textureMatrixStack;
	else
		warnOnce(unsupportedMatrixModes, matrixMode, "Unsupported glMatrixMode value=" + matrixMode);
}

function Java_org_lwjgl_opengl_GL11_nglLoadIdentity(lib, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglLoadIdentity);
	glMatrix.mat4.identity(getCurMatrixTop());
}

function Java_org_lwjgl_opengl_GL11_nglOrtho(lib, left, right, bottom, top, nearVal, farVal, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglOrtho);
	var m = getCurMatrixTop();
	var o = glMatrix.mat4.create();
	glMatrix.mat4.ortho(o, left, right, bottom, top, nearVal, farVal);
	var out = glMatrix.mat4.create();
	setCurMatrixTop(glMatrix.mat4.multiply(out, m, o));
}

function Java_org_lwjgl_opengl_GL11_nglTranslatef(lib, x, y, z, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglTranslatef);
	var m = getCurMatrixTop();
	var out = glMatrix.mat4.create();
	setCurMatrixTop(glMatrix.mat4.translate(out, m, glMatrix.vec3.fromValues(x, y, z)));
}

function Java_org_lwjgl_opengl_GL11_nglViewport(lib, x, y, width, height, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglViewport);
	glCtx.viewport(x, y, width, height);
}

function Java_org_lwjgl_opengl_GL11_nglDisable(lib, a, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglDisable);
	if(a == glCtx.BLEND || a == glCtx.CULL_FACE || a == glCtx.DEPTH_TEST || a == glCtx.SCISSOR_TEST || a == glCtx.STENCIL_TEST)
		glCtx.disable(a);
	else if(a == glCtx.TEXTURE_2D || a == 0x806F/*GL_TEXTURE_3D*/)
	{
		glCtx.uniform1f(texMaskLocation, 0);
	}
	else if(verboseLog)
		console.log("glDisable " + a.toString(16));
}

function Java_org_lwjgl_opengl_GL11_nglEnable(lib, a, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglEnable);
	if(a == glCtx.BLEND || a == glCtx.CULL_FACE || a == glCtx.DEPTH_TEST || a == glCtx.SCISSOR_TEST || a == glCtx.STENCIL_TEST)
		glCtx.enable(a);
	else if(a == glCtx.TEXTURE_2D || a == 0x806F/*GL_TEXTURE_3D*/)
	{
	glCtx.uniform1f(texMaskLocation, 1);
	}
	else if(verboseLog)
		console.log("glEnable " + a.toString(16));
}

function Java_org_lwjgl_opengl_GL11_nglGenTextures(lib, n, memPtr, funcPtr)
{
	checkNoList(curList);
	var v = lib.getJNIDataView();
	var buf = new Int32Array(v.buffer, Number(memPtr), n);
	for(var i=0;i<n;i++)
	{
		var id = textureObjects.length;
		buf[i] = id;
		textureObjects[id] = glCtx.createTexture();
	}
}

function Java_org_lwjgl_opengl_GL11_nglBindTexture(lib, target, id, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglBindTexture);
	assert(target == glCtx.TEXTURE_2D);
	glCtx.bindTexture(target, textureObjects[id]);
}

function Java_org_lwjgl_opengl_GL11_nglTexParameteri(lib, target, pname, param, funcPtr)
{
	checkNoList(curList);
	glCtx.texParameteri(target, pname, param);
}

function Java_org_lwjgl_opengl_GL11_nglTexImage2D(lib, target, level, internalFormat, width, height, border, format, type, memPtr, funcPtr)
{
	checkNoList(curList);
	assert(target == glCtx.TEXTURE_2D);
	var v = lib.getJNIDataView();
	var upload = normalizeTextureUpload(v, memPtr, width, height, internalFormat, format, type);
	glCtx.texImage2D(target, level, upload.internalFormat, width, height, border, upload.format, upload.type, upload.data);
	var texImageErr = glCtx.getError();
	if(texImageErr != glCtx.NO_ERROR)
	{
		warnOnce(
			texImageWarnings,
			"texImage-error-" + upload.internalFormat + "-" + upload.format + "-" + upload.type,
			"LWJGL texImage2D error=" + texImageErr + " ifmt=" + upload.internalFormat + " fmt=" + upload.format + " type=" + upload.type + " size=" + width + "x" + height + " ptr=" + memPtr + " dataCtor=" + (upload.data && upload.data.constructor ? upload.data.constructor.name : "null")
		);
	}
}

function Java_org_lwjgl_opengl_GL11_nglTexCoordPointer(lib, size, type, stride, memPtr, funcPtr)
{
	texCoordData.size = size;
	texCoordData.type = type;
	texCoordData.stride = stride;
	texCoordData.pointer = Number(memPtr);
}

function Java_org_lwjgl_opengl_GL11_nglEnableClientState(lib, v, funcPtr)
{
	if(v == 0x8074/*GL_VERTEX_ARRAY*/)
	{
		vertexData.enabled = true;
	}
	else if(v == 0x8075/*GL_NORMAL_ARRAY*/)
	{
		normalData.enabled = true;
	}
	else if(v == 0x8076/*GL_COLOR_ARRAY*/)
	{
		colorData.enabled = true;
	}
	else if(v == 0x8078/*GL_TEXTURE_COORD_ARRAY*/)
	{
		texCoordData.enabled = true;
	}
	else if(verboseLog)
	{
		console.log("glEnableClientState");
	}
}

function Java_org_lwjgl_opengl_GL11_nglColorPointer(lib, size, type, stride, memPtr, funcPtr)
{
	colorData.size = size;
	colorData.type = type;
	colorData.stride = stride;
	colorData.pointer = Number(memPtr);
}

function Java_org_lwjgl_opengl_GL11_nglVertexPointer(lib, size, type, stride, memPtr, funcPtr)
{
	vertexData.size = size;
	vertexData.type = type;
	vertexData.stride = stride;
	vertexData.pointer = Number(memPtr);
}

function Java_org_lwjgl_opengl_GL11_nglDrawArrays(lib, mode, first, count, funcPtr)
{
	var v = lib.getJNIDataView();
	if(curList)
	{
		// Capture client state at this point in time
		return pushDrawArraysInList(curList, v, mode, first, count);
	}
	// Upload vertex data
	uploadData(v, vertexData, vertexBuffer, vertexPosition, count);
	// Upload color data
	uploadData(v, colorData, colorBuffer, colorLocation, count);
	// Upload tex coord data
	uploadData(v, texCoordData, texCoordBuffer, texCoord, count);
	drawArraysImpl(mode, first, count);
}

function Java_org_lwjgl_opengl_GL11_nglDisableClientState(lib, v, funcPtr)
{
	if(v == 0x8074/*GL_VERTEX_ARRAY*/)
	{
		vertexData.enabled = false;
	}
	else if(v == 0x8075/*GL_NORMAL_ARRAY*/)
	{
		normalData.enabled = false;
	}
	else if(v == 0x8076/*GL_COLOR_ARRAY*/)
	{
		colorData.enabled = false;
	}
	else if(v == 0x8078/*GL_TEXTURE_COORD_ARRAY*/)
	{
		texCoordData.enabled = false;
	}
	else if(verboseLog)
	{
		console.log("glDisableClientState");
	}
}

function Java_org_lwjgl_opengl_GL11_nglColor4f(lib, r, g, b, a, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglColor4f);
	immediateModeData.currentColor[0] = r;
	immediateModeData.currentColor[1] = g;
	immediateModeData.currentColor[2] = b;
	immediateModeData.currentColor[3] = a;
	applyCurrentColorAttrib();
}

function Java_org_lwjgl_opengl_GL11_nglAlphaFunc()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glAlphaFunc");
}

function Java_org_lwjgl_opengl_GL11_nglGenLists(lib, range, funcPtr)
{
	checkNoList(curList);
	var ret = cmdLists.length;
	for(var i=0;i<range;i++)
		cmdLists.push([]);
	return ret;
}

function Java_org_lwjgl_opengl_GL11_nglListBase(lib, base, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglListBase);
	listBase = base;
}

function Java_org_lwjgl_opengl_GL11_nglNewList(lib, list, mode, funcPtr)
{
	checkNoList(curList);
	assert(mode == 0x1300/*GL_COMPILE*/);
	curList = cmdLists[list];
	// Wipe out the current contents of the list if any
	curList.length = 0;
}

function Java_org_lwjgl_opengl_GL11_nglEndList(lib, funcPtr)
{
	curList = null;
}

function Java_org_lwjgl_opengl_GL11_nglColor3f(lib, r, g, b, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglColor3f);
	immediateModeData.currentColor[0] = r;
	immediateModeData.currentColor[1] = g;
	immediateModeData.currentColor[2] = b;
	immediateModeData.currentColor[3] = 1;
	applyCurrentColorAttrib();
	if(verboseLog)
		console.log("glColor3f");
}

function Java_org_lwjgl_opengl_LinuxDisplay_nGetNativeCursorCapabilities()
{
}

function Java_org_lwjgl_opengl_GL11_nglShadeModel()
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglShadeModel);
	if(verboseLog)
		console.log("glShaderModel");
}

function Java_org_lwjgl_opengl_GL11_nglClearDepth(lib, a, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglClearDepth);
	glCtx.clearDepth(a);
}

function Java_org_lwjgl_opengl_GL11_nglDepthFunc(lib, a, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglDepthFunc);
	glCtx.depthFunc(a);
}

function Java_org_lwjgl_opengl_GL11_nglCullFace(lib, mode, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglCullFace);
	glCtx.cullFace(mode);
}

function Java_org_lwjgl_opengl_GL11_nglPushMatrix(lib, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPushMatrix);
	curMatrixStack.push(glMatrix.mat4.clone(curMatrixStack[curMatrixStack.length - 1]));
}

function Java_org_lwjgl_opengl_GL11_nglPopMatrix(lib, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPopMatrix);
	curMatrixStack.pop();
}

function Java_org_lwjgl_opengl_GL11_nglMultMatrixf(lib, memPtr, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglMultMatrixf);
	var m = getCurMatrixTop();
	var v = lib.getJNIDataView();
	var buf = new Float32Array(v.buffer, Number(memPtr), 16);
	var out = glMatrix.mat4.create();
	setCurMatrixTop(glMatrix.mat4.multiply(out, m, buf));
}

function Java_org_lwjgl_opengl_GL11_nglRotatef(lib, angle, x, y, z, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglRotatef);
	var m = getCurMatrixTop();
	var out = glMatrix.mat4.create();
	setCurMatrixTop(glMatrix.mat4.rotate(out, m, angle * Math.PI / 180.0, glMatrix.vec3.fromValues(x, y, z)));
}

function Java_org_lwjgl_opengl_GL11_nglDepthMask(lib, a, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglDepthMask);
	glCtx.depthMask(a);
}

function Java_org_lwjgl_opengl_GL11_nglBlendFunc(lib, sfactor, dfactor)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglBlendFunc);
	glCtx.blendFunc(sfactor, dfactor);
}

function Java_org_lwjgl_opengl_GL11_nglColorMask(lib, r, g, b, a, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglColorMask);
	glCtx.colorMask(r, g, b, a);
}

function Java_org_lwjgl_opengl_GL11_nglCopyTexSubImage2D(lib, target, level, xoffset, yoffset, x, y, width, height, funcPtr)
{
	checkNoList(curList);
	glCtx.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
}

function Java_org_lwjgl_opengl_GL11_nglScalef(lib, x, y, z, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglScalef);
	var m = getCurMatrixTop();
	var out = glMatrix.mat4.create();
	setCurMatrixTop(glMatrix.mat4.scale(out, m, glMatrix.vec3.fromValues(x, y, z)));
}

function Java_org_lwjgl_opengl_GL11_nglCallLists(lib, n, type, memPtr, funcPtr)
{
	checkNoList(curList);
	var v = lib.getJNIDataView();
	var buf;
	if(type == glCtx.UNSIGNED_BYTE)
		buf = new Uint8Array(v.buffer, Number(memPtr), n);
	else if(type == glCtx.UNSIGNED_SHORT)
		buf = new Uint16Array(v.buffer, Number(memPtr), n);
	else if(type == glCtx.UNSIGNED_INT)
		buf = new Uint32Array(v.buffer, Number(memPtr), n);
	else
	{
		warnOnce(unsupportedDrawModes, "calllists-" + type, "Unsupported glCallLists type=" + type);
		return;
	}
	for(var i=0;i<n;i++)
		callList(listBase + buf[i]);
}

function Java_org_lwjgl_opengl_GL11_nglFlush()
{
	checkNoList(curList);
	glCtx.flush();
}

function Java_org_lwjgl_opengl_GL11_nglTexSubImage2D(lib, target, level, xoffset, yoffset, width, height, format, type, memPtr, funcPtr)
{
	checkNoList(curList);
	assert(target == glCtx.TEXTURE_2D);
	var v = lib.getJNIDataView();
	var upload = normalizeTextureUpload(v, memPtr, width, height, format, format, type);
	glCtx.texSubImage2D(target, level, xoffset, yoffset, width, height, upload.format, upload.type, upload.data);
	var texSubImageErr = glCtx.getError();
	if(texSubImageErr != glCtx.NO_ERROR)
	{
		warnOnce(
			texImageWarnings,
			"texSubImage-error-" + upload.format + "-" + upload.type,
			"LWJGL texSubImage2D error=" + texSubImageErr + " fmt=" + upload.format + " type=" + upload.type + " size=" + width + "x" + height + " ptr=" + memPtr + " dataCtor=" + (upload.data && upload.data.constructor ? upload.data.constructor.name : "null")
		);
	}
}

function Java_org_lwjgl_opengl_GL11_nglGetFloatv(lib, a, memPtr, funcPtr)
{
	checkNoList(curList);
	var v = lib.getJNIDataView();
	var buf = new Float32Array(v.buffer, Number(memPtr), 16);
	if(a == /*GL_MODELVIEW_MATRIX*/0xba6)
	{
		var m = modelViewMatrixStack[modelViewMatrixStack.length - 1];
		for(var i=0;i<16;i++)
			buf[i] = m[i];
	}
	else if(a == /*GL_PROJECTION_MATRIX*/0xba7)
	{
		var m = projMatrixStack[projMatrixStack.length - 1];
		for(var i=0;i<16;i++)
			buf[i] = m[i];
	}
	else if(verboseLog)
	{
		console.log("glGetFloat "+a);
	}
}

function Java_org_lwjgl_opengl_GL11_nglFogfv()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glFog");
}

function Java_org_lwjgl_opengl_GL11_nglNormal3f()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glNormal3f");
}

function Java_org_lwjgl_opengl_GL11_nglFogi()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glFogi");
}

function Java_org_lwjgl_opengl_GL11_nglFogf()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glFogf");
}

function Java_org_lwjgl_opengl_GL11_nglColorMaterial()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glColorMaterial");
}

function Java_org_lwjgl_opengl_GL11_nglCallList(lib, listId, funcPtr)
{
	checkNoList(curList);
	callList(listId);
}

function Java_org_lwjgl_opengl_GL13_nglActiveTexture()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glActiveTexture");
}

function Java_org_lwjgl_opengl_GL11_nglLightfv()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glLightfv");
}

function Java_org_lwjgl_opengl_GL11_nglLightModelfv()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glLightModelfv");
}

function Java_org_lwjgl_opengl_GL11_nglNormalPointer(lib, type, stride, memPtr, funcPtr)
{
	normalData.size = 3;
	normalData.type = type;
	normalData.stride = stride;
	normalData.pointer = Number(memPtr);
}

function Java_org_lwjgl_opengl_GL13_nglMultiTexCoord2f()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glMultiTexCoord2f");
}

function Java_org_lwjgl_opengl_GL13_nglClientActiveTexture()
{
	if(verboseLog)
		console.log("glClientActiveTexture");
}

function Java_org_lwjgl_opengl_GL11_nglLineWidth()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glLineWidth");
}

function Java_org_lwjgl_opengl_GL11_nglPolygonOffset()
{
	checkNoList(curList);
	if(verboseLog)
		console.log("glPolygonOffset");
}

function Java_org_lwjgl_opengl_GL11_nglBegin(lib, mode, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglBegin);
	immediateModeData.mode = mode;
	immediateModeData.vertexPos = 0;
	immediateModeData.colorPos = 0;
	immediateModeData.texCoordPos = 0;
}

function Java_org_lwjgl_opengl_GL11_nglTexCoord2f(lib, x, y, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglTexCoord2f);
	var curPos = immediateModeData.texCoordPos;
	immediateModeData.texCoordBuf =
		ensureImmediateArrayCapacity(immediateModeData.texCoordBuf, curPos + 2);
	immediateModeData.texCoordBuf[curPos] = x;
	immediateModeData.texCoordBuf[curPos + 1] = y;
	immediateModeData.texCoordPos = curPos + 2;
}

function Java_org_lwjgl_opengl_GL11_nglVertex3f(lib, x, y, z, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglVertex3f);
	var curPos = immediateModeData.vertexPos;
	immediateModeData.vertexBuf =
		ensureImmediateArrayCapacity(immediateModeData.vertexBuf, curPos + 3);
	immediateModeData.vertexBuf[curPos] = x;
	immediateModeData.vertexBuf[curPos + 1] = y;
	immediateModeData.vertexBuf[curPos + 2] = z;
	immediateModeData.vertexPos = curPos + 3;

	var colorPos = immediateModeData.colorPos;
	immediateModeData.colorBuf =
		ensureImmediateArrayCapacity(immediateModeData.colorBuf, colorPos + 4);
	immediateModeData.colorBuf[colorPos] = immediateModeData.currentColor[0];
	immediateModeData.colorBuf[colorPos + 1] = immediateModeData.currentColor[1];
	immediateModeData.colorBuf[colorPos + 2] = immediateModeData.currentColor[2];
	immediateModeData.colorBuf[colorPos + 3] = immediateModeData.currentColor[3];
	immediateModeData.colorPos = colorPos + 4;
}

function Java_org_lwjgl_opengl_GL11_nglEnd(lib, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglEnd);
	var vertexCount = immediateModeData.vertexPos / 3;
	// Upload vertex data
	uploadDataImpl(immediateModeData.vertexBuf.subarray(0, immediateModeData.vertexPos), vertexBuffer, vertexPosition, 3, glCtx.FLOAT, 3 * 4);
	// Upload the OpenGL immediate-mode color captured for each vertex.
	uploadDataImpl(immediateModeData.colorBuf.subarray(0, vertexCount * 4), colorBuffer, colorLocation, 4, glCtx.FLOAT, 4 * 4);
	// Upload tex coord data when present; otherwise use the OpenGL default (0, 0).
	if(immediateModeData.texCoordPos >= vertexCount * 2)
	{
		uploadDataImpl(immediateModeData.texCoordBuf.subarray(0, vertexCount * 2), texCoordBuffer, texCoord, 2, glCtx.FLOAT, 2 * 4);
	}
	else
	{
		glCtx.disableVertexAttribArray(texCoord);
		glCtx.vertexAttrib2f(texCoord, 0, 0);
	}
	// NOTE: We count vertices
	drawArraysImpl(immediateModeData.mode, 0, vertexCount);
}

// These stubs make sure audio creation fails sooner rather than later
function Java_org_lwjgl_openal_AL_nCreate()
{
}

function Java_org_lwjgl_openal_AL10_initNativeStubs()
{
}

function Java_org_lwjgl_openal_ALC10_initNativeStubs()
{
}

function Java_org_lwjgl_openal_ALC10_nalcOpenDevice()
{
}

function Java_org_lwjgl_openal_AL_resetNativeStubs()
{
}

function Java_org_lwjgl_openal_AL_nDestroy()
{
}

// Basic input support
async function Java_org_lwjgl_opengl_LinuxEvent_createEventBuffer(lib)
{
	// This is intended to represent a X11 event, but we are free to use any layout
	var ByteBuffer = await lib.java.nio.ByteBuffer;
	return await ByteBuffer.allocateDirect(4 * 8);
}

async function Java_org_lwjgl_opengl_LinuxEvent_nNextEvent(lib, windowId, buffer)
{
	// Resolve the address and directly access the JNI memory
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	var e = eventQueue.shift();
	switch(e.type)
	{
		case "focus":
			v.setInt32(0, /*FocusIn*/9, true);
			break;
		case "mousedown":
			v.setInt32(0, /*ButtonPress*/4, true);
			v.setInt32(4, e.x, true);
			v.setInt32(8, e.y, true);
			v.setInt32(12, e.button, true);
			break;
		case "mouseup":
			v.setInt32(0, /*ButtonRelease*/5, true);
			v.setInt32(4, e.x, true);
			v.setInt32(8, e.y, true);
			v.setInt32(12, e.button, true);
			break;
		case "mousemove":
			v.setInt32(0, /*MotionNotify*/6, true);
			v.setInt32(4, e.x, true);
			v.setInt32(8, e.y, true);
			break;
		case "keydown":
			v.setInt32(0, /*KeyPress*/2, true);
			v.setInt32(4, e.keyCode, true);
			break;
		case "keyup":
			v.setInt32(0, /*KeyRelease*/3, true);
			v.setInt32(4, e.keyCode, true);
			break;
		default:
			warnOnce(unsupportedEventTypes, e.type, "Unsupported X11 event type=" + e.type);
	}
}

function Java_org_lwjgl_opengl_LinuxEvent_nGetWindow()
{
	// Only a single window is emulated
	return 0;
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetType(lib, buffer)
{
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(0, true);
}

function Java_org_lwjgl_opengl_LinuxEvent_nFilterEvent()
{
}

function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonTime()
{
	// TODO: Event timestamps
}

function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonRoot()
{
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonXRoot(lib, buffer)
{
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(4, true);
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonYRoot(lib, buffer)
{
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(8, true);
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonX(lib, buffer)
{
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(4, true);
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonY(lib, buffer)
{
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(8, true);
}

function Java_org_lwjgl_opengl_LinuxEvent_nGetFocusDetail()
{
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonType(lib, buffer)
{
	// Same as type, apparently
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(0, true);
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonButton(lib, buffer)
{
	const v = lib.getJNIDataView();
	return v.getInt32(12, true);
}

function Java_org_lwjgl_opengl_LinuxDisplay_nGrabPointer()
{
	glCanvas.requestPointerLock();
	lockedMousePos = { x: 0, y: 0 };
}

function Java_org_lwjgl_opengl_LinuxDisplay_nUngrabPointer()
{
	document.exitPointerLock();
	lockedMousePos = null;
}

function Java_org_lwjgl_opengl_LinuxDisplay_nDefineCursor()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_getRootWindow()
{
}

function Java_org_lwjgl_opengl_LinuxDisplay_nSetWindowIcon()
{
}

function Java_org_lwjgl_opengl_LinuxMouse_nGetWindowWidth()
{
	return getCanvasWidth();
}

function Java_org_lwjgl_opengl_LinuxMouse_nSendWarpEvent()
{
}

function Java_org_lwjgl_opengl_LinuxMouse_nWarpCursor()
{
}

function Java_org_lwjgl_opengl_LinuxEvent_nSetWindow()
{
}

function Java_org_lwjgl_opengl_LinuxEvent_nSendEvent()
{
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyAddress(lib, buffer)
{
	// Should probably be a pointer, cheat and use directly the value
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(4, true);
}

function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyTime()
{
	// TODO: Event timestamps
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyType(lib, buffer)
{
	// Same as type, apparently
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(0, true);
}

async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyKeyCode(lib, buffer)
{
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	return v.getInt32(4, true);
}

function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyState()
{
}

function Java_org_lwjgl_opengl_LinuxKeyboard_lookupKeysym(lib, eventPtr, index)
{
	return Number(eventPtr);
}

async function Java_org_lwjgl_opengl_LinuxKeyboard_lookupString(lib, eventPtr, buffer)
{
	// Only support single chars
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	v.setInt8(bufferAddr, Number(eventPtr));
	return 1;
}

export default {
	Java_org_lwjgl_DefaultSysImplementation_getPointerSize,
	Java_org_lwjgl_DefaultSysImplementation_getJNIVersion,
	Java_org_lwjgl_DefaultSysImplementation_setDebug,
	Java_org_lwjgl_opengl_LinuxDisplay_nLockAWT,
	Java_org_lwjgl_opengl_LinuxDisplay_nUnlockAWT,
	Java_org_lwjgl_opengl_LinuxDisplay_setErrorHandler,
	Java_org_lwjgl_opengl_LinuxDisplay_openDisplay,
	Java_org_lwjgl_opengl_LinuxDisplay_nInternAtom,
	Java_org_lwjgl_opengl_LinuxDisplay_nIsXrandrSupported,
	Java_org_lwjgl_opengl_LinuxDisplay_nIsXF86VidModeSupported,
	Java_org_lwjgl_opengl_LinuxDisplay_nGetDefaultScreen,
	Java_org_lwjgl_opengl_LinuxDisplay_nGetAvailableDisplayModes,
	Java_org_lwjgl_opengl_LinuxDisplay_nGetCurrentGammaRamp,
	Java_org_lwjgl_opengl_LinuxPeerInfo_createHandle,
	Java_org_lwjgl_opengl_GLContext_nLoadOpenGLLibrary,
	Java_org_lwjgl_opengl_LinuxDisplayPeerInfo_initDefaultPeerInfo,
	Java_org_lwjgl_opengl_LinuxDisplayPeerInfo_initDrawable,
	Java_org_lwjgl_opengl_AWTSurfaceLock_createHandle,
	Java_org_lwjgl_opengl_AWTSurfaceLock_lockAndInitHandle,
	Java_org_lwjgl_opengl_LinuxAWTGLCanvasPeerInfo_getScreenFromSurfaceInfo,
	Java_org_lwjgl_opengl_LinuxAWTGLCanvasPeerInfo_nInitHandle,
	Java_org_lwjgl_opengl_AWTSurfaceLock_nUnlock,
	Java_org_lwjgl_opengl_LinuxPeerInfo_nGetDrawable,
	Java_org_lwjgl_opengl_LinuxDisplay_nCreateWindow,
	Java_org_lwjgl_opengl_LinuxDisplay_mapRaised,
	Java_org_lwjgl_opengl_LinuxDisplay_nCreateBlankCursor,
	Java_org_lwjgl_opengl_LinuxDisplay_nSetTitle,
	Java_org_lwjgl_opengl_LinuxMouse_nGetButtonCount,
	Java_org_lwjgl_opengl_LinuxMouse_nQueryPointer,
	Java_org_lwjgl_opengl_LinuxMouse_nGetWindowHeight,
	Java_org_lwjgl_opengl_LinuxKeyboard_getModifierMapping,
	Java_org_lwjgl_opengl_LinuxKeyboard_nSetDetectableKeyRepeat,
	Java_org_lwjgl_opengl_LinuxKeyboard_openIM,
	Java_org_lwjgl_opengl_LinuxKeyboard_allocateComposeStatus,
	Java_org_lwjgl_opengl_LinuxContextImplementation_nCreate,
	Java_org_lwjgl_opengl_LinuxContextImplementation_nMakeCurrent,
	Java_org_lwjgl_opengl_LinuxContextImplementation_nIsCurrent,
	Java_org_lwjgl_opengl_GLContext_ngetFunctionAddress,
	Java_org_lwjgl_opengl_GL11_nglGetString,
	Java_org_lwjgl_opengl_GL11_nglGetIntegerv,
	Java_org_lwjgl_opengl_GL11_nglGetError,
	Java_org_lwjgl_opengl_LinuxContextImplementation_nSetSwapInterval,
	Java_org_lwjgl_opengl_GL11_nglClearColor,
	Java_org_lwjgl_opengl_GL11_nglClear,
	Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers,
	Java_org_lwjgl_opengl_LinuxEvent_getPending,
	Java_org_lwjgl_opengl_GL11_nglMatrixMode,
	Java_org_lwjgl_opengl_GL11_nglLoadIdentity,
	Java_org_lwjgl_opengl_GL11_nglOrtho,
	Java_org_lwjgl_opengl_GL11_nglTranslatef,
	Java_org_lwjgl_opengl_GL11_nglViewport,
	Java_org_lwjgl_opengl_GL11_nglDisable,
	Java_org_lwjgl_opengl_GL11_nglEnable,
	Java_org_lwjgl_opengl_GL11_nglGenTextures,
	Java_org_lwjgl_opengl_GL11_nglBindTexture,
	Java_org_lwjgl_opengl_GL11_nglTexParameteri,
	Java_org_lwjgl_opengl_GL11_nglTexImage2D,
	Java_org_lwjgl_opengl_GL11_nglTexCoordPointer,
	Java_org_lwjgl_opengl_GL11_nglEnableClientState,
	Java_org_lwjgl_opengl_GL11_nglColorPointer,
	Java_org_lwjgl_opengl_GL11_nglVertexPointer,
	Java_org_lwjgl_opengl_GL11_nglDrawArrays,
	Java_org_lwjgl_opengl_GL11_nglDisableClientState,
	Java_org_lwjgl_opengl_GL11_nglColor4f,
	Java_org_lwjgl_opengl_GL11_nglAlphaFunc,
	Java_org_lwjgl_opengl_GL11_nglGenLists,
	Java_org_lwjgl_opengl_GL11_nglListBase,
	Java_org_lwjgl_opengl_GL11_nglNewList,
	Java_org_lwjgl_opengl_GL11_nglEndList,
	Java_org_lwjgl_opengl_GL11_nglColor3f,
	Java_org_lwjgl_opengl_LinuxDisplay_nGetNativeCursorCapabilities,
	Java_org_lwjgl_opengl_GL11_nglShadeModel,
	Java_org_lwjgl_opengl_GL11_nglClearDepth,
	Java_org_lwjgl_opengl_GL11_nglDepthFunc,
	Java_org_lwjgl_opengl_GL11_nglCullFace,
	Java_org_lwjgl_opengl_GL11_nglPushMatrix,
	Java_org_lwjgl_opengl_GL11_nglPopMatrix,
	Java_org_lwjgl_opengl_GL11_nglMultMatrixf,
	Java_org_lwjgl_opengl_GL11_nglRotatef,
	Java_org_lwjgl_opengl_GL11_nglDepthMask,
	Java_org_lwjgl_opengl_GL11_nglBlendFunc,
	Java_org_lwjgl_opengl_GL11_nglColorMask,
	Java_org_lwjgl_opengl_GL11_nglCopyTexSubImage2D,
	Java_org_lwjgl_opengl_GL11_nglScalef,
	Java_org_lwjgl_opengl_GL11_nglCallLists,
	Java_org_lwjgl_opengl_GL11_nglFlush,
	Java_org_lwjgl_opengl_GL11_nglTexSubImage2D,
	Java_org_lwjgl_opengl_GL11_nglGetFloatv,
	Java_org_lwjgl_opengl_GL11_nglFogfv,
	Java_org_lwjgl_opengl_GL11_nglNormal3f,
	Java_org_lwjgl_opengl_GL11_nglFogi,
	Java_org_lwjgl_opengl_GL11_nglFogf,
	Java_org_lwjgl_opengl_GL11_nglColorMaterial,
	Java_org_lwjgl_opengl_GL11_nglCallList,
	Java_org_lwjgl_opengl_GL13_nglActiveTexture,
	Java_org_lwjgl_opengl_GL11_nglLightfv,
	Java_org_lwjgl_opengl_GL11_nglLightModelfv,
	Java_org_lwjgl_opengl_GL11_nglNormalPointer,
	Java_org_lwjgl_opengl_GL13_nglMultiTexCoord2f,
	Java_org_lwjgl_opengl_GL13_nglClientActiveTexture,
	Java_org_lwjgl_opengl_GL11_nglLineWidth,
	Java_org_lwjgl_opengl_GL11_nglPolygonOffset,
	Java_org_lwjgl_opengl_GL11_nglBegin,
	Java_org_lwjgl_opengl_GL11_nglTexCoord2f,
	Java_org_lwjgl_opengl_GL11_nglVertex3f,
	Java_org_lwjgl_opengl_GL11_nglEnd,
	Java_org_lwjgl_openal_AL_nCreate,
	Java_org_lwjgl_openal_AL10_initNativeStubs,
	Java_org_lwjgl_openal_ALC10_initNativeStubs,
	Java_org_lwjgl_openal_ALC10_nalcOpenDevice,
	Java_org_lwjgl_openal_AL_resetNativeStubs,
	Java_org_lwjgl_openal_AL_nDestroy,
	Java_org_lwjgl_opengl_LinuxEvent_createEventBuffer,
	Java_org_lwjgl_opengl_LinuxEvent_nNextEvent,
	Java_org_lwjgl_opengl_LinuxEvent_nGetWindow,
	Java_org_lwjgl_opengl_LinuxEvent_nGetType,
	Java_org_lwjgl_opengl_LinuxEvent_nFilterEvent,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonTime,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonRoot,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonXRoot,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonYRoot,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonX,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonY,
	Java_org_lwjgl_opengl_LinuxEvent_nGetFocusDetail,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonType,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonButton,
	Java_org_lwjgl_opengl_LinuxDisplay_nGrabPointer,
	Java_org_lwjgl_opengl_LinuxDisplay_nUngrabPointer,
	Java_org_lwjgl_opengl_LinuxDisplay_nDefineCursor,
	Java_org_lwjgl_opengl_LinuxDisplay_getRootWindow,
	Java_org_lwjgl_opengl_LinuxDisplay_nSetWindowIcon,
	Java_org_lwjgl_opengl_LinuxMouse_nGetWindowWidth,
	Java_org_lwjgl_opengl_LinuxMouse_nSendWarpEvent,
	Java_org_lwjgl_opengl_LinuxMouse_nWarpCursor,
	Java_org_lwjgl_opengl_LinuxEvent_nSetWindow,
	Java_org_lwjgl_opengl_LinuxEvent_nSendEvent,
	Java_org_lwjgl_opengl_LinuxEvent_nGetKeyAddress,
	Java_org_lwjgl_opengl_LinuxEvent_nGetKeyTime,
	Java_org_lwjgl_opengl_LinuxEvent_nGetKeyType,
	Java_org_lwjgl_opengl_LinuxEvent_nGetKeyKeyCode,
	Java_org_lwjgl_opengl_LinuxEvent_nGetKeyState,
	Java_org_lwjgl_opengl_LinuxKeyboard_lookupKeysym,
	Java_org_lwjgl_opengl_LinuxKeyboard_lookupString,
}
