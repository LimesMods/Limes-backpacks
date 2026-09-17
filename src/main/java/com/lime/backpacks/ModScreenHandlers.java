package com.lime.backpacks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<BackpackScreenHandler> BACKPACK_SCREEN_HANDLER;

    public static void register() {
        BACKPACK_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of("limesbackpacks", "backpack"),
                new ExtendedScreenHandlerType<>(
                        (syncId, playerInventory, rows) -> new BackpackScreenHandler(syncId, playerInventory, rows),
                        PacketCodecs.VAR_INT
                )
        );
    }
}
