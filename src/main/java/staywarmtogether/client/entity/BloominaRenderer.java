package staywarmtogether.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import staywarmtogether.entity.Bloomina;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

public class BloominaRenderer<T extends Bloomina> extends GeoEntityRenderer<T> {

    public BloominaRenderer(EntityRendererProvider.Context context) {
        super(context, new BloominaModel<>());
        this.addLayer(new BloominaHeldItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(GeoModel model, T animatable, float partialTick, RenderType type, PoseStack poseStack,
                       MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlay,
                       float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        if(animatable.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        super.render(model, animatable, partialTick, type, poseStack, bufferSource, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

        @Override
    public RenderType getRenderType(T animatable, float partialTick, PoseStack poseStack,
                                    @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                                    ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return entity.getLightLevel();
    }
}
