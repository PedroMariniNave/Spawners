package com.zpedroo.voltzspawners.utils.menu;

import com.zpedroo.multieconomy.api.CurrencyAPI;
import com.zpedroo.multieconomy.objects.Currency;
import com.zpedroo.voltzspawners.VoltzSpawners;
import com.zpedroo.voltzspawners.listeners.PlayerChatListener;
import com.zpedroo.voltzspawners.listeners.PlayerGeneralListeners;
import com.zpedroo.voltzspawners.managers.BonusManager;
import com.zpedroo.voltzspawners.managers.DataManager;
import com.zpedroo.voltzspawners.objects.Manager;
import com.zpedroo.voltzspawners.objects.PlayerChat;
import com.zpedroo.voltzspawners.objects.PlacedSpawner;
import com.zpedroo.voltzspawners.objects.Spawner;
import com.zpedroo.voltzspawners.utils.FileUtils;
import com.zpedroo.voltzspawners.utils.builder.InventoryBuilder;
import com.zpedroo.voltzspawners.utils.builder.InventoryUtils;
import com.zpedroo.voltzspawners.utils.builder.ItemBuilder;
import com.zpedroo.voltzspawners.enums.Permission;
import com.zpedroo.voltzspawners.enums.PlayerAction;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import com.zpedroo.voltzspawners.utils.formatter.TimeFormatter;
import com.zpedroo.voltzspawners.utils.item.Items;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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

public class Menus extends InventoryUtils {

    private static Menus instance;
    public static Menus getInstance() { return instance; }

    private final ItemStack nextPageItem;
    private final ItemStack previousPageItem;

    public Menus() {
        instance = this;
        this.nextPageItem = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Next-Page").build();
        this.previousPageItem = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Previous-Page").build();
    }

    public void openMainMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.MAIN;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        InventoryBuilder inventory = new InventoryBuilder(title, size);

