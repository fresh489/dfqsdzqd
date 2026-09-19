package fr.missilemod.explosion;

import fr.missilemod.MissileMod;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.UUID;

/**
 * Explosion personnalisee (mode CUSTOM), etalee sur plusieurs ticks.
 * Cratere irregulier : bruit lisse + petite variation aleatoire par bloc, et fond aplati
 * (cuvette plus large que profonde) pour eviter une sphere parfaite.
 *
 * Phase 1 (WAIT_CHUNKS) : les chunks couverts sont forces et on attend qu'ils soient charges.
 * Phase 2 (CARVE)       : parcours du cube englobant couche par couche, du haut vers le bas.
 *                         Les blocs sont retires avec UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS :
 *                         pas de mise a jour des voisins, pas de drops, et les changements sont regroupes
 *                         par section dans un seul paquet par tick.
 * Phase 3 (SHELL)       : mise a jour des blocs de la coque (bord du cratere) pour que le sable tombe,
 *                         que l'eau coule, que les portes orphelines disparaissent, etc.
 *
 * Eclairage : en 1.20.1, le moteur de lumiere met les verifications en file et les traite de maniere
 * asynchrone, en lot. Le recalcul se fait donc naturellement "a la fin", sans bloquer le tick.
 */
public class SphericalExplosion {

    private static final int CARVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int MAX_CHUNK_WAIT_TICKS = 100;
    private static final float EDGE_KEEP_CHANCE = 0.6F;

    private enum Phase { WAIT_CHUNKS, CARVE, SHELL, DONE }

    private final ServerLevel level;
    private final BlockPos center;
    private final int radius;
    private final float irregularity;
    private final int extent;
    private final float resistanceLimit;
    private final double extentSq;
    private final long seed;
    private final UUID ticketOwner = UUID.randomUUID();
    private final LongList chunks = new LongArrayList();
    private final LongList shell = new LongArrayList();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    private Phase phase = Phase.WAIT_CHUNKS;
    private int dx;
    private int dy;
    private int dz;
    private int shellIndex;
    private int waitTicks;
    private boolean ticketsReleased;

