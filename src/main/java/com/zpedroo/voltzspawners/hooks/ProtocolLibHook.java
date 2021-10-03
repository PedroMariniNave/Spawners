package com.zpedroo.voltzspawners.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.objects.PlayerSpawner;
import com.zpedroo.voltzspawners.objects.SpawnerHologram;
import com.zpedroo.voltzspawners.utils.EntityHider;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class ProtocolLibHook extends PacketAdapter {

    public ProtocolLibHook(Plugin plugin, PacketType packetType) {
        super(plugin, packetType);
    }

    private Map<Player, List<PlayerSpawner>> spawnersChanged = new HashMap<>(128);

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        Block block = player.getTargetBlock(null, 15);

        Location location = block.getLocation();
        PlayerSpawner spawner = DataManager.getInstance().getSpawner(location);

        if (spawner == null) {
            if (!spawnersChanged.containsKey(player)) return;

            List<PlayerSpawner> spawners = spawnersChanged.remove(player);

            for (PlayerSpawner playerSpawner : spawners) {
                playerSpawner.getHologram().hideTo(player);

                for (Entity entity : playerSpawner.getEntities()) {
                    try {
                        EntityHider.getInstance().showEntity(player, entity);
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
            return;
        }

        SpawnerHologram hologram = spawner.getHologram();

        hologram.showTo(player);

        for (Entity entity : spawner.getEntities()) {
            try {
                EntityHider.getInstance().hideEntity(player, entity);
            } catch (Exception ex) {
                // ignore
            }
        }

        List<PlayerSpawner> spawnersList = spawnersChanged.containsKey(player) ? spawnersChanged.get(player) : new ArrayList<>(2);
        spawnersList.add(spawner);

        spawnersChanged.put(player, spawnersList);
    }
}
