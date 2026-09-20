package com.lime.backpacks;

import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BackpackBlockEntity extends BlockEntity implements WorldlyContainer {
    private ItemStack backpackStack = ItemStack.EMPTY;
    private BackpackInventory inventory;

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BACKPACK_BLOCK_ENTITY, pos, state);
    }

    public void setBackpack(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem backpack)) {
            this.backpackStack = ItemStack.EMPTY;
            this.inventory = null;
            setChanged();
            return;
        }

        this.backpackStack = stack.copy();
        this.backpackStack.setCount(1);
        this.inventory = new BackpackInventory(this.backpackStack, backpack.getTier().getSlotCount());
        syncLanternLight();
        setChanged();
    }

    public ItemStack getBackpack() {
        return backpackStack;
    }

    private void syncLanternLight() {
        if (level == null || level.isClientSide() || !getBlockState().hasProperty(BackpackBlock.LIT)) return;
        boolean lit = BackpackLantern.isEnabled(backpackStack);
        if (getBlockState().getValue(BackpackBlock.LIT) != lit) {
            level.setBlock(worldPosition, getBlockState().setValue(BackpackBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    public void syncTier() {
        // Upgrade already-placed backpacks from before tier-specific particles.
        if (level != null && !level.isClientSide() && backpackStack.getItem() instanceof BackpackItem item
                && getBlockState().getValue(BackpackBlock.TIER) != item.getTier().ordinal()) {
            level.setBlock(worldPosition, getBlockState().setValue(BackpackBlock.TIER, item.getTier().ordinal()), 3);
        }
    }

    public ItemStack takeBackpack() {
        if (inventory != null) inventory.setChanged();
        ItemStack result = backpackStack.copy();
        backpackStack = ItemStack.EMPTY;
        inventory = null;
        setChanged();
        return result;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        // BlockEntity's default implementation scatters every Inventory into
        // the world. Our block loot returns the whole bag with its contents.
        if (inventory != null) inventory.setChanged();
    }

    private BackpackInventory contents() {
        return inventory;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        backpackStack = ItemStack.EMPTY;
        inventory = null;
        view.read("Backpack", ItemStack.CODEC).ifPresent(this::loadBackpack);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // The renderer needs the stored item, including its lantern/quiver state.
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void loadBackpack(ItemStack stack) {
        if (stack.getItem() instanceof BackpackItem backpack) {
            backpackStack = stack.copy();
            backpackStack.setCount(1);
            inventory = new BackpackInventory(backpackStack, backpack.getTier().getSlotCount());
            syncLanternLight();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        if (!backpackStack.isEmpty()) {
            view.store("Backpack", ItemStack.CODEC, backpackStack);
        }
    }

    @Override
    public int getContainerSize() {
        return contents() == null ? 0 : contents().getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return contents() == null || contents().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return contents() == null ? ItemStack.EMPTY : contents().getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (contents() == null) return ItemStack.EMPTY;
        ItemStack result = contents().removeItem(slot, amount);
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (contents() == null) return ItemStack.EMPTY;
        ItemStack result = contents().removeItemNoUpdate(slot);
        setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (contents() == null) return;
        contents().setItem(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        // Slot clicks and hopper merges can mutate an existing stack directly.
        if (inventory != null) inventory.setChanged();
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (contents() != null) contents().startOpen(user);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (contents() != null) contents().stopOpen(user);
    }

    @Override
    public void clearContent() {
        if (contents() != null) {
            contents().clearContent();
            setChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < getContainerSize() && !(stack.getItem() instanceof BackpackItem)
                && getItem(slot).getCount() < getMaxStackSize(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < getContainerSize();
    }
}
