package com.lime.backpacks;

import com.lime.backpacks.gui.BackpackGuiTheme;
import java.util.ArrayList;
import java.util.List;

/** Raster checks of the same native pixel drawing used by BackpackScreen; no image files. */
public final class BackpackGuiChecks {
    public static void main(String[] args) {
        for (BackpackTier tier : BackpackTier.values()) {
            int rows = tier.getRows();
            int extra = BackpackGuiTheme.EXTRA_BINDING_HEIGHT;
            int height = 114 + rows * 18 + extra;
            int[][] canvas = new int[height + 4][176];
            BackpackGuiTheme.Pixels pixels = (left, top, right, bottom, color) -> {
                check(left >= 0 && top >= -4 && right <= 176 && bottom <= height,
                        tier + ": drawing outside panel bounds");
                check(left < right && top < bottom, "Empty pixel rectangle");
                for (int y = top; y < bottom; y++)
                    for (int x = left; x < right; x++) canvas[y + 4][x] = color;
            };
            var theme = BackpackGuiTheme.forRows(rows);
            check(theme.equals(BackpackGuiTheme.forTier(tier)), "Wrong tier mapping");
            theme.drawPanel(pixels, rows);
            check(contrast(BackpackGuiTheme.TITLE, canvas[10][88]) >= 4.5, tier + ": title contrast");
            check(contrast(BackpackGuiTheme.INVENTORY_TITLE, canvas[rows * 18 + 25 + extra][88]) >= 4.5,
                    tier + ": player inventory title contrast");
            int divider = rows * 18 + 19 + extra;
            check(divider - (rows * 18 + 17) >= 8, "Bottom hem must have room for stitching");
            check(canvas[8][7] != canvas[8][9], "Missing top stitch/gap pattern");
            check(canvas[divider - 1][7] != canvas[divider - 1][9], "Missing bottom stitch/gap pattern");
            check(canvas[8][83] != canvas[8][7], "Top seam must not cover the fastening");
            check(canvas[divider + 4][88] == 0xFF000000, "Missing separate inventory outline");
            check(canvas[divider + 5][88] == 0xFFFFFFFF, "Missing inventory top bevel");
            check(canvas[divider + 7][88] == 0xFFC6C6C6, "Inventory background must be vanilla grey");
            check(canvas[divider + 3][88] != canvas[divider + 5][88], "Bag binding blends into inventory");
            check(canvas[height][1] == 0xFFFFFFFF, "Inventory left frame must not use tier colors");
            check(canvas[height][174] == 0xFF555555, "Inventory right frame must use vanilla shadow");
            List<int[]> slots = new ArrayList<>();
            for (int row = 0; row < rows; row++)
                for (int col = 0; col < 9; col++) slots.add(new int[]{8 + col * 18, 18 + row * 18});
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) slots.add(new int[]{8 + col * 18, rows * 18 + 32 + extra + row * 18});
            for (int col = 0; col < 9; col++) slots.add(new int[]{8 + col * 18, rows * 18 + 90 + extra});
            check(slots.size() == tier.getSlotCount() + 36, "Capacity changed");
            for (int i = 0; i < slots.size(); i++) {
                int[] slot = slots.get(i);
                theme.drawSlot(pixels, slot[0], slot[1], i < tier.getSlotCount());
            }
            for (int i = 0; i < slots.size(); i++) {
                int x = slots.get(i)[0], y = slots.get(i)[1];
                int interior = canvas[y + 4][x];
                for (int dy = 0; dy < 16; dy++)
                    for (int dx = 0; dx < 16; dx++)
                        check(canvas[y + dy + 4][x + dx] == interior,
                                tier + ": border intrudes into item/count area at slot " + i);
                if (i < tier.getSlotCount()) {
                    check(contrast(0xFFFFFFFF, interior) >= 4.5, tier + ": stack count contrast");
                } else {
                    // Vanilla relies on the count text's dark drop shadow on grey slots.
                    check(interior == 0xFF8B8B8B, "Player slot must use vanilla grey");
                    check(canvas[y + 3][x] == 0xFF373737, "Player slot top must use vanilla shadow");
                    check(canvas[y + 20][x] == 0xFFFFFFFF, "Player slot bottom must use vanilla highlight");
                }
                check(luminance(canvas[y + 3][x]) < luminance(interior), "Missing top shadow");
                check(luminance(canvas[y + 20][x]) > luminance(interior), "Missing bottom bevel");
            }
            System.out.println("PASS " + tier + ": " + slots.size()
                    + " aligned slots, readable titles/counts, fastening and panel bounds.");
        }
    }

    private static double luminance(int color) {
        double[] channel = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = ((color >> (16 - i * 8)) & 255) / 255.0;
            channel[i] = c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4);
        }
        return .2126 * channel[0] + .7152 * channel[1] + .0722 * channel[2];
    }

    private static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
