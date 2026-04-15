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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final Map<UUID, List<PendingReward>> pendingRewardsByPlayer;
    private final Map<UUID, Map<EquipmentSlot, ItemStack>> armorToEquipOnRespawn;

    public PlayerListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.random = new Random();
        this.profileResolver = new DeathProfileResolver(plugin);
        this.keepItemRuleMatcher = new KeepItemRuleMatcher();
        this.placeholderParser = new PlaceholderParser();
        this.miniMessage = MiniMessage.miniMessage();
        this.pendingRewardsByPlayer = new HashMap<>();
        this.armorToEquipOnRespawn = new HashMap<>();
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
        queueGiveItemsForRespawn(player, optionsSections);
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
        Map<EquipmentSlot, ItemStack> equippedArmor = new EnumMap<>(EquipmentSlot.class);

        addArmorPiece(equippedArmor, EquipmentSlot.HEAD, player.getInventory().getHelmet());
        addArmorPiece(equippedArmor, EquipmentSlot.CHEST, player.getInventory().getChestplate());
        addArmorPiece(equippedArmor, EquipmentSlot.LEGS, player.getInventory().getLeggings());
        addArmorPiece(equippedArmor, EquipmentSlot.FEET, player.getInventory().getBoots());

        Map<EquipmentSlot, ItemStack> armorToRespawn = new EnumMap<>(EquipmentSlot.class);
        
        for (Map.Entry<EquipmentSlot, ItemStack> armorEntry : equippedArmor.entrySet()) {
            ItemStack equippedPiece = armorEntry.getValue();
            if (equippedPiece == null || equippedPiece.getType().isAir() || !(equippedPiece.getItemMeta() instanceof Damageable)) {
                continue;
            }

            removeOneMatchingDrop(event, equippedPiece);

            ItemStack keptPiece = equippedPiece.clone();
            boolean broke = applyArmorDurabilityLoss(keptPiece, lostPercentage);
            if (broke) {
                logger.info("Armor broke after durability loss: " + keptPiece.getType());
                continue;
            }
            
            logger.info("Keeping armor with durability loss: " + keptPiece.getType());
            armorToRespawn.put(armorEntry.getKey(), keptPiece);
        }
        
        if (!armorToRespawn.isEmpty()) {
            armorToEquipOnRespawn.put(player.getUniqueId(), armorToRespawn);
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
                logger.info("Removed armor drop: " + droppedItem.getType() + " (full item)");
                iterator.remove();
            } else {
                int remaining = droppedItem.getAmount() - target.getAmount();
                logger.info("Reduced armor drop: " + droppedItem.getType() + " from " + droppedItem.getAmount() + " to " + remaining);
                droppedItem.setAmount(remaining);
            }
            return;
        }
        logger.warning("Could not find matching drop for armor: " + target.getType());
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

    private void queueGiveItemsForRespawn(Player player, List<ConfigurationSection> optionsSections) {
        List<PendingReward> pendingRewards = new ArrayList<>();

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
                    pendingRewards.add(targetPlayer -> executeCommandReward(targetPlayer, rule));
                    continue;
                }

                if ("ITEM".equalsIgnoreCase(type)) {
                    pendingRewards.add(targetPlayer -> giveItemReward(targetPlayer, rule));
                    continue;
                }

                if ("MESSAGE".equalsIgnoreCase(type)) {
                    pendingRewards.add(targetPlayer -> sendMessageReward(targetPlayer, rule));
                }
            }
        }

        if (!pendingRewards.isEmpty()) {
            pendingRewardsByPlayer.put(player.getUniqueId(), pendingRewards);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        

        Map<EquipmentSlot, ItemStack> armorToEquip = armorToEquipOnRespawn.remove(playerUuid);
        if (armorToEquip != null && !armorToEquip.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map.Entry<EquipmentSlot, ItemStack> entry : armorToEquip.entrySet()) {
                    switch (entry.getKey()) {
                        case HEAD -> player.getInventory().setHelmet(entry.getValue());
                        case CHEST -> player.getInventory().setChestplate(entry.getValue());
                        case LEGS -> player.getInventory().setLeggings(entry.getValue());
                        case FEET -> player.getInventory().setBoots(entry.getValue());
                        default -> {}
                    }
                }
            });
        }
        List<PendingReward> pendingRewards = pendingRewardsByPlayer.remove(playerUuid);
        if (pendingRewards == null || pendingRewards.isEmpty()) {
            return;
        }

        logger.info("[DEATH-REWARD] Queued " + pendingRewards.size() + " rewards for " + player.getName() + ", will execute in 5 ticks");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player respawnedPlayer = event.getPlayer();
            if (respawnedPlayer == null || !respawnedPlayer.isOnline()) {
                logger.warning("[DEATH-REWARD] Player " + (respawnedPlayer == null ? "NULL" : respawnedPlayer.getName()) + " is null or offline when executing pending rewards");
                return;
            }
            logger.info("[DEATH-REWARD] Executing " + pendingRewards.size() + " pending rewards for " + respawnedPlayer.getName());
            for (PendingReward pendingReward : pendingRewards) {
                pendingReward.apply(respawnedPlayer);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        pendingRewardsByPlayer.remove(playerUuid);
        armorToEquipOnRespawn.remove(playerUuid);
    }

    private void executeCommandReward(Player player, ConfigurationSection rule) {
        String command = rule.getString("command", "");
        if (command.isBlank()) {
            return;
        }

        String parsedCommand = placeholderParser.parse(player, command);
        logger.info("[REWARD] Executing command: " + parsedCommand);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
    }

    private void giveItemReward(Player player, ConfigurationSection rule) {
        String materialName = rule.getString("material", "");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            logger.warning("Invalid material in give-items: " + materialName);
            return;
        }

        if (!player.isOnline()) {
            logger.warning("Player " + player.getName() + " is offline when trying to give item reward");
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

        logger.info("[REWARD] Giving item to " + player.getName() + ": " + reward.getType() + " x" + reward.getAmount());
        var leftovers = player.getInventory().addItem(reward);
        if (leftovers.isEmpty()) {
            logger.info("[REWARD] Item successfully added to inventory of " + player.getName());
        } else {
            logger.info("[REWARD] Item partially given to " + player.getName() + ", dropping " + leftovers.size() + " stacks");
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
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

    private double normalizePercentage(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    @FunctionalInterface
    private interface PendingReward {
        void apply(Player player);
    }
}
