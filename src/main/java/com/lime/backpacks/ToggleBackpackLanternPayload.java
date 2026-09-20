package com.lime.backpacks;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleBackpackLanternPayload() implements CustomPacketPayload {
    public static final Type<ToggleBackpackLanternPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath("limesbackpacks", "toggle_lantern"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleBackpackLanternPayload> CODEC =
            StreamCodec.unit(new ToggleBackpackLanternPayload());

    @Override
    public Type<ToggleBackpackLanternPayload> type() { return ID; }
}
