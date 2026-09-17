package com.lime.backpacks.client;

import com.lime.backpacks.BackpackBlock;
import com.lime.backpacks.BackpackBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class BackpackBlockEntityRenderer
        implements BlockEntityRenderer<BackpackBlockEntity, BackpackBlockEntityRenderState> {
    private final ItemModelManager itemModelManager;

    public BackpackBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemModelManager = context.itemModelManager();
    }

    @Override
    public BackpackBlockEntityRenderState createRenderState() {
        return new BackpackBlockEntityRenderState();
    }

    @Override
    public void updateRenderState(BackpackBlockEntity entity, BackpackBlockEntityRenderState state,
                                  float tickProgress, Vec3d cameraPos,
                                  ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlay);
        state.rotation = entity.getCachedState().get(BackpackBlock.ROTATION);
        state.itemRenderState.clear();
        if (entity.getWorld() != null && !entity.getBackpack().isEmpty()) {
            itemModelManager.clearAndUpdate(state.itemRenderState, entity.getBackpack(),
                    ItemDisplayContext.GROUND, entity.getWorld(), null, 0);
        }
    }

    @Override
    public void render(BackpackBlockEntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue renderQueue, CameraRenderState cameraState) {
        if (state.itemRenderState.isEmpty()) return;

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                com.lime.backpacks.BackpackPlacement.modelRotationDegrees(state.rotation)));

        // Use the model's ground transform and enlarge it back to the same
        // natural scale as the backpack worn on a player. This keeps the pack
        // upright and above the floor instead of applying the torso transform.
        matrices.scale(2.0f, 2.0f, 2.0f);
        // These bounds include the item's ground translation and scale. Place
        // the lowest rendered vertex on y=0 and rotate around the model center.
        var bounds = state.itemRenderState.getModelBoundingBox();
        matrices.translate(-(bounds.minX + bounds.maxX) / 2.0, -bounds.minY,
                -(bounds.minZ + bounds.maxZ) / 2.0);

        state.itemRenderState.render(matrices, renderQueue, state.lightmapCoordinates,
                OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();
    }
}
