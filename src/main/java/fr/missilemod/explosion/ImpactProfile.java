package fr.missilemod.explosion;

/**
 * Tous les parametres d'un impact. Chaque munition fournit son propre profil
 * et appelle {@link ImpactSystem#detonate}.
 *
 * @param power            puissance de l'explosion Minecraft (mode HYBRID). Reperes : TNT = 4, creeper charge = 6.
 * @param craterRadius     rayon du cratere en blocs (mode CUSTOM) et taille de la zone brulee (tous modes)
 * @param debrisCount      nombre de blocs projetes qui retombent
 * @param smokeSeconds     duree de la fumee persistante
 * @param smoke            couleur / type de fumee
 * @param column           grande colonne de fumee verticale (calibre strategique)
 * @param shake            intensite du tremblement de camera au plus pres (0 = aucun, 1 = fort)
 * @param secondaryImpacts nombre de petits impacts secondaires autour du point d'impact
 * @param entityDamage     degats au centre (mode CUSTOM), 2 = un coeur
 * @param entityRadius     rayon des degats et de la projection (mode CUSTOM)
 * @param fragmentation    munition a fragmentation : degats aux entites dans les deux modes, gerbe d'etincelles,
 *                         son metallique sec
 */
public record ImpactProfile(
        String name,
        float power,
        int craterRadius,
        int debrisCount,
        int smokeSeconds,
        SmokeType smoke,
        boolean column,
        float shake,
        int secondaryImpacts,
        float entityDamage,
        int entityRadius,
        boolean fragmentation) {

    public enum SmokeType {
        LIGHT,  // gris clair
        DARK,   // gris fonce
        DUST    // poussiere couleur du terrain
    }

    /** Irregularite du bord du cratere (en blocs), proportionnelle a sa taille. */
    public float irregularity() {
        return 0.8F + this.craterRadius * 0.15F;
    }

    public double knockback() {
        return 0.8D + this.power * 0.18D;
    }
}
