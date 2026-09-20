package com.lime.backpacks.mixin.client;

import com.lime.backpacks.BackpackLantern;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional API target: never loaded when LambDynamicLights is absent. */
@Pseudo
@Mixin(targets = "dev.lambdaurora.lambdynlights.api.item.ItemLightSource", remap = false)
public abstract class LanternItemLightMixin {
    @Inject(method = "getLuminance", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void limesbackpacks$toggleItemLight(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (BackpackLantern.hasLantern(stack) && !BackpackLantern.isEnabled(stack)) cir.setReturnValue(0);
    }
}
