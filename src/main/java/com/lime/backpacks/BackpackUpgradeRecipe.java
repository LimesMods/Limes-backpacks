package com.lime.backpacks;

import com.mojang.serialization.MapCodec;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.List;

public class BackpackUpgradeRecipe implements CraftingRecipe {
    private final ShapedRecipe delegate;

    public BackpackUpgradeRecipe(ShapedRecipe delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return delegate.matches(input, world);
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        ItemStack result = delegate.craft(input, lookup);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.getItem() instanceof BackpackItem) {
                ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
                if (container != null) {
                    result.set(DataComponentTypes.CONTAINER, container);
                }
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<BackpackUpgradeRecipe> getSerializer() {
        return ModRecipes.BACKPACK_UPGRADE;
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return delegate.getCategory();
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return delegate.getIngredientPlacement();
    }

    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return delegate.getRecipeBookCategory();
    }

    @Override
    public String getGroup() {
        return delegate.getGroup();
    }

    @Override
    public boolean showNotification() {
        return delegate.showNotification();
    }

    @Override
    public List<RecipeDisplay> getDisplays() {
        return delegate.getDisplays();
    }

    public static class Serializer implements RecipeSerializer<BackpackUpgradeRecipe> {
        private static final MapCodec<BackpackUpgradeRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(
                        BackpackUpgradeRecipe::new,
                        r -> r.delegate
                );

        private static final PacketCodec<RegistryByteBuf, BackpackUpgradeRecipe> PACKET_CODEC =
                ShapedRecipe.Serializer.PACKET_CODEC.xmap(
                        BackpackUpgradeRecipe::new,
                        r -> r.delegate
                );

        @Override
        public MapCodec<BackpackUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, BackpackUpgradeRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
