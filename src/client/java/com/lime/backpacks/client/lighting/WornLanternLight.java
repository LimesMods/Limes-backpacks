package com.lime.backpacks.client.lighting;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class WornLanternLight implements DynamicLightBehavior {
    private Vec3d position = Vec3d.ZERO;
    private int luminance;
    private boolean changed = true;
    private boolean removed;
    private Vec3d anchor;
    private Vec3d anchorPlayerPosition;
    private int anchorAge;

    void captureAnchor(PlayerEntity player, Vec3d position) {
        anchor = position;
        anchorPlayerPosition = player.getEntityPos();
        anchorAge = player.age;
    }

    void tick(PlayerEntity player, int luminance) {
        Vec3d next = anchor != null && player.age - anchorAge <= 2
                ? anchor.add(player.getEntityPos().subtract(anchorPlayerPosition))
                : BackpackDynamicLights.fallbackPosition(player);
        update(next, luminance);
    }

    public void update(Vec3d next, int light) {
        light = MathHelper.clamp(light, 0, 15);
        changed |= next.squaredDistanceTo(position) > .000001 || luminance != light;
        position = next;
        luminance = light;
    }

    public void remove() { removed = true; changed = true; }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        if (removed) return 0;
        return Math.max(0, luminance - Vec3d.ofCenter(pos).distanceTo(position) * falloffRatio);
    }

    @Override
    public BoundingBox getBoundingBox() {
        int x = MathHelper.floor(position.x), y = MathHelper.floor(position.y), z = MathHelper.floor(position.z);
        return new BoundingBox(x, y, z, x + 1, y + 1, z + 1);
    }

    @Override
    public boolean hasChanged() { boolean result = changed; changed = false; return result; }

    @Override
    public boolean isRemoved() { return removed; }
}
