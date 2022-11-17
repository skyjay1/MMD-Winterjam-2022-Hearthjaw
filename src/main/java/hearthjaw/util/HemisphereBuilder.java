package hearthjaw.util;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.NonNullLazy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HemisphereBuilder implements INBTSerializable<CompoundTag> {

    protected BlockPos center;
    protected int width;
    protected int maxDepth;
    protected int increment;

    protected final NonNullLazy<List<BlockPos>> positions = NonNullLazy.of(this::calculate);

    public HemisphereBuilder(BlockPos center, int width, int maxDepth, int increment) {
        this.center = center.immutable();
        this.width = width;
        this.maxDepth = Math.abs(maxDepth);
        this.increment = increment;
    }

    public List<BlockPos> getPositions() {
        return positions.get();
    }

    /**
     * @param level the level
     * @param random the random source
     * @return the next position that needs a building block, if any
     */
    public Optional<BlockPos> getNextPosition(final LevelReader level, final RandomSource random) {
        if(getPositions().isEmpty()) {
            return Optional.empty();
        }
        for(int tries = 0, maxTries = width * 2; tries < maxTries; tries++) {
            BlockPos pos = Util.getRandom(getPositions(), random);
            if(canBuildAt(level, pos) && getBuildingBlock(level, pos).isPresent()) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    /**
     * Determines the building block to place at the given position.
     * Returns increased layer snow for existing snow layers,
     * single layer snow for supported positions, and packed ice for semi-supported positions
     * @param level the level
     * @param pos the position to be replaced with a building block
     * @return the building block if possible, or an empty optional
     */
    public Optional<BlockState> getBuildingBlock(final LevelReader level, final BlockPos pos) {
        // check for existing snow layer
        BlockState blockState = level.getBlockState(pos);
        if(blockState.is(Blocks.SNOW)) {
            int layers = increment + blockState.getValue(SnowLayerBlock.LAYERS);
            if(layers >= 8) {
                return Optional.of(Blocks.SNOW_BLOCK.defaultBlockState());
            }
            return Optional.of(Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers));
        }
        // check for vertical support
        BlockPos below = pos.below();
        if(level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return Optional.of(Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1));
        }
        // check for horizontal support
        BlockPos relative;
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            relative = pos.relative(direction);
            if(level.getBlockState(relative).isFaceSturdy(level, relative, direction.getOpposite())) {
                return Optional.of(Blocks.BLUE_ICE.defaultBlockState());
            }
        }
        // no checks passed
        return Optional.empty();
    }

    /**
     * @param level the level
     * @param pos the position
     * @return true if the given position can be replaced with a building block
     */
    public boolean canBuildAt(final LevelReader level, final BlockPos pos) {
        final BlockState blockState = level.getBlockState(pos);
        // check for empty
        if(blockState.isAir()) {
            return true;
        }
        // check for unfinished snow block
        if(blockState.is(Blocks.SNOW)) {
            return blockState.getValue(SnowLayerBlock.LAYERS) < 8;
        }
        // check for replaceable material
        if(blockState.getMaterial().isReplaceable()) {
            return true;
        }
        // no checks passed
        return false;
    }

    @NotNull
    private List<BlockPos> calculate() {
        // calculate values
        final double radius = (double) this.width * 0.5D;
        final Vec3 center = Vec3.atCenterOf(this.center);
        final double margin = 0.55D;
        // create a list to store positions
        List<BlockPos> list = new ArrayList<>(Mth.ceil(2.0D * Math.PI * radius * radius));
        // Iterate over a portion of the bounds and add block positions that are within range.
        Vec3 test;
        for(double y = -maxDepth; y < radius; y++) {
            for(double x = 0; x < radius; x++) {
                for(double z = 0; z < radius; z++) {
                    // do not cover the center block
                    if(x == 0 && z == 0) {
                        continue;
                    }
                    // check if the given position is at the edge of a sphere
                    test = center.add(x, Math.max(0, y), z);
                    if(center.closerThan(test, radius + margin) && !center.closerThan(test, radius - margin)) {
                        // add the block position and its mirrors
                        list.add(this.center.offset(x, y, z));
                        list.add(this.center.offset(x, y, -z));
                        list.add(this.center.offset(-x, y, z));
                        list.add(this.center.offset(-x, y, -z));
                    }
                }
            }
        }

        return list;
    }

    //// NBT ////

    private static final String KEY_CENTER = "Center";
    private static final String KEY_WIDTH = "Width";
    private static final String KEY_MAX_DEPTH = "MaxDepth";
    private static final String KEY_INCREMENT = "Increment";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put(KEY_CENTER, NbtUtils.writeBlockPos(center));
        tag.putInt(KEY_WIDTH, width);
        tag.putInt(KEY_MAX_DEPTH, maxDepth);
        tag.putInt(KEY_INCREMENT, increment);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        center = NbtUtils.readBlockPos(tag.getCompound(KEY_CENTER));
        width = tag.getInt(KEY_WIDTH);
        maxDepth = tag.getInt(KEY_MAX_DEPTH);
        increment = tag.getInt(KEY_INCREMENT);
    }
}
