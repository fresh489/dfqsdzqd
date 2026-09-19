package fr.missilemod.client;

import fr.missilemod.MissileMod;
import fr.missilemod.entity.MissileType;
import fr.missilemod.registry.ModBlockEntities;
import fr.missilemod.registry.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MissileMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (MissileType type : MissileType.values()) {
            event.registerEntityRenderer(ModEntities.MISSILES.get(type).get(), context -> new MissileRenderer(context, type));
        }
        event.registerEntityRenderer(ModEntities.SHELL.get(), ShellRenderer::new);
        event.registerEntityRenderer(ModEntities.BOMBLET.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SMOKE_GRENADE.get(), ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ARTILLERY.get(), ArtilleryRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ModModels.register(event);
    }

    private ClientModEvents() {
    }
}
