package fr.missilemod.block;

import fr.missilemod.entity.ShellType;

/**
 * Pieces d'artillerie (partie 3). Dimensions du modele en blocs : pivot = axe d'elevation (tourillons),
 * muzzle = distance du pivot a la bouche du tube.
 */
public enum ArtilleryType {
    //       id          calibre                      sommet  min  max   gravite pivotY  pivotZ   bouche  vit.  elev. min/max  portee min/max  recharge recul  secousse douille
    MORTAR("mortar", ShellType.Caliber.MORTAR_81, 0.9D, 6.0D, 300.0D, 0.08D, 0.1875D, 0.0D, 1.625D, 4.0F, 40.0F, 87.0F, 16, 320, 60, 1.5F, 0.3F, false),
    M777_105("m777_105", ShellType.Caliber.C105, 0.25D, 8.0D, 400.0D, 0.06D, 1.5D, -0.125D, 4.55D, 1.5F, -3.0F, 72.0F, 40, 700, 100, 6.0F, 0.6F, true),
    M777_155("m777_155", ShellType.Caliber.C155, 0.28D, 10.0D, 500.0D, 0.06D, 1.5D, -0.125D, 6.3D, 1.2F, -3.0F, 72.0F, 60, 1200, 160, 8.0F, 0.9F, true);

    public final String id;
    public final ShellType.Caliber caliber;
    public final double apexFactor;
    public final double minApex;
    public final double maxApex;
    public final double gravity;
    public final double pivotY;
    public final double pivotZ;
    public final double muzzleDistance;
    public final float aimRate;
    public final float minElevation;
    public final float maxElevation;
    public final int minRange;
    public final int maxRange;
    public final int reloadTicks;
    public final float recoilPixels;
    public final float shake;
    public final boolean ejectsCasing;

    ArtilleryType(String id, ShellType.Caliber caliber, double apexFactor, double minApex, double maxApex, double gravity,
                  double pivotY, double pivotZ, double muzzleDistance, float aimRate, float minElevation,
                  float maxElevation, int minRange, int maxRange, int reloadTicks, float recoilPixels, float shake,
                  boolean ejectsCasing) {
        this.id = id;
        this.caliber = caliber;
        this.apexFactor = apexFactor;
        this.minApex = minApex;
        this.maxApex = maxApex;
        this.gravity = gravity;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;
        this.muzzleDistance = muzzleDistance;
        this.aimRate = aimRate;
        this.minElevation = minElevation;
        this.maxElevation = maxElevation;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.reloadTicks = reloadTicks;
        this.recoilPixels = recoilPixels;
        this.shake = shake;
        this.ejectsCasing = ejectsCasing;
    }

    public boolean accepts(ShellType shell) {
        return shell.caliber == this.caliber;
    }
}
