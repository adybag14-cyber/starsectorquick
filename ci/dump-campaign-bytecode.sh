#!/usr/bin/env bash
set -euo pipefail

OUT=${1:-test_output/bytecode}
mkdir -p "$OUT"
CP=$(find jars -maxdepth 1 -type f -name '*.jar' -printf '%p:' | sed 's/:$//')
classes=(
  data.scripts.world.SectorGen
  com.fs.starfarer.campaign.CustomCampaignEntity
  com.fs.starfarer.campaign.BaseLocation
  com.fs.starfarer.campaign.BaseCampaignEntity
  com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl
  com.fs.starfarer.api.impl.campaign.ids.People
  com.fs.starfarer.api.util.Misc
  com.fs.starfarer.campaign.econ.Economy
  com.fs.starfarer.campaign.econ.oOOO
  com.fs.starfarer.campaign.save.CampaignGameManager
  com.fs.starfarer.campaign.CampaignEngine
  com.fs.starfarer.campaign.CampaignState
  com.fs.starfarer.campaign.Faction
  com.fs.starfarer.campaign.CharacterStats
  com.fs.starfarer.rpg.Person
  com.fs.starfarer.campaign.fleet.CampaignFleet
  com.fs.starfarer.campaign.fleet.MutableFleetStats
  com.fs.starfarer.campaign.fleet.FleetMember
  com.fs.starfarer.campaign.fleet.FleetData
  com.fs.starfarer.campaign.fleet.CampaignFleetView
  com.fs.starfarer.campaign.fleet.SmoothMovementModule
  com.fs.starfarer.campaign.fleet.SmoothFacingModule
  com.fs.starfarer.campaign.accidents.AccidentManager
  com.fs.starfarer.campaign.fleet.LogisticsModule
  com.fs.starfarer.campaign.o0OO
  com.fs.graphics.Sprite
  com.fs.starfarer.campaign.F
  com.fs.starfarer.BaseGameState
  com.fs.state.AppDriver
  com.fs.starfarer.api.Global
)
for class_name in "${classes[@]}"; do
  file_name=${class_name//./_}.javap.txt
  echo "Disassembling $class_name -> $OUT/$file_name"
  javap -classpath "$CP" -p -c -s -constants "$class_name" > "$OUT/$file_name" 2>&1 || true
done
