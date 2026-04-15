package org.Textureless.kip.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.Textureless.kip.listeners.death.DeathProfileResolver;
import org.Textureless.kip.listeners.death.KeepItemRuleMatcher;
import org.Textureless.kip.placeholders.PlaceholderParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerListener implements Listener {


    private final JavaPlugin plugin;
    private final Logger logger;
    private final Random random;
    private final DeathProfileResolver profileResolver;
    private final KeepItemRuleMatcher keepItemRuleMatcher;
    private final PlaceholderParser placeholderParser;
    private final MiniMessage miniMessage;
    private final Map<UUID, Map<EquipmentSlot, ItemStack>> respawnArmorByPlayer;

    public PlayerListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.random = new Random();
        this.profileResolver = new DeathProfileResolver(plugin);
        this.keepItemRuleMatcher = new KeepItemRuleMatcher();
        this.placeholderParser = new PlaceholderParser();
        this.miniMessage = MiniMessage.miniMessage();
        this.respawnArmorByPlayer = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        YamlConfiguration profilesConfig = profileResolver.loadProfiles();
        List<ConfigurationSection> profiles = profileResolver.resolveApplicableProfiles(player, profilesConfig);
        if (profiles.isEmpty()) {
            logger.warning("No profile available to process death event for player " + player.getName());
            return;
        }

        List<ConfigurationSection> optionsSections = extractOptionsSections(profiles);
        if (optionsSections.isEmpty()) {
            logger.warning("No options section found in applicable profiles for player " + player.getName());
            return;
        }

        ConfigurationSection primaryOptions = optionsSections.getLast();
        applyExperienceLoss(event, primaryOptions);
        keepEquippedArmorWithDurabilityLoss(player, event, primaryOptions);

        applyKeepItems(player, event, optionsSections);
        applyGiveItems(player, optionsSections);
    }

    private List<ConfigurationSection> extractOptionsSections(List<ConfigurationSection> profiles) {
        List<ConfigurationSection> optionsSections = new ArrayList<>();
        for (ConfigurationSection profile : profiles) {
            ConfigurationSection options = profile.getConfigurationSection("options");
            if (options != null) {
                optionsSections.add(options);
            }
        }

        return optionsSections;
    }

    private void applyExperienceLoss(PlayerDeathEvent event, ConfigurationSection options) {
        double lostPercentage = normalizePercentage(options.getDouble("experience-lost-percentage", 100.0));
        int droppedExp = event.getDroppedExp();
        int adjustedExp = (int) Math.round(droppedExp * (lostPercentage / 100.0));
        event.setDroppedExp(Math.max(0, adjustedExp));
    }

    private void keepEquippedArmorWithDurabilityLoss(Player player,
                                                     PlayerDeathEvent event,
                                                     ConfigurationSection options) {
        double lostPercentage = normalizePercentage(options.getDouble("armor-lost-percentage", 100.0));
        Map<EquipmentSlot, ItemStack> armorToEquipOnRespawn = new EnumMap<>(EquipmentSlot.class);

        addArmorPiece(armorToEquipOnRespawn, EquipmentSlot.HEAD, player.getInventory().getHelmet());
        addArmorPiece(armorToEquipOnRespawn, EquipmentSlot.CHEST, player.getInventory().getChestplate());
        addArmorPiece(armorToEquipOnRespawn, EquipmentSlot.LEGS, player.getInventory().getLeggings());
        addArmorPiece(armorToEquipOnRespawn, EquipmentSlot.FEET, player.getInventory().getBoots());

        for (Map.Entry<EquipmentSlot, ItemStack> armorPiece : armorToEquipOnRespawn.entrySet()) {
            ItemStack equippedPiece = armorPiece.getValue();
            if (equippedPiece == null || equippedPiece.getType().isAir() || !(equippedPiece.getItemMeta() instanceof Damageable)) {
                continue;
            }

            removeOneMatchingDrop(event, equippedPiece);

            ItemStack keptPiece = equippedPiece.clone();
            if (!applyArmorDurabilityLoss(keptPiece, lostPercentage)) {
                armorToEquipOnRespawn.put(armorPiece.getKey(), keptPiece);
            }
        }

        if (!armorToEquipOnRespawn.isEmpty()) {
            respawnArmorByPlayer.put(player.getUniqueId(), armorToEquipOnRespawn);
        }
    }

    private void addArmorPiece(Map<EquipmentSlot, ItemStack> armorPieces,
                               EquipmentSlot slot,
                               ItemStack itemStack) {
        if (itemStack != null) {
            armorPieces.put(slot, itemStack);
        }
    }

    private void removeOneMatchingDrop(PlayerDeathEvent event, ItemStack target) {
        for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext(); ) {
            ItemStack droppedItem = iterator.next();
            if (!droppedItem.isSimilar(target)) {
                continue;
            }

            if (droppedItem.getAmount() <= target.getAmount()) {
                iterator.remove();
            } else {
                droppedItem.setAmount(droppedItem.getAmount() - target.getAmount());
            }
            return;
        }
    }

    private boolean applyArmorDurabilityLoss(ItemStack itemStack, double lostPercentage) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable)) {
            return false;
        }

        int maxDurability = itemStack.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return false;
        }

        int damageToApply = Math.max(1, (int) Math.ceil(maxDurability * (lostPercentage / 100.0)));
        int newDamage = damageable.getDamage() + damageToApply;
        if (newDamage >= maxDurability) {
            return true;
        }

        damageable.setDamage(newDamage);
        itemStack.setItemMeta(damageable);
        return false;
    }

    private void applyKeepItems(Player player, PlayerDeathEvent event, List<ConfigurationSection> optionsSections) {
        for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext(); ) {
            ItemStack itemStack = iterator.next();
            if (!shouldKeepItem(player, itemStack, optionsSections)) {
                continue;
            }

            iterator.remove();
            event.getItemsToKeep().add(itemStack);
        }
    }

    private boolean shouldKeepItem(Player player, ItemStack itemStack, List<ConfigurationSection> optionsSections) {
        for (ConfigurationSection options : optionsSections) {
            ConfigurationSection keepItems = options.getConfigurationSection("keep-items");
            if (keepItems == null) {
                continue;
            }

            if (keepItemRuleMatcher.matchesAnyRule(player, itemStack, keepItems, random, placeholderParser)) {
                return true;
            }
        }

        return false;
    }

    private void applyGiveItems(Player player, List<ConfigurationSection> optionsSections) {
        for (ConfigurationSection options : optionsSections) {
            ConfigurationSection giveItems = options.getConfigurationSection("give-items");
            if (giveItems == null) {
                continue;
            }

            for (String key : giveItems.getKeys(false)) {
                ConfigurationSection rule = giveItems.getConfigurationSection(key);
                if (rule == null) {
                    continue;
                }

                String type = rule.getString("type", "");
                if ("COMMAND".equalsIgnoreCase(type)) {
                    executeCommandReward(player, rule);
                    continue;
                }

                if ("ITEM".equalsIgnoreCase(type)) {
                    giveItemReward(player, rule);
                    continue;
                }

                if ("MESSAGE".equalsIgnoreCase(type)) {
                    sendMessageReward(player, rule);
                }
            }
        }
    }

    private void executeCommandReward(Player player, ConfigurationSection rule) {
        String command = rule.getString("command", "");
        if (command.isBlank()) {
            return;
        }

        String parsedCommand = placeholderParser.parse(player, command).replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
    }

    private void giveItemReward(Player player, ConfigurationSection rule) {
        String materialName = rule.getString("material", "");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            logger.warning("Invalid material in give-items: " + materialName);
            return;
        }

        int amount = Math.max(1, rule.getInt("amount", 1));
        ItemStack reward = new ItemStack(material, amount);
        ItemMeta itemMeta = reward.getItemMeta();

        if (itemMeta != null) {
            String name = rule.getString("name");
            if (name != null && !name.isBlank()) {
                itemMeta.displayName(Component.text(placeholderParser.parse(player, name)));
            }

            List<String> lore = rule.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> parsedLore = placeholderParser.parse(player, lore);
                itemMeta.lore(parsedLore.stream().map(Component::text).toList());
            }
            reward.setItemMeta(itemMeta);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            var leftovers = player.getInventory().addItem(reward);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        });
    }

    private void sendMessageReward(Player player, ConfigurationSection rule) {
        String message = rule.getString("message", "");
        if (!message.isBlank()) {
            player.sendMessage(Component.text(placeholderParser.parse(player, message)));
        }

        List<String> messages = rule.getStringList("messages");
        for (String line : placeholderParser.parse(player, messages)) {
            if (!line.isBlank()) {
                player.sendMessage(Component.text(line));
            }
        }

        String miniMessageLine = rule.getString("minmessage", "");
        if (!miniMessageLine.isBlank()) {
            player.sendMessage(miniMessage.deserialize(placeholderParser.parse(player, miniMessageLine)));
        }

        List<String> miniMessages = rule.getStringList("minmessages");
        for (String line : placeholderParser.parse(player, miniMessages)) {
            if (!line.isBlank()) {
                player.sendMessage(miniMessage.deserialize(line));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Map<EquipmentSlot, ItemStack> armorToEquip = respawnArmorByPlayer.remove(event.getPlayer().getUniqueId());
        if (armorToEquip == null || armorToEquip.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Map.Entry<EquipmentSlot, ItemStack> armorPiece : armorToEquip.entrySet()) {
                switch (armorPiece.getKey()) {
                    case HEAD -> event.getPlayer().getInventory().setHelmet(armorPiece.getValue());
                    case CHEST -> event.getPlayer().getInventory().setChestplate(armorPiece.getValue());
                    case LEGS -> event.getPlayer().getInventory().setLeggings(armorPiece.getValue());
                    case FEET -> event.getPlayer().getInventory().setBoots(armorPiece.getValue());
                    default -> {
                    }
                }
            }
        });
    }

    private double normalizePercentage(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
