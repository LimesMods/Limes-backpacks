package com.lime.backpacks.client;

import com.lime.backpacks.BackpackScreenHandler;
import com.lime.backpacks.gui.BackpackGuiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BackpackScreen extends AbstractContainerScreen<BackpackScreenHandler> {
    private final BackpackGuiTheme theme;

    public BackpackScreen(BackpackScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176,
                114 + handler.getRows() * 18 + BackpackGuiTheme.EXTRA_BINDING_HEIGHT);
        this.theme = BackpackGuiTheme.forRows(handler.getRows());
        // Leave the standalone inventory panel's top bevel clear of its label.
        this.inventoryLabelY = this.imageHeight - 92;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (LimesBackpacksClient.OPEN_BACKPACK_KEY.matches(input)) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (LimesBackpacksClient.OPEN_BACKPACK_KEY.matchesMouse(click)) {
            onClose();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        BackpackGuiTheme.Pixels pixels = (left, top, right, bottom, color) ->
                context.fill(this.leftPos + left, this.topPos + top, this.leftPos + right, this.topPos + bottom, color);
        theme.drawPanel(pixels, menu.getRows());
        int backpackSlots = menu.getRows() * 9;
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            theme.drawSlot(pixels, slot.x, slot.y, i < backpackSlots);
        }
        super.extractContents(context, mouseX, mouseY, delta);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY,
                BackpackGuiTheme.TITLE, false);
        context.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY,
                BackpackGuiTheme.INVENTORY_TITLE, false);
    }
}
