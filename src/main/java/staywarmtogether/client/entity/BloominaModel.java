package staywarmtogether.client.entity;

import staywarmtogether.SWTMain;
import staywarmtogether.entity.Bloomina;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class BloominaModel<T extends Bloomina> extends AnimatedGeoModel<T> {

    private static final ResourceLocation MODEL = new ResourceLocation(SWTMain.MODID, "geo/bloomina.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(SWTMain.MODID, "animations/bloomina.animation.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(SWTMain.MODID, "textures/entity/bloomina.png");

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
