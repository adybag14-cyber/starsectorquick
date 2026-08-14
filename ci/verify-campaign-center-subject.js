'use strict';

const assert = require('assert');
const {
  hasCampaignCenterSubject,
  isCampaignFramePlayable,
  campaignCenterSubjectGate,
} = require('./campaign-visual-gate');

const fail = { centerBrightPixels: 149, centerWarmPixels: 7 };
const warmPass = { centerBrightPixels: 150, centerWarmPixels: 8 };
const brightPass = { centerBrightPixels: 350, centerWarmPixels: 0 };
const brightNearMiss = { centerBrightPixels: 349, centerWarmPixels: 7 };
const strongWarmPass = { centerBrightPixels: 120, centerWarmPixels: 12 };
const knownBrokenTextureFrame = { centerBrightPixels: 131, centerWarmPixels: 9 };

assert.strictEqual(hasCampaignCenterSubject(warmPass), true, 'warm-pixel path should pass');
assert.strictEqual(hasCampaignCenterSubject(brightPass), true, 'bright centered-subject fallback should pass');
assert.strictEqual(hasCampaignCenterSubject(brightNearMiss), false, 'bright fallback boundary should remain strict');
assert.strictEqual(hasCampaignCenterSubject(strongWarmPass), true, 'strong warm-centered subject should pass with lower brightness');
assert.strictEqual(hasCampaignCenterSubject(knownBrokenTextureFrame), false, 'known broken missing-ship texture frame must remain rejected');
assert.strictEqual(hasCampaignCenterSubject(fail), false, 'sub-threshold frame should fail');

assert.deepStrictEqual(
  campaignCenterSubjectGate('campaign', warmPass, fail),
  { first: true, second: false, overall: true },
  'first-frame-only subject should pass overall',
);
assert.deepStrictEqual(
  campaignCenterSubjectGate('campaign', fail, brightPass),
  { first: false, second: true, overall: true },
  'second-frame-only subject should pass overall',
);
assert.deepStrictEqual(
  campaignCenterSubjectGate('campaign', fail, brightNearMiss),
  { first: false, second: false, overall: false },
  'both failing frames should fail overall',
);
assert.deepStrictEqual(
  campaignCenterSubjectGate('title', null, null),
  { first: true, second: true, overall: true },
  'non-campaign states should bypass the center-subject gate',
);


const playableStats = {
  centerBrightPixels: 160, centerWarmPixels: 12, nonBlackRatio: 0.2,
  darkRatio: 0.7, midToneRatio: 0.1, quantizedColorCount: 500, variance: 900
};
assert.strictEqual(isCampaignFramePlayable(playableStats), true, 'full playable frame should pass');
assert.strictEqual(isCampaignFramePlayable({ ...playableStats, quantizedColorCount: 199 }), false, 'texture-poor frame must fail playable gate');
assert.strictEqual(isCampaignFramePlayable({ ...playableStats, centerWarmPixels: 9, centerBrightPixels: 131 }), false, 'known broken ship signature must fail playable gate');

console.log('CampaignCenterSubjectGate: OK center subject and first-playable-frame gates');
