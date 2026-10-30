package CreateSableBridge;

import CreateSableBridge.bridge.Bearing.BearingHandler;
import CreateSableBridge.bridge.Bearing.MechanicalBearingBlockEntitySubLevelBridge;
import com.simibubi.create.AllContraptionTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import CreateSableBridge.EventsRegisterer;

@Mod(CreateSableBridge.MOD_ID)
public class CreateSableBridge {
    public static final String MOD_ID = "createsable_bridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CreateSableBridge(IEventBus modBus) {
        LOGGER.info("Create + Sable Compat initialized!");
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() ->
                SableContraptionHandlers.register(AllContraptionTypes.BEARING.value(), new BearingHandler())));
    }
}