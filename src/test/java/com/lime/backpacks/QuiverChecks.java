package com.lime.backpacks;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class QuiverChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static ItemStack bag(Item item, ItemStack arrow) {
        ItemStack bag = new ItemStack(item);
        var contents = DefaultedList.ofSize(81, ItemStack.EMPTY);
        contents.set(80, arrow);
        bag.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(contents));
        return bag;
    }

    public static void run() throws Exception {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        ModItems.register();
        // Tags normally arrive from world datapacks; bind vanilla arrows for this world-free test.
        var tags = RegistryEntry.Reference.class.getDeclaredMethod("setTags", Collection.class);
        tags.setAccessible(true);
        for (Item item : List.of(Items.ARROW, Items.TIPPED_ARROW, Items.SPECTRAL_ARROW))
            tags.invoke(Registries.ITEM.getEntry(item), List.of(ItemTags.ARROWS));
        tags.invoke(Registries.ITEM.getEntry(Items.FIREWORK_ROCKET), List.of());

        // Force all three mixin targets through Fabric's transformer.
        check(QuiverUser.class.isAssignableFrom(Class.forName("net.minecraft.entity.player.PlayerEntity")), "Player mixin applied");
        for (String target : List.of("net.minecraft.item.RangedWeaponItem", "net.minecraft.client.item.ItemModelManager")) {
            check(java.util.Arrays.stream(Class.forName(target).getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("limesbackpacks$")), "Mixin applied: " + target);
        }
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            Class.forName("com.lime.backpacks.client.BackpackRenderer");
            Class.forName("com.lime.backpacks.TrinketsCompat");
        }

        var inventory = new SimpleInventory(3);
        inventory.setStack(0, bag(ModItems.DIAMOND_BACKPACK, new ItemStack(Items.ARROW, 64)));
        check(QuiverAmmo.findInInventory(inventory, s -> true) == null, "Diamond must not supply ammunition");
        inventory.setStack(1, bag(ModItems.NETHERITE_BACKPACK, new ItemStack(Items.ARROW, 2)));
        var source = QuiverAmmo.findInInventory(inventory, s -> true);
        check(source != null && source.arrowSlot() == 80, "Search all 81 backpack slots");
        check(QuiverAmmo.hasStoredArrows(inventory.getStack(1)), "Loaded quiver visible");
        check(QuiverAmmo.consume(source, 1).getCount() == 1, "Consume one arrow");
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        check(source.projectile().getCount() == 1, "Consumption persisted in container component");
        check(QuiverAmmo.consume(source, 1).getCount() == 1, "Last arrow fires");
        check(!QuiverAmmo.hasStoredArrows(inventory.getStack(1)), "Empty quiver hides arrows");
        check(QuiverAmmo.findInInventory(inventory, s -> true) == null, "No phantom arrows after depletion");
        check(QuiverAmmo.consume(source, 1).isEmpty(), "Cannot consume the last arrow twice");

        ItemStack tipped = new ItemStack(Items.TIPPED_ARROW, 3);
        tipped.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Test arrow"));
        tipped.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(0x765432), List.of(), Optional.empty()));
        inventory.setStack(1, bag(ModItems.NETHERITE_BACKPACK, tipped));
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        ItemStack shot = QuiverAmmo.consume(source, 1);
        check(ItemStack.areItemsAndComponentsEqual(shot, tipped), "Preserve tipped arrow components");
        check(QuiverAmmo.findInInventory(inventory, s -> s.isOf(Items.ARROW)) == null, "Honor weapon ammunition predicate");
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        inventory.setStack(1, bag(ModItems.NETHERITE_BACKPACK, new ItemStack(Items.SPECTRAL_ARROW, 4)));
        check(QuiverAmmo.consume(source, 1).isEmpty(), "Cannot consume from a removed or swapped bag");
        source = QuiverAmmo.findInInventory(inventory, s -> true);
        check(QuiverAmmo.consume(source, 1).isOf(Items.SPECTRAL_ARROW), "Spectral arrows supported");
        inventory.setStack(1, bag(ModItems.NETHERITE_BACKPACK, new ItemStack(Items.FIREWORK_ROCKET, 5)));
        check(QuiverAmmo.findInInventory(inventory, s -> true) == null, "Quiver supplies arrows only");
        System.out.println("PASS: quiver selection, last arrow, persistence, special arrows, stale bag protection and Fabric mixin application");
    }
}
