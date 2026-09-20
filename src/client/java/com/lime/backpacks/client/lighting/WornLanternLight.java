package com.lime.backpacks.client.lighting;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WornLanternLight implements DynamicLightBehavior {
    private Vec3 position = Vec3.ZERO;
    private int luminance;
    private boolean changed = true;
    private boolean removed;
    private Vec3 anchor;
    private Vec3 anchorPlayerPosition;
    private int anchorAge;

    void captureAnchor(Player player, Vec3 position) {
        anchor = position;
        anchorPlayerPosition = player.position();
        anchorAge = player.tickCount;
    }

    void tick(Player player, int luminance) {
        Vec3 next = anchor != null && player.tickCount - anchorAge <= 2
                ? anchor.add(player.position().subtract(anchorPlayerPosition))
                : BackpackDynamicLights.fallbackPosition(player);
        update(next, luminance);
    }

    public void update(Vec3 next, int light) {
        light = Mth.clamp(light, 0, 15);
        changed |= next.distanceToSqr(position) > .000001 || luminance != light;
        position = next;
        luminance = light;
    }

    public void remove() { removed = true; changed = true; }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        if (removed) return 0;
        return Math.max(0, luminance - Vec3.atCenterOf(pos).distanceTo(position) * falloffRatio);
    }

    @Override
    public BoundingBox getBoundingBox() {
        int x = Mth.floor(position.x), y = Mth.floor(position.y), z = Mth.floor(position.z);
        return new BoundingBox(x, y, z, x + 1, y + 1, z + 1);
    }

    @Override
    public boolean hasChanged() { boolean result = changed; changed = false; return result; }

    @Override
    public boolean isRemoved() { return removed; }
}
