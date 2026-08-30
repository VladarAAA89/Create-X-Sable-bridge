//package CreateSableBridge.mixin;
//
//import com.simibubi.create.content.contraptions.AssemblyException;
//import com.simibubi.create.content.contraptions.bearing.BearingContraption;
//import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import CreateSableBridge.bridge.MechanicalBearingBlockEntitySubLevelBridge;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
//public abstract class MechanicalBearingMixin {
//
//    //@Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
//    private void interceptSableAssembly(CallbackInfo ci) throws AssemblyException {
//        MechanicalBearingBlockEntity bearing = (MechanicalBearingBlockEntity) (Object) this;
//        ServerLevel level = (ServerLevel) bearing.getLevel();
//
//        if (level == null || level.isClientSide()) return;
//
//        BlockPos bearingPos = bearing.getBlockPos();
//        Direction bearingFacing = bearing.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
//        float currentSpeed = bearing.getSpeed();
//
//        // 1. Позволяем Create просканировать блоки
//        BearingContraption contraption = new BearingContraption();
//        if (!contraption.assemble(level, bearingPos)) {
//            return;
//        }
//
//        // 2. Извлекаем собранные блоки и конвертируем их для вашего Bridge
//        Map<BlockPos, BlockState> sableBlocks = new HashMap<>();
//        for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
//            // Create хранит локальные координаты (относительно подшипника), нам нужно перевести их в глобальные
//            BlockPos globalPos = contraption.anchor.offset(entry.getKey());
//            sableBlocks.put(globalPos, entry.getValue().state());
//        }
//
//        // 3. Вызываем ваш мост для создания физического субуровня Sable
//        MechanicalBearingBlockEntitySubLevelBridge.createBearingSubLevel(
//                level,
//                bearingPos,
//                bearingFacing,
//                sableBlocks,
//                currentSpeed
//        );
//
//        // 4. Очищаем оригинальные блоки из мира (как это делает Create)
//        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
//
//        // 5. Отменяем оригинальный метод, чтобы Create не спавнил свой фейковый ContraptionEntity
//        ci.cancel();
//    }
//}