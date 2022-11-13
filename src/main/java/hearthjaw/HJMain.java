package hearthjaw;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(HJMain.MODID)
public class HJMain {
    public static final String MODID = "hearthjaw";

    public static final Logger LOGGER = LogManager.getLogger(HJMain.MODID);

    public HJMain() {
        // registry events
        HJRegistry.register();
        // common event handlers
        FMLJavaModLoadingContext.get().getModEventBus().register(HJEvents.ModHandler.class);
        MinecraftForge.EVENT_BUS.register(HJEvents.ForgeHandler.class);
        // client event handlers
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().register(hearthjaw.client.HJClientEvents.ModHandler.class);
            MinecraftForge.EVENT_BUS.register(hearthjaw.client.HJClientEvents.ForgeHandler.class);
        });
    }
}
