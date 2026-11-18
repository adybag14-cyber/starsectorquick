'use strict';

const assert = require('node:assert/strict');
const { campaignTestViewport, campaignControlPosition } = require('./campaign-ui-layout');

assert.deepEqual(campaignTestViewport(), { width: 1440, height: 1180 });
assert.deepEqual(campaignTestViewport(1024, 768), { width: 1440, height: 1180 });
for (let slot = 0; slot < 7; slot++) {
  const point = campaignControlPosition(slot, 1024, 768);
  assert.equal(point.nx, (68.5 + 131 * slot) / 1024);
  assert.equal(point.ny, 748 / 768);
}

for (const [width, height] of [[1024, 768], [1920, 1080], [2560, 1440]]) {
  const viewport = campaignTestViewport(width, height);
  // Apply the actual page layout constraints: 40px padding + 4px border.
  const displayedWidth = Math.min(width, viewport.width - 44);
  assert.equal(displayedWidth, width, 'canvas must fit without CSS downsampling');
  assert.ok(viewport.height >= height + 300, 'canvas and launcher must fit fallback capture');
  for (let slot = 0; slot < 7; slot++) {
    const point = campaignControlPosition(slot, width, height);
    for (const scale of [1, 0.727]) {
      const canvasBox = { x: 22, y: 247, width: width * scale, height: height * scale };
      const clickX = canvasBox.x + canvasBox.width * point.nx;
      const clickY = canvasBox.y + canvasBox.height * point.ny;
      const nativeX = (clickX - canvasBox.x) / scale;
      const nativeY = (clickY - canvasBox.y) / scale;
      assert.ok(Math.abs(nativeX - (68.5 + 131 * slot)) < 1e-9);
      assert.ok(Math.abs(nativeY - (height - 20)) < 1e-9);
      assert.ok(nativeX > 6 + 131 * slot && nativeX < 131 + 131 * slot,
        'click must stay inside its intended fixed-size button');
    }
  }
}

// Regression demonstration: the previous normalized Fleet click lands beyond
// the Fleet button at Full HD, even though its CSS event is delivered correctly.
const oldFleetX = ((68.5 + 131) / 1024) * 1920;
assert.ok(oldFleetX > 131 + 131);
assert.throws(() => campaignControlPosition(7, 1920, 1080), RangeError);
console.log('PASS campaign UI layout: default unchanged; native canvas fits; all seven fixed button centers survive CSS scaling');
