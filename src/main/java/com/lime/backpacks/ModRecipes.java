package com.lime.backpacks;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {
    public static RecipeSerializer<BackpackUpgradeRecipe> BACKPACK_UPGRADE;

    public static void register() {
        BACKPACK_UPGRADE = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of("limesbackpacks", "backpack_upgrade"),
                new BackpackUpgradeRecipe.Serializer()
        );
    }
}
