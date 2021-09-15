package com.zpedroo.voltzspawners.utils.config;

import com.zpedroo.voltzspawners.FileUtils;
import org.bukkit.ChatColor;

public class Titles {

    public static final String WHEN_KILL_TITLE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Titles.when-kill.title"));

    public static final String WHEN_KILL_SUBTITLE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Titles.when-kill.subtitle"));

    public static final String WHEN_SELL_TITLE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Titles.when-sell.title"));

    public static final String WHEN_SELL_SUBTITLE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Titles.when-sell.subtitle"));

    private static String getColored(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}