// Load the glMatrix library from the same origin to satisfy the page CSP.
const { default: glMatrix } = await import("./gl-matrix-loader.js");

const glCanvas = window.lwjglCanvasElement;
if (!(glCanvas instanceof HTMLCanvasElement)) throw new Error("window.lwjglCanvasElement is not set or is not a canvas");
const glCtx = glCanvas.getContext("webgl2", {
	antialias: false,
	alpha: false,
	preserveDrawingBuffer: false,
	desynchronized: true,
	powerPreference: "high-performance"
});

window.__lwjglGraphicsInfo = (() => {
	if(!glCtx) return null;
	try {
		const extensions = glCtx.getSupportedExtensions() || [];
		return {
			backend: "webgl2",
			version: glCtx.getParameter(glCtx.VERSION),
			shadingLanguageVersion: glCtx.getParameter(glCtx.SHADING_LANGUAGE_VERSION),
			vendor: glCtx.getParameter(glCtx.VENDOR),
			renderer: glCtx.getParameter(glCtx.RENDERER),
			maxTextureSize: glCtx.getParameter(glCtx.MAX_TEXTURE_SIZE),
			maxVertexAttribs: glCtx.getParameter(glCtx.MAX_VERTEX_ATTRIBS),
			maxCombinedTextureUnits: glCtx.getParameter(glCtx.MAX_COMBINED_TEXTURE_IMAGE_UNITS),
			maxDrawBuffers: glCtx.getParameter(glCtx.MAX_DRAW_BUFFERS),
			maxSamples: glCtx.getParameter(glCtx.MAX_SAMPLES),
			extensionCount: extensions.length,
			extensions,
			contextAttributes: glCtx.getContextAttributes()
		};
	} catch(error) {
		return { backend: "webgl2", error: String(error && (error.message || error) || error) };
	}
})();
try { console.log("LWJGLGraphicsInfo: " + JSON.stringify(window.__lwjglGraphicsInfo)); } catch(_) {}
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
	uniform float uPointSize;
	varying vec2 vTexCoord;
	varying vec4 vColor;
	void main() {
		gl_Position = projection * modelView * aVertexPosition;
		gl_PointSize = uPointSize;
		vTexCoord = aTexCoord;
		vColor = aColor;
	}
`;
// NOTE: Only the default GL_MODULATE texEnv is supported here
var fragmentShaderSrc = `
	precision mediump float;
	uniform float uTextureMask;
	uniform sampler2D uSampler;
	uniform float uAlphaTestEnabled;
	uniform float uAlphaFunc;
	uniform float uAlphaRef;
	varying vec2 vTexCoord;
	varying vec4 vColor;
	bool alphaTestPass(float alpha) {
		if(uAlphaTestEnabled < 0.5) return true;
		if(uAlphaFunc < 512.5) return false;             // GL_NEVER 0x0200
		if(uAlphaFunc < 513.5) return alpha < uAlphaRef; // GL_LESS
		if(uAlphaFunc < 514.5) return abs(alpha - uAlphaRef) <= (1.0 / 255.0); // GL_EQUAL
		if(uAlphaFunc < 515.5) return alpha <= uAlphaRef; // GL_LEQUAL
		if(uAlphaFunc < 516.5) return alpha > uAlphaRef;  // GL_GREATER
		if(uAlphaFunc < 517.5) return abs(alpha - uAlphaRef) > (1.0 / 255.0); // GL_NOTEQUAL
		if(uAlphaFunc < 518.5) return alpha >= uAlphaRef; // GL_GEQUAL
		return true;                                      // GL_ALWAYS 0x0207
	}
	void main() {
		vec4 texSample = texture2D(uSampler, vTexCoord);
		vec4 fragment = mix(vColor, texSample * vColor, uTextureMask);
		if(!alphaTestPass(fragment.a)) discard;
		gl_FragColor = fragment;
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
// WEBGL_QUAD_INDEX_BATCH_V2: WebGL2 has no GL_QUADS. Cache an index buffer that
// expands each legacy 4-vertex quad into two triangles so a whole OpenGL quad
// batch is submitted in one WebGL draw call instead of one call per quad.
var quadIndexBuffer = glCtx.createBuffer();
var quadIndexVertexCapacity = 0;
var vertexPosition = glCtx.getAttribLocation(program, "aVertexPosition");
var colorLocation = glCtx.getAttribLocation(program, "aColor");
var texCoord = glCtx.getAttribLocation(program, "aTexCoord");
var mvLocation = glCtx.getUniformLocation(program, "modelView");
var projLocation = glCtx.getUniformLocation(program, "projection");
var pointSizeLocation = glCtx.getUniformLocation(program, "uPointSize");
var pointSizeState = 1.0;
var samplerLocation = glCtx.getUniformLocation(program, "uSampler");
var samplerLocation2 = glCtx.getUniformLocation(program, "uSampler2");
var texMaskLocation = glCtx.getUniformLocation(program, "uTextureMask");
var alphaTestEnabledLocation = glCtx.getUniformLocation(program, "uAlphaTestEnabled");
var alphaFuncLocation = glCtx.getUniformLocation(program, "uAlphaFunc");
var alphaRefLocation = glCtx.getUniformLocation(program, "uAlphaRef");
var alphaTestState = { enabled: false, func: 0x0207/*GL_ALWAYS*/, ref: 0.0 };
var alphaTestWarnings = new Set();
var texture2DEnabled = false;
var attribStateStack = [];
var attribStateWarnings = new Set();
function syncAlphaTestUniforms()
{
	glCtx.uniform1f(alphaTestEnabledLocation, alphaTestState.enabled ? 1 : 0);
	glCtx.uniform1f(alphaFuncLocation, alphaTestState.func);
	glCtx.uniform1f(alphaRefLocation, alphaTestState.ref);
}
function setTexture2DEnabled(enabled)
{
	texture2DEnabled = !!enabled;
	glCtx.uniform1f(texMaskLocation, texture2DEnabled ? 1 : 0);
}
function getCompatEnableState(cap)
{
	if(cap == 0x0BC0/*GL_ALPHA_TEST*/) return alphaTestState.enabled;
	if(cap == glCtx.TEXTURE_2D || cap == 0x806F/*GL_TEXTURE_3D*/) return texture2DEnabled;
	try { return glCtx.isEnabled(cap); } catch(_) { return false; }
}
function setCompatEnableState(cap, enabled)
{
	if(cap == 0x0BC0/*GL_ALPHA_TEST*/)
	{
		alphaTestState.enabled = !!enabled;
		syncAlphaTestUniforms();
		return;
	}
	if(cap == glCtx.TEXTURE_2D || cap == 0x806F/*GL_TEXTURE_3D*/)
	{
		setTexture2DEnabled(enabled);
		return;
	}
	try { enabled ? glCtx.enable(cap) : glCtx.disable(cap); } catch(_) {}
}
function snapshotAttribState(mask)
{
	var state = { mask: mask };
	if(mask & 0x2000/*GL_ENABLE_BIT*/)
	{
		state.enable = {};
		for(const cap of [glCtx.BLEND, glCtx.CULL_FACE, glCtx.DEPTH_TEST, glCtx.SCISSOR_TEST, glCtx.STENCIL_TEST, 0x0BC0/*GL_ALPHA_TEST*/, glCtx.TEXTURE_2D])
			state.enable[cap] = getCompatEnableState(cap);
	}
	if(mask & 0x4000/*GL_COLOR_BUFFER_BIT*/)
	{
		state.color = {
			blendSrcRgb: glCtx.getParameter(glCtx.BLEND_SRC_RGB),
			blendDstRgb: glCtx.getParameter(glCtx.BLEND_DST_RGB),
			blendSrcAlpha: glCtx.getParameter(glCtx.BLEND_SRC_ALPHA),
			blendDstAlpha: glCtx.getParameter(glCtx.BLEND_DST_ALPHA),
			colorMask: Array.from(glCtx.getParameter(glCtx.COLOR_WRITEMASK)),
			clearColor: Array.from(glCtx.getParameter(glCtx.COLOR_CLEAR_VALUE))
		};
		state.alpha = { func: alphaTestState.func, ref: alphaTestState.ref };
	}
	if(mask & 0x0100/*GL_DEPTH_BUFFER_BIT*/)
	{
		state.depth = {
			writeMask: glCtx.getParameter(glCtx.DEPTH_WRITEMASK),
			func: glCtx.getParameter(glCtx.DEPTH_FUNC),
			clearValue: glCtx.getParameter(glCtx.DEPTH_CLEAR_VALUE)
		};
	}
	if(mask & 0x0800/*GL_VIEWPORT_BIT*/)
	{
		state.viewport = {
			box: Array.from(glCtx.getParameter(glCtx.VIEWPORT)),
			depthRange: Array.from(glCtx.getParameter(glCtx.DEPTH_RANGE))
		};
	}
	if(mask & 0x0400/*GL_STENCIL_BUFFER_BIT*/)
	{
		state.stencil = {
			func: glCtx.getParameter(glCtx.STENCIL_FUNC), ref: glCtx.getParameter(glCtx.STENCIL_REF),
			valueMask: glCtx.getParameter(glCtx.STENCIL_VALUE_MASK), writeMask: glCtx.getParameter(glCtx.STENCIL_WRITEMASK),
			fail: glCtx.getParameter(glCtx.STENCIL_FAIL), depthFail: glCtx.getParameter(glCtx.STENCIL_PASS_DEPTH_FAIL),
			depthPass: glCtx.getParameter(glCtx.STENCIL_PASS_DEPTH_PASS), clearValue: glCtx.getParameter(glCtx.STENCIL_CLEAR_VALUE)
		};
	}
	return state;
}
function restoreAttribState(state)
{
	if(state.enable)
	{
		for(const key of Object.keys(state.enable)) setCompatEnableState(Number(key), state.enable[key]);
	}
	if(state.color)
	{
		glCtx.blendFuncSeparate(state.color.blendSrcRgb, state.color.blendDstRgb, state.color.blendSrcAlpha, state.color.blendDstAlpha);
		glCtx.colorMask(...state.color.colorMask);
		glCtx.clearColor(...state.color.clearColor);
	}
	if(state.depth)
	{
		glCtx.depthMask(state.depth.writeMask);
		glCtx.depthFunc(state.depth.func);
		glCtx.clearDepth(state.depth.clearValue);
	}
	if(state.viewport)
	{
		glCtx.viewport(...state.viewport.box);
		glCtx.depthRange(...state.viewport.depthRange);
	}
	if(state.stencil)
	{
		glCtx.stencilFunc(state.stencil.func, state.stencil.ref, state.stencil.valueMask);
		glCtx.stencilMask(state.stencil.writeMask);
		glCtx.stencilOp(state.stencil.fail, state.stencil.depthFail, state.stencil.depthPass);
		glCtx.clearStencil(state.stencil.clearValue);
	}
	// GL_COLOR_BUFFER_BIT owns the alpha comparison function/reference. The
	// alpha-test enable itself is restored above through GL_ENABLE_BIT.
	if(state.alpha)
	{
		alphaTestState.func = state.alpha.func;
		alphaTestState.ref = state.alpha.ref;
		syncAlphaTestUniforms();
	}
}
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
	currentTexCoord: [0, 0],
	texCoordBuf: new Float32Array(32),
	texCoordPos: 0,
	interleavedBuf: new Float32Array(96),
	interleavedPos: 0
};
// WEBGL_IMMEDIATE_INTERLEAVED_V1: immediate mode already captures exactly
// position(3)+color(4)+texcoord(2) per vertex. Keep those nine floats together
// so glEnd performs one WebGL upload instead of three independent uploads.
var immediateInterleavedEnabled = typeof window === "undefined" || window.__LWJGL_IMMEDIATE_INTERLEAVED__ !== false;
// LWJGL_IMMEDIATE_COLOR_ATTRIB_DEFER_V1: glColor inside interleaved glBegin/glEnd
// is already captured per vertex. Defer the generic color attribute until a
// client-array draw actually needs it instead of issuing two WebGL calls here.
var immediateBeginActive = false;
var verboseLog = false;
var strictWebGLValidation = typeof window !== "undefined" && window.__LWJGL_STRICT_WEBGL_VALIDATION__ === true;
var presentationReadbackDiagnostics = typeof window !== "undefined" && window.__LWJGL_PRESENTATION_READBACK_DIAGNOSTICS__ === true;
// LWJGL_DETAILED_DRAW_TELEMETRY_OPTIN_V1: high-frequency draw/upload counters are diagnostics only. Browser gameplay defaults them off; Node/static verifiers opt in.
var detailedDrawTelemetryEnabled = typeof window === "undefined" || window.__LWJGL_DETAILED_DRAW_TELEMETRY__ === true;
var frameCount = 0;
var presentationStats = {
	swapCount: 0,
	samples: [],
	lastFramebufferStatus: null,
	lastViewport: null,
	recentFps: 0,
	recentFrameMs: 0,
	legacyDrawCalls: 0,
	webglDrawCalls: 0,
	quadBatches: 0,
	quadQuads: 0,
	quadDrawCallsSaved: 0,
	quadIndexBufferUploads: 0,
	immediateInterleavedDraws: 0,
	immediateInterleavedUploads: 0,
	immediateInterleavedUploadsSaved: 0,
	immediateInterleavedBytes: 0,
	immediatePointerLayoutRefreshes: 0,
	immediateColorAttribDeferredObserved: false,
	detailedDrawTelemetryActive: detailedDrawTelemetryEnabled
};
var recentSwapTimes = [];
if(typeof window !== "undefined")
	window.__lwjglPresentationStats = presentationStats;
