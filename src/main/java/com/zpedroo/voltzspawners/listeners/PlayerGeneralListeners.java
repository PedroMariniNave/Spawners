package com.zpedroo.voltzspawners.listeners;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.utils.menu.Menus;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerGeneralListeners implements Listener {

    private static List<Player> choosingGift;

    static {
        choosingGift = new ArrayList<>(32);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType().equals(Material.AIR)) return;

        ItemStack item = event.getItem().clone();
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey("SpawnersGift")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        Menus.getInstance().openGiftMenu(player);
        getChoosingGift().add(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 10f);

        item.setAmount(1);
        player.getInventory().removeItem(item);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClose(InventoryCloseEvent event) {
        if (!getChoosingGift().contains(event.getPlayer())) return;

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        VoltzSpawners.get().getServer().getScheduler().runTaskLater(VoltzSpawners.get(), () -> player.openInventory(inventory), 0L);
    }

    public static List<Player> getChoosingGift() {
        return choosingGift;
    }
}