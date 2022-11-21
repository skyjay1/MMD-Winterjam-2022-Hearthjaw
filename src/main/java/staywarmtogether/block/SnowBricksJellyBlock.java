package staywarmtogether.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import staywarmtogether.SWTRegistry;

public class SnowBricksJellyBlock extends HorizontalDirectionalBlock {

    public static final int MAX_JELLY = 3;
    public static final IntegerProperty JELLY_LEVEL = IntegerProperty.create("jelly_level", 0, MAX_JELLY);

    public SnowBricksJellyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(JELLY_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(JELLY_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // INTERACT //

    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if(itemstack.is(Items.GLASS_BOTTLE) && blockState.getValue(JELLY_LEVEL) >= MAX_JELLY) {
            if(!level.isClientSide()) {
                // shrink itemstack
                itemstack.shrink(1);
                // play sound
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                // add jelly
                ItemStack jelly = new ItemStack(SWTRegistry.ItemReg.RIMEITE_JELLY.get());
                if (itemstack.isEmpty()) {
                    player.setItemInHand(hand, jelly);
                } else if (!player.getInventory().add(jelly)) {
                    player.drop(jelly, false);
                }
                // award stat
                player.awardStat(Stats.ITEM_USED.get(item));
                // broadcast event
                level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
                // update block state
                level.setBlock(blockPos, blockState.setValue(JELLY_LEVEL, 0), Block.UPDATE_ALL);
            }
            return InteractionResult.SUCCESS;
        }

        return super.use(blockState, level, blockPos, player, hand, hitResult);
    }

    // REDSTONE //

    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        return blockState.getValue(JELLY_LEVEL);
    }

}
