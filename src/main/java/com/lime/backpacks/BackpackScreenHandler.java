package com.lime.backpacks;

import com.lime.backpacks.gui.BackpackGuiTheme;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class BackpackScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final int rows;

    // Server-side constructor (opened from item use)
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, int rows) {
        super(ModScreenHandlers.BACKPACK_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        this.rows = rows;
        checkSize(inventory, rows * 9);
        inventory.onOpen(playerInventory.player);

        // Backpack slots — reject BackpackItem to prevent nesting
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return !(stack.getItem() instanceof BackpackItem);
                    }
                });
            }
        }

        int yOffset = rows * 18 + 14 + BackpackGuiTheme.EXTRA_BINDING_HEIGHT;

        // Player inventory (rows 1-3) — lock slots that hold a backpack while the screen is open
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, yOffset + row * 18 + 18) {
                    @Override
                    public boolean canTakeItems(PlayerEntity player) {
                        return !(getStack().getItem() instanceof BackpackItem);
                    }
                });
            }
        }

        // Hotbar — same lock
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, yOffset + 76) {
                @Override
                public boolean canTakeItems(PlayerEntity player) {
                    return !(getStack().getItem() instanceof BackpackItem);
                }
            });
        }
    }

    // Client-side constructor (receives rows count from packet via ExtendedScreenHandlerType)
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, int rows) {
        this(syncId, playerInventory, new SimpleInventory(rows * 9), rows);
    }

    public int getRows() {
        return rows;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();

            int backpackSlots = rows * 9;
            if (slotIndex < backpackSlots) {
                if (!insertItem(stack, backpackSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!insertItem(stack, 0, backpackSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }
}
