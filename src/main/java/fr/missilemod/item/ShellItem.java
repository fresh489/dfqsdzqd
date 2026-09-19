package fr.missilemod.item;

import fr.missilemod.entity.ShellType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Obus en inventaire : il est consomme par le mortier ou l'obusier au moment du tir. */
public class ShellItem extends Item {

    private final ShellType type;

    public ShellItem(ShellType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public ShellType getShellType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.missilemod.caliber." + this.type.caliber.name().toLowerCase()));
    }
}
