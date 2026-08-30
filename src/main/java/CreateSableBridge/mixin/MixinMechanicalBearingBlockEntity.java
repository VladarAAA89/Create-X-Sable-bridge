package CreateSableBridge.mixin;

import CreateSableBridge.bridge.MechanicalBearingBlockEntitySubLevelBridge;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
public abstract class MixinMechanicalBearingBlockEntity {

    @Shadow protected boolean running;

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    private void redirectAssembleToSable(CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;

        if (be.getLevel() == null || be.getLevel().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) be.getLevel();
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();

        if (!state.hasProperty(BlockStateProperties.FACING)) return;
        Direction direction = state.getValue(BlockStateProperties.FACING);

        try {
            BearingContraption contraption = new BearingContraption(false, direction);
            if (!contraption.assemble(level, pos)) {
                return;
            }

            Map<BlockPos, StructureTemplate.StructureBlockInfo> rawBlocks = contraption.getBlocks();
            Map<BlockPos, BlockState> worldBlocks = new HashMap<>();

            // Точка отсчета Create — это блок, к которому крепится конструкция
            BlockPos anchorPos = contraption.anchor != null ? contraption.anchor : pos.relative(direction);

            for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : rawBlocks.entrySet()) {
                // Прибавляем смещение к anchorPos, а не к pos
                BlockPos absolutePos = anchorPos.offset(entry.getKey());
                worldBlocks.put(absolutePos, entry.getValue().state());
            }

            if (!worldBlocks.isEmpty()) {
                float initialSpeed = be.getSpeed();

                MechanicalBearingBlockEntitySubLevelBridge.createBearingSubLevel(
                        level, pos, direction, worldBlocks, initialSpeed
                );

                this.running = true;
                ci.cancel();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void redirectDisassembleFromSable(CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;

        if (be.getLevel() == null || be.getLevel().isClientSide()) {
            return;
        }

        if (!this.running) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) be.getLevel();
        BlockPos pos = be.getBlockPos();

        MechanicalBearingBlockEntitySubLevelBridge.removeBearingSubLevel(serverLevel, pos);
        this.running = false;

        ci.cancel();
    }

    @Inject(method = "onSpeedChanged", at = @At("HEAD"))
    private void interceptSpeedChange(float previousSpeed, CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;

        if (be.getLevel() == null || be.getLevel().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) be.getLevel();
        BlockPos pos = be.getBlockPos();
        float newSpeed = be.getSpeed();

        MechanicalBearingBlockEntitySubLevelBridge.updateVelocityByPos(level, pos, newSpeed);
    }
}