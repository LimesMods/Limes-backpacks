package com.lime.backpacks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class LanternChecks {
    public static void run() throws Exception {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        ModItems.register();
        PlacedBackpackChecks.run();
        BackpackSoundChecks.run();
        check(!BackpackLantern.hasLantern(ItemStack.EMPTY), "Empty stack emits light");
        check(!BackpackLantern.hasLantern(new ItemStack(Items.LANTERN)), "Loose lantern classified as backpack");
        for (var item : new net.minecraft.item.Item[]{ModItems.LEATHER_BACKPACK, ModItems.COPPER_BACKPACK,
                ModItems.IRON_BACKPACK, ModItems.GOLD_BACKPACK}) {
            check(!BackpackLantern.hasLantern(new ItemStack(item)), "Lower tier emits light");
        }
        check(BackpackLantern.hasLantern(new ItemStack(ModItems.DIAMOND_BACKPACK)), "Diamond not lit");
        check(BackpackLantern.hasLantern(new ItemStack(ModItems.NETHERITE_BACKPACK)), "Netherite not lit");
        checkPerBackpackState();

        // No optional API should be needed to load the normal client entrypoint or models.
        Class.forName("com.lime.backpacks.client.LimesBackpacksClient");
        var deserialize = Class.forName("net.minecraft.client.render.model.json.JsonUnbakedModel")
                .getMethod("deserialize", java.io.Reader.class);
        for (String name : new String[]{"diamond_backpack", "diamond_backpack_worn",
                "netherite_backpack", "netherite_backpack_worn", "netherite_backpack_empty_quiver"}) {
            try (var stream = LanternChecks.class.getClassLoader().getResourceAsStream(
                    "assets/limesbackpacks/models/item/" + name + ".json")) {
                check(stream != null, "Missing lantern model " + name);
                String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                check(deserialize.invoke(null, new java.io.StringReader(json)) != null, "Invalid Minecraft model");
                var elements = com.google.gson.JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("elements");
                int glowing = 0;
                for (var element : elements) {
                    var cube = element.getAsJsonObject();
                    if (cube.has("light_emission") && cube.get("light_emission").getAsInt() > 0) {
                        check(cube.get("name").getAsString().startsWith("lantern_glow_"), "Frame/fabric became emissive");
                        glowing++;
                    }
                }
                check(glowing == 4, "Expected only four glowing lantern windows");
            }
        }
        System.out.println("PASS: all five lantern models deserialize with four emissive windows each.");
        for (String name : new String[]{"diamond_backpack", "netherite_backpack", "netherite_backpack_empty_quiver"}) {
            try (var stream = LanternChecks.class.getClassLoader().getResourceAsStream(
                    "assets/limesbackpacks/models/item/" + name + "_unlit.json")) {
                check(stream != null, "Missing unlit variant");
                String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                check(deserialize.invoke(null, new java.io.StringReader(json)) != null, "Invalid unlit model");
                var elements = com.google.gson.JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("elements");
                int windows = 0;
                for (var element : elements) {
                    var cube = element.getAsJsonObject();
                    check(!cube.has("light_emission") || cube.get("light_emission").getAsInt() == 0, "Unlit model emits light");
                    String part = cube.get("name").getAsString();
                    if (name.endsWith("empty_quiver")) check(!part.startsWith("arrow_"), "Unlit empty quiver gained arrows");
                    if (part.startsWith("lantern_unlit_")) {
                        windows++;
                        check(cube.get("shade").getAsBoolean(), "Unlit window lacks shading");
                        for (var face : cube.getAsJsonObject("faces").asMap().values()) {
                            check(face.getAsJsonObject().get("texture").getAsString().equals("#unlit_glass"), "Flame still visible");
                        }
                    }
                }
                check(windows == 4, "Missing dark windows");
            }
        }
        System.out.println("PASS: all three unlit models have shaded dark windows and preserve quiver state.");
        if (FabricLoader.getInstance().isModLoaded("lambdynlights_api")) {
            Class<?> sourceType = Class.forName("dev.lambdaurora.lambdynlights.api.item.ItemLightSource");
            Object source = sourceType.getConstructor(net.minecraft.predicate.item.ItemPredicate.class, int.class)
                    .newInstance(net.minecraft.predicate.item.ItemPredicate.Builder.create().build(), 15);
            var luminance = sourceType.getMethod("getLuminance", ItemStack.class);
            check((int) luminance.invoke(source, new ItemStack(ModItems.DIAMOND_BACKPACK)) == 15, "On backpack should emit light");
            for (var backpack : new net.minecraft.item.Item[]{ModItems.DIAMOND_BACKPACK, ModItems.NETHERITE_BACKPACK}) {
                ItemStack off = new ItemStack(backpack);
                ItemStack other = new ItemStack(backpack);
                BackpackLantern.toggle(off);
                check((int) luminance.invoke(source, off) == 0, "Off held/dropped backpack still emits light");
                check((int) luminance.invoke(source, other) == 15, "Toggle affected another backpack of the same tier");
                check((int) luminance.invoke(source, new ItemStack(Items.LANTERN)) == 15, "Toggle affected vanilla lantern");
                BackpackLantern.toggle(off);
                check((int) luminance.invoke(source, off) == 15, "Turning back on failed");
            }
            System.out.println("PASS: real optional API mixin isolates each backpack's light state and preserves vanilla light.");
            Class<?> type = Class.forName("com.lime.backpacks.client.lighting.WornLanternLight");
            Object light = type.getConstructor().newInstance();
            var update = type.getMethod("update", Vec3d.class, int.class);
            var sample = type.getMethod("lightAtPos", BlockPos.class, double.class);
            var changed = type.getMethod("hasChanged");
            update.invoke(light, new Vec3d(.5, 64.5, .5), 15);
            check((double) sample.invoke(light, new BlockPos(0,64,0), 1d) == 15, "Source brightness");
            check((double) sample.invoke(light, new BlockPos(5,64,0), 1d) == 10, "Light falloff");
            check((double) sample.invoke(light, new BlockPos(20,64,0), 1d) == 0, "Light range");
            check((boolean) changed.invoke(light), "Initial update missing");
            check(!(boolean) changed.invoke(light), "Idle light must not rebuild constantly");
            update.invoke(light, new Vec3d(-2.5, 64.5, .5), 15);
            check((boolean) changed.invoke(light), "Moving source not invalidated");
            var bounds = type.getMethod("getBoundingBox").invoke(light);
            check((int) bounds.getClass().getMethod("startX").invoke(bounds) == -3, "Negative-coordinate bounds");
            update.invoke(light, new Vec3d(.5, 64.5, .5), 0);
            check((double) sample.invoke(light, new BlockPos(0,64,0), 1d) == 0, "Disabled light remains lit");
            type.getMethod("remove").invoke(light);
            check((boolean) type.getMethod("isRemoved").invoke(light), "Light removal");
            check((double) sample.invoke(light, new BlockPos(0,64,0), 1d) == 0, "Ghost light after removal");
            Class.forName("com.lime.backpacks.client.lighting.BackpackDynamicLights").getConstructor().newInstance();
            System.out.println("PASS optional lighting API: source brightness, movement, falloff, bounds and removal.");
        } else {
            System.out.println("PASS without LambDynamicLights/API: normal backpack classes load and tier rules work.");
        }
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            Class.forName("com.lime.backpacks.client.BackpackRenderer");
            System.out.println("PASS Trinkets renderer links successfully.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void checkPerBackpackState() {
        var equipped = new ItemStack(ModItems.NETHERITE_BACKPACK);
        var main = new ItemStack(ModItems.DIAMOND_BACKPACK);
        var off = new ItemStack(ModItems.NETHERITE_BACKPACK);
        var stored = new ItemStack(ModItems.DIAMOND_BACKPACK);
        var back = new net.minecraft.inventory.SimpleInventory(1);
        back.setStack(0, equipped);
        var target = BackpackLantern.selectTarget(BackpackLantern.findEquipped(back), main, off);
        check(target == equipped, "Back slot must beat both hands");
        BackpackLantern.toggle(target);
        check(!BackpackLantern.isEnabled(equipped), "Equipped lantern did not turn off");
        check(BackpackLantern.isEnabled(main) && BackpackLantern.isEnabled(off)
                && BackpackLantern.isEnabled(stored), "Changed an untargeted backpack");
        check(BackpackLantern.selectTarget(equipped, main, off) == equipped, "Off backpack must retain priority");
        BackpackLantern.toggle(BackpackLantern.selectTarget(equipped, main, off));
        check(BackpackLantern.isEnabled(equipped), "Equipped backpack could not turn back on");
        check(BackpackLantern.selectTarget(ItemStack.EMPTY, main, off) == main, "Main hand priority");
        check(BackpackLantern.selectTarget(ItemStack.EMPTY, new ItemStack(Items.STICK), off) == off, "Offhand fallback");
        check(BackpackLantern.selectTarget(new ItemStack(ModItems.LEATHER_BACKPACK), main, off) == main,
                "Non-lantern back slot should allow held lantern");
        check(BackpackLantern.selectTarget(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY).isEmpty(), "Should not select stored backpack");
        check(BackpackLantern.findEquipped(null).isEmpty(), "Missing Trinkets/back slot");
        for (var item : new net.minecraft.item.Item[]{ModItems.DIAMOND_BACKPACK, ModItems.NETHERITE_BACKPACK}) {
            var bag = new ItemStack(item);
            check(BackpackLantern.isEnabled(bag), "Existing bag should default on");
            var custom = new net.minecraft.nbt.NbtCompound();
            custom.putString("other_mod", "preserve me");
            bag.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                    net.minecraft.component.type.NbtComponent.of(custom));
            bag.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("My backpack"));
            var inventory = new BackpackInventory(bag, ((BackpackItem) item).getTier().getSlotCount());
            inventory.setStack(0, new ItemStack(Items.DIAMOND, 12));
            BackpackLantern.toggle(bag);
            check(!BackpackLantern.isEnabled(bag), "Lantern should be off");
            inventory.setStack(1, new ItemStack(Items.ARROW, 32));
            check(!BackpackLantern.isEnabled(bag), "Saving backpack contents reset lantern");
            // The real item codec is the save/load boundary for chests, dropped items and player inventories.
            var ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE,
                    net.minecraft.registry.DynamicRegistryManager.of(net.minecraft.registry.Registries.REGISTRIES));
            var encoded = ItemStack.CODEC.encodeStart(ops, bag).getOrThrow();
            var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            check(!BackpackLantern.isEnabled(restored), "Off state lost across save/load");
            check(ItemStack.areItemsAndComponentsEqual(bag, restored), "Save/load altered item contents or metadata");
            var contents = new BackpackInventory(restored, inventory.size());
            check(contents.getStack(0).getCount() == 12 && contents.getStack(1).getCount() == 32, "Toggle lost backpack contents");
            check(restored.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA).copyNbt()
                    .getString("other_mod", "").equals("preserve me"), "Toggle overwrote unrelated custom data");
            BackpackLantern.toggle(restored);
            check(BackpackLantern.isEnabled(restored) && !BackpackLantern.isEnabled(bag), "Independent copies share toggle state");
        }
        System.out.println("PASS: back/main/offhand priority, isolated state, real item save/load, contents and metadata preservation.");
    }
}
