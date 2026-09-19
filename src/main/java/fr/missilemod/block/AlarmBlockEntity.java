package fr.missilemod.block;

import fr.missilemod.config.MissileConfig;
import fr.missilemod.entity.IncomingMunition;
import fr.missilemod.registry.ModBlockEntities;
import fr.missilemod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Detection des munitions en vol autour du bloc. Le scan n'a lieu qu'une fois par seconde (pas a chaque tick) ;
 * entre deux scans, seuls les sons sont joues. L'alerte s'eteint 3 s apres la derniere detection.
 */
public class AlarmBlockEntity extends BlockEntity {

    private static final int SCAN_PERIOD = 20;
    private static final int OFF_DELAY = 60;

    private int tier;
    private long lastDetection = Long.MIN_VALUE / 2;
    private int soundCooldown;

    public AlarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_ALARM.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlarmBlockEntity alarm) {
        long now = level.getGameTime();
        if ((now + pos.asLong()) % SCAN_PERIOD == 0) {
            alarm.scan(level, pos, now);
        }
        if (alarm.tier > 0 && --alarm.soundCooldown <= 0) {
            alarm.playTierSound(level, pos);
        }
        int alert = state.getValue(MissileAlarmBlock.ALERT);
        if (alert != alarm.tier) {
            level.setBlock(pos, state.setValue(MissileAlarmBlock.ALERT, alarm.tier), 3);
        }
    }

    private void scan(Level level, BlockPos pos, long now) {
        double radius = MissileConfig.ALARM_RADIUS.get();
        Vec3 center = Vec3.atCenterOf(pos);
        double closest = Double.MAX_VALUE;
        for (Entity entity : level.getEntities((Entity) null, new AABB(pos).inflate(radius),
                e -> e instanceof IncomingMunition munition && munition.isIncomingMunition())) {
            closest = Math.min(closest, entity.position().distanceTo(center));
        }
        int newTier = closest <= 30.0D ? 3 : closest <= 80.0D ? 2 : closest <= radius ? 1 : 0;
        if (newTier > 0) {
            this.lastDetection = now;
            if (newTier != this.tier) {
                this.soundCooldown = 0; // reagit tout de suite au changement de palier
            }
            this.tier = newTier;
        } else if (now - this.lastDetection > OFF_DELAY) {
            this.tier = 0;
        }
    }

    private void playTierSound(Level level, BlockPos pos) {
        switch (this.tier) {
            case 1 -> {
                level.playSound(null, pos, ModSounds.ALARM_BEEP.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
                this.soundCooldown = 40;  // bip lent et discret
            }
            case 2 -> {
                level.playSound(null, pos, ModSounds.ALARM_FAST.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                this.soundCooldown = 10;  // alarme rapide
            }
            default -> {
                level.playSound(null, pos, ModSounds.SIREN.get(), SoundSource.BLOCKS, 6.0F, 1.0F);
                this.soundCooldown = 31;  // sirene forte et continue (le son dure 1,6 s)
            }
        }
    }
}
