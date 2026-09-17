package com.lime.backpacks;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    public static Item LEATHER_BACKPACK;
    public static Item COPPER_BACKPACK;
    public static Item IRON_BACKPACK;
    public static Item GOLD_BACKPACK;
    public static Item DIAMOND_BACKPACK;
    public static Item NETHERITE_BACKPACK;

    public static void register() {
        LEATHER_BACKPACK = registerBackpack("leather_backpack", BackpackTier.LEATHER);
        COPPER_BACKPACK = registerBackpack("copper_backpack", BackpackTier.COPPER);
        IRON_BACKPACK = registerBackpack("iron_backpack", BackpackTier.IRON);
        GOLD_BACKPACK = registerBackpack("gold_backpack", BackpackTier.GOLD);
        DIAMOND_BACKPACK = registerBackpack("diamond_backpack", BackpackTier.DIAMOND);
        NETHERITE_BACKPACK = registerBackpack("netherite_backpack", BackpackTier.NETHERITE);
    }

    private static Item registerBackpack(String name, BackpackTier tier) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("limesbackpacks", name));
        Item.Settings settings = new Item.Settings().registryKey(key).maxCount(1);
        if (tier == BackpackTier.NETHERITE) settings.fireproof();
        return Registry.register(
                Registries.ITEM,
                key,
                new BackpackItem(tier, settings)
        );
    }
}
