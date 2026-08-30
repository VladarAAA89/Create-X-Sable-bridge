package CreateSableBridge;

import CreateSableBridge.bridge.MechanicalBearingBlockEntitySubLevelBridge;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashMap;
import java.util.Map;

public class SablePhysicsHandler {

    // Обратите внимание: аргументы изменились!
    public static void applyRotaryPhysics(ServerLevel level, MechanicalBearingBlockEntity be, BearingContraption bearing) {
        BlockPos anchorPos = bearing.anchor;

        // 1. Копируем блоки для Sable с правильными глобальными координатами
        Map<BlockPos, BlockState> sableBlocks = new HashMap<>();
        for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : bearing.getBlocks().entrySet()) {
            BlockPos globalPos = anchorPos.offset(entry.getKey());
            sableBlocks.put(globalPos, entry.getValue().state());
        }

        if (sableBlocks.isEmpty()) return;

        // 2. Получаем ось вращения
        Direction axis = Direction.UP;
        BlockState bearingState = level.getBlockState(anchorPos);
        if (bearingState.hasProperty(BlockStateProperties.FACING)) {
            axis = bearingState.getValue(BlockStateProperties.FACING);
        }

        // 3. Получаем скорость (теперь напрямую из переданного BlockEntity)
        float speedRpm = be.getSpeed();

        // 4. Генерируем физический мир Sable
        MechanicalBearingBlockEntitySubLevelBridge.createBearingSubLevel(
                level, anchorPos, axis, sableBlocks, speedRpm
        );

        // 5. ОЧЕНЬ ВАЖНО: Удаляем реальные блоки из мира Minecraft,
        // чтобы они не дублировались с физическим объектом Sable.
        bearing.removeBlocksFromWorld(level, BlockPos.ZERO);
    }
}