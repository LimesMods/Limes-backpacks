package com.lime.backpacks.mixin.client;

import com.lime.backpacks.QuiverAmmo;
import com.lime.backpacks.BackpackLantern;
import com.lime.backpacks.QuiverUser;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public abstract class QuiverItemModelMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private ItemStack limesbackpacks$chooseQuiver(ItemStack stack, ItemStackRenderState state, ItemStack original,
                                               ItemDisplayContext display, Level world, ItemOwner context, int seed) {
        boolean quiver = QuiverAmmo.isQuiver(stack);
        boolean unlit = BackpackLantern.hasLantern(stack) && !BackpackLantern.isEnabled(stack);
        boolean heldLantern = BackpackLantern.hasLantern(stack)
                && BackpackLantern.isEnabled(stack)
                && (display.firstPerson()
                || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        if (!quiver && !unlit && !heldLantern) return stack;
        boolean handDisplay = display.firstPerson()
                || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean arrows = !quiver || QuiverAmmo.hasStoredArrows(stack)
                || (context instanceof QuiverUser user && user.limesbackpacks$hasArrows())
                // ItemModelResolver may not provide the owning Player for a
                // first-person hand render. Read the local inventory as a
                // fallback so the visible quiver matches the arrows available
                // to the player, just like vanilla bow selection.
                || (handDisplay && Minecraft.getInstance().player != null
                && QuiverAmmo.hasAvailableArrows(Minecraft.getInstance().player));
        if (arrows && !unlit && !heldLantern) return stack;
        ItemStack rendered = stack.copy();
        String model = quiver ? (arrows ? "netherite_backpack" : "netherite_backpack_empty_quiver")
                : "diamond_backpack";
        rendered.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("limesbackpacks", model + (unlit || heldLantern ? "_unlit" : "")));
        return rendered;
    }
}
