package com.lime.backpacks.client;

import com.lime.backpacks.ModScreenHandlers;
import com.lime.backpacks.ModBlocks;
import com.lime.backpacks.OpenBackpackPayload;
import com.lime.backpacks.ToggleBackpackLanternPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.Identifier;

public class LimesBackpacksClient implements ClientModInitializer {
    public static KeyMapping OPEN_BACKPACK_KEY;
    public static KeyMapping TOGGLE_LANTERN_KEY;

    private static final KeyMapping.Category BACKPACK_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("limesbackpacks", "backpacks"));

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.BACKPACK_SCREEN_HANDLER, BackpackScreen::new);
        BlockEntityRenderers.register(ModBlocks.BACKPACK_BLOCK_ENTITY, BackpackBlockEntityRenderer::new);
        TOGGLE_LANTERN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.limesbackpacks.toggle_lantern", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_G, BACKPACK_CATEGORY));

        OPEN_BACKPACK_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.limesbackpacks.open_backpack",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_B,
                BACKPACK_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_LANTERN_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null
                        && ClientPlayNetworking.canSend(ToggleBackpackLanternPayload.ID)) {
                    ClientPlayNetworking.send(new ToggleBackpackLanternPayload());
                }
            }
            while (OPEN_BACKPACK_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null
                        && ClientPlayNetworking.canSend(OpenBackpackPayload.ID)) {
                    ClientPlayNetworking.send(new OpenBackpackPayload());
                }
            }
        });

        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            TrinketsClientCompat.registerRenderers();
        }
    }
}
