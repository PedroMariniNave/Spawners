package com.zpedroo.voltzspawners.utils.item;

import com.zpedroo.voltzspawners.utils.FileUtils;
import com.zpedroo.voltzspawners.utils.builder.ItemBuilder;
import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Items {

    private static Items instance;
    public static Items getInstance() { return instance; }

    private ItemStack energy;
    private ItemStack infiniteEnergy;
    private ItemStack infiniteRepair;
    private ItemStack pickaxe;
    private ItemStack repair;
    private ItemStack gift;

    public Items() {
        instance = this;
        this.energy = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Energy").build();
        this.infiniteEnergy = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Infinite-Energy").build();
        this.infiniteRepair = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Infinite-Repair").build();
        this.pickaxe = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Pickaxe").build();
        this.repair = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Repair").build();
        this.gift = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Gift").build();
    }

    public ItemStack getEnergy(BigInteger amount) {
        NBTItem nbt = new NBTItem(energy.clone());
        nbt.setString("SpawnersEnergy", amount.toString());

        ItemStack item = nbt.getItem();

        if (item.getItemMeta() != null) {
            String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
            List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : null;
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) meta.setDisplayName(StringUtils.replaceEach(displayName, new String[] {
                    "{amount}"
            }, new String[] {
                    NumberFormatter.getInstance().format(amount)
            }));

            if (lore != null) {
                List<String> newLore = new ArrayList<>(lore.size());

                for (String str : lore) {
                    newLore.add(StringUtils.replaceEach(str, new String[] {
                            "{amount}"
                    }, new String[] {
                            NumberFormatter.getInstance().format(amount)
                    }));
                }

                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getInfiniteEnergy() {
        NBTItem nbt = new NBTItem(infiniteEnergy.clone());
        nbt.addCompound("SpawnersInfiniteEnergy");

        return nbt.getItem();
    }

    public ItemStack getInfiniteRepair() {
        NBTItem nbt = new NBTItem(infiniteRepair.clone());
        nbt.addCompound("SpawnersInfiniteRepair");

        return nbt.getItem();
    }

    public ItemStack getPickaxe() {
        NBTItem nbt = new NBTItem(pickaxe.clone());
        nbt.addCompound("SpawnersPickaxe");

        return nbt.getItem();
    }

    public ItemStack getRepair(BigInteger percentage) {
        NBTItem nbt = new NBTItem(repair.clone());
        nbt.setString("SpawnersRepair", percentage.toString());

        ItemStack item = nbt.getItem();

        if (item.getItemMeta() != null) {
            String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
            List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : null;
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) meta.setDisplayName(StringUtils.replaceEach(displayName, new String[] {
                    "{percentage}"
            }, new String[] {
                    NumberFormatter.getInstance().format(percentage)
            }));

            if (lore != null) {
                List<String> newLore = new ArrayList<>(lore.size());

                for (String str : lore) {
                    newLore.add(StringUtils.replaceEach(str, new String[] {
                            "{percentage}"
                    }, new String[] {
                            NumberFormatter.getInstance().format(percentage)
                    }));
                }

                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getGift(Player player) {
        NBTItem nbt = new NBTItem(gift.clone());
        nbt.addCompound("SpawnersGift");
        nbt.setString("GiftOwner", player.getUniqueId().toString());

        return nbt.getItem();
    }
}
