package com.zpedroo.voltzspawners.tasks;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import org.bukkit.scheduler.BukkitRunnable;

import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class QuotationTask extends BukkitRunnable {

    public QuotationTask(VoltzSpawners voltzSpawners) {
        this.runTaskTimerAsynchronously(voltzSpawners, 20 * 60L, 20 * 60L);
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() < NEXT_UPDATE) return;

        SpawnerManager.updatePrices(false);
    }
}