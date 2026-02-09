package com.puddingkc.utils;

import com.puddingkc.PetPassenger;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiExpansion extends PlaceholderExpansion {

    private final PetPassenger plugin;
    public PapiExpansion(PetPassenger plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "petpassenger";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || !player.isOnline() || params.isEmpty()) { return null; }
        return params.equalsIgnoreCase("enabled") ? String.valueOf(plugin.getEnabled(player)) : null;
    }
}
