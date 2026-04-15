package org.Textureless.kip.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlaceholderParser {


    public String parse(Player player, String input) {
        var logger = Bukkit.getLogger();

        if (input == null || input.isBlank()) {
            return "";
        }

        String parsed = input.replace("{player}", player.getName());
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logger.warning("PlaceholderAPI is not enabled. Placeholders will not be parsed.");
            return parsed;
        }

        try {
            return PlaceholderAPI.setPlaceholders(player, parsed);
        } catch (NoClassDefFoundError ignored) {
            logger.warning("Failed to parse placeholders for player " + player.getName() + ".");

            return parsed;
        }
    }

    public List<String> parse(Player player, List<String> lines) {
        List<String> parsed = new ArrayList<>(lines.size());
        for (String line : lines) {
            parsed.add(parse(player, line));
        }

        return parsed;
    }
}
