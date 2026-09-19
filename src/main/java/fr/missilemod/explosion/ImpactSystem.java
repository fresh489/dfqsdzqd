package fr.missilemod.explosion;

import fr.missilemod.config.MissileConfig;
import fr.missilemod.network.ModNetwork;
import fr.missilemod.network.ShakePacket;
import fr.missilemod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Procedure d'impact unique, appelee par toutes les munitions avec leur {@link ImpactProfile}.
 *
 * Sequence : tick 0 flash + explosion/cratere ; ticks 1-5 debris ; ticks 2-20 nuage de poussiere ;
 * chaleur juste apres l'impact ; fumee persistante selon le calibre ; particules residuelles ;
 * sol brule et feux sur les bords ; impacts secondaires (gros calibres).
 */
public final class ImpactSystem {

    public static final String OVERHAUL_MOD_ID = "explosionoverhaul";
    private static final double VIEW_DISTANCE_SQR = 512.0D * 512.0D;
    private static final double SOUND_BLOCKS_PER_TICK = 343.0D / 20.0D; // vitesse du son : 343 blocs/s

    // =====================================================================
    // Point d'entree
    // =====================================================================

    public static void detonate(ServerLevel level, Vec3 at, @Nullable Entity source, @Nullable Entity owner,
                                ImpactProfile profile) {
        boolean hybrid = isHybridMode();
        boolean destroy = canDestroyBlocks(level);
        BlockPos center = BlockPos.containing(at);
        RandomSource random = level.random;

        // Echantillon du terrain AVANT destruction : couleur de la poussiere et nature des debris.
        List<BlockState> palette = samplePalette(level, center, Math.max(2, profile.craterRadius() / 2 + 1));
        BlockState dustState = palette.isEmpty() ? Blocks.DIRT.defaultBlockState() : palette.get(0);

        // --- Tick 0 : explosion et cratere ---
        if (hybrid) {
            float power = (float) (profile.power() * MissileConfig.POWER_MULTIPLIER.get());
            level.explode(source, level.damageSources().explosion(source, owner), null, at.x, at.y, at.z,
                    power, false, destroy ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
            if (profile.fragmentation()) {
                // L'explosion Minecraft d'une charge a fragmentation est petite : les eclats font le gros des degats.
                damageEntities(level, at, source, owner, profile);
            }
        } else {
            flash(level, at, profile);
            fireball(level, at, profile);
            damageEntities(level, at, source, owner, profile);
            if (destroy) {
                ExplosionManager.schedule(new SphericalExplosion(level, center, profile.craterRadius(),
                        profile.irregularity(), MissileConfig.BLAST_RESISTANCE_LIMIT.get().floatValue()));
            }
            shakeAndSound(level, at, profile);
        }

        // --- Fragmentation : gerbe d'etincelles dans toutes les directions et son metallique sec ---
        if (profile.fragmentation()) {
            fragmentation(level, at, profile);
        }

        // --- Ticks 1 a 5 : debris projetes qui retombent ---
        if (destroy && MissileConfig.DEBRIS.get() && profile.debrisCount() > 0) {
            int total = Math.min(profile.debrisCount(), MissileConfig.MAX_DEBRIS.get());
            int perTick = Math.max(1, Mth.ceil(total / 5.0F));
            ImpactScheduler.repeat(level, 1, 1, 5, i -> spawnDebris(level, at, center, palette,
                    Math.min(perTick, total - i * perTick), profile));
        }

        // --- Ticks 2 a 20 : nuage de poussiere qui s'elargit ---
        ImpactScheduler.repeat(level, 2, 2, 10, i -> dustRing(level, at, dustState, profile, i / 9.0D));

        // --- Juste apres l'impact : chaleur (braises et scintillement) ---
        ImpactScheduler.repeat(level, 1, 3, 10, i -> heat(level, at, profile, 1.0D - i / 10.0D));

        // --- Fumee persistante qui se disperse, puis particules residuelles ---
        if (MissileConfig.LINGERING_SMOKE.get() && profile.smokeSeconds() > 0) {
            int smokeTicks = profile.smokeSeconds() * 20;
            int emissions = Math.max(1, smokeTicks / 4);
            ImpactScheduler.repeat(level, 10, 4, emissions,
                    i -> lingeringSmoke(level, at, dustState, profile, i / (double) emissions));
            ImpactScheduler.repeat(level, 10 + smokeTicks, 10, 10,
                    i -> residual(level, at, profile, 1.0D - i / 10.0D));
        }

        // --- Sol brule, fissures, poussiere au sol et feux sur les bords ---
        if (destroy && MissileConfig.SCORCHED_GROUND.get()) {
            // En mode hybride, Explosion Overhaul creuse son cratere en differe : on attend qu'il ait fini.
            ImpactScheduler.schedule(level, hybrid ? 80 : 40, () -> scorch(level, center, profile));
        }

        // --- Impacts secondaires autour de l'impact principal ---
        if (MissileConfig.SECONDARY_IMPACTS.get() && profile.secondaryImpacts() > 0) {
            for (int i = 0; i < profile.secondaryImpacts(); i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = profile.craterRadius() * (1.1D + random.nextDouble() * 0.9D);
                int x = Mth.floor(at.x + Math.cos(angle) * dist);
                int z = Mth.floor(at.z + Math.sin(angle) * dist);
                int delay = 5 + random.nextInt(25);
                ImpactScheduler.schedule(level, delay, () -> {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    detonate(level, new Vec3(x + 0.5D, y, z + 0.5D), null, owner, ImpactProfiles.SECONDARY);
                });
            }
        }
    }

    public static boolean isHybridMode() {
        ImpactMode mode = MissileConfig.IMPACT_MODE.get();
        if (mode == ImpactMode.AUTO) {
            return ModList.get().isLoaded(OVERHAUL_MOD_ID);
        }
        return mode == ImpactMode.HYBRID;
    }

    private static boolean canDestroyBlocks(ServerLevel level) {
        return MissileConfig.DESTROY_BLOCKS.get()
                && (!MissileConfig.RESPECT_MOB_GRIEFING.get()
                || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING));
    }

