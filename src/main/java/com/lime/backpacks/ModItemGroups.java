package com.lime.backpacks;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final RegistryKey<ItemGroup> BACKPACKS = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of("limesbackpacks", "backpacks")
    );

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, BACKPACKS,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ModItems.NETHERITE_BACKPACK))
                        .displayName(Text.translatable("itemGroup.limesbackpacks.backpacks"))
                        .entries((context, entries) -> {
                            entries.add(ModItems.LEATHER_BACKPACK);
                            entries.add(ModItems.COPPER_BACKPACK);
                            entries.add(ModItems.IRON_BACKPACK);
                            entries.add(ModItems.GOLD_BACKPACK);
                            entries.add(ModItems.DIAMOND_BACKPACK);
                            entries.add(ModItems.NETHERITE_BACKPACK);
                        })
                        .build()
        );
    }
}
