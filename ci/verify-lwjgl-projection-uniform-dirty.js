'use strict';

const fs = require('fs');
const vm = require('vm');

const sourcePath = process.argv[2] || 'build/final/wasm-modules/lwjgl.js';
const src = fs.readFileSync(sourcePath, 'utf8');

if (!src.includes('projectionUniformDirtyActive: true')) throw new Error('runtime activation flag missing');
const programUseCount = (src.match(/glCtx\.useProgram\(/g) || []).length;
if (programUseCount !== 1) {
  throw new Error(`projection uniform cache requires one persistent shader program; found ${programUseCount} useProgram calls`);
}


function hasFunction(name) {
  return src.includes(`function ${name}(`);
}

function extract(name) {
  const start = src.indexOf(`function ${name}(`);
  if (start < 0) throw new Error(`missing ${name}`);
  const brace = src.indexOf('{', start);
  let depth = 0;
  for (let i = brace; i < src.length; i++) {
    if (src[i] === '{') depth++;
    else if (src[i] === '}' && --depth === 0) return src.slice(start, i + 1);
  }
  throw new Error(`unterminated ${name}`);
}

function bodyHas(name, needle) {
  if (!extract(name).includes(needle)) throw new Error(`${name} missing ${needle}`);
}

bodyHas('Java_org_lwjgl_opengl_GL11_nglLoadIdentity', 'markCurMatrixUniformDirty();');
bodyHas('Java_org_lwjgl_opengl_GL11_nglPopMatrix', 'markCurMatrixUniformDirty();');
for (const name of [
  'Java_org_lwjgl_opengl_GL11_nglOrtho',
  'Java_org_lwjgl_opengl_GL11_nglTranslatef',
  'Java_org_lwjgl_opengl_GL11_nglMultMatrixf',
  'Java_org_lwjgl_opengl_GL11_nglRotatef',
  'Java_org_lwjgl_opengl_GL11_nglScalef',
]) bodyHas(name, 'setCurMatrixTop(');
if (extract('Java_org_lwjgl_opengl_GL11_nglPushMatrix').includes('markCurMatrixUniformDirty')) {
  throw new Error('push should not dirty an identical matrix clone');
}

function exercise(drawName) {
  const mv = [new Float32Array(16)];
  const pr = [new Float32Array(16)];
  const tx = [new Float32Array(16)];
  const calls = [];
  const glCtx = {
    TRIANGLES: 4,
    POINTS: 0,
    LINES: 1,
    LINE_LOOP: 2,
    LINE_STRIP: 3,
    TRIANGLE_STRIP: 5,
    TRIANGLE_FAN: 6,
    ELEMENT_ARRAY_BUFFER: 34963,
    UNSIGNED_INT: 5125,
    uniformMatrix4fv(loc) { calls.push(['u', loc]); },
    drawArrays(mode, first, count) { calls.push(['d', mode, first, count]); },
    drawElements() { calls.push(['de']); },
    bindBuffer() {},
  };
  const context = vm.createContext({
    glCtx,
    modelViewMatrixStack: mv,
    projMatrixStack: pr,
    textureMatrixStack: tx,
    curMatrixStack: mv,
    mvLocation: 'mv',
    projLocation: 'pr',
    projectionUniformDirty: true,
    detailedDrawTelemetryEnabled: false,
    presentationStats: {},
    assert(value) { if (!value) throw new Error('assert'); },
    ensureQuadIndexCapacity() {},
    quadIndexBuffer: {},
    unsupportedDrawModes: new Set(),
    warnOnce() {},
    console,
  });
  vm.runInContext([
    extract('markCurMatrixUniformDirty'),
    extract('setCurMatrixTop'),
    extract(drawName),
  ].join('\n'), context);
  const draw = context[drawName];
  const uniformsSince = n => calls.slice(n).filter(call => call[0] === 'u').map(call => call[1]);
  const expectUniforms = (label, expected, action) => {
    const n = calls.length;
    action();
    const actual = uniformsSince(n).join(',');
    if (actual !== expected) throw new Error(`${drawName} ${label}: expected ${expected}, got ${actual}`);
  };

  expectUniforms('first draw', 'mv,pr', () => draw(glCtx.TRIANGLES, 0, 3));
  expectUniforms('repeat draw', 'mv', () => draw(glCtx.TRIANGLES, 0, 3));
  context.curMatrixStack = mv;
  context.setCurMatrixTop(new Float32Array(16));
  expectUniforms('modelview mutation', 'mv', () => draw(glCtx.TRIANGLES, 0, 3));
  context.curMatrixStack = pr;
  context.setCurMatrixTop(new Float32Array(16));
  expectUniforms('projection mutation', 'mv,pr', () => draw(glCtx.TRIANGLES, 0, 3));
  context.curMatrixStack = tx;
  context.setCurMatrixTop(new Float32Array(16));
  expectUniforms('texture mutation', 'mv', () => draw(glCtx.TRIANGLES, 0, 3));
}

const drawPaths = ['drawArraysImpl'];
if (hasFunction('drawArraysImplProduction')) drawPaths.push('drawArraysImplProduction');
for (const drawPath of drawPaths) exercise(drawPath);

console.log(`verify-lwjgl-projection-uniform-dirty: OK paths=${drawPaths.join(',')} initial=mv+pr repeat=mv projection-mutation=mv+pr`);
