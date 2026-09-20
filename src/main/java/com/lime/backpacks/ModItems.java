package com.lime.backpacks;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

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
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("limesbackpacks", name));
        Item.Properties settings = new Item.Properties().setId(key).stacksTo(1);
        if (tier == BackpackTier.NETHERITE) settings.fireResistant();
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new BackpackItem(tier, settings)
        );
    }
}
