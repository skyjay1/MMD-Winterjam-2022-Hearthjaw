package hearthjaw.block;

import hearthjaw.HJRegistry;
import hearthjaw.entity.Bloomina;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.Tags;

public class BloominaBudBlock extends FlowerBlock implements BonemealableBlock {

    protected static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    public BloominaBudBlock(Properties properties) {
        super(MobEffects.ABSORPTION, 20, properties);
        registerDefaultState(getStateDefinition().any().setValue(AGE, 1));
    }

    private int getMaxAge() {
        return 2;
    }

    private void finishGrowing(final ServerLevel level, final BlockState blockState, final BlockPos pos) {
        // break block
        level.destroyBlock(pos, false);
        // spawn bloomina
        Bloomina bloomina = HJRegistry.EntityReg.BLOOMINA.get().create(level);
        bloomina.setPos(Vec3.atCenterOf(pos));
        bloomina.setBaby(true);
        level.addFreshEntityWithPassengers(bloomina);
        bloomina.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.BREEDING, null, null);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 vec3 = blockState.getOffset(level, pos);
        return SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        // use shears to set age to 0
        if(itemStack.is(Tags.Items.SHEARS)) {
            level.setBlock(pos, blockState.setValue(AGE, 0), Block.UPDATE_CLIENTS);
            return InteractionResult.SUCCESS;
        }
        return super.use(blockState, level, pos, player, hand, hitResult);
    }

    @Override
    public void randomTick(BlockState blockState, ServerLevel level, BlockPos pos, RandomSource rand) {
        super.randomTick(blockState, level, pos, rand);
        int age = level.getBlockState(pos).getValue(AGE);
        // attempt to increase age
        if(age > 0 && age < getMaxAge() && rand.nextFloat() < 0.15F) {
            level.setBlock(pos, blockState.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
        // finish growing
        if(age >= getMaxAge() && rand.nextFloat() < 0.15F) {
            finishGrowing(level, blockState, pos);
        }
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter level, BlockPos pos, BlockState blockState, boolean isClient) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource rand, BlockPos pos, BlockState blockState) {
        return rand.nextInt(3) == 0;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource rand, BlockPos pos, BlockState blockState) {
        int prevAge = blockState.getValue(AGE);
        if(prevAge >= getMaxAge()) {
            finishGrowing(level, blockState, pos);
        } else {
            level.setBlock(pos, blockState.setValue(AGE, Math.min(getMaxAge(), prevAge + 1)), Block.UPDATE_CLIENTS);
        }
    }
}
