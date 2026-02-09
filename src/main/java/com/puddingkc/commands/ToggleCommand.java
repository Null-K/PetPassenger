package com.puddingkc.commands;

import com.puddingkc.PetPassenger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record ToggleCommand(PetPassenger plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
        if (sender instanceof Player player) {
            if (player.hasPermission("petpassenger.toggle")) {
                plugin.toggle(player);
                return true;
            }
        }
        return false;
    }
}
