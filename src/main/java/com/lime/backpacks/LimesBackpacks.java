package com.lime.backpacks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class LimesBackpacks implements ModInitializer {
    @Override
    public void onInitialize() {
        ModItems.register();
        ModBlocks.register();
        ModItemGroups.register();
        ModRecipes.register();
        ModScreenHandlers.register();
        UseBlockCallback.EVENT.register(BackpackPlacement::onUseBlock);
        if (FabricLoader.getInstance().isModLoaded("trinkets")) BackpackEquipSounds.register();

        PayloadTypeRegistry.playC2S().register(OpenBackpackPayload.ID, OpenBackpackPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenBackpackPayload.ID, (payload, context) ->
                context.server().execute(() -> openBackpack(context.player())));
        PayloadTypeRegistry.playC2S().register(ToggleBackpackLanternPayload.ID, ToggleBackpackLanternPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleBackpackLanternPayload.ID, (payload, context) ->
                context.server().execute(() -> BackpackLantern.toggleFor(context.player())));
    }

    private static void openBackpack(ServerPlayerEntity player) {
        // Try Trinkets back slot first
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            if (TrinketsCompat.openEquippedBackpack(player)) return;
        }

        // Search all inventory slots (main + offhand)
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof BackpackItem backpackItem) {
                BackpackInventory inventory = new BackpackInventory(stack, backpackItem.getTier().getSlotCount());
                player.openHandledScreen(backpackItem.createScreenFactory(stack, inventory));
                return;
            }
        }
    }
}
