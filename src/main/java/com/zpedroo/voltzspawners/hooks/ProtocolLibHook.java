package com.zpedroo.voltzspawners.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.objects.PlacedSpawner;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ProtocolLibHook extends PacketAdapter {

    public ProtocolLibHook(Plugin plugin, PacketType packetType) {
        super(plugin, packetType);
    }

    private static final Map<PlacedSpawner, List<UUID>> spawnerViewers = new HashMap<>(16);

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        Block block = player.getTargetBlock((HashSet<Byte>) null, 15);

        Location location = block.getLocation();
        PlacedSpawner placedSpawner = DataManager.getInstance().getPlacedSpawner(location);

        if (placedSpawner == null) {
            removeViewer(player);
            return;
        }

        addViewer(player, placedSpawner);
    }

    public static void removeViewer(Player player) {
        new HashSet<>(spawnerViewers.entrySet()).stream().filter(spawnersEntry -> spawnersEntry.getValue().contains(player.getUniqueId()))
                .forEach(entry -> {
                    entry.getValue().remove(player.getUniqueId());
                    if (entry.getValue().size() <= 0) {
                        PlacedSpawner spawner = entry.getKey();
                        spawner.getHologram().removeHologram();
                        spawner.showEntities();
                        spawnerViewers.remove(spawner);
                    }
                });
    }

    private static void addViewer(Player player, PlacedSpawner placedSpawner) {
        if (spawnerViewers.containsKey(placedSpawner) && spawnerViewers.get(placedSpawner).contains(player.getUniqueId())) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                placedSpawner.getHologram().spawnHologram();
                placedSpawner.hideEntities();
            }
        }.runTaskLater(VoltzSpawners.get(), 0L);

        List<UUID> viewers = spawnerViewers.getOrDefault(placedSpawner, new ArrayList<>(2));
        viewers.add(player.getUniqueId());

        spawnerViewers.put(placedSpawner, viewers);
    }
}
