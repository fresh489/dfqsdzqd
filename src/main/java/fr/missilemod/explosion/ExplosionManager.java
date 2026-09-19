package fr.missilemod.explosion;

import fr.missilemod.MissileMod;
import fr.missilemod.config.MissileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * File d'attente des explosions. Chaque tick de monde, on consomme un budget de blocs
 * (blocksPerTick) reparti entre les explosions de ce monde, dans l'ordre d'arrivee.
 */
@Mod.EventBusSubscriber(modid = MissileMod.MOD_ID)
public final class ExplosionManager {

    private static final List<SphericalExplosion> JOBS = new ArrayList<>();
    private static final List<SphericalExplosion> PENDING = new ArrayList<>();

    public static void schedule(SphericalExplosion job) {
        job.start();
        PENDING.add(job);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!PENDING.isEmpty()) {
            JOBS.addAll(PENDING);
            PENDING.clear();
        }
        if (JOBS.isEmpty()) {
            return;
        }

        int budget = MissileConfig.BLOCKS_PER_TICK.get();
        Iterator<SphericalExplosion> iterator = JOBS.iterator();
        while (iterator.hasNext() && budget > 0) {
            SphericalExplosion job = iterator.next();
            if (job.level() != level) {
                continue;
            }
            budget -= job.tick(budget);
            if (job.isDone()) {
                job.finish();
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.addAll(PENDING);
        PENDING.clear();
        for (SphericalExplosion job : JOBS) {
            job.finish();
        }
        JOBS.clear();
    }

    private ExplosionManager() {
    }
}
