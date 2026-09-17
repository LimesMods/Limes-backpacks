package com.lime.backpacks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    private static final String MOD_ID = "limesbackpacks";
    public static final net.minecraft.sound.BlockSoundGroup BACKPACK_SOUNDS = new net.minecraft.sound.BlockSoundGroup(
            .70f, 1.0f,
            net.minecraft.sound.SoundEvents.ITEM_BUNDLE_REMOVE_ONE,
            net.minecraft.sound.SoundEvents.BLOCK_WOOL_STEP,
            net.minecraft.sound.SoundEvents.ITEM_BUNDLE_INSERT,
            net.minecraft.sound.SoundEvents.BLOCK_WOOL_HIT,
            net.minecraft.sound.SoundEvents.BLOCK_WOOL_FALL);
    public static final Block BACKPACK_BLOCK = registerBlock("placed_backpack",
            new BackpackBlock(Block.Settings.create()
                    .registryKey(blockKey("placed_backpack"))
                    .strength(0.5f)
                    .sounds(BACKPACK_SOUNDS)
                    .dynamicBounds()
                    .nonOpaque()));

    public static final BlockEntityType<BackpackBlockEntity> BACKPACK_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MOD_ID, "placed_backpack"),
            FabricBlockEntityTypeBuilder.create(BackpackBlockEntity::new, BACKPACK_BLOCK).build());

    private ModBlocks() {}

    private static Block registerBlock(String id, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, id), block);
    }

    private static RegistryKey<Block> blockKey(String id) {
        return RegistryKey.of(Registries.BLOCK.getKey(), Identifier.of(MOD_ID, id));
    }

    public static void register() {
        // Referencing this class completes the static registrations.
    }
}
