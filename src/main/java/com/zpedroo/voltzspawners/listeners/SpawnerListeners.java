package com.zpedroo.voltzspawners.listeners;

import com.zpedroo.voltzspawners.hooks.WorldGuardHook;
import com.zpedroo.voltzspawners.spawner.Spawner;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;

public class SpawnerListeners implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final ItemStack item = event.getItemInHand().clone();
        if (item.getType().equals(Material.AIR)) return;

        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey("SpawnersEnergy") || nbt.hasKey("SpawnersInfiniteEnergy") || nbt.hasKey("SpawnersInfiniteRepair") || nbt.hasKey("SpawnersRepair")) event.setCancelled(true);
        if (!nbt.hasKey("SpawnersAmount")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (!WorldGuardHook.getInstance().canBuild(player, block.getLocation())) return;

        Spawner spawner = getManager().getDataCache().getSpawners().get(nbt.getString("SpawnersType").toUpperCase());
        if (spawner == null) return;

        if (!StringUtils.equals(spawner.getPermission(), "NULL")) {
            if (!player.hasPermission(spawner.getPermission())) {
                player.sendMessage(Messages.SPAWNER_PERMISSION);
                return;
            }
        }

        BigInteger stack = new BigInteger(nbt.getString("SpawnersAmount"));
        Object[] objects = getManager().getNearMachines(player, block, stack, spawner.getType());
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

                            PlayerSpawner nearSpawner = getManager().getSpawner(blocks.getLocation());
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

        item.setAmount(1);
        if (player.getInventory().getItemInOffHand().isSimilar(item)) {
            player.getInventory().setItemInOffHand(null);
        } else {
            player.getInventory().removeItem(item);
        }
        if (overLimit.compareTo(BigInteger.ZERO) >= 1) player.getInventory().addItem(spawner.getItem(overLimit, integrity));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        PlayerSpawner spawner = getManager().getDataCache().getPlayerSpawners().get(block.getLocation());
        if (spawner == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!spawner.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        ItemStack item = player.getItemInHand().clone();

        if (item.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(item);
            if (nbt.hasKey("SpawnersInfiniteEnergy")) {
                if (spawner.hasInfiniteEnergy()) return;

                spawner.setInfiniteEnergy(true);
                item.setAmount(1);
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 10f, 10f);
                player.getInventory().removeItem(item);
                return;
            }

            if (nbt.hasKey("SpawnersInfiniteRepair")) {
                if (spawner.hasInfiniteIntegrity()) return;

                spawner.setInfiniteIntegrity(true);
                item.setAmount(1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
                player.getInventory().removeItem(item);
                return;
            }

            if (nbt.hasKey("SpawnersEnergy")) {
                if (spawner.hasInfiniteEnergy()) return;
                
                BigInteger amount = new BigInteger(nbt.getString("SpawnersEnergy"));

                spawner.addEnergy(amount);
                item.setAmount(1);
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 10f, 10f);
                player.getInventory().removeItem(item);
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
                item.setAmount(1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
                player.getInventory().removeItem(item);
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

        PlayerSpawner spawner = getManager().getSpawner(block.getLocation());
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
        if (StringUtils.equals(event.getEntity().getName(), "Spawner Item")) {
            event.setCancelled(true);
        }
    }

    private SpawnerManager getManager() {
        return SpawnerManager.getInstance();
    }
}