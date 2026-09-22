package com.lime.backpacks.mixin.client;

import com.lime.backpacks.client.IrisShaderCompat;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Installs both halves of the shader integration: vanilla lantern block-light
 * color for placed backpacks and vanilla lantern item material for the two
 * glow-only overlay models.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings", remap = false)
public abstract class IrisWorldRenderingSettingsMixin {
    @Inject(method = "setBlockStateIds", at = @At("HEAD"), require = 0, remap = false)
    private void limesbackpacks$addLanternBlockMaterial(
            Object2IntMap<BlockState> mapping, CallbackInfo ci) {
        IrisShaderCompat.addLanternMaterial(mapping);
    }

    @ModifyVariable(method = "setItemIds", at = @At("HEAD"), argsOnly = true,
            require = 1, remap = false)
    private Object2IntFunction<Object> limesbackpacks$addLanternOverlayMaterials(
            Object2IntFunction<Object> mapping) {
        return IrisShaderCompat.addLanternOverlayMaterials(mapping);
    }
}
