package com.zpedroo.voltzspawners.objects;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.managers.BonusManager;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.utils.config.Messages;
import com.zpedroo.voltzspawners.utils.config.Titles;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class PlacedSpawner {

    private Location location;
    private UUID ownerUUID;
    private BigInteger stack;
    private BigInteger energy;
    private BigInteger drops;
    private BigInteger integrity;
    private Spawner spawner;
    private SpawnerHologram hologram;
    private List<Manager> managers;
    private Set<Entity> entities;
    private boolean infiniteEnergy;
    private boolean infiniteIntegrity;
    private boolean publicSpawner;
    private boolean status;
    private boolean update;
    private boolean deleted;
    private int spawnDelay;

    public PlacedSpawner(Location location, UUID ownerUUID, BigInteger stack, BigInteger energy, BigInteger drops, BigInteger integrity, Spawner spawner, List<Manager> managers, boolean infiniteEnergy, boolean infiniteIntegrity, boolean publicSpawner) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.stack = stack;
        this.energy = energy;
        this.drops = drops;
        this.integrity = integrity;
        this.spawner = spawner;
        this.hologram = new SpawnerHologram(this);
        this.managers = managers;
        this.entities = new HashSet<>(1);
        this.infiniteEnergy = infiniteEnergy;
        this.infiniteIntegrity = infiniteIntegrity;
        this.publicSpawner = publicSpawner;
        this.status = ALWAYS_ENABLED_WORLDS.contains(location.getWorld().getName()) ? true : false;
        this.update = false;
        this.deleted = false;
        this.spawnDelay = spawner.getDelay();
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public OfflinePlayer getOwner() {
        return Bukkit.getOfflinePlayer(ownerUUID);
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

    public BigInteger getIntegrity() {
        return integrity;
    }

    public Spawner getSpawner() {
        return spawner;
    }

    public List<Manager> getManagers() {
        return managers;
    }

    public Manager getManager(UUID uuid) {
        for (Manager manager : managers) {
            if (!manager.getUUID().equals(uuid)) continue;

            return manager;
        }

        return null;
    }

    public boolean hasInfiniteEnergy() {
        return infiniteEnergy;
    }

    public boolean hasInfiniteIntegrity() {
        return infiniteIntegrity;
    }

    public boolean isPublic() {
        return publicSpawner;
    }

    public boolean isEnabled() {
        return status;
    }

    public boolean isQueueUpdate() {
        return update;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean hasReachStackLimit() {
        if (spawner.getMaximumStack().signum() <= 0) return false;

        return stack.compareTo(spawner.getMaximumStack()) >= 0;
    }

    public boolean canInteract(Player player) {
        if (player.getUniqueId().equals(getOwnerUUID())) return true;
        if (player.hasPermission("machines.admin")) return true;

        Manager manager = getManager(player.getUniqueId());
        return manager != null;
    }

    public int getSpawnDelay() {
        return spawnDelay;
    }

    public SpawnerHologram getHologram() {
        return hologram;
    }

    public Set<Entity> getEntities() {
        return entities;
    }

    public void delete() {
        this.deleted = true;

        DataManager.getInstance().getCache().getDeletedSpawners().add(location);
        DataManager.getInstance().getCache().getPlacedSpawners().remove(location);
        DataManager.getInstance().getCache().getPlayerSpawnersByUUID(ownerUUID).remove(this);

        this.removeEntities();
        this.location.getBlock().setType(Material.AIR);
        this.hologram.removeHologramAndItem();
    }

    public void setInfiniteEnergy(boolean infiniteEnergy) {
        this.infiniteEnergy = infiniteEnergy;
        this.update = true;
        this.hologram.updateHologramAndItem();
    }

    public void setInfiniteIntegrity(boolean infiniteIntegrity) {
        this.infiniteIntegrity = infiniteIntegrity;
        this.update = true;
        this.hologram.updateHologramAndItem();
    }

    public void setPublic(boolean publicSpawner) {
        this.publicSpawner = publicSpawner;
        this.update = true;
    }

    public String replace(String text) {
        if (text == null || text.isEmpty()) return "";

        return StringUtils.replaceEach(text, new String[] {
                "{owner}",
                "{stack}",
                "{max_stack}",
                "{drops}",
                "{type}",
                "{energy}",
                "{integrity}",
                "{status}"
        }, new String[] {
                Bukkit.getOfflinePlayer(ownerUUID).getName(),
                NumberFormatter.getInstance().format(stack),
                NumberFormatter.getInstance().format(spawner.getMaximumStack()),
                NumberFormatter.getInstance().format(drops),
                spawner.getTypeTranslated(),
                infiniteEnergy ? "∞" : NumberFormatter.getInstance().format(energy),
                infiniteIntegrity ? "∞" : NumberFormatter.getInstance().format(integrity) + "%",
                status ? Messages.ENABLED : Messages.DISABLED
        });
    }

    public void switchStatus() {
        this.setStatus(!status);
    }

    public void setStatus(boolean status) {
        this.status = status;
        this.hologram.updateHologramAndItem();
    }

    public void updateDelay() {
        this.spawnDelay = spawner.getDelay();
    }

    public void setSpawnDelay(int spawnDelay) {
        this.spawnDelay = spawnDelay;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void addEnergy(BigInteger amount) {
        this.setEnergy(energy.add(amount));
    }

    public void removeEnergy(BigInteger amount) {
        this.setEnergy(energy.subtract(amount));
    }

    public void setEnergy(BigInteger amount) {
        this.energy = amount;
        if (energy.signum() <= 0) this.energy = BigInteger.ZERO;

        this.update = true;
        this.hologram.updateHologramAndItem();
    }

    public void addStack(BigInteger amount) {
        this.setStack(stack.add(amount));
    }

    public void removeStack(BigInteger amount) {
        this.setStack(stack.subtract(amount));
    }

    public void setStack(BigInteger amount) {
        this.stack = amount;
        this.update = true;
        if (stack.signum() <= 0) {
            VoltzSpawners.get().getServer().getScheduler().runTaskLater(VoltzSpawners.get(), this::delete, 0L); // fix async block remove
            return;
        }

        this.hologram.updateHologramAndItem();
    }

    public void addDrops(BigInteger amount) {
        this.setDrops(drops.add(amount));
    }

    public void setDrops(BigInteger value) {
        this.drops = value;
        this.update = true;
        this.hologram.updateHologramAndItem();
    }

    public void addIntegrity(BigInteger amount) {
        this.setIntegrity(integrity.add(amount));
    }

    public void setIntegrity(BigInteger amount) {
        if (amount.signum() <= 0) {
            this.delete();
            return;
        }

        this.integrity = amount;
        this.hologram.updateHologramAndItem();
    }

    public void sellDrops(Player player) {
        if (drops.signum() <= 0) return;

        OfflinePlayer spawnerOwner = getOwner();
        BigInteger finalDropsPrice = BonusManager.applyBonus(spawnerOwner.getName(), drops.multiply(spawner.getDropsValue()));

        for (String cmd : spawner.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), StringUtils.replaceEach(cmd, new String[]{
                    "{player}",
                    "{amount}"
            }, new String[]{
                    player.getName(),
                    finalDropsPrice.toString()
            }));
        }

        player.sendTitle(Titles.WHEN_SELL_TITLE, StringUtils.replaceEach(Titles.WHEN_SELL_SUBTITLE, new String[]{
                "{value}"
        }, new String[]{
                NumberFormatter.getInstance().format(finalDropsPrice)
        }));

        this.drops = BigInteger.ZERO;
        this.hologram.updateHologramAndItem();
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntities() {
        for (Entity entity : entities) {
            entity.remove();
        }

        entities.clear();
    }

    public void hideEntities() {
        for (Entity entity : entities) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (livingEntity == null) continue;

            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            livingEntity.setNoDamageTicks(Integer.MAX_VALUE);
        }
    }

    public void showEntities() {
        for (Entity entity : entities) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (livingEntity == null) continue;

            livingEntity.removePotionEffect(PotionEffectType.INVISIBILITY);
            livingEntity.setNoDamageTicks(0);
        }
    }

    public void cache() {
        DataManager.getInstance().getCache().getPlacedSpawners().put(location, this);

        List<PlacedSpawner> spawners = DataManager.getInstance().getCache().getPlayerSpawnersByUUID(ownerUUID);
        spawners.add(this);

        DataManager.getInstance().getCache().setUUIDSpawners(ownerUUID, spawners);
    }
}