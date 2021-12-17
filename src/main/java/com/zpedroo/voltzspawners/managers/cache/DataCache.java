package com.zpedroo.voltzspawners.managers.cache;

import com.zpedroo.voltzspawners.objects.Bonus;
import com.zpedroo.voltzspawners.objects.PlacedSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import org.bukkit.Location;

import java.math.BigInteger;
import java.util.*;

public class DataCache {

    private Map<String, Spawner> spawners;
    private Map<Location, PlacedSpawner> placedSpawners;
    private Map<UUID, List<PlacedSpawner>> placedSpawnersByUUID;
    private Map<UUID, BigInteger> topSpawners;
    private Set<Location> deletedSpawners;
    private List<Bonus> bonuses;

    public DataCache() {
        this.spawners = new HashMap<>(24);
        this.placedSpawnersByUUID = new HashMap<>(32);
        this.deletedSpawners = new HashSet<>(32);
        this.bonuses = new ArrayList<>(4);
    }

    public Map<String, Spawner> getSpawners() {
        return spawners;
    }

    public Map<Location, PlacedSpawner> getPlacedSpawners() {
        return placedSpawners;
    }

    public Map<UUID, List<PlacedSpawner>> getPlacedSpawnersByUUID() {
        return placedSpawnersByUUID;
    }

    public List<PlacedSpawner> getPlayerSpawnersByUUID(UUID uuid) {
        return placedSpawnersByUUID.getOrDefault(uuid, new LinkedList<>());
    }

    public Map<UUID, BigInteger> getTopSpawners() {
        return topSpawners;
    }

    public Set<Location> getDeletedSpawners() {
        return deletedSpawners;
    }

    public List<Bonus> getBonuses() {
        return bonuses;
    }

    public void setPlacedSpawners(Map<Location, PlacedSpawner> playerSpawners) {
        this.placedSpawners = playerSpawners;
    }

    public void setUUIDSpawners(UUID uuid, List<PlacedSpawner> spawners) {
        this.placedSpawnersByUUID.put(uuid, spawners);
    }

    public void setTopSpawners(Map<UUID, BigInteger> topSpawners) {
        this.topSpawners = topSpawners;
    }
}