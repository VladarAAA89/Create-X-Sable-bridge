package CreateSableBridge;

import CreateSableBridge.bridge.MechanicalBearingBlockEntitySubLevelBridge;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import CreateSableBridge.EventsRegisterer;

@Mod(CreateSableBridge.MOD_ID)
public class CreateSableBridge {
    // ID мода должен совпадать с тем, что написано в src/main/resources/META-INF/neoforge.mods.toml
    public static final String MOD_ID = "createsable_bridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CreateSableBridge() {
        LOGGER.info("Create + Sable Compat initialized!");
        EventsRegisterer.register();
    }
}