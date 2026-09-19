package fr.missilemod.explosion;

import fr.missilemod.explosion.ImpactProfile.SmokeType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Profils d'impact de toutes les munitions (cahier des charges, parties 1 et 2).
 * Testables avec /missileimpact <nom>.
 */
public final class ImpactProfiles {

    //                  nom            puiss. crat. debr. fumee type             colonne shake  second. degats rayon  frag
    // --- Missiles (2.1) ---
    public static final ImpactProfile LIGHT = new ImpactProfile("leger", 3.0F, 3, 6, 4, SmokeType.LIGHT, false, 0.3F, 0, 20.0F, 6, false);
    public static final ImpactProfile MEDIUM = new ImpactProfile("moyen", 6.0F, 6, 20, 7, SmokeType.DARK, false, 0.6F, 0, 40.0F, 12, false);
    public static final ImpactProfile HEAVY = new ImpactProfile("lourd", 10.0F, 10, 40, 14, SmokeType.DARK, false, 1.0F, 3, 70.0F, 18, false);
    public static final ImpactProfile STRATEGIC = new ImpactProfile("strategique", 18.0F, 17, 80, 25, SmokeType.DUST, true, 1.6F, 6, 120.0F, 30, false);

    // --- Fragmentation (2.2) : petit cratere, gros degats aux entites dans 8-10 blocs ---
    public static final ImpactProfile FRAG_LIGHT = new ImpactProfile("frag_leger", 2.0F, 2, 4, 3, SmokeType.DARK, false, 0.3F, 0, 45.0F, 8, true);
    public static final ImpactProfile FRAG_HEAVY = new ImpactProfile("frag_lourd", 3.5F, 3, 8, 5, SmokeType.DARK, false, 0.5F, 2, 80.0F, 10, true);

    // --- Obus d'artillerie (2.3) ---
    public static final ImpactProfile MORTAR = new ImpactProfile("mortier", 3.5F, 3, 8, 5, SmokeType.DARK, false, 0.35F, 0, 30.0F, 7, false);
    public static final ImpactProfile SHELL_105_HE = new ImpactProfile("obus_105", 5.0F, 4, 14, 7, SmokeType.DARK, false, 0.5F, 0, 40.0F, 9, false);
    public static final ImpactProfile SHELL_105_FRAG = new ImpactProfile("obus_105_frag", 2.5F, 2, 6, 4, SmokeType.DARK, false, 0.35F, 0, 55.0F, 9, true);
    public static final ImpactProfile SHELL_155_HE = new ImpactProfile("obus_155", 7.5F, 6, 24, 10, SmokeType.DARK, false, 0.8F, 1, 60.0F, 13, false);
    public static final ImpactProfile SHELL_155_FRAG = new ImpactProfile("obus_155_frag", 3.5F, 3, 8, 5, SmokeType.DARK, false, 0.5F, 0, 80.0F, 11, true);

    // --- Mines (2.4) : gros degats, petite explosion ---
    public static final ImpactProfile MINE_AP = new ImpactProfile("mine", 2.0F, 1, 3, 3, SmokeType.DUST, false, 0.3F, 0, 40.0F, 4, false);
    public static final ImpactProfile MINE_AT = new ImpactProfile("mine_antichar", 5.0F, 3, 12, 6, SmokeType.DUST, false, 0.6F, 0, 90.0F, 6, false);

    // --- Sous-munitions (2.6) et frappe aerienne (2.7) ---
    public static final ImpactProfile BOMBLET = new ImpactProfile("sous_munition", 2.5F, 2, 3, 3, SmokeType.DARK, false, 0.25F, 0, 25.0F, 5, false);
    public static final ImpactProfile AERIAL_BOMB = new ImpactProfile("bombe_aerienne", 7.0F, 6, 24, 9, SmokeType.DARK, false, 0.8F, 0, 60.0F, 12, false);

    /** Petit impact secondaire autour des gros calibres. */
    public static final ImpactProfile SECONDARY = new ImpactProfile("secondaire", 2.5F, 2, 3, 3, SmokeType.DARK, false, 0.15F, 0, 12.0F, 4, false);

    public static final Map<String, ImpactProfile> BY_NAME = new LinkedHashMap<>();

    static {
        for (ImpactProfile profile : new ImpactProfile[]{LIGHT, MEDIUM, HEAVY, STRATEGIC, FRAG_LIGHT, FRAG_HEAVY,
                MORTAR, SHELL_105_HE, SHELL_105_FRAG, SHELL_155_HE, SHELL_155_FRAG, MINE_AP, MINE_AT,
                BOMBLET, AERIAL_BOMB, SECONDARY}) {
            BY_NAME.put(profile.name(), profile);
        }
    }

    private ImpactProfiles() {
    }
}
