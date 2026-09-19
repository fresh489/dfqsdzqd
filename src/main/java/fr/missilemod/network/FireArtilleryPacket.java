package fr.missilemod.network;

import fr.missilemod.block.ArtilleryBlockEntity;
import fr.missilemod.block.ArtilleryType;
import fr.missilemod.entity.ShellType;
import fr.missilemod.item.ShellItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> serveur : ordre de tir d'une piece d'artillerie. Tout est revalide cote serveur. */
public class FireArtilleryPacket {

    private final BlockPos gunPos;
    private final int x;
    private final int y;
    private final int z;
    private final int shell;

    public FireArtilleryPacket(BlockPos gunPos, int x, int y, int z, ShellType shell) {
        this(gunPos, x, y, z, shell.ordinal());
    }

    private FireArtilleryPacket(BlockPos gunPos, int x, int y, int z, int shell) {
        this.gunPos = gunPos;
        this.x = x;
        this.y = y;
        this.z = z;
        this.shell = shell;
    }

    public static void encode(FireArtilleryPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.gunPos);
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        buf.writeInt(msg.z);
        buf.writeVarInt(msg.shell);
    }

    public static FireArtilleryPacket decode(FriendlyByteBuf buf) {
        return new FireArtilleryPacket(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readVarInt());
    }

    public static void handle(FireArtilleryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            msg.apply(player);
        }
        ctx.get().setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(this.gunPos)
                || !(level.getBlockEntity(this.gunPos) instanceof ArtilleryBlockEntity gun)
                || player.distanceToSqr(Vec3.atCenterOf(this.gunPos)) > 8.0D * 8.0D) {
            return;
        }
        if (!gun.isReady()) {
            error(player, "message.missilemod.gun_busy");
            return;
        }
        ArtilleryType type = gun.getArtilleryType();
        ShellType shellType = ShellType.byId(this.shell);
        if (!type.accepts(shellType)) {
            error(player, "message.missilemod.wrong_caliber");
            return;
        }
        BlockPos targetPos = new BlockPos(this.x, this.y, this.z);
        if (!level.getWorldBorder().isWithinBounds(targetPos)) {
            error(player, "message.missilemod.out_of_border");
            return;
        }
        if (level.isOutsideBuildHeight(targetPos)) {
            error(player, "message.missilemod.out_of_height", level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            return;
        }
        Vec3 target = Vec3.atBottomCenterOf(targetPos).add(0.0D, 0.5D, 0.0D);
        double dx = target.x - (this.gunPos.getX() + 0.5D);
        double dz = target.z - (this.gunPos.getZ() + 0.5D);
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        if (distance < gun.minRange()) {
            error(player, "message.missilemod.too_close", distance, gun.minRange());
            return;
        }
        if (distance > gun.maxRange()) {
            error(player, "message.missilemod.too_far", distance, gun.maxRange());
            return;
        }
        if (!player.getAbilities().instabuild && !consumeShell(player.getInventory(), shellType)) {
            error(player, "message.missilemod.no_shell");
            return;
        }
        gun.startFire(target, shellType, player);
        player.displayClientMessage(Component.translatable("message.missilemod.gun_firing",
                this.x, this.y, this.z, distance).withStyle(ChatFormatting.GOLD), false);
    }

    private static boolean consumeShell(Inventory inventory, ShellType shellType) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof ShellItem item && item.getShellType() == shellType) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static void error(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), false);
    }
}
