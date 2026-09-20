package com.lime.backpacks;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class QuiverChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static ItemStack bag(Item item, ItemStack arrow) {
        ItemStack bag = new ItemStack(item);
        var contents = NonNullList.withSize(81, ItemStack.EMPTY);
        contents.set(80, arrow);
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return bag;
    }

    public static void run() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ModItems.register();
        var stackableComponents = net.minecraft.core.component.DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .build();
        for (Item item : List.of(Items.ARROW, Items.FIREWORK_ROCKET, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW)) {
            item.builtInRegistryHolder().bindComponents(stackableComponents);
        }
        for (Item item : List.of(ModItems.LEATHER_BACKPACK, ModItems.COPPER_BACKPACK, ModItems.IRON_BACKPACK,
                ModItems.GOLD_BACKPACK, ModItems.DIAMOND_BACKPACK, ModItems.NETHERITE_BACKPACK)) {
            item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
        }
        // Tags normally arrive from world datapacks; bind vanilla arrows for this world-free test.
        var tags = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
        tags.setAccessible(true);
        for (Item item : List.of(Items.ARROW, Items.TIPPED_ARROW, Items.SPECTRAL_ARROW))
            tags.invoke(BuiltInRegistries.ITEM.wrapAsHolder(item), List.of(ItemTags.ARROWS));
        tags.invoke(BuiltInRegistries.ITEM.wrapAsHolder(Items.FIREWORK_ROCKET), List.of());

        // Force all three mixin targets through Fabric's transformer.
        check(QuiverUser.class.isAssignableFrom(Class.forName("net.minecraft.world.entity.player.Player")), "Player mixin applied");
        for (String target : List.of("net.minecraft.world.item.ProjectileWeaponItem", "net.minecraft.client.renderer.item.ItemModelResolver")) {
            check(java.util.Arrays.stream(Class.forName(target).getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("limesbackpacks$")), "Mixin applied: " + target);
        }
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            Class.forName("com.lime.backpacks.client.BackpackRenderer");
            Class.forName("com.lime.backpacks.TrinketsCompat");
        }

        var inventory = new SimpleContainer(3);
        inventory.setItem(0, bag(ModItems.DIAMOND_BACKPACK, new ItemStack(Items.ARROW, 64)));
        check(QuiverAmmo.findInInventory(inventory, s -> true) == null, "Diamond must not supply ammunition");
        inventory.setItem(1, bag(ModItems.NETHERITE_BACKPACK, new ItemStack(Items.ARROW, 2)));
        var source = QuiverAmmo.findInInventory(inventory, s -> true);
        check(source != null && source.arrowSlot() == 80, "Search all 81 backpack slots");
        check(QuiverAmmo.hasStoredArrows(inventory.getItem(1)), "Loaded quiver visible");
        check(QuiverAmmo.consume(source, 1).getCount() == 1, "Consume one arrow");
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        check(source.projectile().getCount() == 1, "Consumption persisted in container component");
        check(QuiverAmmo.consume(source, 1).getCount() == 1, "Last arrow fires");
        check(!QuiverAmmo.hasStoredArrows(inventory.getItem(1)), "Empty quiver hides arrows");
        check(QuiverAmmo.findInInventory(inventory, s -> true) == null, "No phantom arrows after depletion");
        check(QuiverAmmo.consume(source, 1).isEmpty(), "Cannot consume the last arrow twice");

        ItemStack tipped = new ItemStack(Items.TIPPED_ARROW, 3);
        tipped.set(DataComponents.CUSTOM_NAME, Component.literal("Test arrow"));
        tipped.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(0x765432), List.of(), Optional.empty()));
        inventory.setItem(1, bag(ModItems.NETHERITE_BACKPACK, tipped));
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        ItemStack shot = QuiverAmmo.consume(source, 1);
        check(ItemStack.isSameItemSameComponents(shot, tipped), "Preserve tipped arrow components");
        check(QuiverAmmo.findInInventory(inventory, s -> s.is(Items.ARROW)) == null, "Honor weapon ammunition predicate");
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        inventory.setItem(1, bag(ModItems.NETHERITE_BACKPACK, new ItemStack(Items.SPECTRAL_ARROW, 4)));
        check(QuiverAmmo.consume(source, 1).isEmpty(), "Cannot consume from a removed or swapped bag");
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        check(QuiverAmmo.consume(source, 1).is(Items.SPECTRAL_ARROW), "Spectral arrows supported");
        inventory.setItem(1, bag(ModItems.NETHERITE_BACKPACK, new ItemStack(Items.FIREWORK_ROCKET, 5)));
        check(QuiverAmmo.findInInventory(inventory, s -> true) == null, "Quiver supplies arrows only");
        System.out.println("PASS: quiver selection, last arrow, persistence, special arrows, stale bag protection and Fabric mixin application");
    }
}
