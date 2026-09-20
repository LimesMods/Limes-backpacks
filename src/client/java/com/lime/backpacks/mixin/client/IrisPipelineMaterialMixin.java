package com.lime.backpacks.mixin.client;

import com.lime.backpacks.client.IrisShaderCompat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Adds the vanilla lantern material to lit backpack block states before Iris
 * builds its shader material tables. Iris remains an optional dependency.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public abstract class IrisPipelineMaterialMixin {
    @ModifyArg(
            method = "beginLevelRendering",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/materialmap/WorldRenderingSettings;setBlockStateIds(Lit/unimi/dsi/fastutil/objects/Object2IntMap;)V"
            ),
            index = 0,
            require = 0,
            remap = false
    )
    private Object2IntMap<BlockState> limesbackpacks$addLanternMaterial(
            Object2IntMap<BlockState> mapping) {
        return IrisShaderCompat.addLanternMaterial(mapping);
    }
}
