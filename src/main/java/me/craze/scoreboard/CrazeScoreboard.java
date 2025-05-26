package me.craze.scoreboard;

import org.bukkit.plugin.java.JavaPlugin;

public class CrazeScoreboard extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("CrazeScoreboard has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CrazeScoreboard has been disabled.");
    }
}
