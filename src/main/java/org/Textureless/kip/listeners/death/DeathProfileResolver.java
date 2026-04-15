package org.Textureless.kip.listeners.death;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DeathProfileResolver {

    private final JavaPlugin plugin;

    public DeathProfileResolver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public YamlConfiguration loadProfiles() {
        File file = new File(plugin.getDataFolder(), "profiles.yml");
        return YamlConfiguration.loadConfiguration(file);
    }

    public List<ConfigurationSection> resolveApplicableProfiles(Player player, YamlConfiguration profilesConfig) {
        List<ProfileEntry> selectedProfiles = new ArrayList<>();
        Set<String> selectedKeys = new HashSet<>();

        for (String profileKey : profilesConfig.getKeys(false)) {
            ConfigurationSection profile = profilesConfig.getConfigurationSection(profileKey);
            if (profile == null || !isApplicableProfile(player, profile)) {
                continue;
            }

            selectedProfiles.add(new ProfileEntry(profileKey, profile, getPriority(profile)));
            selectedKeys.add(profileKey);
        }

        String defaultProfileName = plugin.getConfig().getString("default-profile", "default");
        if (!selectedKeys.contains(defaultProfileName)) {
            ConfigurationSection defaultProfile = profilesConfig.getConfigurationSection(defaultProfileName);
            if (defaultProfile != null) {
                selectedProfiles.add(new ProfileEntry(defaultProfileName, defaultProfile, getPriority(defaultProfile)));
            }
        }

        selectedProfiles.sort(Comparator.comparingInt(ProfileEntry::priority));
        List<ConfigurationSection> orderedProfiles = new ArrayList<>(selectedProfiles.size());
        for (ProfileEntry selectedProfile : selectedProfiles) {
            orderedProfiles.add(selectedProfile.profile());
        }

        return orderedProfiles;
    }

    private boolean isApplicableProfile(Player player, ConfigurationSection profile) {
        ConfigurationSection display = profile.getConfigurationSection("display");
        if (display == null) {
            return false;
        }

        String permission = display.getString("permission", "");
        return permission.isBlank() || player.hasPermission(permission);
    }

    private int getPriority(ConfigurationSection profile) {
        ConfigurationSection display = profile.getConfigurationSection("display");
        return display == null ? 0 : display.getInt("priority", 0);
    }

    private record ProfileEntry(String key, ConfigurationSection profile, int priority) {
    }
}
