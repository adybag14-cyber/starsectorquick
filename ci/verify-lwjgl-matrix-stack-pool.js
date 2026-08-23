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
if (!path) throw new Error('usage: node ci/verify-lwjgl-matrix-stack-pool.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const names = [
  'acquireCompatMatrixStackEntry',
  'releaseCompatMatrixStackEntry',
  'Java_org_lwjgl_opengl_GL11_nglPushMatrix',
  'Java_org_lwjgl_opengl_GL11_nglPopMatrix',
];

let creates = 0;
let copies = 0;
let dirtyMarks = 0;
const warnings = [];
const base = new Float32Array(16);
base[0] = 7;
const stack = [base];
const context = vm.createContext({
  compatMatrixStackPool: [],
  compatMatrixStackPoolMax: 64,
  glMatrix: {
    mat4: {
      create() { creates++; return new Float32Array(16); },
      copy(out, sourceMatrix) { copies++; out.set(sourceMatrix); return out; },
    },
  },
  curList: null,
  pushInList() { throw new Error('unexpected display-list path'); },
  ensureCurMatrixStack() { return stack; },
  markCurMatrixUniformDirty() { dirtyMarks++; },
  warnOnce(cache, key, message) { warnings.push([cache, key, message]); },
  matrixStackWarnings: new Set(),
  Float32Array,
});
vm.runInContext(names.map(name => extractFunction(source, name)).join('\n'), context);

context.Java_org_lwjgl_opengl_GL11_nglPushMatrix(null, null);
expect(stack.length === 2, 'push did not add a matrix');
const firstEntry = stack[1];
expect(firstEntry !== base && firstEntry[0] === 7, 'push did not copy the active matrix');
expect(creates === 1 && copies === 1, `first push should allocate/copy once, got creates=${creates} copies=${copies}`);

context.Java_org_lwjgl_opengl_GL11_nglPopMatrix(null, null);
expect(stack.length === 1, 'pop did not remove the top matrix');
expect(context.compatMatrixStackPool.length === 1 && context.compatMatrixStackPool[0] === firstEntry,
  'popped matrix was not retained in the pool');
expect(dirtyMarks === 1, 'successful pop did not invalidate the matrix uniform');

base[0] = 11;
context.Java_org_lwjgl_opengl_GL11_nglPushMatrix(null, null);
expect(stack[1] === firstEntry && stack[1][0] === 11, 'second push did not reuse and refresh the pooled matrix');
expect(creates === 1 && copies === 2, `pooled push allocated unexpectedly: creates=${creates} copies=${copies}`);
context.Java_org_lwjgl_opengl_GL11_nglPopMatrix(null, null);

context.Java_org_lwjgl_opengl_GL11_nglPopMatrix(null, null);
expect(stack.length === 1, 'underflow pop removed the base matrix');
expect(dirtyMarks === 2, 'underflow pop incorrectly invalidated the matrix uniform');
expect(warnings.some(row => row[1] === 'matrix-stack-underflow'), 'underflow warning was not emitted');

for (let i = 0; i < 70; i++) context.releaseCompatMatrixStackEntry(new Float32Array(16));
expect(context.compatMatrixStackPool.length === 64, `matrix pool exceeded its bound: ${context.compatMatrixStackPool.length}`);
expect(source.includes('LWJGL_MATRIX_STACK_POOL_V1'), 'missing matrix stack pool marker');
console.log(`verify-lwjgl-matrix-stack-pool: OK creates=${creates} copies=${copies} pool=${context.compatMatrixStackPool.length}`);