    // =====================================================================
    // Mode CUSTOM : flash, boule de feu, degats, tremblement, son retarde
    // =====================================================================

    private static void flash(ServerLevel level, Vec3 at, ImpactProfile profile) {
        broadcast(level, ParticleTypes.FLASH, at.x, at.y + 0.5D, at.z, 1 + profile.craterRadius() / 6, 0.2D, 0.2D, 0.2D, 0.0D);
    }

    private static void fireball(ServerLevel level, Vec3 at, ImpactProfile profile) {
        double spread = Math.max(0.8D, profile.craterRadius() * 0.35D);
        int r = profile.craterRadius();
        broadcast(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, scaled(1 + r / 3), spread, spread * 0.6D, spread, 0.0D);
        broadcast(level, ParticleTypes.EXPLOSION, at.x, at.y, at.z, scaled(8 + r * 3), spread * 1.2D, spread * 0.8D, spread * 1.2D, 0.0D);
        broadcast(level, ParticleTypes.FLAME, at.x, at.y, at.z, scaled(10 + r * 5), spread * 0.8D, spread * 0.5D, spread * 0.8D, 0.2D);
        broadcast(level, ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, scaled(20 + r * 8), spread, spread * 0.7D, spread, 0.12D);
    }

    private static void damageEntities(ServerLevel level, Vec3 at, @Nullable Entity source, @Nullable Entity owner,
                                       ImpactProfile profile) {
        double radius = profile.entityRadius();
        double maxDamage = profile.entityDamage();
        double knockback = profile.knockback();
        DamageSource damageSource = level.damageSources().explosion(source, owner);
        AABB area = AABB.ofSize(at, radius * 2.0D, radius * 2.0D, radius * 2.0D);

        for (Entity entity : level.getEntities(source, area, e -> e.isAlive() && !e.isSpectator())) {
            if (entity.ignoreExplosion()) {
                continue;
            }
            Vec3 entityCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            double distance = entityCenter.distanceTo(at);
            if (distance > radius) {
                continue;
            }
            double factor = 1.0D - distance / radius;
            if (maxDamage > 0.0D) {
                entity.hurt(damageSource, (float) (maxDamage * factor));
            }
            if (entity instanceof Player player && player.isCreative() && player.getAbilities().flying) {
                continue;
            }
            double strength = knockback * factor;
            if (entity instanceof LivingEntity living) {
                strength = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, strength);
            }
            Vec3 push = distance < 1.0E-3D ? new Vec3(0.0D, 1.0D, 0.0D) : entityCenter.subtract(at).normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(
                    push.x * strength, push.y * strength + 0.25D * factor, push.z * strength));
            entity.hurtMarked = true;
        }
    }

    /**
     * Tremblement (puissance x distance) et son d'explosion retarde a la vitesse du son :
     * les joueurs eloignes voient le flash, puis entendent le boum et sentent l'onde de choc.
     */
    private static void shakeAndSound(ServerLevel level, Vec3 at, ImpactProfile profile) {
        double shakeReach = 24.0D + profile.craterRadius() * 10.0D;
        float pitch = Mth.clamp(0.9F - profile.power() * 0.025F, 0.45F, 0.9F);
        for (ServerPlayer player : level.players()) {
            double distance = Math.sqrt(player.distanceToSqr(at));
            if (distance * distance > VIEW_DISTANCE_SQR) {
                continue;
            }
            int delay = MissileConfig.DELAYED_SOUND.get() ? (int) (distance / SOUND_BLOCKS_PER_TICK) : 0;
            float volume = (float) Math.max(4.0D, distance / 16.0D + 2.0D);
            long seed = level.random.nextLong();

            float strength = 0.0F;
            int duration = 0;
            if (MissileConfig.CAMERA_SHAKE.get() && distance < shakeReach && profile.shake() > 0.0F) {
                double falloff = Math.pow(1.0D - distance / shakeReach, 1.5D);
                strength = (float) (profile.shake() * falloff);
                duration = (int) (8 + 40 * profile.shake() * falloff);
            }
            final float finalStrength = strength;
            final int finalDuration = duration;
            ImpactScheduler.schedule(level, delay, () -> {
                if (player.isRemoved() || player.level() != level) {
                    return;
                }
                player.connection.send(new ClientboundSoundPacket(
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.MISSILE_EXPLOSION.get()),
                        SoundSource.BLOCKS, at.x, at.y, at.z, volume, pitch, seed));
                if (finalStrength > 0.02F) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new ShakePacket(finalStrength, finalDuration));
                }
            });
        }
    }

    private static void fragmentation(ServerLevel level, Vec3 at, ImpactProfile profile) {
        double r = profile.entityRadius();
        level.playSound(null, at.x, at.y, at.z, ModSounds.FRAG_IMPACT.get(), SoundSource.BLOCKS, 6.0F,
                0.9F + level.random.nextFloat() * 0.2F);
        // Vitesse elevee : les particules partent loin dans toutes les directions.
        broadcast(level, ParticleTypes.CRIT, at.x, at.y + 0.5D, at.z, scaled((int) (r * 12)), 0.3D, 0.3D, 0.3D, r * 0.12D);
        broadcast(level, ParticleTypes.FIREWORK, at.x, at.y + 0.5D, at.z, scaled((int) (r * 6)), 0.3D, 0.3D, 0.3D, r * 0.05D);
        broadcast(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5D, at.z, scaled((int) (r * 4)), 0.5D, 0.5D, 0.5D, r * 0.08D);
        ImpactScheduler.repeat(level, 2, 2, 3, i -> broadcast(level, ParticleTypes.CRIT, at.x, at.y + 0.5D, at.z,
                scaled((int) (r * 4)), r * 0.4D, r * 0.2D, r * 0.4D, 0.1D));
    }

    // =====================================================================
    // Effets communs aux deux modes
    // =====================================================================

    private static List<BlockState> samplePalette(ServerLevel level, BlockPos center, int radius) {
        List<BlockState> states = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= 1 && states.size() < 64; dy++) {
            for (int dx = -radius; dx <= radius && states.size() < 64; dx++) {
                for (int dz = -radius; dz <= radius && states.size() < 64; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (isDebrisCandidate(level, pos, state)) {
                        states.add(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                                ? Blocks.DIRT.defaultBlockState() : state);
                    }
                }
            }
        }
        // Un peu de gravier et de pierre concassee dans tous les cas.
        states.add(Blocks.GRAVEL.defaultBlockState());
        states.add(Blocks.COBBLESTONE.defaultBlockState());
        states.add(Blocks.COARSE_DIRT.defaultBlockState());
        return states;
    }

    private static boolean isDebrisCandidate(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && !state.hasBlockEntity()
                && state.getFluidState().isEmpty()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && state.getBlock().getExplosionResistance() <= 9.0F
                && state.isCollisionShapeFullBlock(level, pos);
    }

    /** Debris : vrais blocs projetes vers le haut qui retombent et se posent. */
    private static void spawnDebris(ServerLevel level, Vec3 at, BlockPos center, List<BlockState> palette,
                                    int count, ImpactProfile profile) {
        RandomSource random = level.random;
        double scale = 0.6D + profile.craterRadius() / 12.0D;
        for (int i = 0; i < count; i++) {
            BlockPos pos = center.offset(random.nextInt(3) - 1, 1 + random.nextInt(2), random.nextInt(3) - 1);
            int tries = 0;
            while (!level.getBlockState(pos).isAir() && tries++ < 8) {
                pos = pos.above();
            }
            if (!level.getBlockState(pos).isAir() || level.isOutsideBuildHeight(pos)) {
                continue;
            }
            BlockState state = palette.get(random.nextInt(palette.size()));
            // fall() remplace la case (deja vide ici) par de l'air puis cree le bloc qui tombe.
            FallingBlockEntity debris = FallingBlockEntity.fall(level, pos, state);
            debris.dropItem = false;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double horizontal = (0.2D + random.nextDouble() * 0.5D) * scale;
            double vertical = (0.5D + random.nextDouble() * 0.6D) * Math.min(scale, 2.0D);
            debris.setDeltaMovement(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
        }
        broadcast(level, new BlockParticleOption(ParticleTypes.BLOCK, palette.get(0)), at.x, at.y + 0.5D, at.z,
                scaled(10 + profile.craterRadius() * 4), 0.6D, 0.4D, 0.6D, 0.4D);
    }

    /** Anneau de poussiere au ras du sol qui s'elargit (progress de 0 a 1). */
    private static void dustRing(ServerLevel level, Vec3 at, BlockState dustState, ImpactProfile profile, double progress) {
        double radius = profile.craterRadius() * (0.5D + progress * 1.6D);
        int points = scaled(8 + profile.craterRadius() * 2);
        ParticleOptions dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, dustState);
        for (int k = 0; k < points; k++) {
            double angle = (k / (double) points) * Math.PI * 2.0D + level.random.nextDouble() * 0.3D;
            double x = at.x + Math.cos(angle) * radius;
            double z = at.z + Math.sin(angle) * radius;
            broadcast(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, at.y + 0.3D, z, 1, 0.4D, 0.2D, 0.4D, 0.01D);
            broadcast(level, dust, x, at.y + 0.8D + progress, z, 1, 0.6D, 0.5D, 0.6D, 0.0D);
        }
    }

    /** Chaleur apres l'impact : braises et petites flammes qui montent (intensity de 1 a 0). */
    private static void heat(ServerLevel level, Vec3 at, ImpactProfile profile, double intensity) {
        double spread = Math.max(0.5D, profile.craterRadius() * 0.4D);
        broadcast(level, ParticleTypes.LAVA, at.x, at.y + 0.3D, at.z, scaled((int) (2 + profile.craterRadius() * intensity)),
                spread, 0.2D, spread, 0.0D);
        broadcast(level, ParticleTypes.SMALL_FLAME, at.x, at.y + 0.5D, at.z, scaled((int) (4 + profile.craterRadius() * 2 * intensity)),
                spread, 0.5D, spread, 0.03D);
    }

    /** Fumee persistante : dense au debut, de plus en plus diffuse (progress de 0 a 1). */
    private static void lingeringSmoke(ServerLevel level, Vec3 at, BlockState dustState, ImpactProfile profile, double progress) {
        double fade = 1.0D - progress;
        double spread = profile.craterRadius() * (0.3D + progress * 0.9D);
        int count = scaled((int) Math.ceil((2 + profile.craterRadius() * 0.5D) * fade));
        if (count <= 0) {
            return;
        }
        switch (profile.smoke()) {
            case LIGHT -> broadcast(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.5D, at.z, count,
                    spread, 0.3D, spread, 0.01D);
            case DARK -> {
                broadcast(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.8D, at.z, count, spread, 0.4D, spread, 0.02D);
                broadcast(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.5D, at.z, Math.max(1, count / 2),
                        spread, 0.3D, spread, 0.01D);
            }
            case DUST -> {
                broadcast(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, dustState), at.x, at.y + 2.0D, at.z,
                        count * 2, spread, 1.5D, spread, 0.0D);
                broadcast(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.5D, at.z, count, spread, 0.3D, spread, 0.01D);
            }
        }
        if (profile.column()) {
            // Colonne verticale : la fumee de signal monte tres haut d'elle-meme.
            broadcast(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at.x, at.y + 1.0D, at.z, Math.max(1, count / 2),
                    1.5D + progress * 3.0D, 0.5D, 1.5D + progress * 3.0D, 0.02D);
        }
    }

    private static void residual(ServerLevel level, Vec3 at, ImpactProfile profile, double intensity) {
        double spread = profile.craterRadius() * 0.8D;
        broadcast(level, ParticleTypes.SMOKE, at.x, at.y + 0.3D, at.z, scaled((int) Math.ceil((3 + profile.craterRadius() / 2.0D) * intensity)),
                spread, 0.2D, spread, 0.01D);
        broadcast(level, ParticleTypes.WHITE_ASH, at.x, at.y + 1.0D, at.z, scaled((int) Math.ceil(4 * intensity)),
                spread, 0.8D, spread, 0.0D);
    }

    /**
     * Sol brule : dans le cratere, les blocs naturels du fond deviennent terre brute, gravier ou blackstone ;
     * autour, traces de poussiere et fissures ; quelques feux sur le bord. Les constructions ne sont pas touchees.
     */
    private static void scorch(ServerLevel level, BlockPos center, ImpactProfile profile) {
        RandomSource random = level.random;
        int r = Math.max(2, profile.craterRadius());
        int extent = Mth.ceil(r * 1.6D) + 1;
        boolean fires = MissileConfig.EDGE_FIRES.get();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -extent; dx <= extent; dx++) {
            for (int dz = -extent; dz <= extent; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + (random.nextDouble() - 0.5D) * 1.5D;
                if (d > extent) {
                    continue;
                }
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (Math.abs(top - center.getY()) > r + 4) {
                    continue;
                }
                pos.set(x, top, z);
                BlockState state = level.getBlockState(pos);
                if (!isNaturalGround(state)) {
                    continue;
                }
                boolean inside = d <= r;
                double chance = inside ? 0.7D : 0.35D * (1.0D - (d - r) / (extent - r));
                if (random.nextDouble() < chance) {
                    level.setBlock(pos, inside ? innerScorch(random) : outerScorch(random), 3);
                }
                if (fires && d > r * 0.75D && d < r * 1.25D && random.nextFloat() < 0.06F) {
                    BlockPos above = pos.above();
                    if (level.getBlockState(above).isAir()) {
                        level.setBlock(above, BaseFireBlock.getState(level, above), 11);
                    }
                }
            }
        }
    }

    private static boolean isNaturalGround(BlockState state) {
        return !state.hasBlockEntity()
                && (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.CLAY));
    }

    private static BlockState innerScorch(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 40) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (roll < 62) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (roll < 85) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    }

    private static BlockState outerScorch(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 55) {
            return Blocks.COARSE_DIRT.defaultBlockState();   // traces de poussiere
        }
        if (roll < 80) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return random.nextBoolean()
                ? Blocks.COBBLESTONE.defaultBlockState()     // fissures
                : Blocks.ROOTED_DIRT.defaultBlockState();
    }

    // =====================================================================
    // Utilitaires
    // =====================================================================

    private static int scaled(int count) {
        return Math.max(0, (int) Math.round(count * MissileConfig.PARTICLE_MULTIPLIER.get()));
    }

    /** Envoie des particules visibles de loin (jusqu'a 512 blocs) aux joueurs proches. */
    private static void broadcast(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
                                  double dx, double dy, double dz, double speed) {
        if (count <= 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= VIEW_DISTANCE_SQR) {
                level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    private ImpactSystem() {
    }
}
