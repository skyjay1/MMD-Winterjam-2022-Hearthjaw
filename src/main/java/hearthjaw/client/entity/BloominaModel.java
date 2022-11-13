package hearthjaw.client.entity;

import hearthjaw.HJMain;
import hearthjaw.entity.Bloomina;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class BloominaModel<T extends Bloomina> extends AnimatedGeoModel<T> {

    private static final ResourceLocation MODEL = new ResourceLocation(HJMain.MODID, "geo/bloomina.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(HJMain.MODID, "animations/bloomina.animation.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(HJMain.MODID, "textures/entity/bloomina.png");

    public BloominaModel() {
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
}
