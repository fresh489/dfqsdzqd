package fr.missilemod.explosion;

import fr.missilemod.MissileMod;
import fr.missilemod.entity.BombletEntity;
import fr.missilemod.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Ouverture d'une ogive a sous-munitions (2.6) : 8 a 15 petites bombes aux trajectoires aleatoires,
 * dont 1 ou 2 n'explosent pas et restent au sol comme des mines.
 */
public final class ClusterRelease {

    public static void release(ServerLevel level, Vec3 at, Vec3 velocity, @Nullable Entity owner, int count) {
        RandomSource random = level.random;
        int total = count > 0 ? count : 8 + random.nextInt(8);
        int duds = 1 + random.nextInt(2);
        keepAreaLoaded(level, at, velocity);

        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, SoundSource.BLOCKS, 8.0F, 0.6F);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 30, 0.8D, 0.8D, 0.8D, 0.1D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 20, 0.5D, 0.5D, 0.5D, 0.05D);

        Vec3 base = velocity.scale(0.35D);
        for (int i = 0; i < total; i++) {
            spawn(level, at, base, owner, false, i < duds, 0.35D);
        }
    }

    /** Frappe aerienne : bombes larguees le long d'une ligne, avec la vitesse de l'avion. */
    public static void airDrop(ServerLevel level, Vec3 at, Vec3 velocity, @Nullable Entity owner) {
        spawn(level, at, velocity, owner, true, false, 0.08D);
    }

    private static void spawn(ServerLevel level, Vec3 at, Vec3 base, @Nullable Entity owner, boolean big, boolean dud,
                              double spread) {
        RandomSource random = level.random;
        BombletEntity bomb = ModEntities.BOMBLET.get().create(level);
        if (bomb == null) {
            return;
        }
        bomb.setup(big, dud);
        bomb.setPos(at.x + random.nextGaussian() * 0.3D, at.y + random.nextGaussian() * 0.3D,
                at.z + random.nextGaussian() * 0.3D);
        bomb.setDeltaMovement(base.add(
                (random.nextDouble() - 0.5D) * 2.0D * spread,
                (random.nextDouble() - 0.3D) * spread * 0.5D,
                (random.nextDouble() - 0.5D) * 2.0D * spread));
        if (owner != null) {
            bomb.setOwner(owner);
        }
        level.addFreshEntity(bomb);
    }

    /** Garde la zone de chute chargee 15 s, meme si aucun joueur n'est a proximite. */
    private static void keepAreaLoaded(ServerLevel level, Vec3 at, Vec3 velocity) {
        UUID ticket = UUID.randomUUID();
        Vec3 ahead = at.add(velocity.scale(20.0D));
        int minX = (Mth.floor(Math.min(at.x, ahead.x)) >> 4) - 1;
        int maxX = (Mth.floor(Math.max(at.x, ahead.x)) >> 4) + 1;
        int minZ = (Mth.floor(Math.min(at.z, ahead.z)) >> 4) - 1;
        int maxZ = (Mth.floor(Math.max(at.z, ahead.z)) >> 4) + 1;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                ForgeChunkManager.forceChunk(level, MissileMod.MOD_ID, ticket, cx, cz, true, true);
            }
        }
        ImpactScheduler.schedule(level, 300, () -> {
            for (int cx = minX; cx <= maxX; cx++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    ForgeChunkManager.forceChunk(level, MissileMod.MOD_ID, ticket, cx, cz, false, true);
                }
            }
        });
    }

    private ClusterRelease() {
    }
}
