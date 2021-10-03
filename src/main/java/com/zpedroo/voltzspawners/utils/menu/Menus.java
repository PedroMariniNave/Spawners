package com.zpedroo.voltzspawners.utils.menu;

import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.listeners.PlayerChatListener;
import com.zpedroo.voltzspawners.listeners.PlayerGeneralListeners;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.objects.Manager;
import com.zpedroo.voltzspawners.objects.PlayerChat;
import com.zpedroo.voltzspawners.objects.PlayerSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.utils.FileUtils;
import com.zpedroo.voltzspawners.utils.builder.InventoryBuilder;
import com.zpedroo.voltzspawners.utils.builder.InventoryUtils;
import com.zpedroo.voltzspawners.utils.builder.ItemBuilder;
import com.zpedroo.voltzspawners.utils.enums.Action;
import com.zpedroo.voltzspawners.utils.enums.Permission;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import com.zpedroo.voltzspawners.utils.formatter.TimeFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.zpedroo.voltzspawners.utils.config.Messages.*;
import static com.zpedroo.voltzspawners.utils.config.Settings.*;

public class Menus {

    private static Menus instance;
    public static Menus getInstance() { return instance; }

    private InventoryUtils inventoryUtils;

    private ItemStack nextPageItem;
    private ItemStack previousPageItem;

    public Menus() {
        instance = this;
        this.inventoryUtils = new InventoryUtils();
        this.nextPageItem = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Next-Page").build();
        this.previousPageItem = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Previous-Page").build();
    }

