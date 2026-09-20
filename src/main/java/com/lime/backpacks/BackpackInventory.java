package com.lime.backpacks;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class BackpackInventory extends SimpleContainer {
    private final ItemStack backpackStack;

    public BackpackInventory(ItemStack stack, int size) {
        super(size);
        this.backpackStack = stack;
        ItemContainerContents component = stack.get(DataComponents.CONTAINER);
        if (component != null) {
            NonNullList<ItemStack> loaded = NonNullList.withSize(size, ItemStack.EMPTY);
            component.copyInto(loaded);
            for (int i = 0; i < size; i++) {
                setItem(i, loaded.get(i));
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        NonNullList<ItemStack> stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) {
            stacks.set(i, getItem(i));
        }
        backpackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
    }
}
