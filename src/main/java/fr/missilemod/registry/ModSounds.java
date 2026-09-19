package fr.missilemod.registry;

import fr.missilemod.MissileMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MissileMod.MOD_ID);

    public static final RegistryObject<SoundEvent> MISSILE_LAUNCH = register("missile.launch");
    public static final RegistryObject<SoundEvent> MISSILE_ENGINE = register("missile.engine");
    public static final RegistryObject<SoundEvent> MISSILE_EXPLOSION = register("missile.explosion");
    public static final RegistryObject<SoundEvent> FRAG_IMPACT = register("impact.frag");
    public static final RegistryObject<SoundEvent> SHELL_WHISTLE = register("shell.whistle");
    public static final RegistryObject<SoundEvent> CANNON_FIRE = register("artillery.cannon_fire");
    public static final RegistryObject<SoundEvent> MORTAR_FIRE = register("artillery.mortar_fire");
    public static final RegistryObject<SoundEvent> ALARM_BEEP = register("alarm.beep");
    public static final RegistryObject<SoundEvent> ALARM_FAST = register("alarm.fast");
    public static final RegistryObject<SoundEvent> SIREN = register("alarm.siren");
    public static final RegistryObject<SoundEvent> MINE_CLICK = register("mine.click");
    public static final RegistryObject<SoundEvent> JET_FLYBY = register("strike.jet_flyby");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MissileMod.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
