package fr.missilemod.registry;

import fr.missilemod.MissileMod;
import fr.missilemod.block.AlarmBlockEntity;
import fr.missilemod.block.ArtilleryBlockEntity;
import fr.missilemod.block.ArtilleryType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MissileMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<AlarmBlockEntity>> MISSILE_ALARM = BLOCK_ENTITIES.register(
            "missile_alarm", () -> BlockEntityType.Builder.of(AlarmBlockEntity::new, ModBlocks.MISSILE_ALARM.get()).build(null));

    public static final RegistryObject<BlockEntityType<ArtilleryBlockEntity>> ARTILLERY = BLOCK_ENTITIES.register(
            "artillery", () -> BlockEntityType.Builder.of(ArtilleryBlockEntity::new, artilleryBlocks()).build(null));

    private static Block[] artilleryBlocks() {
        ArtilleryType[] types = ArtilleryType.values();
        Block[] blocks = new Block[types.length];
        for (int i = 0; i < types.length; i++) {
            blocks[i] = ModBlocks.ARTILLERY.get(types[i]).get();
        }
        return blocks;
    }

    private ModBlockEntities() {
    }
}
