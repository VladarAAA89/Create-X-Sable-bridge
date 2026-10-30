package CreateSableBridge.mixin;

import CreateSableBridge.bridge.Bearing.BearingSavedData;
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

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void createsable$blockVanillaEntity(CallbackInfo ci) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

        // Если это сущность подшипника Create, но мы уже контролируем этот подшипник через Sable
        if (self.getContraption() instanceof BearingContraption bc) {
            BlockPos bearingPos = bc.anchor.relative(bc.getFacing().getOpposite());
            BearingSavedData savedData = BearingSavedData.get(level);

            if (savedData.bearings.containsKey(bearingPos)) {
                // Уничтожаем энтити Create сразу при рождении, чтобы остался только саб-уровень Sable
                self.discard();
            }
        }
    }
}