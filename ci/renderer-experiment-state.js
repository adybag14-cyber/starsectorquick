'use strict';

// Diagnostic toggles belong to an experiment, not to the running campaign.
// Preserve absence as well as explicit values, including exceptional exits.
async function captureExperimentFlags(page) {
  return page.evaluate(() => ['__LWJGL_COLOR_ATTRIB_CACHE__', '__LWJGL_EXTENDED_ATTRIB_CACHE__', '__LWJGL_CAMPAIGN_PACING__', '__LWJGL_DEPTH_STATE_CACHE__', '__LWJGL_VERTEX_ARRAY_CACHE__']
    .map(name => ({ name, present: Object.prototype.hasOwnProperty.call(window, name), value: window[name] })));
}
async function restoreExperimentFlags(page, saved) {
  await page.evaluate(saved => {
    for (const row of saved) {
      if (row.present) window[row.name] = row.value;
      else delete window[row.name];
    }
    window.__lwjglInvalidateExtendedAttribState?.();
    window.__lwjglInvalidateVertexArrayState?.();
  }, saved);
}
module.exports = { captureExperimentFlags, restoreExperimentFlags };