// Set to a non-zero value to stop after a certain number of frames
var frameLimit = 0;
var unsupportedDrawModes = new Set();
var clientArrayWarnings = new Set();
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
	glCtx.renderbufferStorage(glCtx.RENDERBUFFER, glCtx.DEPTH24_STENCIL8, fbWidth, fbHeight);
	glCtx.bindRenderbuffer(glCtx.RENDERBUFFER, null);
	glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);
	glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb);
}
// LWJGL_CLIENT_ARRAY_COMPAT_V1
// Desktop OpenGL permits tightly-packed stride=0 and client array scalar types
// that WebGL2 does not accept in vertexAttribPointer(). Normalize both here.
function clientArrayComponentBytes(type)
{
	if(type == 0 || type == glCtx.FLOAT || type == 0x1404/*GL_INT*/ || type == 0x1405/*GL_UNSIGNED_INT*/) return 4;
	if(type == glCtx.BYTE || type == glCtx.UNSIGNED_BYTE) return 1;
	if(type == glCtx.SHORT || type == glCtx.UNSIGNED_SHORT) return 2;
	if(typeof glCtx.HALF_FLOAT != "undefined" && type == glCtx.HALF_FLOAT) return 2;
	if(type == 0x140A/*GL_DOUBLE*/) return 8;
	return 0;
}
function normalizeLegacyClientArrayLayout(type, stride)
{
	// Old bridge.jar FloatBuffer overloads called ngl*Pointer(size, stride, 0, ...).
	// A zero type means packed GL_FLOAT; an impossible low non-zero enum means
	// the original stride was shifted into the type slot.
	if(type == 0) return { type: glCtx.FLOAT, stride: stride };
	if(stride == 0 && type > 0 && type < 0x1000) return { type: glCtx.FLOAT, stride: type };
	return { type: type, stride: stride };
}
function clientArrayEffectiveStride(size, type, stride)
{
	var layout = normalizeLegacyClientArrayLayout(type, stride);
	type = layout.type;
	stride = layout.stride;
	if(stride > 0) return stride;
	var componentBytes = clientArrayComponentBytes(type);
	return componentBytes > 0 ? size * componentBytes : 0;
}
function clientArrayByteLength(size, type, stride, count)
{
	if(count <= 0) return 0;
	var layout = normalizeLegacyClientArrayLayout(type, stride);
	var effectiveStride = clientArrayEffectiveStride(size, layout.type, layout.stride);
	var componentBytes = clientArrayComponentBytes(layout.type);
	if(effectiveStride <= 0 || componentBytes <= 0) return 0;
	return (count - 1) * effectiveStride + size * componentBytes;
}
function isWebGLClientArrayType(type)
{
	return type == glCtx.BYTE || type == glCtx.UNSIGNED_BYTE ||
		type == glCtx.SHORT || type == glCtx.UNSIGNED_SHORT ||
		type == glCtx.FLOAT ||
		(typeof glCtx.HALF_FLOAT != "undefined" && type == glCtx.HALF_FLOAT);
}
function isIntegerClientArrayType(type)
{
	return type == glCtx.BYTE || type == glCtx.UNSIGNED_BYTE ||
		type == glCtx.SHORT || type == glCtx.UNSIGNED_SHORT ||
		type == 0x1404/*GL_INT*/ || type == 0x1405/*GL_UNSIGNED_INT*/;
}
function convertDesktopClientArrayToFloat(buf, size, type, stride, count, normalizeColor)
{
	var componentBytes = clientArrayComponentBytes(type);
	if(componentBytes <= 0) return null;
	var effectiveStride = clientArrayEffectiveStride(size, type, stride);
	if(effectiveStride <= 0) return null;
	var view = new DataView(buf.buffer, buf.byteOffset, buf.byteLength);
	var out = new Float32Array(size * count);
	for(var i=0;i<count;i++)
	{
		var vertexBase = i * effectiveStride;
		for(var c=0;c<size;c++)
		{
			var off = vertexBase + c * componentBytes;
			if(off + componentBytes > view.byteLength) return null;
			var value;
			if(type == 0x1404/*GL_INT*/)
			{
				value = view.getInt32(off, true);
				if(normalizeColor) value = Math.max(-1, value / 2147483647);
			}
			else if(type == 0x1405/*GL_UNSIGNED_INT*/)
			{
				value = view.getUint32(off, true);
				if(normalizeColor) value = value / 4294967295;
			}
			else if(type == 0x140A/*GL_DOUBLE*/)
				value = view.getFloat64(off, true);
			else
				return null;
			out[i * size + c] = value;
		}
	}
	return out;
}
// LWJGL_IMMEDIATE_POINTER_DIRTY_V1
// Immediate-mode interleaving always uses one fixed 3/4/2-float layout.
// Legacy client uploads are the only other pointer writers, so they mark that
// fixed layout dirty instead of forcing three vertexAttribPointer calls per draw.
var immediatePointerLayoutDirty = true;
function uploadDataImpl(buf, buffer, attributeLocation, size, type, stride, count)
{
	var originalType = type;
	var originalStride = stride;
	var layout = normalizeLegacyClientArrayLayout(type, stride);
	type = layout.type;
	stride = layout.stride;
	if(originalType != type || originalStride != stride)
	{
		warnOnce(clientArrayWarnings, "legacy-layout-" + attributeLocation + "-" + originalType + "-" + originalStride,
			"LWJGL client array repaired legacy layout type=" + originalType + " stride=" + originalStride + " -> type=" + type + " stride=" + stride + " attr=" + attributeLocation);
	}
	var effectiveStride = clientArrayEffectiveStride(size, type, stride);
	if(effectiveStride <= 0)
	{
		warnOnce(clientArrayWarnings, "unknown-type-" + type, "Unsupported LWJGL client array type=" + type);
		return false;
	}
	var normalized = attributeLocation == colorLocation && isIntegerClientArrayType(type);
	var uploadBuf = buf;
	var uploadType = type;
	var uploadStride = stride;
	if(!isWebGLClientArrayType(type))
	{
		uploadBuf = convertDesktopClientArrayToFloat(buf, size, type, stride, count, normalized);
		if(uploadBuf == null)
		{
			warnOnce(clientArrayWarnings, "convert-failed-" + type, "Failed to convert LWJGL client array type=" + type);
			return false;
		}
		warnOnce(clientArrayWarnings, "converted-" + type, "Converted desktop LWJGL client array type=" + type + " to GL_FLOAT");
		uploadType = glCtx.FLOAT;
		uploadStride = 0;
		normalized = false;
	}
	glCtx.bindBuffer(glCtx.ARRAY_BUFFER, buffer);
	glCtx.bufferData(glCtx.ARRAY_BUFFER, uploadBuf, glCtx.STATIC_DRAW);
	glCtx.vertexAttribPointer(attributeLocation, size, uploadType, normalized, uploadStride, 0);
	immediatePointerLayoutDirty = true;
	if(strictWebGLValidation)
	{
		var attribErr = glCtx.getError();
		if(attribErr != glCtx.NO_ERROR)
		{
			warnOnce(clientArrayWarnings, "attrib-error-" + attributeLocation + "-" + uploadType + "-" + uploadStride,
				"LWJGL vertexAttribPointer error=" + attribErr + " attr=" + attributeLocation + " size=" + size + " type=" + uploadType + " stride=" + uploadStride);
			return false;
		}
	}
	glCtx.enableVertexAttribArray(attributeLocation);
	return true;
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
		var layout = normalizeLegacyClientArrayLayout(data.type, data.stride);
		var effectiveStride = clientArrayEffectiveStride(data.size, layout.type, layout.stride);
		var byteLength = clientArrayByteLength(data.size, layout.type, layout.stride, count);
		if(effectiveStride <= 0 || byteLength <= 0)
		{
			warnOnce(clientArrayWarnings, "bad-stride-" + data.type, "Unable to determine LWJGL client array stride type=" + data.type + " size=" + data.size);
			return;
		}
		var buf = data.buf;
		if(buf == null)
		{
			assert(v && data.pointer);
			buf = new Uint8Array(v.buffer, data.pointer, byteLength);
		}
		uploadDataImpl(buf, buffer, attributeLocation, data.size, layout.type, layout.stride, count);
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
		var layout = normalizeLegacyClientArrayLayout(data.type, data.stride);
		var byteLength = clientArrayByteLength(data.size, layout.type, layout.stride, count);
		if(byteLength <= 0) return ret;
		ret.type = layout.type;
		ret.stride = layout.stride;
		var buf = new Uint8Array(v.buffer, data.pointer, byteLength);
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
function ensureQuadIndexCapacity(vertexCount)
{
	if(vertexCount <= quadIndexVertexCapacity)
		return;
	// Grow geometrically so varying sprite batches do not reallocate the element
	// buffer every time a slightly larger batch appears.
	var capacity = quadIndexVertexCapacity > 0 ? quadIndexVertexCapacity : 256;
	while(capacity < vertexCount)
		capacity *= 2;
	capacity = Math.ceil(capacity / 4) * 4;
	var quadCount = capacity / 4;
	var indices = new Uint32Array(quadCount * 6);
	for(var q=0;q<quadCount;q++)
	{
		var v = q * 4;
		var i = q * 6;
		indices[i] = v;
		indices[i + 1] = v + 1;
		indices[i + 2] = v + 2;
		indices[i + 3] = v;
		indices[i + 4] = v + 2;
		indices[i + 5] = v + 3;
	}
	glCtx.bindBuffer(glCtx.ELEMENT_ARRAY_BUFFER, quadIndexBuffer);
	glCtx.bufferData(glCtx.ELEMENT_ARRAY_BUFFER, indices, glCtx.STATIC_DRAW);
	quadIndexVertexCapacity = capacity;
	presentationStats.quadIndexBufferUploads++;
}
function drawArraysImpl(mode, first, count)
{
	// TODO: Conditional
	glCtx.uniformMatrix4fv(mvLocation, false, modelViewMatrixStack[modelViewMatrixStack.length - 1]);
	glCtx.uniformMatrix4fv(projLocation, false, projMatrixStack[projMatrixStack.length - 1]);
	// Client-array upload/capture currently assumes first==0. Preserve that
	// established contract rather than pretending a non-zero base vertex is safe.
	assert(first == 0);
	if(detailedDrawTelemetryEnabled) presentationStats.legacyDrawCalls++;
	if(mode == 7/*QUADS*/ && (count % 4) == 0)
	{
		var quadCount = count / 4;
		if(quadCount <= 1)
		{
			// A single quad already costs one WebGL draw; avoid index-buffer work.
			glCtx.drawArrays(glCtx.TRIANGLE_FAN, 0, count);
			if(detailedDrawTelemetryEnabled) presentationStats.webglDrawCalls++;
		}
		else
		{
			ensureQuadIndexCapacity(count);
			glCtx.bindBuffer(glCtx.ELEMENT_ARRAY_BUFFER, quadIndexBuffer);
			glCtx.drawElements(glCtx.TRIANGLES, quadCount * 6, glCtx.UNSIGNED_INT, 0);
			if(detailedDrawTelemetryEnabled) presentationStats.webglDrawCalls++;
			if(detailedDrawTelemetryEnabled)
			{
				presentationStats.quadBatches++;
				presentationStats.quadQuads += quadCount;
				presentationStats.quadDrawCallsSaved += quadCount - 1;
			}
		}
	}
	else if(mode == 8/*QUAD_STRIP*/)
	{
		glCtx.drawArrays(glCtx.TRIANGLE_STRIP, first, count);
		if(detailedDrawTelemetryEnabled) presentationStats.webglDrawCalls++;
	}
	else if(mode == 9/*POLYGON*/)
	{
		glCtx.drawArrays(glCtx.TRIANGLE_FAN, first, count);
		if(detailedDrawTelemetryEnabled) presentationStats.webglDrawCalls++;
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
		if(detailedDrawTelemetryEnabled) presentationStats.webglDrawCalls++;
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
glCtx.uniform1f(pointSizeLocation, pointSizeState);
glCtx.uniform1f(texMaskLocation, 0);
syncAlphaTestUniforms();
var curList = null;
var listBase = 0;
var cmdLists = [null];
// The first null implicitly solves resetting on 0 id
var textureObjects = [null];
var textureGenerateMipmap = [false];
var boundTexture2DId = 0;
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
glCtx.renderbufferStorage(glCtx.RENDERBUFFER, glCtx.DEPTH24_STENCIL8, getCanvasWidth(), getCanvasHeight());
glCtx.framebufferRenderbuffer(glCtx.FRAMEBUFFER, glCtx.DEPTH_STENCIL_ATTACHMENT, glCtx.RENDERBUFFER, depthRb);
ensureFramebufferSize();
// Synthesize a focus event; LWJGL's Linux backend expects X11 focus state.
var eventQueue = [{type:"focus", time: performance.now()}];
// LWJGL_INPUT_QUERY_TELEMETRY_OPTIN_V1: polling getters are on the hot input path.
// Keep their diagnostic counters off in production; event/delivery counters stay live.
var inputQueryTelemetryEnabled = typeof window !== "undefined" && window.__LWJGL_INPUT_QUERY_TELEMETRY__ === true;
var inputStats = {
	domEvents: 0,
	deliveredEvents: 0,
	droppedEvents: 0,
	coalescedMoves: 0,
	queueHighWater: eventQueue.length,
	byType: {}
};
if(typeof window !== "undefined") window.__lwjglInputStats = inputStats;

// LWJGL_DIRECT_INPUT_BRIDGE_V1
var keyboardInputState = {
	down: new Uint8Array(256),
	queue: [],
	current: null,
	repeatEvents: false
};
var mouseInputState = {
	x: 0,
	y: 0,
	dx: 0,
	dy: 0,
	wheel: 0,
	buttons: new Uint8Array(8),
	queue: [],
	current: null,
	inside: true,
	grabbed: false,
	lastX: null,
	lastY: null
};
inputStats.directKeyboardDelivered = 0;
inputStats.directMouseDelivered = 0;
inputStats.keyboardStateQueries = 0;
inputStats.keyboardPressedQueries = 0;
inputStats.mouseButtonQueries = 0;
inputStats.mousePressedQueries = 0;
inputStats.mousePositionQueries = 0;
inputStats.keyboardQueueHighWater = 0;
inputStats.mouseQueueHighWater = 0;
inputStats.keyboardGlobalCaptures = 0;
inputStats.inputQueryTelemetryActive = inputQueryTelemetryEnabled;

function inputEventNanos()
{
	return Math.floor(performance.now() * 1000000);
}
function enqueueKeyboardDirect(event)
{
	if(event.repeat && !keyboardInputState.repeatEvents) return;
	if(keyboardInputState.queue.length >= 256)
	{
		keyboardInputState.queue.shift();
		inputStats.droppedEvents++;
	}
	keyboardInputState.queue.push(event);
	inputStats.keyboardQueueHighWater = Math.max(inputStats.keyboardQueueHighWater, keyboardInputState.queue.length);
}
function enqueueMouseDirect(event, coalesceMove)
{
	var queue = mouseInputState.queue;
	if(coalesceMove && queue.length && queue[queue.length - 1].button < 0 && queue[queue.length - 1].wheel === 0)
	{
		queue[queue.length - 1] = event;
		inputStats.coalescedMoves++;
		return;
	}
	if(queue.length >= 256)
	{
		var staleMove = queue.findIndex(e => e.button < 0 && e.wheel === 0);
		if(staleMove >= 0) queue.splice(staleMove, 1);
		else queue.shift();
		inputStats.droppedEvents++;
	}
	queue.push(event);
	inputStats.mouseQueueHighWater = Math.max(inputStats.mouseQueueHighWater, queue.length);
}

// A canvas is not keyboard-focusable by default. Desktop LWJGL always owns a
// focused native window, so make that contract explicit in the browser.
glCanvas.tabIndex = 0;
glCanvas.style.outline = "none";
function focusGameCanvas()
{
	try { glCanvas.focus({preventScroll: true}); }
	catch(_) { try { glCanvas.focus(); } catch(__) {} }
}

function enqueueInputEvent(event, coalesceMove)
{
	inputStats.domEvents++;
	inputStats.byType[event.type] = (inputStats.byType[event.type] || 0) + 1;
	if(coalesceMove && eventQueue.length && eventQueue[eventQueue.length - 1].type === event.type)
	{
		eventQueue[eventQueue.length - 1] = event;
		inputStats.coalescedMoves++;
		return;
	}
	// Do not let pointer motion outrun the emulated X11 consumer and add seconds
	// of input latency. Prefer discarding stale motion over button/key events.
	if(eventQueue.length >= 512)
	{
		var staleMove = eventQueue.findIndex(e => e.type === "mousemove");
		if(staleMove >= 0) eventQueue.splice(staleMove, 1);
		else eventQueue.shift();
		inputStats.droppedEvents++;
	}
	eventQueue.push(event);
	inputStats.queueHighWater = Math.max(inputStats.queueHighWater, eventQueue.length);
}

function convertMousePos(clientX, clientY) {
	const fbWidthNow = getCanvasWidth();
	const fbHeightNow = getCanvasHeight();
	const rect = glCanvas.getBoundingClientRect();
	const clientWidth = Math.max(1, rect.width || glCanvas.clientWidth || fbWidthNow);
	const clientHeight = Math.max(1, rect.height || glCanvas.clientHeight || fbHeightNow);
	return [
		(clientX - rect.left) * fbWidthNow / clientWidth,
		(clientY - rect.top) * fbHeightNow / clientHeight
	];
}

/** Convert from MouseEvent.button to X11 mouse button. */
function convertMouseButton(button) {
	if(button === 0) return 1;
	if(button === 1) return 2;
	if(button === 2) return 3;
	return button + 1;
}

/** If non-null, the game wants relative/pointer-locked mouse input. */
let lockedMousePos = null;

function requestGamePointerLock()
{
	if(!lockedMousePos || document.pointerLockElement === glCanvas) return;
	try {
		const result = glCanvas.requestPointerLock();
		if(result && typeof result.catch === "function") result.catch(() => {});
	} catch(_) {}
}

glCanvas.addEventListener("mousemove", evt => {
	let [x, y] = convertMousePos(evt.clientX, evt.clientY);
	if (lockedMousePos) {
		const rect = glCanvas.getBoundingClientRect();
		const xScale = getCanvasWidth() / Math.max(1, rect.width || glCanvas.clientWidth || getCanvasWidth());
		const yScale = getCanvasHeight() / Math.max(1, rect.height || glCanvas.clientHeight || getCanvasHeight());
		x = lockedMousePos.x = Math.max(0, Math.min(getCanvasWidth() - 1, lockedMousePos.x + evt.movementX * xScale));
		y = lockedMousePos.y = Math.max(0, Math.min(getCanvasHeight() - 1, lockedMousePos.y + evt.movementY * yScale));
	}
	var directX = Math.round(x);
	var directY = Math.round(getCanvasHeight() - 1 - y);
	var directDx = mouseInputState.lastX == null ? 0 : directX - mouseInputState.lastX;
	var directDy = mouseInputState.lastY == null ? 0 : directY - mouseInputState.lastY;
	mouseInputState.lastX = mouseInputState.x = directX;
	mouseInputState.lastY = mouseInputState.y = directY;
	mouseInputState.dx += directDx;
	mouseInputState.dy += directDy;
	enqueueMouseDirect({button:-1, state:false, dx:directDx, dy:directDy, x:directX, y:directY, wheel:0, nanos:inputEventNanos()}, true);
	enqueueInputEvent({ type: "mousemove", x, y, time: performance.now() }, true);
});
function mouseHandler(evt) {
	focusGameCanvas();
	requestGamePointerLock();
	const [x, y] = convertMousePos(evt.clientX, evt.clientY);
	const directX = Math.round(x);
	const directY = Math.round(getCanvasHeight() - 1 - y);
	const directButton = evt.button === 0 ? 0 : (evt.button === 2 ? 1 : (evt.button === 1 ? 2 : evt.button));
	const down = evt.type === "mousedown";
	if(directButton >= 0 && directButton < mouseInputState.buttons.length) mouseInputState.buttons[directButton] = down ? 1 : 0;
	mouseInputState.x = mouseInputState.lastX = directX;
	mouseInputState.y = mouseInputState.lastY = directY;
	enqueueMouseDirect({button:directButton, state:down, dx:0, dy:0, x:directX, y:directY, wheel:0, nanos:inputEventNanos()}, false);
	enqueueInputEvent({ type: evt.type, x, y, button: convertMouseButton(evt.button), time: performance.now() }, false);
	if(evt.type === "mousedown") evt.preventDefault();
}
glCanvas.addEventListener("mousedown", mouseHandler);
glCanvas.addEventListener("mouseup", mouseHandler);
glCanvas.addEventListener("contextmenu", evt => evt.preventDefault());
glCanvas.addEventListener("wheel", evt => {
	focusGameCanvas();
	const [x, y] = convertMousePos(evt.clientX, evt.clientY);
	const button = evt.deltaY < 0 ? 4 : 5;
	const wheel = evt.deltaY < 0 ? 120 : -120;
	const time = performance.now();
	const directX = Math.round(x);
	const directY = Math.round(getCanvasHeight() - 1 - y);
	mouseInputState.wheel += wheel;
	enqueueMouseDirect({button:-1, state:false, dx:0, dy:0, x:directX, y:directY, wheel, nanos:inputEventNanos()}, false);
	enqueueInputEvent({type:"mousedown", x, y, button, time}, false);
	enqueueInputEvent({type:"mouseup", x, y, button, time: time + 0.01}, false);
	evt.preventDefault();
}, {passive:false});
glCanvas.addEventListener("focus", () => enqueueInputEvent({type:"focus", time:performance.now()}, false));
glCanvas.addEventListener("blur", () => enqueueInputEvent({type:"blur", time:performance.now()}, false));
glCanvas.addEventListener("mouseenter", () => { mouseInputState.inside = true; });
glCanvas.addEventListener("mouseleave", () => { mouseInputState.inside = false; });

const x11KeySyms = {
	Escape: 0xff1b, Enter: 0xff0d, Tab: 0xff09, Backspace: 0xff08,
	Insert: 0xff63, Delete: 0xffff, Home: 0xff50, End: 0xff57,
	PageUp: 0xff55, PageDown: 0xff56,
	ArrowLeft: 0xff51, ArrowUp: 0xff52, ArrowRight: 0xff53, ArrowDown: 0xff54,
	Shift: 0xffe1, Control: 0xffe3, Alt: 0xffe9, Meta: 0xffe7,
	CapsLock: 0xffe5, NumLock: 0xff7f, ScrollLock: 0xff14, Pause: 0xff13,
	F1: 0xffbe, F2: 0xffbf, F3: 0xffc0, F4: 0xffc1, F5: 0xffc2, F6: 0xffc3,
	F7: 0xffc4, F8: 0xffc5, F9: 0xffc6, F10: 0xffc7, F11: 0xffc8, F12: 0xffc9
};
const lwjglKeyByCode = {
	Escape:1, Digit1:2, Digit2:3, Digit3:4, Digit4:5, Digit5:6, Digit6:7, Digit7:8, Digit8:9, Digit9:10, Digit0:11,
	Minus:12, Equal:13, Backspace:14, Tab:15,
	KeyQ:16, KeyW:17, KeyE:18, KeyR:19, KeyT:20, KeyY:21, KeyU:22, KeyI:23, KeyO:24, KeyP:25,
	BracketLeft:26, BracketRight:27, Enter:28, ControlLeft:29,
	KeyA:30, KeyS:31, KeyD:32, KeyF:33, KeyG:34, KeyH:35, KeyJ:36, KeyK:37, KeyL:38,
	Semicolon:39, Quote:40, Backquote:41, ShiftLeft:42, Backslash:43,
	KeyZ:44, KeyX:45, KeyC:46, KeyV:47, KeyB:48, KeyN:49, KeyM:50,
	Comma:51, Period:52, Slash:53, ShiftRight:54, NumpadMultiply:55, AltLeft:56, Space:57, CapsLock:58,
	F1:59, F2:60, F3:61, F4:62, F5:63, F6:64, F7:65, F8:66, F9:67, F10:68,
	NumLock:69, ScrollLock:70, Numpad7:71, Numpad8:72, Numpad9:73, NumpadSubtract:74,
	Numpad4:75, Numpad5:76, Numpad6:77, NumpadAdd:78, Numpad1:79, Numpad2:80, Numpad3:81,
	Numpad0:82, NumpadDecimal:83, F11:87, F12:88, F13:100, F14:101, F15:102,
	NumpadEnter:156, ControlRight:157, NumpadDivide:181, AltRight:184,
	Home:199, ArrowUp:200, PageUp:201, ArrowLeft:203, ArrowRight:205,
	End:207, ArrowDown:208, PageDown:209, Insert:210, Delete:211,
	MetaLeft:219, MetaRight:220, ContextMenu:221, Pause:197
};
function lwjglKeyForEvent(e)
{
	return lwjglKeyByCode[e.code] || 0;
}
function keySymForEvent(e)
{
	if(x11KeySyms[e.key] !== undefined) return x11KeySyms[e.key];
	if(e.code && /^Key[A-Z]$/.test(e.code)) return e.code.charCodeAt(3) + 32; // X11 lowercase letter keysym
	if(e.code && /^Digit[0-9]$/.test(e.code)) return e.code.charCodeAt(5);
	if(typeof e.key === "string" && e.key.length === 1) return e.key.codePointAt(0);
	return e.keyCode || 0;
}
function modifierMaskForEvent(e)
{
	// X11 ShiftMask=1, ControlMask=4, Mod1Mask(Alt)=8, Mod4Mask(Meta)=64.
	return (e.shiftKey ? 1 : 0) | (e.ctrlKey ? 4 : 0) | (e.altKey ? 8 : 0) | (e.metaKey ? 64 : 0);
}
function shouldCaptureGameKeyboard(e)
{
	if(!glCanvas || !glCanvas.isConnected) return false;
	if(document.activeElement === glCanvas || document.pointerLockElement === glCanvas) return true;
	const active = document.activeElement;
	if(active)
	{
		const tag = String(active.tagName || "").toUpperCase();
		if(tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || active.isContentEditable) return false;
	}
	// Desktop LWJGL owns the native keyboard window even when an in-game panel
	// changes focus. Once Starsector has started, preserve that contract instead
	// of silently dropping shortcuts until the player clicks the canvas again.
	const runtimeState = document.body && document.body.dataset ? String(document.body.dataset.runtimeState || "") : "";
	const gameStarted = !!window.__STARSECTOR_STARTING__ || runtimeState === "running" || runtimeState === "campaign";
	return gameStarted && glCanvas.getClientRects().length > 0;
}
/** @param {KeyboardEvent} e */
function keyHandler(e)
{
	if(!shouldCaptureGameKeyboard(e)) return;
	if(document.activeElement !== glCanvas && document.pointerLockElement !== glCanvas)
	{
		inputStats.keyboardGlobalCaptures++;
	}
	const keySym = keySymForEvent(e);
	const lwjglKey = lwjglKeyForEvent(e);
	const charCode = typeof e.key === "string" && e.key.length === 1 ? e.key.codePointAt(0) : 0;
	const down = e.type === "keydown";
	if(lwjglKey > 0 && lwjglKey < keyboardInputState.down.length) keyboardInputState.down[lwjglKey] = down ? 1 : 0;
	enqueueKeyboardDirect({key:lwjglKey, state:down, charCode, nanos:inputEventNanos(), repeat:!!e.repeat});
	enqueueInputEvent({
		type: e.type,
		keySym,
		keyCode: keySym,
		keyState: modifierMaskForEvent(e),
		charCode,
		time: performance.now(),
		repeat: !!e.repeat
	}, false);
	e.preventDefault();
}
window.addEventListener("keydown", keyHandler, true);
window.addEventListener("keyup", keyHandler, true);

// Direct org.lwjgl.input.Keyboard bridge.
function Java_org_lwjgl_input_Keyboard_nReset()
{
	keyboardInputState.down.fill(0);
	keyboardInputState.queue.length = 0;
	keyboardInputState.current = null;
}
function Java_org_lwjgl_input_Keyboard_nPoll() {}
function Java_org_lwjgl_input_Keyboard_nIsKeyDown(lib, key)
{
	var down = key >= 0 && key < keyboardInputState.down.length && keyboardInputState.down[key] !== 0;
	return down;
}
function Java_org_lwjgl_input_Keyboard_nNext()
{
	keyboardInputState.current = keyboardInputState.queue.shift() || null;
	if(keyboardInputState.current) inputStats.directKeyboardDelivered++;
	return keyboardInputState.current != null;
}
function Java_org_lwjgl_input_Keyboard_nGetEventKey() { return keyboardInputState.current ? keyboardInputState.current.key : 0; }
function Java_org_lwjgl_input_Keyboard_nGetEventKeyState() { return !!(keyboardInputState.current && keyboardInputState.current.state); }
function Java_org_lwjgl_input_Keyboard_nGetEventCharacter() { return keyboardInputState.current ? keyboardInputState.current.charCode : 0; }
function Java_org_lwjgl_input_Keyboard_nGetEventNanoseconds() { return keyboardInputState.current ? keyboardInputState.current.nanos : inputEventNanos(); }
function Java_org_lwjgl_input_Keyboard_nIsRepeatEvent() { return !!(keyboardInputState.current && keyboardInputState.current.repeat); }
function Java_org_lwjgl_input_Keyboard_nSetRepeatEvents(lib, enabled) { keyboardInputState.repeatEvents = !!enabled; }

// Direct org.lwjgl.input.Mouse bridge.
function Java_org_lwjgl_input_Mouse_nReset()
{
	mouseInputState.dx = mouseInputState.dy = mouseInputState.wheel = 0;
	mouseInputState.buttons.fill(0);
	mouseInputState.queue.length = 0;
	mouseInputState.current = null;
	mouseInputState.lastX = mouseInputState.lastY = null;
}
function Java_org_lwjgl_input_Mouse_nPoll() {}
function Java_org_lwjgl_input_Mouse_nIsButtonDown(lib, button)
{
	var down = button >= 0 && button < mouseInputState.buttons.length && mouseInputState.buttons[button] !== 0;
	return down;
}
function Java_org_lwjgl_input_Mouse_nNext()
{
	mouseInputState.current = mouseInputState.queue.shift() || null;
	if(mouseInputState.current) inputStats.directMouseDelivered++;
	return mouseInputState.current != null;
}
function Java_org_lwjgl_input_Mouse_nGetEventButton() { return mouseInputState.current ? mouseInputState.current.button : -1; }
function Java_org_lwjgl_input_Mouse_nGetEventButtonState() { return !!(mouseInputState.current && mouseInputState.current.state); }
function Java_org_lwjgl_input_Mouse_nGetEventDX() { return mouseInputState.current ? mouseInputState.current.dx : 0; }
function Java_org_lwjgl_input_Mouse_nGetEventDY() { return mouseInputState.current ? mouseInputState.current.dy : 0; }
function Java_org_lwjgl_input_Mouse_nGetEventX() { return mouseInputState.current ? mouseInputState.current.x : mouseInputState.x; }
function Java_org_lwjgl_input_Mouse_nGetEventY() { return mouseInputState.current ? mouseInputState.current.y : mouseInputState.y; }
function Java_org_lwjgl_input_Mouse_nGetEventDWheel() { return mouseInputState.current ? mouseInputState.current.wheel : 0; }
function Java_org_lwjgl_input_Mouse_nGetEventNanoseconds() { return mouseInputState.current ? mouseInputState.current.nanos : inputEventNanos(); }
function Java_org_lwjgl_input_Mouse_nGetX() { return Math.round(mouseInputState.x); }
function Java_org_lwjgl_input_Mouse_nGetY() { return Math.round(mouseInputState.y); }
function Java_org_lwjgl_input_Mouse_nGetDX() { var value=Math.round(mouseInputState.dx); mouseInputState.dx=0; return value; }
function Java_org_lwjgl_input_Mouse_nGetDY() { var value=Math.round(mouseInputState.dy); mouseInputState.dy=0; return value; }
function Java_org_lwjgl_input_Mouse_nGetDWheel() { var value=Math.round(mouseInputState.wheel); mouseInputState.wheel=0; return value; }
// LWJGL_INPUT_QUERY_TELEMETRY_OVERRIDES_BEGIN
// Install diagnostic wrappers only when explicitly requested so production polling
// executes the original branch-free bridge functions.
if(inputQueryTelemetryEnabled)
{
	var rawKeyboardIsKeyDown = Java_org_lwjgl_input_Keyboard_nIsKeyDown;
	Java_org_lwjgl_input_Keyboard_nIsKeyDown = function(lib, key)
	{
		inputStats.keyboardStateQueries++;
		var down = rawKeyboardIsKeyDown(lib, key);
		if(down) inputStats.keyboardPressedQueries++;
		return down;
	};
	var rawMouseIsButtonDown = Java_org_lwjgl_input_Mouse_nIsButtonDown;
	Java_org_lwjgl_input_Mouse_nIsButtonDown = function(lib, button)
	{
		inputStats.mouseButtonQueries++;
		var down = rawMouseIsButtonDown(lib, button);
		if(down) inputStats.mousePressedQueries++;
		return down;
	};
	var rawMouseGetX = Java_org_lwjgl_input_Mouse_nGetX;
	Java_org_lwjgl_input_Mouse_nGetX = function() { inputStats.mousePositionQueries++; return rawMouseGetX(); };
	var rawMouseGetY = Java_org_lwjgl_input_Mouse_nGetY;
	Java_org_lwjgl_input_Mouse_nGetY = function() { inputStats.mousePositionQueries++; return rawMouseGetY(); };
}
// LWJGL_INPUT_QUERY_TELEMETRY_OVERRIDES_END
function Java_org_lwjgl_input_Mouse_nSetGrabbed(lib, grabbed)
{
	mouseInputState.grabbed = !!grabbed;
	if(grabbed)
	{
		lockedMousePos = {x:getCanvasWidth()/2, y:getCanvasHeight()/2};
		requestGamePointerLock();
	}
	else
	{
		lockedMousePos = null;
		try { if(document.pointerLockElement === glCanvas) document.exitPointerLock(); } catch(_) {}
	}
}
function Java_org_lwjgl_input_Mouse_nSetCursorPosition(lib, x, y)
{
	mouseInputState.x = mouseInputState.lastX = Number(x) || 0;
	mouseInputState.y = mouseInputState.lastY = Number(y) || 0;
	if(lockedMousePos) lockedMousePos = {x:mouseInputState.x, y:getCanvasHeight()-1-mouseInputState.y};
}
function Java_org_lwjgl_input_Mouse_nIsInsideWindow() { return mouseInputState.inside; }

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

// LWJGL_INTEGER_PIXEL_STORE_COMPAT_V1
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
		return;
	}
	try
	{
		var value = glCtx.getParameter(id);
		if(typeof value === "number")
		{
			buf[0] = value | 0;
			return;
		}
		if(typeof value === "boolean")
		{
			buf[0] = value ? 1 : 0;
			return;
		}
		if(value != null && typeof value.length === "number")
		{
			var n = Math.min(4, value.length);
			for(var i=0;i<n;i++) buf[i] = Number(value[i]) | 0;
			return;
		}
	}
	catch(err)
	{
		warnOnce(attribStateWarnings, "get-integer-" + id,
			"LWJGL glGetIntegerv unsupported pname=" + id + " error=" + err);
		return;
	}
	if(verboseLog) console.log("glGetInteger", id);
}

function Java_org_lwjgl_opengl_GL11_nglPixelStorei(lib, pname, param, funcPtr)
{
	checkNoList(curList);
	try
	{
		glCtx.pixelStorei(pname, param);
	}
	catch(err)
	{
		warnOnce(attribStateWarnings, "pixel-store-" + pname,
			"LWJGL glPixelStorei unsupported pname=" + pname + " param=" + param + " error=" + err);
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
	var swapNow = performance.now();
	recentSwapTimes.push(swapNow);
	if(recentSwapTimes.length > 121) recentSwapTimes.shift();
	if(recentSwapTimes.length >= 2)
	{
		var recentDuration = recentSwapTimes[recentSwapTimes.length - 1] - recentSwapTimes[0];
		presentationStats.recentFps = recentDuration > 0 ? (recentSwapTimes.length - 1) * 1000 / recentDuration : 0;
		presentationStats.recentFrameMs = recentDuration > 0 ? recentDuration / (recentSwapTimes.length - 1) : 0;
	}
	if(presentationReadbackDiagnostics && presentationStats.samples.length < 8 && (presentationStats.swapCount == 1 || (presentationStats.swapCount % 300) == 0))
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
	else if(a == 0x0BC0/*GL_ALPHA_TEST*/)
	{
		alphaTestState.enabled = false;
		syncAlphaTestUniforms();
	}
	else if(a == glCtx.TEXTURE_2D || a == 0x806F/*GL_TEXTURE_3D*/)
	{
		setTexture2DEnabled(false);
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
	else if(a == 0x0BC0/*GL_ALPHA_TEST*/)
	{
		alphaTestState.enabled = true;
		syncAlphaTestUniforms();
		warnOnce(alphaTestWarnings, "enabled", "LWJGL alpha-test compatibility enabled.");
	}
	else if(a == glCtx.TEXTURE_2D || a == 0x806F/*GL_TEXTURE_3D*/)
	{
		setTexture2DEnabled(true);
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
		textureGenerateMipmap[id] = false;
	}
}

function Java_org_lwjgl_opengl_GL11_nglBindTexture(lib, target, id, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglBindTexture);
	assert(target == glCtx.TEXTURE_2D);
	boundTexture2DId = id;
	glCtx.bindTexture(target, textureObjects[id]);
}

// LWJGL_GENERATE_MIPMAP_COMPAT_V1
function Java_org_lwjgl_opengl_GL11_nglTexParameteri(lib, target, pname, param, funcPtr)
{
	checkNoList(curList);
	if(pname == 0x8191/*GL_GENERATE_MIPMAP*/)
	{
		textureGenerateMipmap[boundTexture2DId] = !!param;
		return;
	}
	if((pname == glCtx.TEXTURE_WRAP_S || pname == glCtx.TEXTURE_WRAP_T) && param == 0x2900/*GL_CLAMP*/)
		param = glCtx.CLAMP_TO_EDGE;
	glCtx.texParameteri(target, pname, param);
}

function Java_org_lwjgl_opengl_GL11_nglTexImage2D(lib, target, level, internalFormat, width, height, border, format, type, memPtr, funcPtr)
{
	checkNoList(curList);
	assert(target == glCtx.TEXTURE_2D);
	var v = lib.getJNIDataView();
	var upload = normalizeTextureUpload(v, memPtr, width, height, internalFormat, format, type);
	glCtx.texImage2D(target, level, upload.internalFormat, width, height, border, upload.format, upload.type, upload.data);
	if(level == 0 && textureGenerateMipmap[boundTexture2DId])
		glCtx.generateMipmap(target);
	if(strictWebGLValidation)
	{
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
	if(immediateBeginActive && immediateInterleavedEnabled)
	{
		if(!presentationStats.immediateColorAttribDeferredObserved) presentationStats.immediateColorAttribDeferredObserved = true;
	}
	else applyCurrentColorAttrib();
}

// LWJGL_ALPHA_TEST_COMPAT_V1
function Java_org_lwjgl_opengl_GL11_nglAlphaFunc(lib, func, ref, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglAlphaFunc);
	if(func < 0x0200/*GL_NEVER*/ || func > 0x0207/*GL_ALWAYS*/)
	{
		warnOnce(alphaTestWarnings, "invalid-func-" + func, "Unsupported LWJGL alpha-test func=" + func);
		return;
	}
	alphaTestState.func = func;
	alphaTestState.ref = Math.max(0, Math.min(1, ref));
	syncAlphaTestUniforms();
	warnOnce(alphaTestWarnings, "configured-" + func + "-" + alphaTestState.ref,
		"LWJGL alpha-test configured func=" + func + " ref=" + alphaTestState.ref);
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
	if(immediateBeginActive && immediateInterleavedEnabled)
	{
		if(!presentationStats.immediateColorAttribDeferredObserved) presentationStats.immediateColorAttribDeferredObserved = true;
	}
	else applyCurrentColorAttrib();
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

// LWJGL_ATTRIB_STACK_COMPAT_V1
function Java_org_lwjgl_opengl_GL11_nglIsEnabled(lib, cap, funcPtr)
{
	return getCompatEnableState(cap);
}
function Java_org_lwjgl_opengl_GL11_nglPushAttrib(lib, mask, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPushAttrib);
	attribStateStack.push(snapshotAttribState(mask));
}
function Java_org_lwjgl_opengl_GL11_nglPopAttrib(lib, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPopAttrib);
	if(attribStateStack.length == 0)
	{
		warnOnce(attribStateWarnings, 'underflow', 'LWJGL glPopAttrib ignored empty attribute stack.');
		return;
	}
	restoreAttribState(attribStateStack.pop());
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

function Java_org_lwjgl_opengl_GL11_nglCopyTexImage2D(lib, target, level, internalFormat, x, y, width, height, border, funcPtr)
{
	checkNoList(curList);
	assert(target == glCtx.TEXTURE_2D);
	glCtx.copyTexImage2D(target, level, internalFormat, x, y, width, height, border);
	if(level == 0 && textureGenerateMipmap[boundTexture2DId])
		glCtx.generateMipmap(target);
}

function Java_org_lwjgl_opengl_GL11_nglCopyTexSubImage2D(lib, target, level, xoffset, yoffset, x, y, width, height, funcPtr)
{
	checkNoList(curList);
	glCtx.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
	if(level == 0 && textureGenerateMipmap[boundTexture2DId])
		glCtx.generateMipmap(target);
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
	if(level == 0 && textureGenerateMipmap[boundTexture2DId])
		glCtx.generateMipmap(target);
	if(strictWebGLValidation)
	{
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

// LWJGL_RASTER_STATE_COMPAT_V1
function Java_org_lwjgl_opengl_GL11_nglScissor(lib, x, y, width, height, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglScissor);
	glCtx.scissor(x, y, Math.max(0, width), Math.max(0, height));
}
function Java_org_lwjgl_opengl_GL11_nglStencilFunc(lib, func, ref, mask, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglStencilFunc);
	glCtx.stencilFunc(func, ref, mask >>> 0);
}
function Java_org_lwjgl_opengl_GL11_nglStencilOp(lib, sfail, dpfail, dppass, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglStencilOp);
	glCtx.stencilOp(sfail, dpfail, dppass);
}

// LWJGL_POINT_SIZE_COMPAT_V1
function Java_org_lwjgl_opengl_GL11_nglPointSize(lib, size, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPointSize);
	pointSizeState = Math.max(1, Number(size) || 1);
	glCtx.uniform1f(pointSizeLocation, pointSizeState);
}

