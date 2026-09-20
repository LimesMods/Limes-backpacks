package com.lime.backpacks;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class BackpackItem extends Item {
    private final BackpackTier tier;

    public BackpackItem(BackpackTier tier, Item.Properties settings) {
        super(settings);
        this.tier = tier;
    }

    public BackpackTier getTier() {
        return tier;
    }

    public ExtendedMenuProvider<Integer> createScreenFactory(ItemStack stack, Container inventory) {
        return new ExtendedMenuProvider<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer player) {
                return tier.getRows();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new BackpackScreenHandler(syncId, playerInventory, inventory, tier.getRows());
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("item.limesbackpacks." + tier.getId() + "_backpack");
            }
        };
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!world.isClientSide()) {
            if (FabricLoader.getInstance().isModLoaded("trinkets")
                    && user instanceof ServerPlayer serverPlayer
                    && TrinketsCompat.equipFromHand(serverPlayer, hand)) {
                return InteractionResult.SUCCESS;
            }
            BackpackInventory inventory = new BackpackInventory(stack, tier.getSlotCount());
            user.openMenu(createScreenFactory(stack, inventory));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        }

        Level world = context.getLevel();
        return BackpackPlacement.tryPlace(world, context.getPlayer(), context.getClickedPos(),
                context.getClickedFace(), context.getRotation(), context.getItemInHand());
    }
}
