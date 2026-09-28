'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const {createPlayerAbilityEvidence}=require('./player-ability-evidence');
const source=fs.readFileSync('ci/campaign-render-test.js','utf8');
const context=vm.createContext({});
for(const name of ['latestAbilityReadiness','latestAbilityStableReadiness','latestAbilityUiReadiness']) {
  const begin=source.indexOf('function '+name+'(');assert.ok(begin>=0);
  const next=source.indexOf('\n}',begin)+2;
  vm.runInContext(source.slice(begin,next),context);
}
const cases=[];
const check=(name,fn)=>{fn();cases.push(name);};
const event=(type,owner,id='transponder',extra={})=>({event:type,id,owner,instanceHash:'collision',...extra});
check('NPC readiness cannot override unready player',()=>{
  const x=createPlayerAbilityEvidence();
  const input=[event('ability-unready','player'),event('ability-ready-stable','other')];
  input.forEach(row=>x.record(row));
  // The original unfiltered source reproducibly returns true on this sequence.
  assert.equal(context.latestAbilityStableReadiness(input,'transponder'),true);
  assert.equal(context.latestAbilityStableReadiness(x.playerEvents,'transponder'),false);
  assert.deepEqual(x.allEvents,input);
});
check('NPC unready cannot override ready player',()=>{
  const x=createPlayerAbilityEvidence();[event('ability-ready-stable','player'),event('ability-unready','other')].forEach(e=>x.record(e));
  assert.equal(context.latestAbilityStableReadiness(x.playerEvents,'transponder'),true);
});
check('same-name NPC lifecycle cannot satisfy player cleanup',()=>{
  const x=createPlayerAbilityEvidence();x.record(event('ability-deactivate','player'));x.record(event('ability-settled','other'));
  assert.equal(x.playerEvents.some(e=>e.event==='ability-settled'),false);
  x.record(event('ability-settled','player'));assert.equal(x.playerEvents.at(-1).event,'ability-settled');
});
for(const type of ['ability-press','ability-activate','ability-deactivate','ability-ready','ability-ready-stable','ability-unready','ability-settled','ability-ui-ready','ability-ui-unready','ability-ui-action']) {
  check(type+' requires exact player owner',()=>{
    const x=createPlayerAbilityEvidence();
    for(const owner of ['other','unavailable',undefined,'PLAYER','player-other',''])assert.equal(x.record(event(type,owner)),false);
    assert.equal(x.record(event(type,'player')),true);assert.equal(x.playerEvents.length,1);assert.equal(x.allEvents.length,7);
  });
}
check('instance hash does not override explicit owner',()=>{
  const x=createPlayerAbilityEvidence();x.record(event('ability-ready','player'));x.record(event('ability-unready','other'));
  assert.equal(context.latestAbilityReadiness(x.playerEvents,'transponder'),true);
});
check('UI readiness cannot come from unrelated fleet',()=>{
  const x=createPlayerAbilityEvidence();x.record(event('ability-ui-unready','player'));x.record(event('ability-ui-ready','other'));
  assert.equal(context.latestAbilityUiReadiness(x.playerEvents,'transponder'),false);
});
check('non-ability UI/gameplay and event object identity retained',()=>{
  const x=createPlayerAbilityEvidence();const e={event:'core-tab-ready',tab:'FLEET'};x.record(e);
  assert.equal(x.playerEvents[0],e);assert.equal(x.allEvents[0],e);
});
check('same-burst cleanup preserves deactivation-before-settle order',()=>{
  const x=createPlayerAbilityEvidence();
  [event('ability-activate','other'),event('ability-deactivate','player'),event('ability-settled','other'),event('ability-settled','player')].forEach(e=>x.record(e));
  assert.deepEqual(x.playerEvents.map(e=>e.event),['ability-deactivate','ability-settled']);
  assert.equal(x.playerEvents.slice(1).length,1);
});
check('counts account for all events and cannot be externally mutated',()=>{
  const x=createPlayerAbilityEvidence();[event('ability-ready','player'),event('ability-ready','other'),event('ability-ready','unavailable'),event('ability-ready',undefined),{event:'core-tab-ready'}].forEach(e=>x.record(e));
  const s=x.summary();assert.equal(s.all,5);assert.equal(s.player+s.other+s.unavailable+s.invalidOwner+s.nonAbility,5);
  assert.equal(s.gateEvents,2);s.all=999;assert.equal(x.summary().all,5);
});
check('invalid input fails closed',()=>{assert.throws(()=>createPlayerAbilityEvidence().record(null));});
if(process.env.STARSECTOR_VERIFY_OWNERSHIP_INTEGRATION==='true') {
  check('harness stores all events but gates on player projection',()=>{
    assert.match(source,/const gameplayEvents = gameplayEvidence\.playerEvents/);
    assert.match(source,/gameplayEvidence\.record\(event\)/);
    assert.match(source,/gameplayEvents: gameplayEvidence\.allEvents/);
    assert.match(source,/playerGameplayEvents: gameplayEvents/);
    assert.doesNotMatch(source,/gameplayEvents\.push\(event\)/);
  });
}
console.log('test-player-ability-evidence: OK '+cases.length+' cases; original NPC-readiness false positive reproduced and rejected');
