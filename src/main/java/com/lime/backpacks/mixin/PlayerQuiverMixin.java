package com.lime.backpacks.mixin;

import com.lime.backpacks.QuiverAmmo;
import com.lime.backpacks.QuiverUser;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerQuiverMixin implements QuiverUser {
    @Unique private static final EntityDataAccessor<Boolean> limesbackpacks_HAS_ARROWS =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique private QuiverAmmo.Source limesbackpacks$ammoSource;

    @Inject(method = "defineSynchedData", at = @At("TAIL"), require = 1)
    private void limesbackpacks$init(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(limesbackpacks_HAS_ARROWS, false);
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 1)
    private void limesbackpacks$syncArrows(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide())
            player.getEntityData().set(limesbackpacks_HAS_ARROWS, QuiverAmmo.hasAvailableArrows(player));
    }

    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true, require = 1)
    private void limesbackpacks$findAmmo(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        limesbackpacks$ammoSource = null;
        if (!cir.getReturnValue().isEmpty() || !(weapon.getItem() instanceof ProjectileWeaponItem ranged)) return;
        limesbackpacks$ammoSource = QuiverAmmo.find((Player) (Object) this, ranged.getAllSupportedProjectiles());
        if (limesbackpacks$ammoSource != null) cir.setReturnValue(limesbackpacks$ammoSource.projectile());
    }

    @Override public QuiverAmmo.Source limesbackpacks$getAmmoSource() { return limesbackpacks$ammoSource; }
    @Override public void limesbackpacks$setAmmoSource(QuiverAmmo.Source source) { limesbackpacks$ammoSource = source; }
    @Override public boolean limesbackpacks$hasArrows() {
        return ((Player) (Object) this).getEntityData().get(limesbackpacks_HAS_ARROWS);
    }
}
