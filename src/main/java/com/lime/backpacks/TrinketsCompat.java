package com.lime.backpacks;

import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketInventory;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public class TrinketsCompat {

    public static net.minecraft.inventory.Inventory getBackInventory(net.minecraft.entity.player.PlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player);
        return component.isEmpty() ? null
                : component.get().getInventory().getOrDefault("chest", Map.of()).get("back");
    }

    public static boolean hasEquippedLitLantern(net.minecraft.entity.player.PlayerEntity player) {
        var back = getBackInventory(player);
        if (back == null) return false;
        for (int i = 0; i < back.size(); i++) {
            if (BackpackLantern.isEnabled(back.getStack(i))) return true;
        }
        return false;
    }

    public static QuiverAmmo.Source findQuiverAmmo(net.minecraft.entity.player.PlayerEntity player,
                                                   java.util.function.Predicate<ItemStack> accepted) {
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) return null;
        var back = component.get().getInventory().getOrDefault("chest", Map.of()).get("back");
        return back == null ? null : QuiverAmmo.findInInventory(back, accepted);
    }

    public static void syncQuiver(net.minecraft.inventory.Inventory inventory) {
        if (inventory instanceof TrinketInventory trinket) {
            trinket.markUpdate();
            trinket.update();
            trinket.getComponent().update();
        }
    }

    /** Equips one held backpack into the standard chest/back Trinkets slot. */
    public static boolean equipFromHand(ServerPlayerEntity player, Hand hand) {
        var componentOptional = TrinketsApi.getTrinketComponent(player);
        if (componentOptional.isEmpty()) return false;

        Map<String, Map<String, TrinketInventory>> groups = componentOptional.get().getInventory();
        TrinketInventory back = groups.getOrDefault("chest", Map.of()).get("back");
        if (back == null || back.size() < 1) return false;

        ItemStack held = player.getStackInHand(hand);
        if (!(held.getItem() instanceof BackpackItem) || !back.isValid(0, held)) return false;

        ItemStack previous = back.getStack(0).copy();
        ItemStack equipped = held.copy();
        equipped.setCount(1);
        back.setStack(0, equipped);
        // TrinketInventory.setStack writes the value but does not mark the slot
        // dirty afterward. Marking and applying the update prevents the next
        // client/save sync from restoring the old empty slot.
        back.markUpdate();
        back.update();

        // Do not decrement the live hand stack after changing the Trinkets
        // inventory. Build the remainder first so the item-use transaction
        // cannot overwrite the swapped-in stack.
        ItemStack remainder = held.copy();
        remainder.decrement(1);
        if (!previous.isEmpty()) {
            player.setStackInHand(hand, previous);
            if (!remainder.isEmpty() && !player.getInventory().insertStack(remainder)) {
                player.dropItem(remainder, false);
            }
        } else {
            player.setStackInHand(hand, remainder);
        }

        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
        player.playerScreenHandler.sendContentUpdates();
        componentOptional.get().update();
        return true;
    }

    public static boolean openEquippedBackpack(ServerPlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) return false;

        var equipped = component.get().getEquipped(stack -> stack.getItem() instanceof BackpackItem);
        if (equipped.isEmpty()) return false;

        var ref = equipped.get(0).getLeft();
        ItemStack backpackStack = ref.inventory().getStack(ref.index());
        if (!(backpackStack.getItem() instanceof BackpackItem backpackItem)) return false;

        BackpackInventory inventory = new BackpackInventory(backpackStack, backpackItem.getTier().getSlotCount());
        player.openHandledScreen(backpackItem.createScreenFactory(backpackStack, inventory));
        return true;
    }
}
