package com.lime.backpacks.mixin.client;

import com.lime.backpacks.QuiverAmmo;
import com.lime.backpacks.BackpackLantern;
import com.lime.backpacks.QuiverUser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelManager.class)
public abstract class QuiverItemModelMixin {
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private ItemStack limesbackpacks$chooseQuiver(ItemStack stack, ItemRenderState state, ItemStack original,
                                               ItemDisplayContext display, World world, HeldItemContext context, int seed) {
        boolean quiver = QuiverAmmo.isQuiver(stack);
        boolean unlit = BackpackLantern.hasLantern(stack) && !BackpackLantern.isEnabled(stack);
        boolean heldLantern = BackpackLantern.hasLantern(stack)
                && BackpackLantern.isEnabled(stack)
                && (display.isFirstPerson()
                || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        if (!quiver && !unlit && !heldLantern) return stack;

        boolean handDisplay = display.isFirstPerson()
                || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean guiDisplay = display == ItemDisplayContext.GUI;

        // HeldItemContext is a wrapper around the owning entity in 1.21.11,
        // rather than the PlayerEntity itself. Read the entity before checking
        // the quiver flag so worn and held item renders can see arrows in the
        // player's normal inventory as well as arrows inside the backpack.
        boolean ownerHasArrows = context != null
                && context.getEntity() instanceof PlayerEntity player
                && QuiverAmmo.hasAvailableArrows(player);
        boolean ownerReportsArrows = context != null
                && context.getEntity() instanceof QuiverUser user
                && user.limesbackpacks$hasArrows();

        // Inventory and hotbar previews can be rendered without an owner in
        // their HeldItemContext. During those client GUI renders, use the
        // local player so the icon reflects arrows carried outside the bag.
        boolean localPlayerHasArrows = (guiDisplay || handDisplay)
                && MinecraftClient.getInstance().player != null
                && QuiverAmmo.hasAvailableArrows(MinecraftClient.getInstance().player);
        boolean arrows = !quiver || QuiverAmmo.hasStoredArrows(stack)
                || ownerHasArrows
                || ownerReportsArrows
                || localPlayerHasArrows;
        if (arrows && !unlit && !heldLantern) return stack;
        ItemStack rendered = stack.copy();
        String model = quiver ? (arrows ? "netherite_backpack" : "netherite_backpack_empty_quiver")
                : "diamond_backpack";
        rendered.set(DataComponentTypes.ITEM_MODEL, Identifier.of("limesbackpacks", model + (unlit || heldLantern ? "_unlit" : "")));
        return rendered;
    }
}
