package com.lime.backpacks.mixin.client;

import com.lime.backpacks.client.IrisShaderCompat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the backpack's lit block states at the point Iris stores the
 * material map used while rendering block entities. This is intentionally an
 * optional Iris mixin; the mod remains fully functional without Iris.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings", remap = false)
public abstract class IrisWorldRenderingSettingsMixin {
    @Inject(method = "setBlockStateIds", at = @At("HEAD"), require = 0, remap = false)
    private void limesbackpacks$addLanternMaterial(Object2IntMap<BlockState> mapping, CallbackInfo ci) {
        IrisShaderCompat.addLanternMaterial(mapping);
    }
}
