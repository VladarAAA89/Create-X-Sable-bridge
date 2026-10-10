package CreateSableBridge;

import CreateSableBridge.bridge.Bearing.BearingSavedData;
import CreateSableBridge.bridge.Bearing.MechanicalBearingBlockEntitySubLevelBridge;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SableAssemblyHandler {

    // УНИВЕРСАЛЬНАЯ СБОРКА
    public static boolean tryAssemble(BlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return false;
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockPos pos = be.getBlockPos();

        try {
            // 1. Проверка на Подшипник
            if (be instanceof MechanicalBearingBlockEntity bearingBE) {
                Direction direction = bearingBE.getBlockState().hasProperty(BlockStateProperties.FACING)
                        ? bearingBE.getBlockState().getValue(BlockStateProperties.FACING)
                        : Direction.UP;

                BearingContraption contraption = new BearingContraption(false, direction);
                if (contraption.assemble(level, pos)) {
                    SablePhysicsHandler.applyRotaryPhysics(level, bearingBE, contraption);
                    return true; // Успешно собрали через Sable
                }
            }

            // 2. Проверка на Поршень (задел на будущее)
            else if (be instanceof MechanicalPistonBlockEntity pistonBE) {
                // Здесь будет сборка PistonContraption
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // УНИВЕРСАЛЬНАЯ РАЗБОРКА
    public static boolean tryDisassemble(BlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return false;
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockPos pos = be.getBlockPos();

        try {
            // 1. Проверка на Подшипник
            if (be instanceof MechanicalBearingBlockEntity) {
                MechanicalBearingBlockEntitySubLevelBridge.removeBearingSubLevel(level, pos);
                return true;
            }

            // 2. Проверка на Поршень (задел на будущее)
            else if (be instanceof MechanicalPistonBlockEntity) {
                // Возврат блоков поршня...
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // В классе SableAssemblyHandler.java добавьте

    public static boolean isSableControlled(BlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return false;
        BearingSavedData savedData = BearingSavedData.get((ServerLevel) be.getLevel());
        return savedData.bearings.containsKey(be.getBlockPos());
    }
}