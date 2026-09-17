package com.lime.backpacks;

public enum BackpackTier {
    LEATHER("leather", 9, 1),
    COPPER("copper", 18, 2),
    IRON("iron", 27, 3),
    GOLD("gold", 36, 4),
    DIAMOND("diamond", 54, 6),
    NETHERITE("netherite", 81, 9);

    private final String id;
    private final int slotCount;
    private final int rows;

    BackpackTier(String id, int slotCount, int rows) {
        this.id = id;
        this.slotCount = slotCount;
        this.rows = rows;
    }

    public String getId() { return id; }
    public int getSlotCount() { return slotCount; }
    public int getRows() { return rows; }
}
