package com.lime.backpacks;

import com.google.gson.JsonParser;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Uses the same mesh vertices and centering as the placed item renderer. */
public final class BackpackShapes {
    private static final VoxelShape[][] SHAPES = new VoxelShape[6][8];

    public static synchronized VoxelShape get(BackpackTier tier, int rotation) {
        if (SHAPES[tier.ordinal()][rotation] == null) load(tier);
        return SHAPES[tier.ordinal()][rotation];
    }

    private static void load(BackpackTier tier) {
        String path = "/assets/limesbackpacks/models/item/" + tier.getId() + "_backpack_worn.json";
        try (var stream = BackpackShapes.class.getResourceAsStream(path)) {
            var model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var vertices = new ArrayList<Vector3f>();
            for (var entry : model.getAsJsonArray("elements")) {
                var element = entry.getAsJsonObject();
                var from = element.getAsJsonArray("from");
                var to = element.getAsJsonArray("to");
                for (int corner = 0; corner < 8; corner++) {
                    Vector3f v = new Vector3f();
                    for (int axis = 0; axis < 3; axis++)
                        v.setComponent(axis, ((corner & (1 << axis)) == 0 ? from : to).get(axis).getAsFloat());
                    if (element.has("rotation")) {
                        var r = element.getAsJsonObject("rotation");
                        var o = r.getAsJsonArray("origin");
                        var origin = new Vector3f(o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat());
                        float angle = (float) Math.toRadians(r.get("angle").getAsDouble());
                        String axis = r.get("axis").getAsString();
                        v.sub(origin);
                        switch (axis) { case "x" -> v.rotateX(angle); case "y" -> v.rotateY(angle); case "z" -> v.rotateZ(angle); }
                        if (r.has("rescale") && r.get("rescale").getAsBoolean()) {
                            float scale = 1f / (float) Math.cos(angle);
                            v.mul(axis.equals("x") ? 1 : scale, axis.equals("y") ? 1 : scale, axis.equals("z") ? 1 : scale);
                        }
                        v.add(origin);
                    }
                    vertices.add(v);
                }
            }
            Vector3f min = new Vector3f(Float.POSITIVE_INFINITY), max = new Vector3f(Float.NEGATIVE_INFINITY);
            vertices.forEach(v -> { min.min(v); max.max(v); });
            var center = new Vector3f((min.x + max.x) / 2, min.y, (min.z + max.z) / 2);
            // Ground scale .5 multiplied by renderer scale 2 gives model pixels / 16.
            for (int rotation = 0; rotation < 8; rotation++) {
                Vector3f low = new Vector3f(Float.POSITIVE_INFINITY), high = new Vector3f(Float.NEGATIVE_INFINITY);
                float angle = (float) Math.toRadians(BackpackPlacement.modelRotationDegrees(rotation));
                for (var vertex : vertices) {
                    var v = new Vector3f(vertex).sub(center).div(16).rotateY(angle).add(.5f, 0, .5f);
                    low.min(v); high.max(v);
                }
                SHAPES[tier.ordinal()][rotation] = Shapes.box(low.x, 0, low.z, high.x, high.y, high.z);
            }
        } catch (Exception e) { throw new IllegalStateException("Cannot load backpack hitbox: " + path, e); }
    }
}
