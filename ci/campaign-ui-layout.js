'use strict';

// launch.html reserves 20px padding on each side and a 2px canvas border.
// Leave enough vertical room for launcher controls as well as the full canvas;
// comparator screenshots must not resample thin ship/selection-ring pixels.
function positiveDimension(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? Math.ceil(number) : fallback;
}

function campaignTestViewport(renderWidth = 1024, renderHeight = 768) {
  const width = positiveDimension(renderWidth, 1024);
  const height = positiveDimension(renderHeight, 768);
  return {
    width: Math.max(1440, width + 44),
    height: Math.max(1180, height + 400),
  };
}

// The bottom controls retain fixed game-pixel dimensions at every resolution:
// 125px buttons, 6px gaps, first center x=68.5, center 20px from the bottom.
function campaignControlPosition(slot, renderWidth = 1024, renderHeight = 768) {
  if (!Number.isInteger(slot) || slot < 0 || slot > 6) {
    throw new RangeError('Campaign control slot must be an integer from 0 to 6');
  }
  const width = positiveDimension(renderWidth, 1024);
  const height = positiveDimension(renderHeight, 768);
  const x = 68.5 + 131 * slot;
  const y = height - 20;
  return { x, y, nx: x / width, ny: y / height };
}

module.exports = { campaignTestViewport, campaignControlPosition };
