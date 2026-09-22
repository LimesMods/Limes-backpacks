package com.lime.backpacks.mixin.client;

import com.lime.backpacks.client.IrisShaderCompat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gives lit placed backpacks the vanilla lantern block-light material before
 * Iris installs the shader pack's block-state table.
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
