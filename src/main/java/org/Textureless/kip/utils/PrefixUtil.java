package org.Textureless.kip.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrefixUtil {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private PrefixUtil() {
    }

    public static Component prefix(JavaPlugin plugin) {
        return SERIALIZER.deserialize(plugin.getConfig().getString("prefix", "&7[&6KIP&7] &r"));
    }
}

