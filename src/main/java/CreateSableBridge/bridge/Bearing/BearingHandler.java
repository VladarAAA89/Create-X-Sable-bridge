package CreateSableBridge.bridge.Bearing;

import CreateSableBridge.Interfaces.SableContraptionHandler;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class BearingHandler implements SableContraptionHandler {
    @Override
    public boolean assemble(ServerLevel level, Contraption c, BlockPos offset) {
        if (!(c instanceof BearingContraption bc)) return false;
        try {
            Direction facing = bc.getFacing();
            BlockPos bearingPos = bc.anchor.relative(facing.getOpposite()).offset(offset);

            Map<BlockPos, BlockState> worldBlocks = new HashMap<>();
            bc.getBlocks().forEach((local, info) ->
                    worldBlocks.put(local.offset(bc.anchor).offset(offset), info.state()));

            float rpm = level.getBlockEntity(bearingPos) instanceof KineticBlockEntity k ? k.getSpeed() : 0f;

            MechanicalBearingBlockEntitySubLevelBridge.createBearingSubLevel(
                    level, bearingPos, facing, worldBlocks, rpm);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}