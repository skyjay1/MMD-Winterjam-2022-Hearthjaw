package hearthjaw.block;

import hearthjaw.HJMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class MovingLightBlock extends LightBlock implements SimpleWaterloggedBlock {

    private final TagKey<EntityType<?>> MOVING_LIGHT = ForgeRegistries.ENTITY_TYPES.tags().createTagKey(new ResourceLocation(HJMain.MODID, "moving_light"));

    public MovingLightBlock(final Properties prop) {
        super(prop);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(WATERLOGGED, false)
                .setValue(LEVEL, 15));
    }

    protected static boolean removeLightBlock(final Level level, final BlockState state, final BlockPos pos, final int flag) {
        // replace with air OR water depending on waterlogged state
        final BlockState replaceWith = state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                : Blocks.AIR.defaultBlockState();
        return level.setBlock(pos, replaceWith, flag);
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean isMoving) {
        state.setValue(WATERLOGGED, oldState.getFluidState().is(FluidTags.WATER));
        // schedule next tick
        int delay = 5;
        level.scheduleTick(pos, this, delay);
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, delay);
        }
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource rand) {
        super.tick(state, level, pos, rand);
        // schedule next tick
        int delay = 5;
        level.scheduleTick(pos, this, delay);
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, delay);
        }
        // check for light source at the given position
        List<Entity> list = level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(0.25D), e -> e.getType().is(MOVING_LIGHT));
        // remove this block if no light source was found
        if(list.isEmpty()) {
            removeLightBlock(level, state, pos, UPDATE_ALL);
        }
    }

    @Override
    protected boolean isAir(BlockState state) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext cxt) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext cxt) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public ItemStack getCloneItemStack(final BlockGetter level, final BlockPos pos, final BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canBeReplaced(final BlockState state, final BlockPlaceContext useContext) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
