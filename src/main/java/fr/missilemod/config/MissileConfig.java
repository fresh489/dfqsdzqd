package fr.missilemod.config;

import fr.missilemod.explosion.ImpactMode;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration cote serveur : fichier serverconfig/missilemod-server.toml dans le dossier du monde.
 */
public final class MissileConfig {
    public static final ForgeConfigSpec SPEC;

    // Vol
    public static final ForgeConfigSpec.DoubleValue ASCENT_SPEED;
    public static final ForgeConfigSpec.DoubleValue CRUISE_SPEED;
    public static final ForgeConfigSpec.IntValue CRUISE_ALTITUDE;
    public static final ForgeConfigSpec.IntValue MAX_RANGE;
    public static final ForgeConfigSpec.IntValue MAX_FLIGHT_SECONDS;

    // Explosion
    public static final ForgeConfigSpec.EnumValue<ImpactMode> IMPACT_MODE;
    public static final ForgeConfigSpec.DoubleValue POWER_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue DESTROY_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue RESPECT_MOB_GRIEFING;
    public static final ForgeConfigSpec.DoubleValue BLAST_RESISTANCE_LIMIT;
    public static final ForgeConfigSpec.IntValue BLOCKS_PER_TICK;

    // Effets
    public static final ForgeConfigSpec.BooleanValue DEBRIS;
    public static final ForgeConfigSpec.IntValue MAX_DEBRIS;
    public static final ForgeConfigSpec.BooleanValue SCORCHED_GROUND;
    public static final ForgeConfigSpec.BooleanValue EDGE_FIRES;
    public static final ForgeConfigSpec.BooleanValue LINGERING_SMOKE;
    public static final ForgeConfigSpec.BooleanValue SECONDARY_IMPACTS;
    public static final ForgeConfigSpec.BooleanValue CAMERA_SHAKE;
    public static final ForgeConfigSpec.BooleanValue DELAYED_SOUND;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_MULTIPLIER;

    // [munitions]
    public static final ForgeConfigSpec.DoubleValue DUD_CHANCE;
    public static final ForgeConfigSpec.IntValue ALARM_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ARTILLERY_RANGE_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Vol du missile").push("flight");
        ASCENT_SPEED = b.comment("Vitesse d'ascension verticale, en blocs par seconde")
                .defineInRange("ascentBlocksPerSecond", 20.0, 1.0, 100.0);
        CRUISE_SPEED = b.comment("Vitesse de croisiere, en blocs par seconde")
                .defineInRange("cruiseBlocksPerSecond", 45.0, 1.0, 100.0);
        CRUISE_ALTITUDE = b.comment("Altitude de croisiere au-dessus du point le plus haut entre le depart et la cible")
                .defineInRange("cruiseAltitude", 40, 5, 256);
        MAX_RANGE = b.comment("Distance maximale entre le missile et la cible, en blocs")
                .defineInRange("maxRange", 500, 16, 30000);
        MAX_FLIGHT_SECONDS = b.comment("Auto-destruction apres ce nombre de secondes de vol")
                .defineInRange("maxFlightSeconds", 60, 5, 600);
        b.pop();

        b.comment("Explosion").push("explosion");
        IMPACT_MODE = b.comment(
                        "AUTO   : HYBRID si Explosion Overhaul est installe, sinon CUSTOM.",
                        "HYBRID : vraie explosion Minecraft (reprise par Explosion Overhaul) + debris, sol brule, fumee du mod.",
                        "CUSTOM : tout est gere par le mod (cratere irregulier, degats, tremblement, son retarde).")
                .defineEnum("impactMode", ImpactMode.AUTO);
        POWER_MULTIPLIER = b.comment("Multiplicateur de puissance des explosions en mode HYBRID",
                        "Explosion Overhaul deconseille les puissances superieures a 50.")
                .defineInRange("powerMultiplier", 1.0, 0.1, 5.0);
        DESTROY_BLOCKS = b.comment("Si false, aucune munition ne detruit ni ne modifie de blocs (utile sur un serveur)")
                .define("destroyBlocks", true);
        RESPECT_MOB_GRIEFING = b.comment("Si true, aucun bloc n'est detruit quand la gamerule mobGriefing vaut false")
                .define("respectMobGriefing", true);
        BLAST_RESISTANCE_LIMIT = b.comment(
                        "Mode CUSTOM : les blocs dont la resistance aux explosions est STRICTEMENT superieure sont epargnes.",
                        "Reperes vanilla : pierre 6, obsidienne 1200, bedrock/barriere 3600000.")
                .defineInRange("blastResistanceLimit", 1500.0, 0.0, 1.0E7);
        BLOCKS_PER_TICK = b.comment("Mode CUSTOM : nombre maximal de blocs detruits par tick (etalement de la charge)")
                .defineInRange("blocksPerTick", 2000, 100, 50000);
        b.pop();

        b.comment("Effets d'impact").push("effects");
        DEBRIS = b.comment("Debris : vrais blocs projetes qui retombent et se posent")
                .define("debris", true);
        MAX_DEBRIS = b.comment("Nombre maximal de debris par impact (performance)")
                .defineInRange("maxDebris", 80, 0, 500);
        SCORCHED_GROUND = b.comment("Sol brule dans et autour du cratere (terre brute, gravier, blackstone)")
                .define("scorchedGround", true);
        EDGE_FIRES = b.comment("Quelques feux sur le bord du cratere")
                .define("edgeFires", true);
        LINGERING_SMOKE = b.comment("Fumee persistante apres l'impact")
                .define("lingeringSmoke", true);
        SECONDARY_IMPACTS = b.comment("Petits impacts secondaires autour des gros calibres")
                .define("secondaryImpacts", true);
        CAMERA_SHAKE = b.comment("Mode CUSTOM : tremblement de camera (en HYBRID, c'est Explosion Overhaul qui le gere)")
                .define("cameraShake", true);
        DELAYED_SOUND = b.comment("Mode CUSTOM : son retarde selon la distance (vitesse du son)")
                .define("delayedSound", true);
        PARTICLE_MULTIPLIER = b.comment("Multiplicateur du nombre de particules (0.5 = moitie moins, pour les petits PC)")
                .defineInRange("particleMultiplier", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Munitions et equipements").push("munitions");
        DUD_CHANCE = b.comment("Probabilite qu'un missile soit rate et tombe sans exploser (0.02 = 1 sur 50)")
                .defineInRange("dudChance", 0.02, 0.0, 1.0);
        ALARM_RADIUS = b.comment("Rayon de detection du bloc d'alerte anti-missile, en blocs")
                .defineInRange("alarmRadius", 200, 32, 512);
        ARTILLERY_RANGE_MULTIPLIER = b.comment("Multiplicateur de la portee maximale du mortier et des obusiers",
                        "Portees de base : mortier 320, M777 105 mm 700, M777ER 155 mm 1200 blocs")
                .defineInRange("artilleryRangeMultiplier", 1.0, 0.1, 5.0);
        b.pop();

        SPEC = b.build();
    }

    private MissileConfig() {
    }
}
