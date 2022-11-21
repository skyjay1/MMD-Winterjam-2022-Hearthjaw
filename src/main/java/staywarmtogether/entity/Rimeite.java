package staywarmtogether.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import staywarmtogether.SWTRegistry;
import staywarmtogether.util.IglooBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Rimeite extends PathfinderMob implements RangedAttackMob, IAnimatable {

    // SYNCED DATA //
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Rimeite.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_BRICK = SynchedEntityData.defineId(Rimeite.class, EntityDataSerializers.BOOLEAN);
    private static final String KEY_STATE = "State";
    private static final String KEY_BRICK = "Brick";
    private static final String KEY_QUEEN = "Queen";
    private static final String KEY_ANIMATION_TIME = "AnimationTime";
    // STATES //
    protected static final byte STATE_IDLE = (byte) 0;
    protected static final byte STATE_SCOOP = (byte) 1;
    protected static final byte STATE_BUILD = (byte) 2;
    protected static final byte STATE_THROW = (byte) 3;
    // EVENTS //
    protected static final byte START_SCOOPING_EVENT = (byte) 9;
    protected static final byte START_BUILDING_EVENT = (byte) 10;
    protected static final byte START_THROWING_EVENT = (byte) 11;
    // CONSTANTS //
    protected static final int SCOOP_TIME = 20;
    protected static final int THROW_TIME = 20;
    protected static final int MIN_SAW_QUEEN_TIME = 1200;
    protected static final int MAX_SAW_QUEEN_TIME = 3600;
    protected static final double MIN_SAW_QUEEN_DISTANCE = 2.5D;

    // SERVER SIDE VARIABLES //
    protected UUID queenId;
    protected BlockPos buildTarget;
    protected int sawQueenTimestamp;
    // GOAL INSTANCES //
    protected MeleeAttackGoal meleeAttackGoal;
    protected RangedAttackGoal rangedAttackGoal;
    /**
     * When true, the combat goal will be refreshed the next tick.
     * Avoids ConcurrentModificationException when calling {@link #setHasBrick(boolean)} from within a goal.
     **/
    protected boolean combatGoalDirty;

    // GECKOLIB //
    protected AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected int animationTimer;

    //// CONSTRUCTOR ////

    public Rimeite(EntityType<? extends Rimeite> entityType, Level level) {
        super(entityType, level);
        meleeAttackGoal = new MeleeAttackGoal(this, 1.0D, true);
        rangedAttackGoal = new RimeiteThrowBrickGoal(this, 1.0D, THROW_TIME, 6.0F);
        updateCombatGoal();
    }

    public static Rimeite create(final ServerLevel level, final RimeiteQueen queen) {
        final Rimeite entity = SWTRegistry.EntityReg.RIMEITE.get().create(level);
        entity.moveTo(queen.position());
        level.addFreshEntity(entity);
        entity.setQueen(queen);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(queen.blockPosition()), MobSpawnType.BREEDING, null, null);
        return entity;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.MAX_HEALTH, 10.0D)
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
        this.goalSelector.addGoal(2, new Rimeite.VisitQueenGoal(this, 1.1D, MIN_SAW_QUEEN_DISTANCE));
        this.goalSelector.addGoal(3, new Rimeite.FeedQueenGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new Rimeite.ScoopBrickGoal(this, 1.0D, SCOOP_TIME));
        this.goalSelector.addGoal(5, new Rimeite.BuildIglooGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new Rimeite.HarvestSnowGoal(this, 1.0D, 14));
        this.goalSelector.addGoal(7, new Rimeite.FollowQueenGoal(this, 1.0D, 6));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.7D));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Spider.class, false));
    }

    @Override
    public void aiStep() {
        // update combat goal
        if(combatGoalDirty) {
            updateCombatGoal();
        }
        // update AI
        super.aiStep();
        // server side logic
        if(!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // take damage in warm biomes
            Biome biome = level.getBiome(blockPosition()).value();
            if(biome.shouldSnowGolemBurn(blockPosition())) {
                hurt(DamageSource.ON_FIRE, 1.0F);
            }
            // periodically check if queen exists
            if((tickCount) % 80 == 5) {
                Optional<RimeiteQueen> oQueen = getQueen(serverLevel);
                if(oQueen.isPresent()) {
                    RimeiteQueen queen = oQueen.get();
                    // update restriction to match the queen
                    if(queen.hasRestriction()) {
                        this.restrictTo(queen.getRestrictCenter(), (int) (queen.getRestrictRadius() + 8));
                    }
                    // update queen distance check
                    if(position().closerThan(queen.position(), MIN_SAW_QUEEN_DISTANCE)) {
                        setSawQueen(tickCount);
                    }
                    // check if the entity has not seen the queen for too long
                    if(wantsToForgetQueen()) {
                        setQueen(null);
                    }
                } else {
                    // no queen found, take damage
                    this.hurt(DamageSource.STARVE, 2.0F);
                }
            }
            // rarely destroy held brick
            if(getHasBrick() && random.nextFloat() < 0.0002F) {
                setHasBrick(false);
                serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getEyeY(), getZ(), 15, 0.15D, 0.15D, 0.15D, 0.05D);
            }
        }

    }

    @Override
    public void tick() {
        super.tick();
        // update animations
        if(animationTimer > 0) {
            if(--animationTimer <= 0 && !level.isClientSide()) {
                // throw brick at end of animation
                if(isThrowing() && getTarget() != null && getHasBrick()) {
                    actuallyPerformRangedAttack(getTarget());
                }
                // place block at end of animation
                if(isBuilding() && buildTarget != null && buildAt(buildTarget)) {
                    setBuildTarget(null);
                }
                // update state
                setState(STATE_IDLE);
            }
        }
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        return 0.25F;
    }

    @Override
    public void push(Entity entity) {
        if(entity.getType() != SWTRegistry.EntityReg.RIMEITE_QUEEN.get() && entity.getType() != this.getType()) {
            super.push(entity);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return (damageSource.getEntity() instanceof Rimeite) || damageSource == DamageSource.FREEZE || super.isInvulnerableTo(damageSource);
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    protected float getJumpPower() {
        return super.getJumpPower() + 0.1F;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevel, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(serverLevel, difficulty, spawnType, spawnDataIn, dataTag);
        this.setLeftHanded(false);
        return data;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return getQueenId().isPresent();
    }

    //// RANGED ATTACK ////

    /**
     * Adds or removes the combat goal to make sure ranged is only used when
     * the entity has a brick, and melee is only used when it does not.
     */
    protected void updateCombatGoal() {
        if(!level.isClientSide()) {
            // update flag
            combatGoalDirty = false;
            // remove attack goals
            this.goalSelector.removeGoal(meleeAttackGoal);
            this.goalSelector.removeGoal(rangedAttackGoal);
            // determine the attack goal to use
            if(getHasBrick()) {
                this.goalSelector.addGoal(1, rangedAttackGoal);
            } else {
                this.goalSelector.addGoal(1, meleeAttackGoal);
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // start attack animation
        setState(STATE_THROW);
        animationTimer = THROW_TIME;
        level.broadcastEntityEvent(this, START_THROWING_EVENT);
    }

    public void actuallyPerformRangedAttack(final LivingEntity target) {
        final SnowBrickProjectile brick = SWTRegistry.EntityReg.SNOW_BRICK.get().create(level);
        brick.moveTo(getX(), getEyeY(), getZ());
        brick.setOwner(this);
        // determine offset
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.33D) - brick.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        // calculate motion
        brick.shoot(dx, dy + distance * brick.getGravity(), dz, 0.8F, 2);
        // play sound
        // TODO this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        // add entity
        this.level.addFreshEntity(brick);
        // remove brick
        setHasBrick(false);
    }

    //// GECKOLIB ////

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        byte state = getState();
        switch(state) {
            case STATE_THROW:
            case STATE_BUILD:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite.throw", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                break;
            case STATE_SCOOP:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite.scoop", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME));
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

    public boolean isBuilding() {
        return getState() == STATE_BUILD;
    }

    public boolean isThrowing() {
        return getState() == STATE_THROW;
    }

    @Override
    public void handleEntityEvent(byte event) {
        final double particleMotion = 0.15D;
        switch(event) {
            case START_SCOOPING_EVENT:
                animationTimer = SCOOP_TIME;
                break;
            case START_THROWING_EVENT:
            case START_BUILDING_EVENT:
                animationTimer = THROW_TIME;
                break;
            case EntityEvent.HURT:
                super.handleEntityEvent(event);
                for(int i = 0, n = 1 + random.nextInt(5); i < n; i++) {
                    level.addParticle(ParticleTypes.ITEM_SNOWBALL, getX(), getEyeY(), getZ(),
                            (random.nextDouble() - 0.5D) * 2.0D * particleMotion,
                            random.nextDouble() * particleMotion,
                            (random.nextDouble() - 0.5D) * 2.0D * particleMotion);
                }
                break;
            case EntityEvent.DEATH:
                super.handleEntityEvent(event);
                for(int i = 0, n = 40 + random.nextInt(15); i < n; i++) {
                    level.addParticle(ParticleTypes.ITEM_SNOWBALL, getX(), getEyeY(), getZ(),
                            (random.nextDouble() - 0.5D) * 2.0D * particleMotion,
                            random.nextDouble() * particleMotion,
                            (random.nextDouble() - 0.5D) * 2.0D * particleMotion);
                }
                break;
            default:
                super.handleEntityEvent(event);
        }
    }

    //// BUILDING ////

    public boolean getHasBrick() {
        return getEntityData().get(DATA_BRICK);
    }

    public void setHasBrick(final boolean brick) {
        getEntityData().set(DATA_BRICK, brick);
        if(!level.isClientSide()) {
            combatGoalDirty = true;
        }
    }

    public Optional<BlockPos> getBuildTarget() {
        return Optional.ofNullable(buildTarget);
    }

    public void setBuildTarget(@Nullable BlockPos target) {
        buildTarget = target;
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

    /**
     * Places a building block at the given position
     * @param blockPos the block position
     * @return true if the block was placed
     */
    public boolean buildAt(final BlockPos blockPos) {
        if(level.isClientSide()) {
            return false;
        }
        // determine the block to place
        Optional<BlockState> oBlockState = IglooBuilder.getBuildingBlock(level, blockPos);
        // place the block
        if (oBlockState.isPresent() && ForgeEventFactory.getMobGriefingEvent(level, this)) {
            level.setBlock(blockPos, oBlockState.get(), Block.UPDATE_ALL);
            // play sound
            playSound(oBlockState.get().getSoundType().getPlaceSound());
            // remove brick and reset build target
            setHasBrick(false);
            return true;
        }
        return false;
    }

    //// QUEEN ////

    public void setQueen(@Nullable final RimeiteQueen queen) {
        setQueenId((queen != null) ? queen.getUUID() : null);
    }

    public void setQueenId(@Nullable final UUID queenId) {
        this.queenId = queenId;
    }

    public Optional<UUID> getQueenId() {
        return Optional.ofNullable(queenId);
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
        if(!(queen instanceof RimeiteQueen)) {
            setQueen(null);
            return Optional.empty();
        }
        return Optional.of((RimeiteQueen) queen);
    }

    public void setSawQueen(final int time) {
        this.sawQueenTimestamp = time;
    }

    public boolean wantsToSeeQueen() {
        return tickCount - sawQueenTimestamp > MIN_SAW_QUEEN_TIME + ((getId() * 3) % 1200);
    }

    public boolean wantsToForgetQueen() {
        return tickCount - sawQueenTimestamp > MAX_SAW_QUEEN_TIME + ((getId() * 3) % 1200);
    }

    //// NBT ////

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setState(tag.getByte(KEY_STATE));
        setHasBrick(tag.getBoolean(KEY_BRICK));
        if(tag.contains(KEY_QUEEN)) {
            setQueenId(tag.getUUID(KEY_QUEEN));
        }
        animationTimer = tag.getInt(KEY_ANIMATION_TIME);

        updateCombatGoal();
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(KEY_STATE, getState());
        tag.putBoolean(KEY_BRICK, getHasBrick());
        getQueenId().ifPresent(uuid -> tag.putUUID(KEY_QUEEN, uuid));
        tag.putInt(KEY_ANIMATION_TIME, animationTimer);
    }

    //// GOALS ////

    static class RimeiteThrowBrickGoal extends RangedAttackGoal {

        protected final Rimeite entity;

        public RimeiteThrowBrickGoal(Rimeite entity, double speed, int interval, float distance) {
            super(entity, speed, interval, distance);
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            return (entity.isIdle() || entity.isThrowing()) && entity.getHasBrick() && super.canUse();
        }
    }

    static abstract class MoveToQueenGoal extends Goal {

        protected final Rimeite entity;
        protected final int interval;
        protected final double speedModifier;

        protected RimeiteQueen queen;
        protected boolean isTargetReached;
        protected int cooldown;

        public MoveToQueenGoal(final Rimeite entity, final int interval, final double speed) {
            this.entity = entity;
            this.interval = interval;
            this.cooldown = 10;
            this.speedModifier = speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        /**
         * Checks if the cooldown is finished and the queen is present
         * @return true if the goal can be used
         */
        @Override
        public boolean canUse() {
            if(cooldown > 0) {
                cooldown--;
                return false;
            }
            // locate queen
            Optional<RimeiteQueen> oQueen = entity.getQueen((ServerLevel) entity.level);
            if(oQueen.isEmpty()) {
                return false;
            }
            queen = oQueen.get();
            if(!canUse(queen)) {
                setCooldown();
                return false;
            }
            // all checks passed
            return true;
        }

        /**
         * @param queen the queen
         * @return true if the goal can run
         */
        protected abstract boolean canUse(@NotNull final RimeiteQueen queen);

        @Override
        public boolean canContinueToUse() {
            return queen != null && canUse(queen);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return cooldown <= 0;
        }

        @Override
        public void start() {
            moveMobToQueen();
            isTargetReached = false;
        }

        @Override
        public void tick() {
            if (null == queen) {
                stop();
                return;
            }
            // update target reached flag
            isTargetReached = entity.position().closerThan(queen.position(), getAcceptedDistance());
            if(isTargetReached) {
                // update
                if(onReachedQueen()) {
                    stop();
                }
            } else if(entity.tickCount % 40 == 0) {
                // recalculate path
                moveMobToQueen();
            }
        }

        @Override
        public void stop() {
            setCooldown();
        }

        /**
         * Updates the cooldown amount to a random number in the range [interval, interval * 3 / 2)
         */
        protected void setCooldown() {
            cooldown = interval + entity.random.nextInt(Math.max(1, interval / 2));
        }

        /**
         * Called every tick that the entity is within distance of the queen
         * @return true to stop running the goal
         */
        protected abstract boolean onReachedQueen();

        /**
         * Updates the entity navigation to target the queen
         */
        protected void moveMobToQueen() {
            if(queen != null) {
                entity.getNavigation().moveTo(queen, speedModifier);
            }
        }

        /**
         * @return the minimum distance required to consider the target reached
         */
        protected double getAcceptedDistance() {
            return 2.0D;
        }
    }

    static class ScoopBrickGoal extends MoveToQueenGoal {

        protected final int maxDuration;

        protected int duration;

        public ScoopBrickGoal(final Rimeite entity, final double speed, final int maxDuration) {
            super(entity, 30, speed);
            this.maxDuration = maxDuration;
        }

        @Override
        protected boolean canUse(@NotNull RimeiteQueen queen) {
            // check for existing brick
            if(entity.getHasBrick()) {
                return false;
            }
            // check for held item
            if(entity.getMainHandItem().is(Items.SNOWBALL)) {
                return false;
            }
            // verify queen has igloo builder
            if(!queen.hasIglooBuilder() || queen.isDeadOrDying() || queen.isIglooComplete()) {
                return false;
            }
            // verify queen is finished making a brick
            if(!queen.getHasBrick() || queen.isBricking()) {
                return false;
            }
            // all checks passed
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return queen != null && (!entity.getNavigation().isDone() || entity.isScooping());
        }

        @Override
        public void start() {
            super.start();
            duration = 0;
        }

        @Override
        protected boolean onReachedQueen() {
            // begin scooping
            if(duration <= 0) {
                // stop moving
                entity.getNavigation().stop();
                // verify queen still has a brick
                if(!queen.getHasBrick()) {
                    return true;
                }
                // set duration
                duration = 1;
                // update entity state
                entity.setState(STATE_SCOOP);
                entity.animationTimer = SCOOP_TIME;
                entity.level.broadcastEntityEvent(entity, START_SCOOPING_EVENT);
                // remove brick from queen
                queen.setHasBrick(false);
                entity.setHasBrick(true);
                return false;
            } else {
                // update scooping
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(queen);
                // finish scooping
                return duration++ >= maxDuration;
            }
        }

        @Override
        public void stop() {
            super.stop();
            if(entity.isScooping()) {
                entity.setState(STATE_IDLE);
            }
            duration = 0;
        }

        @Override
        protected double getAcceptedDistance() {
            return 1.85D;
        }
    }

    static class VisitQueenGoal extends MoveToQueenGoal {

        protected final double distance;

        public VisitQueenGoal(Rimeite entity, double speed, double distance) {
            super(entity, 40, speed);
            this.distance = distance;
        }

        @Override
        protected boolean canUse(final @NotNull RimeiteQueen queen) {
            return entity.wantsToSeeQueen();
        }

        @Override
        protected boolean onReachedQueen() {
            // update timestamp
            entity.setSawQueen(entity.tickCount);
            ((ServerLevel)entity.level).sendParticles(ParticleTypes.HEART, entity.getX(), entity.getEyeY() + 0.15D, entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            return true;
        }

        @Override
        protected double getAcceptedDistance() {
            return distance;
        }
    }

    static class FeedQueenGoal extends MoveToQueenGoal {

        public FeedQueenGoal(Rimeite entity, double speed) {
            super(entity, 40, speed);
        }

        @Override
        protected boolean canUse(@NotNull RimeiteQueen queen) {
            return entity.getMainHandItem().is(Items.SNOWBALL);
        }

        @Override
        protected boolean onReachedQueen() {
            // determine snow value of held item
            ItemStack heldItem = entity.getMainHandItem();
            int snowAmount = queen.getSnowAmountForItem(heldItem);
            // add snow and remove item
            queen.addSnow(snowAmount);
            heldItem.shrink(1);
            entity.setItemInHand(InteractionHand.MAIN_HAND, heldItem);
            // play sound
            entity.playSound(SoundType.SNOW.getPlaceSound());
            return true;
        }

        @Override
        protected double getAcceptedDistance() {
            return 2.0D;
        }
    }

    static class HarvestSnowGoal extends MoveToBlockGoal {

        protected final Rimeite entity;

        protected RimeiteQueen queen;

        public HarvestSnowGoal(final Rimeite entity, final double speed, final int searchRange) {
            super(entity, speed, searchRange);
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            // check game rule
            if(!entity.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return false;
            }
            // do not run when held item is occupied
            if(!entity.getMainHandItem().isEmpty()) {
                return false;
            }
            // do not run when holding brick
            if(entity.getHasBrick()) {
                return false;
            }
            // locate queen
            Optional<RimeiteQueen> oQueen = entity.getQueen((ServerLevel) entity.level);
            if(oQueen.isEmpty()) {
                return false;
            }
            queen = oQueen.get();
            // ensure queen has igloo builder
            if(!queen.hasIglooBuilder() || queen.isDeadOrDying()) {
                return false;
            }
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return queen != null && entity.getMainHandItem().isEmpty() && !entity.getHasBrick() && super.canContinueToUse();
        }

        @Override
        public void tick() {
            if(isReachedTarget() && ForgeEventFactory.getMobGriefingEvent(entity.level, entity)) {
                // destroy block
                entity.level.destroyBlock(blockPos.above(), false);
                // update held item
                entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL));
                // finish
                stop();
                return;
            }
            super.tick();
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            final BlockState blockState = level.getBlockState(pos.above());
            return blockState.is(Blocks.SNOW) && blockState.getValue(SnowLayerBlock.LAYERS) <= 4;
        }
    }

    static class BuildIglooGoal extends MoveToBlockGoal {

        protected final Rimeite entity;

        protected RimeiteQueen queen;
        protected int moveToVerticalOffset;

        public BuildIglooGoal(final Rimeite entity, final double speed) {
            super(entity, speed, -1, -1);
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            // check game rule
            if(!entity.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return false;
            }
            // check for idle and held brick
            if(!(entity.isIdle() && entity.getHasBrick())) {
                return false;
            }
            // locate queen
            Optional<RimeiteQueen> oQueen = entity.getQueen((ServerLevel) entity.level);
            if(oQueen.isEmpty()) {
                return false;
            }
            queen = oQueen.get();
            // ensure queen has igloo builder
            if(!queen.hasIglooBuilder() || queen.isDeadOrDying() || queen.isIglooComplete()) {
                return false;
            }
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return entity.isIdle() && entity.getBuildTarget().isPresent() && super.canContinueToUse();
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
                List<Entity> list = entity.level.getEntitiesOfClass(Entity.class, new AABB(blockPos).deflate(0.125D));
                if(list.isEmpty()) {
                    // update state
                    entity.setState(STATE_BUILD);
                    entity.animationTimer = THROW_TIME;
                    entity.level.broadcastEntityEvent(entity, START_BUILDING_EVENT);
                }
                stop();
                return;
            }
            super.tick();
        }

        @Override
        public void stop() {
            super.stop();
        }

        @Override
        protected int nextStartTick(PathfinderMob mob) {
            return reducedTickDelay(60 + mob.getRandom().nextInt(60));
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
            return queen != null && queen.hasIglooBuilder() && IglooBuilder.canBuildAt(level, pos);
        }
    }

    static class FollowQueenGoal extends FollowMobGoal {

        protected final Rimeite entity;
        protected final int maxDistance;

        protected RimeiteQueen queen;

        public FollowQueenGoal(final Rimeite entity, final double speed, final int maxDistance) {
            super(entity, speed, Math.max(2.0F, maxDistance * 0.5F), (float) maxDistance);
            this.entity = entity;
            this.maxDistance = maxDistance;
        }

        @Override
        public boolean canUse() {
            // locate queen
            Optional<RimeiteQueen> oQueen = entity.getQueen((ServerLevel) entity.level);
            if(oQueen.isEmpty()) {
                return false;
            }
            queen = oQueen.get();
            // check if queen has home position
            if(queen.hasRestriction()) {
                return false;
            }
            // check distance to queen
            if(entity.position().closerThan(queen.position(), maxDistance)) {
                return false;
            }
            // all checks passed
            this.followingMob = queen;
            return true;
        }

    }
}
