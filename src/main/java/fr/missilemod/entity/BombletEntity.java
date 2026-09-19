package fr.missilemod.entity;

import fr.missilemod.block.LandMineBlock;
import fr.missilemod.explosion.ImpactProfiles;
import fr.missilemod.explosion.ImpactSystem;
import fr.missilemod.registry.ModBlocks;
import fr.missilemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sous-munition (bombe a sous-munitions, 2.6) ou bombe d'avion (frappe aerienne, 2.7).
 * Tombe par gravite et explose au contact. Une sous-munition "ratee" devient une mine au sol.
 */
public class BombletEntity extends ThrowableItemProjectile implements IncomingMunition {

    private boolean big;
    private boolean dud;

    public BombletEntity(EntityType<? extends BombletEntity> type, Level level) {
        super(type, level);
    }

    public void setup(boolean big, boolean dud) {
        this.big = big;
        this.dud = dud;
        setItem(new ItemStack(big ? ModItems.AERIAL_BOMB.get() : ModItems.BOMBLET.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOMBLET.get();
    }

    @Override
    protected float getGravity() {
        return this.big ? 0.06F : 0.05F;
    }

    @Override
    public boolean isIncomingMunition() {
        return isAlive();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && this.random.nextInt(this.big ? 1 : 3) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.02D, 0.0D);
        }
        if (!level().isClientSide && getY() < level().getMinBuildHeight() - 16) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        Vec3 at = result.getLocation();
        if (this.dud && !this.big) {
            placeDudMine(level, result);
        } else {
            Entity owner = getOwner();
            ImpactSystem.detonate(level, at, this, owner, this.big ? ImpactProfiles.AERIAL_BOMB : ImpactProfiles.BOMBLET);
        }
        discard();
    }

    private void placeDudMine(ServerLevel level, HitResult result) {
        BlockPos pos = result instanceof BlockHitResult blockHit
                ? blockHit.getBlockPos().relative(blockHit.getDirection())
                : BlockPos.containing(result.getLocation());
        BlockState mine = ModBlocks.LAND_MINE.get().defaultBlockState();
        if (level.getBlockState(pos).canBeReplaced() && mine.canSurvive(level, pos)) {
            level.setBlock(pos, mine.setValue(LandMineBlock.FROM_BOMBLET, true), 3);
        }
        level.playSound(null, pos, SoundEvents.METAL_HIT, SoundSource.BLOCKS, 1.0F, 1.4F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Big", this.big);
        tag.putBoolean("Dud", this.dud);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.big = tag.getBoolean("Big");
        this.dud = tag.getBoolean("Dud");
    }
}
