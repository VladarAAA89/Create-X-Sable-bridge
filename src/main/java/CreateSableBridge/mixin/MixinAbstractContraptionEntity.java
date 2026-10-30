package CreateSableBridge.mixin;

import CreateSableBridge.bridge.Bearing.BearingSavedData;
import CreateSableBridge.bridge.Bearing.MechanicalBearingBlockEntitySubLevelBridge;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class MixinAbstractContraptionEntity {

    @Inject(method = "disassemble", at = @At("HEAD"))
    private void createsable$release(CallbackInfo ci) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;
        if (!(self.getContraption() instanceof BearingContraption bc)) return;

        BlockPos bearingPos = bc.anchor.relative(bc.getFacing().getOpposite());
        MechanicalBearingBlockEntitySubLevelBridge.releaseForCreate(level, bearingPos, self);
    }
}