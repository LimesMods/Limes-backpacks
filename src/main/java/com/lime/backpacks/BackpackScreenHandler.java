package com.lime.backpacks;

import com.lime.backpacks.gui.BackpackGuiTheme;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BackpackScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final int rows;

    // Server-side constructor (opened from item use)
    public BackpackScreenHandler(int syncId, Inventory playerInventory, Container inventory, int rows) {
        super(ModScreenHandlers.BACKPACK_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        this.rows = rows;
        checkContainerSize(inventory, rows * 9);
        inventory.startOpen(playerInventory.player);

        // Backpack slots — reject BackpackItem to prevent nesting
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
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
                    public boolean mayPickup(Player player) {
                        return !(getItem().getItem() instanceof BackpackItem);
                    }
                });
            }
        }

        // Hotbar — same lock
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, yOffset + 76) {
                @Override
                public boolean mayPickup(Player player) {
                    return !(getItem().getItem() instanceof BackpackItem);
                }
            });
        }
    }

    // Client-side constructor (receives rows count from packet via ExtendedScreenHandlerType)
    public BackpackScreenHandler(int syncId, Inventory playerInventory, int rows) {
        this(syncId, playerInventory, new SimpleContainer(rows * 9), rows);
    }

    public int getRows() {
        return rows;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            int backpackSlots = rows * 9;
            if (slotIndex < backpackSlots) {
                if (!moveItemStackTo(stack, backpackSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stack, 0, backpackSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }
}
