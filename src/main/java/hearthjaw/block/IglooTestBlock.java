package hearthjaw.block;

import hearthjaw.HJMain;
import hearthjaw.util.HemisphereBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

public class IglooTestBlock extends Block {

    private static final int SIZE = 9;

    public IglooTestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(hand == InteractionHand.MAIN_HAND && !level.isClientSide()) {
            HemisphereBuilder builder = new HemisphereBuilder(blockPos, SIZE, 2, 2);
            for(int i = 0; i < 20; i++) {
                Optional<BlockPos> nextPos = builder.getNextPosition(level, level.random);
                HJMain.LOGGER.debug("igloo: pos=" + nextPos);
                if(nextPos.isPresent()) {
                    Optional<BlockState> nextState = builder.getBuildingBlock(level, nextPos.get());
                    HJMain.LOGGER.debug("igloo: block=" + nextState);
                    if(nextState.isPresent()) {
                        level.setBlock(nextPos.get(), nextState.get(), Block.UPDATE_ALL);
                    }
                }
            }
        }


        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
