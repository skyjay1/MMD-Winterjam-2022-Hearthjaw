package hearthjaw.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class HearthgoopBlock extends Block {

    public HearthgoopBlock(Properties properties) {
        super(properties);
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
