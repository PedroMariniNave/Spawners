package com.zpedroo.voltzspawners.tasks;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.EntityManager;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.utils.config.Settings;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.xenondevs.particle.ParticleEffect;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;

public class    SpawnerTask extends BukkitRunnable {

    public SpawnerTask(VoltzSpawners voltzSpawners) {
        this.runTaskTimer(voltzSpawners, 20 * 30L, Settings.SPAWNER_UPDATE);
    }

    @Override
    public void run() {
        new HashSet<>(SpawnerManager.getInstance().getDataCache().getPlayerSpawners().values()).forEach(spawner -> {
            if (spawner == null) return;
            if (!spawner.isEnabled()) {
                if (!spawner.hasInfiniteEnergy() && spawner.getEnergy().signum() <= 0) return;
                if (!Settings.ALWAYS_ENABLED_WORLDS.contains(spawner.getLocation().getWorld().getName())) return;

                spawner.switchStatus();
            }

            int delay = spawner.getDelay() - Settings.SPAWNER_UPDATE;
            spawner.setDelay(delay);

            if (delay >= 0) return;

            BigInteger amount = null;

            if (spawner.getEnergy().compareTo(spawner.getStack()) >= 0) {
                amount = spawner.getStack();
            } else {
                amount = spawner.getStack().subtract(spawner.getEnergy());
            }

            if (!spawner.hasInfiniteIntegrity()) {
                if (spawner.getIntegrity() <= 70) { // low efficiency
                    Integer toDivide = (100 - spawner.getIntegrity()) / 10;

                    if (toDivide >= 2) amount = amount.divide(BigInteger.valueOf(toDivide));
                }

                Random random = new Random();

                if (random.nextInt(100 + 1) <= 25) {
                    spawner.setIntegrity(spawner.getIntegrity() - 1);
                }

                if (spawner.getIntegrity() <= 60) { // 60% of integrity = chance of stop machine and lost all drops
                    if (random.nextInt(100 + 1) <= 5) {
                        spawner.switchStatus();
                        spawner.setDrops(BigInteger.ZERO);
                        spawner.getLocation().getWorld().playSound(spawner.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 10f, 10f);

                        ParticleEffect.EXPLOSION_HUGE.display(spawner.getLocation().clone().add(0.5D, 0D, 0.5D));
                    }
                }
            }

            if (amount.signum() <= 0) amount = BigInteger.ONE;
            BigInteger energy = amount.divide(BigInteger.TEN); // 10 stacks = 1 energy
            EntityManager.spawn(spawner, amount);

            if (!spawner.hasInfiniteEnergy()) {
                spawner.removeEnergy(energy.signum() <= 0 ? BigInteger.ONE : energy);

                if (spawner.getEnergy().signum() <= 0) {
                    spawner.switchStatus();
                }
            }

            spawner.updateDelay();
            spawner.getLocation().getWorld().playSound(spawner.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
        });
    }
}