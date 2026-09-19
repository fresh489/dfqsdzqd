package fr.missilemod.item;

import fr.missilemod.entity.MissileEntity;
import fr.missilemod.entity.MissileType;
import fr.missilemod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class MissileItem extends Item {

    private final MissileType type;

    public MissileItem(MissileType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public MissileType getMissileType() {
        return this.type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        // Comme un oeuf d'apparition : si le bloc clique n'a pas de collision (herbe haute), on pose dedans.
        BlockPos pos = clickedState.getCollisionShape(level, clicked).isEmpty()
                ? clicked
                : clicked.relative(context.getClickedFace());

        EntityType<MissileEntity> type = ModEntities.MISSILES.get(this.type).get();
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;

        AABB box = type.getAABB(x, y, z).deflate(1.0E-3D);
        if (!level.noCollision(box)
                || !level.getEntities((Entity) null, box, e -> !e.isSpectator()).isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            MissileEntity missile = type.create(serverLevel);
            if (missile == null) {
                return InteractionResult.FAIL;
            }
            Player player = context.getPlayer();
            missile.moveTo(x, y, z, 0.0F, 0.0F);
            // Convention du missile : yaw = atan2(dx, dz). Pour le regard du joueur, cela donne -yRot.
            missile.initPlaced(player != null ? -player.getYRot() : 0.0F);
            serverLevel.addFreshEntity(missile);
            serverLevel.playSound(null, x, y, z, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, pos);

            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
