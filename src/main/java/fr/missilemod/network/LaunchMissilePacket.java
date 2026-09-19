package fr.missilemod.network;

import fr.missilemod.config.MissileConfig;
import fr.missilemod.entity.MissileEntity;
import fr.missilemod.entity.MissileState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> serveur : demande de lancement d'un missile vers des coordonnees.
 * Le serveur ne fait AUCUNE confiance au client et revalide tout.
 */
public record LaunchMissilePacket(int entityId, int x, int y, int z) {

    private static final double MAX_INTERACTION_DISTANCE_SQR = 8.0D * 8.0D;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
    }

    public static LaunchMissilePacket decode(FriendlyByteBuf buf) {
        return new LaunchMissilePacket(buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        // consumerMainThread : deja execute sur le thread serveur, paquet marque comme traite.
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            process(player);
        }
    }

    private void process(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(this.entityId);

        if (!(entity instanceof MissileEntity missile) || !missile.isAlive()) {
            error(player, "message.missilemod.missile_not_found");
            return;
        }
        if (missile.getState() != MissileState.POSED) {
            error(player, "message.missilemod.already_launched");
            return;
        }
        if (player.distanceToSqr(missile) > MAX_INTERACTION_DISTANCE_SQR) {
            error(player, "message.missilemod.too_far_from_missile");
            return;
        }

        BlockPos targetPos = new BlockPos(this.x, this.y, this.z);
        if (!level.getWorldBorder().isWithinBounds(targetPos)) {
            error(player, "message.missilemod.out_of_border");
            return;
        }
        if (level.isOutsideBuildHeight(targetPos)) {
            error(player, "message.missilemod.out_of_height",
                    level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            return;
        }

        double distance = Math.sqrt(missile.getCenter().distanceToSqr(Vec3.atCenterOf(targetPos)));
        int maxRange = (int) Math.round(MissileConfig.MAX_RANGE.get() * missile.getMissileType().range);
        if (distance > maxRange) {
            error(player, "message.missilemod.too_far", (int) Math.round(distance), maxRange);
            return;
        }

        missile.launch(player, targetPos);
        player.displayClientMessage(Component.translatable("message.missilemod.launched",
                        this.x, this.y, this.z, (int) Math.round(distance))
                .withStyle(ChatFormatting.GOLD), false);
    }

    private static void error(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), false);
    }
}
