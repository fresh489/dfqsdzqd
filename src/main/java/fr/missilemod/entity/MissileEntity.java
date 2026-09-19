package fr.missilemod.entity;

import fr.missilemod.MissileMod;
import fr.missilemod.client.ClientHooks;
import fr.missilemod.config.MissileConfig;
import fr.missilemod.explosion.ClusterRelease;
import fr.missilemod.explosion.ImpactSystem;
import fr.missilemod.registry.ModItems;
import fr.missilemod.registry.ModSounds;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Missile AIM-120. La position de l'entite est la base de la hitbox (1 x 3 blocs).
 * Toute la logique de vol travaille sur le "centre" (position + 1,5 bloc), autour duquel le modele pivote.
 * Convention d'orientation : yaw = atan2(dx, dz) en degres, pitch = +90 vers le haut, -90 vers le bas.
 */
public class MissileEntity extends Entity implements IncomingMunition {

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.FLOAT);

    private static final double RAD_TO_DEG = 180.0D / Math.PI;
    private static final int PITCHOVER_TICKS = 20;
    private static final double DIVE_RATIO = 1.0D;                 // pique a partir d'un angle de 45 degres
    private static final double CRUISE_TURN_RATE = Math.toRadians(5.0D);
    private static final double DIVE_TURN_RATE = Math.toRadians(12.0D);
    private static final double CLUSTER_RELEASE_HEIGHT = 45.0D;

    private final MissileType missileType;

    // ---------- Etat serveur ----------
    private Vec3 heading = new Vec3(0.0D, 1.0D, 0.0D);
    private Vec3 pitchoverStart = new Vec3(0.0D, 1.0D, 0.0D);
    @Nullable
    private BlockPos target;
    private double cruiseY;
    private int flightTicks;
    private int pitchoverTicks;
    private boolean diving;
    @Nullable
    private UUID ownerId;
    private final LongSet forcedChunks = new LongOpenHashSet();

    // ---------- Etat client (visuel) ----------
    private float visYaw;
    private float visPitch = 90.0F;
    private float prevVisYaw;
    private float prevVisPitch = 90.0F;
    private boolean visInitialized;
    private boolean engineSoundStarted;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.missileType = MissileType.fromEntityType(type);
        this.blocksBuilding = true;
    }

    // =====================================================================
    // Donnees synchronisees
    // =====================================================================

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STATE, MissileState.POSED.ordinal());
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_PITCH, 90.0F);
    }

    public MissileState getState() {
        return MissileState.byId(this.entityData.get(DATA_STATE));
    }

    private void setState(MissileState state) {
        this.entityData.set(DATA_STATE, state.ordinal());
    }

    public boolean isFlying() {
        return getState().isFlying();
    }

    public void initPlaced(float yaw) {
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, 90.0F);
    }

    public MissileType getMissileType() {
        return this.missileType;
    }

    /** Distance entre la base de la hitbox et le centre du missile (moitie de sa hauteur). */
    public double centerOffset() {
        return this.missileType.height / 2.0D;
    }

    private double noseLength() {
        return this.missileType.height / 2.0D;
    }

    private double tailLength() {
        return this.missileType.height / 2.0D + 0.05D;
    }

    @Override
    public boolean isIncomingMunition() {
        return isFlying() && isAlive();
    }

    public Vec3 getCenter() {
        return position().add(0.0D, centerOffset(), 0.0D);
    }

    private void setHeading(Vec3 newHeading) {
        Vec3 h = newHeading.normalize();
        if (h.lengthSqr() < 1.0E-6D) {
            return;
        }
        this.heading = h;
        double horizontal = Math.sqrt(h.x * h.x + h.z * h.z);
        if (horizontal > 1.0E-3D) {
            this.entityData.set(DATA_YAW, (float) (Math.atan2(h.x, h.z) * RAD_TO_DEG));
        }
        this.entityData.set(DATA_PITCH, (float) (Math.atan2(h.y, horizontal) * RAD_TO_DEG));
    }

    public static Vec3 directionFrom(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosPitch = Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * cosPitch, Math.sin(pitch), Math.cos(yaw) * cosPitch);
    }

    public float getVisualYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.prevVisYaw, this.visYaw);
    }

    public float getVisualPitch(float partialTick) {
        return Mth.lerp(partialTick, this.prevVisPitch, this.visPitch);
    }

    // =====================================================================
    // Lancement (appele par le paquet reseau, apres validation)
    // =====================================================================

    public void launch(ServerPlayer player, BlockPos targetPos) {
        if (!(level() instanceof ServerLevel serverLevel) || getState() != MissileState.POSED) {
            return;
        }
        this.target = targetPos.immutable();
        this.ownerId = player.getUUID();
        this.flightTicks = 0;
        this.pitchoverTicks = 0;
        this.diving = false;

        Vec3 center = getCenter();
        this.cruiseY = computeCruiseY(serverLevel, center, this.target,
                MissileConfig.CRUISE_ALTITUDE.get() * this.missileType.altitude);

        double dx = this.target.getX() + 0.5D - center.x;
        double dz = this.target.getZ() + 0.5D - center.z;
        if (dx * dx + dz * dz > 1.0E-4D) {
            this.entityData.set(DATA_YAW, (float) (Math.atan2(dx, dz) * RAD_TO_DEG));
        }
        setHeading(new Vec3(0.0D, 1.0D, 0.0D));
        setDeltaMovement(Vec3.ZERO);
        setState(MissileState.ASCENT);

        serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.MISSILE_LAUNCH.get(),
                SoundSource.NEUTRAL, 6.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2D, getZ(),
                40, 0.8D, 0.1D, 0.8D, 0.02D);
        // Grosse colonne de poussiere au depart, proportionnelle a la taille du missile.
        double scale = this.missileType.height / 3.0D;
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.5D, getZ(),
                (int) (50 * scale), 1.2D * scale, 0.8D * scale, 1.2D * scale, 0.03D);
        serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3D, getZ(),
                (int) (30 * scale), 1.5D * scale, 0.2D, 1.5D * scale, 0.08D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(),
                (int) (20 * scale), 0.8D * scale, 1.5D * scale, 0.8D * scale, 0.02D);
        updateChunkTickets(serverLevel, center, new Vec3(0.0D, 1.0D, 0.0D));
    }

    /**
     * Altitude de croisiere = point le plus haut (depart, cible, et relief des chunks deja charges
     * sur la trajectoire) + cruiseAltitude, plafonnee sous la hauteur max du monde.
     * On n'echantillonne QUE les chunks deja charges pour ne jamais bloquer le thread serveur.
     */
    private static double computeCruiseY(ServerLevel level, Vec3 start, BlockPos targetPos, double altitude) {
        double highest = Math.max(start.y, targetPos.getY());
        double dx = targetPos.getX() + 0.5D - start.x;
        double dz = targetPos.getZ() + 0.5D - start.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        int samples = (int) (distance / 8.0D) + 1;
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            int bx = Mth.floor(start.x + dx * t);
            int bz = Mth.floor(start.z + dz * t);
            if (level.getChunkSource().hasChunk(bx >> 4, bz >> 4)) {
                highest = Math.max(highest, level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz));
            }
        }
        return Math.min(highest + altitude, level.getMaxBuildHeight() - 3.0D);
    }

    // =====================================================================
    // Tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level();
        MissileState state = getState();
        switch (state) {
            case POSED, DUD -> tickPosed();
            case ASCENT, PITCHOVER, CRUISE -> tickFlight(serverLevel, state);
            case EXPLODED -> discard();
        }
    }

    private void tickPosed() {
        // Gravite simple : si le sol disparait, le missile retombe.
        move(MoverType.SELF, getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        setDeltaMovement(0.0D, getDeltaMovement().y * 0.98D, 0.0D);
        if (getY() < level().getMinBuildHeight() - 64) {
            discard();
        }
    }

    private void tickFlight(ServerLevel level, MissileState state) {
        if (this.target == null) {
            explode(getCenter());
            return;
        }
        this.flightTicks++;
        if (this.flightTicks > MissileConfig.MAX_FLIGHT_SECONDS.get() * 20) {
            explode(getCenter()); // auto-destruction de securite
            return;
        }

        Vec3 center = getCenter();
        Vec3 targetCenter = Vec3.atCenterOf(this.target);
        double ascentSpeed = MissileConfig.ASCENT_SPEED.get() / 20.0D * this.missileType.speed;
        double cruiseSpeed = MissileConfig.CRUISE_SPEED.get() / 20.0D * this.missileType.speed;
        double speed = cruiseSpeed;

        switch (state) {
            case ASCENT -> {
                setHeading(new Vec3(0.0D, 1.0D, 0.0D));
                speed = ascentSpeed;
                if (center.y >= this.cruiseY) {
                    this.pitchoverStart = this.heading;
                    this.pitchoverTicks = 0;
                    setState(MissileState.PITCHOVER);
                }
            }
            case PITCHOVER -> {
                this.pitchoverTicks++;
                double t = Math.min(1.0D, this.pitchoverTicks / (double) PITCHOVER_TICKS);
                double eased = t * t * (3.0D - 2.0D * t);
                setHeading(slerp(this.pitchoverStart, desiredHeading(center, targetCenter), eased));
                speed = Mth.lerp(t, ascentSpeed, cruiseSpeed);
                if (t >= 1.0D) {
                    setState(MissileState.CRUISE);
                }
            }
            default -> {
                Vec3 desired = desiredHeading(center, targetCenter);
                setHeading(turnTowards(this.heading, desired, this.diving ? DIVE_TURN_RATE : CRUISE_TURN_RATE));
            }
        }

        // Sous-munitions : l'ogive s'ouvre a quelques dizaines de blocs au-dessus de la cible.
        if (this.missileType.cluster && this.diving && center.y - targetCenter.y <= CLUSTER_RELEASE_HEIGHT) {
            setState(MissileState.EXPLODED);
            Entity owner = this.ownerId != null ? level.getEntity(this.ownerId) : null;
            ClusterRelease.release(level, center, this.heading.scale(speed), owner, 0);
            discard();
            return;
        }

        // Arrivee sur la cible
        if (state != MissileState.ASCENT && center.distanceTo(targetCenter) <= speed + 0.5D) {
            explode(targetCenter);
            return;
        }

        Vec3 velocity = this.heading.scale(speed);

        // Raycast du centre jusqu'a la position future du nez : aucun bloc ne peut etre traverse.
        Vec3 rayEnd = center.add(this.heading.scale(noseLength())).add(velocity);
        HitResult hit = raycast(level, center, rayEnd);
        if (hit != null) {
            explode(hit.getLocation());
            return;
        }

        Vec3 newCenter = center.add(velocity);
        setPos(newCenter.x, newCenter.y - centerOffset(), newCenter.z);
        setDeltaMovement(velocity);

        if (newCenter.y < level.getMinBuildHeight() - 16) {
            discard();
            return;
        }
        updateChunkTickets(level, newCenter, velocity);
    }

    private Vec3 desiredHeading(Vec3 center, Vec3 targetCenter) {
        Vec3 toTarget = targetCenter.subtract(center);
        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        double heightAbove = center.y - targetCenter.y;
        if (!this.diving && horizontal <= Math.max(0.0D, heightAbove) * DIVE_RATIO + 2.0D) {
            this.diving = true; // debut du pique final
        }
        if (this.diving || horizontal < 1.0E-3D) {
            return toTarget.normalize();
        }
        // Vol en palier avec correction douce de l'altitude
        double climb = Mth.clamp((this.cruiseY - center.y) * 0.05D, -0.25D, 0.25D);
        return new Vec3(toTarget.x / horizontal, climb, toTarget.z / horizontal).normalize();
    }

    private static Vec3 slerp(Vec3 from, Vec3 to, double t) {
        double dot = Mth.clamp(from.dot(to), -1.0D, 1.0D);
        double theta = Math.acos(dot);
        if (theta < 1.0E-4D) {
            return to;
        }
        if (Math.PI - theta < 1.0E-3D) {
            // Vecteurs opposes : on tourne dans un plan perpendiculaire arbitraire.
            Vec3 axisHelper = Math.abs(from.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 perpendicular = axisHelper.subtract(from.scale(axisHelper.dot(from))).normalize();
            double angle = theta * t;
            return from.scale(Math.cos(angle)).add(perpendicular.scale(Math.sin(angle))).normalize();
        }
        double sinTheta = Math.sin(theta);
        return from.scale(Math.sin((1.0D - t) * theta) / sinTheta)
                .add(to.scale(Math.sin(t * theta) / sinTheta))
                .normalize();
    }

    private static Vec3 turnTowards(Vec3 current, Vec3 desired, double maxRadians) {
        double theta = Math.acos(Mth.clamp(current.dot(desired), -1.0D, 1.0D));
        if (theta <= maxRadians) {
            return desired;
        }
        return slerp(current, desired, maxRadians / theta);
    }

    @Nullable
    private HitResult raycast(ServerLevel level, Vec3 from, Vec3 to) {
        BlockHitResult blockHit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 segmentEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : to;

        AABB sweep = new AABB(from, segmentEnd).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, this, from, segmentEnd, sweep,
                this::canHitEntity);
        if (entityHit != null) {
            return entityHit;
        }
        return blockHit.getType() != HitResult.Type.MISS ? blockHit : null;
    }

    private boolean canHitEntity(Entity entity) {
        if (entity == this || !entity.isAlive() || !entity.isPickable() || entity.isSpectator()) {
            return false;
        }
        // Le tireur ne peut pas etre touche pendant la premiere demi-seconde.
        return !(this.flightTicks < 10 && this.ownerId != null && this.ownerId.equals(entity.getUUID()));
    }

    private void explode(Vec3 at) {
        if (!(level() instanceof ServerLevel serverLevel) || getState() == MissileState.EXPLODED) {
            return;
        }
        if (isFlying() && serverLevel.random.nextDouble() < MissileConfig.DUD_CHANCE.get()) {
            becomeDud(serverLevel);
            return;
        }
        setState(MissileState.EXPLODED);
        Entity owner = this.ownerId != null ? serverLevel.getEntity(this.ownerId) : null;
        ImpactSystem.detonate(serverLevel, at, this, owner, this.missileType.profile());
        discard(); // declenche remove() -> liberation des tickets de chunk
    }

    /** Missile rate : il s'ecrase sans exploser et reste sur place (recuperable accroupi, sans objet). */
    private void becomeDud(ServerLevel level) {
        setState(MissileState.DUD);
        setDeltaMovement(Vec3.ZERO);
        releaseChunkTickets(level);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5D, getZ(), 20, 0.6D, 0.3D, 0.6D, 0.05D);
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5D, getZ(), 30, 0.4D, 0.4D, 0.4D, 0.02D);
    }

    // =====================================================================
    // Chargement des chunks (ForgeChunkManager)
    // =====================================================================

    private void updateChunkTickets(ServerLevel level, Vec3 center, Vec3 velocity) {
        LongSet wanted = new LongOpenHashSet();
        wanted.add(chunkKey(center));
        wanted.add(chunkKey(center.add(velocity.scale(12.0D))));
        wanted.add(chunkKey(center.add(velocity.scale(24.0D))));

        LongIterator current = this.forcedChunks.iterator();
        while (current.hasNext()) {
            long key = current.nextLong();
            if (!wanted.contains(key)) {
                forceChunk(level, key, false);
                current.remove();
            }
        }
        LongIterator toAdd = wanted.iterator();
        while (toAdd.hasNext()) {
            long key = toAdd.nextLong();
            if (this.forcedChunks.add(key)) {
                forceChunk(level, key, true);
            }
        }
    }

    private void releaseChunkTickets(ServerLevel level) {
        LongIterator it = this.forcedChunks.iterator();
        while (it.hasNext()) {
            forceChunk(level, it.nextLong(), false);
        }
        this.forcedChunks.clear();
    }

    private void forceChunk(ServerLevel level, long key, boolean add) {
        ForgeChunkManager.forceChunk(level, MissileMod.MOD_ID, this,
                ChunkPos.getX(key), ChunkPos.getZ(key), add, true);
    }

    private static long chunkKey(Vec3 position) {
        return ChunkPos.asLong(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            releaseChunkTickets((ServerLevel) level());
        }
        super.remove(reason);
    }

    // =====================================================================
    // Cote client : interpolation, trainee, son
    // =====================================================================

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        // En vol, le serveur envoie une position a chaque tick : on la rejoint au tick suivant,
        // le renderer interpole ensuite entre l'ancienne et la nouvelle position a chaque image.
        this.lerpSteps = isFlying() ? 1 : Math.max(1, steps);
    }

    private void clientTick() {
        if (this.lerpSteps > 0) {
            double nx = getX() + (this.lerpX - getX()) / this.lerpSteps;
            double ny = getY() + (this.lerpY - getY()) / this.lerpSteps;
            double nz = getZ() + (this.lerpZ - getZ()) / this.lerpSteps;
            this.lerpSteps--;
            setPos(nx, ny, nz);
        }

        float syncedYaw = this.entityData.get(DATA_YAW);
        float syncedPitch = this.entityData.get(DATA_PITCH);
        if (!this.visInitialized) {
            this.prevVisYaw = this.visYaw = syncedYaw;
            this.prevVisPitch = this.visPitch = syncedPitch;
            this.visInitialized = true;
        } else {
            this.prevVisYaw = this.visYaw;
            this.prevVisPitch = this.visPitch;
            this.visYaw = syncedYaw;
            this.visPitch = syncedPitch;
        }

        if (isFlying()) {
            spawnTrail();
            if (!this.engineSoundStarted) {
                this.engineSoundStarted = true;
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.startEngineSound(this));
            }
        }
    }

    private void spawnTrail() {
        Level level = level();
        Vec3 dir = directionFrom(this.visYaw, this.visPitch);
        Vec3 tail = getCenter().subtract(dir.scale(tailLength()));
        Vec3 prevDir = directionFrom(this.prevVisYaw, this.prevVisPitch);
        Vec3 prevTail = new Vec3(this.xo, this.yo + centerOffset(), this.zo).subtract(prevDir.scale(tailLength()));
        float scale = this.missileType.trailScale;
        double jitter = 0.08D * scale;

        // On remplit tout le segment parcouru pendant le tick pour une trainee continue.
        int steps = Math.max(3, Math.round(6 * scale));
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            double x = Mth.lerp(t, prevTail.x, tail.x) + this.random.nextGaussian() * jitter;
            double y = Mth.lerp(t, prevTail.y, tail.y) + this.random.nextGaussian() * jitter;
            double z = Mth.lerp(t, prevTail.z, tail.z) + this.random.nextGaussian() * jitter;
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y, z,
                    -dir.x * 0.02D + (this.random.nextDouble() - 0.5D) * 0.015D,
                    0.005D,
                    -dir.z * 0.02D + (this.random.nextDouble() - 0.5D) * 0.015D);
            if (i % 2 == 0 || scale > 1.4F) {
                level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, x, y, z,
                        (this.random.nextDouble() - 0.5D) * 0.05D,
                        (this.random.nextDouble() - 0.5D) * 0.05D,
                        (this.random.nextDouble() - 0.5D) * 0.05D);
            }
        }
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.FLAME,
                    tail.x + (this.random.nextDouble() - 0.5D) * 0.15D,
                    tail.y + (this.random.nextDouble() - 0.5D) * 0.15D,
                    tail.z + (this.random.nextDouble() - 0.5D) * 0.15D,
                    -dir.x * 0.15D + (this.random.nextDouble() - 0.5D) * 0.04D,
                    -dir.y * 0.15D + (this.random.nextDouble() - 0.5D) * 0.04D,
                    -dir.z * 0.15D + (this.random.nextDouble() - 0.5D) * 0.04D);
        }
    }

    // =====================================================================
    // Interactions joueur
    // =====================================================================

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (getState() != MissileState.POSED) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openTargetScreen(this));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Clic gauche : accroupi = recuperer le missile ; sinon aucun degat. */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        MissileState state = getState();
        if (!(attacker instanceof Player player) || (state != MissileState.POSED && state != MissileState.DUD)) {
            return false;
        }
        if (!level().isClientSide) {
            if (player.isShiftKeyDown()) {
                // Un missile rate est simplement retire : il ne rend pas d'objet.
                if (!player.getAbilities().instabuild && state == MissileState.POSED) {
                    spawnAtLocation(new ItemStack(ModItems.MISSILES.get(this.missileType).get()));
                }
                playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.0F);
                discard();
            } else {
                player.displayClientMessage(Component.translatable("message.missilemod.sneak_to_pickup"), true);
            }
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // le missile ne subit pas de degats (utiliser /kill si besoin)
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return (getState() == MissileState.POSED || getState() == MissileState.DUD) && isAlive();
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.MISSILES.get(this.missileType).get());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 512.0D * 512.0D;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(this.missileType.height / 2.0D + 0.5D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // =====================================================================
    // Sauvegarde NBT
    // =====================================================================

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("State", getState().ordinal());
        tag.putFloat("VisualYaw", this.entityData.get(DATA_YAW));
        tag.putFloat("VisualPitch", this.entityData.get(DATA_PITCH));
        writeVec(tag, "Heading", this.heading);
        writeVec(tag, "PitchoverStart", this.pitchoverStart);
        if (this.target != null) {
            tag.putInt("TargetX", this.target.getX());
            tag.putInt("TargetY", this.target.getY());
            tag.putInt("TargetZ", this.target.getZ());
        }
        tag.putDouble("CruiseY", this.cruiseY);
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("PitchoverTicks", this.pitchoverTicks);
        tag.putBoolean("Diving", this.diving);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setState(MissileState.byId(tag.getInt("State")));
        this.entityData.set(DATA_YAW, tag.getFloat("VisualYaw"));
        this.entityData.set(DATA_PITCH, tag.contains("VisualPitch") ? tag.getFloat("VisualPitch") : 90.0F);
        this.heading = readVec(tag, "Heading");
        this.pitchoverStart = readVec(tag, "PitchoverStart");
        this.target = tag.contains("TargetX")
                ? new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"))
                : null;
        this.cruiseY = tag.getDouble("CruiseY");
        this.flightTicks = tag.getInt("FlightTicks");
        this.pitchoverTicks = tag.getInt("PitchoverTicks");
        this.diving = tag.getBoolean("Diving");
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    private static void writeVec(CompoundTag tag, String prefix, Vec3 vec) {
        tag.putDouble(prefix + "X", vec.x);
        tag.putDouble(prefix + "Y", vec.y);
        tag.putDouble(prefix + "Z", vec.z);
    }

    private static Vec3 readVec(CompoundTag tag, String prefix) {
        Vec3 vec = new Vec3(tag.getDouble(prefix + "X"), tag.getDouble(prefix + "Y"), tag.getDouble(prefix + "Z"));
        return vec.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : vec.normalize();
    }
}
