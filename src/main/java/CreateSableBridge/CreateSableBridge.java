package CreateSableBridge;

import CreateSableBridge.bridge.Bearing.BearingHandler;
import com.simibubi.create.AllContraptionTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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