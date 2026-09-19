package fr.missilemod.registry;

import fr.missilemod.MissileMod;
import fr.missilemod.entity.BombletEntity;
import fr.missilemod.entity.MissileEntity;
import fr.missilemod.entity.MissileType;
import fr.missilemod.entity.ShellEntity;
import fr.missilemod.entity.SmokeGrenadeEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MissileMod.MOD_ID);

    /** Un type d'entite par missile : la hitbox (largeur x hauteur) est propre a chaque type. */
    public static final Map<MissileType, RegistryObject<EntityType<MissileEntity>>> MISSILES = new EnumMap<>(MissileType.class);

    static {
        for (MissileType type : MissileType.values()) {
            MISSILES.put(type, ENTITY_TYPES.register(type.entityId,
                    () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                            .sized(type.width, type.height)
                            .clientTrackingRange(16)       // en chunks (plafonne par la view-distance du serveur)
                            .updateInterval(1)             // synchro position/rotation a chaque tick
                            .setShouldReceiveVelocityUpdates(true)
                            .fireImmune()
                            .build(new ResourceLocation(MissileMod.MOD_ID, type.entityId).toString())));
        }
    }

    /** Conserve pour le code existant : missile AIM-120 (id "missile"). */
    public static final RegistryObject<EntityType<MissileEntity>> MISSILE = MISSILES.get(MissileType.AIM120);

    public static final RegistryObject<EntityType<ShellEntity>> SHELL = ENTITY_TYPES.register("shell",
            () -> EntityType.Builder.<ShellEntity>of(ShellEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(16).updateInterval(1).fireImmune()
                    .build(new ResourceLocation(MissileMod.MOD_ID, "shell").toString()));

    public static final RegistryObject<EntityType<BombletEntity>> BOMBLET = ENTITY_TYPES.register("bomblet",
            () -> EntityType.Builder.<BombletEntity>of(BombletEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(10).updateInterval(2)
                    .build(new ResourceLocation(MissileMod.MOD_ID, "bomblet").toString()));

    public static final RegistryObject<EntityType<SmokeGrenadeEntity>> SMOKE_GRENADE = ENTITY_TYPES.register("smoke_grenade",
            () -> EntityType.Builder.<SmokeGrenadeEntity>of(SmokeGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(2)
                    .build(new ResourceLocation(MissileMod.MOD_ID, "smoke_grenade").toString()));

    private ModEntities() {
    }
}
