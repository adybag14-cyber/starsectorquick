package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.CoreScript;
import com.fs.starfarer.api.impl.campaign.events.CoreEventProbabilityManager;
import com.fs.starfarer.api.impl.campaign.fleets.DisposableLuddicPathFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.DisposablePirateFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.MercFleetManagerV2;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.List;

import data.hullmods.HeavyArmor;
import data.scripts.world.corvus.Corvus;
import data.scripts.world.systems.AlGebbar;
import data.scripts.world.systems.Arcadia;
import data.scripts.world.systems.Askonia;
import data.scripts.world.systems.Aztlan;
import data.scripts.world.systems.Canaan;
import data.scripts.world.systems.Duzahk;
import data.scripts.world.systems.Eos;
import data.scripts.world.systems.Galatia;
import data.scripts.world.systems.Hybrasil;
import data.scripts.world.systems.Isirah;
import data.scripts.world.systems.KumariKandam;
import data.scripts.world.systems.Magec;
import data.scripts.world.systems.Mayasura;
import data.scripts.world.systems.Naraka;
import data.scripts.world.systems.Penelope;
import data.scripts.world.systems.Samarra;
import data.scripts.world.systems.Thule;
import data.scripts.world.systems.TiaTaxet;
import data.scripts.world.systems.Tyle;
import data.scripts.world.systems.Valhalla;
import data.scripts.world.systems.Westernesse;
import data.scripts.world.systems.Yma;
import data.scripts.world.systems.Zagan;

public class SectorGen implements SectorGeneratorPlugin {

