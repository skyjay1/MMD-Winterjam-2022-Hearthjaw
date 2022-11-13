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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.FlyingAnimal;
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
import net.minecraft.world.level.pathfinder.BlockPathTypes;
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

public class Bloomina extends AgeableMob implements FlyingAnimal, IAnimatable {

    // SYNCHED DATA //
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Bloomina.class, EntityDataSerializers.BYTE);
    private static final String KEY_STATE = "State";
    // STATES //
    private static final byte STATE_IDLE = (byte) 0;
    private static final byte STATE_BURP = (byte) 1;
    private static final byte STATE_HIDE = (byte) 2;

    // GECKOLIB //
    private AnimationFactory factory = GeckoLibUtil.createFactory(this);

    //// CONSTRUCTOR ////

    public Bloomina(EntityType<? extends AgeableMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.26F)
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FLYING_SPEED, 0.9F);
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
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
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
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(!isFlying());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevel, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
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

    //// GECKOLIB ////

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        byte state = getState();
        switch(state) {
            case STATE_BURP:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.burp", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                break;
            case STATE_HIDE:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bloomina.hide", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME));
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
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(KEY_STATE, getState());
    }

    //// FLYING ANIMAL ////

    @Override
    public boolean isFlying() {
        return !isHiding();
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.isEmptyBlock(pos) ? 10.0F : 0.0F;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    //// GOALS ////

}
