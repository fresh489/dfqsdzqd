package fr.missilemod.entity;

import fr.missilemod.explosion.ImpactProfile;
import fr.missilemod.explosion.ImpactProfiles;
import fr.missilemod.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/**
 * Gamme de missiles (cahier des charges 2.1, 2.2, 2.6). Tous partagent la meme entite et la meme logique de vol ;
 * seuls la taille, le modele, la vitesse, la portee et le profil d'impact changent.
 *
 * speed / range / altitude : multiplicateurs des valeurs de la config (section [flight]).
 */
public enum MissileType {
    //            entite                 item                        modele       texture                     larg. haut. vit. port. alt. trainee  sous-mun. profil
    AIM120("missile", "missile_aim120", "aim120", "missile_aim120", 1.0F, 3.0F, 1.0D, 1.0D, 1.0D, 1.0F, false, () -> ImpactProfiles.MEDIUM),
    GRAD("missile_grad", "missile_grad", "grad", "missile_grad", 1.0F, 2.0F, 1.3D, 0.6D, 0.6D, 0.7F, false, () -> ImpactProfiles.LIGHT),
    GRAD_FRAG("missile_grad_frag", "missile_grad_frag", "grad", "missile_grad_frag", 1.0F, 2.0F, 1.3D, 0.6D, 0.6D, 0.7F, false, () -> ImpactProfiles.FRAG_LIGHT),
    TOMAHAWK("missile_tomahawk", "missile_tomahawk", "tomahawk", "missile_tomahawk", 1.0F, 3.0F, 0.8D, 2.0D, 0.5D, 0.9F, false, () -> ImpactProfiles.MEDIUM),
    TOMAHAWK_CLUSTER("missile_tomahawk_cluster", "missile_tomahawk_cluster", "tomahawk", "missile_tomahawk_cluster", 1.0F, 3.0F, 0.8D, 2.0D, 1.0D, 0.9F, true, () -> ImpactProfiles.MEDIUM),
    SCUD("missile_scud", "missile_scud", "scud", "missile_scud", 1.0F, 4.0F, 1.1D, 2.5D, 2.0D, 1.5F, false, () -> ImpactProfiles.HEAVY),
    SCUD_FRAG("missile_scud_frag", "missile_scud_frag", "scud", "missile_scud_frag", 1.0F, 4.0F, 1.1D, 2.5D, 2.0D, 1.5F, false, () -> ImpactProfiles.FRAG_HEAVY),
    STRATEGIC("missile_strategic", "missile_strategic", "strategic", "missile_strategic", 1.25F, 5.0F, 1.2D, 6.0D, 3.0D, 2.2F, false, () -> ImpactProfiles.STRATEGIC);

    public final String entityId;
    public final String itemId;
    public final String model;
    public final String texture;
    public final float width;
    public final float height;
    public final double speed;
    public final double range;
    public final double altitude;
    public final float trailScale;
    public final boolean cluster;
    private final Supplier<ImpactProfile> profile;

    MissileType(String entityId, String itemId, String model, String texture, float width, float height,
                double speed, double range, double altitude, float trailScale, boolean cluster,
                Supplier<ImpactProfile> profile) {
        this.entityId = entityId;
        this.itemId = itemId;
        this.model = model;
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.range = range;
        this.altitude = altitude;
        this.trailScale = trailScale;
        this.cluster = cluster;
        this.profile = profile;
    }

    public ImpactProfile profile() {
        return this.profile.get();
    }

    public static MissileType fromEntityType(EntityType<?> type) {
        for (MissileType missileType : values()) {
            if (ModEntities.MISSILES.get(missileType).get() == type) {
                return missileType;
            }
        }
        return AIM120;
    }
}
