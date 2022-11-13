package hearthjaw.client;

import hearthjaw.HJRegistry;
import hearthjaw.client.entity.BloominaRenderer;
import hearthjaw.client.entity.HearthjawModel;
import hearthjaw.client.entity.HearthjawRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class HJClientEvents {

    public static final class ForgeHandler {

    }

    public static final class ModHandler {

        @SubscribeEvent
        public static void registerEntityLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {

        }

        @SubscribeEvent
        public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(HJRegistry.EntityReg.BLOOMINA.get(), BloominaRenderer::new);
            event.registerEntityRenderer(HJRegistry.EntityReg.HEARTHJAW.get(), HearthjawRenderer::new);
        }

    }
}
