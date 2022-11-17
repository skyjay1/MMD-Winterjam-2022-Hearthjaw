package hearthjaw.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BloomLanternBlock extends LanternBlock {

    protected static final VoxelShape SHAPE = Shapes.or(
        box(5.5D, 9.0D, 5.5D, 10.5D, 11.0D, 10.5D),
        box(6.5D, 7.0D, 6.5D, 9.5D, 12.0D, 9.5D),
        box(5.0D, 0.0D, 5.0D, 11.0D, 7.0D, 11.0D)
    );

    public BloomLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
