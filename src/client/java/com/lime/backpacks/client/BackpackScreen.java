package com.lime.backpacks.client;

import com.lime.backpacks.BackpackScreenHandler;
import com.lime.backpacks.gui.BackpackGuiTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class BackpackScreen extends HandledScreen<BackpackScreenHandler> {
    private final BackpackGuiTheme theme;

    public BackpackScreen(BackpackScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.theme = BackpackGuiTheme.forRows(handler.getRows());
        this.backgroundHeight = 114 + handler.getRows() * 18 + BackpackGuiTheme.EXTRA_BINDING_HEIGHT;
        // Leave the standalone inventory panel's top bevel clear of its label.
        this.playerInventoryTitleY = this.backgroundHeight - 92;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (LimesBackpacksClient.OPEN_BACKPACK_KEY.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (LimesBackpacksClient.OPEN_BACKPACK_KEY.matchesMouse(click)) {
            close();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        BackpackGuiTheme.Pixels pixels = (left, top, right, bottom, color) ->
                context.fill(this.x + left, this.y + top, this.x + right, this.y + bottom, color);
        theme.drawPanel(pixels, handler.getRows());
        int backpackSlots = handler.getRows() * 9;
        for (int i = 0; i < handler.slots.size(); i++) {
            var slot = handler.slots.get(i);
            theme.drawSlot(pixels, slot.x, slot.y, i < backpackSlots);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY,
                BackpackGuiTheme.TITLE, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle,
                this.playerInventoryTitleX, this.playerInventoryTitleY,
                BackpackGuiTheme.INVENTORY_TITLE, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Screen.renderWithTooltip already draws the background on its own layer.
        // Redrawing it here can cover stack counts with the slot borders.
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
