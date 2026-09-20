package com.lime.backpacks;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public final class BackpackPlacement {
    private BackpackPlacement() {}

    public static float modelRotationDegrees(int rotation) {
        // The pocket side faces model north (-Z). Minecraft player yaw runs
        // opposite to the renderer's positive Y rotation.
        return 180.0f - rotation * 45.0f;
    }

    public static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!player.isSneaking() || !player.getStackInHand(hand).isEmpty()) {
            return ActionResult.PASS;
        }
        if (!world.getBlockState(hit.getBlockPos()).isOf(ModBlocks.BACKPACK_BLOCK)) {
            ItemStack equipped = getEquippedBackpack(player);
            if (!equipped.isEmpty()) {
                ActionResult result = tryPlace(world, player, hit.getBlockPos(), hit.getSide(), player.getYaw(), equipped);
                if (result.isAccepted() && !world.isClient()) {
                    clearEquippedBackpack(player);
                }
                return result;
            }
        }
        return ActionResult.PASS;
    }

    public static ActionResult tryPlace(World world, PlayerEntity player, BlockPos clickedPos,
                                        Direction side, float playerYaw, ItemStack source) {
        if (!(source.getItem() instanceof BackpackItem)) return ActionResult.PASS;

        BlockPos placePos = clickedPos.offset(side);
        if (!world.getBlockState(placePos).isReplaceable()
                || world.getBlockState(placePos.down()).isAir()) {
            return ActionResult.FAIL;
        }

        int rotation = Math.floorMod(Math.round((playerYaw + 180.0f) / 45.0f), 8);
        if (!world.isClient()) {
            boolean placed = world.setBlockState(placePos,
                    ModBlocks.BACKPACK_BLOCK.getDefaultState().with(BackpackBlock.ROTATION, rotation)
                            .with(BackpackBlock.TIER, ((BackpackItem) source.getItem()).getTier().ordinal())
                            .with(BackpackBlock.LIT, BackpackLantern.isEnabled(source)),
                    Block.NOTIFY_ALL);
            if (!placed) return ActionResult.FAIL;
            if (world.getBlockEntity(placePos) instanceof BackpackBlockEntity entity) {
                entity.setBackpack(source);
            } else {
                return ActionResult.FAIL;
            }
            source.decrement(1);
            var sounds = ModBlocks.BACKPACK_SOUNDS;
            world.playSound(null, placePos, sounds.getPlaceSound(),
                    net.minecraft.sound.SoundCategory.BLOCKS, sounds.getVolume(), sounds.getPitch());
        }
        return ActionResult.SUCCESS;
    }

    private static ItemStack getEquippedBackpack(PlayerEntity player) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            return ItemStack.EMPTY;
        }
        var inventory = TrinketsCompat.getBackInventory(player);
        if (inventory == null || inventory.size() == 0) return ItemStack.EMPTY;
        ItemStack stack = inventory.getStack(0);
        return stack.getItem() instanceof BackpackItem ? stack : ItemStack.EMPTY;
    }

    private static void clearEquippedBackpack(PlayerEntity player) {
        var inventory = TrinketsCompat.getBackInventory(player);
        if (inventory == null || inventory.size() == 0) return;
        inventory.setStack(0, ItemStack.EMPTY);
        inventory.markDirty();
        TrinketsCompat.syncQuiver(inventory);
    }
}
