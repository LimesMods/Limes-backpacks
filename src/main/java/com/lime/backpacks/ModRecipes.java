package com.lime.backpacks;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    public static RecipeSerializer<BackpackUpgradeRecipe> BACKPACK_UPGRADE;

    public static void register() {
        BACKPACK_UPGRADE = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath("limesbackpacks", "backpack_upgrade"),
                BackpackUpgradeRecipe.Serializer.INSTANCE
        );
    }
}
