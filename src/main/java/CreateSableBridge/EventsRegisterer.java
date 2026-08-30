package CreateSableBridge;

import CreateSableBridge.bridge.MechanicalBearingBlockEntitySubLevelBridge;
import net.neoforged.neoforge.common.NeoForge;

public class EventsRegisterer {
    public static void register() {
        NeoForge.EVENT_BUS.register(MechanicalBearingBlockEntitySubLevelBridge.class);
    }
}
