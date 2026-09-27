'use strict';

/** Capture actual compositor pixels without Playwright's in-page font/RAF waits.
 * Used only after the normal locator capture has failed. No fake image, canvas
 * resizing, frame skipping or game-state mutation is used to make capture pass.
 */
async function captureCompositorViewport(page, timeoutMs = 12000) {
  if (!Number.isFinite(timeoutMs) || timeoutMs < 1) throw new Error('Invalid screenshot timeout');
  const session = await page.context().newCDPSession(page);
  let timer;
  try {
    const result = await Promise.race([
      session.send('Page.captureScreenshot', {
        format: 'png', fromSurface: true, captureBeyondViewport: false, optimizeForSpeed: true,
      }),
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error('Compositor screenshot timed out')), timeoutMs); }),
    ]);
    if (!result || typeof result.data !== 'string') throw new Error('Compositor returned no screenshot');
    const bytes = Buffer.from(result.data, 'base64');
    const signature = Buffer.from([137,80,78,71,13,10,26,10]);
    if (bytes.length < 33 || !bytes.subarray(0,8).equals(signature)) throw new Error('Compositor returned invalid PNG');
    return bytes;
  } finally {
    if (timer) clearTimeout(timer);
    await session.detach().catch(() => {});
  }
}
module.exports = { captureCompositorViewport };
