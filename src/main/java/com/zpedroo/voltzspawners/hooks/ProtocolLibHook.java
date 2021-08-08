package com.zpedroo.voltzspawners.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.spawner.SpawnerHologram;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProtocolLibHook {

    private static ProtocolLibHook instance;
    public static ProtocolLibHook getInstance() { return instance; }

    private ProtocolManager protocolManager;

    private HashMap<Player, List<SpawnerHologram>> holograms;

    public ProtocolLibHook() {
        instance = this;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.holograms = new HashMap<>(512);
        this.registerPackets();
    }

    private void registerPackets() {
        getProtocolManager().addPacketListener(new PacketAdapter(VoltzSpawners.get(), ListenerPriority.LOWEST, PacketType.Play.Client.LOOK) {
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                Block block = player.getTargetBlock(null, 15);

                Location location = block.getLocation();

                PlayerSpawner machine = SpawnerManager.getInstance().getSpawner(location);

                if (machine == null) {
                    if (!holograms.containsKey(player)) return;

                    List<SpawnerHologram> holoList = holograms.remove(player);

                    for (SpawnerHologram hologram : holoList) {
                        hologram.hideTo(player);
                    }
                    return;
                }

                SpawnerHologram hologram = machine.getHologram();

                hologram.showTo(player);

                List<SpawnerHologram> holoList = holograms.containsKey(player) ? holograms.get(player) : new ArrayList<>();
                holoList.add(hologram);

                holograms.put(player, holoList);
            }
        });
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
}
