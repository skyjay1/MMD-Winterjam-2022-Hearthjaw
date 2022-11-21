package staywarmtogether.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import staywarmtogether.SWTMain;
import staywarmtogether.entity.SnowBrickProjectile;

public class SnowBrickModel<T extends SnowBrickProjectile> extends EntityModel<T> {

    public static final ModelLayerLocation SNOW_BRICK_LOCATION = new ModelLayerLocation(new ResourceLocation(SWTMain.MODID, "snow_brick"), "snow_brick");
    private final ModelPart brick;

    public SnowBrickModel(ModelPart root) {
        this.brick = root.getChild("brick");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("brick", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -3.5F, 3.0F, 3.0F, 7.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 22.5F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // do nothing
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        brick.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
