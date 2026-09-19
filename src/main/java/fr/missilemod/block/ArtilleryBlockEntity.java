package fr.missilemod.block;

import fr.missilemod.config.MissileConfig;
import fr.missilemod.entity.Ballistics;
import fr.missilemod.entity.ShellEntity;
import fr.missilemod.entity.ShellType;
import fr.missilemod.network.ModNetwork;
import fr.missilemod.network.ShakePacket;
import fr.missilemod.registry.ModBlockEntities;
import fr.missilemod.registry.ModEntities;
import fr.missilemod.registry.ModItems;
import fr.missilemod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Logique de tir d'une piece d'artillerie :
 * IDLE -> AIMING (le tube s'oriente) -> LOADING (bruit mecanique) -> tir (flash, fumee, recul, poussiere, douille)
 * -> RELOADING -> IDLE.
 * Le client anime lui-meme l'orientation vers l'angle cible et le recul a partir de l'heure du dernier tir.
 */
public class ArtilleryBlockEntity extends BlockEntity {

    public enum Phase { IDLE, AIMING, LOADING, RELOADING }

    private static final int LOADING_TICKS = 30;

    private Phase phase = Phase.IDLE;
    private int phaseTicks;
    private float yaw;
    private float pitch = 20.0F;
    private float targetYaw;
    private float targetPitch = 20.0F;
    private long fireTime = Long.MIN_VALUE / 2;
    private boolean initialized;
    @Nullable
    private Vec3 target;
    private ShellType shell = ShellType.MORTAR;
    @Nullable
    private UUID ownerId;

    // Client : angles de l'image precedente pour l'interpolation
    private float prevYaw;
    private float prevPitch;

    public ArtilleryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARTILLERY.get(), pos, state);
    }

    public ArtilleryType getArtilleryType() {
        return getBlockState().getBlock() instanceof ArtilleryBlock block ? block.getArtilleryType() : ArtilleryType.MORTAR;
    }

    public Phase getPhase() {
        return this.phase;
    }

    public boolean isReady() {
        return this.phase == Phase.IDLE;
    }

    public float getYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.prevYaw, this.yaw);
    }

    public float getPitch(float partialTick) {
        return Mth.lerp(partialTick, this.prevPitch, this.pitch);
    }

    /** Recul du tube en pixels a l'instant donne : recul brutal (2 ticks) puis retour progressif (40 ticks). */
    public float getRecoil(float partialTick) {
        if (this.level == null) {
            return 0.0F;
        }
        float age = (float) (this.level.getGameTime() - this.fireTime) + partialTick;
        float max = getArtilleryType().recoilPixels;
        if (age < 0.0F || age > 42.0F) {
            return 0.0F;
        }
        if (age < 2.0F) {
            return max * age / 2.0F;
        }
        float t = 1.0F - (age - 2.0F) / 40.0F;
        return max * t * t;
    }

    private void initAngles() {
        if (!this.initialized) {
            this.initialized = true;
            Direction facing = getBlockState().hasProperty(ArtilleryBlock.FACING)
                    ? getBlockState().getValue(ArtilleryBlock.FACING) : Direction.NORTH;
            this.yaw = this.targetYaw = (float) Math.toDegrees(Math.atan2(facing.getStepX(), facing.getStepZ()));
            this.pitch = this.targetPitch = getArtilleryType() == ArtilleryType.MORTAR ? 60.0F : 10.0F;
            this.prevYaw = this.yaw;
            this.prevPitch = this.pitch;
        }
    }

    // =====================================================================
    // Geometrie du tir
    // =====================================================================

    /** Position des tourillons (axe d'elevation) dans le monde. */
    private Vec3 pivot(float yawDeg) {
        ArtilleryType type = getArtilleryType();
        double yawRad = Math.toRadians(yawDeg);
        return Vec3.atBottomCenterOf(this.worldPosition)
                .add(Math.sin(yawRad) * type.pivotZ, type.pivotY, Math.cos(yawRad) * type.pivotZ);
    }

    private static Vec3 direction(float yawDeg, float pitchDeg) {
        double yawRad = Math.toRadians(yawDeg);
        double pitchRad = Math.toRadians(pitchDeg);
        return new Vec3(Math.sin(yawRad) * Math.cos(pitchRad), Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
    }

    private Vec3 muzzle() {
        return pivot(this.yaw).add(direction(this.yaw, this.pitch).scale(getArtilleryType().muzzleDistance));
    }

    public double rangeMultiplier() {
        return MissileConfig.ARTILLERY_RANGE_MULTIPLIER.get();
    }

    public int maxRange() {
        return (int) Math.round(getArtilleryType().maxRange * rangeMultiplier());
    }

    public int minRange() {
        return getArtilleryType().minRange;
    }

    // =====================================================================
    // Commande de tir (appelee par FireArtilleryPacket apres validation)
    // =====================================================================

    public void startFire(Vec3 targetPos, ShellType shellType, ServerPlayer player) {
        initAngles();
        ArtilleryType type = getArtilleryType();
        Ballistics.Solution solution = Ballistics.solve(pivot(this.yaw), targetPos, type.apexFactor, type.minApex,
                type.maxApex, type.gravity);
        this.target = targetPos;
        this.shell = shellType;
        this.ownerId = player.getUUID();
        this.targetYaw = (float) solution.yawDegrees();
        this.targetPitch = Mth.clamp((float) solution.elevationDegrees(), type.minElevation, type.maxElevation);
        this.phase = Phase.AIMING;
        this.phaseTicks = 0;
        sync();
    }

    // =====================================================================
    // Ticks
    // =====================================================================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArtilleryBlockEntity gun) {
        gun.initAngles();
        ServerLevel serverLevel = (ServerLevel) level;
        gun.prevYaw = gun.yaw;
        gun.prevPitch = gun.pitch;
        gun.phaseTicks++;
        switch (gun.phase) {
            case AIMING -> {
                gun.approachTarget();
                if (Math.abs(Mth.wrapDegrees(gun.yaw - gun.targetYaw)) < 0.01F && Math.abs(gun.pitch - gun.targetPitch) < 0.01F) {
                    gun.phase = Phase.LOADING;
                    gun.phaseTicks = 0;
                    serverLevel.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
                    gun.sync();
                }
            }
            case LOADING -> {
                if (gun.phaseTicks == 12) {
                    serverLevel.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8F, 0.7F);
                } else if (gun.phaseTicks == 22) {
                    serverLevel.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
                } else if (gun.phaseTicks >= LOADING_TICKS) {
                    gun.fire(serverLevel);
                    gun.phase = Phase.RELOADING;
                    gun.phaseTicks = 0;
                    gun.sync();
                }
            }
            case RELOADING -> {
                if (gun.phaseTicks >= gun.getArtilleryType().reloadTicks) {
                    gun.phase = Phase.IDLE;
                    gun.phaseTicks = 0;
                    serverLevel.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 0.8F, 0.8F);
                    gun.sync();
                }
            }
            default -> {
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ArtilleryBlockEntity gun) {
        gun.initAngles();
        gun.prevYaw = gun.yaw;
        gun.prevPitch = gun.pitch;
        gun.approachTarget();
    }

    private void approachTarget() {
        float rate = getArtilleryType().aimRate;
        this.yaw = Mth.approachDegrees(this.yaw, this.targetYaw, rate);
        this.pitch = Mth.approach(this.pitch, this.targetPitch, rate);
    }

    private void fire(ServerLevel level) {
        if (this.target == null) {
            return;
        }
        ArtilleryType type = getArtilleryType();
        Vec3 muzzle = muzzle();
        Vec3 dir = direction(this.yaw, this.pitch);
        Entity owner = this.ownerId != null ? level.getPlayerByUUID(this.ownerId) : null;

        ShellEntity shellEntity = ModEntities.SHELL.get().create(level);
        if (shellEntity != null) {
            Ballistics.Solution solution = Ballistics.solve(muzzle, this.target, type.apexFactor, type.minApex,
                    type.maxApex, type.gravity);
            shellEntity.launch(this.shell, muzzle, solution, this.target, owner);
            level.addFreshEntity(shellEntity);
        }
        this.fireTime = level.getGameTime();

        // Son puissant
        boolean mortar = type == ArtilleryType.MORTAR;
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z,
                mortar ? ModSounds.MORTAR_FIRE.get() : ModSounds.CANNON_FIRE.get(),
                SoundSource.BLOCKS, mortar ? 5.0F : 12.0F, 0.9F + level.random.nextFloat() * 0.2F);

        // Flash a la bouche + grosse fumee dans l'axe du tube
        level.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, mortar ? 1 : 3, 0.2D, 0.2D, 0.2D, 0.0D);
        for (int i = 0; i < (mortar ? 6 : 14); i++) {
            Vec3 p = muzzle.add(dir.scale(i * 0.35D));
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 3, 0.25D, 0.25D, 0.25D, 0.02D);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y, p.z, 2, 0.3D, 0.3D, 0.3D, 0.01D);
        }
        // Poussiere soulevee autour de la piece
        BlockState ground = level.getBlockState(this.worldPosition.below());
        if (!ground.isAir()) {
            BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            double radius = mortar ? 1.5D : 3.0D;
            for (int k = 0; k < 24; k++) {
                double a = k / 24.0D * Math.PI * 2.0D;
                double x = this.worldPosition.getX() + 0.5D + Math.cos(a) * radius;
                double z = this.worldPosition.getZ() + 0.5D + Math.sin(a) * radius;
                level.sendParticles(dust, x, this.worldPosition.getY() + 0.1D, z, 3, 0.4D, 0.1D, 0.4D, 0.15D);
                level.sendParticles(ParticleTypes.POOF, x, this.worldPosition.getY() + 0.2D, z, 1, 0.3D, 0.1D, 0.3D, 0.02D);
            }
        }
        // Douille ejectee a l'arriere
        if (type.ejectsCasing) {
            Vec3 breech = pivot(this.yaw).subtract(dir.scale(1.2D));
            ItemEntity casing = new ItemEntity(level, breech.x, breech.y, breech.z, new ItemStack(ModItems.SHELL_CASING.get()));
            casing.setDeltaMovement(-dir.x * 0.15D + (level.random.nextDouble() - 0.5D) * 0.1D, 0.25D,
                    -dir.z * 0.15D + (level.random.nextDouble() - 0.5D) * 0.1D);
            casing.setPickUpDelay(20);
            level.addFreshEntity(casing);
        }
        // Tremblement de camera pour les joueurs proches
        double reach = mortar ? 16.0D : 40.0D;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(reach))) {
            double distance = player.position().distanceTo(muzzle);
            if (distance < reach) {
                float strength = (float) (type.shake * (1.0D - distance / reach));
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new ShakePacket(strength, (int) (8 + 20 * strength)));
            }
        }
    }

    // =====================================================================
    // Synchronisation et sauvegarde
    // =====================================================================

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Phase", this.phase.name());
        tag.putInt("PhaseTicks", this.phaseTicks);
        tag.putFloat("Yaw", this.yaw);
        tag.putFloat("Pitch", this.pitch);
        tag.putFloat("TargetYaw", this.targetYaw);
        tag.putFloat("TargetPitch", this.targetPitch);
        tag.putLong("FireTime", this.fireTime);
        tag.putBoolean("Initialized", this.initialized);
        tag.putInt("Shell", this.shell.ordinal());
        if (this.target != null) {
            tag.putDouble("TargetX", this.target.x);
            tag.putDouble("TargetY", this.target.y);
            tag.putDouble("TargetZ", this.target.z);
        }
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        try {
            this.phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException e) {
            this.phase = Phase.IDLE;
        }
        this.phaseTicks = tag.getInt("PhaseTicks");
        boolean wasInitialized = this.initialized;
        this.initialized = tag.getBoolean("Initialized");
        if (this.initialized) {
            float loadedYaw = tag.getFloat("Yaw");
            float loadedPitch = tag.getFloat("Pitch");
            if (!wasInitialized || this.level == null || !this.level.isClientSide) {
                // Premier chargement : on prend l'angle tel quel. Ensuite, le client anime lui-meme vers la cible.
                this.yaw = this.prevYaw = loadedYaw;
                this.pitch = this.prevPitch = loadedPitch;
            }
        }
        this.targetYaw = tag.getFloat("TargetYaw");
        this.targetPitch = tag.getFloat("TargetPitch");
        this.fireTime = tag.getLong("FireTime");
        this.shell = ShellType.byId(tag.getInt("Shell"));
        this.target = tag.contains("TargetX")
                ? new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"))
                : null;
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(8.0D);
    }
}
