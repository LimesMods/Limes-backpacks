package com.lime.backpacks;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class BackpackInventory extends SimpleInventory {
    private final ItemStack backpackStack;

    public BackpackInventory(ItemStack stack, int size) {
        super(size);
        this.backpackStack = stack;
        ContainerComponent component = stack.get(DataComponentTypes.CONTAINER);
        if (component != null) {
            DefaultedList<ItemStack> loaded = DefaultedList.ofSize(size, ItemStack.EMPTY);
            component.copyTo(loaded);
            for (int i = 0; i < size; i++) {
                setStack(i, loaded.get(i));
            }
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        DefaultedList<ItemStack> stacks = DefaultedList.ofSize(size(), ItemStack.EMPTY);
        for (int i = 0; i < size(); i++) {
            stacks.set(i, getStack(i));
        }
        backpackStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
    }
}
