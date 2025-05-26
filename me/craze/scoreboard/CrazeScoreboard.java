package me.craze.csb;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CrazeScoreboard extends JavaPlugin {

    private Economy economy;
    private Map<UUID, Scoreboard> playerBoards = new HashMap<>();
    private Map<String, FileConfiguration> worldConfigs = new HashMap<>();
    private Map<String, File> worldConfigFiles = new HashMap<>();
    private List<String> animatedTitles = new ArrayList<>();
    private int animationIndex = 0;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().warning("Vault not found! Money placeholders won't work.");
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().warning("PlaceholderAPI not found! Placeholders won't work.");
        }

        loadAllWorldConfigs();

        // Default animated titles
        animatedTitles.add(ChatColor.GOLD + "" + ChatColor.BOLD + "CrazeScoreboard");
        animatedTitles.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "CrazeScoreboard");
        animatedTitles.add(ChatColor.GOLD + "" + ChatColor.BOLD + "CrazeScoreboard");
        animatedTitles.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "CrazeScoreboard");

        // Schedule scoreboard updates for all online players every 5 seconds (100 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                animationIndex = (animationIndex + 1) % animatedTitles.size();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(this, 0L, 100L);

        // Command
        this.getCommand("csb").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("crazescoreboard.reload")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to reload.");
                    return true;
                }
                loadAllWorldConfigs();
                sender.sendMessage(ChatColor.GREEN + "CrazeScoreboard configs reloaded!");
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + "CrazeScoreboard v1.0 by Mehetab");
            sender.sendMessage(ChatColor.YELLOW + "/csb reload - Reload configs");
            return true;
        });
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private void loadAllWorldConfigs() {
        worldConfigs.clear();
        worldConfigFiles.clear();

        File worldsFolder = new File(getDataFolder(), "worlds");
        if (!worldsFolder.exists()) {
            worldsFolder.mkdirs();
        }

        File[] files = worldsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            worldConfigs.put(file.getName().replace(".yml", ""), cfg);
            worldConfigFiles.put(file.getName().replace(".yml", ""), file);
        }
        getLogger().info("Loaded " + worldConfigs.size() + " world configs.");
    }

    private void updateScoreboard(Player player) {
        World world = player.getWorld();
        String worldName = world.getName();

        FileConfiguration config = worldConfigs.getOrDefault(worldName, null);
        if (config == null) {
            // fallback default config
            config = createDefaultWorldConfig(worldName);
        }

        if (!config.getBoolean("enabled", true)) {
            // Scoreboard disabled for this world
            playerBoards.remove(player.getUniqueId());
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }

        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            playerBoards.put(player.getUniqueId(), board);
        }

        String title = animatedTitles.get(animationIndex);
        Objective objective = board.getObjective("craze");
        if (objective == null) {
            objective = board.registerNewObjective("craze", "dummy", title);
        } else {
            objective.setDisplayName(title);
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear old scores (because entries can't be removed, reset scoreboard)
        board.getEntries().forEach(board::resetScores);

        List<String> lines = config.getStringList("lines");
        if (lines.isEmpty()) {
            lines = Arrays.asList(
                ChatColor.GRAY + "---------------------",
                ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + "%statistic_player_kills%",
                ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + "%statistic_player_deaths%",
                ChatColor.YELLOW + "Money: " + ChatColor.WHITE + "%vault_eco_balance_formatted%",
                ChatColor.YELLOW + "Time: " + ChatColor.WHITE + "%server_time%",
                ChatColor.YELLOW + "Online: " + ChatColor.WHITE + "%server_online%",
                ChatColor.YELLOW + "World: " + ChatColor.WHITE + worldName,
                ChatColor.GRAY + "---------------------"
            );
        }

        Collections.reverse(lines); // Because scoreboard lines count downwards

        int score = lines.size();
        for (String line : lines) {
            String parsed = parsePlaceholders(player, line.replace("CustomWorldNameHere", worldName));
            objective.getScore(parsed).setScore(score);
            score--;
        }

        player.setScoreboard(board);
    }

    private String parsePlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // Custom placeholders if Vault present
        if (economy != null) {
            if (text.contains("%vault_eco_balance_formatted%")) {
                String balance = economy.format(economy.getBalance(player));
                text = text.replace("%vault_eco_balance_formatted%", balance);
            }
        }

        if (text.contains("%server_time%")) {
            String time = new SimpleDateFormat("HH:mm").format(new Date());
            text = text.replace("%server_time%", time);
        }
        if (text.contains("%server_online%")) {
            text = text.replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private FileConfiguration createDefaultWorldConfig(String worldName) {
        File file = new File(getDataFolder() + "/worlds", worldName + ".yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                cfg.set("enabled", true);
                cfg.set("lines", Arrays.asList(
                        "&7---------------------",
                        "&eKills: &f%statistic_player_kills%",
                        "&eDeaths: &f%statistic_player_deaths%",
                        "&eMoney: &f%vault_eco_balance_formatted%",
                        "&eTime: &f%server_time%",
                        "&eOnline: &f%server_online%",
                        "&eWorld: &fCustomWorldNameHere",
                        "&7---------------------"
                ));
                cfg.save(file);
                worldConfigs.put(worldName, cfg);
                worldConfigFiles.put(worldName, file);
                getLogger().info("Created default config for world: " + worldName);
                return cfg;
            } catch (IOException e) {
                getLogger().warning("Could not create default config for world: " + worldName);
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
