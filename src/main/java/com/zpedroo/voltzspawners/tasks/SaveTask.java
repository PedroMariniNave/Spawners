package com.zpedroo.voltzspawners.tasks;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.DataManager;
import org.bukkit.scheduler.BukkitRunnable;

import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class SaveTask extends BukkitRunnable {

    public SaveTask(VoltzSpawners voltzSpawners) {
        this.runTaskTimerAsynchronously(voltzSpawners, 20 * SAVE_INTERVAL, 20 * SAVE_INTERVAL);
    }

    @Override
    public void run() {
        DataManager.getInstance().saveAll();
        DataManager.getInstance().updateTopSpawners();
    }
}