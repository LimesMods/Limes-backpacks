package com.lime.backpacks.client;

import com.lime.backpacks.BackpackLantern;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Selection for Iris's two camera-player light channels; never changes inventory stacks. */
public final class CarriedLanternLight {
    private CarriedLanternLight() {}

    public static boolean useLantern(InteractionHand channel, ItemStack main, ItemStack off,
                                     boolean wornLantern, int existingBrightness) {
        if (BackpackLantern.isEnabled(channel == InteractionHand.MAIN_HAND ? main : off)) return true;
        // Use the secondary channel for the back slot. Keep a full-strength held
        // light and avoid adding the worn source twice when already holding a lit bag.
        return channel == InteractionHand.OFF_HAND && wornLantern && existingBrightness < 15
                && !BackpackLantern.isEnabled(main);
    }
}
