package fr.missilemod;

import com.mojang.logging.LogUtils;
import fr.missilemod.config.MissileConfig;
import fr.missilemod.network.ModNetwork;
import fr.missilemod.registry.ModBlockEntities;
import fr.missilemod.registry.ModBlocks;
import fr.missilemod.registry.ModCreativeTabs;
import fr.missilemod.registry.ModEntities;
import fr.missilemod.registry.ModItems;
import fr.missilemod.registry.ModSounds;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;

@Mod(MissileMod.MOD_ID)
public class MissileMod {
    public static final String MOD_ID = "missilemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MissileMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModCreativeTabs.CREATIVE_TABS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MissileConfig.SPEC);
        ModNetwork.register();

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ForgeChunkManager.setForcedChunkLoadingCallback(MOD_ID, (level, helper) -> {
            // Au redemarrage du serveur, on purge les tickets : les missiles en vol
            // et les explosions en cours ne sont pas persistants cote tickets.
            new ArrayList<>(helper.getEntityTickets().keySet()).forEach(helper::removeAllTickets);
            new ArrayList<>(helper.getBlockTickets().keySet()).forEach(helper::removeAllTickets);
        }));
        LOGGER.info("MissileMod : initialisation terminee");
    }
}
