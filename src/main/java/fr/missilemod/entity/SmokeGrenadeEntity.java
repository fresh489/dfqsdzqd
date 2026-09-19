package fr.missilemod.entity;

import fr.missilemod.item.SmokeGrenadeItem;
import fr.missilemod.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/**
 * Grenade fumigene (2.5) : rebondit, roule un peu, puis degage une fumee epaisse et opaque de sa couleur
 * pendant 15 a 20 secondes. La fumee est emise cote client (aucune charge reseau).
 */
public class SmokeGrenadeEntity extends Entity implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(SmokeGrenadeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_SMOKING =
            SynchedEntityData.defineId(SmokeGrenadeEntity.class, EntityDataSerializers.BOOLEAN);

    private int restTicks;
    private int smokeTicks;
    private int smokeDuration = 360;

    public SmokeGrenadeEntity(EntityType<? extends SmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(DATA_ITEM, stack.copyWithCount(1));
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = this.entityData.get(DATA_ITEM);
        return stack.isEmpty() ? new ItemStack(ModItems.SMOKE_GRENADES.get(SmokeGrenadeItem.SmokeColor.WHITE).get()) : stack;
    }

    private SmokeGrenadeItem.SmokeColor color() {
        return getItem().getItem() instanceof SmokeGrenadeItem grenade ? grenade.getColor() : SmokeGrenadeItem.SmokeColor.WHITE;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_SMOKING, false);
    }

    @Override
    public void tick() {
        super.tick();
        boolean smoking = this.entityData.get(DATA_SMOKING);
        Vec3 motion = getDeltaMovement();

        // Physique simple : gravite, rebonds amortis, roulement avec frottement.
        if (!isNoGravity()) {
            motion = motion.add(0.0D, -0.05D, 0.0D);
        }
        Vec3 before = motion;
        move(MoverType.SELF, motion);
        motion = getDeltaMovement();
        if (this.horizontalCollision) {
            motion = new Vec3(-before.x * 0.4D, motion.y, -before.z * 0.4D);
        }
        if (this.verticalCollision && before.y < -0.12D) {
            motion = new Vec3(motion.x * 0.6D, -before.y * 0.3D, motion.z * 0.6D);
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.METAL_HIT, SoundSource.PLAYERS, 0.5F, 1.6F);
            }
        } else if (onGround()) {
            motion = new Vec3(motion.x * 0.8D, motion.y, motion.z * 0.8D);  // roule puis s'arrete
        } else {
            motion = motion.scale(0.99D);
        }
        setDeltaMovement(motion);

        if (level().isClientSide) {
            if (smoking) {
                emitSmoke();
            }
            return;
        }
        if (!smoking) {
            if (onGround() && motion.horizontalDistanceSqr() < 0.001D) {
                if (++this.restTicks > 10) {
                    this.entityData.set(DATA_SMOKING, true);
                    this.smokeDuration = 300 + this.random.nextInt(101);
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
                }
            } else {
                this.restTicks = 0;
            }
            if (this.tickCount > 400) {
                this.entityData.set(DATA_SMOKING, true); // securite : fume meme s'il n'a jamais trouve le sol
            }
        } else {
            this.smokeTicks++;
            if (this.smokeTicks % 40 == 0) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 0.5F);
            }
            if (this.smokeTicks > this.smokeDuration) {
                discard();
            }
        }
        if (getY() < level().getMinBuildHeight() - 16) {
            discard();
        }
    }

    private void emitSmoke() {
        SmokeGrenadeItem.SmokeColor color = color();
        double spread = Math.min(3.5D, 0.4D + this.tickCount * 0.01D);
        for (int i = 0; i < 2; i++) {
            level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                    getX() + (this.random.nextDouble() - 0.5D) * 0.4D, getY() + 0.2D,
                    getZ() + (this.random.nextDouble() - 0.5D) * 0.4D,
                    (this.random.nextDouble() - 0.5D) * 0.03D, 0.02D + this.random.nextDouble() * 0.03D,
                    (this.random.nextDouble() - 0.5D) * 0.03D);
        }
        if (color != SmokeGrenadeItem.SmokeColor.WHITE) {
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(color.red, color.green, color.blue), 4.0F);
            for (int i = 0; i < 8; i++) {
                level().addParticle(dust, true,
                        getX() + (this.random.nextDouble() - 0.5D) * spread * 2.0D,
                        getY() + 0.3D + this.random.nextDouble() * spread * 1.5D,
                        getZ() + (this.random.nextDouble() - 0.5D) * spread * 2.0D,
                        0.0D, 0.0D, 0.0D);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                        getX() + (this.random.nextDouble() - 0.5D) * spread * 2.0D,
                        getY() + 0.2D + this.random.nextDouble() * spread,
                        getZ() + (this.random.nextDouble() - 0.5D) * spread * 2.0D,
                        0.0D, 0.01D, 0.0D);
            }
        }
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
        tag.put("Item", getItem().save(new CompoundTag()));
        tag.putBoolean("Smoking", this.entityData.get(DATA_SMOKING));
        tag.putInt("SmokeTicks", this.smokeTicks);
        tag.putInt("SmokeDuration", this.smokeDuration);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setItem(ItemStack.of(tag.getCompound("Item")));
        this.entityData.set(DATA_SMOKING, tag.getBoolean("Smoking"));
        this.smokeTicks = tag.getInt("SmokeTicks");
        this.smokeDuration = tag.contains("SmokeDuration") ? tag.getInt("SmokeDuration") : 360;
    }
}
