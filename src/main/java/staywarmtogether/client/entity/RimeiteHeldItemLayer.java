package staywarmtogether.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import software.bernie.geckolib3.renderers.geo.layer.AbstractLayerGeo;
import staywarmtogether.entity.Rimeite;

import java.util.Optional;

public class RimeiteHeldItemLayer<T extends Rimeite> extends AbstractLayerGeo<T> {

    private final ItemInHandRenderer itemInHandRenderer;

    public RimeiteHeldItemLayer(GeoEntityRenderer<T> entityRendererIn, ItemInHandRenderer itemRenderer) {
        super(entityRendererIn, entityRendererIn::getTextureLocation, entity -> entityRendererIn.getGeoModelProvider().getModelResource(entity));
        this.itemInHandRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        GeoModel model = getEntityModel().getModel(this.funcGetCurrentModel.apply(entity));
        Optional<GeoBone> bone = model.getBone("bone");
        if(bone.isPresent()) {
            poseStack.mulPoseMatrix(bone.get().getModelSpaceXform());
            poseStack.translate(0.0D / 16.0D, 0.0D / 16.0D, -5.0D / 16.0D);
            poseStack.mulPose(Vector3f.XP.rotationDegrees(25.0F));
            ItemStack itemStack = entity.getItemInHand(InteractionHand.MAIN_HAND);
            itemInHandRenderer.renderItem(entity, itemStack, ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, false, poseStack, multiBufferSource, packedLight);
        }
        poseStack.popPose();
    }
}
