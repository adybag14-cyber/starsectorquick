'use strict';

const assert = require('assert');
const {
  hasCampaignCenterSubject,
  campaignCenterSubjectGate,
} = require('./campaign-visual-gate');

const fail = { centerBrightPixels: 149, centerWarmPixels: 7 };
const warmPass = { centerBrightPixels: 150, centerWarmPixels: 8 };
const brightPass = { centerBrightPixels: 350, centerWarmPixels: 0 };
const brightNearMiss = { centerBrightPixels: 349, centerWarmPixels: 7 };

assert.strictEqual(hasCampaignCenterSubject(warmPass), true, 'warm-pixel path should pass');
assert.strictEqual(hasCampaignCenterSubject(brightPass), true, 'bright centered-subject fallback should pass');
assert.strictEqual(hasCampaignCenterSubject(brightNearMiss), false, 'bright fallback boundary should remain strict');
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

console.log('CampaignCenterSubjectGate: OK first/second, warm, bright-fallback, failure, non-campaign');
