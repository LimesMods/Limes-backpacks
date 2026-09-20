package com.lime.backpacks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.util.shape.VoxelShape;

public class BackpackBlock extends BlockWithEntity {
    public static final IntProperty ROTATION = IntProperty.of("rotation", 0, 7);
    public static final IntProperty TIER = IntProperty.of("tier", 0, 5);
    public static final BooleanProperty LIT = BooleanProperty.of("lit");

    public BackpackBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(ROTATION, 0)
                .with(TIER, 0)
                .with(LIT, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(BackpackBlock::new);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, TIER, LIT);
    }

    @Override
    public BackpackBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape(state, world, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape(state, world, pos);
    }

    private VoxelShape shape(BlockState state, BlockView world, BlockPos pos) {
        var tier = BackpackTier.values()[state.get(TIER)];
        if (world.getBlockEntity(pos) instanceof BackpackBlockEntity entity
                && entity.getBackpack().getItem() instanceof BackpackItem backpack) tier = backpack.getTier();
        return BackpackShapes.get(tier, state.get(ROTATION));
    }

    @Override
    public <T extends net.minecraft.block.entity.BlockEntity> net.minecraft.block.entity.BlockEntityTicker<T>
            getTicker(World world, BlockState state, net.minecraft.block.entity.BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlocks.BACKPACK_BLOCK_ENTITY,
                (w, p, s, entity) -> entity.syncTier());
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return interact(state, world, pos, player);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, net.minecraft.util.Hand hand, BlockHitResult hit) {
        return interact(state, world, pos, player);
    }

    private ActionResult interact(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!(world.getBlockEntity(pos) instanceof BackpackBlockEntity backpackEntity)
                || backpackEntity.getBackpack().isEmpty()) {
            return ActionResult.PASS;
        }

        if (player.isSneaking()) {
            if (!world.isClient()) {
                ItemStack pickedUp = backpackEntity.takeBackpack();
                world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                if (!tryEquipInBackSlot(player, pickedUp) && !player.getInventory().insertStack(pickedUp)) {
                    player.dropItem(pickedUp, false);
                }
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient() && backpackEntity.getBackpack().getItem() instanceof BackpackItem backpackItem) {
            player.openHandledScreen(backpackItem.createScreenFactory(backpackEntity.getBackpack(), backpackEntity));
        }
        return ActionResult.SUCCESS;
    }

    private static boolean tryEquipInBackSlot(PlayerEntity player, ItemStack stack) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) return false;
        var back = TrinketsCompat.getBackInventory(player);
        if (back == null || back.size() == 0 || !back.getStack(0).isEmpty()) return false;
        back.setStack(0, stack);
        back.markDirty();
        TrinketsCompat.syncQuiver(back);
        return true;
    }

    @Override
    protected java.util.List<ItemStack> getDroppedStacks(BlockState state,
            net.minecraft.loot.context.LootWorldContext.Builder context) {
        // Mining retains the original block entity in the loot context even
        // after the world has removed it. Return one complete item, not its contents.
        var blockEntity = context.getOptional(net.minecraft.loot.context.LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof BackpackBlockEntity entity && !entity.getBackpack().isEmpty()) {
            entity.markDirty();
            return java.util.List.of(entity.getBackpack().copy());
        }
        return java.util.List.of();
    }

}
