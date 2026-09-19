package fr.missilemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.missilemod.MissileMod;
import fr.missilemod.explosion.ImpactProfile;
import fr.missilemod.explosion.ImpactProfiles;
import fr.missilemod.explosion.ImpactSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /missileimpact <profil>         : impact sur le bloc vise (jusqu'a 256 blocs)
 * /missileimpact <profil> x y z   : impact aux coordonnees
 * Reserve aux operateurs (niveau 2), pour tester les calibres sans lancer de missile.
 */
@Mod.EventBusSubscriber(modid = MissileMod.MOD_ID)
public final class ImpactCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("missileimpact")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("profil", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ImpactProfiles.BY_NAME.keySet(), builder))
                        .executes(ctx -> runAtLook(ctx))
                        .then(Commands.argument("position", BlockPosArgument.blockPos())
                                .executes(ctx -> run(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "position"))))));
    }

    private static int runAtLook(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(256.0D));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            ctx.getSource().sendFailure(Component.translatable("command.missilemod.no_target"));
            return 0;
        }
        // Impact a la surface du bloc vise (case d'air devant la face touchee).
        return run(ctx, hit.getBlockPos().relative(hit.getDirection()));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, BlockPos pos) {
        String name = StringArgumentType.getString(ctx, "profil");
        ImpactProfile profile = ImpactProfiles.BY_NAME.get(name);
        if (profile == null) {
            ctx.getSource().sendFailure(Component.translatable("command.missilemod.unknown_profile", name,
                    String.join(", ", ImpactProfiles.BY_NAME.keySet())));
            return 0;
        }
        ServerLevel level = ctx.getSource().getLevel();
        ImpactSystem.detonate(level, Vec3.atCenterOf(pos), null, ctx.getSource().getEntity(), profile);
        String mode = ImpactSystem.isHybridMode() ? "HYBRID" : "CUSTOM";
        ctx.getSource().sendSuccess(() -> Component.translatable("command.missilemod.impact", name,
                pos.getX(), pos.getY(), pos.getZ(), mode), true);
        return 1;
    }

    private ImpactCommand() {
    }
}
