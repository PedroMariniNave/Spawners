package com.zpedroo.voltzspawners.spawner;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.utils.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpawnerHologram {

    private String[] hologramLines;
    private TextLine[] textLines;
    private Item displayItem;

    private Hologram hologram;

    public SpawnerHologram(PlayerSpawner spawner) {
        this.hologramLines = Settings.SPAWNER_HOLOGRAM;
        Bukkit.getScheduler().runTaskLater(VoltzSpawners.get(), () -> update(spawner), 0L);
    }

    public void update(PlayerSpawner spawner) {
        spawner.getLocation().getBlock().setType(spawner.getSpawner().getBlock());

        if (hologram != null && hologram.isDeleted()) return;

        if (hologram == null) {
            hologram = HologramsAPI.createHologram(VoltzSpawners.get(), spawner.getLocation().clone().add(0.5D, 3.95, 0.5D));
            textLines = new TextLine[hologramLines.length];

            for (int i = 0; i < hologramLines.length; i++) {
                textLines[i] = hologram.insertTextLine(i, spawner.replace(hologramLines[i]));
            }

            hologram.getVisibilityManager().setVisibleByDefault(false);

            displayItem = spawner.getLocation().getWorld().dropItem(spawner.getLocation().clone().add(0.5D, 1D, 0.5D), spawner.getSpawner().getDisplayItem());
            displayItem.setVelocity(new Vector(0, 0.1, 0));
            displayItem.setPickupDelay(Integer.MAX_VALUE);
            displayItem.setCustomName("Spawner Item");
            displayItem.setCustomNameVisible(false);
        } else {
            for (int i = 0; i < hologramLines.length; i++) {
                this.textLines[i].setText(spawner.replace(hologramLines[i]));
            }
        }
    }

    public void showTo(Player player) {
        if (hologram == null) return;

        this.hologram.getVisibilityManager().showTo(player);
    }

    public void hideTo(Player player) {
        if (hologram == null) return;

        this.hologram.getVisibilityManager().hideTo(player);
    }

    public void remove() {
        if (hologram == null) return;

        this.hologram.delete();
        this.displayItem.remove();
        this.hologram = null;
    }
}