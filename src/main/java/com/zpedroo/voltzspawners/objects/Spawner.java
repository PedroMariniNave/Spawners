package com.zpedroo.voltzspawners.objects;

import com.zpedroo.voltzspawners.utils.formatter.NumberFormatter;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Spawner {

    private EntityType entity;
    private String entityName;
    private ItemStack item;
    private Material block;
    private String type;
    private String typeTranslated;
    private String displayName;
    private int delay;
    private BigInteger dropsAmount;
    private BigInteger dropsValue;
    private BigInteger dropsPreviousValue;
    private BigInteger dropsMinimumValue;
    private BigInteger dropsMaximumValue;
    private BigInteger maximumStack;
    private String permission;
    private List<String> commands;

    public Spawner(EntityType entity, String entityName, ItemStack item, Material block, String type, String typeTranslated, String displayName, int delay, BigInteger dropsAmount, BigInteger dropsValue, BigInteger dropsPreviousValue, BigInteger dropsMinimumValue, BigInteger dropsMaximumValue, BigInteger maximumStack, String permission, List<String> commands) {
        this.entity = entity;
        this.entityName = entityName;
        this.item = item;
        this.block = block;
        this.type = type;
        this.typeTranslated = typeTranslated;
        this.displayName = displayName;
        this.delay = delay;
        this.dropsAmount = dropsAmount;
        this.dropsValue = dropsValue;
        this.dropsPreviousValue = dropsPreviousValue;
        this.dropsMinimumValue = dropsMinimumValue;
        this.dropsMaximumValue = dropsMaximumValue;
        this.maximumStack = maximumStack;
        this.permission = permission;
        this.commands = commands;
    }

    public Material getBlock() {
        return block;
    }

    public EntityType getEntity() {
        return entity;
    }

    public String getEntityName() {
        return entityName;
    }

    public ItemStack getDisplayItem() {
        return item.clone();
    }

    public ItemStack getItem(BigInteger amount, BigInteger integrity) {
        NBTItem nbt = new NBTItem(item.clone());
        nbt.setString("SpawnersAmount", amount.toString());
        nbt.setString("SpawnersIntegrity", integrity.toString());
        nbt.setString("SpawnersType", getType());

        ItemStack item = nbt.getItem();
        if (item.getItemMeta() != null) {
            String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
            List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : null;
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) meta.setDisplayName(StringUtils.replaceEach(displayName, new String[]{
                    "{amount}",
                    "{integrity}"
            }, new String[]{
                    NumberFormatter.getInstance().format(amount),
                    integrity.toString() + "%"
            }));

            if (lore != null) {
                List<String> newLore = new ArrayList<>(lore.size());

                for (String str : lore) {
                    newLore.add(StringUtils.replaceEach(str, new String[]{
                            "{amount}",
                            "{integrity}"
                    }, new String[]{
                            NumberFormatter.getInstance().format(amount),
                            integrity.toString() + "%"
                    }));
                }

                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public String getType() {
        return type;
    }

    public String getTypeTranslated() {
        return typeTranslated;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDelay() {
        return delay;
    }

    public BigInteger getDropsAmount() {
        return dropsAmount;
    }

    public BigInteger getDropsValue() {
        return dropsValue;
    }

    public BigInteger getDropsPreviousValue() {
        return dropsPreviousValue;
    }

    public BigInteger getDropsMinimumValue() {
        return dropsMinimumValue;
    }

    public BigInteger getDropsMaximumValue() {
        return dropsMaximumValue;
    }

    public BigInteger getMaximumStack() {
        return maximumStack;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setDropsValue(BigInteger value) {
        this.dropsValue = value;
    }

    public void setDropsPreviousValue(BigInteger value) {
        this.dropsPreviousValue = value;
    }
}