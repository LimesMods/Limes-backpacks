package com.lime.backpacks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || data.copyTag().getBooleanOr(ENABLED_KEY, true);
    }

    public static void toggle(ItemStack stack) {
        if (!hasLantern(stack)) return;
        boolean enabled = isEnabled(stack);
        var data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag nbt = data == null ? new CompoundTag() : data.copyTag();
        nbt.putBoolean(ENABLED_KEY, !enabled);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public static ItemStack findEquipped(Container back) {
        if (back != null) {
            for (int i = 0; i < back.getContainerSize(); i++) {
                if (hasLantern(back.getItem(i))) return back.getItem(i);
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
    public static void toggleFor(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        Container back = FabricLoader.getInstance().isModLoaded("trinkets")
                ? TrinketsCompat.getBackInventory(player) : null;
        ItemStack equipped = findEquipped(back);
        ItemStack target = selectTarget(equipped, player.getMainHandItem(), player.getOffhandItem());
        if (target.isEmpty()) return;
        toggle(target);
        if (target == equipped && back != null) {
            back.setChanged();
            TrinketsCompat.syncQuiver(back);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }
}
