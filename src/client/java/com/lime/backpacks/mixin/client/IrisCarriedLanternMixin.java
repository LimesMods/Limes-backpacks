package com.lime.backpacks.mixin.client;

import com.lime.backpacks.TrinketsCompat;
import com.lime.backpacks.client.CarriedLanternLight;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Supplies brightness AND the lantern ID to the actual held-light uniforms.
 * Does not change currentRenderedItemId, which controls the backpack's surface material.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier", remap = false)
public abstract class IrisCarriedLanternMixin {
    @Shadow @Final private Hand hand;
    @Shadow @Final private Object2IntFunction<Object> itemIdMap;
    @Shadow private int intID;
    @Shadow private int lightValue;
    @Shadow private Vector3f lightColor;
    @Unique private Object limesbackpacks$lanternId;
    @Unique private boolean limesbackpacks$resolved;
    @Unique private boolean limesbackpacks$logged;

    @Inject(method = "update", at = @At("TAIL"), require = 1, remap = false)
    private void limesbackpacks$updateCarriedLight(CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        boolean worn = FabricLoader.getInstance().isModLoaded("trinkets")
                && TrinketsCompat.hasEquippedLitLantern(player);
        if (!CarriedLanternLight.useLantern(hand, player.getMainHandStack(),
                player.getOffHandStack(), worn, lightValue)) return;

        if (!limesbackpacks$resolved) {
            limesbackpacks$resolved = true;
            try {
                limesbackpacks$lanternId = Class.forName(
                        "net.irisshaders.iris.shaderpack.materialmap.NamespacedId")
                        .getConstructor(String.class, String.class).newInstance("minecraft", "lantern");
            } catch (ReflectiveOperationException exception) {
                LoggerFactory.getLogger("Lime's Backpacks/Iris Compat")
                        .warn("Cannot resolve Iris's lantern light identifier", exception);
            }
        }
        if (limesbackpacks$lanternId == null) return;
        intID = itemIdMap.getInt(limesbackpacks$lanternId);
        lightValue = 15;
        // Vanilla lanterns use Iris's default light color. Complementary obtains
        // its warm hue from intID through its own GetSpecialBlocklightColor table.
        lightColor = new Vector3f(1.0f);
        if (!limesbackpacks$logged) {
            limesbackpacks$logged = true;
            LoggerFactory.getLogger("Lime's Backpacks/Iris Compat").info(
                    "Carried backpack shader light active: channel={}, lanternId={}, brightness={}",
                    hand, intID, lightValue);
        }
    }
}
