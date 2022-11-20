package staywarmtogether.client.entity;

import staywarmtogether.SWTMain;
import staywarmtogether.entity.Hearthjaw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class HearthjawModel<T extends Hearthjaw> extends AnimatedGeoModel<T> {

    private static final ResourceLocation MODEL = new ResourceLocation(SWTMain.MODID, "geo/hearthjaw.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(SWTMain.MODID, "animations/hearthjaw.animation.json");

    private static final ResourceLocation TEXTURE_COLD = new ResourceLocation(SWTMain.MODID, "textures/entity/hearthjaw/cold.png");
    private static final ResourceLocation TEXTURE_WARM = new ResourceLocation(SWTMain.MODID, "textures/entity/hearthjaw/warm.png");

    public HearthjawModel() {
        super();
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        return entity.isWarm() ? TEXTURE_WARM : TEXTURE_COLD;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(T animatable, int instanceId, AnimationEvent animationEvent) {
        super.setCustomAnimations(animatable, instanceId, animationEvent);
        // calculate head rotation
        final float xRot = -Mth.lerp(animationEvent.getPartialTick(), animatable.xRotO, animatable.getXRot());
        final float yBodyRot = Mth.rotLerp(animationEvent.getPartialTick(), animatable.yBodyRotO, animatable.yBodyRot);
        final float yHeadRot = Mth.rotLerp(animationEvent.getPartialTick(), animatable.yHeadRotO, animatable.yHeadRot);
        final float yRot = -(yHeadRot - yBodyRot);
        // rotate head bone
        final IBone head = this.getBone("head");
        head.setRotationX((float) Math.toRadians(xRot));
        head.setRotationY((float) Math.toRadians(yRot));
    }
}
