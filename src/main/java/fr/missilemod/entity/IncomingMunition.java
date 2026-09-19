package fr.missilemod.entity;

/** Munition detectable par le bloc d'alerte anti-missile. */
public interface IncomingMunition {
    /** true tant que la munition est en vol. */
    boolean isIncomingMunition();
}
