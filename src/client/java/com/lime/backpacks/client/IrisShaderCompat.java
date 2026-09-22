package com.lime.backpacks.client;

import com.lime.backpacks.BackpackBlock;
import com.lime.backpacks.ModBlocks;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Keeps backpack lanterns consistent with vanilla lanterns in Iris shader packs
 * without making Iris a required dependency.
 *
 * <p>Placed lit backpack states inherit the vanilla lantern block material so
 * Complementary uses the lantern's warm light color instead of nearby emissive
 * ore colors. The visible glow remains a separate four-pane item model.</p>
 */
public final class IrisShaderCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("Lime's Backpacks/Iris Compat");
    private static boolean blockMaterialLogged;
    private static Method currentPackMethod;
    private static Method irisConfigMethod;
    private static Method shadersEnabledMethod;
    private static boolean currentPackMethodResolved;

    private IrisShaderCompat() {}

    /**
     * Returns whether Iris is currently rendering a shader pack. Reflection
     * keeps Iris optional while still allowing the worn-light implementation
     * to avoid feeding the same lantern through both Iris and LambDynamicLights.
     */
    public static boolean isShaderPackActive() {
        if (!currentPackMethodResolved) {
            currentPackMethodResolved = true;
            try {
                currentPackMethod = Class.forName("net.irisshaders.iris.Iris")
                        .getMethod("getCurrentPack");
                irisConfigMethod = Class.forName("net.irisshaders.iris.Iris")
                        .getMethod("getIrisConfig");
                shadersEnabledMethod = Class.forName("net.irisshaders.iris.config.IrisConfig")
                        .getMethod("areShadersEnabled");
            } catch (ReflectiveOperationException ignored) {
                currentPackMethod = null;
                irisConfigMethod = null;
                shadersEnabledMethod = null;
            }
        }
        if (currentPackMethod == null || irisConfigMethod == null || shadersEnabledMethod == null) return false;
        try {
            Object config = irisConfigMethod.invoke(null);
            return config != null && Boolean.TRUE.equals(shadersEnabledMethod.invoke(config))
                    && currentPackMethod.invoke(null) instanceof java.util.Optional<?> pack
                    && pack.isPresent();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Gives a placed, lit backpack the same block-light classification as a
     * vanilla lantern. This controls Complementary's emitted light color; the
     * glow-only item model below still controls which visible faces bloom.
     */
    public static Object2IntMap<BlockState> addLanternMaterial(Object2IntMap<BlockState> mapping) {
        if (mapping == null) return null;

        int materialId = -1;
        for (BlockState lanternState : Blocks.LANTERN.getStateManager().getStates()) {
            if (mapping.containsKey(lanternState)) {
                materialId = mapping.getInt(lanternState);
                break;
            }
        }
        if (materialId < 0) {
            if (!blockMaterialLogged) {
                LOGGER.warn("Iris loaded, but the active shader pack has no vanilla lantern material mapping.");
                blockMaterialLogged = true;
            }
            return mapping;
        }

        int mappedStates = 0;
        for (BlockState backpackState : ModBlocks.BACKPACK_BLOCK.getStateManager().getStates()) {
            if (backpackState.get(BackpackBlock.LIT)) {
                mapping.put(backpackState, materialId);
                mappedStates++;
            }
        }
        if (!blockMaterialLogged) {
            LOGGER.info("Mapped {} lit backpack block states to Iris lantern material {}.",
                    mappedStates, materialId);
            blockMaterialLogged = true;
        }
        return mapping;
    }

    /**
     * Teaches Iris that the two custom glow-only model IDs use the same shader
     * material as a vanilla lantern. Iris prefers an item's model component ID
     * over its registry item ID, so constructing the overlay from Items.LANTERN
     * is not sufficient by itself.
     */
    public static Object2IntFunction<Object> addLanternOverlayMaterials(
            Object2IntFunction<Object> mapping) {
        if (mapping == null) return null;
        try {
            Class<?> namespacedId = Class.forName(
                    "net.irisshaders.iris.shaderpack.materialmap.NamespacedId");
            Constructor<?> constructor = namespacedId.getConstructor(String.class, String.class);
            Object lantern = constructor.newInstance("minecraft", "lantern");
            Object diamondGlow = constructor.newInstance(
                    "limesbackpacks", "diamond_backpack_lantern_glow");
            Object netheriteGlow = constructor.newInstance(
                    "limesbackpacks", "netherite_backpack_lantern_glow");
            int lanternMaterial = mapping.getInt(lantern);
            if (!mapping.containsKey(lantern)) return mapping;

            return new Object2IntFunction<>() {
                @Override
                public int getInt(Object key) {
                    if (diamondGlow.equals(key) || netheriteGlow.equals(key)) return lanternMaterial;
                    return mapping.getInt(key);
                }

                @Override
                public boolean containsKey(Object key) {
                    return diamondGlow.equals(key) || netheriteGlow.equals(key) || mapping.containsKey(key);
                }

                @Override
                public int defaultReturnValue() {
                    return mapping.defaultReturnValue();
                }
            };
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return mapping;
        }
    }

}
