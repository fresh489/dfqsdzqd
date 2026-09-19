package fr.missilemod.entity;

import net.minecraft.world.phys.Vec3;

/**
 * Trajectoire en cloche sans frottement : on choisit l'altitude du sommet, puis on calcule la vitesse initiale
 * pour arriver EXACTEMENT sur la cible apres un nombre entier de ticks.
 */
public final class Ballistics {

    /**
     * @param velocity vitesse initiale en blocs par tick
     * @param ticks    duree du vol en ticks
     * @param gravity  gravite en blocs par tick^2
     */
    public record Solution(Vec3 velocity, int ticks, double gravity) {

        /** Angle d'elevation du tir en degres (0 = horizontal, 90 = vertical). */
        public double elevationDegrees() {
            double horizontal = Math.sqrt(this.velocity.x * this.velocity.x + this.velocity.z * this.velocity.z);
            return Math.toDegrees(Math.atan2(this.velocity.y, horizontal));
        }

        /** Direction horizontale du tir en degres, convention yaw = atan2(dx, dz). */
        public double yawDegrees() {
            return Math.toDegrees(Math.atan2(this.velocity.x, this.velocity.z));
        }

        public Vec3 positionAt(Vec3 origin, double t) {
            return origin.add(this.velocity.x * t, this.velocity.y * t - 0.5D * this.gravity * t * t, this.velocity.z * t);
        }

        public Vec3 velocityAt(double t) {
            return new Vec3(this.velocity.x, this.velocity.y - this.gravity * t, this.velocity.z);
        }
    }

    /**
     * @param apexFactor hauteur du sommet au-dessus du point le plus haut, en fraction de la distance horizontale
     *                   (0,25 = obusier, environ 45 degres ; 0,9 = mortier, environ 75 degres)
     */
    public static Solution solve(Vec3 from, Vec3 to, double apexFactor, double minApex, double maxApex, double gravity) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double top = Math.max(from.y, to.y) + Math.min(maxApex, Math.max(minApex, horizontal * apexFactor));
        double up = Math.sqrt(2.0D * gravity * (top - from.y)) / gravity;
        double down = Math.sqrt(2.0D * (top - to.y) / gravity);
        int ticks = Math.max(2, (int) Math.ceil(up + down));
        double vy = (dy + 0.5D * gravity * ticks * ticks) / ticks;
        return new Solution(new Vec3(dx / ticks, vy, dz / ticks), ticks, gravity);
    }

    private Ballistics() {
    }
}
