package com.lime.backpacks;

import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.TrinketInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class TrinketsCompat {

    public static net.minecraft.world.Container getBackInventory(net.minecraft.world.entity.player.Player player) {
        var attachment = TrinketsApi.getAttachment(player);
        return attachment == null ? null : attachment.getInventory("chest/back");
    }

    public static boolean hasEquippedLitLantern(net.minecraft.world.entity.player.Player player) {
        var back = getBackInventory(player);
        if (back == null) return false;
        for (int i = 0; i < back.getContainerSize(); i++) {
            if (BackpackLantern.isEnabled(back.getItem(i))) return true;
        }
        return false;
    }

    public static QuiverAmmo.Source findQuiverAmmo(net.minecraft.world.entity.player.Player player,
                                                   java.util.function.Predicate<ItemStack> accepted) {
        var attachment = TrinketsApi.getAttachment(player);
        if (attachment == null) return null;
        var back = attachment.getInventory("chest/back");
        return back == null ? null : QuiverAmmo.findInInventory(back, accepted);
    }

    public static void syncQuiver(net.minecraft.world.Container inventory) {
        if (inventory instanceof TrinketInventory trinket) {
            trinket.setChanged();
        }
    }

    /** Equips one held backpack into the standard chest/back Trinkets slot. */
    public static boolean equipFromHand(ServerPlayer player, InteractionHand hand) {
        var attachment = TrinketsApi.getAttachment(player);
        if (attachment == null) return false;

        TrinketInventory back = attachment.getInventory("chest/back");
        if (back == null || back.getContainerSize() < 1) return false;

        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof BackpackItem) || !back.canPlaceItem(0, held)) return false;

        ItemStack previous = back.getItem(0).copy();
        ItemStack equipped = held.copy();
        equipped.setCount(1);
        back.setItem(0, equipped);
        // Mark the inventory dirty so the equipped stack is persisted and synced.
        back.setChanged();

        // Do not decrement the live hand stack after changing the Trinkets
        // inventory. Build the remainder first so the item-use transaction
        // cannot overwrite the swapped-in stack.
        ItemStack remainder = held.copy();
        remainder.shrink(1);
        if (!previous.isEmpty()) {
            player.setItemInHand(hand, previous);
            if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
                player.drop(remainder, false, Prediction.SERVER_ONLY);
            }
        } else {
            player.setItemInHand(hand, remainder);
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public static boolean openEquippedBackpack(ServerPlayer player) {
        var attachment = TrinketsApi.getAttachment(player);
        if (attachment == null) return false;

        var equipped = attachment.findFirst(stack -> stack.getItem() instanceof BackpackItem);
        if (equipped.isEmpty()) return false;

        ItemStack backpackStack = equipped.get().get();
        if (!(backpackStack.getItem() instanceof BackpackItem backpackItem)) return false;

        BackpackInventory inventory = new BackpackInventory(backpackStack, backpackItem.getTier().getSlotCount());
        player.openMenu(backpackItem.createScreenFactory(backpackStack, inventory));
        return true;
    }
}
