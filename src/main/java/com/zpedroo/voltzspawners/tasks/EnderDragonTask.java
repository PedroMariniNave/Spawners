package com.zpedroo.voltzspawners.tasks;

import com.zpedroo.voltzspawners.VoltzSpawners;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class EnderDragonTask extends BukkitRunnable {

    private final Entity entity;
    private final Location location;

    public EnderDragonTask(VoltzSpawners voltzSpawners, Entity entity) {
        this.entity = entity;
        this.location = entity.getLocation();
        this.runTaskTimerAsynchronously(voltzSpawners, 1L, 1L);
    }

    @Override
    public void run() {
        if (entity == null || entity.isDead()) {
            this.cancel();
            return;
        }

        entity.teleport(location);
    }
}