'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const vm = require('node:vm');
const { captureExperimentFlags, restoreExperimentFlags } = require('./renderer-experiment-state');
const { runPairedExperiment } = require('./cheerpj-paired-experiment');
const { runPacingScreen } = require('./cheerpj-pacing-screen');
const { runRafPairedExperiment } = require('./cheerpj-raf-paired');

(async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'starsector-experiment-state-'));
  let checks = 0;
  try {
    for (const initial of [{}, { __LWJGL_COLOR_ATTRIB_CACHE__: false, __LWJGL_EXTENDED_ATTRIB_CACHE__: true, __LWJGL_CAMPAIGN_PACING__: 'flush' }]) {
      let invalidations = 0;
      const window = { ...initial, __lwjglInvalidateExtendedAttribState: () => invalidations++,
        __lwjglExtendedAttribStats: {}, __lwjglSnapshotPresentationIntervals: () => ({}),
        __lwjglPresentationStats: { swapCount: 0 }, __lwjglCampaignBoundaryStats: {} };
      const page = { evaluate: async (fn, arg) => vm.runInNewContext('('+fn.toString()+')(input)', {window, input:arg}),
        waitForFunction: async () => { throw new Error('injected warmup failure'); } };
      const before = JSON.parse(JSON.stringify(await captureExperimentFlags(page)));
      window.__LWJGL_COLOR_ATTRIB_CACHE__ = true;window.__LWJGL_EXTENDED_ATTRIB_CACHE__ = false;window.__LWJGL_CAMPAIGN_PACING__ = 'raf';
      await restoreExperimentFlags(page, before);
      assert.deepEqual(JSON.parse(JSON.stringify(await captureExperimentFlags(page))), before);checks++;
      for (const [name, experiment] of [['extended-attrib-ab.json', runPairedExperiment], ['campaign-pacing-screen.json', runPacingScreen], ['campaign-raf-paired.json', runRafPairedExperiment]]) {
        await assert.rejects(experiment(page, directory), /injected warmup failure/);checks++;
        assert.deepEqual(JSON.parse(JSON.stringify(await captureExperimentFlags(page))), before);checks++;
        assert.equal(JSON.parse(fs.readFileSync(path.join(directory,name),'utf8')).ok, false);checks++;
      }
      assert.ok(invalidations >= 3);checks++;
    }
    await assert.rejects(restoreExperimentFlags({evaluate:async()=>{throw new Error('page closed');}},[]), /page closed/);checks++;
    console.log('test-renderer-experiment-state: OK '+checks+' checks; defaults restored after failed experiments');
  } finally { fs.rmSync(directory, {recursive:true,force:true}); }
})().catch(error=>{console.error(error);process.exitCode=1;});
