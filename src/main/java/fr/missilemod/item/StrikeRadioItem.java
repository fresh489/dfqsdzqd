package fr.missilemod.item;

import fr.missilemod.explosion.ClusterRelease;
import fr.missilemod.explosion.ImpactScheduler;
import fr.missilemod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Radio de frappe aerienne (2.7) : on vise un point, quelques secondes plus tard un avion (invisible) passe
 * dans un bruit de reacteur et largue 6 bombes en ligne sur la zone.
 */
public class StrikeRadioItem extends Item {

    private static final double MAX_DISTANCE = 300.0D;
    private static final int COOLDOWN_TICKS = 20 * 30;
    private static final int BOMBS = 6;

    public StrikeRadioItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(MAX_DISTANCE)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.missilemod.radio_no_target")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            BlockPos targetPos = hit.getBlockPos();
            Vec3 target = Vec3.atCenterOf(targetPos);
            player.displayClientMessage(Component.translatable("message.missilemod.radio_confirmed",
                    targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GOLD), false);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(),
                    SoundSource.PLAYERS, 0.8F, 1.6F);
            scheduleStrike(serverLevel, target, player);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void scheduleStrike(ServerLevel level, Vec3 target, Player player) {
        // L'avion arrive dans l'axe joueur -> cible.
        Vec3 flat = new Vec3(target.x - player.getX(), 0.0D, target.z - player.getZ());
        Vec3 heading = flat.lengthSqr() < 1.0E-3D ? new Vec3(1.0D, 0.0D, 0.0D) : flat.normalize();
        double altitude = target.y + 60.0D;
        double planeSpeed = 1.6D; // blocs par tick
        // Bruit de reacteur au-dessus de la zone 3 s apres l'appel
        ImpactScheduler.schedule(level, 60, () -> level.playSound(null, target.x, altitude, target.z,
                ModSounds.JET_FLYBY.get(), SoundSource.AMBIENT, 16.0F, 1.0F));
        // Largage en ligne, centre sur la cible, compte tenu de la derive due a la vitesse de l'avion.
        for (int i = 0; i < BOMBS; i++) {
            double offset = (i - (BOMBS - 1) / 2.0D) * 5.0D;
            Vec3 release = new Vec3(target.x, altitude, target.z)
                    .add(heading.scale(offset - planeSpeed * 0.4D * 40.0D));
            ImpactScheduler.schedule(level, 90 + i * 3, () -> ClusterRelease.airDrop(level, release,
                    heading.scale(planeSpeed * 0.4D), player));
        }
    }
}