        for (String items : FileUtils.get().getSection(file, "Inventory.items")) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + items).build();
            int slot = FileUtils.get().getInt(file, "Inventory.items." + items + ".slot");
            String action = FileUtils.get().getString(file, "Inventory.items." + items + ".action");

            inventory.addItem(item, slot, () -> {
                switch (action.toUpperCase()) {
                    case "SPAWNERS":
                        openPlayerSpawnersMenu(player);
                        player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                        break;
                    case "SHOP":
                        openShopMenu(player);
                        player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                        break;
                    case "TOP":
                        openTopSpawnersMenu(player);
                        player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                        break;
                }
            }, ActionType.ALL_CLICKS);
        }

        inventory.open(player);
    }

    public void openSpawnerMenu(Player player, PlacedSpawner spawner) {
        File folder = new File(VoltzSpawners.get().getDataFolder() + "/spawners/" + spawner.getSpawner().getType() + ".yml");
        FileConfiguration file = YamlConfiguration.loadConfiguration(folder);

        Manager manager = spawner.getManager(player.getUniqueId());

        String title = ChatColor.translateAlternateColorCodes('&', file.getString("Spawner-Menu.title"));
        int size = file.getInt("Spawner-Menu.size");

        InventoryBuilder inventory = new InventoryBuilder(title, size);

        final BigInteger finalDropsPrice = spawner.getSpawner().getDropsValue().multiply(spawner.getDrops());
        BigInteger dropsPriceBonus = BonusManager.getBonusByValue(spawner.getOwner().getName(), finalDropsPrice);

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
                    NumberFormatter.getInstance().format(finalDropsPrice) + (dropsPriceBonus.signum() <= 0 ? "" : StringUtils.replaceEach(BONUS, new String[]{
                            "{amount}"
                    }, new String[]{
                            NumberFormatter.getInstance().format(dropsPriceBonus)
                    })),
                    NumberFormatter.getInstance().format(spawner.getSpawner().getDropsValue()),
                    NumberFormatter.getInstance().format(spawner.getSpawner().getDropsPreviousValue()),
                    getStatistics(spawner.getSpawner().getDropsValue(), spawner.getSpawner().getDropsPreviousValue()),
                    spawner.isEnabled() ? ENABLED : DISABLED,
                    spawner.isPublic() ? PUBLIC : PRIVATE
            }).build();

            int slot = file.getInt("Spawner-Menu.items." + items + ".slot");
            String action = file.getString("Spawner-Menu.items." + items + ".action", "NULL");

            inventory.addItem(item, slot, () -> {
                switch (action.toUpperCase()) {
                    case "SWITCH":
                        if (!spawner.hasInfiniteEnergy() && spawner.getEnergy().signum() <= 0) {
                            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        if (spawner.getIntegrity().signum() <= 0) {
                            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        spawner.switchStatus();
                        player.closeInventory();
                        player.playSound(player.getLocation(), Sound.PORTAL_TRIGGER, 0.5f, 100f);
                        break;
                    case "SWITCH_PRIVACY":
                        spawner.setPublic(!spawner.isPublic());
                        openSpawnerMenu(player, spawner);
                        player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                        break;
                    case "SELL_DROPS":
                        if (!player.getUniqueId().equals(spawner.getOwnerUUID()) && (manager != null && !manager.hasPermission(Permission.SELL_DROPS))) {
                            player.sendMessage(NEED_PERMISSION);
                            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        if (spawner.getDrops().signum() <= 0) {
                            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        spawner.sellDrops(player);
                        player.closeInventory();
                        player.playSound(player.getLocation(), Sound.HORSE_ARMOR, 0.5f, 0.5f);
                        break;
                    case "MANAGERS":
                        openManagersMenu(player, spawner);
                        player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                        break;
                    case "REMOVE_STACK":
                        if (!player.getUniqueId().equals(spawner.getOwnerUUID()) && (manager != null && !manager.hasPermission(Permission.REMOVE_STACK))) {
                            player.sendMessage(NEED_PERMISSION);
                            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, spawner, PlayerAction.REMOVE_STACK));
                        player.closeInventory();
                        clearChat(player);

                        for (String msg : REMOVE_STACK) {
                            player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                                    "{tax}"
                            }, new String[]{
                                    String.valueOf(TAX_REMOVE_STACK)
                            }));
                        }
                        break;
                }
            }, ActionType.ALL_CLICKS);
        }

        inventory.open(player);
    }

    public void openTopSpawnersMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.TOP_SPAWNERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        InventoryBuilder inventory = new InventoryBuilder(title, size);

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

            inventory.addItem(item, slot, () -> {
                openOtherSpawnersMenu(player, uuid);
            }, ActionType.ALL_CLICKS);
        }

        inventory.open(player);
    }

    public void openPlayerSpawnersMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.PLAYER_SPAWNERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder inventory = new InventoryBuilder(title, size, previousPageItem, previousPageSlot, nextPageItem, nextPageSlot);

        List<PlacedSpawner> spawners = DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId());

        if (spawners.isEmpty()) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.addItem(item, slot);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (PlacedSpawner spawner : spawners) {
                if (++i >= slots.length) i = 0;

                List<String> lore = new ArrayList<>(8);

                ItemStack item = spawner.getSpawner().getDisplayItem();
                int slot = Integer.parseInt(slots[i]);
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

                inventory.addItem(item, slot, () -> {
                    openSpawnerMenu(player, spawner);
                    player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                }, ActionType.ALL_CLICKS);
            }
        }

        BigInteger dropsAmount = getTotalDrops(player);
        BigInteger dropsPrice = getTotalDropsPrice(player);
        BigInteger dropsPriceBonus = BonusManager.getBonusByValue(player.getName(), dropsPrice);

        ItemStack sellAllItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Sell-All", new String[]{
                "{drops}",
                "{price}"
        }, new String[]{
                NumberFormatter.getInstance().format(dropsAmount),
                NumberFormatter.getInstance().format(dropsPrice) + (dropsPriceBonus.signum() <= 0 ? "" : StringUtils.replaceEach(BONUS, new String[]{
                        "{amount}"
                }, new String[]{
                        NumberFormatter.getInstance().format(dropsPriceBonus)
                })),
        }).build();
        int sellAllSlot = FileUtils.get().getInt(file, "Sell-All.slot");

        inventory.addDefaultItem(sellAllItem, sellAllSlot, () -> {
            if (dropsPrice.signum() <= 0) {
                player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
                return;
            }

            for (PlacedSpawner spawner : spawners) {
                if (spawner == null) continue;
                if (spawner.getDrops().signum() <= 0) continue;

                spawner.sellDrops(player);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.HORSE_ARMOR, 0.5f, 100f);
        }, ActionType.ALL_CLICKS);

        ItemStack enableAllItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Enable-All").build();
        int enableAllSlot = FileUtils.get().getInt(file, "Enable-All.slot");

        inventory.addDefaultItem(enableAllItem, enableAllSlot, () -> {
            for (PlacedSpawner spawner : spawners) {
                if (spawner == null) continue;
                if (!spawner.hasInfiniteEnergy() && spawner.getEnergy().signum() <= 0) continue;

                spawner.setStatus(true);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.PORTAL_TRIGGER, 0.5f, 100f);
        }, ActionType.ALL_CLICKS);

        ItemStack disableAllItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Disable-All").build();
        int disableAllSlot = FileUtils.get().getInt(file, "Disable-All.slot");

        inventory.addDefaultItem(disableAllItem, disableAllSlot, () -> {
            for (PlacedSpawner spawner : spawners) {
                if (spawner == null) continue;

                spawner.setStatus(false);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.PORTAL_TRIGGER, 0.5f, 0.5f);
        }, ActionType.ALL_CLICKS);

        inventory.open(player);
    }

    public void openOtherSpawnersMenu(Player player, UUID target) {
        FileUtils.Files file = FileUtils.Files.OTHER_SPAWNERS;

        String title = ChatColor.translateAlternateColorCodes('&', StringUtils.replaceEach(FileUtils.get().getString(file, "Inventory.title"), new String[]{
                "{target}"
        }, new String[]{
                Bukkit.getOfflinePlayer(target).getName()
        }));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder inventory = new InventoryBuilder(title, size, previousPageItem, previousPageSlot, nextPageItem, nextPageSlot);

        List<PlacedSpawner> spawners = DataManager.getInstance().getCache().getPlayerSpawnersByUUID(target);

        if (spawners.isEmpty()) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.addItem(item, slot);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (PlacedSpawner spawner : spawners) {
                if (++i >= slots.length) i = 0;

                List<String> lore = new ArrayList<>(8);

                ItemStack item = spawner.getSpawner().getDisplayItem();
                int slot = Integer.parseInt(slots[i]);
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

                inventory.addItem(item, slot);
            }
        }

        inventory.open(player);
    }

    public void openManagersMenu(Player player, PlacedSpawner spawner) {
        FileUtils.Files file = FileUtils.Files.MANAGERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder inventory = new InventoryBuilder(title, size, previousPageItem, previousPageSlot, nextPageItem, nextPageSlot);

        if (spawner.getManagers().isEmpty()) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.addItem(item, slot);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (Manager manager : spawner.getManagers()) {
                if (++i >= slots.length) i = 0;

                ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Item", new String[]{
                        "{player}",
                        "{add_stack}",
                        "{remove_stack}",
                        "{add_friends}",
                        "{remove_friends}",
                        "{sell_drops}"
                }, new String[]{
                        Bukkit.getOfflinePlayer(manager.getUUID()).getName(),
                        manager.hasPermission(Permission.ADD_STACK) ? TRUE : FALSE,
                        manager.hasPermission(Permission.REMOVE_STACK) ? TRUE : FALSE,
                        manager.hasPermission(Permission.ADD_FRIENDS) ? TRUE : FALSE,
                        manager.hasPermission(Permission.REMOVE_FRIENDS) ? TRUE : FALSE,
                        manager.hasPermission(Permission.SELL_DROPS) ? TRUE : FALSE
                }).build();
                int slot = Integer.parseInt(slots[i]);

                inventory.addItem(item, slot);

                if (spawner.getOwnerUUID().equals(player.getUniqueId()) || manager.hasPermission(Permission.REMOVE_FRIENDS)) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? (ArrayList<String>) meta.getLore() : new ArrayList<>();

                    for (String toAdd : FileUtils.get().getStringList(file, "Extra-Lore")) {
                        if (toAdd == null) break;

                        lore.add(ChatColor.translateAlternateColorCodes('&', toAdd));
                    }

                    meta.setLore(lore);
                    item.setItemMeta(meta);

                    inventory.addAction(slot, () -> {
                        openPermissionsMenu(player, spawner, manager);
                        player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                    }, ActionType.ALL_CLICKS);
                }
            }
        }

        Manager manager = spawner.getManager(player.getUniqueId());

        ItemStack addFriendItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Add-Friend").build();
        int addFriendSlot = FileUtils.get().getInt(file, "Add-Friend.slot");

        inventory.addDefaultItem(addFriendItem, addFriendSlot, () -> {
            if (spawner.getOwnerUUID().equals(player.getUniqueId()) || (manager != null && manager.hasPermission(Permission.ADD_FRIENDS))) {
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, spawner, PlayerAction.ADD_FRIEND));
                player.closeInventory();
                clearChat(player);

                for (String msg : ADD_FRIEND) {
                    player.sendMessage(msg);
                }
                return;
            }

            player.sendMessage(NEED_PERMISSION);
            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
        }, ActionType.ALL_CLICKS);

        ItemStack backItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Back").build();
        int backSlot = FileUtils.get().getInt(file, "Back.slot");

        inventory.addDefaultItem(backItem, backSlot, () -> {
            openSpawnerMenu(player, spawner);
            player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
        }, ActionType.ALL_CLICKS);

        inventory.open(player);
    }

    public void openPermissionsMenu(Player player, PlacedSpawner spawner, Manager manager) {
        FileUtils.Files file = FileUtils.Files.PERMISSIONS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        InventoryBuilder inventory = new InventoryBuilder(title, size);

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            Permission permission = null;

            try {
                permission = Permission.valueOf(str.toUpperCase());
            } catch (Exception ex) {
                // ignore
            }

            int slot = FileUtils.get().getInt(file, "Inventory.items." + str + ".slot");

            if (permission == null) {
                ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                        "{friend}"
                }, new String[]{
                        Bukkit.getOfflinePlayer(manager.getUUID()).getName()
                }).build();

                String action = FileUtils.get().getString(file, "Inventory.items." + str + ".action");

                inventory.addItem(item, slot, () -> {
                    switch (action.toUpperCase()) {
                        case "BACK":
                            openManagersMenu(player, spawner);
                            player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                            break;
                        case "REMOVE":
                            spawner.getManagers().remove(manager);
                            spawner.setUpdate(true);
                            openManagersMenu(player, spawner);
                            player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                            break;
                    }
                }, ActionType.ALL_CLICKS);
            } else {
                ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str + "." + (manager.hasPermission(permission) ? "true" : "false")).build();

                final Permission finalPermission = permission;

                inventory.addItem(item, slot, () -> {
                    if (!player.getUniqueId().equals(spawner.getOwnerUUID())) {
                        player.sendMessage(ONLY_OWNER);
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    manager.setPermission(finalPermission, !manager.hasPermission(finalPermission));
                    spawner.setUpdate(true);
                    openPermissionsMenu(player, spawner, manager);
                    player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
                }, ActionType.ALL_CLICKS);
            }
        }

        inventory.open(player);
    }

    public void openShopMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.SHOP;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder inventory = new InventoryBuilder(title, size, previousPageItem, previousPageSlot, nextPageItem, nextPageSlot);
        String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
        int i = -1;

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            if (++i >= slots.length) i = 0;

            Spawner spawner = DataManager.getInstance().getSpawner(str);
            if (spawner == null) continue;

            Currency currency = CurrencyAPI.getCurrency(FileUtils.get().getString(file, "Inventory.items." + str + ".currency"));
            if (currency == null) currency = CurrencyAPI.getVaultCurrency();

            BigInteger price = NumberFormatter.getInstance().filter(FileUtils.get().getString(file, "Inventory.items." + str + ".price", "0"))
                    .multiply(spawner.getDropsValue());
            String toGet = spawner.getPermission() != null && !player.hasPermission(spawner.getPermission()) ? "locked" : "unlocked";
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(),
                    FileUtils.get().getFile(file).get().contains("Inventory.items." + str + ".locked")
                            && FileUtils.get().getFile(file).get().contains("Inventory.items." + str + ".unlocked")
                            ? "Inventory.items." + str + "." + toGet
                            : "Inventory.items." + str, new String[]{
                            "{price}",
                            "{drops_now}",
                            "{drops_previous}",
                            "{statistics}",
                            "{type}",
                            "{update}"
                    }, new String[]{
                            StringUtils.replaceEach(currency.getAmountDisplay(), new String[]{
                                    "{amount}"
                            }, new String[]{
                                    NumberFormatter.getInstance().format(price)
                            }),
                            NumberFormatter.getInstance().format(spawner.getDropsValue()),
                            NumberFormatter.getInstance().format(spawner.getDropsPreviousValue()),
                            getStatistics(spawner.getDropsValue(), spawner.getDropsPreviousValue()),
                            spawner.getTypeTranslated(),
                            TimeFormatter.getInstance().format(NEXT_UPDATE - System.currentTimeMillis())
                    }).build();
            int slot = Integer.parseInt(slots[i]);

            final Currency finalCurrency = currency;
            inventory.addItem(item, slot, () -> {
                if (price.signum() <= 0) return;
                if (spawner.getPermission() != null && !player.hasPermission(spawner.getPermission())) return;

                player.closeInventory();
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, spawner, finalCurrency, price, PlayerAction.BUY_SPAWNER));
                clearChat(player);

                for (String msg : CHOOSE_AMOUNT) {
                    player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                            "{item}",
                            "{price}",
                            "{drops_now}",
                            "{drops_previous}",
                            "{statistics}",
                            "{type}"
                    }, new String[]{
                            spawner.getDisplayName(),
                            StringUtils.replaceEach(finalCurrency.getAmountDisplay(), new String[]{
                                    "{amount}"
                            }, new String[]{
                                    NumberFormatter.getInstance().format(price)
                            }),
                            NumberFormatter.getInstance().format(spawner.getDropsValue()),
                            NumberFormatter.getInstance().format(spawner.getDropsPreviousValue()),
                            getStatistics(spawner.getDropsValue(), spawner.getDropsPreviousValue()),
                            spawner.getTypeTranslated()
                    }));
                }
            }, ActionType.ALL_CLICKS);
        }

        ItemStack buyEnergyItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Buy-Energy").build();
        int buyEnergySlot = FileUtils.get().getInt(file, "Buy-Energy.slot");
        BigInteger buyEnergyPrice = NumberFormatter.getInstance().filter(FileUtils.get().getString(file, "Buy-Energy.default-price"));

        inventory.addDefaultItem(buyEnergyItem, buyEnergySlot, () -> {
            player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
            openChooseCurrencyMenu(player, PlayerAction.BUY_ENERGY, buyEnergyPrice);
        }, ActionType.ALL_CLICKS);

        ItemStack buyPickaxeItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Buy-Pickaxe").build();
        int buyPickaxeSlot = FileUtils.get().getInt(file, "Buy-Pickaxe.slot");
        BigInteger buyPickaxePrice = NumberFormatter.getInstance().filter(FileUtils.get().getString(file, "Buy-Pickaxe.default-price"));

        inventory.addDefaultItem(buyPickaxeItem, buyPickaxeSlot, () -> {
            player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
            openChooseCurrencyMenu(player, PlayerAction.BUY_PICKAXE, buyPickaxePrice);
        }, ActionType.ALL_CLICKS);

        ItemStack buyRepairItem = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Buy-Repair").build();
        int buyRepairSlot = FileUtils.get().getInt(file, "Buy-Repair.slot");
        BigInteger buyRepairPrice = NumberFormatter.getInstance().filter(FileUtils.get().getString(file, "Buy-Repair.default-price"));

        inventory.addDefaultItem(buyRepairItem, buyRepairSlot, () -> {
            player.playSound(player.getLocation(), Sound.CLICK, 2f, 2f);
            openChooseCurrencyMenu(player, PlayerAction.BUY_REPAIR, buyRepairPrice);
        }, ActionType.ALL_CLICKS);

        inventory.open(player);
    }

    public void openChooseCurrencyMenu(Player player, PlayerAction playerAction, BigInteger defaultPrice) {
        FileUtils.Files file = FileUtils.Files.CHOOSE_CURRENCY;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        InventoryBuilder inventory = new InventoryBuilder(title, size);

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            String currencyName = FileUtils.get().getString(file, "Inventory.items." + str + ".currency");
            Currency currency = CurrencyAPI.getCurrency(currencyName);
            if (currency == null) continue;

            double weight = FileUtils.get().getDouble(file, "Inventory.items." + str + ".weight");
            BigInteger finalPrice = new BigInteger(String.format("%.0f", defaultPrice.doubleValue() * weight));

            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                    "{price}"
            }, new String[]{
                    StringUtils.replaceEach(currency.getAmountDisplay(), new String[]{
                            "{amount}"
                    }, new String[]{
                            NumberFormatter.getInstance().format(finalPrice)
                    })
            }).build();
            int slot = FileUtils.get().getInt(file, "Inventory.items." + str + ".slot");
            inventory.addItem(item, slot, () -> {
                player.closeInventory();
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, finalPrice, currency, playerAction));
                clearChat(player);

                String itemName = "";
                switch (playerAction) {
                    case BUY_ENERGY:
                        itemName = Items.getInstance().getEnergy(BigInteger.ONE).getItemMeta().getDisplayName();
                        break;
                    case BUY_PICKAXE:
                        itemName = Items.getInstance().getPickaxe().getItemMeta().getDisplayName();
                        break;
                    case BUY_REPAIR:
                        itemName = Items.getInstance().getRepair(BigInteger.ONE).getItemMeta().getDisplayName();
                        break;
                }

                for (String msg : CHOOSE_AMOUNT) {
                    player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                            "{item}",
                            "{price}",
                    }, new String[]{
                            itemName,
                            StringUtils.replaceEach(currency.getAmountDisplay(), new String[]{
                                    "{amount}"
                            }, new String[]{
                                    NumberFormatter.getInstance().format(finalPrice)
                            })
                    }));
                }
            }, ActionType.ALL_CLICKS);
        }

        inventory.open(player);
    }

    public void openGiftMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.GIFT;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder inventory = new InventoryBuilder(title, size, previousPageItem, previousPageSlot, nextPageItem, nextPageSlot);

        String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
        int i = -1;
        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            if (++i >= slots.length) i = 0;

            Spawner spawner = DataManager.getInstance().getSpawner(str);
            if (spawner == null) continue;

            BigInteger spawnersAmount = new BigInteger(FileUtils.get().getString(file, "Inventory.items." + str + ".spawners-amount", "1"));
            String toGet = spawner.getPermission() != null && !player.hasPermission(spawner.getPermission()) ? "locked" : "unlocked";
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(),
                    FileUtils.get().getFile(file).get().contains("Inventory.items." + str + ".locked")
                            && FileUtils.get().getFile(file).get().contains("Inventory.items." + str + ".unlocked")
                            ? "Inventory.items." + str + "." + toGet
                            : "Inventory.items." + str, new String[]{
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

            inventory.addItem(item, slot, () -> {
                if (spawner.getPermission() != null && !player.hasPermission(spawner.getPermission())) return;

                PlayerGeneralListeners.getChoosingGift().remove(player);
                player.getInventory().addItem(spawner.getItem(spawnersAmount, BigInteger.valueOf(100)));
                player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 0.5f, 10f);
                player.closeInventory();
            }, ActionType.ALL_CLICKS);
        }

        inventory.open(player);
    }

    private BigInteger getTotalDrops(Player player) {
        BigInteger dropsAmount = BigInteger.ZERO;

        for (PlacedSpawner spawner : DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())) {
            if (spawner == null) continue;
            if (spawner.getDrops().signum() <= 0) continue;

            dropsAmount = dropsAmount.add(spawner.getDrops());
        }

        return dropsAmount;
    }

    private BigInteger getTotalDropsPrice(Player player) {
        BigInteger dropsPrice = BigInteger.ZERO;

        for (PlacedSpawner spawner : DataManager.getInstance().getCache().getPlayerSpawnersByUUID(player.getUniqueId())) {
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