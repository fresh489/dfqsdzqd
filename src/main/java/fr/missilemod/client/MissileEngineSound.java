package fr.missilemod.client;

import fr.missilemod.entity.MissileEntity;
import fr.missilemod.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Son de moteur en boucle qui suit le missile et s'arrete a sa disparition. */
public class MissileEngineSound extends AbstractTickableSoundInstance {

    private final MissileEntity missile;

    public MissileEngineSound(MissileEntity missile) {
        super(ModSounds.MISSILE_ENGINE.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.missile = missile;
        this.looping = true;
        this.delay = 0;
        this.volume = 4.0F;   // > 1 : portee d'attenuation etendue
        this.pitch = 0.7F;
        this.x = missile.getX();
        this.y = missile.getY() + missile.centerOffset();
        this.z = missile.getZ();
    }

    @Override
    public void tick() {
        if (this.missile.isRemoved() || !this.missile.isFlying()) {
            stop();
            return;
        }
        this.x = this.missile.getX();
        this.y = this.missile.getY() + missile.centerOffset();
        this.z = this.missile.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
