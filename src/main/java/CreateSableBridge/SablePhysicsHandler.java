//package CreateSableBridge;
//
//import CreateSableBridge.bridge.Bearing.MechanicalBearingBlockEntitySubLevelBridge;
//import com.simibubi.create.content.contraptions.bearing.BearingContraption;
//import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
//import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
//import net.minecraft.world.phys.AABB;
//
//import java.util.*;
//
//public class SablePhysicsHandler {
//
//    // Обратите внимание: аргументы изменились!
//    public static void applyRotaryPhysics(ServerLevel level, MechanicalBearingBlockEntity be, BearingContraption bearing) {
//        BlockPos anchorPos = bearing.anchor;
//
//        // 1. Копируем блоки для Sable с правильными глобальными координатами
//        Map<BlockPos, BlockState> sableBlocks = new HashMap<>();
//        for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : bearing.getBlocks().entrySet()) {
//            BlockPos globalPos = anchorPos.offset(entry.getKey());
//            sableBlocks.put(globalPos, entry.getValue().state());
//        }
//
//        if (sableBlocks.isEmpty()) return;
//
//        // 2. Получаем ось вращения
//        Direction axis = Direction.UP;
//        BlockState bearingState = level.getBlockState(anchorPos);
//        if (bearingState.hasProperty(BlockStateProperties.FACING)) {
//            axis = bearingState.getValue(BlockStateProperties.FACING);
//        }
//
//        // 3. Получаем скорость (теперь напрямую из переданного BlockEntity)
//        float speedRpm = be.getSpeed();
//
//        // 4. Генерируем физический мир Sable
//        BlockPos bearingPos = be.getBlockPos();
//        Direction facing = be.getBlockState().getValue(BlockStateProperties.FACING);
//        BlockPos anchorBlock = bearingPos.relative(facing);
//
//        List<SuperGlueEntity> glue = findGlue(level, sableBlocks.keySet());
//        List<AABB> glueBoxes = new ArrayList<>();
//        for (SuperGlueEntity g : glue)
//            glueBoxes.add(g.getBoundingBox().move(-anchorBlock.getX(), -anchorBlock.getY(), -anchorBlock.getZ()));
//
//        MechanicalBearingBlockEntitySubLevelBridge.createBearingSubLevel(
//                level, bearingPos, facing, sableBlocks, be.getSpeed());
//
//        for (SuperGlueEntity g : glue) g.discard();
//    }
//
//    private static List<SuperGlueEntity> findGlue(ServerLevel level, Set<BlockPos> blocks) {
//        List<SuperGlueEntity> result = new ArrayList<>();
//        AABB area = null;
//        for (BlockPos p : blocks) {
//            AABB cell = new AABB(p);
//            area = area == null ? cell : area.minmax(cell);
//        }
//        if (area == null) return result;
//
//        for (SuperGlueEntity g : level.getEntitiesOfClass(SuperGlueEntity.class, area.inflate(1))) {
//            AABB b = g.getBoundingBox().deflate(0.1);
//            for (BlockPos p : BlockPos.betweenClosed(
//                    BlockPos.containing(b.minX, b.minY, b.minZ),
//                    BlockPos.containing(b.maxX, b.maxY, b.maxZ))) {
//                if (blocks.contains(p)) { result.add(g); break; }
//            }
//        }
//        return result;
//    }
//}