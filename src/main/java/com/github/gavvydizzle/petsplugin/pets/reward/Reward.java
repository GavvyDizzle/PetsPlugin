package com.github.gavvydizzle.petsplugin.pets.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class Reward {

    private final String id;
    private final int weight;
    private final List<String> messages, commands;

    public Reward(String id, int weight, List<String> messages, List<String> commands) {
        if (weight <= 0) {
            throw new RuntimeException("Reward weight must be positive");
        }

        this.id = id;
        this.weight = weight;
        this.messages = messages;
        this.commands = commands;
    }

    /**
     * Collects the reward for this player.
     * This will send all messages then run commands after.
     * @param player The player
     */
    protected void collect(Player player) {
        for (String message : messages) {
            player.sendMessage(message);
        }
        for (String command : commands) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }
    }

    protected String getId() {
        return id;
    }

    protected int getWeight() {
        return weight;
    }

    protected List<String> getMessages() {
        return messages;
    }

    protected List<String> getCommands() {
        return commands;
    }
}
