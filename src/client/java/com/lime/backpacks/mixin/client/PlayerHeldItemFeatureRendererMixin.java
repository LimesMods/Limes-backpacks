package com.lime.backpacks.mixin.client;

import com.lime.backpacks.BackpackLantern;
import com.lime.backpacks.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Renders the separate lantern panes on a backpack held in third person. */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class PlayerHeldItemFeatureRendererMixin {
    @Unique
    private final ItemRenderState limesbackpacks$lanternGlow = new ItemRenderState();

    @Inject(
            method = "renderItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V",
                    shift = At.Shift.AFTER),
            require = 1
    )
    private void limesbackpacks$renderLanternGlow(
            ArmedEntityRenderState renderState,
            ItemRenderState itemRenderState,
            ItemStack stack,
            net.minecraft.util.Arm arm,
            MatrixStack matrices,
            OrderedRenderCommandQueue renderQueue,
            int light,
            CallbackInfo ci) {
        if (!BackpackLantern.hasLantern(stack) || !BackpackLantern.isEnabled(stack)) return;

        Identifier model = lanternModel(stack);
        if (model == null) return;

        ItemStack lantern = new ItemStack(Items.LANTERN);
        lantern.set(DataComponentTypes.ITEM_MODEL, model);
        limesbackpacks$lanternGlow.clear();
        MinecraftClient.getInstance().getItemModelManager().clearAndUpdate(
                limesbackpacks$lanternGlow,
                lantern,
                arm == net.minecraft.util.Arm.RIGHT
                        ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                        : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                MinecraftClient.getInstance().world,
                null,
                0
        );
        limesbackpacks$lanternGlow.render(
                matrices, renderQueue,
                net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, 0);
    }

    @Unique
    private static Identifier lanternModel(ItemStack stack) {
        if (stack.isOf(ModItems.DIAMOND_BACKPACK)) {
            return Identifier.of("limesbackpacks", "diamond_backpack_lantern_glow");
        }
        if (stack.isOf(ModItems.NETHERITE_BACKPACK)) {
            return Identifier.of("limesbackpacks", "netherite_backpack_lantern_glow");
        }
        return null;
    }
}
