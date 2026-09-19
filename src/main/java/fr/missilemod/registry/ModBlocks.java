package fr.missilemod.registry;

import fr.missilemod.MissileMod;
import fr.missilemod.block.ArtilleryBlock;
import fr.missilemod.block.ArtilleryType;
import fr.missilemod.block.LandMineBlock;
import fr.missilemod.block.MissileAlarmBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MissileMod.MOD_ID);

    public static final RegistryObject<Block> LAND_MINE = BLOCKS.register("land_mine",
            () -> new LandMineBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F).noCollission().sound(SoundType.METAL).pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<Block> ANTI_TANK_MINE = BLOCKS.register("anti_tank_mine",
            () -> new LandMineBlock(true, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(0.8F).noCollission().sound(SoundType.METAL).pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<Block> MISSILE_ALARM = BLOCKS.register("missile_alarm",
            () -> new MissileAlarmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MissileAlarmBlock.ALERT) > 0 ? 10 : 0)));

    public static final Map<ArtilleryType, RegistryObject<Block>> ARTILLERY = new EnumMap<>(ArtilleryType.class);

    static {
        for (ArtilleryType type : ArtilleryType.values()) {
            ARTILLERY.put(type, BLOCKS.register(type.id, () -> new ArtilleryBlock(type,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(5.0F, 12.0F)
                            .sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())));
        }
    }

    private ModBlocks() {
    }
}
