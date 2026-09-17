package com.lime.backpacks;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleBackpackLanternPayload() implements CustomPayload {
    public static final Id<ToggleBackpackLanternPayload> ID =
            new Id<>(Identifier.of("limesbackpacks", "toggle_lantern"));
    public static final PacketCodec<RegistryByteBuf, ToggleBackpackLanternPayload> CODEC =
            PacketCodec.unit(new ToggleBackpackLanternPayload());

    @Override
    public Id<ToggleBackpackLanternPayload> getId() { return ID; }
}
