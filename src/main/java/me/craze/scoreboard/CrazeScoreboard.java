package me.craze;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CrazeScoreboard extends JavaPlugin {

    private final Map<String, FileConfiguration> worldConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        for (World world : Bukkit.getWorlds()) {
            loadWorldConfig(world.getName());
        }

        getLogger().info("CrazeScoreboard enabled.");
    }

    @Override
    public void onDisable() {
        for (Map.Entry<String, FileConfiguration> entry : worldConfigs.entrySet()) {
            saveWorldConfig(entry.getKey());
        }
        getLogger().info("CrazeScoreboard disabled.");
    }

    private void loadWorldConfig(String worldName) {
        File worldConfigFile = new File(getDataFolder(), worldName + ".yml");

        if (!worldConfigFile.exists()) {
            try {
                getDataFolder().mkdirs();
                worldConfigFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(worldConfigFile);
        worldConfigs.put(worldName, config);
    }

    private void saveWorldConfig(String worldName) {
        FileConfiguration config = worldConfigs.get(worldName);
        File file = new File(getDataFolder(), worldName + ".yml");

        if (config != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        String worldName = player.getWorld().getName();
        FileConfiguration config = worldConfigs.get(worldName);

        if (args.length == 0) {
            player.sendMessage("Usage: /csb add <board>");
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /csb add <board>");
                return true;
            }

            String boardName = args[1];
            config.set("boards." + boardName, "Welcome to " + worldName);
            saveWorldConfig(worldName);

            player.sendMessage("Scoreboard '" + boardName + "' added to world: " + worldName);
            return true;
        }

        return false;
    }
}
