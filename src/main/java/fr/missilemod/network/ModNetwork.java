package fr.missilemod.network;

import fr.missilemod.MissileMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MissileMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(LaunchMissilePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LaunchMissilePacket::encode)
                .decoder(LaunchMissilePacket::decode)
                .consumerMainThread(LaunchMissilePacket::handle)
                .add();
        CHANNEL.messageBuilder(ShakePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShakePacket::encode)
                .decoder(ShakePacket::decode)
                .consumerMainThread(ShakePacket::handle)
                .add();
        CHANNEL.messageBuilder(FireArtilleryPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FireArtilleryPacket::encode)
                .decoder(FireArtilleryPacket::decode)
                .consumerMainThread(FireArtilleryPacket::handle)
                .add();
    }

    private ModNetwork() {
    }
}
