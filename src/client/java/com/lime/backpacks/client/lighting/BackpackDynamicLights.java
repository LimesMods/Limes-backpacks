package com.lime.backpacks.client.lighting;

import com.lime.backpacks.ModItems;
import com.lime.backpacks.TrinketsCompat;
import com.lime.backpacks.client.IrisShaderCompat;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Only LambDynamicLights loads this optional entrypoint; dedicated servers never do. */
public final class BackpackDynamicLights implements DynamicLightsInitializer {
    private static final Map<UUID, WornLanternLight> LIGHTS = new HashMap<>();
    private ClientWorld world;

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        // LDL's standard item handling covers main/offhand and dropped items, not stored bags.
        context.itemLightSourceManager().onRegisterEvent().register(registry -> {
            registry.register(ModItems.DIAMOND_BACKPACK, 15);
            registry.register(ModItems.NETHERITE_BACKPACK, 15);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client, context));
    }

    private void tick(MinecraftClient client, DynamicLightsContext context) {
        var manager = context.dynamicLightBehaviorManager();
        if (world != client.world) {
            LIGHTS.values().forEach(light -> { light.remove(); manager.remove(light); });
            LIGHTS.clear();
            world = client.world;
        }
        if (world == null) return;
        var active = new HashSet<UUID>();
        // Iris receives the local worn lantern through its held-light uniforms.
        // Running this moving LambDynamicLights source at the same time makes
        // the two lighting systems alternately win as the player crosses block
        // boundaries, which appears as flicker while running.
        boolean irisShaderLighting = FabricLoader.getInstance().isModLoaded("iris")
                && IrisShaderCompat.isShaderPackActive();
        if (FabricLoader.getInstance().isModLoaded("trinkets") && !irisShaderLighting) {
            for (PlayerEntity player : world.getPlayers()) {
                if (!player.isAlive() || player.isSpectator() || !TrinketsCompat.hasEquippedLitLantern(player)) continue;
                active.add(player.getUuid());
                WornLanternLight light = LIGHTS.get(player.getUuid());
                if (light == null) {
                    light = new WornLanternLight();
                    LIGHTS.put(player.getUuid(), light);
                    light.update(fallbackPosition(player), 15);
                    manager.add(light);
                }
                int luminance = context.itemLightSourceManager().getLuminance(
                        new ItemStack(Items.LANTERN), player.isSubmergedInWater());
                light.tick(player, luminance);
            }
        }
        LIGHTS.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey())) return false;
            entry.getValue().remove();
            manager.remove(entry.getValue());
            return true;
        });

    }

    public static void captureAnchor(PlayerEntity player, Vec3d anchor) {
        WornLanternLight light = LIGHTS.get(player.getUuid());
        // Ignore inventory/GUI entity previews and any non-world rendering matrices.
        if (light != null && anchor.squaredDistanceTo(player.getEntityPos()) < 9) {
            light.captureAnchor(player, anchor);
        }
    }

    static Vec3d fallbackPosition(PlayerEntity player) {
        // Also works in first person and for off-screen players, where no model is rendered.
        Vec3d offset = new Vec3d(-.36, player.isSneaking() ? .77 : .96, -.40);
        if (player.isInSwimmingPose() || player.isGliding()) {
            offset = new Vec3d(-.36, .30, -.32);
        }
        return player.getEntityPos().add(offset.rotateY((float) Math.toRadians(-player.bodyYaw)));
    }
}
