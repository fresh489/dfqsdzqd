package fr.missilemod.item;

import fr.missilemod.entity.SmokeGrenadeEntity;
import fr.missilemod.registry.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Grenade fumigene lancable a la main (2.5). */
public class SmokeGrenadeItem extends Item {

    public enum SmokeColor {
        WHITE("white", 0.95F, 0.95F, 0.95F),
        RED("red", 0.85F, 0.12F, 0.1F),
        GREEN("green", 0.2F, 0.7F, 0.2F),
        PURPLE("purple", 0.55F, 0.2F, 0.75F);

        public final String id;
        public final float red;
        public final float green;
        public final float blue;

        SmokeColor(String id, float red, float green, float blue) {
            this.id = id;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    private final SmokeColor color;

    public SmokeGrenadeItem(SmokeColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public SmokeColor getColor() {
        return this.color;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.6F, 0.6F);
        if (!level.isClientSide) {
            SmokeGrenadeEntity grenade = ModEntities.SMOKE_GRENADE.get().create(level);
            if (grenade != null) {
                grenade.setItem(stack);
                Vec3 eye = player.getEyePosition();
                grenade.setPos(eye.x, eye.y - 0.1D, eye.z);
                grenade.setDeltaMovement(player.getLookAngle().scale(0.9D).add(player.getDeltaMovement()));
                level.addFreshEntity(grenade);
            }
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
