package hearthjaw.entity;


import hearthjaw.HJMain;
import hearthjaw.HJRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class Bloomina extends Animal implements FlyingAnimal, IAnimatable {

    // SYNCHED DATA //
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Bloomina.class, EntityDataSerializers.BYTE);
    private static final String KEY_STATE = "State";
    private static final String KEY_FERTILE = "Fertile";
    private static final String KEY_HIDING_TIME = "HidingTime";
    private static final String KEY_COOKING_TIME = "CookingTime";
    private static final String KEY_RESTRICTION = "Home";
    private static final String KEY_WANDER_DISTANCE = "WanderDistance";
    // STATES //
    protected static final byte STATE_IDLE = (byte) 0;
    protected static final byte STATE_HIDE = (byte) 1;
    // EVENTS //
    protected static final byte START_BURPING_EVENT = (byte) 9;
    protected static final byte START_HIDING_EVENT = (byte) 10;

    // CONSTANTS //
    protected static final TagKey<Item> FOOD = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(HJMain.MODID, "bloomina_food"));
    protected static final TagKey<Item> COOK = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(HJMain.MODID, "bloomina_cook"));
    protected static final TagKey<Block> HOME = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(HJMain.MODID, "bloomina_home"));
    protected static final int BURP_TIME = 26;
    protected static final int START_HIDE_TIME = 40;
    protected static final int MIN_HIDING_TIME = 90 + START_HIDE_TIME;
    protected static final int MIN_COOKING_TIME = 180;
    protected static final Predicate<LivingEntity> DO_FOLLOW_PLAYER = e -> e.getMainHandItem().is(HJRegistry.ItemReg.BLOOMLIGHT_ROD.get()) || e.getOffhandItem().is(HJRegistry.ItemReg.BLOOMLIGHT_ROD.get());

    // SERVER SIDE VARIABLES //
    protected boolean isFertile;
    protected int hidingTimeLeft;
    protected int cookingTimeLeft;

    // GECKOLIB //
    protected AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected int burpingTimer;
    protected int startHidingTimer;

    //// CONSTRUCTOR ////

    public Bloomina(EntityType<? extends Bloomina> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FLYING_SPEED, 1.1D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    //// METHODS ////

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DATA_STATE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Goals
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MovingLightSourceGoal<>(this, 3, false, Bloomina::getLightLevel));
        this.goalSelector.addGoal(2, new Bloomina.HideGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0F));
        this.goalSelector.addGoal(4, new Bloomina.PlantOffspringGoal(this, 1.0F, 6));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.15D, Ingredient.of(FOOD), false));
        this.goalSelector.addGoal(6, new Bloomina.FollowPlayerGoal(this, 1.5D, 1200, 80, 15.0F, DO_FOLLOW_PLAYER));
        this.goalSelector.addGoal(7, new Bloomina.FeedHearthjawGoal(this, 1.0F));
        this.goalSelector.addGoal(8, new Bloomina.StartNappingGoal(this));
        this.goalSelector.addGoal(9, new Bloomina.GoToLanternGoal(this, 9, 1.0F, 10));
        this.goalSelector.addGoal(10, new Bloomina.BloominaWanderGoal(this, 40, 0.8F));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Hearthjaw.class, 8.0F));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // update restriction
        if(tickCount % 55 == 1 && hasRestriction() && getRestrictCenter() != BlockPos.ZERO) {
            BlockState blockState = level.getBlockState(getRestrictCenter());
            if(!blockState.is(HOME)) {
                clearRestriction();
            }
        }
        // update burping animation
        if(burpingTimer > 0) {
            --burpingTimer;
            if(burpingTimer == BURP_TIME - 4) {
                // play sound
                this.playSound(getBurpSound(), getSoundVolume() + 0.2F, 1.1F + random.nextFloat() * 0.4F);
            }
        }
        // update hiding animation
        if(startHidingTimer > 0) {
            --startHidingTimer;
        }
        // update cooking time
        if(cookingTimeLeft > 0 && level instanceof ServerLevel serverLevel) {
            updateCooking(serverLevel);
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if(isHiding()) {
            return InteractionResult.FAIL;
        }
        ItemStack itemStack = player.getItemInHand(hand);
        if(!isBaby() && canCook(itemStack)) {
            if(player.level instanceof ServerLevel serverLevel && startCooking(serverLevel, itemStack)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevel, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        this.setLeftHanded(false);
        if(serverLevel.getRandom().nextInt(15) == 0) {
            setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHARCOAL));
        }
        return super.finalizeSpawn(serverLevel, difficulty, spawnType, spawnDataIn, dataTag);
    }

    @Override
    protected void actuallyHurt(final DamageSource source, final float amount) {
        super.actuallyHurt(source, amount);
        // update state
        scare();
        int hidingTime = MIN_HIDING_TIME + random.nextInt(MIN_HIDING_TIME);
        hidingTimeLeft = Math.max(hidingTimeLeft, hidingTime);
        // alert nearby hearthjaws
        if(source.getEntity() instanceof LivingEntity sourceEntity) {
            List<Hearthjaw> list = level.getEntitiesOfClass(Hearthjaw.class, getBoundingBox().inflate(10.0D));
            for (Hearthjaw hearthjaw : list) {
                hearthjaw.setLastHurtByMob(sourceEntity);
            }
        }
    }

    //// ANIMAL ////

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob parent) {
        return HJRegistry.EntityReg.BLOOMINA.get().create(serverLevel);
    }

    @Override
    public boolean canBreed() {
        return !isFertile && !isHiding();
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(FOOD);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel serverLevel, Animal parent) {
        // update fertile flag
        this.isFertile = true;
        // reset age and love values
        this.setAge(6000);
        parent.setAge(6000);
        this.resetLove();
        parent.resetLove();
        // broadcast event
        serverLevel.broadcastEntityEvent(this,EntityEvent.IN_LOVE_HEARTS);
        if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            serverLevel.addFreshEntity(new ExperienceOrb(serverLevel, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
        }
    }

    //// GECKOLIB ////

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        byte state = getState();
        switch(state) {
            case STATE_HIDE:
                if(startHidingTimer > 0) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.hide", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME));
                } else {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.hiding", ILoopType.EDefaultLoopTypes.LOOP));
                }
                break;
            case STATE_IDLE:
            default:
                if(burpingTimer > 0) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.burp", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                } else {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.idle", ILoopType.EDefaultLoopTypes.LOOP));
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
        isFertile = tag.getBoolean(KEY_FERTILE);
        hidingTimeLeft = tag.getInt(KEY_HIDING_TIME);
        cookingTimeLeft = tag.getInt(KEY_COOKING_TIME);
        if(tag.contains(KEY_RESTRICTION) && tag.contains(KEY_WANDER_DISTANCE)) {
            BlockPos home = NbtUtils.readBlockPos(tag.getCompound(KEY_RESTRICTION));
            int wanderDistance = tag.getInt(KEY_WANDER_DISTANCE);
            restrictTo(home, wanderDistance);
        }
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(KEY_STATE, getState());
        tag.putBoolean(KEY_FERTILE, isFertile);
        tag.putInt(KEY_HIDING_TIME, hidingTimeLeft);
        tag.putInt(KEY_COOKING_TIME, cookingTimeLeft);
        if(hasRestriction() && getRestrictCenter() != BlockPos.ZERO) {
            tag.put(KEY_RESTRICTION, NbtUtils.writeBlockPos(getRestrictCenter()));
            tag.putInt(KEY_WANDER_DISTANCE, (int)getRestrictRadius());
        }
    }

    //// FLYING ANIMAL ////

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.isEmptyBlock(pos) ? 10.0F : 0.0F;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    //// MOVING LIGHT SOURCE ////

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    public int getLightLevel() {
        return isBaby() ? 8 : 12;
    }

    //// SOUNDS ////

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return HJRegistry.SoundReg.BLOOMINA_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return isHiding() ? 400 : 200;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    protected SoundEvent getBurpSound() {
        return HJRegistry.SoundReg.BLOOMINA_BURP.get();
    }

    //// STATE ////

    public byte getState() {
        return getEntityData().get(DATA_STATE);
    }

    public void setState(final byte state) {
        getEntityData().set(DATA_STATE, state);
    }

    public boolean isIdle() {
        return getState() == STATE_IDLE;
    }

    public boolean isHiding() {
        return getState() == STATE_HIDE;
    }

    public void scare() {
        if(!isHiding()) {
            setState(STATE_HIDE);
            startHidingTimer = START_HIDE_TIME;
            if(!level.isClientSide()) {
                level.broadcastEntityEvent(this, START_HIDING_EVENT);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        switch(event) {
            case START_HIDING_EVENT:
                startHidingTimer = START_HIDE_TIME;
                break;
            case START_BURPING_EVENT:
                burpingTimer = BURP_TIME;
                break;
            default:
                super.handleEntityEvent(event);
        }
    }

    //// COOKING ////

    public boolean canCook(final ItemStack itemStack) {
        return itemStack.is(COOK);
    }

    public boolean startCooking(ServerLevel serverLevel, ItemStack itemStack) {
        if(canCook(itemStack) && getMainHandItem().isEmpty()) {
            setItemInHand(InteractionHand.MAIN_HAND, itemStack.split(1));
            cookingTimeLeft = MIN_COOKING_TIME + random.nextInt(MIN_COOKING_TIME);
            return true;
        }
        return false;
    }

    public void updateCooking(ServerLevel serverLevel) {
        if(cookingTimeLeft > 0) {
            // decrement cooking time
            if(--cookingTimeLeft <= 0) {
                stopCooking(serverLevel);
            }
            // add particles
            if(random.nextInt(4) == 0) {
                ParticleOptions particle = random.nextBoolean() ? ParticleTypes.SMOKE : ParticleTypes.FLAME;
                serverLevel.sendParticles(particle, this.getX(), this.getY() + 0.5D, this.getZ(), 1, 0.5D, 0.5D, 0.5D, 0.0D);
            }
        }
    }

    public void stopCooking(ServerLevel serverLevel) {
        cookingTimeLeft = 0;
        ItemStack heldItem = getMainHandItem();
        if(canCook(heldItem)) {
            // update held item
            ItemStack cooked = new ItemStack(Items.CHARCOAL);
            setItemInHand(InteractionHand.MAIN_HAND, cooked);
            // broadcast event
            serverLevel.broadcastEntityEvent(this, START_BURPING_EVENT);
            burpingTimer = BURP_TIME;
            // add particles
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.5D, 0.5D, 0.5D, 0.0D);
        }
    }

    //// GOALS ////

    static class HideGoal extends Goal {

        private static final UUID ARMOR_UUID = UUID.fromString("0db6d492-07f6-4488-adc9-9887e996cb33");
        private static final AttributeModifier ARMOR_BONUS = new AttributeModifier(ARMOR_UUID, "Armor bonus", 8.0D, AttributeModifier.Operation.ADDITION);
        private final Bloomina entity;

        public HideGoal(final Bloomina entity) {
            this.entity = entity;
            this.setFlags(EnumSet.allOf(Goal.Flag.class));
        }

        @Override
        public boolean canUse() {
            return entity.isHiding();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            entity.getAttribute(Attributes.ARMOR).addTransientModifier(ARMOR_BONUS);
        }

        @Override
        public void tick() {
            // stop navigation
            entity.getNavigation().stop();
            // update hiding time
            if(--entity.hidingTimeLeft <= 0) {
                stop();
            }
        }

        @Override
        public void stop() {
            entity.setState(STATE_IDLE);
            entity.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_BONUS);
        }
    }

    static class StartNappingGoal extends Goal {

        private final Bloomina entity;

        public StartNappingGoal(final Bloomina entity) {
            this.entity = entity;
            this.setFlags(EnumSet.allOf(Goal.Flag.class));
        }

        @Override
        public boolean canUse() {
            return entity.isIdle() && (entity.level.isNight() || entity.level.isThundering()) && entity.random.nextFloat() < 0.0012F;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            entity.setState(STATE_HIDE);
            entity.hidingTimeLeft = 400 + entity.random.nextInt(100);
        }
    }

    static class BloominaWanderGoal extends Goal {

        protected final Bloomina entity;
        protected final double speed;
        protected final int chance;

        public BloominaWanderGoal(final Bloomina entity, final int chance, final double speed) {
            this.entity = entity;
            this.speed = speed;
            this.chance = Math.min(1, chance);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.entity.navigation.isDone()
                    && this.entity.isIdle()
                    && this.entity.random.nextInt(chance) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.entity.navigation.isInProgress();
        }

        @Override
        public void start() {
            Vec3 vec3 = this.findPos();
            if (vec3 != null) {
                this.entity.navigation.moveTo(this.entity.navigation.createPath(new BlockPos(vec3), 1), speed);
            }
        }

        @Nullable
        private Vec3 findPos() {
            Vec3 targetPos;
            if (!entity.isWithinRestriction()) {
                Vec3 homeVec = Vec3.atCenterOf(entity.getRestrictCenter());
                targetPos = homeVec.subtract(entity.position()).normalize();
            } else {
                targetPos = entity.getViewVector(0.0F);
            }

            Vec3 hoverVec = HoverRandomPos.getPos(this.entity, 8, 7, targetPos.x, targetPos.z, ((float) Math.PI / 2F), 3, 1);
            return hoverVec != null ? hoverVec : AirAndWaterRandomPos.getPos(this.entity, 8, 4, -2, targetPos.x, targetPos.z, Math.PI / 2D);
        }
    }

    static class PlantOffspringGoal extends MoveToBlockGoal {

        protected final Bloomina entity;

        public PlantOffspringGoal(Bloomina entity, double speed, int searchRange) {
            super(entity, speed, searchRange, 4);
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            return entity.isFertile && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return entity.isFertile && super.canContinueToUse();
        }

        @Override
        public void tick() {
            BlockPos budPos = blockPos.above();
            if(isReachedTarget()) {
                // place bud
                entity.level.setBlock(budPos, HJRegistry.BlockReg.BLOOMINA_BUD.get().defaultBlockState(), Block.UPDATE_ALL);
                // play sound
                entity.playSound(SoundEvents.CROP_PLANTED);
                // send particles
                if(entity.level instanceof ServerLevel) {
                    Vec3 vec = Vec3.atCenterOf(budPos);
                    ((ServerLevel)entity.level).sendParticles(ParticleTypes.HAPPY_VILLAGER, vec.x(), vec.y(), vec.z(), 10, 0.5D, 0.5D, 0.5D, 0.0D);
                }
                // update fertile flag
                entity.isFertile = false;
            }
            super.tick();
        }

        @Override
        public double acceptedDistance() {
            return 1.25D;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            final BlockPos above = pos.above();
            final BlockState blockState = level.getBlockState(above);
            if(!blockState.getMaterial().isReplaceable() || blockState.getMaterial().blocksMotion()) {
                return false;
            }
            if(!HJRegistry.BlockReg.BLOOMINA_BUD.get().canSurvive(blockState, level, above)) {
                return false;
            }
            // all checks passed
            return true;
        }
    }

    static class GoToLanternGoal extends MoveToBlockGoal {

        protected final Bloomina entity;
        protected final int homeRange;

        public GoToLanternGoal(Bloomina entity, int homeRange, double speed, int searchRange) {
            super(entity, speed, searchRange, 4);
            this.entity = entity;
            this.homeRange = homeRange;
        }

        @Override
        public boolean canUse() {
            return !entity.hasRestriction() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !entity.hasRestriction() && super.canContinueToUse();
        }

        @Override
        public void tick() {
            if(isReachedTarget()) {
                entity.restrictTo(blockPos, homeRange);
                // send particles
                ((ServerLevel)entity.level).sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getEyeY(), entity.getZ(), 6, 0.45D, 0.45D, 0.45D, 0.0D);
            }
            super.tick();
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getBlockState(pos).is(HOME);
        }
    }
    
    static class FeedHearthjawGoal extends Goal {
        
        protected final Bloomina entity;
        protected final double speed;
        protected final TargetingConditions conditions;
        
        protected Hearthjaw target;
        private int timeToRecalcPath;

        public FeedHearthjawGoal(Bloomina entity, double speed) {
            this.entity = entity;
            this.speed = speed;
            this.conditions = TargetingConditions.forNonCombat()
                    .selector(e -> e instanceof Hearthjaw hearthjaw && hearthjaw.getFuel() < Hearthjaw.GOO_COST);
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if(!(entity.isIdle() && entity.getMainHandItem().is(Items.CHARCOAL))) {
                return false;
            }
            // locate nearest hearthjaw
            double range = entity.getAttributeValue(Attributes.FOLLOW_RANGE);
            conditions.range(range);
            Vec3 position = entity.position();
            target = entity.level.getNearestEntity(Hearthjaw.class, conditions, entity, position.x(), position.y(), position.z(), entity.getBoundingBox().inflate(range));
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && conditions.test(entity, target) 
                    && !entity.getNavigation().isDone()
                    && !entity.position().closerThan(target.position(), getAcceptedDistance());
        }

        @Override
        public void start() {
            timeToRecalcPath = 0;
        }

        @Override
        public void tick() {
            // Copied from FollowMobGoal
            if (target != null && !entity.isLeashed()) {
                entity.getLookControl().setLookAt(target, 10.0F, (float)entity.getMaxHeadXRot());
                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = this.adjustedTickDelay(10);
                    double dx = entity.getX() - target.getX();
                    double dy = entity.getY() - target.getY();
                    double dz = entity.getZ() - target.getZ();
                    double disSq = dx * dx + dy * dy + dz * dz;
                    double minDis = getAcceptedDistance();
                    if (!(disSq <= minDis * minDis)) {
                        entity.getNavigation().moveTo(target, speed);
                    } else {
                        entity.getNavigation().stop();
                        // feed hearthjaw
                        ItemStack heldItem = entity.getMainHandItem();
                        if(target.feed(heldItem)) {
                            // shrink item stack
                            if (heldItem.getCount() > 1) {
                                heldItem.shrink(1);
                            } else {
                                heldItem = heldItem.getCraftingRemainingItem();
                            }
                            // update held item
                            if(heldItem.is(COOK)) {
                                entity.setItemInHand(InteractionHand.MAIN_HAND, heldItem);
                            } else {
                                // drop invalid items
                                Block.popResource(entity.level, entity.blockPosition(), heldItem.copy());
                                entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void stop() {
            target = null;
            entity.getNavigation().stop();
        }
        
        protected double getAcceptedDistance() {
            return 2.0D;
        }
    }

    static class FollowPlayerGoal extends Goal {

        protected static final float MIN_RANGE = 3.0F;

        protected final Bloomina entity;
        protected final double speed;
        protected final TargetingConditions conditions;
        protected final int maxDuration;
        protected final int minCooldown;
        protected final float range;

        protected Player player;
        protected int duration;
        protected int cooldown;

        protected Vec3 target;

        public FollowPlayerGoal(final Bloomina entity, final double speed, final int maxDuration, final int minCooldown,
                                final float range, final Predicate<LivingEntity> predicate) {
            this.entity = entity;
            this.speed = speed;
            this.maxDuration = maxDuration;
            this.minCooldown = Math.max(1, minCooldown);
            this.cooldown = 20;
            this.range = Math.max(MIN_RANGE, range);
            this.conditions = TargetingConditions.forNonCombat()
                    .selector(predicate);
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            // do not execute when entity is not idle
            if(!entity.isIdle()) {
                return false;
            }
            // reduce cooldown and attempt to locate a player
            if(--cooldown <= 0) {
                conditions.range(entity.getAttributeValue(Attributes.FOLLOW_RANGE));
                player = entity.level.getNearestPlayer(conditions, entity);
                // reset cooldown when no player is found
                if(null == player) {
                    cooldown = minCooldown;
                }
            }
            // verify target player exists
            return player != null;
        }

        @Override
        public boolean canContinueToUse() {
            return duration < maxDuration && entity.isIdle()
                    && player != null && target != null
                    && conditions.test(entity, player);
        }

        @Override
        public void tick() {
            if(null == player || ++duration >= maxDuration) {
                stop();
                return;
            }
            if(null == target || duration % 2 == (entity.getId() % 2)) {
                final Optional<Vec3> oTarget = getWantedPosition(player);
                if (oTarget.isPresent()) {
                    target = oTarget.get();
                    // check if entity needs to move
                    if(!entity.position().closerThan(target, 1.5D)) {
                        entity.getNavigation().moveTo(target.x(), target.y(), target.z(), speed);
                    }
                }
            }
            // spawn particles
            if(entity.random.nextInt(8) == 0) {
                ((ServerLevel)entity.level).sendParticles(ParticleTypes.GLOW, target.x(), target.y(), target.z(), 1, 0.5D, 0.5D, 0.5D, 0.05D);
            }
        }

        @Override
        public void stop() {
            cooldown = minCooldown + entity.random.nextInt(minCooldown);
            duration = 0;
            player = null;
        }

        /**
         * Calculates the player look position and returns an updated
         * target location if the look position is valid (not too close
         * to the player and not too close to the previous location)
         * @param player the player
         * @return the updated position, or empty if no conditions passed
         * @see #raytraceFromEntity(LivingEntity, float)
         */
        private Optional<Vec3> getWantedPosition(final Player player) {
            final Vec3 start = player.getEyePosition(1.0F);
            BlockHitResult hitResult = raytraceFromEntity(player, range);
            Vec3 location = hitResult.getLocation();
            // verify hit vec is not too close to the player or the previous location
            if(!start.closerThan(location, MIN_RANGE) && (null == target || !target.closerThan(location, 1.0D))) {
                // scale the hit location back 1.5 blocks
                Vec3 fromPlayer = start.subtract(location);
                fromPlayer = location.add(fromPlayer.normalize().scale(1.5D));
                return Optional.of(fromPlayer);
            }
            // no checks passed
            return Optional.empty();
        }

        private static BlockHitResult raytraceFromEntity(final LivingEntity player, final float range) {
            // raytrace to determine which block the player is looking at within the given range
            final Vec3 startVec = player.getEyePosition(1.0F);
            final float pitch = (float) Math.toRadians(-player.getXRot());
            final float yaw = (float) Math.toRadians(-player.getYRot());
            float cosYaw = Mth.cos(yaw - (float) Math.PI);
            float sinYaw = Mth.sin(yaw - (float) Math.PI);
            float cosPitch = -Mth.cos(pitch);
            float sinPitch = Mth.sin(pitch);
            final Vec3 endVec = startVec.add(sinYaw * cosPitch * range, sinPitch * range, cosYaw * cosPitch * range);
            return player.level.clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        }
    }
}
