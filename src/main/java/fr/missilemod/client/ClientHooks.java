package fr.missilemod.client;

import fr.missilemod.entity.MissileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Seul point d'entree du code commun vers le code client.
 * Toujours appele via DistExecutor : cette classe n'est jamais chargee sur un serveur dedie.
 */
public final class ClientHooks {

    public static void openTargetScreen(MissileEntity missile) {
        Minecraft.getInstance().setScreen(new TargetScreen(missile));
    }

    public static void openArtilleryScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ArtilleryScreen(pos));
    }

    public static void startEngineSound(MissileEntity missile) {
        Minecraft.getInstance().getSoundManager().play(new MissileEngineSound(missile));
    }

    public static void shakeCamera(float intensity, int duration) {
        CameraShake.add(intensity, duration);
    }

    private ClientHooks() {
    }
}
