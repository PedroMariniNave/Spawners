package com.zpedroo.voltzspawners.listeners;

import com.zpedroo.voltzspawners.hooks.WorldGuardHook;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.objects.PlayerSpawner;
import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.utils.config.Messages;
import com.zpedroo.voltzspawners.utils.config.Settings;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import com.zpedroo.voltzspawners.utils.item.Items;
import com.zpedroo.voltzspawners.utils.menu.Menus;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class SpawnerListeners implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final ItemStack item = event.getItemInHand();
        if (item.getType().equals(Material.AIR)) return;

        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey("SpawnersEnergy") || nbt.hasKey("SpawnersInfiniteEnergy") || nbt.hasKey("SpawnersInfiniteRepair") || nbt.hasKey("SpawnersRepair")) event.setCancelled(true);
        if (!nbt.hasKey("SpawnersAmount")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (!WorldGuardHook.getInstance().canBuild(player, block.getLocation())) return;

        Spawner spawner = DataManager.getInstance().getSpawner(nbt.getString("SpawnersType").toUpperCase());
        if (spawner == null) return;

        if (!StringUtils.equals(spawner.getPermission(), "NULL")) {
            if (!player.hasPermission(spawner.getPermission())) {
                player.sendMessage(Messages.SPAWNER_PERMISSION);
                return;
            }
        }

        BigInteger stack = new BigInteger(nbt.getString("SpawnersAmount"));
        Object[] objects = SpawnerManager.getInstance().getNearSpawners(player, block, stack, spawner.getType());
        PlayerSpawner playerSpawner = objects != null ? (PlayerSpawner) objects[0] : null;
        BigInteger overLimit = null;
        Integer integrity = nbt.getInteger("SpawnersIntegrity");

        if (playerSpawner != null) {
            if (!playerSpawner.canInteract(player)) return;

            overLimit = (BigInteger) objects[1];
            playerSpawner.addStack(stack.subtract(overLimit));
        } else {
            int radius = Settings.STACK_RADIUS;

            if (radius > 0) {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (!WorldGuardHook.getInstance().canBuild(player, new Location(block.getWorld(), x, y, z))) continue;

                            Block blocks = block.getRelative(x, y, z);
                            if (blocks.getType().equals(Material.AIR)) continue;

                            PlayerSpawner nearSpawner = DataManager.getInstance().getSpawner(blocks.getLocation());
                            if (nearSpawner == null) continue;

                            player.sendMessage(Messages.NEAR_SPAWNER);
                            return;
                        }
                    }
                }
            }

            if (spawner.getMaxStack().signum() > 0 && stack.compareTo(spawner.getMaxStack()) > 0) {
                overLimit = stack.subtract(spawner.getMaxStack());
            } else overLimit = BigInteger.ZERO;

            playerSpawner = new PlayerSpawner(block.getLocation(), player.getUniqueId(), stack.subtract(overLimit), BigInteger.ZERO, BigInteger.ZERO, integrity, spawner, new ArrayList<>(), false, false, false);
            playerSpawner.cache();
            playerSpawner.setQueueUpdate(true);
        }

        item.setAmount(item.getAmount() - 1);

        if (overLimit.compareTo(BigInteger.ZERO) >= 1) player.getInventory().addItem(spawner.getItem(overLimit, integrity));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().equals(Material.AIR)) return;

        PlayerSpawner spawner = null;
        if (event.getClickedBlock() != null) spawner = DataManager.getInstance().getSpawner(event.getClickedBlock().getLocation());
        if (spawner != null) return;

        NBTItem nbt = new NBTItem(item);

        List<PlayerSpawner> spawners = DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId());

        if (nbt.hasKey("SpawnersEnergy")) {

            event.setCancelled(true);

            if (spawners.isEmpty()) {
                player.sendMessage(Messages.ZERO_SPAWNERS_ENERGY);
                return;
            }

            Integer spawnersWithInfiniteEnergy = 0;
            for (PlayerSpawner playerSpawner : spawners) {
                if (playerSpawner.hasInfiniteEnergy()) ++spawnersWithInfiniteEnergy;
            }

            Integer spawnersAmount = spawners.size();
            BigInteger amount = new BigInteger(nbt.getString("SpawnersEnergy"));
            BigInteger toAdd = amount.divide(BigInteger.valueOf(spawnersAmount - spawnersWithInfiniteEnergy));
            while (toAdd.compareTo(BigInteger.ONE) < 0) {
                toAdd = amount.divide(BigInteger.valueOf(--spawnersAmount));
            }

            for (int i = 0; i < spawnersAmount; ++i) {
                PlayerSpawner playerSpawner = spawners.get(i);
                if (playerSpawner.hasInfiniteEnergy()) continue;

                playerSpawner.addEnergy(toAdd);
            }

            item.setAmount(item.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
            player.sendMessage(StringUtils.replaceEach(Messages.SUCCESSFUL_ENERGIZED, new String[]{
                    "{spawners}",
                    "{energy}"
            }, new String[]{
                    NumberFormatter.getInstance().formatDecimal(spawnersAmount.doubleValue() - spawnersWithInfiniteEnergy.doubleValue()),
                    NumberFormatter.getInstance().format(toAdd)
            }));
            return;
        }

        if (nbt.hasKey("SpawnersRepair")) {

            event.setCancelled(true);

            if (spawners.isEmpty()) {
                player.sendMessage(Messages.ZERO_SPAWNERS_REPAIR);
                return;
            }


            Integer spawnersAmount = spawners.size();
            Integer percentage = nbt.getInteger("SpawnersRepair");
            Integer toAdd = percentage / spawnersAmount;
            Integer amountRepaired = 0;
            while (toAdd < 1) {
                toAdd = percentage / --spawnersAmount;
            }

            Integer overLimit = 0;

            for (int i = 0; i < spawnersAmount; ++i) {
                PlayerSpawner playerSpawner = spawners.get(i);
                Integer excess = 0;
                if (playerSpawner.getIntegrity() >= 100 || playerSpawner.hasInfiniteIntegrity() || playerSpawner.getIntegrity() + toAdd > 100) {
                    excess = toAdd - (100 - playerSpawner.getIntegrity());
                    overLimit += excess;
                }

                if (toAdd - excess <= 0) continue;

                Integer toRepair = toAdd - excess;
                amountRepaired += toRepair;

                playerSpawner.addIntegrity(toRepair);
            }

            if (overLimit / toAdd == spawnersAmount) return;

            item.setAmount(item.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
            player.sendMessage(StringUtils.replaceEach(Messages.SUCCESSFUL_REPAIRED, new String[]{
                    "{spawners}",
                    "{repair}"
            }, new String[]{
                    NumberFormatter.getInstance().formatDecimal(spawnersAmount.doubleValue()),
                    NumberFormatter.getInstance().format(BigInteger.valueOf(amountRepaired / spawnersAmount))
            }));
            if (overLimit > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        Block block = event.getClickedBlock();
        if (block == null || block.getType().equals(Material.AIR)) return;

        PlayerSpawner spawner = DataManager.getInstance().getSpawner(block.getLocation());
        if (spawner == null) return;

        event.setCancelled(true);

        if (!spawner.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        if (item != null && item.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(item);

            if (nbt.hasKey("SpawnersInfiniteEnergy")) {
                if (spawner.hasInfiniteEnergy()) return;

                spawner.setInfiniteEnergy(true);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("SpawnersInfiniteRepair")) {
                if (spawner.hasInfiniteIntegrity()) return;

                spawner.setInfiniteIntegrity(true);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
                return;
            }

            if (nbt.hasKey("SpawnersEnergy")) {
                if (spawner.hasInfiniteEnergy()) return;

                BigInteger amount = new BigInteger(nbt.getString("SpawnersEnergy"));

                spawner.addEnergy(amount);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("SpawnersRepair")) {
                if (spawner.getIntegrity() >= 100 || spawner.hasInfiniteIntegrity()) return;

                Integer percentage = nbt.getInteger("SpawnersRepair");
                Integer overLimit = 0;

                if (spawner.getIntegrity() + percentage > 100) {
                    overLimit = percentage - (100 - spawner.getIntegrity());
                }

                spawner.addIntegrity(percentage - overLimit);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
                if (overLimit > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
                return;
            }
        }

        Menus.getInstance().openSpawnerMenu(player, spawner);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2f);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        PlayerSpawner spawner = DataManager.getInstance().getSpawner(block.getLocation());
        if (spawner == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!spawner.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        ItemStack item = event.getPlayer().getItemInHand().clone();
        BigInteger toGive = spawner.getStack();

        Boolean correctPickaxe = false;

        if (item.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(item);

            if (nbt.hasKey("SpawnersPickaxe")) correctPickaxe = true;
        }

        if (!correctPickaxe) {
            toGive = spawner.getStack().subtract(spawner.getStack().multiply(BigInteger.valueOf(Settings.TAX_REMOVE_STACK)).divide(BigInteger.valueOf(100)));
        }

        if (toGive.compareTo(spawner.getStack()) < 0) {
            player.sendMessage(StringUtils.replaceEach(Messages.INCORRECT_PICKAXE, new String[]{
                    "{lost}"
            }, new String[]{
                    NumberFormatter.getInstance().format(spawner.getStack().subtract(toGive))
            }));
        }

        if (spawner.hasInfiniteEnergy()) {
            player.getInventory().addItem(Items.getInstance().getInfiniteEnergy());
        }

        if (spawner.hasInfiniteIntegrity()) {
            player.getInventory().addItem(Items.getInstance().getInfiniteRepair());
        }

        if (spawner.getDrops().signum() > 0) spawner.sellDrops(player);

        spawner.delete();
        player.getInventory().addItem(spawner.getSpawner().getItem(toGive, spawner.getIntegrity()));

        if (spawner.getEnergy().signum() <= 0) return;

        player.getInventory().addItem(Items.getInstance().getEnergy(spawner.getEnergy()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDespawn(ItemDespawnEvent event) {
        if (!event.getEntity().hasMetadata("Spawner Item")) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Player || entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof Item) continue;

            entity.remove();
        }
    }
}