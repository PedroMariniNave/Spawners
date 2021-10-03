package com.zpedroo.voltzspawners.objects;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.utils.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
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

            for (Entity nearEntity : spawner.getLocation().getWorld().getNearbyEntities(spawner.getLocation().clone().add(0.5D, 0D, 0.5D), 1D, 1D, 1D)) {
                if (nearEntity.hasMetadata("Spawner Item")) nearEntity.remove();
            }

            displayItem = spawner.getLocation().getWorld().dropItem(spawner.getLocation().clone().add(0.5D, 1D, 0.5D), spawner.getSpawner().getDisplayItem());
            displayItem.setVelocity(new Vector(0, 0.1, 0));
            displayItem.setPickupDelay(Integer.MAX_VALUE);
            displayItem.setMetadata("Spawner Item", new FixedMetadataValue(VoltzSpawners.get(), true));
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