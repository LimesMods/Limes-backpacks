package com.lime.backpacks;

import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.stream.IntStream;

public class BackpackBlockEntity extends BlockEntity implements SidedInventory {
    private ItemStack backpackStack = ItemStack.EMPTY;
    private BackpackInventory inventory;

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BACKPACK_BLOCK_ENTITY, pos, state);
    }

    public void setBackpack(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem backpack)) {
            this.backpackStack = ItemStack.EMPTY;
            this.inventory = null;
            markDirty();
            return;
        }

        this.backpackStack = stack.copy();
        this.backpackStack.setCount(1);
        this.inventory = new BackpackInventory(this.backpackStack, backpack.getTier().getSlotCount());
        syncLanternLight();
        markDirty();
    }

    public ItemStack getBackpack() {
        return backpackStack;
    }

    private void syncLanternLight() {
        if (world == null || world.isClient() || !getCachedState().contains(BackpackBlock.LIT)) return;
        boolean lit = BackpackLantern.isEnabled(backpackStack);
        if (getCachedState().get(BackpackBlock.LIT) != lit) {
            world.setBlockState(pos, getCachedState().with(BackpackBlock.LIT, lit), Block.NOTIFY_ALL);
        }
    }

    public void syncTier() {
        // Upgrade already-placed backpacks from before tier-specific particles.
        if (world != null && !world.isClient() && backpackStack.getItem() instanceof BackpackItem item
                && getCachedState().get(BackpackBlock.TIER) != item.getTier().ordinal()) {
            world.setBlockState(pos, getCachedState().with(BackpackBlock.TIER, item.getTier().ordinal()), 3);
        }
    }

    public ItemStack takeBackpack() {
        if (inventory != null) inventory.markDirty();
        ItemStack result = backpackStack.copy();
        backpackStack = ItemStack.EMPTY;
        inventory = null;
        markDirty();
        return result;
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        // BlockEntity's default implementation scatters every Inventory into
        // the world. Our block loot returns the whole bag with its contents.
        if (inventory != null) inventory.markDirty();
    }

    private BackpackInventory contents() {
        return inventory;
    }

    @Override
    protected void readData(ReadView view) {
        backpackStack = ItemStack.EMPTY;
        inventory = null;
        view.read("Backpack", ItemStack.CODEC).ifPresent(this::loadBackpack);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        // The renderer needs the stored item, including its lantern/quiver state.
        return createNbt(registries);
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
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
    protected void writeData(WriteView view) {
        if (!backpackStack.isEmpty()) {
            view.put("Backpack", ItemStack.CODEC, backpackStack);
        }
    }

    @Override
    public int size() {
        return contents() == null ? 0 : contents().size();
    }

    @Override
    public boolean isEmpty() {
        return contents() == null || contents().isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return contents() == null ? ItemStack.EMPTY : contents().getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (contents() == null) return ItemStack.EMPTY;
        ItemStack result = contents().removeStack(slot, amount);
        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (contents() == null) return ItemStack.EMPTY;
        ItemStack result = contents().removeStack(slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (contents() == null) return;
        contents().setStack(slot, stack);
        markDirty();
    }

    @Override
    public void markDirty() {
        // Slot clicks and hopper merges can mutate an existing stack directly.
        if (inventory != null) inventory.markDirty();
        super.markDirty();
        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return !isRemoved() && world != null && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0;
    }

    @Override
    public void onOpen(ContainerUser user) {
        if (contents() != null) contents().onOpen(user);
    }

    @Override
    public void onClose(ContainerUser user) {
        if (contents() != null) contents().onClose(user);
    }

    @Override
    public void clear() {
        if (contents() != null) {
            contents().clear();
            markDirty();
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return IntStream.range(0, size()).toArray();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < size() && !(stack.getItem() instanceof BackpackItem)
                && getStack(slot).getCount() < getMaxCount(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < size();
    }
}
