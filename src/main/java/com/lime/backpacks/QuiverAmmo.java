package com.lime.backpacks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.collection.DefaultedList;
import java.util.function.Predicate;

public final class QuiverAmmo {
    private QuiverAmmo() {}

    public record Source(Inventory owner, int bagSlot, ItemStack backpack, int arrowSlot, ItemStack projectile) {}

    public static boolean isQuiver(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem item && item.getTier() == BackpackTier.NETHERITE;
    }

    public static boolean hasStoredArrows(ItemStack stack) {
        if (!isQuiver(stack)) return false;
        var contents = stack.get(DataComponentTypes.CONTAINER);
        return contents != null && contents.stream().anyMatch(s -> !s.isEmpty() && s.isIn(ItemTags.ARROWS));
    }

    public static Source findInInventory(Inventory inventory, Predicate<ItemStack> accepted) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack bag = inventory.getStack(slot);
            if (!isQuiver(bag)) continue;
            var contents = bag.get(DataComponentTypes.CONTAINER);
            if (contents == null) continue;
            var stacks = DefaultedList.ofSize(BackpackTier.NETHERITE.getSlotCount(), ItemStack.EMPTY);
            contents.copyTo(stacks);
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack arrow = stacks.get(i);
                if (!arrow.isEmpty() && arrow.isIn(ItemTags.ARROWS) && accepted.test(arrow))
                    return new Source(inventory, slot, bag, i, arrow.copy());
            }
        }
        return null;
    }

    public static Source find(PlayerEntity player, Predicate<ItemStack> accepted) {
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            Source equipped = TrinketsCompat.findQuiverAmmo(player, accepted);
            if (equipped != null) return equipped;
        }
        return findInInventory(player.getInventory(), accepted);
    }

    public static boolean hasAvailableArrows(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isIn(ItemTags.ARROWS)) return true;
        }
        return find(player, s -> true) != null;
    }

    /** Commit a shot to the live container component, never to a detached copy only. */
    public static ItemStack consume(Source source, int count) {
        if (count <= 0 || source.owner().getStack(source.bagSlot()) != source.backpack()) return ItemStack.EMPTY;
        var component = source.backpack().get(DataComponentTypes.CONTAINER);
        if (component == null) return ItemStack.EMPTY;
        var stacks = DefaultedList.ofSize(BackpackTier.NETHERITE.getSlotCount(), ItemStack.EMPTY);
        component.copyTo(stacks);
        ItemStack live = stacks.get(source.arrowSlot());
        if (live.getCount() < count || !ItemStack.areItemsAndComponentsEqual(live, source.projectile())) return ItemStack.EMPTY;
        ItemStack fired = live.split(count);
        source.backpack().set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
        source.owner().markDirty();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) TrinketsCompat.syncQuiver(source.owner());
        source.projectile().decrement(count);
        return fired;
    }
}
