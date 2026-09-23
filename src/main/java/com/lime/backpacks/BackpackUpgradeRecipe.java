package com.lime.backpacks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import java.util.List;

public class BackpackUpgradeRecipe implements CraftingRecipe {
    private final ShapedRecipe delegate;

    public BackpackUpgradeRecipe(ShapedRecipe delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        if (!delegate.matches(input, world)) {
            return false;
        }
        // Only the backpack's contents carry over, so filled bundles or shulker boxes would lose their items.
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!(stack.getItem() instanceof BackpackItem) && hasStoredItems(stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasStoredItems(ItemStack stack) {
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null && container.nonEmptyItemCopyStream().findAny().isPresent()) {
            return true;
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        return bundle != null && !bundle.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = delegate.assemble(input);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof BackpackItem) {
                ItemContainerContents container = stack.get(DataComponents.CONTAINER);
                if (container != null) {
                    result.set(DataComponents.CONTAINER, container);
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
    public CraftingBookCategory category() {
        return delegate.category();
    }

    @Override
    public PlacementInfo placementInfo() {
        return delegate.placementInfo();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return delegate.recipeBookCategory();
    }

    @Override
    public String group() {
        return delegate.group();
    }

    @Override
    public boolean showNotification() {
        return delegate.showNotification();
    }

    @Override
    public List<RecipeDisplay> display() {
        return delegate.display();
    }

    public static class Serializer {
        private static final MapCodec<BackpackUpgradeRecipe> CODEC =
                ShapedRecipe.MAP_CODEC.xmap(
                        BackpackUpgradeRecipe::new,
                        r -> r.delegate
                );

        private static final StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> PACKET_CODEC =
                ShapedRecipe.STREAM_CODEC.map(
                        BackpackUpgradeRecipe::new,
                        r -> r.delegate
                );

        public static final RecipeSerializer<BackpackUpgradeRecipe> INSTANCE =
                new RecipeSerializer<>(CODEC, PACKET_CODEC);
    }
}
