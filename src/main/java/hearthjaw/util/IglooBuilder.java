package hearthjaw.util;

import hearthjaw.HJRegistry;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.NonNullLazy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class IglooBuilder implements INBTSerializable<CompoundTag> {

    public static final IglooBuilder EMPTY = new IglooBuilder(BlockPos.ZERO, 0, 0);

    protected BlockPos center;
    protected int width;
    protected int maxDepth;

    protected final NonNullLazy<List<BlockPos>> positions = NonNullLazy.of(this::calculate);

    public IglooBuilder(BlockPos center, int width, int maxDepth) {
        this.center = center.immutable();
        this.width = width;
        this.maxDepth = Math.abs(maxDepth);
    }

    public IglooBuilder(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// METHODS ////

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
     * Returns full snow brick blocks for half snow bricks, and
     * half snow bricks for supported and semi-supported positions
     * @param level the level
     * @param pos the position to be replaced with a building block
     * @return the building block if possible, or an empty optional
     */
    public Optional<BlockState> getBuildingBlock(final LevelReader level, final BlockPos pos) {
        // check for existing snow bricks
        BlockState blockState = level.getBlockState(pos);
        if(blockState.is(HJRegistry.BlockReg.SNOW_BRICKS.get())) {
            switch (blockState.getValue(SlabBlock.TYPE)) {
                case TOP:
                case BOTTOM:
                    return Optional.of(blockState.setValue(SlabBlock.TYPE, SlabType.DOUBLE));
                case DOUBLE: default:
                    return Optional.empty();
            }
        }
        // check for horizontal support
        BlockPos relative;
        for(Direction direction : Direction.values()) {
            if(direction == Direction.UP) {
                continue;
            }
            relative = pos.relative(direction);
            if(level.getBlockState(relative).isFaceSturdy(level, relative, direction.getOpposite())) {
                return Optional.of(HJRegistry.BlockReg.SNOW_BRICKS.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
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
        if(blockState.is(HJRegistry.BlockReg.SNOW_BRICKS.get())) {
            return blockState.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;
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
                    // do not cover the bottom middle blocks on north and south
                    if(y > -2 && y < 2 && x < 1) {
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

    //// GETTERS ////

    public List<BlockPos> getPositions() {
        return positions.get();
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getWidth() {
        return width;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    //// NBT ////

    private static final String KEY_CENTER = "Center";
    private static final String KEY_WIDTH = "Width";
    private static final String KEY_MAX_DEPTH = "MaxDepth";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put(KEY_CENTER, NbtUtils.writeBlockPos(center));
        tag.putInt(KEY_WIDTH, width);
        tag.putInt(KEY_MAX_DEPTH, maxDepth);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        center = NbtUtils.readBlockPos(tag.getCompound(KEY_CENTER));
        width = tag.getInt(KEY_WIDTH);
        maxDepth = tag.getInt(KEY_MAX_DEPTH);
    }

    //// EQUALITY ////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IglooBuilder)) return false;
        IglooBuilder that = (IglooBuilder) o;
        return width == that.width && maxDepth == that.maxDepth && Objects.equals(center, that.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, width, maxDepth);
    }

    //// OTHER ////


    @Override
    public String toString() {
        return "IglooBuilder{"
                + " center=" + center.toShortString()
                + " width=" + width
                + " }";
    }
}
