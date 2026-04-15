package org.Textureless.kip.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlaceholderParser {

    private final boolean placeholderApiEnabled;

    public PlaceholderParser() {
        this.placeholderApiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public String parse(Player player, String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String parsed = input.replace("{player}", player.getName());
        if (!placeholderApiEnabled) {
            return parsed;
        }

        try {
            return PlaceholderAPI.setPlaceholders(player, parsed);
        } catch (NoClassDefFoundError ignored) {
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
