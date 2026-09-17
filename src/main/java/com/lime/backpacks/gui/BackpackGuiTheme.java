package com.lime.backpacks.gui;

import com.lime.backpacks.BackpackTier;

/** Original pixel-art GUI, independent of vanilla chest textures and resource packs. */
public record BackpackGuiTheme(int fabric, int fastening) {
    public static final int WIDTH = 176;
    public static final int EXTRA_BINDING_HEIGHT = 6;
    public static final int TITLE = 0xFFFFF1DF;
    public static final int INVENTORY_TITLE = 0xFF404040;
    private static final int INVENTORY_BACKGROUND = 0xFFC6C6C6;
    private static final int INVENTORY_SLOT = 0xFF8B8B8B;
    private static final int INVENTORY_SLOT_SHADOW = 0xFF373737;

    @FunctionalInterface
    public interface Pixels {
        void fill(int left, int top, int right, int bottom, int color);
    }

    public static BackpackGuiTheme forRows(int rows) {
        for (BackpackTier tier : BackpackTier.values()) {
            if (tier.getRows() == rows) return forTier(tier);
        }
        throw new IllegalArgumentException("Unknown backpack row count: " + rows);
    }

    public static BackpackGuiTheme forTier(BackpackTier tier) {
        // Cloth atlas centers and pocket fastening swatches from the actual 3D models.
        return switch (tier) {
            case LEATHER -> new BackpackGuiTheme(0xFF795F4C, 0xFFAC896B);
            case COPPER -> new BackpackGuiTheme(0xFF976F55, 0xFFCD8E34);
            case IRON -> new BackpackGuiTheme(0xFF80847D, 0xFFD4D4D4);
            case GOLD -> new BackpackGuiTheme(0xFF937F56, 0xFFFCE869);
            case DIAMOND -> new BackpackGuiTheme(0xFF617F82, 0xFFB6F8F0);
            case NETHERITE -> new BackpackGuiTheme(0xFF514D51, 0xFF766C84);
        };
    }

    public void drawPanel(Pixels pixels, int rows) {
        int height = 114 + rows * 18 + EXTRA_BINDING_HEIGHT;
        int divider = rows * 18 + 19 + EXTRA_BINDING_HEIGHT;
        int edge = mix(fabric, 0xFF161718, .62f);
        // Stepped two-pixel corners, with a fabric binding instead of chest hardware.
        pixels.fill(2, 0, WIDTH - 2, divider, edge);
        pixels.fill(0, 2, WIDTH, divider - 2, edge);
        pixels.fill(2, 2, WIDTH - 2, divider - 1, mix(fabric, 0xFFEADDCB, .30f));
        pixels.fill(3, 3, WIDTH - 3, divider - 2, mix(fabric, 0xFF18191B, .20f));
        pixels.fill(4, 16, WIDTH - 4, divider - 2, fabric);
        // A full-width sewn hem below the last slot, not a line over the slot bevel.
        pixels.fill(3, rows * 18 + 17, WIDTH - 3, divider - 2,
                mix(fabric, edge, .16f));
        // The bag has its own finished bottom binding. The inventory below has
        // a separate vanilla-grey frame, not a continuation of the fabric border.
        pixels.fill(3, divider - 2, WIDTH - 3, divider - 1, mix(fabric, edge, .32f));
        pixels.fill(2, divider - 1, WIDTH - 2, divider, edge);
        drawInventoryPanel(pixels, divider, height);

        int thread = mix(fabric, 0xFFE6D6BB, .25f);
        for (int y = 4; y + 2 <= divider - 3; y += 5) {
            pixels.fill(4, y, 5, y + 2, thread);
            pixels.fill(WIDTH - 5, y, WIDTH - 4, y + 2, thread);
        }
        for (int x = 7; x + 2 <= WIDTH - 7; x += 5) {
            // Leave the central fastening clear; the title begins below this seam.
            if (x + 2 <= 81 || x >= 95) pixels.fill(x, 4, x + 2, 5, thread);
            pixels.fill(x, divider - 5, x + 2, divider - 4, thread);
        }

        // The pocket-opening band and keeper sit above the title, not over it.
        // All rectangles are integer GUI pixels; highlights have no glow/emissive effect.
        int darkMetal = mix(fastening, 0xFF242021, .44f);
        int lightMetal = mix(fastening, 0xFFFFF5DC, .24f);
        pixels.fill(65, -2, 111, 3, edge);
        pixels.fill(66, -1, 110, 2, darkMetal);
        pixels.fill(67, -1, 109, 0, lightMetal);
        pixels.fill(69, 0, 82, 2, fastening);
        pixels.fill(93, 0, 106, 2, fastening);
        pixels.fill(82, -4, 94, 5, edge);
        pixels.fill(83, -3, 93, 4, darkMetal);
        pixels.fill(83, -3, 92, -2, lightMetal);
        pixels.fill(83, -2, 91, 3, fastening);
        pixels.fill(84, -1, 87, 2, mix(fastening, lightMetal, .35f));
        pixels.fill(90, 1, 92, 4, darkMetal);
    }

    private static void drawInventoryPanel(Pixels pixels, int top, int bottom) {
        int outline = 0xFF000000;
        int bevelShadow = 0xFF555555;
        // Standalone stepped corners and the vanilla light top/left, dark bottom/right bevel.
        pixels.fill(2, top, WIDTH - 2, bottom, outline);
        pixels.fill(0, top + 2, WIDTH, bottom - 2, outline);
        pixels.fill(2, top + 1, WIDTH - 2, bottom - 1, bevelShadow);
        pixels.fill(1, top + 2, WIDTH - 1, bottom - 2, bevelShadow);
        pixels.fill(2, top + 1, WIDTH - 3, top + 3, 0xFFFFFFFF);
        pixels.fill(1, top + 3, 3, bottom - 3, 0xFFFFFFFF);
        pixels.fill(3, top + 3, WIDTH - 3, bottom - 3, INVENTORY_BACKGROUND);
    }

    /** Coordinates are the real Slot's 16x16 item origin, not a separate texture grid. */
    public void drawSlot(Pixels pixels, int x, int y, boolean backpack) {
        int interior = backpack ? mix(fabric, 0xFF151719, .32f) : INVENTORY_SLOT;
        int shadow = backpack ? mix(fabric, 0xFF131416, .61f) : INVENTORY_SLOT_SHADOW;
        int highlight = backpack ? mix(fabric, 0xFFE2D7C3, .32f) : 0xFFFFFFFF;
        pixels.fill(x - 1, y - 1, x + 17, y + 17, shadow);
        pixels.fill(x, y, x + 17, y + 17, highlight);
        pixels.fill(x, y, x + 16, y + 16, interior);
        // Bevel corners, like a small recess in the fabric rather than a metal tray.
        pixels.fill(x + 16, y - 1, x + 17, y, interior);
        pixels.fill(x - 1, y + 16, x, y + 17, interior);
    }

    private static int mix(int a, int b, float amount) {
        int r = Math.round(((a >> 16) & 255) * (1 - amount) + ((b >> 16) & 255) * amount);
        int g = Math.round(((a >> 8) & 255) * (1 - amount) + ((b >> 8) & 255) * amount);
        int blue = Math.round((a & 255) * (1 - amount) + (b & 255) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | blue;
    }
}
