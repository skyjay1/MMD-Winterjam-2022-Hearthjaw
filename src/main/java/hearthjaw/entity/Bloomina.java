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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IPlantable;
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

public class Bloomina extends Animal implements FlyingAnimal, IAnimatable {

    // SYNCHED DATA //
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Bloomina.class, EntityDataSerializers.BYTE);
    private static final String KEY_STATE = "State";
    private static final String KEY_FERTILE = "Fertile";
    private static final String KEY_HIDING_TIME = "HidingTime";
    private static final String KEY_COOKING_TIME = "CookingTime";
    // STATES //
    protected static final byte STATE_IDLE = (byte) 0;
    protected static final byte STATE_BURP = (byte) 1;
    protected static final byte STATE_HIDE = (byte) 2;
    // EVENTS //
    protected static final byte START_HIDING_EVENT = (byte) 9;

    // CONSTANTS //
    protected static final TagKey<Item> FOOD = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(HJMain.MODID, "bloomina_food"));
    protected static final TagKey<Item> COOK = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(HJMain.MODID, "bloomina_cook"));
    protected static final int BURP_TIME = 26;
    protected static final int START_HIDE_TIME = 40;
    protected static final int MIN_HIDING_TIME = 90 + START_HIDE_TIME;
    protected static final int MIN_COOKING_TIME = 180;

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
                .add(Attributes.MOVEMENT_SPEED, 0.30F)
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.95F);
    }

    //// STATE ////

    public byte getState() {
        return getEntityData().get(DATA_STATE);
    }

    public void setState(final byte state) {
        // DEBUG
        HJMain.LOGGER.debug("Set Bloomina state to " + state);
        getEntityData().set(DATA_STATE, state);
    }

    public boolean isIdle() {
        return getState() == STATE_IDLE;
    }

    public boolean isBurping() {
        return getState() == STATE_BURP;
    }

    public boolean isHiding() {
        return getState() == STATE_HIDE;
    }

    @Override
    public void handleEntityEvent(byte event) {
        switch(event) {
            case START_HIDING_EVENT:
                startHidingTimer = START_HIDE_TIME;
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
            // update state
            setState(STATE_BURP);
            burpingTimer = BURP_TIME;
            // add particles
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.5D, 0.5D, 0.5D, 0.0D);
        }
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
        this.goalSelector.addGoal(1, new MovingLightSourceGoal<>(this, 4, false, Bloomina::getLightLevel));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0F));
        this.goalSelector.addGoal(4, new Bloomina.PlantOffspringGoal(this, 1.0F, 6));
        this.goalSelector.addGoal(8, new Bloomina.BloominaWanderGoal(this, 40, 0.8F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Hearthjaw.class, 8.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // update burping animation
        if(burpingTimer > 0 && --burpingTimer <= 0) {
            setState(STATE_IDLE);
        }
        // update hiding animation
        if(startHidingTimer > 0) {
            --startHidingTimer;
        }
        // update hiding time
        if(hidingTimeLeft > 0 && --hidingTimeLeft <= 0) {
            setState(STATE_IDLE);
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
        ItemStack itemStack = player.getItemInHand(hand);
        if(canCook(itemStack)) {
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
        return super.finalizeSpawn(serverLevel, difficulty, spawnType, spawnDataIn, dataTag);
    }

    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        final float multiplier = isHiding() ? 0.1F : 1.0F;
        return super.hurt(source, amount * multiplier);
    }

    @Override
    protected void actuallyHurt(final DamageSource source, final float amount) {
        super.actuallyHurt(source, amount);
        // update state
        if(!isHiding()) {
            setState(STATE_HIDE);
            startHidingTimer = START_HIDE_TIME;
            if(!level.isClientSide()) {
                level.broadcastEntityEvent(this, START_HIDING_EVENT);
            }
        }
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
        // DEBUG
        HJMain.LOGGER.debug("Bloomina is now fertile");
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
            case STATE_BURP:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.burp", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                break;
            case STATE_HIDE:
                if(startHidingTimer > 0) {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.hide", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME));
                } else {
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.hiding", ILoopType.EDefaultLoopTypes.LOOP));
                }
                break;
            case STATE_IDLE:
            default:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.idle", ILoopType.EDefaultLoopTypes.LOOP));
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
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(KEY_STATE, getState());
        tag.putBoolean(KEY_FERTILE, isFertile);
        tag.putInt(KEY_HIDING_TIME, hidingTimeLeft);
        tag.putInt(KEY_COOKING_TIME, cookingTimeLeft);
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

    public int getLightLevel() {
        return 12;
    }

    //// GOALS ////

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
            Vec3 targetPos = this.entity.getViewVector(0.0F);

            Vec3 hoverVec = HoverRandomPos.getPos(this.entity, 8, 7, targetPos.x, targetPos.z, ((float) Math.PI / 2F), 3, 1);
            return hoverVec != null ? hoverVec : AirAndWaterRandomPos.getPos(this.entity, 8, 4, -2, targetPos.x, targetPos.z, Math.PI / 2D);
        }
    }

    static class PlantOffspringGoal extends MoveToBlockGoal {

        protected final Bloomina entity;

        public PlantOffspringGoal(Bloomina entity, double speed, int searchRange) {
            super(entity, speed, searchRange, searchRange);
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
            if(isReachedTarget()) {
                BlockPos budPos = blockPos.above();
                // DEBUG
                HJMain.LOGGER.debug("Planted bud at " + budPos.toShortString());
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
}
