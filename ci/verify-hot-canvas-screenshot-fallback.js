'use strict';
const assert = require('assert');
const { PNG } = require('pngjs');
const { cropGameCanvasFromViewportScreenshot } = require('./canvas-screenshot-fallback');

function setPixel(png, x, y, r, g, b, a = 255) {
  const i = (y * png.width + x) * 4;
  png.data[i] = r; png.data[i + 1] = g; png.data[i + 2] = b; png.data[i + 3] = a;
}
function fillRect(png, x, y, w, h, r, g, b, a = 255) {
  for (let yy = y; yy < y + h; yy += 1) for (let xx = x; xx < x + w; xx += 1) setPixel(png, xx, yy, r, g, b, a);
}
function borderRect(png, x, y, w, h, t = 2) {
  fillRect(png, x, y, w, t, 0, 217, 255);
  fillRect(png, x, y + h - t, w, t, 0, 217, 255);
  fillRect(png, x, y, t, h, 0, 217, 255);
  fillRect(png, x + w - t, y, t, h, 0, 217, 255);
}

const png = new PNG({ width: 1440, height: 1180 });
fillRect(png, 0, 0, png.width, png.height, 0, 0, 0);
// Decoy launcher button: broad cyan area, but not a matching 4:3 border pair.
fillRect(png, 570, 90, 300, 42, 0, 217, 255);
const x = 206, y = 210, contentW = 1024, contentH = 768;
borderRect(png, x, y, contentW + 4, contentH + 4, 2);
for (let yy = 0; yy < contentH; yy += 1) {
  for (let xx = 0; xx < contentW; xx += 1) {
    setPixel(png, x + 2 + xx, y + 2 + yy, 18 + (xx % 70), 28 + (yy % 90), 45 + ((xx + yy) % 120));
  }
}
const found = cropGameCanvasFromViewportScreenshot(PNG.sync.write(png), 1024, 768);
assert(found, 'failed to locate synthetic game canvas');
const cropped = PNG.sync.read(found.buffer);
assert.strictEqual(cropped.width, 1024);
assert.strictEqual(cropped.height, 768);
assert.deepStrictEqual(found.clip, { x: x + 2, y: y + 2, width: 1024, height: 768 });
const first = (0 * cropped.width + 0) * 4;
assert.strictEqual(cropped.data[first], 18, 'crop includes border instead of content');
console.log(`verify-hot-canvas-screenshot-fallback: OK clip=${found.clip.x},${found.clip.y},${found.clip.width}x${found.clip.height}`);
