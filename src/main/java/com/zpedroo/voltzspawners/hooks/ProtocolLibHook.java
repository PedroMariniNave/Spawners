package com.zpedroo.voltzspawners.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import com.zpedroo.voltzspawners.utils.EntityHider;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProtocolLibHook {

    private static ProtocolLibHook instance;
    public static ProtocolLibHook getInstance() { return instance; }

    private ProtocolManager protocolManager;

    private HashMap<Player, List<PlayerSpawner>> spawnersHidden;

    public ProtocolLibHook() {
        instance = this;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.spawnersHidden = new HashMap<>(512);
        this.registerPackets();
    }

    private void registerPackets() {
        getProtocolManager().addPacketListener(new PacketAdapter(VoltzSpawners.get(), ListenerPriority.LOWEST, PacketType.Play.Client.LOOK) {
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                Block block = player.getTargetBlock(null, 15);

                Location location = block.getLocation();

                PlayerSpawner spawner = SpawnerManager.getInstance().getSpawner(location);

                if (spawner == null) {
                    if (!spawnersHidden.containsKey(player)) return;

                    List<PlayerSpawner> spawners = spawnersHidden.remove(player);

                    for (PlayerSpawner toHide : spawners) {
                        toHide.getHologram().hideTo(player);

                        for (Entity entity : toHide.getEntities()) {
                            VoltzSpawners.get().getServer().getScheduler().runTaskLater(VoltzSpawners.get(), () -> EntityHider.getInstance().showEntity(player, entity), 0L);
                        }
                    }
                    return;
                }

                for (Entity entity : spawner.getEntities()) {
                    EntityHider.getInstance().hideEntity(player, entity);
                }

                spawner.getHologram().showTo(player);

                List<PlayerSpawner> holoList = spawnersHidden.containsKey(player) ? spawnersHidden.get(player) : new ArrayList<>();
                holoList.add(spawner);

                spawnersHidden.put(player, holoList);
            }
        });
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
}
