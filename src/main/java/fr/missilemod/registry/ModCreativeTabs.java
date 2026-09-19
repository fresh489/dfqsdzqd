package fr.missilemod.registry;

import fr.missilemod.MissileMod;
import fr.missilemod.entity.MissileType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MissileMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MISSILES_TAB = CREATIVE_TABS.register("missiles",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.missilemod.missiles"))
                    .icon(() -> new ItemStack(ModItems.MISSILES.get(MissileType.SCUD).get()))
                    .displayItems((parameters, output) -> {
                        ModItems.MISSILES.values().forEach(item -> output.accept(item.get()));
                        ModItems.ARTILLERY.values().forEach(item -> output.accept(item.get()));
                        ModItems.SHELLS.values().forEach(item -> output.accept(item.get()));
                        List.of(ModItems.LAND_MINE, ModItems.ANTI_TANK_MINE, ModItems.MISSILE_ALARM, ModItems.STRIKE_RADIO)
                                .forEach(item -> output.accept(item.get()));
                        ModItems.SMOKE_GRENADES.values().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