	public void generate(SectorAPI sector) {
		//ClassLoader cl = Global.getSettings().getScriptClassLoader();
		
		StarSystemAPI system = sector.createStarSystem("Corvus");
		//system.getLocation().set(16000 - 8000, 9000 - 10000);
		system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
		
		//sector.setCurrentLocation(system);
		sector.setRespawnLocation(system);
		sector.getRespawnCoordinates().set(-2500, -3500);
		
		initFactionRelationships(sector);
		
		new Galatia().generate(sector);
		new Askonia().generate(sector);
		new Eos().generate(sector);
		new Valhalla().generate(sector);
		new Arcadia().generate(sector);
		new Magec().generate(sector);
		new Corvus().generate(sector);
		new Aztlan().generate(sector);
		new Samarra().generate(sector);
		new Penelope().generate(sector);
		new Yma().generate(sector);
		new Hybrasil().generate(sector);
		new Duzahk().generate(sector);
		new TiaTaxet().generate(sector);
		new Canaan().generate(sector);
		new AlGebbar().generate(sector);
		new Isirah().generate(sector);
		new KumariKandam().generate(sector);
		new Naraka().generate(sector);
		new Thule().generate(sector);
		new Mayasura().generate(sector);
		new Zagan().generate(sector);
		new Westernesse().generate(sector);
		new Tyle().generate(sector);
		
		LocationAPI hyper = Global.getSector().getHyperspace();
		SectorEntityToken atlanticLabel = hyper.addCustomEntity("atlantic_label_id", null, "atlantic_label", null);
		SectorEntityToken perseanLabel = hyper.addCustomEntity("persean_label_id", null, "persean_label", null);
		SectorEntityToken luddicLabel = hyper.addCustomEntity("luddic_label_id", null, "luddic_label", null);
		SectorEntityToken zinLabel = hyper.addCustomEntity("zin_label_id", null, "zin_label", null);
		SectorEntityToken abyssLabel = hyper.addCustomEntity("opabyss_label_id", null, "opabyss_label", null);
		SectorEntityToken telmunLabel = hyper.addCustomEntity("telmun_label_id", null, "telmun_label", null);
		SectorEntityToken cathedralLabel = hyper.addCustomEntity("cathedral_label_id", null, "cathedral_label", null);
		SectorEntityToken coreLabel = hyper.addCustomEntity("core_label_id", null, "core_label", null);
		
		atlanticLabel.setFixedLocation(500, -2000);
		perseanLabel.setFixedLocation(-10000, 1000);
		luddicLabel.setFixedLocation(-14000, -9500);
		zinLabel.setFixedLocation(-22000, -17000); 
		telmunLabel.setFixedLocation(-16000, 0);
		cathedralLabel.setFixedLocation(-12700, -12000);
		coreLabel.setFixedLocation(0, -6000);
		
		abyssLabel.setFixedLocation(-65000, -47000);
		
		/*SectorEntityToken deep_hyperspace_test = Global.getSector().getHyperspace().addTerrain(Terrain.NEBULA, new BaseTiledTerrain.TileParams(
				"   xx     " +
				"   xxx    " +
				"  xxx x   " +
				"  xx   x  " +
				" xxxx xxx " +
				"  xxxxxxx " +
				" xxxxxxxxx" +
				" xxxxxxxxx" +
				"  xxxxxxx " +
				" xxxxxxx  " +
				" x xxxxx  " +
				"  xxxxxx  " +
				" xxxx xxx " +
				"xxxx  xxx " +
				" xxxx     " +
				"xxxxxxxxx " +
				"  xxxxxxxx" +
				" xxxxxxxxx" +
				"  xxxxxxx " +
				"   xxx    ",
				10, 20, // size of the nebula grid, should match above string
				"terrain", "deep_hyperspace", 4, 4));
		
		deep_hyperspace_test.getLocation().set(5000,5000);*/
		
		
		SectorEntityToken deep_hyperspace = Misc.addNebulaFromPNG("data/campaign/terrain/hyperspace_map.png",
		//SectorEntityToken deep_hyperspace = Misc.addNebulaFromPNG("data/campaign/terrain/hyperspace_map_filled.png",
				  0, 0, // center of nebula
				  Global.getSector().getHyperspace(), // location to add to
				  "terrain", "deep_hyperspace", // "nebula_blue", // texture to use, uses xxx_map for map
				  4, 4, Terrain.HYPERSPACE, null); // number of cells in texture
		
		
		
		// ensure area around stars is clear
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2f;
		for (StarSystemAPI curr : sector.getStarSystems()) {
			float radius = curr.getMaxRadiusInHyperspace() * 0.5f;
			editor.clearArc(curr.getLocation().x, curr.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
			editor.clearArc(curr.getLocation().x, curr.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
		}
		
		
		
//		PirateSpawnPoint pirateSpawn = new PirateSpawnPoint(sector, sector.getHyperspace(), 1, 15, system.getHyperspaceAnchor());
//		system.addSpawnPoint(pirateSpawn);
//		for (int i = 0; i < 2; i++) {
//			pirateSpawn.spawnFleet();
//		}
		
		// need to do this after hyperspace terrain exists
		//SectorProcGen.generate();
		// this is done through settings.json, "plugins"->"newGameSectorProcGen"
		
		sector.registerPlugin(new CoreCampaignPluginImpl());
		sector.addScript(new CoreScript());
		sector.addScript(new CoreEventProbabilityManager());
		
		sector.addScript(new EconomyFleetRouteManager());
		//sector.addScript(new MercFleetManager());
		sector.addScript(new MercFleetManagerV2());
		
		
		sector.addScript(new DisposablePirateFleetManager());
		sector.addScript(new DisposableLuddicPathFleetManager());
		
//		sector.addScript(new LuddicPathFleetManager());
//		sector.addScript(new PirateFleetManager());
//		sector.addScript(new BountyPirateFleetManager());
		
	}
	
	public static void initFactionRelationships(SectorAPI sector) {
		
		
		// forget why this is necessary - workaround for some JANINO issue, I think
		Class c = HeavyArmor.class;
		
		FactionAPI hegemony = getFactionOrNull(sector, Factions.HEGEMONY);
		FactionAPI tritachyon = getFactionOrNull(sector, Factions.TRITACHYON);
		FactionAPI pirates = getFactionOrNull(sector, Factions.PIRATES);
		FactionAPI independent = getFactionOrNull(sector, Factions.INDEPENDENT);
		FactionAPI kol = getFactionOrNull(sector, Factions.KOL);
		FactionAPI church = getFactionOrNull(sector, Factions.LUDDIC_CHURCH);
		FactionAPI path = getFactionOrNull(sector, Factions.LUDDIC_PATH);
		FactionAPI player = getFactionOrNull(sector, Factions.PLAYER);
		FactionAPI diktat = getFactionOrNull(sector, Factions.DIKTAT);
		FactionAPI persean = getFactionOrNull(sector, Factions.PERSEAN);
		FactionAPI remnant = getFactionOrNull(sector, Factions.REMNANTS);
		FactionAPI threat = getFactionOrNull(sector, Factions.THREAT);
		FactionAPI dweller = getFactionOrNull(sector, Factions.DWELLER);
		FactionAPI derelict = getFactionOrNull(sector, Factions.DERELICT);
		
		List<String> missing = new ArrayList<String>();
		if (hegemony == null) missing.add(Factions.HEGEMONY);
		if (tritachyon == null) missing.add(Factions.TRITACHYON);
		if (pirates == null) missing.add(Factions.PIRATES);
		if (independent == null) missing.add(Factions.INDEPENDENT);
		if (kol == null) missing.add(Factions.KOL);
		if (church == null) missing.add(Factions.LUDDIC_CHURCH);
		if (path == null) missing.add(Factions.LUDDIC_PATH);
		if (player == null) missing.add(Factions.PLAYER);
		if (diktat == null) missing.add(Factions.DIKTAT);
		if (persean == null) missing.add(Factions.PERSEAN);
		if (remnant == null) missing.add(Factions.REMNANTS);
		if (threat == null) missing.add(Factions.THREAT);
		if (dweller == null) missing.add(Factions.DWELLER);
		if (derelict == null) missing.add(Factions.DERELICT);
		if (!missing.isEmpty()) {
			try {
				Global.getLogger(SectorGen.class).warn("SectorGen: missing factions for relationship init: " + missing);
			} catch (Throwable ignored) {
			}
		}
		
		setRelationship(player, hegemony, 0f);
		setRelationship(player, tritachyon, 0f);
		setRelationship(player, persean, 0f);
		//player.setRelationship(pirates.getId(), RepLevel.HOSTILE);
		setRelationship(player, pirates, -0.65f);
		
		setRelationship(player, independent, 0f);
		setRelationship(player, kol, 0f);
		setRelationship(player, church, 0f);
		//player.setRelationship(path.getId(), RepLevel.HOSTILE);
		setRelationship(player, path, -0.65f);
		

		// replaced by hostilities set in CoreLifecyclePluginImpl
		//hegemony.setRelationship(tritachyon.getId(), RepLevel.HOSTILE);
		//hegemony.setRelationship(persean.getId(), RepLevel.HOSTILE);
		
		setRelationship(hegemony, pirates, RepLevel.HOSTILE);
		
		setRelationship(tritachyon, pirates, RepLevel.HOSTILE);
		//tritachyon.setRelationship(independent.getId(), -1);
		setRelationship(tritachyon, kol, RepLevel.HOSTILE);
		//tritachyon.setRelationship(church.getId(), RepLevel.HOSTILE);
		setRelationship(tritachyon, path, RepLevel.HOSTILE);
		setRelationship(tritachyon, persean, RepLevel.SUSPICIOUS);
		
		setRelationship(pirates, kol, RepLevel.HOSTILE);
		setRelationship(pirates, church, RepLevel.HOSTILE);
		setRelationship(pirates, path, 0f);
		setRelationship(pirates, independent, RepLevel.HOSTILE);
		setRelationship(pirates, diktat, RepLevel.HOSTILE);
		setRelationship(pirates, persean, RepLevel.HOSTILE);
		
		setRelationship(church, kol, RepLevel.COOPERATIVE);
		setRelationship(path, kol, RepLevel.FAVORABLE);
		
		setRelationship(path, independent, RepLevel.HOSTILE);
		setRelationship(path, hegemony, RepLevel.HOSTILE);
		setRelationship(path, diktat, RepLevel.HOSTILE);
		setRelationship(path, persean, RepLevel.HOSTILE);
		setRelationship(path, church, RepLevel.COOPERATIVE);
		
		setRelationship(persean, tritachyon, RepLevel.SUSPICIOUS);
		setRelationship(persean, pirates, RepLevel.HOSTILE);
		setRelationship(persean, path, RepLevel.HOSTILE);
		setRelationship(persean, diktat, RepLevel.COOPERATIVE);
		
		setRelationship(player, remnant, RepLevel.HOSTILE);
		setRelationship(independent, remnant, RepLevel.HOSTILE);
		setRelationship(pirates, remnant, RepLevel.HOSTILE);
		setRelationship(hegemony, remnant, RepLevel.HOSTILE);
		setRelationship(kol, remnant, RepLevel.HOSTILE);
		setRelationship(church, remnant, RepLevel.HOSTILE);
		setRelationship(path, remnant, RepLevel.HOSTILE);
		setRelationship(diktat, remnant, RepLevel.HOSTILE);
		setRelationship(persean, remnant, RepLevel.HOSTILE);
		
		setRelationship(player, threat, RepLevel.HOSTILE);
		setRelationship(independent, threat, RepLevel.HOSTILE);
		setRelationship(pirates, threat, RepLevel.HOSTILE);
		setRelationship(hegemony, threat, RepLevel.HOSTILE);
		setRelationship(kol, threat, RepLevel.HOSTILE);
		setRelationship(church, threat, RepLevel.HOSTILE);
		setRelationship(path, threat, RepLevel.HOSTILE);
		setRelationship(diktat, threat, RepLevel.HOSTILE);
		setRelationship(persean, threat, RepLevel.HOSTILE);
		setRelationship(remnant, threat, RepLevel.HOSTILE);
		setRelationship(derelict, threat, RepLevel.HOSTILE);
		setRelationship(tritachyon, threat, RepLevel.HOSTILE);
		
		setRelationship(player, dweller, RepLevel.HOSTILE);
		setRelationship(independent, dweller, RepLevel.HOSTILE);
		setRelationship(pirates, dweller, RepLevel.HOSTILE);
		setRelationship(hegemony, dweller, RepLevel.HOSTILE);
		setRelationship(kol, dweller, RepLevel.HOSTILE);
		setRelationship(church, dweller, RepLevel.HOSTILE);
		setRelationship(path, dweller, RepLevel.HOSTILE);
		setRelationship(diktat, dweller, RepLevel.HOSTILE);
		setRelationship(persean, dweller, RepLevel.HOSTILE);
		setRelationship(remnant, dweller, RepLevel.HOSTILE);
		setRelationship(derelict, dweller, RepLevel.HOSTILE);
		setRelationship(threat, dweller, RepLevel.HOSTILE);
		setRelationship(tritachyon, dweller, RepLevel.HOSTILE);
		
//		independent.setRelationship(hegemony.getId(), 0);
//		independent.setRelationship(tritachyon.getId(), 0);
//		independent.setRelationship(pirates.getId(), 0);
//		independent.setRelationship(independent.getId(), 0);
//		independent.setRelationship(player.getId(), 0);
		
	}
	
	private static FactionAPI getFactionOrNull(SectorAPI sector, String id) {
		if (sector == null || id == null) return null;
		try {
			return sector.getFaction(id);
		} catch (Throwable ignored) {
			return null;
		}
	}
	
	private static void setRelationship(FactionAPI source, FactionAPI target, float value) {
		if (source == null || target == null) return;
		String id = target.getId();
		if (id == null || id.isEmpty()) return;
		source.setRelationship(id, value);
	}
	
	private static void setRelationship(FactionAPI source, FactionAPI target, RepLevel level) {
		if (source == null || target == null || level == null) return;
		String id = target.getId();
		if (id == null || id.isEmpty()) return;
		source.setRelationship(id, level);
	}
}
