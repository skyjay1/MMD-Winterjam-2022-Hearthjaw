package hearthjaw.client.entity;

import hearthjaw.entity.Hearthjaw;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class HearthjawRenderer<T extends Hearthjaw> extends GeoEntityRenderer<T> {

    public HearthjawRenderer(EntityRendererProvider.Context context) {
        super(context, new HearthjawModel<>());
        this.addLayer(new HearthjawEyesLayer<>(this));
    }
}
