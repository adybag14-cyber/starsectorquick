'use strict';

const fs = require('fs');
const vm = require('vm');

function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}(`);
  if (start < 0) throw new Error(`missing function ${name}`);
  const brace = source.indexOf('{', start);
  let depth = 0;
  let quote = null;
  let escaped = false;
  for (let i = brace; i < source.length; i++) {
    const ch = source[i];
    if (quote) {
      if (escaped) escaped = false;
      else if (ch === '\\') escaped = true;
      else if (ch === quote) quote = null;
      continue;
    }
    if (ch === '"' || ch === "'" || ch === '`') { quote = ch; continue; }
    if (ch === '{') depth++;
    else if (ch === '}' && --depth === 0) return source.slice(start, i + 1);
  }
  throw new Error(`unterminated function ${name}`);
}

function expect(condition, message) {
  if (!condition) throw new Error(message);
}

const path = process.argv[2];
if (!path) throw new Error('usage: node ci/verify-lwjgl-matrix-inplace.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const names = [
  'setCompatVec3',
  'Java_org_lwjgl_opengl_GL11_nglOrtho',
  'Java_org_lwjgl_opengl_GL11_nglTranslatef',
  'Java_org_lwjgl_opengl_GL11_nglMultMatrixf',
  'Java_org_lwjgl_opengl_GL11_nglRotatef',
  'Java_org_lwjgl_opengl_GL11_nglScalef',
];

const calls = [];
const matrix = { kind: 'active-matrix' };
const scratchMatrix = { kind: 'scratch-matrix' };
const scratchVec3 = new Float32Array(3);
const buffer = new ArrayBuffer(128);
for (let i = 0; i < 16; i++) new Float32Array(buffer, 16, 16)[i] = i + 1;
let committed = null;

const glMatrix = {
  mat4: {
    ortho(out, left, right, bottom, top, nearVal, farVal) {
      calls.push(['ortho', out, left, right, bottom, top, nearVal, farVal]);
      return out;
    },
    multiply(out, a, b) { calls.push(['multiply', out, a, b]); return out; },
    translate(out, a, v) { calls.push(['translate', out, a, Array.from(v)]); return out; },
    rotate(out, a, radians, axis) { calls.push(['rotate', out, a, radians, Array.from(axis)]); return out; },
    scale(out, a, v) { calls.push(['scale', out, a, Array.from(v)]); return out; },
  },
};
const context = vm.createContext({
  glMatrix,
  compatMatrixScratch: scratchMatrix,
  compatVec3Scratch: scratchVec3,
  curList: null,
  pushInList() { throw new Error('unexpected display-list path'); },
  getCurMatrixTop() { return matrix; },
  setCurMatrixTop(value) { committed = value; },
  Float32Array,
  Number,
  Math,
});
vm.runInContext(names.map(name => extractFunction(source, name)).join('\n'), context);

context.Java_org_lwjgl_opengl_GL11_nglOrtho(null, -2, 4, -3, 5, -1, 1, null);
expect(calls[0][0] === 'ortho' && calls[0][1] === scratchMatrix, 'ortho did not reuse matrix scratch storage');
expect(calls[1][0] === 'multiply' && calls[1][1] === matrix && calls[1][2] === matrix && calls[1][3] === scratchMatrix,
  'ortho multiplication was not in place or changed operand order');
expect(committed === matrix, 'ortho did not commit the active matrix');

context.Java_org_lwjgl_opengl_GL11_nglTranslatef(null, 7, 8, 9, null);
const translate = calls.find(call => call[0] === 'translate');
expect(translate && translate[1] === matrix && translate[2] === matrix && translate[3].join(',') === '7,8,9',
  'translation did not update the active matrix in place');

const lib = { getJNIDataView() { return new DataView(buffer); } };
context.Java_org_lwjgl_opengl_GL11_nglMultMatrixf(lib, 16, null);
const multiply = calls.filter(call => call[0] === 'multiply').at(-1);
expect(multiply[1] === matrix && multiply[2] === matrix && multiply[3] instanceof Float32Array,
  'glMultMatrix did not multiply into the active matrix');
expect(Array.from(multiply[3]).join(',') === Array.from({ length: 16 }, (_, i) => i + 1).join(','),
  'glMultMatrix source view changed');

context.Java_org_lwjgl_opengl_GL11_nglRotatef(null, 90, 0, 0, 1, null);
const rotate = calls.find(call => call[0] === 'rotate');
expect(rotate && rotate[1] === matrix && rotate[2] === matrix && Math.abs(rotate[3] - Math.PI / 2) < 1e-12,
  'rotation did not preserve angle conversion and in-place output');
expect(rotate[4].join(',') === '0,0,1', 'rotation axis scratch values changed');

context.Java_org_lwjgl_opengl_GL11_nglScalef(null, 2, 3, 4, null);
const scale = calls.find(call => call[0] === 'scale');
expect(scale && scale[1] === matrix && scale[2] === matrix && scale[3].join(',') === '2,3,4',
  'scale did not update the active matrix in place');

expect(source.includes('LWJGL_MATRIX_INPLACE_TRANSFORMS_V1'), 'missing in-place matrix marker');
console.log(`verify-lwjgl-matrix-inplace: OK calls=${calls.length}`);
