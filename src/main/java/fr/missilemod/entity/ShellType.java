package fr.missilemod.entity;

import fr.missilemod.explosion.ImpactProfile;
import fr.missilemod.explosion.ImpactProfiles;

import java.util.function.Supplier;

/** Obus d'artillerie et de mortier (cahier des charges 2.3 et 2.6). */
public enum ShellType {
    //          item / texture        modele          calibre            sous-mun. profil
    MORTAR("shell_mortar", "mortar_shell", Caliber.MORTAR_81, false, () -> ImpactProfiles.MORTAR),
    SHELL_105_HE("shell_105_he", "shell_105", Caliber.C105, false, () -> ImpactProfiles.SHELL_105_HE),
    SHELL_105_FRAG("shell_105_frag", "shell_105", Caliber.C105, false, () -> ImpactProfiles.SHELL_105_FRAG),
    SHELL_155_HE("shell_155_he", "shell_155", Caliber.C155, false, () -> ImpactProfiles.SHELL_155_HE),
    SHELL_155_FRAG("shell_155_frag", "shell_155", Caliber.C155, false, () -> ImpactProfiles.SHELL_155_FRAG),
    SHELL_155_CLUSTER("shell_155_cluster", "shell_155", Caliber.C155, true, () -> ImpactProfiles.SHELL_155_HE);

    public enum Caliber {
        MORTAR_81,
        C105,
        C155
    }

    public final String id;
    public final String model;
    public final Caliber caliber;
    public final boolean cluster;
    private final Supplier<ImpactProfile> profile;

    ShellType(String id, String model, Caliber caliber, boolean cluster, Supplier<ImpactProfile> profile) {
        this.id = id;
        this.model = model;
        this.caliber = caliber;
        this.cluster = cluster;
        this.profile = profile;
    }

    public ImpactProfile profile() {
        return this.profile.get();
    }

    public static ShellType byId(int ordinal) {
        ShellType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MORTAR;
    }
}
