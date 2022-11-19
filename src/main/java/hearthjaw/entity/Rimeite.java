package hearthjaw.entity;

import hearthjaw.HJMain;
import hearthjaw.HJRegistry;
import hearthjaw.util.IglooBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Rimeite extends PathfinderMob implements IAnimatable {

    // SYNCED DATA //
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Rimeite.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_BRICK = SynchedEntityData.defineId(Rimeite.class, EntityDataSerializers.BOOLEAN);
    private static final String KEY_STATE = "State";
    private static final String KEY_BRICK = "Brick";
    private static final String KEY_BUILD_TARGET = "BuildTarget";
    private static final String KEY_QUEEN = "Queen";
    // STATES //
    protected static final byte STATE_IDLE = (byte) 0;
    protected static final byte STATE_SCOOP = (byte) 1;
    // EVENTS //
    protected static final byte START_SCOOPING_EVENT = (byte) 9;
    // CONSTANTS //
    protected static final int SCOOP_TIME = 20;

    // SERVER SIDE VARIABLES //
    protected Optional<UUID> queenId;
    protected Optional<BlockPos> buildTarget;

    // GECKOLIB //
    protected AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected int scoopTimer;

    //// CONSTRUCTOR ////

    public Rimeite(EntityType<? extends Rimeite> entityType, Level level) {
        super(entityType, level);
    }

    public static Rimeite create(final ServerLevel level, final RimeiteQueen queen) {
        final Rimeite entity = HJRegistry.EntityReg.RIMEITE.get().create(level);
        entity.moveTo(queen.position());
        level.addFreshEntity(entity);
        entity.setQueen(queen);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(queen.blockPosition()), MobSpawnType.BREEDING, null, null);
        // DEBUG
        HJMain.LOGGER.debug("Queen created Rimeite at " + queen.position());
        return entity;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    //// METHODS ////

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_STATE, (byte) 0);
        getEntityData().define(DATA_BRICK, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new ScoopBrickGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new BuildIglooGoal(this, 0.8D));

        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.7D));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // periodically check if queen exists and start dying if not
        if(tickCount % 90 == (getId() % 10) && level instanceof ServerLevel serverLevel) {
            Optional<RimeiteQueen> oQueen = getQueen(serverLevel);
            if(oQueen.isPresent()) {
                // update restriction to match the queen
                RimeiteQueen queen = oQueen.get();
                if(queen.hasRestriction()) {
                    this.restrictTo(queen.getRestrictCenter(), (int) (queen.getRestrictRadius() + 10));
                }
            } else {
                // TODO
            }
        }
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        return 0.25F;
    }

    @Override
    public void push(Entity entity) {
        if(entity.getType() != HJRegistry.EntityReg.RIMEITE_QUEEN.get()) {
            super.push(entity);
        }
    }

    //// GECKOLIB ////

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        byte state = getState();
        switch(state) {
            case STATE_SCOOP:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite.scoop", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                break;
            case STATE_IDLE:
            default:
                if(this.getDeltaMovement().horizontalDistanceSqr() > 2.500000277905201E-7D) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite.walk", ILoopType.EDefaultLoopTypes.LOOP));
                } else {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite.idle", ILoopType.EDefaultLoopTypes.LOOP));
                }
                break;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    //// STATE ////

    public byte getState() {
        return getEntityData().get(DATA_STATE);
    }

    public void setState(final byte state) {
        getEntityData().set(DATA_STATE, state);
    }

    public boolean isScooping() {
        return getState() == STATE_SCOOP;
    }

    public boolean isIdle() {
        return getState() == STATE_IDLE;
    }

    @Override
    public void handleEntityEvent(byte event) {
        switch(event) {
            case START_SCOOPING_EVENT:
                scoopTimer = SCOOP_TIME;
                break;
            default:
                super.handleEntityEvent(event);
        }
    }

    //// BUILDING ////

    public boolean hasBrick() {
        return getEntityData().get(DATA_BRICK);
    }

    public void setBrick(final boolean brick) {
        getEntityData().set(DATA_BRICK, brick);
    }

    public Optional<BlockPos> getBuildTarget() {
        return buildTarget;
    }

    public void setBuildTarget(@Nullable BlockPos target) {
        buildTarget = Optional.ofNullable(target);
    }

    public boolean isCloseToBuildTarget(final double distance) {
        Optional<BlockPos> oTarget = getBuildTarget();
        if(oTarget.isEmpty()) {
            return false;
        }
        BlockPos targetPos = oTarget.get();
        Vec3 position = position();
        Vec3 target = new Vec3(targetPos.getX() + 0.5D, position.y(), targetPos.getZ() + 0.5D);
        return target.closerThan(position, distance);
    }

    //// QUEEN ////

    public void setQueen(@Nullable final RimeiteQueen queen) {
        setQueenId((queen != null) ? queen.getUUID() : null);
    }

    public void setQueenId(@Nullable final UUID queenId) {
        this.queenId = Optional.ofNullable(queenId);
    }

    public Optional<UUID> getQueenId() {
        return queenId;
    }

    public boolean isQueen(final RimeiteQueen queen) {
        final Optional<UUID> queenId = getQueenId();
        return queen != null && queenId.isPresent() && queen.getUUID().equals(queenId.get());
    }

    public Optional<RimeiteQueen> getQueen(ServerLevel serverLevel) {
        // locate entity uuid
        Optional<UUID> oId = getQueenId();
        if(oId.isEmpty()) {
            return Optional.empty();
        }
        // locate entity
        Entity queen = serverLevel.getEntity(oId.get());
        if(queen instanceof RimeiteQueen) {
            return Optional.of((RimeiteQueen) queen);
        }
        // error
        HJMain.LOGGER.error("Tried to locate RimeiteQueen from UUID, instead got " + Optional.ofNullable(queen));
        setQueen(null);
        return Optional.empty();
    }

    //// NBT ////

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setState(tag.getByte(KEY_STATE));
        setBrick(tag.getBoolean(KEY_BRICK));
        if(tag.contains(KEY_QUEEN)) {
            setQueenId(tag.getUUID(KEY_QUEEN));
        }
        if(tag.contains(KEY_BUILD_TARGET)) {
            setBuildTarget(NbtUtils.readBlockPos(tag.getCompound(KEY_BUILD_TARGET)));
        }
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(KEY_STATE, getState());
        tag.putBoolean(KEY_BRICK, hasBrick());
        getQueenId().ifPresent(uuid -> tag.putUUID(KEY_QUEEN, uuid));
        getBuildTarget().ifPresent(pos -> tag.put(KEY_BUILD_TARGET, NbtUtils.writeBlockPos(pos)));
    }

    //// GOALS ////

    static class ScoopBrickGoal extends Goal {

        protected final Rimeite entity;
        protected final double speedModifier;

        public ScoopBrickGoal(final Rimeite entity, final double speed) {
            this.entity = entity;
            this.speedModifier = speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        // TODO pathfind to queen and scoop up brick

        @Override
        public boolean canUse() {
            return false;
        }
    }

    static class BuildIglooGoal extends MoveToBlockGoal {

        protected final Rimeite entity;

        protected RimeiteQueen queen;
        protected int moveToVerticalOffset;
        protected boolean invalid;

        public BuildIglooGoal(final Rimeite entity, final double speed) {
            super(entity, speed, -1, -1);
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            if(!entity.hasBrick()) {
                // TODO re-enable condition
                //return false;
            }
            Optional<RimeiteQueen> oQueen = entity.getQueen((ServerLevel) entity.level);
            if(oQueen.isEmpty()) {
                return false;
            }
            queen = oQueen.get();
            if(!queen.hasIglooBuilder() || queen.isDeadOrDying()) {
                return false;
            }
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return entity.getBuildTarget().isPresent() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            entity.setBuildTarget(blockPos);
        }

        @Override
        public void tick() {
            if(isReachedTarget()) {
                // ensure there are no entity collisions here
                List<Entity> list = entity.level.getEntitiesOfClass(Entity.class, new AABB(blockPos));
                if(list.isEmpty()) {
                    // determine the block to place
                    Optional<BlockState> oBlockState = queen.getIglooBuilder().getBuildingBlock(entity.level, blockPos);
                    // place the block
                    if (oBlockState.isPresent()) {
                        entity.level.setBlock(blockPos, oBlockState.get(), Block.UPDATE_ALL);
                        entity.setBrick(false);
                    }
                }
                stop();
                return;
            }
            super.tick();
        }

        @Override
        public void stop() {
            super.stop();
            entity.setBuildTarget(null);
        }

        @Override
        protected boolean isReachedTarget() {
            return entity.isCloseToBuildTarget(2.0D);
        }

        @Override
        protected BlockPos getMoveToTarget() {
            return blockPos.below(moveToVerticalOffset);
        }

        @Override
        protected void moveMobToBlock() {
            BlockPos target = getMoveToTarget();
            this.mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ(), this.speedModifier);
        }

        @Override
        protected boolean findNearestBlock() {
            if(null == queen || !queen.hasIglooBuilder()) {
                return false;
            }
            // check if the igloo builder has any positions left
            Optional<BlockPos> oPos = queen.getIglooBuilder().getNextPosition(entity.level, entity.random);
            if(oPos.isEmpty()) {
                return false;
            }
            BlockPos.MutableBlockPos pos = oPos.get().mutable();
            // check if the block is in range and a valid target
            if (!(entity.isWithinRestriction(pos) && this.isValidTarget(this.mob.level, pos))) {
                return false;
            }
            // all checks passed
            this.blockPos = pos.immutable();
            // determine number of blocks below the target to offset the actual pathfinding
            BlockState blockState;
            Path path;
            for(moveToVerticalOffset = -1; moveToVerticalOffset < 24; moveToVerticalOffset++) {
                if(blockPos.equals(pos)) {
                    continue;
                }
                pos.setWithOffset(blockPos, 0, -moveToVerticalOffset, 0);
                blockState = entity.level.getBlockState(pos);
                if(blockState.isPathfindable(entity.level, pos, PathComputationType.LAND) && (path = entity.getNavigation().createPath(pos, 0)) != null) {
                    entity.getNavigation().moveTo(path, speedModifier);
                    break;
                }
            }
            return true;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return queen != null && queen.hasIglooBuilder() && queen.getIglooBuilder().canBuildAt(level, pos);
        }
    }

}
