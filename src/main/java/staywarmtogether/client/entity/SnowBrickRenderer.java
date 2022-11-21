package staywarmtogether.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import staywarmtogether.SWTMain;
import staywarmtogether.entity.SnowBrickProjectile;

public class SnowBrickRenderer<T extends SnowBrickProjectile> extends EntityRenderer<T> {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(SWTMain.MODID, "textures/entity/snow_brick.png");
    protected SnowBrickModel<T> entityModel;

    public SnowBrickRenderer(EntityRendererProvider.Context context) {
        super(context);
        entityModel = new SnowBrickModel<>(context.bakeLayer(SnowBrickModel.SNOW_BRICK_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(T p_114482_) {
        return TEXTURE;
    }

    @Override
    public void render(T entity, float renderOffsetX, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        this.entityModel.prepareMobModel(entity, 0.0F, 0.0F, partialTick);
        this.entityModel.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        final VertexConsumer vertexConsumer = multiBufferSource.getBuffer(entityModel.renderType(getTextureLocation(entity)));
        this.entityModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        super.render(entity, renderOffsetX, partialTick, poseStack, multiBufferSource, packedLight);
    }
}
