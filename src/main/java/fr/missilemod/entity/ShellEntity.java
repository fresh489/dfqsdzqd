package fr.missilemod.entity;

import fr.missilemod.MissileMod;
import fr.missilemod.explosion.ClusterRelease;
import fr.missilemod.explosion.ImpactSystem;
import fr.missilemod.registry.ModSounds;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Obus en vol. La position est calculee analytiquement a chaque tick (aucune derive) : il arrive exactement
 * sur la cible. Siffle ~2 s avant l'impact, puis declenche la procedure d'impact commune.
 */
public class ShellEntity extends Entity implements IncomingMunition {

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(ShellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(ShellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(ShellEntity.class, EntityDataSerializers.FLOAT);
    private static final int WHISTLE_TICKS = 44;       // duree du son de sifflement
    private static final double CLUSTER_RELEASE_HEIGHT = 40.0D;

    private Vec3 origin = Vec3.ZERO;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 target = Vec3.ZERO;
    private double gravity = 0.05D;
    private int flightTicks;
    private int totalTicks;
    private boolean whistled;
    @Nullable
    private UUID ownerId;
    private final LongSet forcedChunks = new LongOpenHashSet();

    private float prevYaw;
    private float prevPitch;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;

    public ShellEntity(EntityType<? extends ShellEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void launch(ShellType type, Vec3 origin, Ballistics.Solution solution, Vec3 target, @Nullable Entity owner) {
        this.entityData.set(DATA_TYPE, type.ordinal());
        this.origin = origin;
        this.velocity = solution.velocity();
        this.gravity = solution.gravity();
        this.totalTicks = solution.ticks();
        this.target = target;
        this.ownerId = owner != null ? owner.getUUID() : null;
        setPos(origin.x, origin.y, origin.z);
        updateOrientation(solution.velocityAt(0.0D));
    }

    public ShellType getShellType() {
        return ShellType.byId(this.entityData.get(DATA_TYPE));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TYPE, 0);
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_PITCH, 45.0F);
    }

    @Override
    public boolean isIncomingMunition() {
        return isAlive();
    }

    public float getYawDegrees(float partialTick) {
        return Mth.rotLerp(partialTick, this.prevYaw, this.entityData.get(DATA_YAW));
    }

    public float getPitchDegrees(float partialTick) {
        return Mth.lerp(partialTick, this.prevPitch, this.entityData.get(DATA_PITCH));
    }

    private void updateOrientation(Vec3 v) {
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        this.entityData.set(DATA_YAW, (float) Math.toDegrees(Math.atan2(v.x, v.z)));
        this.entityData.set(DATA_PITCH, (float) Math.toDegrees(Math.atan2(v.y, horizontal)));
    }

    @Override
    public void tick() {
        this.prevYaw = this.entityData.get(DATA_YAW);
        this.prevPitch = this.entityData.get(DATA_PITCH);
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (this.totalTicks <= 0 || this.flightTicks > this.totalTicks + 100) {
            discard();
            return;
        }
        this.flightTicks++;
        ShellType type = getShellType();
        Ballistics.Solution solution = new Ballistics.Solution(this.velocity, this.totalTicks, this.gravity);
        Vec3 previous = position();
        Vec3 next = this.flightTicks >= this.totalTicks ? this.target : solution.positionAt(this.origin, this.flightTicks);
        Vec3 currentVelocity = solution.velocityAt(this.flightTicks);

        // Sifflement : joue a la cible, pour qu'on l'entende arriver juste avant l'impact.
        if (!this.whistled && this.totalTicks - this.flightTicks <= WHISTLE_TICKS) {
            this.whistled = true;
            level.playSound(null, this.target.x, this.target.y + 4.0D, this.target.z, ModSounds.SHELL_WHISTLE.get(),
                    SoundSource.BLOCKS, 8.0F, type.caliber == ShellType.Caliber.MORTAR_81 ? 1.25F : 1.0F);
        }

        HitResult hit = raycast(level, previous, next);
        if (hit != null) {
            detonate(level, type, hit.getLocation().subtract(currentVelocity.normalize().scale(0.3D)));
            return;
        }

        if (type.cluster && currentVelocity.y < 0.0D && next.y - this.target.y <= CLUSTER_RELEASE_HEIGHT) {
            Entity owner = this.ownerId != null ? level.getEntity(this.ownerId) : null;
            ClusterRelease.release(level, next, currentVelocity, owner, 0);
            discard();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(currentVelocity);
        updateOrientation(currentVelocity);
        if (this.flightTicks >= this.totalTicks) {
            detonate(level, type, this.target);
            return;
        }
        updateChunkTickets(level, next, currentVelocity);
    }

    private void detonate(ServerLevel level, ShellType type, Vec3 at) {
        Entity owner = this.ownerId != null ? level.getEntity(this.ownerId) : null;
        ImpactSystem.detonate(level, at, this, owner, type.profile());
        discard();
    }

    @Nullable
    private HitResult raycast(ServerLevel level, Vec3 from, Vec3 to) {
        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : to;
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, this, from, end,
                new AABB(from, end).inflate(1.0D), e -> e.isAlive() && e.isPickable() && !e.isSpectator()
                        && !(this.flightTicks < 20 && this.ownerId != null && this.ownerId.equals(e.getUUID())));
        if (entityHit != null) {
            return entityHit;
        }
        return blockHit.getType() != HitResult.Type.MISS ? blockHit : null;
    }

    // ---------- Chargement des chunks sur la trajectoire ----------

    private void updateChunkTickets(ServerLevel level, Vec3 position, Vec3 v) {
        LongSet wanted = new LongOpenHashSet();
        wanted.add(chunkKey(position));
        wanted.add(chunkKey(position.add(v.scale(8.0D))));
        wanted.add(chunkKey(position.add(v.scale(16.0D))));
        LongIterator current = this.forcedChunks.iterator();
        while (current.hasNext()) {
            long key = current.nextLong();
            if (!wanted.contains(key)) {
                forceChunk(level, key, false);
                current.remove();
            }
        }
        LongIterator add = wanted.iterator();
        while (add.hasNext()) {
            long key = add.nextLong();
            if (this.forcedChunks.add(key)) {
                forceChunk(level, key, true);
            }
        }
    }

    private void forceChunk(ServerLevel level, long key, boolean add) {
        ForgeChunkManager.forceChunk(level, MissileMod.MOD_ID, this, ChunkPos.getX(key), ChunkPos.getZ(key), add, true);
    }

    private static long chunkKey(Vec3 position) {
        return ChunkPos.asLong(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level && reason.shouldDestroy()) {
            LongIterator it = this.forcedChunks.iterator();
            while (it.hasNext()) {
                forceChunk(level, it.nextLong(), false);
            }
            this.forcedChunks.clear();
        }
        super.remove(reason);
    }

    // ---------- Client ----------

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpSteps = 1;
    }

