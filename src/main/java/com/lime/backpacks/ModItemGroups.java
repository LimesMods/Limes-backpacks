package com.lime.backpacks;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    public static final ResourceKey<CreativeModeTab> BACKPACKS = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("limesbackpacks", "backpacks")
    );

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, BACKPACKS,
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .icon(() -> new ItemStack(ModItems.NETHERITE_BACKPACK))
                        .title(Component.translatable("itemGroup.limesbackpacks.backpacks"))
                        .displayItems((context, entries) -> {
                            entries.accept(ModItems.LEATHER_BACKPACK);
                            entries.accept(ModItems.COPPER_BACKPACK);
                            entries.accept(ModItems.IRON_BACKPACK);
                            entries.accept(ModItems.GOLD_BACKPACK);
                            entries.accept(ModItems.DIAMOND_BACKPACK);
                            entries.accept(ModItems.NETHERITE_BACKPACK);
                        })
                        .build()
        );
    }
}
