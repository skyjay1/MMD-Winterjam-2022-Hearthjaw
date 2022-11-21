package staywarmtogether.client;

import staywarmtogether.SWTRegistry;
import staywarmtogether.client.entity.BloominaRenderer;
import staywarmtogether.client.entity.HearthjawRenderer;
import staywarmtogether.client.entity.RimeiteQueenRenderer;
import staywarmtogether.client.entity.RimeiteRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import staywarmtogether.client.entity.SnowBrickModel;
import staywarmtogether.client.entity.SnowBrickRenderer;

public final class SWTClientEvents {

    public static final class ForgeHandler {

    }

    public static final class ModHandler {

        @SubscribeEvent
        public static void registerEntityLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(SnowBrickModel.SNOW_BRICK_LOCATION, SnowBrickModel::createLayer);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(SWTRegistry.EntityReg.BLOOMINA.get(), BloominaRenderer::new);
            event.registerEntityRenderer(SWTRegistry.EntityReg.HEARTHJAW.get(), HearthjawRenderer::new);
            event.registerEntityRenderer(SWTRegistry.EntityReg.RIMEITE_QUEEN.get(), RimeiteQueenRenderer::new);
            event.registerEntityRenderer(SWTRegistry.EntityReg.RIMEITE.get(), RimeiteRenderer::new);
            event.registerEntityRenderer(SWTRegistry.EntityReg.SNOW_BRICK.get(), SnowBrickRenderer::new);
        }

    }
}
