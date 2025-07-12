package com.lion.cleanchat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CleanChat extends JavaPlugin implements Listener, CommandExecutor {

    private File blockedWordsFile;
    private FileConfiguration blockedWordsConfig;
    private Set<String> blockedWords = new HashSet<>();
    private boolean enabled = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadBlockedWords();
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("cleanchat").setExecutor(this);
        getLogger().info("CleanChat enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CleanChat disabled!");
    }

    private void loadBlockedWords() {
        blockedWordsFile = new File(getDataFolder(), "blockedwords.yml");
        if (!blockedWordsFile.exists()) {
            saveResource("blockedwords.yml", false);
        }
        blockedWordsConfig = YamlConfiguration.loadConfiguration(blockedWordsFile);
        List<String> words = blockedWordsConfig.getStringList("blockedwords");
        blockedWords.clear();
        for (String word : words) {
            blockedWords.add(word.toLowerCase());
        }
        enabled = getConfig().getBoolean("enabled", true);
    }

    private void saveBlockedWords() {
        blockedWordsConfig.set("blockedwords", blockedWords.stream().toList());
        try {
            blockedWordsConfig.save(blockedWordsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;

        String message = event.getMessage().toLowerCase();
        for (String blocked : blockedWords) {
            if (message.contains(blocked)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Your message contains blocked words and was not sent.");
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cleanchat.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "CleanChat Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/cleanchat addword <word> - Add a blocked word");
            sender.sendMessage(ChatColor.YELLOW + "/cleanchat removeword <word> - Remove a blocked word");
            sender.sendMessage(ChatColor.YELLOW + "/cleanchat list - List blocked words");
            sender.sendMessage(ChatColor.YELLOW + "/cleanchat toggle - Enable/disable filter");
            sender.sendMessage(ChatColor.YELLOW + "/cleanchat reload - Reload config and words");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "addword":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cleanchat addword <word>");
                    return true;
                }
                String addWord = args[1].toLowerCase();
                if (blockedWords.contains(addWord)) {
                    sender.sendMessage(ChatColor.RED + "This word is already blocked.");
                } else {
                    blockedWords.add(addWord);
                    saveBlockedWords();
                    sender.sendMessage(ChatColor.GREEN + "Added blocked word: " + addWord);
                }
                break;

            case "removeword":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cleanchat removeword <word>");
                    return true;
                }
                String removeWord = args[1].toLowerCase();
                if (!blockedWords.contains(removeWord)) {
                    sender.sendMessage(ChatColor.RED + "This word is not in the blocked list.");
                } else {
                    blockedWords.remove(removeWord);
                    saveBlockedWords();
                    sender.sendMessage(ChatColor.GREEN + "Removed blocked word: " + removeWord);
                }
                break;

            case "list":
                sender.sendMessage(ChatColor.GOLD + "Blocked words:");
                if (blockedWords.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No blocked words set.");
                } else {
                    for (String w : blockedWords) {
                        sender.sendMessage(ChatColor.YELLOW + "- " + w);
                    }
                }
                break;

            case "toggle":
                enabled = !enabled;
                getConfig().set("enabled", enabled);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "CleanChat filter is now " + (enabled ? "enabled" : "disabled") + ".");
                break;

            case "reload":
                reloadConfig();
                loadBlockedWords();
                sender.sendMessage(ChatColor.GREEN + "CleanChat config and blocked words reloaded.");
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /cleanchat for help.");
                break;
        }
        return true;
    }
}