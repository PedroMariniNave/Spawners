package com.zpedroo.voltzspawners.tasks;

import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.managers.EntityManager;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.xenondevs.particle.ParticleEffect;

import java.math.BigInteger;
import java.util.Random;

import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class SpawnerTask extends BukkitRunnable {

    public SpawnerTask(Plugin plugin) {
        this.runTaskTimer(plugin, SPAWNER_UPDATE, SPAWNER_UPDATE);
    }

    @Override
    public void run() {
        DataManager.getInstance().getCache().getPlacedSpawners().values().stream().filter(placedSpawner ->
            placedSpawner != null && placedSpawner.isEnabled() &&
        placedSpawner.getLocation().getWorld().getChunkAt(placedSpawner.getLocation().getBlock()).isLoaded()).forEach(spawner -> {
            int delay = spawner.getSpawnDelay() - SPAWNER_UPDATE;
            spawner.setSpawnDelay(delay);

            if (delay >= 0) return;

            BigInteger amount = null;

            if (spawner.hasInfiniteEnergy() || spawner.getEnergy().compareTo(spawner.getStack()) >= 0) {
                amount = spawner.getStack();
            } else {
                amount = spawner.getSpawner().getDropsAmount().multiply(spawner.getEnergy()).multiply(BigInteger.TEN);
            }

            if (!spawner.hasInfiniteIntegrity()) {
                if (spawner.getIntegrity().intValue() <= 70) { // low efficiency
                    int toDivide = (100 - spawner.getIntegrity().intValue()) / 10;

                    if (toDivide >= 2) amount = amount.divide(BigInteger.valueOf(toDivide));
                }

                Random random = new Random();

                if (random.nextInt(100 + 1) <= 55) {
                    spawner.setIntegrity(spawner.getIntegrity().subtract(BigInteger.ONE));
                }

                if (spawner.getIntegrity().intValue() <= 60) { // 60% of integrity = chance of stop machine and lost all drops
                    if (random.nextInt(100 + 1) <= 5) {
                        spawner.switchStatus();
                        spawner.setDrops(BigInteger.ZERO);
                        spawner.getLocation().getWorld().playSound(spawner.getLocation(), Sound.EXPLODE, 10f, 10f);

                        ParticleEffect.EXPLOSION_HUGE.display(spawner.getLocation().clone().add(0.5D, 0D, 0.5D));
                    }
                }
            }

            if (amount.signum() <= 0) amount = BigInteger.ONE;

            final BigInteger finalAmount = amount;
            EntityManager.spawn(spawner, finalAmount);
            // VoltzSpawners.get().getServer().getScheduler().runTaskLater(VoltzSpawners.get(), () -> EntityManager.spawn(spawner, finalAmount), 0L);

            if (!spawner.hasInfiniteEnergy()) {
                BigInteger energy = spawner.getStack().divide(BigInteger.TEN); // 10 stacks = 1L
                spawner.removeEnergy(energy.signum() <= 0 ? BigInteger.ONE : energy);

                if (spawner.getEnergy().signum() <= 0) {
                    spawner.switchStatus();
                }
            }

            spawner.updateDelay();
            spawner.getLocation().getWorld().playSound(spawner.getLocation(), Sound.ENDERMAN_TELEPORT, 0.5f, 0.5f);
        });
    }
}