    private void clientTick() {
        if (this.lerpSteps > 0) {
            setPos(this.lerpX, this.lerpY, this.lerpZ);
            this.lerpSteps--;
        }
        level().addParticle(ParticleTypes.SMOKE, true, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 256.0D * 256.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Type", this.entityData.get(DATA_TYPE));
        putVec(tag, "Origin", this.origin);
        putVec(tag, "Velocity", this.velocity);
        putVec(tag, "Target", this.target);
        tag.putDouble("Gravity", this.gravity);
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("TotalTicks", this.totalTicks);
        tag.putBoolean("Whistled", this.whistled);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_TYPE, tag.getInt("Type"));
        this.origin = getVec(tag, "Origin");
        this.velocity = getVec(tag, "Velocity");
        this.target = getVec(tag, "Target");
        this.gravity = tag.contains("Gravity") ? tag.getDouble("Gravity") : 0.05D;
        this.flightTicks = tag.getInt("FlightTicks");
        this.totalTicks = tag.getInt("TotalTicks");
        this.whistled = tag.getBoolean("Whistled");
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    private static void putVec(CompoundTag tag, String key, Vec3 v) {
        tag.putDouble(key + "X", v.x);
        tag.putDouble(key + "Y", v.y);
        tag.putDouble(key + "Z", v.z);
    }

    private static Vec3 getVec(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "X"), tag.getDouble(key + "Y"), tag.getDouble(key + "Z"));
    }
}
