package com.lime.backpacks.client;

import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;

public final class BackpackBlockEntityRenderState extends BlockEntityRenderState {
    public final ItemRenderState itemRenderState = new ItemRenderState();
    public final ItemRenderState lanternGlowRenderState = new ItemRenderState();
    public int rotation;
}
