package com.lime.backpacks.client;

import com.lime.backpacks.BackpackBlock;
import com.lime.backpacks.BackpackBlockEntity;
import com.lime.backpacks.BackpackLantern;
import com.lime.backpacks.ModItems;
import com.lime.backpacks.QuiverAmmo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class BackpackBlockEntityRenderer
        implements BlockEntityRenderer<BackpackBlockEntity, BackpackBlockEntityRenderState> {
    private final ItemModelResolver itemModelManager;

    public BackpackBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelManager = context.itemModelResolver();
    }

    @Override
    public BackpackBlockEntityRenderState createRenderState() {
        return new BackpackBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(BackpackBlockEntity entity, BackpackBlockEntityRenderState state,
                                  float tickProgress, Vec3 cameraPos,
                                  ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.rotation = entity.getBlockState().getValue(BackpackBlock.ROTATION);
        state.itemRenderState.clear();
        state.lanternGlowRenderState.clear();
        if (entity.getLevel() != null && !entity.getBackpack().isEmpty()) {
            ItemStack backpack = entity.getBackpack();
            itemModelManager.updateForTopItem(state.itemRenderState, backpackRenderStack(backpack),
                    ItemDisplayContext.GROUND, entity.getLevel(), null, 0);

            Identifier glowModel = lanternGlowModel(backpack);
            if (glowModel != null) {
                ItemStack lantern = new ItemStack(Items.LANTERN);
                lantern.set(DataComponents.ITEM_MODEL, glowModel);
                itemModelManager.updateForTopItem(state.lanternGlowRenderState, lantern,
                        ItemDisplayContext.GROUND, entity.getLevel(), null, 0);
            }
        }
    }

    /**
     * Keep the backpack on its normal shader material. The lantern glow is
     * rendered separately below using a vanilla lantern item identity, so only
     * the glow receives Complementary's warm lantern material.
     */
    private static ItemStack backpackRenderStack(ItemStack backpack) {
        String model = null;
        if (backpack.is(ModItems.DIAMOND_BACKPACK)) {
            model = "diamond_backpack_unlit";
        } else if (backpack.is(ModItems.NETHERITE_BACKPACK)) {
            model = QuiverAmmo.hasStoredArrows(backpack)
                    ? "netherite_backpack_unlit"
                    : "netherite_backpack_empty_quiver_unlit";
        }

        if (model == null || !BackpackLantern.isEnabled(backpack)) return backpack;

        ItemStack renderStack = backpack.copy();
        renderStack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("limesbackpacks", model));
        return renderStack;
    }

    private static Identifier lanternGlowModel(ItemStack backpack) {
        if (!BackpackLantern.isEnabled(backpack)) return null;
        if (backpack.is(ModItems.DIAMOND_BACKPACK)) {
            return Identifier.fromNamespaceAndPath("limesbackpacks", "diamond_backpack_lantern_glow");
        }
        if (backpack.is(ModItems.NETHERITE_BACKPACK)) {
            return Identifier.fromNamespaceAndPath("limesbackpacks", "netherite_backpack_lantern_glow");
        }
        return null;
    }

    @Override
    public void submit(BackpackBlockEntityRenderState state, PoseStack matrices,
                       SubmitNodeCollector renderQueue, CameraRenderState cameraState) {
        if (state.itemRenderState.isEmpty()) return;

        matrices.pushPose();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.rotateDegrees(Axis.YP,
                com.lime.backpacks.BackpackPlacement.modelRotationDegrees(state.rotation));

        // Use the model's ground transform and enlarge it back to the same
        // natural scale as the backpack worn on a player. This keeps the pack
        // upright and above the floor instead of applying the torso transform.
        matrices.scale(2.0f, 2.0f, 2.0f);
        // These bounds include the item's ground translation and scale. Place
        // the lowest rendered vertex on y=0 and rotate around the model center.
        var bounds = state.itemRenderState.getModelBoundingBox();
        matrices.translate(-(bounds.minX + bounds.maxX) / 2.0, -bounds.minY,
                -(bounds.minZ + bounds.maxZ) / 2.0);

        state.itemRenderState.submit(matrices, renderQueue, state.lightCoords,
                OverlayTexture.NO_OVERLAY, 0);
        if (!state.lanternGlowRenderState.isEmpty()) {
            // Vanilla lanterns render their flame/glass at full brightness. Do
            // the same here so redstone and other shader-colored light sources
            // cannot tint the backpack's lantern overlay.
            state.lanternGlowRenderState.submit(matrices, renderQueue,
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY, 0);
        }
        matrices.popPose();
    }
}
