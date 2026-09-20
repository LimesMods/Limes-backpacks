package com.lime.backpacks;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    public static MenuType<BackpackScreenHandler> BACKPACK_SCREEN_HANDLER;

    public static void register() {
        BACKPACK_SCREEN_HANDLER = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath("limesbackpacks", "backpack"),
                new ExtendedMenuType<>(
                        (syncId, playerInventory, rows) -> new BackpackScreenHandler(syncId, playerInventory, rows),
                        ByteBufCodecs.VAR_INT
                )
        );
    }
}