function Java_org_lwjgl_opengl_GL11_nglLineWidth(lib, width, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglLineWidth);
	try { glCtx.lineWidth(width); } catch(_) {}
}

function Java_org_lwjgl_opengl_GL11_nglPolygonOffset(lib, factor, units, funcPtr)
{
	if(curList) return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPolygonOffset);
	glCtx.polygonOffset(factor, units);
}

function Java_org_lwjgl_opengl_GL11_nglBegin(lib, mode, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglBegin);
	immediateModeData.mode = mode;
	immediateModeData.vertexPos = 0;
	immediateModeData.colorPos = 0;
	immediateModeData.texCoordPos = 0;
	immediateModeData.interleavedPos = 0;
	immediateBeginActive = true;
	// Preserve the existing bridge begin-local texcoord semantics: a vertex
	// without an explicit texcoord in a new begin/end block starts at (0, 0).
	immediateModeData.currentTexCoord[0] = 0;
	immediateModeData.currentTexCoord[1] = 0;
}

function Java_org_lwjgl_opengl_GL11_nglTexCoord2f(lib, x, y, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglTexCoord2f);
	immediateModeData.currentTexCoord[0] = x;
	immediateModeData.currentTexCoord[1] = y;
	if(immediateInterleavedEnabled) return;
	var curPos = immediateModeData.texCoordPos;
	immediateModeData.texCoordBuf =
		ensureImmediateArrayCapacity(immediateModeData.texCoordBuf, curPos + 2);
	immediateModeData.texCoordBuf[curPos] = x;
	immediateModeData.texCoordBuf[curPos + 1] = y;
	immediateModeData.texCoordPos = curPos + 2;
}

