package staywarmtogether.entity;

import staywarmtogether.SWTRegistry;
import staywarmtogether.util.IglooBuilder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RimeiteQueen extends PathfinderMob implements IAnimatable {

    // SYNCED DATA //
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(RimeiteQueen.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_SNOW = SynchedEntityData.defineId(RimeiteQueen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BRICK = SynchedEntityData.defineId(RimeiteQueen.class, EntityDataSerializers.BOOLEAN);
    private static final String KEY_STATE = "State";
    private static final String KEY_POWER = "Power";
    private static final String KEY_SNOW = "Snow";
    private static final String KEY_BRICK = "HasBrick";
    private static final String KEY_BRICK_TIME = "BrickTime";
    private static final String KEY_SUMMON_TIMESTAMP = "SummonTimestamp";
    private static final String KEY_IGLOO_BUILDER = "IglooBuilder";
    private static final String KEY_IGLOO_COMPLETE = "IglooComplete";
    // STATES //
    protected static final byte STATE_IDLE = (byte) 0;
    protected static final byte STATE_BRICK = (byte) 1;
    // EVENTS //
    protected static final byte START_BRICKING_EVENT = (byte) 9;
    protected static final byte SUMMON_CHILD_EVENT = (byte) 10;

    //// CONSTANTS ////
    protected static final UUID HEALTH_BONUS_UUID = UUID.fromString("9def44d5-27f2-4442-929d-e87f3c743506");
    protected static final int MIN_POWER = 0;
    protected static final int MAX_POWER = 5;
    protected static final int BRICK_CAPACITY = 3;
    protected static final int BRICK_COST = 20;
    protected static final int BRICK_TIME = 40;
    protected static final int SUMMON_COOLDOWN = 250;

    //// SERVER SIDE VARIABLES ////
    protected IglooBuilder iglooBuilder = IglooBuilder.EMPTY;
    protected boolean iglooComplete;
    /** Determines igloo size, max children, and health bonus **/
    protected int power;
    /** Game time of the most recent summoned child **/
    protected long summonTimestamp;

    //// SNOW FUEL ////
    public static final Map<ItemPredicate, Integer> SNOW_FUEL_SET = new HashMap<>();
    static {
        SNOW_FUEL_SET.put(ItemPredicate.Builder.item().of(ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation("forge", "snowballs"))).build(), 2);
        SNOW_FUEL_SET.put(ItemPredicate.Builder.item().of(Items.SNOW).build(), 4);
        SNOW_FUEL_SET.put(ItemPredicate.Builder.item().of(Items.ICE).build(), 4);
        SNOW_FUEL_SET.put(ItemPredicate.Builder.item().of(Items.SNOW_BLOCK).build(), 10);
        SNOW_FUEL_SET.put(ItemPredicate.Builder.item().of(Items.WATER_BUCKET).build(), getMaxSnow() / 2);
        SNOW_FUEL_SET.put(ItemPredicate.Builder.item().of(Items.POWDER_SNOW_BUCKET).build(), getMaxSnow());
    }

    // GECKOLIB //
    protected AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected int brickTimer;
    protected int snowO;

    //// CONSTRUCTOR ////

    public RimeiteQueen(EntityType<? extends RimeiteQueen> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.12D)
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    public static boolean checkRimeiteQueenSpawnRules(EntityType<? extends RimeiteQueen> entityType, LevelAccessor level, MobSpawnType mobSpawnType, BlockPos pos, RandomSource random) {
        // check sky visibility
        if(!level.canSeeSky(pos.above())) {
            return false;
        }
        // check invalid biome
        Biome biome = level.getBiome(pos).value();
        if(biome.shouldSnowGolemBurn(pos)) {
            return false;
        }
        // check for nearby queens
        List<RimeiteQueen> list = level.getEntitiesOfClass(RimeiteQueen.class, new AABB(pos).inflate(10.0D));
        if(!list.isEmpty()) {
            return false;
        }
        // check other mob spawn rules
        return Mob.checkMobSpawnRules(entityType, level, mobSpawnType, pos, random);
    }

    //// METHODS ////

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_STATE, (byte) 0);
        getEntityData().define(DATA_SNOW, 0);
        getEntityData().define(DATA_BRICK, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Goals
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new RimeiteQueen.FindNewHomeGoal(this, 200, 10));
        this.goalSelector.addGoal(4, new RimeiteQueen.SummonRimeiteGoal(this, SUMMON_COOLDOWN));
        this.goalSelector.addGoal(5, new RimeiteQueen.MakeBrickGoal(this));
        this.goalSelector.addGoal(6, new RimeiteQueen.RimeiteQueenWanderGoal(this, 0.9D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if(!level.isClientSide()) {
            // take damage in warm biomes
            Biome biome = level.getBiome(blockPosition()).value();
            if(biome.shouldSnowGolemBurn(blockPosition())) {
                hurt(DamageSource.ON_FIRE, 1.0F);
            }
            // update igloo builder based on conditions
            if(tickCount % 25 == 1) {
                checkAndUpdateIglooBuilder();
            }
            // periodically check if igloo is complete
            if(hasIglooBuilder() && tickCount > 50 && random.nextInt(isIglooComplete() ? 300 : 100) == 0) {
                updateIglooComplete();
            }
            // capture snow
            captureSnow();
            // rarely destroy held brick
            if(getHasBrick() && random.nextFloat() < (isIglooComplete() ? 0.005F : 0.0004F)) {
                setHasBrick(false);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // update brick timer
        if(brickTimer > 0) {
            if(--brickTimer <= 0 && !level.isClientSide()) {
                setState(STATE_IDLE);
            }
        }
        // update snow amount
        updateOldSnow();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        // take snow
        /*if(itemStack.is(Items.BUCKET) && getSnow() >= getMaxSnow()) {
            if(!level.isClientSide()) {
                setSnow(0);
                itemStack.shrink(1);
                player.getInventory().add(new ItemStack(Items.POWDER_SNOW_BUCKET));
            }
            return InteractionResult.SUCCESS;
        }*/
        int snowAmount = getSnowAmountForItem(itemStack);
        // use items to fill snow
        if(snowAmount > 0 && getSnow() < getMaxSnow()) {
            if(!level.isClientSide()) {
                // add the snow
                addSnow(snowAmount);
                // shrink or replace the item
                if(itemStack.getCount() == 1) {
                    player.setItemInHand(hand, itemStack.getCraftingRemainingItem());
                }
                itemStack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevel, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        this.power = serverLevel.getRandom().nextIntBetweenInclusive(MIN_POWER, MAX_POWER);
        this.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(HEALTH_BONUS_UUID, "Rimeite Power Bonus", power * 2.0D, AttributeModifier.Operation.ADDITION));
        this.setHealth(getMaxHealth());
        int snow = serverLevel.getRandom().nextInt(getMaxSnow());
        this.setSnow(snow);
        return super.finalizeSpawn(serverLevel, difficulty, spawnType, spawnDataIn, dataTag);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        return 0.25F;
    }

    @Override
    public void push(Entity entity) {
        if(entity.getType() != SWTRegistry.EntityReg.RIMEITE.get()) {
            super.push(entity);
        }
    }

    @Override
    protected void actuallyHurt(final DamageSource source, final float amount) {
        super.actuallyHurt(source, amount);
        // alert nearby children rimeites
        if(source.getEntity() instanceof LivingEntity sourceEntity) {
            double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
            List<Rimeite> list = level.getEntitiesOfClass(Rimeite.class, getBoundingBox().inflate(followRange), r -> r.isQueen(RimeiteQueen.this));
            for (Rimeite rimeite : list) {
                rimeite.setLastHurtByMob(sourceEntity);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return damageSource == DamageSource.FREEZE || super.isInvulnerableTo(damageSource);
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    //// HELPER METHODS ////

    /**
     * @param level the level
     * @param pos the block position
     * @return the type of precipitation at the given position, or NONE if there is currently none
     */
    protected static Optional<Biome.Precipitation> getPrecipitationAt(final Level level, final BlockPos pos) {
        if (!level.isRaining()) {
            return Optional.empty();
        } else if (!level.canSeeSky(pos)) {
            return Optional.empty();
        } else if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return Optional.empty();
        } else {
            Biome biome = level.getBiome(pos).value();
            return Optional.of(biome.getPrecipitation());
        }
    }

    /**
     * @param power the queen power stat
     * @return the maximum number of children this queen can have
     */
    public int getMaxChildren(final int power) {
        return 3 + power * 3 / 2;
    }

    /**
     * @param power the queen power stat
     * @return the desired igloo width based on the given power
     */
    public int getIglooWidth(final int power) {
        return Math.min(48, 6 + ((power / 2) * 2) + 1);
    }

    /**
     * @param power the queen power stat
     * @return the desired igloo depth based on the given power
     */
    public int getIglooDepth(final int power) {
        return -Math.min(8, power / 2 + 2);
    }

    /**
     * Attempts to update current snow based on biome and precipitation
     * @return true if the current snow amount changed
     */
    public boolean captureSnow() {
        if(getSnow() < getMaxSnow() && random.nextInt(10) == 0) {
            int snowAmount = 0;
            // determine precipitation at this position, if any
            BlockPos pos = hasIglooBuilder() ? getIglooBuilder().getCenter().above() : blockPosition().above();
            Biome.Precipitation precip = getPrecipitationAt(level, pos).orElse(Biome.Precipitation.NONE);
            // determine snow amount to add
            if(precip == Biome.Precipitation.RAIN) {
                snowAmount = 2;
            } else if(precip == Biome.Precipitation.SNOW) {
                snowAmount = 4;
            } else if(random.nextInt(level.isRaining() ? 10 : 30) == 0) {
                snowAmount = 1;
            }
            // add snow
            if (snowAmount > 0) {
                addSnow(snowAmount);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks and updates the old snow amount to be closer to the new snow amount
     * @return true if the old snow amount changed
     */
    public boolean updateOldSnow() {
        final int oldSnow = getOldSnow();
        final int currentSnow = getSnow();
        if(oldSnow < currentSnow) {
            setOldSnow(oldSnow + 1);
            return true;
        }
        if(oldSnow > currentSnow) {
            setOldSnow(oldSnow - 1);
            return true;
        }
        return false;
    }

    /**
     * @param itemStack the item stack
     * @return the amount of snow to add when consuming this itemstack
     */
    public int getSnowAmountForItem(final ItemStack itemStack) {
        for(Map.Entry<ItemPredicate, Integer> entry : SNOW_FUEL_SET.entrySet()) {
            if(entry.getKey().matches(itemStack)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    //// IGLOO BUILDER ////

    /**
     * @return true if the entity has an igloo builder that is not empty
     */
    public boolean hasIglooBuilder() {
        return !IglooBuilder.EMPTY.equals(this.iglooBuilder);
    }

    /**
     * Update the igloo builder and home position
     * @param iglooBuilder The updated igloo builder, or {@link IglooBuilder#EMPTY}
     */
    public void setIglooBuilder(@Nonnull IglooBuilder iglooBuilder) {
        this.iglooBuilder = iglooBuilder;
        if(IglooBuilder.EMPTY.equals(this.iglooBuilder)) {
            clearRestriction();
            iglooComplete = false;
        } else {
            restrictTo(this.iglooBuilder.getCenter(), Math.max(1, this.iglooBuilder.getWidth() - 2));
        }
    }

    public IglooBuilder getIglooBuilder() {
        return iglooBuilder;
    }

    public boolean isIglooComplete() {
        return iglooComplete;
    }

    /**
     * Checks if the current igloo builder is valid and resets it if not
     */
    protected void checkAndUpdateIglooBuilder() {
        if(!hasIglooBuilder()) {
            return;
        }
        // verify position can see the sky
        if(!level.canSeeSky(iglooBuilder.getCenter())) {
            setIglooBuilder(IglooBuilder.EMPTY);
            return;
        }
        // verify close to home position
        if(!blockPosition().closerThan(iglooBuilder.getCenter(), iglooBuilder.getWidth() * 2)) {
            setIglooBuilder(IglooBuilder.EMPTY);
            return;
        }
    }

    /**
     * Checks all igloo blocks for completion
     */
    protected void updateIglooComplete() {
        iglooComplete = hasIglooBuilder() && getIglooBuilder().isComplete(level);
    }

    //// STATE ////

    public byte getState() {
        return getEntityData().get(DATA_STATE);
    }

    public void setState(final byte state) {
        getEntityData().set(DATA_STATE, state);
    }

    public boolean isBricking() {
        return getState() == STATE_BRICK;
    }

    public boolean isIdle() {
        return getState() == STATE_IDLE;
    }

    @Override
    public void handleEntityEvent(byte event) {
        switch(event) {
            case START_BRICKING_EVENT:
                brickTimer = BRICK_TIME;
                break;
            case SUMMON_CHILD_EVENT:
                // add particles
                final double particleMotion = 0.15D;
                for(int i = 0; i < 25; i++) {
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

    //// BRICKS ////

    public static int getMaxSnow() {
        return BRICK_COST * BRICK_CAPACITY;
    }

    public int getSnow() {
        return getEntityData().get(DATA_SNOW);
    }

    public int getOldSnow() {
        return snowO;
    }

    public void setOldSnow(final int snow) {
        snowO = Mth.clamp(snow, 0, getMaxSnow());
    }

    public void setSnow(final int snow) {
        getEntityData().set(DATA_SNOW, Mth.clamp(snow, 0, getMaxSnow()));
    }

    public void addSnow(final int snow) {
        setSnow(getSnow() + snow);
    }

    public boolean canMakeBrick() {
        return getSnow() >= BRICK_COST;
    }

    public boolean getHasBrick() {
        return getEntityData().get(DATA_BRICK);
    }

    public void setHasBrick(final boolean hasBrick) {
        getEntityData().set(DATA_BRICK, hasBrick);
    }

    //// GECKOLIB ////

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        byte state = getState();
        switch(state) {
            case STATE_BRICK:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite_queen.brick", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                break;
            case STATE_IDLE:
            default:
                if(getHasBrick()) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite_queen.idle_brick_out", ILoopType.EDefaultLoopTypes.LOOP));
                } else {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rimeite_queen.idle", ILoopType.EDefaultLoopTypes.LOOP));
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

    //// NBT ////

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setState(tag.getByte(KEY_STATE));
        snowO = tag.getInt(KEY_SNOW);
        setSnow(snowO);
        setHasBrick(tag.getBoolean(KEY_BRICK));
        brickTimer = tag.getInt(KEY_BRICK_TIME);
        power = tag.getInt(KEY_POWER);
        summonTimestamp = tag.getLong(KEY_SUMMON_TIMESTAMP);
        setIglooBuilder(new IglooBuilder(tag.getCompound(KEY_IGLOO_BUILDER)));
        iglooComplete = tag.getBoolean(KEY_IGLOO_COMPLETE);
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(KEY_STATE, getState());
        tag.putInt(KEY_SNOW, getSnow());
        tag.putBoolean(KEY_BRICK, getHasBrick());
        tag.putInt(KEY_BRICK_TIME, brickTimer);
        tag.putInt(KEY_POWER, power);
        tag.putLong(KEY_SUMMON_TIMESTAMP, summonTimestamp);
        tag.put(KEY_IGLOO_BUILDER, iglooBuilder.serializeNBT());
        tag.putBoolean(KEY_IGLOO_COMPLETE, iglooComplete);
    }

    //// GOALS ////

    static class SummonRimeiteGoal extends Goal {
        protected final RimeiteQueen entity;
        protected final int summonCooldown;

        public SummonRimeiteGoal(final RimeiteQueen entity, final int summonCooldown) {
            this.entity = entity;
            this.summonCooldown = summonCooldown;
            setFlags(EnumSet.noneOf(Goal.Flag.class));
        }

        @Override
        public boolean canUse() {
            // verify no cooldown
            if(entity.level.getGameTime() - entity.summonTimestamp < summonCooldown) {
                return false;
            }
            // verify entity has a home
            if(!entity.hasIglooBuilder()) {
                return false;
            }
            // count number of children in range
            final int maxChildren = entity.getMaxChildren(entity.power);
            final double range = entity.getAttributeValue(Attributes.FOLLOW_RANGE) + 16.0D;
            List<Rimeite> list = entity.level.getEntitiesOfClass(Rimeite.class, entity.getBoundingBox().inflate(range), r -> r.isQueen(entity));
            return list.size() < maxChildren;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            ServerLevel serverLevel = (ServerLevel) entity.level;
            entity.summonTimestamp = serverLevel.getGameTime();
            Rimeite.create(serverLevel, entity);
            serverLevel.broadcastEntityEvent(entity, SUMMON_CHILD_EVENT);
        }
    }

    static class RimeiteQueenWanderGoal extends WaterAvoidingRandomStrollGoal {

        protected final RimeiteQueen entity;

        public RimeiteQueenWanderGoal(final RimeiteQueen entity, final double speed) {
            super(entity, speed);
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            if(entity.hasIglooBuilder() && entity.isWithinRestriction()) {
                setInterval(200);
                return false;
            }
            setInterval(60);
            return super.canUse();
        }
    }

    static class FindNewHomeGoal extends MoveToBlockGoal {

        protected final RimeiteQueen entity;
        protected final int maxDuration;

        protected int duration;

        public FindNewHomeGoal(final RimeiteQueen entity, final int maxDuration, final int searchRange) {
            super(entity, 1.0D, searchRange, 1);
            this.entity = entity;
            this.maxDuration = maxDuration;
            this.duration = 0;
        }

        @Override
        public boolean canUse() {
            if(!entity.hasIglooBuilder() && super.canUse()) {
                // check for nearby queens
                List<Entity> list = entity.level.getEntities(entity, new AABB(blockPos).inflate(10.0D, 4.0D, 10.0D), e -> e.getType() == SWTRegistry.EntityReg.RIMEITE_QUEEN.get());
                return list.isEmpty();
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return !entity.hasIglooBuilder() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            duration = 1;
        }

        @Override
        public void tick() {
            if(isReachedTarget()) {
                // stop moving
                entity.getNavigation().stop();
                // send particles
                BlockPos entityPos = entity.blockPosition().below();
                BlockState blockstate = entity.level.getBlockState(entityPos);
                ((ServerLevel)entity.level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockstate).setPos(entityPos), entity.getX(), entity.getY() + 0.1D, entity.getZ(), 2, 0.5D, 0.5D, 0.5D, 0.15D);
                // update duration
                if(++duration >= maxDuration) {
                    // calculate igloo width
                    int width = entity.getIglooWidth(entity.power);
                    int depth = entity.getIglooDepth(entity.power);
                    // update igloo builder
                    entity.setIglooBuilder(new IglooBuilder(blockPos.above(), width, depth));
                    // send particles
                    ((ServerLevel)entity.level).sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 0.5D, entity.getZ(), 15, 0.45D, 0.45D, 0.45D, 0.0D);
                    // finish
                    stop();
                    return;
                }
            }
            super.tick();
        }

        @Override
        protected int nextStartTick(PathfinderMob mob) {
            return reducedTickDelay(100 + mob.getRandom().nextInt(100));
        }

        @Override
        public void stop() {
            super.stop();
            this.duration = 0;
        }

        @Override
        public double acceptedDistance() {
            return 1.25D;
        }

        @Override
        protected BlockPos getMoveToTarget() {
            return blockPos;
        }

        @Override
        protected boolean findNearestBlock() {
            BlockPos entityPos = entity.blockPosition();
            if(isValidTarget(entity.level, entityPos)) {
                blockPos = entityPos;
                return true;
            }
            return super.findNearestBlock();
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            // check for sky visibility
            if(!level.canSeeSky(pos)) {
                return false;
            }
            // check for empty block
            BlockState blockState = level.getBlockState(pos);
            if(!(blockState.isAir() || blockState.is(Blocks.SNOW))) {
                return false;
            }
            // check for supporting block
            BlockPos below = pos.below();
            BlockState blockStateBelow = level.getBlockState(below);
            if(!blockStateBelow.isFaceSturdy(level, below, Direction.UP)) {
                return false;
            }
            // check for non-snow biome
            Biome biome = level.getBiome(pos).value();
            if(biome.shouldSnowGolemBurn(pos)) {
                return false;
            }
            // check for air or replaceable blocks around the position
            int width = Math.max(2, entity.getIglooWidth(entity.power) / 2);
            BlockState replace;
            for(BlockPos p : BlockPos.betweenClosed(pos.offset(-width, 0, -width), pos.offset(width, width, width))) {
                replace = level.getBlockState(p);
                if(!(replace.isAir() || replace.getMaterial().isReplaceable())) {
                    return false;
                }
            }
            // all checks passed
            return true;
        }
    }

    public class MakeBrickGoal extends Goal {

        protected final RimeiteQueen entity;

        public MakeBrickGoal(final RimeiteQueen entity) {
            this.entity = entity;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return entity.isIdle() && entity.hasIglooBuilder() && !entity.isIglooComplete()
                    && !entity.getHasBrick() && entity.canMakeBrick();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            // stop moving
            entity.getNavigation().stop();
            // make brick
            entity.addSnow(-BRICK_COST);
            entity.setHasBrick(true);
            entity.setState(STATE_BRICK);
            entity.brickTimer = BRICK_TIME;
            entity.level.broadcastEntityEvent(entity, START_BRICKING_EVENT);
        }
    }
}
