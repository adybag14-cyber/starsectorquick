'use strict';

function hasCampaignCenterSubject(stats) {
  if (!stats) return false;
  const bright = Number(stats.centerBrightPixels || 0);
  const warm = Number(stats.centerWarmPixels || 0);
  return (bright >= 150 && (warm >= 8 || bright >= 350))
    || (bright >= 120 && warm >= 12);
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