// LWJGL_IMMEDIATE_VERTEX_BATCH_V1
function appendImmediateVertex(x, y, z, texS, texT)
{
	if(immediateInterleavedEnabled)
	{
		var pos = immediateModeData.interleavedPos;
		immediateModeData.interleavedBuf = ensureImmediateArrayCapacity(immediateModeData.interleavedBuf, pos + 9);
		var out = immediateModeData.interleavedBuf;
		out[pos] = x; out[pos + 1] = y; out[pos + 2] = z;
		out[pos + 3] = immediateModeData.currentColor[0];
		out[pos + 4] = immediateModeData.currentColor[1];
		out[pos + 5] = immediateModeData.currentColor[2];
		out[pos + 6] = immediateModeData.currentColor[3];
		out[pos + 7] = texS; out[pos + 8] = texT;
		immediateModeData.interleavedPos = pos + 9;
		immediateModeData.vertexPos += 3;
		immediateModeData.colorPos += 4;
		immediateModeData.texCoordPos += 2;
		immediateModeData.currentTexCoord[0] = texS;
		immediateModeData.currentTexCoord[1] = texT;
		return;
	}
	var curPos = immediateModeData.vertexPos;
	immediateModeData.vertexBuf = ensureImmediateArrayCapacity(immediateModeData.vertexBuf, curPos + 3);
	immediateModeData.vertexBuf[curPos] = x;
	immediateModeData.vertexBuf[curPos + 1] = y;
	immediateModeData.vertexBuf[curPos + 2] = z;
	immediateModeData.vertexPos = curPos + 3;
	var texPos = immediateModeData.texCoordPos;
	immediateModeData.texCoordBuf = ensureImmediateArrayCapacity(immediateModeData.texCoordBuf, texPos + 2);
	immediateModeData.texCoordBuf[texPos] = texS;
	immediateModeData.texCoordBuf[texPos + 1] = texT;
	immediateModeData.texCoordPos = texPos + 2;
	var colorPos = immediateModeData.colorPos;
	immediateModeData.colorBuf = ensureImmediateArrayCapacity(immediateModeData.colorBuf, colorPos + 4);
	immediateModeData.colorBuf[colorPos] = immediateModeData.currentColor[0];
	immediateModeData.colorBuf[colorPos + 1] = immediateModeData.currentColor[1];
	immediateModeData.colorBuf[colorPos + 2] = immediateModeData.currentColor[2];
	immediateModeData.colorBuf[colorPos + 3] = immediateModeData.currentColor[3];
	immediateModeData.colorPos = colorPos + 4;
}
function Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(lib, x, y, z, texS, texT, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord);
	appendImmediateVertex(x, y, z, texS, texT);
}

