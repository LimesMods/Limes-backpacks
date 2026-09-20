package com.lime.backpacks.mixin;

import com.lime.backpacks.QuiverAmmo;
import com.lime.backpacks.QuiverUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProjectileWeaponItem.class)
public abstract class RangedWeaponQuiverMixin {
    // Vanilla determines consumption first, preserving Infinity, creative and Multishot.
    @Redirect(method = "useAmmo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;"), require = 1)
    private static ItemStack limesbackpacks$consume(ItemStack ammo, int count, ItemStack weapon,
                                                ItemStack projectile, LivingEntity shooter, boolean multishot) {
        if (shooter instanceof QuiverUser user) {
            QuiverAmmo.Source source = user.limesbackpacks$getAmmoSource();
            if (source != null && source.projectile() == ammo) {
                user.limesbackpacks$setAmmoSource(null);
                return QuiverAmmo.consume(source, count);
            }
        }
        return ammo.split(count);
    }
}
