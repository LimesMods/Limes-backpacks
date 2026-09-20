package com.lime.backpacks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public final class BackpackPlacement {
    private BackpackPlacement() {}

    public static float modelRotationDegrees(int rotation) {
        // The pocket side faces model north (-Z). Minecraft player yaw runs
        // opposite to the renderer's positive Y rotation.
        return 180.0f - rotation * 45.0f;
    }

    public static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!world.getBlockState(hit.getBlockPos()).is(ModBlocks.BACKPACK_BLOCK)) {
            ItemStack equipped = getEquippedBackpack(player);
            if (!equipped.isEmpty()) {
                InteractionResult result = tryPlace(world, player, hit.getBlockPos(), hit.getDirection(), player.getYRot(), equipped);
                if (result.consumesAction() && !world.isClientSide()) {
                    clearEquippedBackpack(player);
                }
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult tryPlace(Level world, Player player, BlockPos clickedPos,
                                        Direction side, float playerYaw, ItemStack source) {
        if (!(source.getItem() instanceof BackpackItem)) return InteractionResult.PASS;

        BlockPos placePos = clickedPos.relative(side);
        if (!world.getBlockState(placePos).canBeReplaced()
                || world.getBlockState(placePos.below()).isAir()) {
            return InteractionResult.FAIL;
        }

        int rotation = Math.floorMod(Math.round((playerYaw + 180.0f) / 45.0f), 8);
        if (!world.isClientSide()) {
            boolean placed = world.setBlock(placePos,
                    ModBlocks.BACKPACK_BLOCK.defaultBlockState().setValue(BackpackBlock.ROTATION, rotation)
                            .setValue(BackpackBlock.TIER, ((BackpackItem) source.getItem()).getTier().ordinal())
                            .setValue(BackpackBlock.LIT, BackpackLantern.isEnabled(source)),
                    Block.UPDATE_ALL);
            if (!placed) return InteractionResult.FAIL;
            if (world.getBlockEntity(placePos) instanceof BackpackBlockEntity entity) {
                entity.setBackpack(source);
            } else {
                return InteractionResult.FAIL;
            }
            source.shrink(1);
            var sounds = ModBlocks.BACKPACK_SOUNDS;
            world.playSound(null, placePos, sounds.getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, sounds.getVolume(), sounds.getPitch());
        }
        return InteractionResult.SUCCESS;
    }

    private static ItemStack getEquippedBackpack(Player player) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            return ItemStack.EMPTY;
        }
        var inventory = TrinketsCompat.getBackInventory(player);
        if (inventory == null || inventory.getContainerSize() == 0) return ItemStack.EMPTY;
        ItemStack stack = inventory.getItem(0);
        return stack.getItem() instanceof BackpackItem ? stack : ItemStack.EMPTY;
    }

    private static void clearEquippedBackpack(Player player) {
        var inventory = TrinketsCompat.getBackInventory(player);
        if (inventory == null || inventory.getContainerSize() == 0) return;
        inventory.setItem(0, ItemStack.EMPTY);
        inventory.setChanged();
        TrinketsCompat.syncQuiver(inventory);
    }
}
