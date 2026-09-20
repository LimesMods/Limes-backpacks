package com.lime.backpacks.client;

import com.lime.backpacks.BackpackBlock;
import com.lime.backpacks.ModBlocks;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Makes lit placed backpacks use the same shader material as a vanilla lantern.
 * Iris loads shader material mappings at runtime. Optional pseudo-mixins call
 * this class only when Iris is present, so Iris remains an optional dependency.
 */
public final class IrisShaderCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("Lime's Backpacks/Iris Compat");
    private static boolean logged;
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

    public static Object2IntMap<BlockState> addLanternMaterial(Object2IntMap<BlockState> mapping) {
        if (mapping == null) return null;

        int materialId = -1;
        for (BlockState lanternState : Blocks.LANTERN.getStateDefinition().getPossibleStates()) {
            if (mapping.containsKey(lanternState)) {
                materialId = mapping.getInt(lanternState);
                break;
            }
        }
        if (materialId < 0) {
            if (!logged) {
                LOGGER.warn("Iris loaded, but the active shader pack has no vanilla lantern material mapping.");
                logged = true;
            }
            return mapping;
        }

        int mappedStates = 0;
        for (BlockState backpackState : ModBlocks.BACKPACK_BLOCK.getStateDefinition().getPossibleStates()) {
            if (backpackState.getValue(BackpackBlock.LIT)) {
                mapping.put(backpackState, materialId);
                mappedStates++;
            }
        }
        if (!logged) {
            LOGGER.info("Mapped {} lit backpack block states to Iris lantern material {}.",
                    mappedStates, materialId);
            logged = true;
        }
        return mapping;
    }

}
