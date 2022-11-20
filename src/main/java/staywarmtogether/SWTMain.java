package staywarmtogether;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import staywarmtogether.client.SWTClientEvents;

@Mod(SWTMain.MODID)
public class SWTMain {
    public static final String MODID = "staywarmtogether";

    public static final Logger LOGGER = LogManager.getLogger(SWTMain.MODID);

    public SWTMain() {
        // registry events
        SWTRegistry.register();
        // common event handlers
        FMLJavaModLoadingContext.get().getModEventBus().register(SWTEvents.ModHandler.class);
        MinecraftForge.EVENT_BUS.register(SWTEvents.ForgeHandler.class);
        // client event handlers
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().register(SWTClientEvents.ModHandler.class);
            MinecraftForge.EVENT_BUS.register(SWTClientEvents.ForgeHandler.class);
        });
    }
}
