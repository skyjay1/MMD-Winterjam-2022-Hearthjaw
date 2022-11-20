package hearthjaw.client.entity;

import hearthjaw.HJMain;
import hearthjaw.entity.Rimeite;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class RimeiteModel<T extends Rimeite> extends AnimatedGeoModel<T> {

    private static final ResourceLocation MODEL = new ResourceLocation(HJMain.MODID, "geo/rimeite.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(HJMain.MODID, "animations/rimeite.animation.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(HJMain.MODID, "textures/entity/rimeite.png");

    public RimeiteModel() {
        super();
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(T animatable, int instanceId, AnimationEvent animationEvent) {
        // hide brick
        boolean hidden = !animatable.getHasBrick();
        final IBone brick = this.getBone("brick");
        brick.setHidden(hidden);
        // run custom animations
        super.setCustomAnimations(animatable, instanceId, animationEvent);
    }
}
