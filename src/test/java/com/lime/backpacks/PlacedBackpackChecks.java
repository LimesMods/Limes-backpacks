package com.lime.backpacks;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PlacedBackpackChecks {
    public static void run() {
        ModBlocks.register();
        checkShapesAndParticles();
        checkClientSync();
        checkBreakLifecycle();

        var defaultState = ModBlocks.BACKPACK_BLOCK.defaultBlockState();
        check(defaultState.getValue(BackpackBlock.ROTATION) == 0, "Placed backpack missing default rotation");
        check(ModBlocks.BACKPACK_BLOCK_ENTITY != null, "Placed backpack block entity was not registered");

        var entity = new BackpackBlockEntity(new BlockPos(0, 64, 0), defaultState);
        var backpack = new ItemStack(ModItems.NETHERITE_BACKPACK);
        entity.setBackpack(backpack);
        check(entity.getContainerSize() == BackpackTier.NETHERITE.getSlotCount(), "Placed backpack lost tier capacity");
        entity.setItem(0, new ItemStack(Items.DIAMOND, 3));
        var lootContext = new net.minecraft.world.level.storage.loot.LootParams.Builder(null)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, entity);
        var drops = ((BackpackBlock) ModBlocks.BACKPACK_BLOCK).getDrops(defaultState, lootContext);
        check(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.getFirst(), entity.getBackpack()),
                "Mining must return exactly one backpack with contents");
        check(drops.getFirst() != entity.getBackpack(), "Drop must be independent of removed entity");
        for (int step = 0; step < 8; step++) {
            float yaw = step * 45f;
            int rotation = Math.floorMod(Math.round((yaw + 180f) / 45f), 8);
            var facing = new org.joml.Vector3f(0, 0, -1).rotateY(
                    (float) Math.toRadians(BackpackPlacement.modelRotationDegrees(rotation)));
            double radians = Math.toRadians(yaw);
            check(Math.abs(facing.x - Math.sin(radians)) < 0.00001
                    && Math.abs(facing.z + Math.cos(radians)) < 0.00001,
                    "Pocket side must face placer at yaw " + yaw);
        }
        check(entity.getItem(0).getCount() == 3, "Placed backpack inventory did not accept an item");
        check(entity.getSlotsForFace(Direction.UP).length == 81, "Hopper slots do not expose the full backpack");
        check(entity.canPlaceItemThroughFace(1, new ItemStack(Items.DIRT), Direction.DOWN), "Hopper insertion rejected normal item");
        check(!entity.canPlaceItemThroughFace(1, new ItemStack(ModItems.LEATHER_BACKPACK), Direction.DOWN),
                "Placed backpack allows nested backpacks");

        var restored = entity.getBackpack().copy();
        var restoredInventory = new BackpackInventory(restored, 81);
        check(restoredInventory.getItem(0).getCount() == 3, "Placed backpack did not save its contents");
        check(restored.getItem() == ModItems.NETHERITE_BACKPACK, "Placed backpack changed tier");
        check(ModBlocks.BACKPACK_BLOCK.defaultBlockState().setValue(BackpackBlock.ROTATION, 7)
                .getValue(BackpackBlock.ROTATION) == 7, "45-degree rotation states are unavailable");
        System.out.println("PASS: placed backpacks preserve tier/contents and expose safe hopper inventory slots.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void checkBreakLifecycle() {
        var state = ModBlocks.BACKPACK_BLOCK.defaultBlockState();
        check(state.getSoundType().getBreakSound() == net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE,
                "Breaking still uses the default block sound");
        check(state.getSoundType().getPlaceSound() == net.minecraft.sounds.SoundEvents.BUNDLE_INSERT,
                "Placement does not use the backpack sound");
        for (var item : new net.minecraft.world.item.Item[]{ModItems.LEATHER_BACKPACK, ModItems.COPPER_BACKPACK,
                ModItems.IRON_BACKPACK, ModItems.GOLD_BACKPACK, ModItems.DIAMOND_BACKPACK, ModItems.NETHERITE_BACKPACK}) {
            var entity = new BackpackBlockEntity(BlockPos.ZERO, state);
            var stack = new ItemStack(item);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("My pack"));
            if (BackpackLantern.hasLantern(stack)) BackpackLantern.toggle(stack);
            entity.setBackpack(stack);
            entity.setItem(0, new ItemStack(Items.DIAMOND, 3));
            entity.setItem(entity.getContainerSize() - 1, new ItemStack(Items.ARROW, 64));
            // A live slot mutation must be flushed before the block is removed.
            entity.getItem(0).shrink(1);
            entity.preRemoveSideEffects(BlockPos.ZERO, state);
            var context = new net.minecraft.world.level.storage.loot.LootParams.Builder(null)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, entity);
            var drops = ((BackpackBlock) ModBlocks.BACKPACK_BLOCK).getDrops(state, context);
            check(drops.size() == 1, "Breaking must return one complete backpack");
            var dropped = drops.getFirst();
            var inventory = new BackpackInventory(dropped, entity.getContainerSize());
            check(inventory.getItem(0).getCount() == 2
                    && inventory.getItem(entity.getContainerSize() - 1).getCount() == 64,
                    "Breaking lost contents at the first or last slot");
            check(ItemStack.isSameItemSameComponents(dropped, entity.getBackpack()),
                    "Breaking lost name, lantern state or other components");
        }
        System.out.println("PASS: six tiers retain contents/metadata across removal and loot; block sounds use bundle events.");
    }

    private static void checkShapesAndParticles() {
        for (var tier : BackpackTier.values()) {
            for (int rotation = 0; rotation < 8; rotation++) {
                var box = BackpackShapes.get(tier, rotation).bounds();
                check(Math.abs(box.minY) < .000001, "Hitbox floats above floor");
                check(box.maxY > .3 && box.maxY < 1.1, "Unexpected tier height");
                // Asymmetric attachments can shift diagonal bounds; opposite
                // rotations must mirror them around the same block-center pivot.
                var opposite = BackpackShapes.get(tier, (rotation + 4) % 8).bounds();
                check(Math.abs(box.minX + opposite.maxX - 1) < .000001
                        && Math.abs(box.minZ + opposite.maxZ - 1) < .000001, "Hitbox rotation pivot is wrong");
                check(box.getXsize() < 1 && box.getZsize() < 1, "Hitbox larger than block");
            }
            String root = "/assets/limesbackpacks/";
            try (var stream = PlacedBackpackChecks.class.getResourceAsStream(root + "textures/block/" + tier.getId() + "_fabric.png");
                 var model = PlacedBackpackChecks.class.getResourceAsStream(root + "models/block/placed_backpack_" + tier.getId() + ".json")) {
                check(stream != null && model != null, "Missing tier particle assets");
                var image = javax.imageio.ImageIO.read(stream);
                check(image != null && image.getWidth() == 16 && image.getHeight() == 16, "Invalid fabric sprite");
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
                    check((image.getRGB(x,y) >>> 24) == 255, "Transparent fabric particle");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        check(BackpackShapes.get(BackpackTier.LEATHER, 0).bounds().maxY
                < BackpackShapes.get(BackpackTier.NETHERITE, 0).bounds().maxY, "Tier sizes are identical");
        System.out.println("PASS: 48 grounded, centered tier/rotation hitboxes and six opaque fabric sprites.");
    }

    private static void checkClientSync() {
        var registries = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        for (var item : new net.minecraft.world.item.Item[]{ModItems.LEATHER_BACKPACK, ModItems.COPPER_BACKPACK,
                ModItems.IRON_BACKPACK, ModItems.GOLD_BACKPACK, ModItems.DIAMOND_BACKPACK, ModItems.NETHERITE_BACKPACK}) {
            var state = ModBlocks.BACKPACK_BLOCK.defaultBlockState();
            var server = new BackpackBlockEntity(BlockPos.ZERO, state);
            var client = new BackpackBlockEntity(BlockPos.ZERO, state);
            var stack = new ItemStack(item);
            if (BackpackLantern.hasLantern(stack)) BackpackLantern.toggle(stack);
            server.setBackpack(stack);
            server.setItem(0, new ItemStack(Items.ARROW, 12));
            var data = server.getUpdateTag(registries);
            check(data.contains("Backpack"), "Client receives no backpack model data");
            client.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, data));
            check(!client.getBackpack().isEmpty(), "Renderer receives empty backpack");
            check(ItemStack.isSameItemSameComponents(server.getBackpack(), client.getBackpack()),
                    "Client lost tier, contents or lantern state");
            server.getItem(0).shrink(1);
            server.setChanged();
            client.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries,
                    server.getUpdateTag(registries)));
            check(client.getItem(0).getCount() == 11, "Direct slot mutation was not synchronized (server="
                    + server.getItem(0).getCount() + ", client=" + client.getItem(0).getCount() + ")");
            server.takeBackpack();
            client.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries,
                    server.getUpdateTag(registries)));
            check(client.getBackpack().isEmpty(), "Client retained removed backpack");
        }
        System.out.println("PASS: all six placed backpack tiers synchronize model data, contents, lantern state and removal.");
    }
}
