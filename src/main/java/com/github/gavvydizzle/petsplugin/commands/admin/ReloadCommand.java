package com.github.gavvydizzle.petsplugin.commands.admin;

import com.github.gavvydizzle.petsplugin.PetsPlugin;
import com.github.gavvydizzle.petsplugin.commands.AdminCommandManager;
import com.github.gavvydizzle.petsplugin.configs.CommandsConfig;
import com.github.gavvydizzle.petsplugin.configs.MenusConfig;
import com.github.gavvydizzle.petsplugin.configs.MessagesConfig;
import com.github.gavvydizzle.petsplugin.configs.SoundsConfig;
import com.github.gavvydizzle.petsplugin.utils.Messages;
import com.github.gavvydizzle.petsplugin.utils.Sounds;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends SubCommand {

    private final AdminCommandManager adminCommandManager;
    private final ArrayList<String> argsList;

    public ReloadCommand(AdminCommandManager adminCommandManager) {
        this.adminCommandManager = adminCommandManager;

        setName("reload");
        setDescription("Reloads this plugin or a specified portion");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " reload [arg]");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());

        argsList = new ArrayList<>();
        argsList.add("commands");
        argsList.add("gui");
        argsList.add("messages");
        argsList.add("pets");
        argsList.add("sounds");
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "commands" -> {
                    reloadCommands();
                    sender.sendMessage(ChatColor.GREEN + "[" + PetsPlugin.getInstance().getName() + "] " + "Successfully reloaded commands");
                }
                case "gui" -> {
                    reloadGUI();
                    sender.sendMessage(ChatColor.GREEN + "[" + PetsPlugin.getInstance().getName() + "] " + "Successfully reloaded all GUIs");
                }
                case "messages" -> {
                    reloadMessages();
                    sender.sendMessage(ChatColor.GREEN + "[" + PetsPlugin.getInstance().getName() + "] " + "Successfully reloaded all messages");
                }
                case "pets" -> {
                    reloadPets(true);
                    sender.sendMessage(ChatColor.GREEN + "[" + PetsPlugin.getInstance().getName() + "] " + "Successfully reloaded all pets");
                }
                case "sounds" -> {
                    reloadSounds();
                    sender.sendMessage(ChatColor.GREEN + "[" + PetsPlugin.getInstance().getName() + "] " + "Successfully reloaded all sounds");
                }
            }
        }
        else {
            reloadCommands();
            reloadPets(false); // Must reload before GUI
            reloadGUI();
            reloadMessages();
            reloadSounds();
            sender.sendMessage(ChatColor.GREEN + "[" + PetsPlugin.getInstance().getName() + "] " + "Successfully reloaded");
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        ArrayList<String> list = new ArrayList<>();

        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], argsList, list);
        }

        return list;
    }

    private void reloadCommands() {
        CommandsConfig.reload();
        adminCommandManager.reload();
    }

    private void reloadGUI() {
        MenusConfig.reload();
        PetsPlugin.getInstance().getInventoryManager().reload();
    }

    private void reloadMessages() {
        MessagesConfig.reload();
        Messages.reloadMessages();
    }

    private void reloadPets(boolean updateMenuPets) {
        PetsPlugin.getInstance().reloadConfig();
        PetsPlugin.getInstance().getPetManager().reload();
        PetsPlugin.getInstance().getInventoryManager().getPetListAdminMenu().reloadContents();

        if (updateMenuPets) PetsPlugin.getInstance().getInventoryManager().updatePetItems();
    }

    private void reloadSounds() {
        SoundsConfig.reload();
        Sounds.reload();
    }

}