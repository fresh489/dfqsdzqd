package fr.missilemod.explosion;

import fr.missilemod.MissileMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Execute des actions differees (en ticks) : debris, poussiere, fumee persistante, son retarde...
 * Remplace la technique de "l'entite invisible qui emet des particules" sans rien stocker dans le monde.
 */
@Mod.EventBusSubscriber(modid = MissileMod.MOD_ID)
public final class ImpactScheduler {

    private record Task(ServerLevel level, long runAt, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    public static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        PENDING.add(new Task(level, level.getGameTime() + Math.max(0, delayTicks), action));
    }

    /** Planifie {@code times} executions, la premiere apres {@code delay} ticks puis toutes les {@code period} ticks. */
    public static void repeat(ServerLevel level, int delay, int period, int times, IndexedAction action) {
        for (int i = 0; i < times; i++) {
            final int index = i;
            schedule(level, delay + i * period, () -> action.run(index));
        }
    }

    @FunctionalInterface
    public interface IndexedAction {
        void run(int index);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!PENDING.isEmpty()) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        if (TASKS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Task> due = new ArrayList<>();
        Iterator<Task> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.level() == level && task.runAt() <= now) {
                due.add(task);
                iterator.remove();
            }
        }
        for (Task task : due) {
            try {
                task.action().run();
            } catch (RuntimeException e) {
                MissileMod.LOGGER.error("Erreur dans un effet d'impact", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TASKS.clear();
        PENDING.clear();
    }

    private ImpactScheduler() {
    }
}
