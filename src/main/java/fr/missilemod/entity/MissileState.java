package fr.missilemod.entity;

public enum MissileState {
    POSED,      // pose au sol, en attente
    ASCENT,     // phase 1 : montee verticale
    PITCHOVER,  // phase 2 : bascule vers la cible
    CRUISE,     // phase 3 : croisiere puis pique
    EXPLODED,   // detone, sera retire
    DUD;        // rate : tombe sans exploser et reste au sol (1 sur 50 par defaut)

    public boolean isFlying() {
        return this == ASCENT || this == PITCHOVER || this == CRUISE;
    }

    public static MissileState byId(int id) {
        MissileState[] values = values();
        return id >= 0 && id < values.length ? values[id] : POSED;
    }
}
