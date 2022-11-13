package hearthjaw.entity;


import hearthjaw.HJMain;
import hearthjaw.HJRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
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

import java.util.EnumSet;
import java.util.UUID;

public class Hearthjaw extends AgeableMob implements NeutralMob, IAnimatable {

    // SYNCHED DATA //
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Hearthjaw.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_FUEL = SynchedEntityData.defineId(Hearthjaw.class, EntityDataSerializers.INT);
    private static final String KEY_STATE = "State";
    private static final String KEY_FUEL = "Fuel";
    private static final String KEY_NAPPING_TIME = "Napping";
    private static final String KEY_WANTS_TO_NAP = "WantsToNap";
    // STATES //
    private static final byte STATE_IDLE = (byte) 0;
    private static final byte STATE_BITE = (byte) 1;
    private static final byte STATE_BREATHE = (byte) 2;
    private static final byte STATE_NAP = (byte) 3;
    // CONSTANTS //
    private static final int GOO_COST = 200;

    // SERVER SIDE VARIABLES //
    private boolean isMovingToCold;
    private boolean isWarm;
    private int nappingTime;
    private boolean wantsToNap;

    // NEUTRAL MOB //
    protected static final UniformInt ANGER_RANGE = TimeUtil.rangeOfSeconds(20, 39);
    protected int angerTime;
    protected UUID angerTarget;

    // GECKOLIB //
    private AnimationFactory factory = GeckoLibUtil.createFactory(this);

    //// CONSTRUCTOR ////

    public Hearthjaw(EntityType<? extends AgeableMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.26F)
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    //// FUEL ////

    public boolean isWarm() {
        return isWarm;
    }

    public boolean hasFuel() {
        return getFuel() > 0;
    }

    public int getFuel() {
        return getEntityData().get(DATA_FUEL);
    }

    public void setFuel(final int amount) {
        getEntityData().set(DATA_FUEL, amount);
    }

    public void addFuel(final int amount) {
        setFuel(Math.max(0, getFuel() + amount));
    }

    //// STATE ////

    public byte getState() {
        return getEntityData().get(DATA_STATE);
    }

    public void setState(final byte state) {
        // DEBUG
        HJMain.LOGGER.debug("Set state to " + state);
        getEntityData().set(DATA_STATE, state);
    }

    public boolean isNapping() {
        return getState() == STATE_NAP;
    }

    public boolean isIdle() {
        return getState() == STATE_IDLE;
    }

    public boolean isBiting() {
        return getState() == STATE_BITE;
    }

    public boolean isBreathing() {
        return getState() == STATE_BREATHE;
    }

