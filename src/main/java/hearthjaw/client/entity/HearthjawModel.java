package hearthjaw.client.entity;

import hearthjaw.HJMain;
import hearthjaw.entity.Hearthjaw;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class HearthjawModel<T extends Hearthjaw> extends AnimatedGeoModel<T> {

    private static final ResourceLocation MODEL = new ResourceLocation(HJMain.MODID, "geo/hearthjaw.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(HJMain.MODID, "animations/hearthjaw.animation.json");

    private static final ResourceLocation TEXTURE_COLD = new ResourceLocation(HJMain.MODID, "textures/entity/hearthjaw/cold.png");
    private static final ResourceLocation TEXTURE_WARM = new ResourceLocation(HJMain.MODID, "textures/entity/hearthjaw/warm.png");

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
}
