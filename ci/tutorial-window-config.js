'use strict';

// Keep public tutorial semantics fixed. Forward only the expressly selected
// renderer experiment; never copy deep-gameplay, recovery or save-reset options.
function tutorialWindowConfig(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new Error('Expected experiment object');
  const result={__STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__:'normal',
    __STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__:'Galatia',
    __STARSECTOR_BROWSER_TUTORIAL__:true,__STARSECTOR_BROWSER_GAMEPLAY_PROBE__:false};
  if (Object.hasOwn(input,'__LWJGL_DEPTH_STATE_CACHE__')) {
    if (typeof input.__LWJGL_DEPTH_STATE_CACHE__ !== 'boolean') throw new Error('Depth cache experiment must be Boolean');
    result.__LWJGL_DEPTH_STATE_CACHE__=input.__LWJGL_DEPTH_STATE_CACHE__;
  }
  return result;
}
if (require.main===module) process.stdout.write(JSON.stringify(tutorialWindowConfig(JSON.parse(process.argv[2]||'{}'))));
module.exports={tutorialWindowConfig};