function Java_org_lwjgl_opengl_GL11_nglVertex3f(lib, x, y, z, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglVertex3f);
	var texPos = immediateModeData.texCoordPos;
	var texS = immediateInterleavedEnabled ? immediateModeData.currentTexCoord[0] : (texPos >= 2 ? immediateModeData.texCoordBuf[texPos - 2] : 0);
	var texT = immediateInterleavedEnabled ? immediateModeData.currentTexCoord[1] : (texPos >= 2 ? immediateModeData.texCoordBuf[texPos - 1] : 0);
	appendImmediateVertex(x, y, z, texS, texT);
}

function uploadImmediateInterleaved(vertexCount)
{
	var floatCount = vertexCount * 9;
	var data = immediateModeData.interleavedBuf.subarray(0, floatCount);
	var stride = 9 * 4;
	glCtx.bindBuffer(glCtx.ARRAY_BUFFER, vertexBuffer);
	glCtx.bufferData(glCtx.ARRAY_BUFFER, data, glCtx.STATIC_DRAW);
	if(immediatePointerLayoutDirty)
	{
		glCtx.vertexAttribPointer(vertexPosition, 3, glCtx.FLOAT, false, stride, 0);
		glCtx.vertexAttribPointer(colorLocation, 4, glCtx.FLOAT, false, stride, 3 * 4);
		glCtx.vertexAttribPointer(texCoord, 2, glCtx.FLOAT, false, stride, 7 * 4);
		immediatePointerLayoutDirty = false;
		presentationStats.immediatePointerLayoutRefreshes++;
	}
	glCtx.enableVertexAttribArray(vertexPosition);
	glCtx.enableVertexAttribArray(colorLocation);
	glCtx.enableVertexAttribArray(texCoord);
	if(strictWebGLValidation)
	{
		var attribErr = glCtx.getError();
		if(attribErr != glCtx.NO_ERROR)
			warnOnce(clientArrayWarnings, "immediate-interleaved-attrib-error-" + attribErr,
				"LWJGL interleaved immediate vertexAttribPointer error=" + attribErr);
	}
	if(detailedDrawTelemetryEnabled)
	{
		presentationStats.immediateInterleavedDraws++;
		presentationStats.immediateInterleavedUploads++;
		presentationStats.immediateInterleavedUploadsSaved += 2;
		presentationStats.immediateInterleavedBytes += floatCount * 4;
	}
}
function Java_org_lwjgl_opengl_GL11_nglEnd(lib, funcPtr)
{
	if(curList)
		return pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglEnd);
	immediateBeginActive = false;
	var vertexCount = immediateModeData.vertexPos / 3;
	if(immediateInterleavedEnabled)
	{
		uploadImmediateInterleaved(vertexCount);
	}
	else
	{
		// Exact legacy fallback: three independent position/color/texcoord uploads.
		uploadDataImpl(immediateModeData.vertexBuf.subarray(0, immediateModeData.vertexPos), vertexBuffer, vertexPosition, 3, glCtx.FLOAT, 3 * 4);
		uploadDataImpl(immediateModeData.colorBuf.subarray(0, vertexCount * 4), colorBuffer, colorLocation, 4, glCtx.FLOAT, 4 * 4);
		if(immediateModeData.texCoordPos >= vertexCount * 2)
			uploadDataImpl(immediateModeData.texCoordBuf.subarray(0, vertexCount * 2), texCoordBuffer, texCoord, 2, glCtx.FLOAT, 2 * 4);
		else
		{
			glCtx.disableVertexAttribArray(texCoord);
			glCtx.vertexAttrib2f(texCoord, 0, 0);
		}
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
	var bufferAddr = Number(await buffer.address());
	var v = lib.getJNIDataView();
	var e = eventQueue.shift();
	if(!e) return;
	inputStats.deliveredEvents++;
	const writeI32 = (off, value) => v.setInt32(bufferAddr + off, Number(value || 0), true);
	writeI32(16, Math.floor(e.time || performance.now()));
	switch(e.type)
	{
		case "focus": writeI32(0, 9/*FocusIn*/); break;
		case "blur": writeI32(0, 10/*FocusOut*/); break;
		case "mousedown":
			writeI32(0, 4/*ButtonPress*/); writeI32(4, e.x); writeI32(8, e.y); writeI32(12, e.button); break;
		case "mouseup":
			writeI32(0, 5/*ButtonRelease*/); writeI32(4, e.x); writeI32(8, e.y); writeI32(12, e.button); break;
		case "mousemove":
			writeI32(0, 6/*MotionNotify*/); writeI32(4, e.x); writeI32(8, e.y); break;
		case "keydown":
			writeI32(0, 2/*KeyPress*/); writeI32(4, e.keySym); writeI32(8, e.keyCode); writeI32(12, e.keyState); writeI32(20, e.charCode); break;
		case "keyup":
			writeI32(0, 3/*KeyRelease*/); writeI32(4, e.keySym); writeI32(8, e.keyCode); writeI32(12, e.keyState); writeI32(20, e.charCode); break;
		default:
			warnOnce(unsupportedEventTypes, e.type, "Unsupported X11 event type=" + e.type);
	}
}

