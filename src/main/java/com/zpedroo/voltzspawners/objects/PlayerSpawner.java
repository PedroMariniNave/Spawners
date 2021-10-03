package com.zpedroo.voltzspawners.objects;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.utils.config.Messages;
import com.zpedroo.voltzspawners.utils.config.Titles;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerSpawner {

    private Location location;
    private UUID ownerUUID;
    private BigInteger stack;
    private BigInteger energy;
    private BigInteger drops;
    private Integer integrity;
    private Spawner spawner;
    private List<Manager> managers;
    private Boolean infiniteEnergy;
    private Boolean infiniteIntegrity;
    private Boolean publicSpawner;
    private Boolean status;
    private Boolean update;
    private Integer delay;
    private SpawnerHologram hologram;
    private Set<Entity> entities;

    public PlayerSpawner(Location location, UUID ownerUUID, BigInteger stack, BigInteger energy, BigInteger drops, Integer integrity, Spawner spawner, List<Manager> managers, Boolean infiniteEnergy, Boolean infiniteIntegrity, Boolean publicSpawner) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.stack = stack;
        this.energy = energy;
        this.drops = drops;
        this.integrity = integrity;
        this.spawner = spawner;
        this.managers = managers;
        this.infiniteEnergy = infiniteEnergy;
        this.infiniteIntegrity = infiniteIntegrity;
        this.publicSpawner = publicSpawner;
        this.status = false;
        this.update = false;
        this.delay = spawner.getDelay();
        this.hologram = new SpawnerHologram(this);
        this.entities = new HashSet<>(16);
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public BigInteger getStack() {
        return stack;
    }

    public BigInteger getEnergy() {
        return energy;
    }

    public BigInteger getDrops() {
        return drops;
    }

    public Integer getIntegrity() {
        return integrity;
    }

    public Spawner getSpawner() {
        return spawner;
    }

    public List<Manager> getManagers() {
        return managers;
    }

    public Boolean hasInfiniteEnergy() {
        return infiniteEnergy;
    }

    public Boolean hasInfiniteIntegrity() {
        return infiniteIntegrity;
    }

    public Boolean isPublic() {
        return publicSpawner;
    }

    public Manager getManager(UUID uuid) {
        for (Manager manager : managers) {
            if (!manager.getUUID().equals(uuid)) continue;

            return manager;
        }

         return null;
    }

    public Boolean isEnabled() {
        return status;
    }

    public Boolean isQueueUpdate() {
        return update;
    }

    public Boolean hasReachStackLimit() {
        if (spawner.getMaxStack().signum() <= 0) return false;

        return stack.compareTo(spawner.getMaxStack()) >= 0;
    }

    public Boolean canInteract(Player player) {
        if (player.getUniqueId().equals(getOwnerUUID())) return true;
        if (player.hasPermission("machines.admin")) return true;

        Manager manager = getManager(player.getUniqueId());
        return manager != null;
    }

    public Integer getDelay() {
        return delay;
    }

    public SpawnerHologram getHologram() {
        return hologram;
    }

    public Set<Entity> getEntities() {
        return entities;
    }

    public void delete() {
        DataManager.getInstance().getCache().getDeletedSpawners().add(location);
        DataManager.getInstance().getCache().getPlayerSpawners().remove(location);
        DataManager.getInstance().getCache().getPlayerSpawnersByUUID(ownerUUID).remove(this);

        this.removeEntities();
        this.hologram.remove();
        this.location.getBlock().setType(Material.AIR);
    }

    public void setInfiniteEnergy(Boolean infiniteEnergy) {
        this.infiniteEnergy = infiniteEnergy;
        this.update = true;
        this.hologram.update(this);
    }

    public void setInfiniteIntegrity(Boolean infiniteIntegrity) {
        this.infiniteIntegrity = infiniteIntegrity;
        this.update = true;
        this.hologram.update(this);
    }

    public void setPublic(Boolean publicSpawner) {
        this.publicSpawner = publicSpawner;
        this.update = true;
    }

    public String replace(String text) {
        if (text == null || text.isEmpty()) return "";

        return StringUtils.replaceEach(text, new String[] {
                "{owner}",
                "{type}",
                "{stack}",
                "{max_stack}",
                "{energy}",
                "{drops}",
                "{integrity}",
                "{status}"
        }, new String[] {
                Bukkit.getOfflinePlayer(ownerUUID).getName(),
                spawner.getTypeTranslated(),
                NumberFormatter.getInstance().format(stack),
                NumberFormatter.getInstance().format(spawner.getMaxStack()),
                infiniteEnergy ? "∞" : NumberFormatter.getInstance().format(energy),
                NumberFormatter.getInstance().format(drops),
                infiniteIntegrity ? "∞" : integrity.toString() + "%",
                status ? Messages.ENABLED : Messages.DISABLED
        });
    }

    public void switchStatus() {
        this.status = !status;
        this.hologram.update(this);
    }

    public void setStatus(Boolean status) {
        this.status = status;
        this.hologram.update(this);
    }

    public void updateDelay() {
        this.delay = spawner.getDelay();
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public void setQueueUpdate(Boolean status) {
        this.update = status;
    }

    public void addEnergy(BigInteger value) {
        this.energy = energy.add(value);
        this.update = true;
        this.hologram.update(this);
    }

    public void removeEnergy(BigInteger value) {
        this.energy = energy.subtract(value);
        if (energy.signum() <= 0) this.energy = BigInteger.ZERO;

        this.update = true;
        this.hologram.update(this);
    }

    public void addStack(BigInteger value) {
        this.stack = stack.add(value);
        this.update = true;
        this.hologram.update(this);
    }

    public void removeStack(BigInteger value) {
        this.stack = stack.subtract(value);
        this.update = true;
        if (stack.signum() <= 0) {
            VoltzSpawners.get().getServer().getScheduler().runTaskLater(VoltzSpawners.get(), this::delete, 0L); // fix async block remove
            return;
        }

        this.hologram.update(this);
    }

    public void setDrops(BigInteger value) {
        this.drops = value;
        this.update = true;
        this.hologram.update(this);
    }

    public void addDrops(BigInteger value) {
        this.drops = drops.add(value);
        this.update = true;
        this.hologram.update(this);
    }

    public void setIntegrity(Integer value) {
        if (value <= 0) {
            this.delete();
            return;
        }

        this.integrity = value;
        this.hologram.update(this);
    }

    public void addIntegrity(Integer value) {
        this.integrity += value;
        this.update = true;
        this.hologram.update(this);
    }

    public void sellDrops(Player player) {
        if (drops.signum() <= 0) return;

        for (String cmd : spawner.getCommands()) {
            if (cmd == null) break;

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), StringUtils.replaceEach(cmd, new String[]{
                    "{player}",
                    "{amount}"
            }, new String[]{
                    player.getName(),
                    drops.toString()
            }));
        }

        player.sendTitle(Titles.WHEN_SELL_TITLE, StringUtils.replaceEach(Titles.WHEN_SELL_SUBTITLE, new String[]{
                "{value}"
        }, new String[]{
                NumberFormatter.getInstance().format(drops)
        }));

        this.drops = BigInteger.ZERO;
        this.hologram.update(this);
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntities() {
        for (Entity entity : entities) {
            if (entity == null) continue;

            entity.remove();
        }

        entities.clear();
    }

    public void cache() {
        DataManager.getInstance().getCache().getPlayerSpawners().put(location, this);

        List<PlayerSpawner> spawners = DataManager.getInstance().getCache().getPlayerSpawnersByUUID(ownerUUID);
        spawners.add(this);

        DataManager.getInstance().getCache().setUUIDSpawners(ownerUUID, spawners);
    }
}