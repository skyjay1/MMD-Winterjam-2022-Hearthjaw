package staywarmtogether.client.entity;

import staywarmtogether.SWTMain;
import staywarmtogether.entity.RimeiteQueen;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class RimeiteQueenModel<T extends RimeiteQueen> extends AnimatedGeoModel<T> {

    private static final ResourceLocation MODEL = new ResourceLocation(SWTMain.MODID, "geo/rimeite_queen.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(SWTMain.MODID, "animations/rimeite_queen.animation.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(SWTMain.MODID, "textures/entity/rimeite_queen.png");

    public RimeiteQueenModel() {
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
        super.setCustomAnimations(animatable, instanceId, animationEvent);
        // hide brick
        final boolean brickHidden = !(animatable.getHasBrick() || animatable.isBricking());
        final IBone brick = this.getBone("brick");
        brick.setHidden(brickHidden);
        // update snow position
        final IBone snow = this.getBone("snow");
        final float startY = 17.0F;
        final float endY = 6.0F;
        final float oldSnow = (float) animatable.getOldSnow();
        final float snowPercent = 1.0F - (oldSnow / (float) RimeiteQueen.getMaxSnow());
        snow.setHidden(oldSnow < 1.0F);
        snow.setPositionY((-(startY - endY) * snowPercent));
    }
}
