package fr.missilemod.client;

import fr.missilemod.MissileMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tremblement de camera cote client. L'intensite arrive deja ponderee par la puissance et la distance ;
 * elle decroit ensuite jusqu'a zero sur la duree recue.
 */
@Mod.EventBusSubscriber(modid = MissileMod.MOD_ID, value = Dist.CLIENT)
public final class CameraShake {

    private static final float MAX_DEGREES = 3.5F;

    private static float intensity;
    private static int duration;
    private static int remaining;

    public static void add(float newIntensity, int newDuration) {
        float current = remaining > 0 ? intensity * remaining / (float) duration : 0.0F;
        if (newIntensity >= current) {
            intensity = Math.min(newIntensity, 2.0F);
            duration = Math.max(1, newDuration);
            remaining = duration;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && remaining > 0 && !Minecraft.getInstance().isPaused()) {
            remaining--;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (remaining <= 0 || mc.level == null || mc.isPaused()) {
            return;
        }
        double partial = event.getPartialTick();
        double t = Math.max(0.0D, Math.min(1.0D, (remaining - partial) / duration));
        float amplitude = (float) (intensity * t * t) * MAX_DEGREES;
        double time = (mc.level.getGameTime() + partial) * 1.7D;
        // Somme de sinus a frequences non multiples : secousse irreguliere mais continue.
        float pitch = (float) (Math.sin(time * 2.3D) + 0.5D * Math.sin(time * 5.1D + 1.3D));
        float yaw = (float) (Math.sin(time * 1.9D + 2.1D) + 0.5D * Math.sin(time * 4.3D));
        float roll = (float) (Math.sin(time * 2.9D + 0.7D));
        event.setPitch(event.getPitch() + pitch * amplitude * 0.6F);
        event.setYaw(event.getYaw() + yaw * amplitude * 0.6F);
        event.setRoll(event.getRoll() + roll * amplitude * 0.4F);
    }

    private CameraShake() {
    }
}
