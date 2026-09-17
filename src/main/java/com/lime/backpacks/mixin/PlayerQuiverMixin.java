package com.lime.backpacks.mixin;

import com.lime.backpacks.QuiverAmmo;
import com.lime.backpacks.QuiverUser;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerQuiverMixin implements QuiverUser {
    @Unique private static final TrackedData<Boolean> limesbackpacks_HAS_ARROWS =
            DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private QuiverAmmo.Source limesbackpacks$ammoSource;

    @Inject(method = "initDataTracker", at = @At("TAIL"), require = 1)
    private void limesbackpacks$init(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(limesbackpacks_HAS_ARROWS, false);
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 1)
    private void limesbackpacks$syncArrows(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getEntityWorld().isClient())
            player.getDataTracker().set(limesbackpacks_HAS_ARROWS, QuiverAmmo.hasAvailableArrows(player));
    }

    @Inject(method = "getProjectileType", at = @At("RETURN"), cancellable = true, require = 1)
    private void limesbackpacks$findAmmo(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        limesbackpacks$ammoSource = null;
        if (!cir.getReturnValue().isEmpty() || !(weapon.getItem() instanceof RangedWeaponItem ranged)) return;
        limesbackpacks$ammoSource = QuiverAmmo.find((PlayerEntity) (Object) this, ranged.getProjectiles());
        if (limesbackpacks$ammoSource != null) cir.setReturnValue(limesbackpacks$ammoSource.projectile());
    }

    @Override public QuiverAmmo.Source limesbackpacks$getAmmoSource() { return limesbackpacks$ammoSource; }
    @Override public void limesbackpacks$setAmmoSource(QuiverAmmo.Source source) { limesbackpacks$ammoSource = source; }
    @Override public boolean limesbackpacks$hasArrows() {
        return ((PlayerEntity) (Object) this).getDataTracker().get(limesbackpacks_HAS_ARROWS);
    }
}
