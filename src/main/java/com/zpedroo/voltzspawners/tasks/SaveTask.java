package com.zpedroo.voltzspawners.tasks;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.utils.config.Settings;
import org.bukkit.scheduler.BukkitRunnable;

public class SaveTask extends BukkitRunnable {

    public SaveTask(VoltzSpawners voltzSpawners) {
        this.runTaskTimerAsynchronously(voltzSpawners, 20 * Settings.SAVE_INTERVAL, 20 * Settings.SAVE_INTERVAL);
    }

    @Override
    public void run() {
        SpawnerManager.getInstance().saveAll();
        SpawnerManager.getInstance().updateTopSpawners();
    }
}