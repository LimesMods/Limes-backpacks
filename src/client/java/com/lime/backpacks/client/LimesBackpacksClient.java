package com.lime.backpacks.client;

import com.lime.backpacks.ModScreenHandlers;
import com.lime.backpacks.ModBlocks;
import com.lime.backpacks.OpenBackpackPayload;
import com.lime.backpacks.ToggleBackpackLanternPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class LimesBackpacksClient implements ClientModInitializer {
    public static KeyBinding OPEN_BACKPACK_KEY;
    public static KeyBinding TOGGLE_LANTERN_KEY;

    private static final KeyBinding.Category BACKPACK_CATEGORY =
            KeyBinding.Category.create(Identifier.of("limesbackpacks", "backpacks"));

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.BACKPACK_SCREEN_HANDLER, BackpackScreen::new);
        BlockEntityRendererFactories.register(ModBlocks.BACKPACK_BLOCK_ENTITY, BackpackBlockEntityRenderer::new);
        TOGGLE_LANTERN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.limesbackpacks.toggle_lantern", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G, BACKPACK_CATEGORY));

        OPEN_BACKPACK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.limesbackpacks.open_backpack",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                BACKPACK_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_LANTERN_KEY.wasPressed()) {
                if (client.player != null && client.currentScreen == null
                        && ClientPlayNetworking.canSend(ToggleBackpackLanternPayload.ID)) {
                    ClientPlayNetworking.send(new ToggleBackpackLanternPayload());
                }
            }
            while (OPEN_BACKPACK_KEY.wasPressed()) {
                if (client.player != null && client.currentScreen == null
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
