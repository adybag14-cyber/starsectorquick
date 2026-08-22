const { PNG } = require('pngjs');

function isBorderCyan(data, offset) {
  const r = data[offset];
  const g = data[offset + 1];
  const b = data[offset + 2];
  const a = data[offset + 3];
  return a >= 200 && r <= 80 && g >= 150 && b >= 180 && g - r >= 90 && b - r >= 100;
}

function longestCyanRun(png, y) {
  let bestStart = -1;
  let bestEnd = -1;
  let runStart = -1;
  for (let x = 0; x < png.width; x += 1) {
    const cyan = isBorderCyan(png.data, (y * png.width + x) * 4);
    if (cyan && runStart < 0) runStart = x;
    if ((!cyan || x === png.width - 1) && runStart >= 0) {
      const runEnd = cyan && x === png.width - 1 ? x : x - 1;
      if (bestStart < 0 || runEnd - runStart > bestEnd - bestStart) {
        bestStart = runStart;
        bestEnd = runEnd;
      }
      runStart = -1;
    }
  }
  return bestStart < 0 ? null : { start: bestStart, end: bestEnd, length: bestEnd - bestStart + 1 };
}

function copyCrop(png, x, y, width, height) {
  const out = new PNG({ width, height });
  for (let row = 0; row < height; row += 1) {
    const srcStart = ((y + row) * png.width + x) * 4;
    const srcEnd = srcStart + width * 4;
    png.data.copy(out.data, row * width * 4, srcStart, srcEnd);
  }
  return PNG.sync.write(out);
}

function cropGameCanvasFromViewportScreenshot(buffer, expectedWidth = 1024, expectedHeight = 768) {
  if (!buffer) return null;
  const png = PNG.sync.read(buffer);
  const expectedRatio = Number(expectedWidth) > 0 && Number(expectedHeight) > 0
    ? Number(expectedWidth) / Number(expectedHeight)
    : 4 / 3;
  const minRun = Math.max(220, Math.floor(png.width * 0.18));
  const qualifying = [];
  for (let y = 0; y < png.height; y += 1) {
    const run = longestCyanRun(png, y);
    if (run && run.length >= minRun) qualifying.push({ y, ...run });
  }
  if (qualifying.length < 2) return null;

  const bands = [];
  for (const row of qualifying) {
    const last = bands[bands.length - 1];
    if (last && row.y === last.y1 + 1 && Math.abs(row.start - last.best.start) <= 6 && Math.abs(row.end - last.best.end) <= 6) {
      last.y1 = row.y;
      if (row.length > last.best.length) last.best = row;
    } else {
      bands.push({ y0: row.y, y1: row.y, best: row });
    }
  }

  let best = null;
  for (let i = 0; i < bands.length; i += 1) {
    for (let j = i + 1; j < bands.length; j += 1) {
      const top = bands[i];
      const bottom = bands[j];
      const topRun = top.best;
      const bottomRun = bottom.best;
      if (Math.abs(topRun.start - bottomRun.start) > 8 || Math.abs(topRun.end - bottomRun.end) > 8) continue;
      const thickness = Math.max(1, Math.min(4, top.y1 - top.y0 + 1, bottom.y1 - bottom.y0 + 1));
      const left = Math.round((topRun.start + bottomRun.start) / 2) + thickness;
      const right = Math.round((topRun.end + bottomRun.end) / 2) - thickness;
      const topInside = top.y1 + 1;
      const bottomInside = bottom.y0 - 1;
      const width = right - left + 1;
      const height = bottomInside - topInside + 1;
      if (width < 200 || height < 150) continue;
      const ratio = width / height;
      const ratioError = Math.abs(ratio - expectedRatio) / expectedRatio;
      if (ratioError > 0.12) continue;
      const area = width * height;
      const score = area * (1 - ratioError);
      if (!best || score > best.score) best = { left, top: topInside, width, height, score };
    }
  }
  if (!best) return null;
  if (best.left < 0 || best.top < 0 || best.left + best.width > png.width || best.top + best.height > png.height) return null;
  return {
    buffer: copyCrop(png, best.left, best.top, best.width, best.height),
    clip: { x: best.left, y: best.top, width: best.width, height: best.height },
  };
}

module.exports = { cropGameCanvasFromViewportScreenshot };
