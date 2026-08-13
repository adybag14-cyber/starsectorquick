'use strict';

function hasCampaignCenterSubject(stats) {
  return Boolean(stats
    && Number(stats.centerBrightPixels || 0) >= 150
    && (Number(stats.centerWarmPixels || 0) >= 8
      || Number(stats.centerBrightPixels || 0) >= 350));
}

function campaignCenterSubjectGate(expectedState, firstStats, secondStats) {
  if (String(expectedState || '').toLowerCase() !== 'campaign') {
    return { first: true, second: true, overall: true };
  }
  const first = hasCampaignCenterSubject(firstStats);
  const second = hasCampaignCenterSubject(secondStats);
  return { first, second, overall: first || second };
}

module.exports = {
  hasCampaignCenterSubject,
  campaignCenterSubjectGate,
};
