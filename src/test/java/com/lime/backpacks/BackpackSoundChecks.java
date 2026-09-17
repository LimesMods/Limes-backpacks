package com.lime.backpacks;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

final class BackpackSoundChecks {
    static void run() {
        Object world = new Object();
        ItemStack bag = new ItemStack(ModItems.NETHERITE_BACKPACK);
        var empty = BackpackEquipSounds.snapshot(ItemStack.EMPTY, world);
        var equipped = BackpackEquipSounds.snapshot(bag, world);
        check(BackpackEquipSounds.change(null, equipped) == null, "Login must be silent");
        var added = BackpackEquipSounds.change(empty, equipped);
        check(added != null && added.equipping(), "Equip sound missing");
        BackpackLantern.toggle(bag);
        bag.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Renamed"));
        new BackpackInventory(bag, 81).setStack(0, new ItemStack(Items.ARROW, 12));
        check(BackpackEquipSounds.change(equipped, BackpackEquipSounds.snapshot(bag, world)) == null,
                "Contents, name or lantern updates replayed equip sound");
        var refreshed = BackpackEquipSounds.change(equipped,
                BackpackEquipSounds.snapshot(bag.copy(), world));
        check(refreshed == null, "Inventory refresh must not replay equip sound");
        var changedTier = BackpackEquipSounds.change(equipped,
                BackpackEquipSounds.snapshot(new ItemStack(ModItems.DIAMOND_BACKPACK), world));
        check(changedTier != null && changedTier.equipping(), "Tier swap must produce one equip event");
        var removed = BackpackEquipSounds.change(equipped, empty);
        check(removed != null && !removed.equipping() && removed.tier() == BackpackTier.NETHERITE,
                "Removal must retain original tier and produce release event");
        check(BackpackEquipSounds.change(equipped, BackpackEquipSounds.snapshot(bag, new Object())) == null,
                "Dimension transfer must be silent");
        check(BackpackEquipSounds.change(empty, empty) == null, "Empty slots must be silent");
        System.out.println("PASS: equip/remove sounds, single-event swaps, silent component updates and world changes.");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
