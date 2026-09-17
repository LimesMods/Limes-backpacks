package com.lime.backpacks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.fabricmc.loader.api.FabricLoader;

public class BackpackItem extends Item {
    private final BackpackTier tier;

    public BackpackItem(BackpackTier tier, Item.Settings settings) {
        super(settings);
        this.tier = tier;
    }

    public BackpackTier getTier() {
        return tier;
    }

    public ExtendedScreenHandlerFactory<Integer> createScreenFactory(ItemStack stack, Inventory inventory) {
        return new ExtendedScreenHandlerFactory<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayerEntity player) {
                return tier.getRows();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return new BackpackScreenHandler(syncId, playerInventory, inventory, tier.getRows());
            }

            @Override
            public Text getDisplayName() {
                return Text.translatable("item.limesbackpacks." + tier.getId() + "_backpack");
            }
        };
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient()) {
            if (FabricLoader.getInstance().isModLoaded("trinkets")
                    && user instanceof ServerPlayerEntity serverPlayer
                    && TrinketsCompat.equipFromHand(serverPlayer, hand)) {
                return ActionResult.SUCCESS;
            }
            BackpackInventory inventory = new BackpackInventory(stack, tier.getSlotCount());
            user.openHandledScreen(createScreenFactory(stack, inventory));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isSneaking()) {
            return super.useOnBlock(context);
        }

        World world = context.getWorld();
        return BackpackPlacement.tryPlace(world, context.getPlayer(), context.getBlockPos(),
                context.getSide(), context.getPlayerYaw(), context.getStack());
    }
}
