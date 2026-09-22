package com.lime.backpacks.client;

import com.lime.backpacks.BackpackItem;
import com.lime.backpacks.BackpackLantern;
import com.lime.backpacks.BackpackTier;
import com.lime.backpacks.ModItems;
import com.lime.backpacks.QuiverAmmo;
import com.lime.backpacks.QuiverUser;
import com.lime.backpacks.client.lighting.BackpackDynamicLights;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class BackpackRenderer implements TrinketRenderer {
    // Keep wearable rendering at its established scale. ItemDisplayContext.FIXED
    // is reserved for frames, where the backpack intentionally uses the larger
    // frame transform.
    private static final float WORN_MODEL_SCALE = 0.75f;
    private final ItemRenderState itemRenderState = new ItemRenderState();
    private final ItemRenderState lanternGlowRenderState = new ItemRenderState();

    @Override
    public void render(ItemStack stack, SlotReference slotReference,
                       EntityModel<? extends LivingEntityRenderState> contextModel,
                       MatrixStack matrices, OrderedRenderCommandQueue renderQueue, int light,
                       LivingEntityRenderState renderState, float limbAngle, float limbDistance) {

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        var wearer = slotReference.inventory().getComponent().getEntity();
        PlayerEntity player = wearer instanceof PlayerEntity p ? p : null;
        PlayerEntity localPlayer = mc.player;

        // Render the mesh without applying a display transform. This prevents
        // the item-frame-only fixed scale/centering from changing the worn size
        // or moving the backpack on the player's back.
        lanternGlowRenderState.clear();
        mc.getItemModelManager().clearAndUpdate(
                itemRenderState,
                shaderSafeBackpackStack(stack, player, localPlayer),
                ItemDisplayContext.NONE,
                mc.world,
                wearer,
                0
        );

        Identifier glowModel = lanternGlowModel(stack);
        if (glowModel != null) {
            ItemStack lantern = new ItemStack(Items.LANTERN);
            lantern.set(DataComponentTypes.ITEM_MODEL, glowModel);
            mc.getItemModelManager().clearAndUpdate(
                    lanternGlowRenderState,
                    lantern,
                    ItemDisplayContext.NONE,
                    mc.world,
                    wearer,
                    0
            );
        }

        if (itemRenderState.isEmpty()) return;

        matrices.push();

        // Trinkets supplies the entity transform (including swimming/flight).
        // Follow the same root -> body hierarchy as the torso mesh, including
        // animated origins, rotations and scale from vanilla or Fresh Moves.
        if (contextModel instanceof BipedEntityModel<?> biped) {
            biped.getRootPart().applyTransform(matrices);
            biped.body.applyTransform(matrices);
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
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
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
            matrices.peek().getPositionMatrix().transformPosition(center);
            Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
            BackpackDynamicLights.captureAnchor(player, camera.add(center.x, center.y, center.z));
        }
        itemRenderState.render(matrices, renderQueue, light, OverlayTexture.DEFAULT_UV, 0);
        if (!lanternGlowRenderState.isEmpty()) {
            lanternGlowRenderState.render(matrices, renderQueue,
                    net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    OverlayTexture.DEFAULT_UV, 0);
        }

        matrices.pop();
    }

    private static ItemStack shaderSafeBackpackStack(ItemStack stack, PlayerEntity wearer,
                                                     PlayerEntity localPlayer) {
        String model = null;
        if (stack.isOf(ModItems.DIAMOND_BACKPACK)) {
            model = "diamond_backpack_unlit";
        } else if (stack.isOf(ModItems.NETHERITE_BACKPACK)) {
            boolean arrows = QuiverAmmo.hasStoredArrows(stack)
                    || (wearer != null && QuiverAmmo.hasAvailableArrows(wearer))
                    || (wearer instanceof QuiverUser user && user.limesbackpacks$hasArrows())
                    // The trinket owner can be a render-side entity whose
                    // inventory is not current. The local client player is
                    // authoritative for the backpack worn by the viewer.
                    || (localPlayer != null && QuiverAmmo.hasAvailableArrows(localPlayer))
                    || (localPlayer instanceof QuiverUser user && user.limesbackpacks$hasArrows());
            model = arrows
                    ? "netherite_backpack_unlit"
                    : "netherite_backpack_empty_quiver_unlit";
        }
        if (model == null || !BackpackLantern.isEnabled(stack)) return stack;

        ItemStack renderStack = stack.copy();
        renderStack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("limesbackpacks", model));
        return renderStack;
    }

    private static Identifier lanternGlowModel(ItemStack stack) {
        if (!BackpackLantern.isEnabled(stack)) return null;
        if (stack.isOf(ModItems.DIAMOND_BACKPACK)) {
            return Identifier.of("limesbackpacks", "diamond_backpack_lantern_glow");
        }
        if (stack.isOf(ModItems.NETHERITE_BACKPACK)) {
            return Identifier.of("limesbackpacks", "netherite_backpack_lantern_glow");
        }
        return null;
    }
}
