'use strict';

// Keep every original event for diagnosis, but do not let an NPC with the same
// ability ID satisfy a player's keyboard/readiness/lifecycle test. The Java
// producer determines ownership by exact registered-plugin reference identity.
// instanceHash is explicitly NOT an identity/authorization token.
function createPlayerAbilityEvidence() {
  const allEvents=[];
  const playerEvents=[];
  const counts={all:0,nonAbility:0,player:0,other:0,unavailable:0,invalidOwner:0};
  return {
    allEvents,playerEvents,
    record(event) {
      if (!event || typeof event.event!=='string') throw new TypeError('Invalid gameplay event');
      allEvents.push(event);counts.all++;
      if (!event.event.startsWith('ability-')) {
        counts.nonAbility++;playerEvents.push(event);return true;
      }
      if (event.owner==='player') {counts.player++;playerEvents.push(event);return true;}
      if (event.owner==='other') counts.other++;
      else if (event.owner==='unavailable') counts.unavailable++;
      else counts.invalidOwner++;
      return false;
    },
    summary() {
      return {...counts,gateEvents:playerEvents.length,
        policy:'Only exact owner=player ability events may satisfy player gameplay gates; raw events are retained.'};
    },
  };
}
module.exports={createPlayerAbilityEvidence};
