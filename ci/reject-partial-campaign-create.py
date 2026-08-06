#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
fixer_path = ROOT / 'jars' / 'Fixer.java'
text = fixer_path.read_text(encoding='utf-8-sig')

old = '''            if (isCampaignGameManagerNpe(t) && isSyntheticPlayerFleetFallbackEnabled()) {
                String recoveryReadiness = checkDirectNewGameRuntimeReadiness();
                if (recoveryReadiness == null) {
                    directNewGameLastNullMarker = "campaign-newgame-npe-synthetic-recovered";
                    System.out.println(
                            "Fixer: direct new-game campaign-manager NPE recovered via synthetic partial runtime readiness.");
                    return null;
                }
                System.out.println(
                        "Fixer: direct new-game campaign-manager NPE synthetic recovery readiness="
                                + String.valueOf(recoveryReadiness));
            }
'''
new = '''            if (isCampaignGameManagerNpe(t) && isSyntheticPlayerFleetFallbackEnabled()) {
                String recoveryReadiness = checkDirectNewGameRuntimeReadiness();
                System.out.println(
                        "Fixer: direct new-game campaign-manager NPE is not accepted as successful creation; "
                                + "synthetic readiness="
                                + String.valueOf(recoveryReadiness)
                                + ". Logging the original create() failure instead of entering a partial CampaignState.");
            }
'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f'campaign-manager synthetic recovery block: expected exactly one match, found {count}')
text = text.replace(old, new, 1)

# Runtime readiness is a validator in the strict campaign path. It must not
# create the state it is supposed to validate. Leave the recovery helpers in
# Fixer for explicitly requested interactive diagnostics, but make this strict
# readiness call observational: a missing real fleet stays missing.
old_runtime_fleet = '''            if (playerFleet == null && isSyntheticPlayerFleetFallbackEnabled()) {
                Object synthesizedFleet =
                        tryCreateSyntheticPlayerFleetForReadiness(sector, "runtime-readiness");
                if (synthesizedFleet != null) {
                    playerFleet = synthesizedFleet;
                }
            }
'''
new_runtime_fleet = '''            if (playerFleet == null && isSyntheticPlayerFleetFallbackEnabled()) {
                System.out.println(
                        "Fixer: strict runtime readiness observed player-fleet-null; synthetic fleet recovery suppressed.");
            }
'''
count = text.count(old_runtime_fleet)
if count != 1:
    raise RuntimeError(f'runtime-readiness synthetic fleet block: expected exactly one match, found {count}')
text = text.replace(old_runtime_fleet, new_runtime_fleet, 1)

# Likewise, do not seed a fallback market merely to make an empty lightweight
# world look ready. Report the real campaign-world population state and let the
# caller decide whether creation is genuinely complete.
old_runtime_world = '''            String worldPopulationIssue =
                    ensureCampaignWorldPopulatedForReadiness(
                            sector, economy, "runtime-readiness");
'''
new_runtime_world = '''            String worldPopulationIssue =
                    checkCampaignWorldPopulationForPlayerFleetNullTransition(null);
'''
count = text.count(old_runtime_world)
if count != 1:
    raise RuntimeError(f'runtime-readiness fallback market block: expected exactly one match, found {count}')
text = text.replace(old_runtime_world, new_runtime_world, 1)

# Title Screen State becomes renderable while ResourceLoaderState is still filling
# SpecStore on a background loader thread. Starting CampaignGameManager.create()
# at that moment races the loader and produces a half-built sector. In
# non-mutating mode the original code explicitly ignored the missing core-spec
# baseline. Make the baseline a real readiness gate instead: do not mutate it and
# do not enter create() until the official loader has registered the sentinels.
old_baseline = '''        if (!allowMutatingSpecPreflight()) {
            long now = System.currentTimeMillis();
            if (autoCampaignCoreSpecBaselinePendingSince <= 0L) {
                autoCampaignCoreSpecBaselinePendingSince = now;
            }
            autoCampaignCoreSpecBaselinePendingCount++;
            if (autoCampaignCoreSpecBaselinePendingCount == 1
                    || autoCampaignCoreSpecBaselinePendingCount % 30 == 0) {
                System.out.println(
                        "Fixer: direct-new-game non-mutating mode defers core spec baseline to ResourceLoaderState.init; proceeding without pre-loader SpecStore warmup: "
                                + baselineIssue);
            }
            return null;
        }
