package CreateSableBridge;

import CreateSableBridge.bridge.Bearing.MechanicalBearingBlockEntitySubLevelBridge;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import net.neoforged.neoforge.common.NeoForge;

public class EventsRegisterer {
    public static void register() {
        NeoForge.EVENT_BUS.register(MechanicalBearingBlockEntitySubLevelBridge.class);
        //NeoForge.EVENT_BUS.register(CreateAssembleCatcher.class);
    }
}