async function linuxEventBufferView(lib, buffer)
{
	const bufferAddr = Number(await buffer.address());
	return {addr: bufferAddr, view: lib.getJNIDataView()};
}
function Java_org_lwjgl_opengl_LinuxEvent_nGetWindow() { return 0; }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetType(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+0,true); }
function Java_org_lwjgl_opengl_LinuxEvent_nFilterEvent() { return false; }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonTime(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+16,true); }
function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonRoot() { return 0; }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonXRoot(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+4,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonYRoot(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+8,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonX(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+4,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonY(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+8,true); }
function Java_org_lwjgl_opengl_LinuxEvent_nGetFocusDetail() { return 0; }
function Java_org_lwjgl_opengl_LinuxEvent_nGetFocusMode() { return 0; }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonType(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+0,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonButton(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+12,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetButtonState(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+12,true); }

function Java_org_lwjgl_opengl_LinuxDisplay_nGrabPointer()
{
	if(!lockedMousePos) lockedMousePos = { x: getCanvasWidth()/2, y: getCanvasHeight()/2 };
	requestGamePointerLock();
}
function Java_org_lwjgl_opengl_LinuxDisplay_nUngrabPointer()
{
	try { if(document.pointerLockElement === glCanvas) document.exitPointerLock(); } catch(_) {}
	lockedMousePos = null;
}
function Java_org_lwjgl_opengl_LinuxDisplay_nDefineCursor() {}
function Java_org_lwjgl_opengl_LinuxDisplay_getRootWindow() { return 0; }
function Java_org_lwjgl_opengl_LinuxDisplay_nSetWindowIcon() {}
function Java_org_lwjgl_opengl_LinuxMouse_nGetWindowWidth() { return getCanvasWidth(); }
function Java_org_lwjgl_opengl_LinuxMouse_nSendWarpEvent() {}
function Java_org_lwjgl_opengl_LinuxMouse_nWarpCursor(lib, display, window, x, y) { lockedMousePos = {x:Number(x)||0, y:Number(y)||0}; }
function Java_org_lwjgl_opengl_LinuxEvent_nSetWindow() {}
function Java_org_lwjgl_opengl_LinuxEvent_nSendEvent() {}
async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyAddress(lib, buffer) { return Number(await buffer.address()); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyTime(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+16,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyType(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+0,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyKeyCode(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+8,true); }
async function Java_org_lwjgl_opengl_LinuxEvent_nGetKeyState(lib, buffer) { const b=await linuxEventBufferView(lib,buffer); return b.view.getInt32(b.addr+12,true); }
function Java_org_lwjgl_opengl_LinuxKeyboard_lookupKeysym(lib, eventPtr, index)
{
	const v = lib.getJNIDataView();
	return v.getInt32(Number(eventPtr) + 4, true);
}
async function Java_org_lwjgl_opengl_LinuxKeyboard_lookupString(lib, eventPtr, buffer)
{
	const dstAddr = Number(await buffer.address());
	const v = lib.getJNIDataView();
	const charCode = v.getInt32(Number(eventPtr) + 20, true);
	if(!charCode) return 0;
	v.setInt8(dstAddr, charCode & 0xff);
	return 1;
}

export default {
	Java_org_lwjgl_input_Keyboard_nReset,
	Java_org_lwjgl_input_Keyboard_nPoll,
	Java_org_lwjgl_input_Keyboard_nIsKeyDown,
	Java_org_lwjgl_input_Keyboard_nNext,
	Java_org_lwjgl_input_Keyboard_nGetEventKey,
	Java_org_lwjgl_input_Keyboard_nGetEventKeyState,
	Java_org_lwjgl_input_Keyboard_nGetEventCharacter,
	Java_org_lwjgl_input_Keyboard_nGetEventNanoseconds,
	Java_org_lwjgl_input_Keyboard_nIsRepeatEvent,
	Java_org_lwjgl_input_Keyboard_nSetRepeatEvents,
	Java_org_lwjgl_input_Mouse_nReset,
	Java_org_lwjgl_input_Mouse_nPoll,
	Java_org_lwjgl_input_Mouse_nIsButtonDown,
	Java_org_lwjgl_input_Mouse_nNext,
	Java_org_lwjgl_input_Mouse_nGetEventButton,
	Java_org_lwjgl_input_Mouse_nGetEventButtonState,
	Java_org_lwjgl_input_Mouse_nGetEventDX,
	Java_org_lwjgl_input_Mouse_nGetEventDY,
	Java_org_lwjgl_input_Mouse_nGetEventX,
	Java_org_lwjgl_input_Mouse_nGetEventY,
	Java_org_lwjgl_input_Mouse_nGetEventDWheel,
	Java_org_lwjgl_input_Mouse_nGetEventNanoseconds,
	Java_org_lwjgl_input_Mouse_nGetX,
	Java_org_lwjgl_input_Mouse_nGetY,
	Java_org_lwjgl_input_Mouse_nGetDX,
	Java_org_lwjgl_input_Mouse_nGetDY,
	Java_org_lwjgl_input_Mouse_nGetDWheel,
	Java_org_lwjgl_input_Mouse_nSetGrabbed,
	Java_org_lwjgl_input_Mouse_nSetCursorPosition,
	Java_org_lwjgl_input_Mouse_nIsInsideWindow,
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
	Java_org_lwjgl_opengl_GL11_nglPixelStorei,
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
	Java_org_lwjgl_opengl_GL11_nglIsEnabled,
	Java_org_lwjgl_opengl_GL11_nglPushAttrib,
	Java_org_lwjgl_opengl_GL11_nglPopAttrib,
	Java_org_lwjgl_opengl_GL11_nglPushMatrix,
	Java_org_lwjgl_opengl_GL11_nglPopMatrix,
	Java_org_lwjgl_opengl_GL11_nglMultMatrixf,
	Java_org_lwjgl_opengl_GL11_nglRotatef,
	Java_org_lwjgl_opengl_GL11_nglDepthMask,
	Java_org_lwjgl_opengl_GL11_nglBlendFunc,
	Java_org_lwjgl_opengl_GL11_nglColorMask,
	Java_org_lwjgl_opengl_GL11_nglCopyTexImage2D,
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
	Java_org_lwjgl_opengl_GL11_nglScissor,
	Java_org_lwjgl_opengl_GL11_nglStencilFunc,
	Java_org_lwjgl_opengl_GL11_nglStencilOp,
	Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord,
	Java_org_lwjgl_opengl_GL11_nglPointSize,
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
	Java_org_lwjgl_opengl_LinuxEvent_nGetFocusMode,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonType,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonButton,
	Java_org_lwjgl_opengl_LinuxEvent_nGetButtonState,
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
