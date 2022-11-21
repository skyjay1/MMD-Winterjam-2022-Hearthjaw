package staywarmtogether.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.NetworkHooks;
import staywarmtogether.SWTRegistry;


public class SnowBrickProjectile extends Projectile {

    protected int lifespan = 300;

    public SnowBrickProjectile(EntityType<? extends SnowBrickProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult raytrace) {
        super.onHitEntity(raytrace);
        Entity thrower = getOwner();
        if (raytrace.getEntity() != thrower && raytrace.getEntity() instanceof LivingEntity livingEntity) {
            // inflict impact damage
            float damage = getImpactDamage(livingEntity);
            if (damage > 0) {
                livingEntity.hurt(DamageSource.indirectMobAttack(this, thrower instanceof LivingEntity ? (LivingEntity) thrower : null), damage);
            }
            // add particle effect
            addParticles(getImpactParticle(livingEntity), 20 + random.nextInt(6));
        }
    }

    @Override
    protected void onHit(HitResult raytrace) {
        super.onHit(raytrace);
        if (this.isAlive()) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        // nothing
    }

    @Override
    public void tick() {
        // remove if too old
        if (this.tickCount > lifespan) {
            discard();
            return;
        }
        // check for impact
        if (!this.level.isClientSide()) {
            HitResult raytraceresult = ProjectileUtil.getHitResult(this, this::canHitEntity);
            if (raytraceresult.getType() != HitResult.Type.MISS
                    && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, raytraceresult)) {
                this.onHit(raytraceresult);
            }
        }
        // particle trail
        if (this.tickCount > 2) {
            addParticles(getTrailParticle(), 2);
        }
        // gravity
        if(!isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -getGravity(), 0));
        }
        // movement
        Vec3 motion = this.getDeltaMovement();
        double d0 = this.getX() + motion.x;
        double d1 = this.getY() + motion.y;
        double d2 = this.getZ() + motion.z;
        // lerp rotation and pitch
        this.updateRotation();
        // actually move the entity
        this.setPos(d0, d1, d2);
        // super method
        super.tick();
    }

    @Override
    public Entity changeDimension(ServerLevel serverWorld, ITeleporter iTeleporter) {
        Entity entity = getOwner();
        if (entity != null && entity.level.dimension() != serverWorld.dimension()) {
            setOwner(null);
        }
        return super.changeDimension(serverWorld, iTeleporter);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public ParticleOptions getImpactParticle(final LivingEntity entity) {
        return ParticleTypes.ITEM_SNOWBALL;
    }

    public ParticleOptions getTrailParticle() {
        return ParticleTypes.ITEM_SNOWBALL;
    }

    public float getImpactDamage(final LivingEntity entity) {
        return entity.isSensitiveToWater() ? 6.0F : 2.0F;
    }

    public float getGravity() {
        return 0.08F;
    }

    protected void addParticles(final ParticleOptions type, final int count) {
        if (this.level.isClientSide()) {
            final double x = getX();
            final double y = getY() + 0.1D;
            final double z = getZ();
            final double motion = 0.08D;
            final double width = getBbWidth() / 2;
            final double height = getBbHeight() / 2;
            for (int i = 0; i < count; i++) {
                level.addParticle(type,
                        x + (level.random.nextDouble() - 0.5D) * width,
                        y + height,
                        z + (level.random.nextDouble() - 0.5D) * width,
                        (level.random.nextDouble() - 0.5D) * motion,
                        (level.random.nextDouble() - 0.5D) * motion,
                        (level.random.nextDouble() - 0.5D) * motion);
            }
        }
    }
}
