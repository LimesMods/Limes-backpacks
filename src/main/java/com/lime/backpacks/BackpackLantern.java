package com.lime.backpacks;

import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.loader.api.FabricLoader;

public final class BackpackLantern {
    private static final String ENABLED_KEY = "limesbackpacks:lantern_enabled";
    private BackpackLantern() {}

    public static boolean hasLantern(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BackpackItem backpack
                && (backpack.getTier() == BackpackTier.DIAMOND || backpack.getTier() == BackpackTier.NETHERITE);
    }

    /** Existing and newly crafted lantern backpacks default to on. */
    public static boolean isEnabled(ItemStack stack) {
        if (!hasLantern(stack)) return false;
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null || data.copyNbt().getBoolean(ENABLED_KEY, true);
    }

    public static void toggle(ItemStack stack) {
        if (!hasLantern(stack)) return;
        boolean enabled = isEnabled(stack);
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = data == null ? new NbtCompound() : data.copyNbt();
        nbt.putBoolean(ENABLED_KEY, !enabled);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static ItemStack findEquipped(Inventory back) {
        if (back != null) {
            for (int i = 0; i < back.size(); i++) {
                if (hasLantern(back.getStack(i))) return back.getStack(i);
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack selectTarget(ItemStack equipped, ItemStack mainHand, ItemStack offHand) {
        if (hasLantern(equipped)) return equipped;
        if (hasLantern(mainHand)) return mainHand;
        return hasLantern(offHand) ? offHand : ItemStack.EMPTY;
    }

    /** Resolve ownership on the server; the client cannot name arbitrary items or players. */
    public static void toggleFor(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator()) return;
        Inventory back = FabricLoader.getInstance().isModLoaded("trinkets")
                ? TrinketsCompat.getBackInventory(player) : null;
        ItemStack equipped = findEquipped(back);
        ItemStack target = selectTarget(equipped, player.getMainHandStack(), player.getOffHandStack());
        if (target.isEmpty()) return;
        toggle(target);
        if (target == equipped && back != null) {
            back.markDirty();
            TrinketsCompat.syncQuiver(back);
        }
        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
        player.playerScreenHandler.sendContentUpdates();
    }
}