'''
new_baseline = '''        if (!allowMutatingSpecPreflight()) {
            long now = System.currentTimeMillis();
            if (autoCampaignCoreSpecBaselinePendingSince <= 0L) {
                autoCampaignCoreSpecBaselinePendingSince = now;
            }
            autoCampaignCoreSpecBaselinePendingCount++;
            long pendingMs = Math.max(0L, now - autoCampaignCoreSpecBaselinePendingSince);
            if (autoCampaignCoreSpecBaselinePendingCount == 1
                    || autoCampaignCoreSpecBaselinePendingCount % 20 == 0) {
                System.out.println(
                        "Fixer: direct-new-game waiting for official ResourceLoaderState core-spec baseline before create (pending="
                                + pendingMs
                                + "ms): "
                                + baselineIssue);
            }
            return "core spec baseline waiting for ResourceLoaderState ("
                    + pendingMs
                    + "ms): "
                    + baselineIssue;
        }
'''
count = text.count(old_baseline)
if count != 1:
    raise RuntimeError(f'non-mutating core-spec baseline block: expected exactly one match, found {count}')
text = text.replace(old_baseline, new_baseline, 1)

# If create nevertheless reaches procgen after the official baseline is present,
# repair only the age/star tables used by pickNebulaAndBackground. This is much
# narrower than the old broad/mutating preflight and avoids hull/variant pollution.
old_procgen = '''                if (!allowMutatingSpecPreflight()) {
                    String tempPlanetCleanupIssue =
                            cleanupTemporaryPlanetSpecsForDirectNewGame(
                                    "procgen-nebula-background");
                    maybeLogNewGamePreflightIssue(
                            "procgen nebula/background observed in non-mutating mode; deferring procgen preload to ResourceLoaderState.init. tempPlanetCleanup="
                                    + String.valueOf(tempPlanetCleanupIssue));
                    return "direct new-game preflight pending";
                }
                String ageIssue = ensureAgeGenSpecsReadyForDirectNewGame();
'''
new_procgen = '''                if (!allowMutatingSpecPreflight()) {
                    String tempPlanetCleanupIssue =
                            cleanupTemporaryPlanetSpecsForDirectNewGame(
                                    "procgen-nebula-background");
                    String ageIssue = ensureAgeGenSpecsReadyForDirectNewGame();
                    String starIssue = ensureStarGenSpecsReadyForDirectNewGame();
                    String pickerIssue = refreshProcgenBackgroundPickersForDirectNewGame();
                    if (ageIssue == null && starIssue == null && pickerIssue == null) {
                        maybeLogNewGamePreflightIssue(
                                "procgen nebula/background targeted runtime repair succeeded in non-mutating mode; preserving repaired age/star tables for retry. tempPlanetCleanup="
                                        + String.valueOf(tempPlanetCleanupIssue));
                    } else {
                        maybeLogNewGamePreflightIssue(
                                "procgen nebula/background targeted runtime repair issues in non-mutating mode: age="
                                        + String.valueOf(ageIssue)
                                        + ", star="
                                        + String.valueOf(starIssue)
                                        + ", picker="
                                        + String.valueOf(pickerIssue)
                                        + ", tempPlanetCleanup="
                                        + String.valueOf(tempPlanetCleanupIssue));
                    }
                    return "direct new-game preflight pending";
                }
                String ageIssue = ensureAgeGenSpecsReadyForDirectNewGame();
'''
count = text.count(old_procgen)
if count != 1:
    raise RuntimeError(f'procgen targeted-repair block: expected exactly one match, found {count}')
text = text.replace(old_procgen, new_procgen, 1)

fixer_path.write_text(text, encoding='utf-8', newline='\n')
print('Disabled synthetic-success recovery for CampaignGameManager.create() NPEs')
print('Made strict direct-new-game runtime readiness observation-only')
print('Made official ResourceLoaderState core-spec baseline mandatory before direct create()')
print('Enabled targeted age/star procgen repair for non-mutating browser campaign retries')
