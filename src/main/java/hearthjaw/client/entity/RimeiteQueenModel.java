package hearthjaw.client.entity;

import hearthjaw.HJMain;
import hearthjaw.entity.Rimeite;
import hearthjaw.entity.RimeiteQueen;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class RimeiteQueenModel<T extends RimeiteQueen> extends AnimatedGeoModel<T> {

    private static final ResourceLocation MODEL = new ResourceLocation(HJMain.MODID, "geo/rimeite_queen.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(HJMain.MODID, "animations/rimeite_queen.animation.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(HJMain.MODID, "textures/entity/rimeite_queen.png");

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
}