    public void openMainMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.MAIN;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);

        for (String items : FileUtils.get().getSection(file, "Inventory.items")) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + items).build();
            int slot = FileUtils.get().getInt(file, "Inventory.items." + items + ".slot");
            String action = FileUtils.get().getString(file, "Inventory.items." + items + ".action");

            switch (action.toUpperCase()) {
                case "SPAWNERS" -> inventoryUtils.addAction(inventory, item, () -> {
                    openPlayerSpawnersMenu(player, DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId()));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "SHOP" -> inventoryUtils.addAction(inventory, item, () -> {
                    openShopMenu(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "TOP" -> inventoryUtils.addAction(inventory, item, () -> {
                    openTopSpawnersMenu(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);
            }

            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    public void openSpawnerMenu(Player player, PlayerSpawner spawner) {
        File folder = new File(VoltzSpawners.get().getDataFolder() + "/spawners/" + spawner.getSpawner().getType() + ".yml");
        FileConfiguration file = YamlConfiguration.loadConfiguration(folder);

        Manager manager = spawner.getManager(player.getUniqueId());

        String title = ChatColor.translateAlternateColorCodes('&', file.getString("Spawner-Menu.title"));
        int size = file.getInt("Spawner-Menu.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);

        for (String items : file.getConfigurationSection("Spawner-Menu.items").getKeys(false)) {
            ItemStack item = ItemBuilder.build(file, "Spawner-Menu.items." + items, new String[]{
                    "{owner}",
                    "{type}",
                    "{stack}",
                    "{energy}",
                    "{drops}",
                    "{price}",
                    "{single_price}",
                    "{drops_previous}",
                    "{statistics}",
                    "{status}",
                    "{privacy_status}"
            }, new String[]{
                    Bukkit.getOfflinePlayer(spawner.getOwnerUUID()).getName(),
                    spawner.getSpawner().getTypeTranslated(),
                    NumberFormatter.getInstance().format(spawner.getStack()),
                    spawner.hasInfiniteEnergy() ? "∞" : NumberFormatter.getInstance().format(spawner.getEnergy()),
                    NumberFormatter.getInstance().format(spawner.getDrops()),
                    NumberFormatter.getInstance().format(spawner.getSpawner().getDropsValue().multiply(spawner.getDrops())),
                    NumberFormatter.getInstance().format(spawner.getSpawner().getDropsValue()),
                    NumberFormatter.getInstance().format(spawner.getSpawner().getDropsPreviousValue()),
                    getStatistics(spawner.getSpawner().getDropsValue(), spawner.getSpawner().getDropsPreviousValue()),
                    spawner.isEnabled() ? ENABLED : DISABLED,
                    spawner.isPublic() ? PUBLIC : PRIVATE
            }).build();

            int slot = file.getInt("Spawner-Menu.items." + items + ".slot");
            String action = file.getString("Spawner-Menu.items." + items + ".action", "NULL");

            switch (action.toUpperCase()) {
                case "SWITCH_STATUS" -> inventoryUtils.addAction(inventory, item, () -> {
                    if (!spawner.hasInfiniteEnergy() && spawner.getEnergy().signum() <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    if (spawner.getIntegrity() <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    spawner.switchStatus();
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 100f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "SELL_DROPS" -> inventoryUtils.addAction(inventory, item, () -> {
                    if (!player.getUniqueId().equals(spawner.getOwnerUUID()) && (manager != null && !manager.can(Permission.SELL_DROPS))) {
                        player.sendMessage(NEED_PERMISSION);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    if (spawner.getDrops().signum() <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    spawner.sellDrops(player);
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.5f, 0.5f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "MANAGERS" -> inventoryUtils.addAction(inventory, item, () -> {
                    openManagersMenu(player, spawner);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "REMOVE_STACK" -> inventoryUtils.addAction(inventory, item, () -> {
                    if (!player.getUniqueId().equals(spawner.getOwnerUUID()) && (manager != null && !manager.can(Permission.REMOVE_STACK))) {
                        player.sendMessage(NEED_PERMISSION);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, spawner, Action.REMOVE_STACK));
                    player.closeInventory();
                    clearChat(player);

                    for (String msg : REMOVE_STACK) {
                        player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                                "{tax}"
                        }, new String[]{
                                String.valueOf(TAX_REMOVE_STACK)
                        }));
                    }
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "SWITCH_PRIVACY" -> inventoryUtils.addAction(inventory, item, () -> {
                    if (!player.getUniqueId().equals(spawner.getOwnerUUID())) {
                        player.sendMessage(ONLY_OWNER);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    spawner.setPublic(!spawner.isPublic());
                    spawner.sellDrops(player);
                    openSpawnerMenu(player, spawner);
                }, InventoryUtils.ActionType.ALL_CLICKS);
            }

            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    public void openPlayerSpawnersMenu(Player player, List<PlayerSpawner> spawners) {
        FileUtils.Files file = FileUtils.Files.PLAYER_SPAWNERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(spawners.size());

        if (spawners.size() <= 0) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.setItem(slot, item);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (PlayerSpawner spawner : spawners) {
                if (++i >= slots.length) i = 0;

                List<String> lore = new ArrayList<>(8);
                List<InventoryUtils.Action> actions = new ArrayList<>(1);

                ItemStack item = spawner.getSpawner().getDisplayItem();
                ItemMeta meta = item.getItemMeta();

                for (String toAdd : FileUtils.get().getStringList(file, "Item-Lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', StringUtils.replaceEach(toAdd, new String[]{
                            "{stack}",
                            "{drops}",
                            "{energy}",
                            "{integrity}",
                            "{status}"
                    }, new String[]{
                            NumberFormatter.getInstance().format(spawner.getStack()),
                            NumberFormatter.getInstance().format(spawner.getDrops()),
                            spawner.hasInfiniteEnergy() ? "∞" : NumberFormatter.getInstance().format(spawner.getEnergy()),
                            spawner.hasInfiniteIntegrity() ? "∞" : spawner.getIntegrity().toString() + "%",
                            spawner.isEnabled() ? ENABLED : DISABLED
                    })));
                }

                meta.setDisplayName(spawner.getSpawner().getDisplayName());
                meta.setLore(lore);
                item.setItemMeta(meta);

                int slot = Integer.parseInt(slots[i]);

                actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                    openSpawnerMenu(player, spawner);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }));

                builders.add(ItemBuilder.build(item, slot, actions));
            }
        }

        ItemStack sellAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Sell-All", new String[]{
                "{drops}",
                "{price}"
        }, new String[]{
                NumberFormatter.getInstance().format(getTotalDrops(player)),
                NumberFormatter.getInstance().format(getTotalDropsPrice(player))
        }).build();
        int sellAllSlot = FileUtils.get().getInt(file, "Sell-All.slot");

        inventory.setItem(sellAllSlot, sellAll);
        inventoryUtils.addAction(inventory, sellAll, () -> {
            BigInteger dropsPrice = getTotalDropsPrice(player);

            if (dropsPrice.signum() <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                return;
            }

            for (PlayerSpawner spawner : DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())) {
                if (spawner == null) continue;

                spawner.sellDrops(player);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.5f, 100f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        ItemStack enableAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Enable-All").build();
        int enableAllSlot = FileUtils.get().getInt(file, "Enable-All.slot");

        inventory.setItem(enableAllSlot, enableAll);
        inventoryUtils.addAction(inventory, enableAll, () -> {
            for (PlayerSpawner spawner : DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())) {
                if (spawner == null) continue;
                if (!spawner.hasInfiniteEnergy() && spawner.getEnergy().signum() <= 0) continue;

                spawner.setStatus(true);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 100f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        ItemStack disableAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Disable-All").build();
        int disableAllSlot = FileUtils.get().getInt(file, "Disable-All.slot");

        inventory.setItem(disableAllSlot, disableAll);
        inventoryUtils.addAction(inventory, disableAll, () -> {
            for (PlayerSpawner spawner : DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())) {
                if (spawner == null) continue;

                spawner.setStatus(false);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 0.5f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    public void openTopSpawnersMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.TOP_SPAWNERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);

        String[] topSlots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");

        Map<UUID, BigInteger> topSpawners = DataManager.getInstance().getCache().getTopSpawners();
        int pos = 0;

        for (Map.Entry<UUID, BigInteger> entry : topSpawners.entrySet()) {
            UUID uuid = entry.getKey();
            BigInteger stack = entry.getValue();

            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Item", new String[]{
                    "{player}",
                    "{spawners}",
                    "{pos}"
            }, new String[]{
                    Bukkit.getOfflinePlayer(uuid).getName(),
                    NumberFormatter.getInstance().format(stack),
                    String.valueOf(++pos)
            }).build();

            int slot = Integer.parseInt(topSlots[pos-1]);

            inventoryUtils.addAction(inventory, item, null, InventoryUtils.ActionType.ALL_CLICKS);

            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    public void openManagersMenu(Player player, PlayerSpawner spawner) {
        FileUtils.Files file = FileUtils.Files.MANAGERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(spawner.getManagers().size());

        if (spawner.getManagers().size() <= 0) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.setItem(slot, item);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (Manager manager : spawner.getManagers()) {
                if (++i >= slots.length) i = 0;

                List<InventoryUtils.Action> actions = new ArrayList<>(1);

                ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Item", new String[]{
                        "{player}",
                        "{add_stack}",
                        "{remove_stack}",
                        "{add_friends}",
                        "{remove_friends}",
                        "{sell_drops}"
                }, new String[]{
                        Bukkit.getOfflinePlayer(manager.getUUID()).getName(),
                        manager.can(Permission.ADD_STACK) ? TRUE : FALSE,
                        manager.can(Permission.REMOVE_STACK) ? TRUE : FALSE,
                        manager.can(Permission.ADD_FRIENDS) ? TRUE : FALSE,
                        manager.can(Permission.REMOVE_FRIENDS) ? TRUE : FALSE,
                        manager.can(Permission.SELL_DROPS) ? TRUE : FALSE
                }).build();
                int slot = Integer.parseInt(slots[i]);

                if (spawner.getOwnerUUID().equals(player.getUniqueId()) || manager.can(Permission.REMOVE_FRIENDS)) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? (ArrayList<String>) meta.getLore() : new ArrayList<>();

                    for (String toAdd : FileUtils.get().getStringList(file, "Extra-Lore")) {
                        if (toAdd == null) break;

                        lore.add(ChatColor.translateAlternateColorCodes('&', toAdd));
                    }

                    meta.setLore(lore);
                    item.setItemMeta(meta);

                    actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                        openPermissionsMenu(player, spawner, manager);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));
                }

                builders.add(ItemBuilder.build(item, slot, actions));
            }
        }

        Manager manager = spawner.getManager(player.getUniqueId());

        ItemStack addFriend = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Add-Friend").build();
        int addFriendSlot = FileUtils.get().getInt(file, "Add-Friend.slot");

        inventory.setItem(addFriendSlot, addFriend);
        inventoryUtils.addAction(inventory, addFriend, () -> {
            if (spawner.getOwnerUUID().equals(player.getUniqueId()) || (manager != null && manager.can(Permission.ADD_FRIENDS))) {
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, spawner, Action.ADD_FRIEND));
                player.closeInventory();
                clearChat(player);

                for (String msg : ADD_FRIEND) {
                    player.sendMessage(msg);
                }
                return;
            }

            player.sendMessage(NEED_PERMISSION);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        ItemStack back = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Back").build();
        int backSlot = FileUtils.get().getInt(file, "Back.slot");

        inventory.setItem(backSlot, back);
        inventoryUtils.addAction(inventory, back, () -> {
            openSpawnerMenu(player, spawner);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    public void openPermissionsMenu(Player player, PlayerSpawner spawner, Manager manager) {
        FileUtils.Files file = FileUtils.Files.PERMISSIONS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(5);

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            Permission permission = null;

            try {
                permission = Permission.valueOf(str.toUpperCase());
            } catch (Exception ex) {
                // ignore
            }

            ItemStack item = null;
            int slot = FileUtils.get().getInt(file, "Inventory.items." + str + ".slot");
            List<InventoryUtils.Action> actions = new ArrayList<>(1);

            if (permission == null) {
                item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                        "{friend}"
                }, new String[]{
                        Bukkit.getOfflinePlayer(manager.getUUID()).getName()
                }).build();

                String action = FileUtils.get().getString(file, "Inventory.items." + str + ".action");

                switch (action.toUpperCase()) {
                    case "BACK" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                        openManagersMenu(player, spawner);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));

                    case "REMOVE" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                        spawner.getManagers().remove(manager);
                        spawner.setQueueUpdate(true);
                        openManagersMenu(player, spawner);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));
                }
            } else {
                item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str + "." + (manager.can(permission) ? "true" : "false")).build();

                final Permission finalPermission = permission;

                actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                    if (!player.getUniqueId().equals(spawner.getOwnerUUID())) {
                        player.sendMessage(ONLY_OWNER);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    manager.set(finalPermission, !manager.can(finalPermission));
                    spawner.setQueueUpdate(true);
                    openPermissionsMenu(player, spawner, manager);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }));
            }

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        InventoryBuilder.build(player, inventory, title, builders);
    }

    public void openShopMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.SHOP;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(54);
        String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
        int i = -1;

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            if (++i >= slots.length) i = 0;

            Spawner spawner = DataManager.getInstance().getSpawner(str);
            if (spawner == null) continue;

            List<InventoryUtils.Action> actions = new ArrayList<>(1);
            BigInteger price = new BigInteger(FileUtils.get().getString(file, "Inventory.items." + str + ".price", "0"));
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                    "{price}",
                    "{drops_now}",
                    "{drops_previous}",
                    "{statistics}",
                    "{type}",
                    "{update}"
            }, new String[]{
                    NumberFormatter.getInstance().format(price),
                    NumberFormatter.getInstance().format(spawner.getDropsValue()),
                    NumberFormatter.getInstance().format(spawner.getDropsPreviousValue()),
                    getStatistics(spawner.getDropsValue(), spawner.getDropsPreviousValue()),
                    spawner.getTypeTranslated(),
                    TimeFormatter.getInstance().format(NEXT_UPDATE - System.currentTimeMillis())
            }).build();
            int slot = Integer.parseInt(slots[i]);

            actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, spawner, price, Action.BUY_SPAWNER));
                player.closeInventory();
                clearChat(player);

                for (String msg : CHOOSE_AMOUNT) {
                    player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                            "{spawner}",
                            "{price}",
                            "{drops_now}",
                            "{drops_previous}",
                            "{statistics}",
                            "{type}"
                    }, new String[]{
                            spawner.getDisplayName(),
                            NumberFormatter.getInstance().format(price),
                            NumberFormatter.getInstance().format(spawner.getDropsValue()),
                            NumberFormatter.getInstance().format(spawner.getDropsPreviousValue()),
                            getStatistics(spawner.getDropsValue(), spawner.getDropsPreviousValue()),
                            spawner.getTypeTranslated()
                    }));
                }
            }));

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    public void openGiftMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.GIFT;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(54);
        String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
        int i = -1;
        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            if (++i >= slots.length) i = 0;

            Spawner spawner = DataManager.getInstance().getSpawner(str);
            if (spawner == null) continue;

            List<InventoryUtils.Action> actions = new ArrayList<>(1);
            BigInteger spawnersAmount = new BigInteger(FileUtils.get().getString(file, "Inventory.items." + str + ".spawners-amount", "1"));
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                    "{drops_now}",
                    "{drops_previous}",
                    "{statistics}",
                    "{type}",
                    "{update}"
            }, new String[]{
                    NumberFormatter.getInstance().format(spawner.getDropsValue()),
                    NumberFormatter.getInstance().format(spawner.getDropsPreviousValue()),
                    getStatistics(spawner.getDropsValue(), spawner.getDropsPreviousValue()),
                    spawner.getTypeTranslated(),
                    TimeFormatter.getInstance().format(NEXT_UPDATE - System.currentTimeMillis())
            }).build();
            int slot = Integer.parseInt(slots[i]);

            actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                PlayerGeneralListeners.getChoosingGift().remove(player);
                player.getInventory().addItem(spawner.getItem(spawnersAmount, 100));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 10f);
                player.closeInventory();
            }));

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    private BigInteger getTotalDrops(Player player) {
        BigInteger dropsAmount = BigInteger.ZERO;

        for (PlayerSpawner spawner : DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())) {
            if (spawner == null) continue;
            if (spawner.getDrops().signum() <= 0) continue;

            dropsAmount = dropsAmount.add(spawner.getDrops());
        }

        return dropsAmount;
    }

    private BigInteger getTotalDropsPrice(Player player) {
        BigInteger dropsPrice = BigInteger.ZERO;

        for (PlayerSpawner spawner : DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())) {
            if (spawner == null) continue;
            if (spawner.getDrops().signum() <= 0) continue;

            dropsPrice = dropsPrice.add(spawner.getDrops().multiply(spawner.getSpawner().getDropsValue()));
        }

        return dropsPrice;
    }

    private String getStatistics(BigInteger value1, BigInteger value2) {
        StringBuilder ret = new StringBuilder();

        if (value1.compareTo(value2) > 0) {
            ret.append("§a⬆");
        } else {
            ret.append("§c⬇");
        }

        double increase = value1.doubleValue() - value2.doubleValue();
        double divide = increase / value2.doubleValue();

        ret.append(NumberFormatter.getInstance().formatDecimal(divide * 100)).append("%");
        return ret.toString().replace("-", "");
    }

    private void clearChat(Player player) {
        for (int i = 0; i < 25; ++i) {
            player.sendMessage("");
        }
    }
}