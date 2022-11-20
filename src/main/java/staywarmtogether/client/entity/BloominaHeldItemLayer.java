package staywarmtogether.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import staywarmtogether.entity.Bloomina;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import software.bernie.geckolib3.renderers.geo.layer.AbstractLayerGeo;

import java.util.Optional;

public class BloominaHeldItemLayer <T extends Bloomina> extends AbstractLayerGeo<T> {

    private final ItemInHandRenderer itemInHandRenderer;

    public BloominaHeldItemLayer(GeoEntityRenderer<T> entityRendererIn, ItemInHandRenderer itemRenderer) {
        super(entityRendererIn, entityRendererIn::getTextureLocation, entity -> entityRendererIn.getGeoModelProvider().getModelResource(entity));
        this.itemInHandRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        GeoModel model = getEntityModel().getModel(this.funcGetCurrentModel.apply(entity));
        Optional<GeoBone> arm = model.getBone("arm0");
        if(arm.isPresent()) {
            poseStack.mulPoseMatrix(arm.get().getModelSpaceXform());
            poseStack.translate(1.5D / 16.0D, -9.0D / 16.0D, -1.0D / 16.0D);
            ItemStack itemStack = entity.getItemInHand(InteractionHand.MAIN_HAND);
            itemInHandRenderer.renderItem(entity, itemStack, ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, false, poseStack, multiBufferSource, packedLight);
        }
        poseStack.popPose();
    }
}
