package com.lime.backpacks;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenBackpackPayload() implements CustomPayload {
    public static final Id<OpenBackpackPayload> ID = new Id<>(Identifier.of("limesbackpacks", "open_backpack"));
    public static final PacketCodec<RegistryByteBuf, OpenBackpackPayload> CODEC = PacketCodec.unit(new OpenBackpackPayload());

    @Override
    public Id<OpenBackpackPayload> getId() {
        return ID;
    }
}
