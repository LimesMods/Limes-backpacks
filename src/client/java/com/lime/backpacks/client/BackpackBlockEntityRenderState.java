package com.lime.backpacks.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class BackpackBlockEntityRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    public final ItemStackRenderState lanternGlowRenderState = new ItemStackRenderState();
    public int rotation;
}
