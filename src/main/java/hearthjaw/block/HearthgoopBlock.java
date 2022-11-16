package hearthjaw.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HearthgoopBlock extends Block {

    private static final VoxelShape SHAPE = box(5.0F, 0.0F, 4.0F, 13.0F, 4.0F, 12.0F);

    public HearthgoopBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState blockState, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(blockState, level, pos, neighbor, neighborPos, isMoving);
        if(!canSurvive(blockState, level, pos)) {
            level.destroyBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource rand) {
        if (rand.nextInt(6) == 1) {
            double px = (double) blockPos.getX() + rand.nextDouble();
            double py = (double) blockPos.getY() + 0.15D;
            double pz = (double) blockPos.getZ() + rand.nextDouble();
            level.addParticle(ParticleTypes.LAVA, px, py, pz, 0.0D, 0.0D, 0.0D);
        }
    }
}
