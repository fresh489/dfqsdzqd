package fr.missilemod.registry;

import fr.missilemod.MissileMod;
import fr.missilemod.block.ArtilleryType;
import fr.missilemod.entity.MissileType;
import fr.missilemod.entity.ShellType;
import fr.missilemod.item.MissileItem;
import fr.missilemod.item.ShellItem;
import fr.missilemod.item.SmokeGrenadeItem;
import fr.missilemod.item.StrikeRadioItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MissileMod.MOD_ID);

    public static final Map<MissileType, RegistryObject<Item>> MISSILES = new EnumMap<>(MissileType.class);
    public static final Map<ShellType, RegistryObject<Item>> SHELLS = new EnumMap<>(ShellType.class);
    public static final Map<SmokeGrenadeItem.SmokeColor, RegistryObject<Item>> SMOKE_GRENADES =
            new EnumMap<>(SmokeGrenadeItem.SmokeColor.class);
    public static final Map<ArtilleryType, RegistryObject<Item>> ARTILLERY = new EnumMap<>(ArtilleryType.class);

    static {
        for (MissileType type : MissileType.values()) {
            Rarity rarity = type == MissileType.STRATEGIC ? Rarity.EPIC
                    : type == MissileType.SCUD || type == MissileType.SCUD_FRAG ? Rarity.RARE : Rarity.COMMON;
            MISSILES.put(type, ITEMS.register(type.itemId,
                    () -> new MissileItem(type, new Item.Properties().stacksTo(type == MissileType.STRATEGIC ? 1 : 4).rarity(rarity))));
        }
        for (ShellType type : ShellType.values()) {
            SHELLS.put(type, ITEMS.register(type.id, () -> new ShellItem(type, new Item.Properties().stacksTo(16))));
        }
        for (SmokeGrenadeItem.SmokeColor color : SmokeGrenadeItem.SmokeColor.values()) {
            SMOKE_GRENADES.put(color, ITEMS.register("smoke_grenade_" + color.id,
                    () -> new SmokeGrenadeItem(color, new Item.Properties().stacksTo(16))));
        }
        for (ArtilleryType type : ArtilleryType.values()) {
            ARTILLERY.put(type, ITEMS.register(type.id,
                    () -> new BlockItem(ModBlocks.ARTILLERY.get(type).get(), new Item.Properties().stacksTo(1))));
        }
    }

    /** Conserve pour le code existant : missile AIM-120. */
    public static final RegistryObject<Item> MISSILE_AIM120 = MISSILES.get(MissileType.AIM120);

    public static final RegistryObject<Item> LAND_MINE = ITEMS.register("land_mine",
            () -> new BlockItem(ModBlocks.LAND_MINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANTI_TANK_MINE = ITEMS.register("anti_tank_mine",
            () -> new BlockItem(ModBlocks.ANTI_TANK_MINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MISSILE_ALARM = ITEMS.register("missile_alarm",
            () -> new BlockItem(ModBlocks.MISSILE_ALARM.get(), new Item.Properties()));
    public static final RegistryObject<Item> BOMBLET = ITEMS.register("bomblet", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AERIAL_BOMB = ITEMS.register("aerial_bomb", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHELL_CASING = ITEMS.register("shell_casing", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STRIKE_RADIO = ITEMS.register("strike_radio",
            () -> new StrikeRadioItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private ModItems() {
    }
}
