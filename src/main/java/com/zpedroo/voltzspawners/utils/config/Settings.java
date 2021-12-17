package com.zpedroo.voltzspawners.utils.config;

import com.zpedroo.voltzspawners.utils.FileUtils;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    public static final String LOOTING_BONUS = FileUtils.get().getString(FileUtils.Files.CONFIG, "Settings.looting-bonus");

    public static final int STACK_RADIUS = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.stack-radius");

    public static final int SPAWNER_UPDATE = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.spawner-update");

    public static final int TAX_REMOVE_STACK = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.tax-remove-stack");

    public static final long SAVE_INTERVAL = FileUtils.get().getLong(FileUtils.Files.CONFIG, "Settings.save-interval");

    public static final String[] SPAWNER_HOLOGRAM = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Settings.hologram")).toArray(new String[1]);

    public static final List<String> ALWAYS_ENABLED_WORLDS = FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Settings.always-enabled-worlds");

    public static long NEXT_UPDATE = FileUtils.get().getLong(FileUtils.Files.CONFIG, "Settings.next-update"); // not final

    private static String getColored(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    private static List<String> getColored(List<String> list) {
        List<String> colored = new ArrayList<>(list.size());
        for (String str : list) {
            colored.add(getColored(str));
        }

        return colored;
    }
}