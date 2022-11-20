package staywarmtogether.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import staywarmtogether.entity.Hearthjaw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

public class HearthjawRenderer<T extends Hearthjaw> extends GeoEntityRenderer<T> {

    public HearthjawRenderer(EntityRendererProvider.Context context) {
        super(context, new HearthjawModel<>());
        this.addLayer(new HearthjawEyesLayer<>(this));
    }

    public RenderType getRenderType(T animatable, float partialTick, PoseStack poseStack,
                             @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                             ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