    //// METHODS ////

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_STATE, (byte) 0);
        getEntityData().define(DATA_FUEL, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Goals
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new Hearthjaw.HearthjawAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new Hearthjaw.NapGoal(this, 1200));
        this.goalSelector.addGoal(5, new Hearthjaw.StartNappingGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new Hearthjaw.PlaceGooGoal(this, 0.9D, 20));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        // Target goals
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, true));

    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(DATA_FUEL)) {
            this.isWarm = getFuel() > 0;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // chance to start wanting to nap
        if (!wantsToNap && isIdle() && null == getTarget()) {
            float chance = level.isNight() ? 0.08F : 0.01F;
            if (random.nextFloat() < chance) {
                wantsToNap = true;
            }
        }
        // chance to stop wanting to nap
        if(wantsToNap) {
            float chance = level.isNight() ? 0.01F : 0.08F;
            if (random.nextFloat() < chance) {
                wantsToNap = false;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // spawn particles
        if (isWarm() && level.isClientSide() && random.nextInt(9) == 1) {
            ParticleOptions particle = random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.LAVA;
            level.addParticle(particle,
                    getX() + 2 * (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + 2 * (random.nextDouble() - 0.5D) * getBbWidth(),
                    0, 0, 0);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // update napping
        if (isNapping()) {
            setState(STATE_IDLE);
        }
        ItemStack itemStack = player.getItemInHand(hand);
        final int burnTime = ForgeHooks.getBurnTime(itemStack, RecipeType.SMELTING);
        // DEBUG
        HJMain.LOGGER.debug("item burn time=" + burnTime + " and isWarm=" + isWarm());
        if (getFuel() < GOO_COST && burnTime > 0) {
            // add fuel
            addFuel(burnTime);
            // consume item
            if (itemStack.getCount() > 1) {
                itemStack.shrink(1);
            } else {
                itemStack = itemStack.getCraftingRemainingItem();
            }
            player.setItemInHand(hand, itemStack);
            // add particles
            if (level instanceof ServerLevel) {
                ((ServerLevel) level).sendParticles(ParticleTypes.FLAME, this.getX(), this.getEyeY(), this.getZ(), 12, 0.5D, 0.5D, 0.5D, 0.5D);
            }
            // DEBUG
            HJMain.LOGGER.debug("consumed item, fuel=" + getFuel() + " and isWarm=" + isWarm());
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.FREEZE || super.isInvulnerableTo(source);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, amount);
        if (isNapping()) {
            setState(STATE_IDLE);
            nappingTime = 0;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        setState(STATE_BITE);
        return super.doHurtTarget(target);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevel, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        if(serverLevel.getRandom().nextFloat() < 0.20F) {
            int fuel = GOO_COST + serverLevel.getRandom().nextInt(GOO_COST * 2);
            setFuel(fuel);
        }
        return super.finalizeSpawn(serverLevel, difficulty, spawnType, spawnDataIn, dataTag);
    }

    //// AGEABLE MOB ////

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob parent) {
        return HJRegistry.EntityReg.HEARTHJAW.get().create(serverLevel);
    }

    @Override
    public boolean canBreed() {
        // TODO
        return false;
    }

    //// NEUTRAL MOB ////

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(ANGER_RANGE.sample(this.random));
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        this.angerTime = time;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.angerTime;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID target) {
        this.angerTarget = target;
    }

    @Override
    public UUID getPersistentAngerTarget() {
        return this.angerTarget;
    }

    //// GECKOLIB ////

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        byte state = getState();
        switch(state) {
            case STATE_BITE:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.hearthjaw.bite", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                break;
            case STATE_BREATHE:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.hearthjaw.breathe", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                break;
            case STATE_NAP:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.hearthjaw.nap", ILoopType.EDefaultLoopTypes.LOOP));
                break;
            case STATE_IDLE:
            default:
                if(this.getDeltaMovement().horizontalDistanceSqr() > 2.500000277905201E-7D) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.hearthjaw.walk", ILoopType.EDefaultLoopTypes.LOOP));
                } else {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.hearthjaw.idle", ILoopType.EDefaultLoopTypes.LOOP));
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
        readPersistentAngerSaveData(level, tag);
        setState(tag.getByte(KEY_STATE));
        setFuel(tag.getInt(KEY_FUEL));
        wantsToNap = tag.getBoolean(KEY_WANTS_TO_NAP);
        nappingTime = tag.getInt(KEY_NAPPING_TIME);
        isWarm = getFuel() > 0;
        // DEBUG
        HJMain.LOGGER.debug("from NBT: fuel=" + getFuel() + ", isWarm=" + isWarm());
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        addPersistentAngerSaveData(tag);
        tag.putByte(KEY_STATE, getState());
        tag.putInt(KEY_FUEL, getFuel());
        tag.putBoolean(KEY_WANTS_TO_NAP, wantsToNap);
        tag.putInt(KEY_NAPPING_TIME, nappingTime);
    }

    //// GOALS ////

    static class NapGoal extends Goal {

        private final Hearthjaw entity;
        private final int duration;

        public NapGoal(Hearthjaw entity, int duration) {
            this.entity = entity;
            this.duration = duration;
            this.setFlags(EnumSet.allOf(Goal.Flag.class));
        }

        @Override
        public boolean canUse() {
            return entity.isNapping();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && entity.random.nextInt(Math.max(1, duration * 2 - entity.nappingTime)) > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            entity.getNavigation().stop();
            entity.nappingTime++;
        }

        @Override
        public void stop() {
            entity.nappingTime = 0;
            entity.setState(STATE_IDLE);
        }
    }

    static class StartNappingGoal extends MoveToBlockGoal {
        private final Hearthjaw entity;

        public StartNappingGoal(Hearthjaw entity, double speed) {
            super(entity, speed, 5, 1);
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            return entity.wantsToNap && entity.isIdle() && null == entity.getTarget() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return entity.wantsToNap && entity.isIdle() && null == entity.getTarget() && super.canContinueToUse();
        }

        @Override
        public void tick() {
            if (this.isReachedTarget()) {
                entity.getNavigation().stop();
                entity.setState(STATE_NAP);
                entity.wantsToNap = false;
                entity.nappingTime = 0;
                stop();
            }
            super.tick();
        }

        @Override
        public double acceptedDistance() {
            return 4.0D;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            final BlockState blockState = level.getBlockState(pos);
            return isGoo(level, blockState, pos) || isLitCampfire(level, blockState, pos);
        }

        private static boolean isGoo(LevelReader level, BlockState blockState, BlockPos pos) {
            return blockState.is(HJRegistry.BlockReg.GOO.get());
        }

        private static boolean isLitCampfire(LevelReader level, BlockState blockState, BlockPos pos) {
            return blockState.is(BlockTags.CAMPFIRES) && blockState.hasProperty(CampfireBlock.LIT) && blockState.getValue(CampfireBlock.LIT);
        }
    }

    static class PlaceGooGoal extends MoveToBlockGoal {
        
        private final Hearthjaw entity;
        private final int maxDuration;
        private int progress;
        
        public PlaceGooGoal(Hearthjaw entity, double speed, int duration) {
            super(entity, speed, 12, 1);
            this.entity = entity;
            this.maxDuration = duration;
        }

        @Override
        public boolean canUse() {
            return entity.isWarm() && entity.getFuel() >= GOO_COST && null == entity.getTarget() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            // DEBUG
            HJMain.LOGGER.debug("Started PlaceGooGoal");
        }

        @Override
        public void tick() {
            if (this.isReachedTarget()) {
                // stop moving and look at target block
                Vec3 vec = Vec3.atCenterOf(blockPos);
                entity.getNavigation().stop();
                entity.getLookControl().setLookAt(vec);
                // set breathing state
                if(!entity.isBreathing()) {
                    entity.setState(STATE_BREATHE);
                }
                if (++this.progress >= maxDuration) {
                    // place goo block
                    if (ForgeEventFactory.getMobGriefingEvent(entity.level, entity)) {
                        entity.level.setBlock(this.blockPos, HJRegistry.BlockReg.GOO.get().defaultBlockState(), Block.UPDATE_ALL);
                    }
                    // play sound and add particles
                    if(entity.level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, entity, SoundEvents.FOX_SPIT, entity.getSoundSource(), 1.0F, entity.random.nextFloat() * 0.4F + 0.8F);
                        serverLevel.sendParticles(ParticleTypes.FLAME, vec.x(), vec.y(), vec.z(), 20, 0.5D, 0.5D, 0.5D, 0.5D);
                    }
                    // deplete fuel
                    entity.addFuel(-GOO_COST);
                    // stop goal
                    this.stop();
                    // update napping
                    entity.wantsToNap = entity.getFuel() < GOO_COST || entity.random.nextInt(5) == 1;
                    // DEBUG
                    HJMain.LOGGER.debug("placed goo block, wantsToNap=" + entity.wantsToNap);
                    return;
                }
            }
            super.tick();
        }

        @Override
        public void stop() {
            super.stop();
            entity.isMovingToCold = false;
            progress = 0;
            if(entity.isBreathing()) {
                entity.setState(STATE_IDLE);
            }
        }

        @Override
        public double acceptedDistance() {
            return 4.0D;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            final BlockState blockState = level.getBlockState(pos);
            final int lightLevel = level.getBrightness(LightLayer.BLOCK, pos);
            if (lightLevel < 1 && (blockState.isAir() || blockState.getMaterial() == Material.TOP_SNOW)
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos, Direction.UP)) {
                entity.isMovingToCold = true;
                return true;
            }
            return false;
        }
    }

    static class HearthjawAttackGoal extends MeleeAttackGoal {

        private final Hearthjaw entity;

        public HearthjawAttackGoal(Hearthjaw entity, double speed, boolean persistent) {
            super(entity, speed, persistent);
            this.entity = entity;
        }

        @Override
        public void tick() {
            super.tick();
            if(getTicksUntilNextAttack() > 10) {
                entity.getNavigation().stop();
            } else if(entity.isBiting()) {
                entity.setState(STATE_IDLE);
            }
        }
    }

}
