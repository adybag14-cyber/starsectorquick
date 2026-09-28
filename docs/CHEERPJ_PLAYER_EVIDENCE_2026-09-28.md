# Player-owned ability evidence and stock transponder settlement

Continuation of PR #183. This change repairs test telemetry/evidence, not game
ability behavior. Production and the default branch remain unchanged.

## What the completed prior run established

The previously pending `d1087c8af4a8eca4b85529e224f8ae3f9bc0f427` qualification,
run 36347632411, completed successfully: mature campaign, public tutorial, targeted
Full-HD, and the final pack audit all passed. The other three workflows passed.
The main artifact is 10941298134, SHA-256
`6b109da6850848b98f59d10e7e4ea065cd1229ce5c3cfad6caa175cc856b270e`.

The eight-window vertex-array experiment returned +1.058% median-window FPS,
-2.627% p95, +0.025% p99 (slightly worse), and -3.864% deviation-from-median jitter.
Pooled FPS was 11.5182 OFF and 11.5223 ON, so the apparent benefit depends on the
summary chosen. This does not justify a universal speedup claim or changing the
runtime default. Vertex-array coalescing remains opt-in. The earlier failed run
36346229667 (+3.012% median FPS but worse p99 and failed default gates) remains
part of the evidence, not discarded because a later run passed.

The normal gate measured 11.2864 FPS, p95 107.4ms, p99 168.6ms and jitter 23.4ms.
That gate used the existing driver configuration and unchanged thresholds. These
are application presentation intervals, not GPU-completion or display scan-out.
Different runs use different CPUs/worlds; absolute results are not a matched
before/after comparison.

## Defect 1: a shared ability name is not player identity

`BrowserGameplayProbe` already marks events with `owner=player/other/unavailable`
using exact reference comparison against the player's registered ability plugin.
The harness previously mixed all events and made readiness and cleanup decisions
using only `id`. An NPC's transponder can have the same ID as the player's.

An exact negative-control sequence fed to the existing readiness function is:

1. Player transponder emits `ability-unready`.
2. Another fleet's transponder emits `ability-ready-stable`.
3. The unfiltered readiness function returns true for the player.

The corrected harness preserves **all original events** under `gameplayEvents`
and in the raw log, while its decision stream contains non-ability events plus
only ability events explicitly owned by the player. Events lacking ownership,
unknown ownership, and NPC events cannot satisfy a player check. `instanceHash`
remains diagnostic only; it is not relied upon for uniqueness or ownership.

The result separately publishes `playerGameplayEvents` and counters describing
which events were eligible. No emitted event, failure or log is removed. The
transponder confirmation test also requires the expected ability ID, rather than
letting an unrelated player ability's state transition suppress confirmation.

This deliberately tightens evidence requirements. A missing player readiness or
settlement event should fail; an NPC event must never make it pass.

## Defect 2: the stock transponder's progress is a UI indicator

The actual stock API JAR was inspected with `javap`. It confirms:

- `BaseAbilityPlugin.isInProgress()` evaluates `getProgressFraction() > 0`.
- `TransponderAbility.getProgressFraction()` returns constant `1f`.
- `BaseToggleAbility.isActive()` returns its on/off flag; `getLevel()` returns the
  effect's fade level.
- Transponder deactivation turns off the entity's transponder flag.

Therefore the former probe predicate
`!isActive() && !isInProgress() && getLevel() <= 0.0001f` cannot report the stock
transponder settled, even after it is off and its effect has fully faded. A test
instantiates the **actual stock class**, proves that contradiction, and verifies
that the probe does not alter the stock method's result.

The new predicate, **only for the exact stock TransponderAbility class**, requires
all of: inactive, a finite nonnegative level no greater than the original 0.0001
threshold, a present entity, and that entity's transponder flag off. Other plugin
classes (including subclasses or a same-ID mock) keep the generic not-in-progress
requirement. Missing entities, throwing plugins, NaNs/infinities, negative levels,
active effects and unfinished fade-outs fail closed.

The log retains `inProgress` unchanged and adds `settled` plus `settlementBasis`
so reviewers can see the distinction. The probe never calls activate/deactivate,
changes cooldowns, or modifies stock code to hide a failed cleanup.

## Tests and compatibility

- 25 stock-class/generic settlement checks, including the old predicate's negative
  control and one-shot actual probe event emission.
- Original three-update stable-readiness tests and four exact-owner identity tests.
- 20 JavaScript evidence tests: NPC ready/unready/UI events, same-name lifecycle
  collisions, same-burst ordering, invalid ownership, exact retention and counts,
  and live-harness integration.
- Existing Full-HD early-stop test, capture-routing and experiment cleanup tests
  remain unchanged and are rerun.

Local compilation uses `--release 8` with JDK 21 and actual runtime API JARs.
Hosted compilation and the CheerpJ Java 17 campaign must separately qualify the
final candidate; a local test is not a substitute for the browser run.

No timeout, frame-time limit, image-difference criterion, renderer default,
simulation load, scene content, model of FPS, or runtime asset is relaxed by this
change. The workflow continues its same-runner paired vertex test and strict
mature/tutorial/Full-HD/pack validation.

## Scope still not certified

No exhaustive combat, mission, mod or audio parity claim is made. The existing
OpenAL stubs and five dangling stock channel-icon references remain documented.
The exact stock-class rule intentionally does not guess semantics for custom
plugins. An independently changed or missing ability registration must fail
ownership evidence rather than inheriting another instance's result.

References / source identity:
- CheerpJ release notes: https://cheerpj.com/docs/changelog
- Stock API source copy (GWT imports differ, verified against the actual JAR):
  https://github.com/adybag14-cyber/starsectorquick/blob/d1087c8af4a8eca4b85529e224f8ae3f9bc0f427/starsector-gwt/src/main/java/com/fs/starfarer/api/impl/campaign/abilities/TransponderAbility.java
- Baseline probe and harness: commit `d1087c8af4a8eca4b85529e224f8ae3f9bc0f427`.
