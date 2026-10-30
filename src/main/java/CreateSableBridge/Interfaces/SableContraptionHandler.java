package CreateSableBridge.Interfaces;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface SableContraptionHandler {
    boolean assemble(ServerLevel level, Contraption contraption, BlockPos offset);
}
