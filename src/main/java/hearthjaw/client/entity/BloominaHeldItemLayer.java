package hearthjaw.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import hearthjaw.entity.Bloomina;
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

    private static void translateRotate(final GeoBone bone, final PoseStack poseStack) {
        poseStack.mulPoseMatrix(bone.getModelSpaceXform());
        /*

        poseStack.translate(bone.getPositionX() / 16.0F, bone.getPositionY() / 16.0F, bone.getPositionZ() / 16.0F);
        if (bone.getRotationZ() != 0.0F) {
            poseStack.mulPose(Vector3f.ZP.rotation(this.zRot));
        }

        if (bone.getRotationY() != 0.0F) {
            poseStack.mulPose(Vector3f.YP.rotation(this.yRot));
        }

        if (bone.getRotationX() != 0.0F) {
            poseStack.mulPose(Vector3f.XP.rotation(this.xRot));
        }

        if (bone.getScaleX() != 1.0F || bone.getScaleY() != 1.0F || bone.getScaleZ() != 1.0F) {
            poseStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
        }*/
    }
}
