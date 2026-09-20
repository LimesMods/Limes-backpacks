package com.lime.backpacks.client;

import com.lime.backpacks.BackpackItem;
import com.lime.backpacks.BackpackLantern;
import com.lime.backpacks.BackpackTier;
import com.lime.backpacks.ModItems;
import com.lime.backpacks.QuiverAmmo;
import com.lime.backpacks.QuiverUser;
import com.lime.backpacks.client.lighting.BackpackDynamicLights;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;

public class BackpackRenderer implements TrinketRenderer {
    // Keep wearable rendering at its established scale. ItemDisplayContext.FIXED
    // is reserved for frames, where the backpack intentionally uses the larger
    // frame transform.
    private static final float WORN_MODEL_SCALE = 0.75f;
    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    private final ItemStackRenderState lanternGlowRenderState = new ItemStackRenderState();

    @Override
    public void submit(ItemStack stack, TrinketSlotAccess slotReference,
                       EntityModel<? extends LivingEntityRenderState> contextModel,
                       PoseStack matrices, SubmitNodeCollector renderQueue, int light,
                       LivingEntityRenderState renderState, float limbAngle, float limbDistance) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var wearer = slotReference.inventory().getAttachment().getEntity();
        Player player = wearer instanceof Player p ? p : null;

        // Render the mesh without applying a display transform. This prevents
        // the item-frame-only fixed scale/centering from changing the worn size
        // or moving the backpack on the player's back.
        lanternGlowRenderState.clear();
        mc.getItemModelResolver().updateForTopItem(
                itemRenderState,
                shaderSafeBackpackStack(stack, player),
                ItemDisplayContext.NONE,
                mc.level,
                wearer,
                0
        );

        Identifier glowModel = lanternGlowModel(stack);
        if (glowModel != null) {
            ItemStack lantern = new ItemStack(Items.LANTERN);
            lantern.set(DataComponents.ITEM_MODEL, glowModel);
            mc.getItemModelResolver().updateForTopItem(
                    lanternGlowRenderState,
                    lantern,
                    ItemDisplayContext.NONE,
                    mc.level,
                    slotReference.inventory().getAttachment().getEntity(),
                    0
            );
        }

        if (itemRenderState.isEmpty()) return;

        matrices.pushPose();

        // Trinkets supplies the entity transform (including swimming/flight).
        // Follow the same root -> body hierarchy as the torso mesh, including
        // animated origins, rotations and scale from vanilla or Fresh Moves.
        if (contextModel instanceof HumanoidModel<?> biped) {
            biped.root().translateAndRotate(matrices);
            biped.body.translateAndRotate(matrices);
        }

        // Raise the three upper tiers by one player-model pixel toward the
        // shoulders. Apply before item scaling so the lift stays exactly 1/16.
        if (stack.getItem() instanceof BackpackItem backpack) {
            float lift = switch (backpack.getTier()) {
                case GOLD, DIAMOND, NETHERITE -> 1.0f / 16.0f;
                default -> 0.0f;
            };
            matrices.translate(0, -lift, 0);
        }

        // One attachment offset in torso-local coordinates for every pose.
        // Preserve the accepted standing alignment and item-model orientation.
        matrices.mulPose(Axis.XP.rotationDegrees(180));
        matrices.translate(0, -0.5, -0.25);

        // Clearance from the back now follows the torso in every pose.
        matrices.translate(0, 0, -0.04);

        // Grow around the shoulder/back contact point in FIXED display space
        // (model coordinates 8,16,12.25 at the existing 0.75 item scale).
        // Keeping this pivot fixed lets the pack grow outward and downward.
        matrices.translate(0, 0.375, 0.19921875);
        // Uniform 15% increase over 1.176, including every attachment.
        matrices.scale(1.3524f, 1.3524f, 1.3524f);
        matrices.translate(0, -0.375, -0.19921875);
        matrices.scale(WORN_MODEL_SCALE, WORN_MODEL_SCALE, WORN_MODEL_SCALE);
        if (BackpackLantern.isEnabled(stack) && FabricLoader.getInstance().isModLoaded("lambdynlights")
                && player != null) {
            // Centers of vanilla_lantern_0 after lengthening the pack. The torso
            // matrix already includes WORN_MODEL_SCALE, just as for the mesh.
            boolean netherite = ((BackpackItem) stack.getItem()).getTier() == BackpackTier.NETHERITE;
            Vector3f center = new Vector3f(netherite ? 13.8f : 13.58f,
                    netherite ? 8.632f : 9.0f, netherite ? 5.949f : 6.031f);
            center.div(16).sub(.5f, .5f, .5f);
            matrices.last().pose().transformPosition(center);
            Vec3 camera = mc.gameRenderer.mainCamera().position();
            BackpackDynamicLights.captureAnchor(player, camera.add(center.x, center.y, center.z));
        }
        itemRenderState.submit(matrices, renderQueue, light, OverlayTexture.NO_OVERLAY, 0);
        if (!lanternGlowRenderState.isEmpty()) {
            lanternGlowRenderState.submit(matrices, renderQueue,
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY, 0);
        }

        matrices.popPose();
    }

    private static ItemStack shaderSafeBackpackStack(ItemStack stack, Player wearer) {
        String model = null;
        if (stack.is(ModItems.DIAMOND_BACKPACK)) {
            model = "diamond_backpack_unlit";
        } else if (stack.is(ModItems.NETHERITE_BACKPACK)) {
            boolean arrows = QuiverAmmo.hasStoredArrows(stack)
                    || (wearer != null && QuiverAmmo.hasAvailableArrows(wearer))
                    || (wearer instanceof QuiverUser user && user.limesbackpacks$hasArrows());
            model = arrows
                    ? "netherite_backpack_unlit"
                    : "netherite_backpack_empty_quiver_unlit";
        }
        if (model == null || !BackpackLantern.isEnabled(stack)) return stack;

        ItemStack renderStack = stack.copy();
        renderStack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("limesbackpacks", model));
        return renderStack;
    }

    private static Identifier lanternGlowModel(ItemStack stack) {
        if (!BackpackLantern.isEnabled(stack)) return null;
        if (stack.is(ModItems.DIAMOND_BACKPACK)) {
            return Identifier.fromNamespaceAndPath("limesbackpacks", "diamond_backpack_lantern_glow");
        }
        if (stack.is(ModItems.NETHERITE_BACKPACK)) {
            return Identifier.fromNamespaceAndPath("limesbackpacks", "netherite_backpack_lantern_glow");
        }
        return null;
    }
}
