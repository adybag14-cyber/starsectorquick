'use strict';
const assert=require('node:assert/strict');
const {tutorialWindowConfig}=require('./tutorial-window-config');
const baseline=tutorialWindowConfig({});
assert.equal(baseline.__STARSECTOR_BROWSER_TUTORIAL__,true);
assert.equal(baseline.__STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__,'Galatia');
assert.equal(baseline.__STARSECTOR_BROWSER_GAMEPLAY_PROBE__,false);
assert.equal(Object.hasOwn(baseline,'__LWJGL_DEPTH_STATE_CACHE__'),false);
for(const value of [true,false]){
 const input={__LWJGL_DEPTH_STATE_CACHE__:value,__STARSECTOR_BROWSER_TUTORIAL__:false,
  __STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__:'Corvus',__STARSECTOR_BROWSER_GAMEPLAY_PROBE__:true,
  __STARSECTOR_FORCE_CHEERPJ_STORAGE_RESET__:true};
 const before=JSON.stringify(input),output=tutorialWindowConfig(input);
 assert.deepEqual(output,{...baseline,__LWJGL_DEPTH_STATE_CACHE__:value});
 assert.equal(JSON.stringify(input),before);
 assert.equal(Object.hasOwn(output,'__STARSECTOR_FORCE_CHEERPJ_STORAGE_RESET__'),false);
}
for(const invalid of [null,[],true,4,'true'])assert.throws(()=>tutorialWindowConfig(invalid),/Expected experiment/);
for(const invalid of [null,undefined,0,1,'true',{}])assert.throws(()=>tutorialWindowConfig({__LWJGL_DEPTH_STATE_CACHE__:invalid}),/Boolean/);
console.log('test-tutorial-window-config: OK fixed Galatia tutorial and Boolean-only renderer experiment propagation');