    public SphericalExplosion(ServerLevel level, BlockPos center, int radius, float irregularity, float resistanceLimit) {
        this.level = level;
        this.center = center.immutable();
        this.radius = radius;
        this.irregularity = irregularity;
        this.resistanceLimit = resistanceLimit;
        this.extent = radius + (int) Math.ceil(irregularity) + 2;
        this.extentSq = (double) this.extent * this.extent;
        this.seed = level.random.nextLong();

        this.dx = -this.extent;
        this.dy = this.extent;
        this.dz = -this.extent;

        int minCx = (center.getX() - this.extent - 2) >> 4;
        int maxCx = (center.getX() + this.extent + 2) >> 4;
        int minCz = (center.getZ() - this.extent - 2) >> 4;
        int maxCz = (center.getZ() + this.extent + 2) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                this.chunks.add(ChunkPos.asLong(cx, cz));
            }
        }
    }

    public ServerLevel level() {
        return this.level;
    }

    public boolean isDone() {
        return this.phase == Phase.DONE;
    }

    public void start() {
        for (int i = 0; i < this.chunks.size(); i++) {
            long key = this.chunks.getLong(i);
            ForgeChunkManager.forceChunk(this.level, MissileMod.MOD_ID, this.ticketOwner,
                    ChunkPos.getX(key), ChunkPos.getZ(key), true, false);
        }
    }

    public void finish() {
        if (!this.ticketsReleased) {
            this.ticketsReleased = true;
            for (int i = 0; i < this.chunks.size(); i++) {
                long key = this.chunks.getLong(i);
                ForgeChunkManager.forceChunk(this.level, MissileMod.MOD_ID, this.ticketOwner,
                        ChunkPos.getX(key), ChunkPos.getZ(key), false, false);
            }
        }
        this.phase = Phase.DONE;
    }

    /** @return le nombre d'unites de budget consommees. */
    public int tick(int budget) {
        switch (this.phase) {
            case WAIT_CHUNKS:
                this.waitTicks++;
                if (allChunksLoaded() || this.waitTicks >= MAX_CHUNK_WAIT_TICKS) {
                    this.phase = Phase.CARVE;
                }
                return 0;
            case CARVE:
                return carve(budget);
            case SHELL:
                return updateShell(budget);
            default:
                return 0;
        }
    }

    private boolean allChunksLoaded() {
        for (int i = 0; i < this.chunks.size(); i++) {
            long key = this.chunks.getLong(i);
            if (!this.level.getChunkSource().hasChunk(ChunkPos.getX(key), ChunkPos.getZ(key))) {
                return false;
            }
        }
        return true;
    }

    private int carve(int budget) {
        int changed = 0;
        int scanned = 0;
        int scanLimit = budget * 8;
        while (changed < budget && scanned < scanLimit) {
            if (this.dy < -this.extent) {
                this.phase = Phase.SHELL;
                break;
            }
            int distSq = this.dx * this.dx + this.dy * this.dy + this.dz * this.dz;
            if (distSq <= this.extentSq) {
                this.cursor.set(this.center.getX() + this.dx, this.center.getY() + this.dy, this.center.getZ() + this.dz);
                if (!this.level.isOutsideBuildHeight(this.cursor)) {
                    // Sous le centre, la distance verticale compte plus : cuvette plus large que profonde.
                    double vertical = this.dy < 0 ? this.dy * 1.35D : this.dy;
                    double dist = Math.sqrt(this.dx * this.dx + vertical * vertical + this.dz * this.dz);
                    double limit = this.radius + edgeNoise(this.dx, this.dy, this.dz);
                    boolean inside = dist <= limit - 1.0D
                            || (dist <= limit && this.level.random.nextFloat() < EDGE_KEEP_CHANCE);
                    if (inside && destroyBlock(this.cursor)) {
                        changed++;
                    }
                    if (dist > limit - 1.0D && dist <= limit + 1.5D) {
                        this.shell.add(this.cursor.asLong());
                    }
                }
            }
            scanned++;
            advanceCursor();
        }
        return changed;
    }

    private void advanceCursor() {
        if (++this.dz > this.extent) {
            this.dz = -this.extent;
            if (++this.dx > this.extent) {
                this.dx = -this.extent;
                this.dy--;
            }
        }
    }

    // ---------- Bruit du bord du cratere ----------

    /** Decalage du bord en blocs : grosses bosses lisses + petite variation propre a chaque bloc. */
    private double edgeNoise(int x, int y, int z) {
        double smooth = valueNoise(x * 0.22D, y * 0.22D, z * 0.22D) * this.irregularity;
        double jitter = (lattice(x + 7919, y - 104729, z + 1299709) * 0.5D) * 0.6D;
        return smooth + jitter;
    }

    private double valueNoise(double x, double y, double z) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double c00 = lerp(fx, lattice(x0, y0, z0), lattice(x0 + 1, y0, z0));
        double c10 = lerp(fx, lattice(x0, y0 + 1, z0), lattice(x0 + 1, y0 + 1, z0));
        double c01 = lerp(fx, lattice(x0, y0, z0 + 1), lattice(x0 + 1, y0, z0 + 1));
        double c11 = lerp(fx, lattice(x0, y0 + 1, z0 + 1), lattice(x0 + 1, y0 + 1, z0 + 1));
        return lerp(fz, lerp(fy, c00, c10), lerp(fy, c01, c11));
    }

    /** Valeur pseudo-aleatoire stable dans [-1, 1] pour un point entier. */
    private double lattice(int x, int y, int z) {
        long h = this.seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return ((h >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private boolean destroyBlock(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(this.level, pos) < 0.0F) {
            return false; // incassable : bedrock, barriere, portail de l'End, etc.
        }
        if (state.getBlock().getExplosionResistance() > this.resistanceLimit) {
            return false;
        }
        // Position immuable : certaines methodes vanilla conservent la reference (ticks planifies, etc.).
        BlockPos immutablePos = pos.immutable();
        if (state.hasBlockEntity()) {
            Clearable.tryClear(this.level.getBlockEntity(immutablePos)); // vide les coffres : aucun item au sol
        }
        return this.level.setBlock(immutablePos, Blocks.AIR.defaultBlockState(), CARVE_FLAGS);
    }

    private int updateShell(int budget) {
        int updated = 0;
        int scanned = 0;
        int scanLimit = budget * 4;
        while (updated < budget && scanned < scanLimit && this.shellIndex < this.shell.size()) {
            scanned++;
            BlockPos pos = BlockPos.of(this.shell.getLong(this.shellIndex++));
            BlockState state = this.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            BlockState newState = Block.updateFromNeighbourShapes(state, this.level, pos);
            if (newState != state) {
                this.level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
            }
            this.level.neighborChanged(pos, Blocks.AIR, this.center);
            updated++;
        }
        if (this.shellIndex >= this.shell.size()) {
            this.phase = Phase.DONE;
        }
        return updated;
    }
}
