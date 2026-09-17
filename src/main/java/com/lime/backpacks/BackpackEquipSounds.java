package com.lime.backpacks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import java.util.HashMap;
import java.util.Map;

/** Observe the completed slot transaction so a swap produces only one sound. */
public final class BackpackEquipSounds {
    private static final Map<ServerPlayerEntity, Snapshot> PREVIOUS = new HashMap<>();
    private BackpackEquipSounds() {}

    record Snapshot(ItemStack stack, BackpackTier tier, Object world) {}
    record Change(BackpackTier tier, boolean equipping) {}

    static Snapshot snapshot(ItemStack stack, Object world) {
        return new Snapshot(stack, stack.getItem() instanceof BackpackItem bag ? bag.getTier() : null, world);
    }

    static Change change(Snapshot previous, Snapshot current) {
        // Login, respawn and dimension transfers establish a silent baseline.
        if (previous == null || previous.world != current.world) return null;
        // Trinkets may replace the stack object during an inventory refresh.
        // Component changes are also expected while the backpack is open or its
        // lantern is toggled, so neither should replay an equip sound.
        if (previous.tier == current.tier) return null;
        if (current.tier != null) return new Change(current.tier, true);
        return previous.tier == null ? null : new Change(previous.tier, false);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var players = server.getPlayerManager().getPlayerList();
            PREVIOUS.keySet().retainAll(players);
            for (ServerPlayerEntity player : players) {
                ItemStack equipped = ItemStack.EMPTY;
                var back = TrinketsCompat.getBackInventory(player);
                if (back != null) {
                    for (int i = 0; i < back.size(); i++) {
                        if (back.getStack(i).getItem() instanceof BackpackItem) {
                            equipped = back.getStack(i);
                            break;
                        }
                    }
                }
                Snapshot current = snapshot(equipped, player.getEntityWorld());
                Snapshot previous = PREVIOUS.put(player, current);
                if (!player.isAlive() || player.isSpectator()) {
                    PREVIOUS.remove(player);
                    continue;
                }
                Change change = change(previous, current);
                if (change != null) play(player, change);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PREVIOUS.clear());
    }

    private static void play(ServerPlayerEntity player, Change change) {
        float pitch = switch (change.tier) {
            case LEATHER -> 1.12f;
            case COPPER -> 1.06f;
            case IRON -> 1.0f;
            case GOLD -> 1.08f;
            case DIAMOND -> .94f;
            case NETHERITE -> .86f;
        };
        pitch += (player.getRandom().nextFloat() - .5f) * .06f;
        play(player, change.equipping ? SoundEvents.ITEM_BUNDLE_INSERT : SoundEvents.ITEM_BUNDLE_REMOVE_ONE,
                change.equipping ? .70f : .46f, pitch);
        if (!change.equipping) {
            play(player, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), .20f, pitch * 1.12f);
            return;
        }
        SoundEvent buckle = switch (change.tier) {
            case LEATHER -> SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value();
            case COPPER -> SoundEvents.ITEM_ARMOR_EQUIP_COPPER.value();
            case IRON -> SoundEvents.ITEM_ARMOR_EQUIP_IRON.value();
            case GOLD -> SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value();
            case DIAMOND -> SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value();
            case NETHERITE -> SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value();
        };
        play(player, buckle, change.tier == BackpackTier.LEATHER ? .32f : .20f, pitch);
    }

    private static void play(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundCategory.PLAYERS, volume, pitch);
    }

}
