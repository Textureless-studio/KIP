package org.Textureless.kip.listeners.death;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.Textureless.kip.placeholders.PlaceholderParser;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class KeepItemRuleMatcher {

    private final ConditionExpressionEvaluator expressionEvaluator;
    private final PlainTextComponentSerializer plainTextSerializer;

    public KeepItemRuleMatcher() {
        this.expressionEvaluator = new ConditionExpressionEvaluator();
        this.plainTextSerializer = PlainTextComponentSerializer.plainText();
    }

    public boolean matchesAnyRule(Player player,
                                  ItemStack itemStack,
                                  ConfigurationSection keepItems,
                                  Random random,
                                  PlaceholderParser placeholderParser) {
        for (String key : keepItems.getKeys(false)) {
            ConfigurationSection rule = keepItems.getConfigurationSection(key);
            if (rule == null) {
                continue;
            }

            if (!matchesRule(player, itemStack, rule, placeholderParser)) {
                continue;
            }

            double percentage = normalizePercentage(rule.getDouble("percentage", 100.0));
            if (random.nextDouble(100.0) <= percentage) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesRule(Player player,
                                ItemStack itemStack,
                                ConfigurationSection rule,
                                PlaceholderParser placeholderParser) {
        ConfigurationSection conditionSection = rule.getConfigurationSection("condition");
        return matchesMaterial(itemStack, rule)
                && matchesModelData(itemStack, rule)
                && matchesEnchantments(itemStack, conditionSection == null ? null : conditionSection.getConfigurationSection("enchantments"))
                && matchesNameEquals(player, itemStack, conditionSection, placeholderParser)
                && matchesLoreContains(player, itemStack, conditionSection, placeholderParser)
                && matchesExpression(player, conditionSection, placeholderParser);
    }

    private boolean matchesMaterial(ItemStack itemStack, ConfigurationSection rule) {
        String materialName = rule.getString("material");
        if (materialName == null || materialName.isBlank()) {
            return false;
        }

        Material material = Material.matchMaterial(materialName);
        return material != null && itemStack.getType() == material;
    }

    private boolean matchesModelData(ItemStack itemStack, ConfigurationSection rule) {
        if (!rule.contains("model-data")) {
            return true;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        int currentModelData = 0;
        if (itemMeta != null && itemMeta.hasCustomModelDataComponent()) {
            CustomModelDataComponent component = itemMeta.getCustomModelDataComponent();
            List<Float> values = component.getFloats();
            if (!values.isEmpty()) {
                currentModelData = Math.round(values.getFirst());
            }
        }

        return currentModelData == rule.getInt("model-data");
    }

    private boolean matchesEnchantments(ItemStack itemStack, ConfigurationSection enchantmentsConfig) {
        if (enchantmentsConfig == null) {
            return true;
        }

        for (String key : enchantmentsConfig.getKeys(false)) {
            ConfigurationSection enchantmentRule = enchantmentsConfig.getConfigurationSection(key);
            if (enchantmentRule == null) {
                return false;
            }

            String enchantmentType = enchantmentRule.getString("type", "");
            Enchantment enchantment = resolveEnchantment(enchantmentType);
            if (enchantment == null) {
                return false;
            }

            int requiredLevel = enchantmentRule.getInt("level", 1);
            int currentLevel = itemStack.getEnchantmentLevel(enchantment);
            if (currentLevel < requiredLevel) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesNameEquals(Player player,
                                      ItemStack itemStack,
                                      ConfigurationSection conditionSection,
                                      PlaceholderParser placeholderParser) {
        if (conditionSection == null || !conditionSection.contains("nameEquals")) {
            return true;
        }

        String expectedName = placeholderParser.parse(player, conditionSection.getString("nameEquals", ""));
        if (expectedName.isBlank()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return false;
        }

        String currentName = plainTextSerializer.serialize(itemMeta.displayName());
        return currentName.equals(expectedName);
    }

    private boolean matchesLoreContains(Player player,
                                        ItemStack itemStack,
                                        ConfigurationSection conditionSection,
                                        PlaceholderParser placeholderParser) {
        if (conditionSection == null || !conditionSection.contains("loreContains")) {
            return true;
        }

        List<String> expectedLoreLines = conditionSection.isList("loreContains")
                ? conditionSection.getStringList("loreContains")
                : List.of(conditionSection.getString("loreContains", ""));

        List<String> parsedExpected = placeholderParser.parse(player, expectedLoreLines);
        parsedExpected.removeIf(String::isBlank);
        if (parsedExpected.isEmpty()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || itemMeta.lore() == null || itemMeta.lore().isEmpty()) {
            return false;
        }

        List<String> currentLore = itemMeta.lore().stream()
                .map(plainTextSerializer::serialize)
                .toList();

        for (String expectedLine : parsedExpected) {
            if (!currentLore.contains(expectedLine)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesExpression(Player player,
                                      ConfigurationSection conditionSection,
                                      PlaceholderParser placeholderParser) {
        if (conditionSection == null || !conditionSection.contains("expression")) {
            return true;
        }

        String expression = placeholderParser.parse(player, conditionSection.getString("expression", ""));
        return expressionEvaluator.evaluate(expression);
    }

    private Enchantment resolveEnchantment(String enchantmentType) {
        if (enchantmentType == null || enchantmentType.isBlank()) {
            return null;
        }

        var enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        NamespacedKey key = resolveEnchantmentKey(enchantmentType);
        Enchantment enchantment = enchantmentRegistry.get(key);
        if (enchantment != null) {
            return enchantment;
        }


        return enchantmentRegistry.get(NamespacedKey.minecraft(enchantmentType.toLowerCase(Locale.ROOT)));
    }

    private NamespacedKey resolveEnchantmentKey(String enchantmentType) {
        String normalized = enchantmentType.toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(normalized);
        if (key != null) {
            return key;
        }

        return NamespacedKey.minecraft(normalized);
    }

    private double normalizePercentage(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
