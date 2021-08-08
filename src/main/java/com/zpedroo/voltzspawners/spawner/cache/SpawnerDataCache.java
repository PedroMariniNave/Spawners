package com.zpedroo.voltzspawners.spawner.cache;

import com.zpedroo.voltzspawners.spawner.Spawner;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import org.bukkit.Location;

import java.math.BigInteger;
import java.util.*;

public class SpawnerDataCache {

    private Map<String, Spawner> spawners;
    private Map<Location, PlayerSpawner> playerSpawners;
    private Map<UUID, List<PlayerSpawner>> playerSpawnersByUUID;
    private Map<UUID, BigInteger> topSpawners;
    private Set<Location> deletedSpawners;

    public SpawnerDataCache() {
        this.spawners = new HashMap<>(32);
        this.playerSpawners = new HashMap<>(5120);
        this.deletedSpawners = new HashSet<>(5120);
        this.playerSpawnersByUUID = new HashMap<>(2560);
        this.topSpawners = new HashMap<>(10);
    }

    public Map<String, Spawner> getSpawners() {
        return spawners;
    }

    public Map<Location, PlayerSpawner> getPlayerSpawners() {
        return playerSpawners;
    }

    public Map<UUID, List<PlayerSpawner>> getPlayerSpawnersByUUID() {
        return playerSpawnersByUUID;
    }

    public List<PlayerSpawner> getPlayerSpawnersByUUID(UUID uuid) {
        if (!playerSpawnersByUUID.containsKey(uuid)) return new ArrayList<>();

        return playerSpawnersByUUID.get(uuid);
    }

    public Map<UUID, BigInteger> getTopSpawners() {
        return topSpawners;
    }

    public Set<Location> getDeletedSpawners() {
        return deletedSpawners;
    }

    public void setPlayerSpawners(HashMap<Location, PlayerSpawner> playerSpawners) {
        this.playerSpawners = playerSpawners;
    }

    public void setUUIDSpawners(UUID uuid, List<PlayerSpawner> spawners) {
        this.playerSpawnersByUUID.put(uuid, spawners);
    }

    public void setTopSpawners(Map<UUID, BigInteger> topSpawners) {
        this.topSpawners = topSpawners;
    }
}