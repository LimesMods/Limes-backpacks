package com.lime.backpacks.client;

import com.lime.backpacks.ModItems;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;

public class TrinketsClientCompat {

    public static void registerRenderers() {
        BackpackRenderer renderer = new BackpackRenderer();
        TrinketRendererRegistry.registerRenderer(ModItems.LEATHER_BACKPACK, renderer);
        TrinketRendererRegistry.registerRenderer(ModItems.COPPER_BACKPACK, renderer);
        TrinketRendererRegistry.registerRenderer(ModItems.IRON_BACKPACK, renderer);
        TrinketRendererRegistry.registerRenderer(ModItems.GOLD_BACKPACK, renderer);
        TrinketRendererRegistry.registerRenderer(ModItems.DIAMOND_BACKPACK, renderer);
        TrinketRendererRegistry.registerRenderer(ModItems.NETHERITE_BACKPACK, renderer);
    }
}
