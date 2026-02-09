package com.puddingkc.commands;

import com.puddingkc.PetPassenger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public record ReloadCommand(PetPassenger plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        if (sender.hasPermission("petpassenger.reload")) {
            plugin.loadConfig();
            sender.sendMessage(plugin.getMessages("reload-success"));
            return true;
        }
        return false;
    }
}
