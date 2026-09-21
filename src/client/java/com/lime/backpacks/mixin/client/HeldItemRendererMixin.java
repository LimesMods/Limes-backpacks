package com.lime.backpacks.mixin.client;

import com.lime.backpacks.BackpackLantern;
import com.lime.backpacks.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the backpack lantern panes after the normal held item has rendered.
 * The backpack itself is switched to its unlit model by QuiverItemModelMixin,
 * so Complementary cannot apply the lantern material to the whole backpack.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique
    private final ItemStackRenderState limesbackpacks$lanternGlow = new ItemStackRenderState();

    @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void limesbackpacks$renderLanternGlow(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState handsState,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector renderQueue,
            int light,
            CallbackInfo ci) {
        if (!BackpackLantern.hasLantern(stack) || !BackpackLantern.isEnabled(stack)) return;

        Identifier model = lanternModel(stack);
        if (model == null) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? client.player.getMainArm() : client.player.getMainArm().getOpposite();
        ItemDisplayContext display = arm == HumanoidArm.RIGHT
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        ItemStack lantern = new ItemStack(Items.LANTERN);
        lantern.set(DataComponents.ITEM_MODEL, model);
        limesbackpacks$lanternGlow.clear();
        client.getItemModelResolver().updateForLiving(
                limesbackpacks$lanternGlow, lantern, display, client.player);
        limesbackpacks$lanternGlow.submit(
                matrices, renderQueue,
                0xF000F0,
                OverlayTexture.NO_OVERLAY, 0);
    }

    @Unique
    private static Identifier lanternModel(ItemStack stack) {
        if (stack.is(ModItems.DIAMOND_BACKPACK)) {
            return Identifier.fromNamespaceAndPath("limesbackpacks", "diamond_backpack_lantern_glow");
        }
        if (stack.is(ModItems.NETHERITE_BACKPACK)) {
            return Identifier.fromNamespaceAndPath("limesbackpacks", "netherite_backpack_lantern_glow");
        }
        return null;
    }

}
