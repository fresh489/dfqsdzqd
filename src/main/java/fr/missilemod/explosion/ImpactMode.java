package fr.missilemod.explosion;

/**
 * AUTO   : mode HYBRID si Explosion Overhaul est installe, sinon CUSTOM.
 * HYBRID : vraie explosion Minecraft (reprise par Explosion Overhaul) + effets complementaires du mod.
 * CUSTOM : tout est gere par le mod (cratere irregulier, degats, tremblement, son retarde).
 */
public enum ImpactMode {
    AUTO,
    HYBRID,
    CUSTOM
}
