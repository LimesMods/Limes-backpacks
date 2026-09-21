package com.lime.backpacks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BackpackBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 5);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public BackpackBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(ROTATION, 0)
                .setValue(TIER, 0)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, TIER, LIT);
    }

    @Override
    public BackpackBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shape(state, world, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shape(state, world, pos);
    }

    private VoxelShape shape(BlockState state, BlockGetter world, BlockPos pos) {
        var tier = BackpackTier.values()[state.getValue(TIER)];
        if (world.getBlockEntity(pos) instanceof BackpackBlockEntity entity
                && entity.getBackpack().getItem() instanceof BackpackItem backpack) tier = backpack.getTier();
        return BackpackShapes.get(tier, state.getValue(ROTATION));
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T>
            getTicker(Level world, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, ModBlocks.BACKPACK_BLOCK_ENTITY,
                (w, p, s, entity) -> entity.syncTier());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, world, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                         Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return interact(state, world, pos, player);
    }

    private InteractionResult interact(BlockState state, Level world, BlockPos pos, Player player) {
        if (!(world.getBlockEntity(pos) instanceof BackpackBlockEntity backpackEntity)
                || backpackEntity.getBackpack().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                ItemStack pickedUp = backpackEntity.takeBackpack();
                world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                if (!tryEquipInBackSlot(player, pickedUp) && !player.getInventory().add(pickedUp)) {
                    player.drop(pickedUp, false, Prediction.SERVER_ONLY);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!world.isClientSide() && backpackEntity.getBackpack().getItem() instanceof BackpackItem backpackItem) {
            player.openMenu(backpackItem.createScreenFactory(backpackEntity.getBackpack(), backpackEntity));
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean tryEquipInBackSlot(Player player, ItemStack stack) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) return false;
        var back = TrinketsCompat.getBackInventory(player);
        if (back == null || back.getContainerSize() == 0 || !back.getItem(0).isEmpty()) return false;
        back.setItem(0, stack);
        back.setChanged();
        TrinketsCompat.syncQuiver(back);
        return true;
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        // Mining retains the original block entity in the loot context even
        // after the world has removed it. Return one complete item, not its contents.
        var blockEntity = context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof BackpackBlockEntity entity && !entity.getBackpack().isEmpty()) {
            entity.setChanged();
            return java.util.List.of(entity.getBackpack().copy());
        }
        return java.util.List.of();
    }

}
