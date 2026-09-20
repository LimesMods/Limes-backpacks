package com.lime.backpacks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import java.util.HashMap;
import java.util.Map;

/** Observe the completed slot transaction so a swap produces only one sound. */
public final class BackpackEquipSounds {
    private static final Map<ServerPlayer, Snapshot> PREVIOUS = new HashMap<>();
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
            var players = server.getPlayerList().getPlayers();
            PREVIOUS.keySet().retainAll(players);
            for (ServerPlayer player : players) {
                ItemStack equipped = ItemStack.EMPTY;
                var back = TrinketsCompat.getBackInventory(player);
                if (back != null) {
                    for (int i = 0; i < back.getContainerSize(); i++) {
                        if (back.getItem(i).getItem() instanceof BackpackItem) {
                            equipped = back.getItem(i);
                            break;
                        }
                    }
                }
                Snapshot current = snapshot(equipped, player.level());
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

    private static void play(ServerPlayer player, Change change) {
        float pitch = switch (change.tier) {
            case LEATHER -> 1.12f;
            case COPPER -> 1.06f;
            case IRON -> 1.0f;
            case GOLD -> 1.08f;
            case DIAMOND -> .94f;
            case NETHERITE -> .86f;
        };
        pitch += (player.getRandom().nextFloat() - .5f) * .06f;
        play(player, change.equipping ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_REMOVE_ONE,
                change.equipping ? .70f : .46f, pitch);
        if (!change.equipping) {
            play(player, SoundEvents.ARMOR_EQUIP_LEATHER.value(), .20f, pitch * 1.12f);
            return;
        }
        SoundEvent buckle = switch (change.tier) {
            case LEATHER -> SoundEvents.ARMOR_EQUIP_LEATHER.value();
            case COPPER -> SoundEvents.ARMOR_EQUIP_COPPER.value();
            case IRON -> SoundEvents.ARMOR_EQUIP_IRON.value();
            case GOLD -> SoundEvents.ARMOR_EQUIP_GOLD.value();
            case DIAMOND -> SoundEvents.ARMOR_EQUIP_DIAMOND.value();
            case NETHERITE -> SoundEvents.ARMOR_EQUIP_NETHERITE.value();
        };
        play(player, buckle, change.tier == BackpackTier.LEATHER ? .32f : .20f, pitch);
    }

    private static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

}
