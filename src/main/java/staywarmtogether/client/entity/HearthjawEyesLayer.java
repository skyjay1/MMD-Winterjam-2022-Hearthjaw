package staywarmtogether.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import staywarmtogether.SWTMain;
import staywarmtogether.entity.Hearthjaw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

public class HearthjawEyesLayer <T extends Hearthjaw> extends GeoLayerRenderer<T> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(SWTMain.MODID, "textures/entity/hearthjaw/eyes.png");

    public HearthjawEyesLayer(IGeoRenderer<T> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if(!entity.isNapping() && !entity.isInvisible()) {
            renderModel(getEntityModel(), TEXTURE, poseStack, multiBufferSource, packedLight, entity, partialTick, 1.0F, 1.0F, 1.0F);
        }
    }
}
