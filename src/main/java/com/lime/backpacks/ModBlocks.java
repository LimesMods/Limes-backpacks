package com.lime.backpacks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    private static final String MOD_ID = "limesbackpacks";
    public static final net.minecraft.world.level.block.SoundType BACKPACK_SOUNDS = new net.minecraft.world.level.block.SoundType(
            .70f, 1.0f,
            net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE,
            net.minecraft.sounds.SoundEvents.WOOL_STEP,
            net.minecraft.sounds.SoundEvents.BUNDLE_INSERT,
            net.minecraft.sounds.SoundEvents.WOOL_HIT,
            net.minecraft.sounds.SoundEvents.WOOL_FALL);
    public static final Block BACKPACK_BLOCK = registerBlock("placed_backpack",
            new BackpackBlock(BlockBehaviour.Properties.of()
                    .setId(blockKey("placed_backpack"))
                    .strength(0.5f)
                    .sound(BACKPACK_SOUNDS)
                    .lightLevel(state -> state.getValue(BackpackBlock.LIT) ? 15 : 0)
                    .dynamicShape()
                    .noOcclusion()));

    public static final BlockEntityType<BackpackBlockEntity> BACKPACK_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "placed_backpack"),
            FabricBlockEntityTypeBuilder.create(BackpackBlockEntity::new, BACKPACK_BLOCK).build());

    private ModBlocks() {}

    private static Block registerBlock(String id, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id), block);
    }

    private static ResourceKey<Block> blockKey(String id) {
        return ResourceKey.create(BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath(MOD_ID, id));
    }

    public static void register() {
        // Referencing this class completes the static registrations.
    }
}
