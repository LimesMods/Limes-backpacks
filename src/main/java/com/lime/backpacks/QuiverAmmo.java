package com.lime.backpacks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.function.Predicate;

public final class QuiverAmmo {
    private QuiverAmmo() {}

    public record Source(Container owner, int bagSlot, ItemStack backpack, int arrowSlot, ItemStack projectile) {}

    public static boolean isQuiver(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem item && item.getTier() == BackpackTier.NETHERITE;
    }

    public static boolean hasStoredArrows(ItemStack stack) {
        if (!isQuiver(stack)) return false;
        var contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents.nonEmptyItemCopyStream().anyMatch(s -> s.is(ItemTags.ARROWS));
    }

    public static Source findInInventory(Container inventory, Predicate<ItemStack> accepted) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack bag = inventory.getItem(slot);
            if (!isQuiver(bag)) continue;
            var contents = bag.get(DataComponents.CONTAINER);
            if (contents == null) continue;
            var stacks = NonNullList.withSize(BackpackTier.NETHERITE.getSlotCount(), ItemStack.EMPTY);
            contents.copyInto(stacks);
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack arrow = stacks.get(i);
                if (!arrow.isEmpty() && arrow.is(ItemTags.ARROWS) && accepted.test(arrow))
                    return new Source(inventory, slot, bag, i, arrow.copy());
            }
        }
        return null;
    }

    public static Source find(Player player, Predicate<ItemStack> accepted) {
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            Source equipped = TrinketsCompat.findQuiverAmmo(player, accepted);
            if (equipped != null) return equipped;
        }
        return findInInventory(player.getInventory(), accepted);
    }

    public static boolean hasAvailableArrows(Player player) {
        // The off-hand slot is separate from PlayerInventory's main container.
        // Include it so a quiver visibly fills when arrows are held in the
        // other hand while the backpack is being rendered.
        if (player.getOffhandItem().is(ItemTags.ARROWS)) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.ARROWS)) return true;
        }
        return find(player, s -> true) != null;
    }

    /** Commit a shot to the live container component, never to a detached copy only. */
    public static ItemStack consume(Source source, int count) {
        if (count <= 0 || source.owner().getItem(source.bagSlot()) != source.backpack()) return ItemStack.EMPTY;
        var component = source.backpack().get(DataComponents.CONTAINER);
        if (component == null) return ItemStack.EMPTY;
        var stacks = NonNullList.withSize(BackpackTier.NETHERITE.getSlotCount(), ItemStack.EMPTY);
        component.copyInto(stacks);
        ItemStack live = stacks.get(source.arrowSlot());
        if (live.getCount() < count || !ItemStack.isSameItemSameComponents(live, source.projectile())) return ItemStack.EMPTY;
        ItemStack fired = live.split(count);
        source.backpack().set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
        source.owner().setChanged();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) TrinketsCompat.syncQuiver(source.owner());
        source.projectile().shrink(count);
        return fired;
    }
}
