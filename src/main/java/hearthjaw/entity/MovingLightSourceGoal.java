package hearthjaw.entity;

import hearthjaw.HJRegistry;
import hearthjaw.block.MovingLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.EnumSet;
import java.util.function.Function;

public class MovingLightSourceGoal<T extends LivingEntity> extends Goal {

	protected final T entity;
	protected final int interval;
	protected final boolean requireAboveEmpty;
	protected final Function<T, Integer> lightLevelFunction;

	protected int lightLevel;

	public MovingLightSourceGoal(final T entity, final int interval, final boolean requireAboveEmpty,
								 final Function<T, Integer> lightLevelFunction) {
		this.entity = entity;
		this.interval = interval;
		this.requireAboveEmpty = requireAboveEmpty;
		this.lightLevelFunction = lightLevelFunction;
		this.setFlags(EnumSet.noneOf(Goal.Flag.class));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public boolean canUse() {
		return (lightLevel = lightLevelFunction.apply(entity)) > 0;
	}

	@Override
	public void start() {
		this.tick();
	}

	@Override
	public void tick() {
		if (this.interval > 1 && this.entity.tickCount % this.interval != 0) {
			return;
		}
		final BlockPos.MutableBlockPos pos = entity.blockPosition().mutable();
		// check each position in a vertical column to attempt to place a light block
		BlockState blockState;
		for (int i = 0, n = Mth.ceil(entity.getBbHeight()) + 1; i <= n; i++) {
			blockState = entity.level.getBlockState(pos);
			// if there's already a matching block, stop here
			if (blockState.is(HJRegistry.BlockReg.LIGHT.get()) && blockState.getValue(MovingLightBlock.LEVEL) >= lightLevel) {
				return;
			}
			// check if block can be placed at this position
			if (canPlaceLightAt(entity.level, blockState, pos)) {
				// create light block state
				boolean waterlogged = blockState.is(Blocks.WATER);
				BlockState light = HJRegistry.BlockReg.LIGHT.get().defaultBlockState()
						.setValue(MovingLightBlock.LEVEL, lightLevel)
						.setValue(MovingLightBlock.WATERLOGGED, waterlogged);
				// place light block
				this.entity.level.setBlock(pos, light, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
				return;
			}
			// update position
			pos.move(Direction.UP);
		}
	}

	protected boolean canPlaceLightAt(final LevelReader level, final BlockState blockState, final BlockPos pos) {
		if(blockState.is(Blocks.WATER) && blockState.getValue(LiquidBlock.LEVEL) != 0) {
			return false;
		}
		if(requireAboveEmpty) {
			BlockState below = level.getBlockState(pos.below());
			if(!below.isAir() && !below.getFluidState().is(Fluids.WATER)) {
				return false;
			}
		}
		return blockState.isAir();
	}
}
